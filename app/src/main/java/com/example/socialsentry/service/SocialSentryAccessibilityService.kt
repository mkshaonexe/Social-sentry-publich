package com.example.socialsentry.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.socialsentry.data.datastore.SocialSentryDataStore
import com.example.socialsentry.data.datastore.YouTubeTrackingDataStore
import com.example.socialsentry.domain.SocialSentryNotificationManager
import com.example.socialsentry.data.model.SocialSentrySettings
import com.example.socialsentry.data.model.YouTubeSession
import com.example.socialsentry.data.model.VideoCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.TimeZone

class SocialSentryAccessibilityService : AccessibilityService(), KoinComponent {

    private val dataStore: SocialSentryDataStore by inject()
    private val youtubeDataStore: YouTubeTrackingDataStore by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var notificationManager: SocialSentryNotificationManager

    @Volatile
    private var settings = SocialSentrySettings()

    // Anti-spam mechanism
    private var lastBlockTime = 0L
    private var lastFacebookBlockTime = 0L
    private val blockDebounceMs = 300L
    private val facebookDebounceMs = 1200L
    private var lastReelsCheckTime = 0L
    private val reelsCheckIntervalMs = 600L
    private var lastFacebookRedirectTime = 0L
    private val facebookPostRedirectCooldownMs = 3000L // 3 seconds cooldown after redirect

    companion object {
        private const val TAG = "SocialSentry"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "SocialSentry Accessibility Service connected")
        notificationManager = SocialSentryNotificationManager(this)
        serviceScope.launch {
            dataStore.settingsFlow.collect { latestSettings ->
                settings = latestSettings
                Log.d(TAG, "Settings updated: ${settings}")
            }
        }
        // Initialize YouTube tracking - check if we need to reset daily stats
        serviceScope.launch {
            youtubeDataStore.checkAndResetIfNewDay()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.d(TAG, "🔍 onAccessibilityEvent called with event: ${event?.eventType} from ${event?.packageName}")
        val packageName = event?.packageName?.toString() ?: return
        val root = rootInActiveWindow ?: return

        // Filter relevant events to avoid excessive processing
        val eventType = event.eventType
        val isRelevantEvent = when (eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> true
            else -> false
        }

        if (!isRelevantEvent) return

        Log.v(TAG, "Processing event from $packageName, type: $eventType")
        
        // End YouTube session if user switches away from YouTube
        if (packageName != "com.google.android.youtube" && currentYouTubeSession != null) {
            serviceScope.launch {
                endCurrentYouTubeSession()
            }
        }
        
        // Update current YouTube session if it exists and we're still on YouTube
        if (packageName == "com.google.android.youtube" && currentYouTubeSession != null) {
            val now = System.currentTimeMillis()
            val sessionDuration = now - (youtubeSessionStartTime ?: now)
            
            // Log session progress every 10 seconds
            if (sessionDuration > 0 && sessionDuration % 10000 < 1000) {
                Log.d(TAG, "YouTube tracking: Session in progress - '${currentYouTubeSession?.title}' (${sessionDuration / 1000}s)")
            }
        }

        val currentMinute = getCurrentMinuteOfDay()

        // Mindful scroll tracking should run regardless of unblock state
        if (packageName == "com.instagram.android" || packageName == "com.facebook.katana") {
            trackMindfulScroll(packageName, event)
        }
        
        // Scroll limiter tracking DISABLED - users can scroll freely on home feeds
        // Scroll limiter functionality has been completely disabled per user request

        // If user has an active temporary unblock session, skip blocking logic only
        if (settings.isTemporaryUnblockActive) {
            return
        }

        when (packageName) {
            "com.instagram.android" -> handleInstagram(root, currentMinute)
            "com.facebook.katana" -> handleFacebook(root, event, currentMinute)
            "com.google.android.youtube" -> {
                handleYoutube(root, currentMinute)
                // Track YouTube usage for stats (independent of blocking)
                trackYouTubeUsage(root)
            }
            "com.zhiliaoapp.musically" -> handleTikTok(currentMinute)
            "com.pinterest" -> {
                Log.d(TAG, "🚫 Pinterest blocking check - isBlocked: ${settings.pinterest.isBlocked}, isTemporaryUnblockActive: ${settings.isTemporaryUnblockActive}")
                handlePinterest(root, currentMinute)
            }
            // Threads currently only uses scroll limiter; show overlay during mandatory break
            "com.instagram.barcelona", "com.instagram.threadsapp" -> handleThreads(currentMinute)
        }
    }

    private fun handleInstagram(root: AccessibilityNodeInfo, currentMinute: Int) {
        // Scroll limiter mandatory break overlay DISABLED - users can scroll freely
        
        val app = settings.instagram
        if (!app.isBlocked || !isWithinTimeRange(app.blockTimeStart, app.blockTimeEnd, currentMinute)) {
            return
        }

        val reelsFeature = app.features.find { it.name == "Reels" }
        if (reelsFeature?.isEnabled == true && isWithinTimeRange(reelsFeature.startTime, reelsFeature.endTime, currentMinute)) {
            blockInstagram(root)
        }
    }
    
    private fun blockInstagram(root: AccessibilityNodeInfo) {
        // V9-style Instagram blocking - look for Reels container
        val reelView = root.findAccessibilityNodeInfosByViewId(
            "com.instagram.android:id/clips_swipe_refresh_container"
        ).firstOrNull()

        if (reelView != null) {
            Log.d(TAG, "Instagram Reels detected - redirecting to feed")
            val feedTab = root.findAccessibilityNodeInfosByViewId(
                "com.instagram.android:id/feed_tab"
            ).firstOrNull()
            exitTheDoom(feedTab)
        }
    }

    private fun handleYoutube(root: AccessibilityNodeInfo, currentMinute: Int) {
        // Scroll limiter mandatory break overlay DISABLED - users can scroll freely
        
        val app = settings.youtube
        if (!app.isBlocked || !isWithinTimeRange(app.blockTimeStart, app.blockTimeEnd, currentMinute)) {
            return
        }

        val shortsFeature = app.features.find { it.name == "Shorts" }
        if (shortsFeature?.isEnabled == true && isWithinTimeRange(shortsFeature.startTime, shortsFeature.endTime, currentMinute)) {
            blockYouTube(root)
        }
    }

    private fun handleThreads(currentMinute: Int) {
        // Scroll limiter mandatory break overlay DISABLED - users can scroll freely
        // No content-specific blocking for Threads at the moment
    }
    
    private fun handlePinterest(root: AccessibilityNodeInfo, currentMinute: Int) {
        val now = System.currentTimeMillis()
        Log.d(TAG, "🔍 handlePinterest called - isBlocked: ${settings.pinterest.isBlocked}, currentMinute: $currentMinute, isTemporaryUnblockActive: ${settings.isTemporaryUnblockActive}")
        
        // PRIORITY: Enforce mandatory break overlay during cooldown window
        val breakEndTime = pinterestMandatoryBreakEndTime
        if (breakEndTime != null && now < breakEndTime) {
            val remainingSeconds = ((breakEndTime - now) / 1000).toInt()
            Log.d(TAG, "Pinterest scroll limiter: Mandatory break active (${remainingSeconds}s remaining)")
            showScrollLimiterOverlay("Pinterest", remainingSeconds)
            return
        }
        
        // Check if Pinterest is blocked by time schedule
        if (settings.pinterest.isBlocked && isWithinTimeRange(settings.pinterest.blockTimeStart, settings.pinterest.blockTimeEnd, currentMinute)) {
            Log.d(TAG, "🚫 Pinterest is blocked by time schedule - blocking now!")
            // For Pinterest, we treat total usage as "reels time" since it doesn't have reels
            // This means we block the entire app when time schedule is active
            blockPinterest()
            return
        }
        
        // Check if Pinterest is blocked by allowance time
        if (settings.pinterest.isBlocked && !settings.isTemporaryUnblockActive) {
            Log.d(TAG, "🚫 Pinterest is blocked - user needs to use allowance time - blocking now!")
            blockPinterest()
            return
        }
        
        Log.d(TAG, "✅ Pinterest is allowed - no blocking needed")
    }
    
    private fun blockPinterest() {
        // For Pinterest, we block the entire app since it doesn't have reels
        // We'll redirect to home screen or show a blocking overlay
        Log.d(TAG, "Pinterest: Blocking entire app (treating total usage as reels time)")
        
        // Try to go back to home screen
        performGlobalAction(GLOBAL_ACTION_HOME)
        
        // Show a toast notification
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(
                this,
                "🚫 Pinterest is blocked - use your allowance time to access it",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun blockYouTube(root: AccessibilityNodeInfo) {
        // V9-style YouTube blocking - look for Shorts container
        root.findAccessibilityNodeInfosByViewId(
            "com.google.android.youtube:id/reel_watch_fragment_root"
        ).firstOrNull() ?: return

        Log.d(TAG, "YouTube Shorts detected - redirecting to home")
        val pivotBar = root.findAccessibilityNodeInfosByViewId(
            "com.google.android.youtube:id/pivot_bar"
        ).firstOrNull()
        val homeTab = pivotBar?.getChild(0)?.getChild(0)

        exitTheDoom(homeTab) {
            homeTab?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    private fun handleTikTok(currentMinute: Int) {
        val app = settings.tiktok
        if (!app.isBlocked || !isWithinTimeRange(app.blockTimeStart, app.blockTimeEnd, currentMinute)) {
            return
        }

        blockTikTok()
    }
    
    private fun blockTikTok() {
        // V9-style TikTok blocking - always go to home screen
        Log.d(TAG, "TikTok blocked - going to home screen")
        exitTheDoom(null) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    private fun handleFacebook(root: AccessibilityNodeInfo, event: AccessibilityEvent?, currentMinute: Int) {
        val now = System.currentTimeMillis()
        val app = settings.facebook
        
        // PRIORITY 1: Reels/Stories/Watch blocking - should ALWAYS work when enabled
        // Scroll limiter is completely disabled - users can scroll freely on home feed
        if (app.isBlocked && isWithinTimeRange(app.blockTimeStart, app.blockTimeEnd, currentMinute)) {
            // Skip detection if we're in post-redirect cooldown to prevent reload loop
            if (now - lastFacebookRedirectTime < facebookPostRedirectCooldownMs) {
                Log.v(TAG, "Facebook: In post-redirect cooldown, skipping detection")
                return
            }
            
            // Enhanced Facebook detection with periodic checking
            val shouldPeriodicCheck = (now - lastReelsCheckTime) > reelsCheckIntervalMs
            
            if (shouldPeriodicCheck) {
                lastReelsCheckTime = now
                Log.v(TAG, "Performing periodic Facebook content check")
            }

            if (detectFacebookBlockableContent(root, event)) {
                if (now - lastFacebookBlockTime < facebookDebounceMs) {
                    Log.v(TAG, "Facebook blocking debounced")
                } else {
                    Log.d(TAG, "Facebook blocked content detected - redirecting")
                    lastFacebookBlockTime = now
                    lastFacebookRedirectTime = now
                    redirectToFacebookHome(root)
                }
            }
        }
        
        // Scroll limiter mandatory break overlay DISABLED - users can scroll freely
    }


    // Facebook Detection Methods  
    private fun detectFacebookBlockableContent(root: AccessibilityNodeInfo, event: AccessibilityEvent?): Boolean {
        val app = settings.facebook
        val reelsEnabled = app.features.find { it.name == "Reels" }?.isEnabled ?: false
        val storiesEnabled = app.features.find { it.name == "Stories" }?.isEnabled ?: false
        val watchEnabled = app.features.find { it.name == "Watch" }?.isEnabled ?: false

        if (!reelsEnabled && !storiesEnabled && !watchEnabled) {
            Log.v(TAG, "Facebook: No blockable features enabled")
            return false
        }

        // PRIORITY 1: Check if currently in blocked content view (most reliable)
        // This should run on ALL window state/content changes
        if (isInFacebookBlockedView(root)) {
            Log.d(TAG, "Facebook: Currently in blocked view")
            return true
        }

        // PRIORITY 2: Check if user clicked on blocked content (only for navigation to blocked content)
        // This is now more specific and won't trigger on normal feed posts
        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            event.source?.let { clickedNode ->
                if (isBlockedFacebookElement(clickedNode)) {
                    Log.d(TAG, "User clicked on blocked Facebook element (tab/button)")
                    return true
                }
            }
        }

        return false
    }

    private fun isBlockedFacebookElement(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val description = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        
        // Only check for SPECIFIC Reels/Stories/Watch identifiers
        // Do NOT use generic keywords like "like", "comment", "share" as they appear on normal posts
        val combined = "$text $description $viewId"
        
        // More specific keywords that indicate ONLY Reels/Stories/Watch, not normal feed
        val specificBlockedKeywords = listOf(
            "reels tab", "stories tab", "watch tab",
            "reels button", "stories button", "watch button",
            "open reels", "view reels", "view stories",
            "for you tab", "for you", // Reels-specific
            "short videos", "short videos tab",
            "reels icon", "reels", "reels"
        )
        
        // Check view IDs for specific blocked content containers
        val blockedViewIdKeywords = listOf(
            "reels_viewer", "stories_viewer", "watch_fragment",
            "reels_tab", "stories_tab", "watch_tab",
            "reels_player", "reels_pager", "reels_fragment",
            "reels_recycler", "reels_container"
        )
        
        // First check specific view IDs
        if (blockedViewIdKeywords.any { viewId.contains(it) }) {
            Log.d(TAG, "Facebook blocked element detected via view ID: $viewId")
            return true
        }
        
        // Then check specific text/description
        val matches = specificBlockedKeywords.any { keyword ->
            combined.contains(keyword)
        }
        
        if (matches) {
            Log.d(TAG, "Facebook blocked element detected via text/description: $combined")
        }
        
        return matches
    }

    private fun isInFacebookBlockedView(root: AccessibilityNodeInfo): Boolean {
        // Method 1: Check for blocked view IDs (updated with current Facebook Reels IDs)
        val blockedViewIds = listOf(
            "com.facebook.katana:id/reels_viewer_container",
            "com.facebook.katana:id/reels_player_container",
            "com.facebook.katana:id/reels_fragment",
            "com.facebook.katana:id/reels_viewpager",
            "com.facebook.katana:id/reels_tab",
            "com.facebook.katana:id/stories_viewer_container",
            "com.facebook.katana:id/stories_fragment",
            "com.facebook.katana:id/video_player_container",
            "com.facebook.katana:id/watch_fragment",
            "com.facebook.katana:id/video_feed",
            "com.facebook.katana:id/reels_pager",
            "com.facebook.katana:id/reels_recycler_view"
        )
        
        for (viewId in blockedViewIds) {
            try {
                val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
                if (nodes.isNotEmpty()) {
                    Log.d(TAG, "Facebook blocked content detected via view ID: $viewId")
                    return true
                }
            } catch (e: Exception) {
                // View ID might not exist, continue checking
            }
        }

        // Method 2: Check for Reels navigation tabs and text
        val reelsTexts = listOf(
            "for you", "For You", "FOR YOU",
            "explore", "Explore", "EXPLORE",
            "reels", "Reels", "REELS",
            "short videos", "Short Videos"
        )
        
        val hasReelsText = reelsTexts.any { text ->
            findTextInNode(root, listOf(text))
        }
        
        if (hasReelsText) {
            // Double check - make sure we're not on main feed
            val mainFeedTexts = listOf("home", "Home", "feed", "Feed")
            val isOnMainFeed = mainFeedTexts.any { text ->
                findTextInNode(root, listOf(text))
            }
            
            if (!isOnMainFeed) {
                Log.d(TAG, "Facebook Reels detected via text content")
                return true
            }
        }

        // Method 3: Check for Reels button/tab in navigation
        val reelsButtonTexts = listOf(
            "reels", "Reels", "Short Videos"
        )
        val reelsButtonDescriptions = listOf(
            "reels tab", "reels button", "reels icon",
            "short videos tab", "short videos button"
        )
        
        // Search for Reels navigation elements
        try {
            val clickableNodes = root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/bottom_navigation")
            for (node in clickableNodes) {
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i)
                    if (child != null) {
                        val text = child.text?.toString()?.lowercase() ?: ""
                        val desc = child.contentDescription?.toString()?.lowercase() ?: ""
                        val combined = "$text $desc"
                        
                        if (reelsButtonTexts.any { combined.contains(it.lowercase()) } ||
                            reelsButtonDescriptions.any { combined.contains(it) }) {
                            Log.d(TAG, "Facebook Reels navigation button detected")
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.v(TAG, "Error checking navigation bar: ${e.message}")
        }

        // Method 4: Check for vertical video layout (most reliable for Reels)
        return detectVerticalVideoContent(root, "Facebook")
    }

    private fun redirectToFacebookHome(root: AccessibilityNodeInfo) {
        Log.d(TAG, "Facebook: Attempting to redirect from blocked content")
        
        // Method 1: Try pressing back button multiple times - most reliable for exiting Reels
        try {
            performGlobalAction(GLOBAL_ACTION_BACK)
            Log.d(TAG, "Facebook: First back button pressed to exit Reels")
            
            // Press back multiple times with delays to ensure we exit Reels
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    Log.d(TAG, "Facebook: Second back button pressed")
                } catch (e: Exception) {
                    Log.e(TAG, "Facebook: Failed to press second back", e)
                }
            }, 300)
            
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val currentRoot = rootInActiveWindow
                if (currentRoot != null && isInFacebookBlockedView(currentRoot)) {
                    try {
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        Log.d(TAG, "Facebook: Third back button pressed")
                    } catch (e: Exception) {
                        Log.e(TAG, "Facebook: Failed to press third back", e)
                    }
                }
            }, 600)
            
            return
        } catch (e: Exception) {
            Log.e(TAG, "Facebook: Failed to press back button", e)
        }
        
        // Method 2: Try to find and click home tab in navigation bar
        val navBarIds = listOf(
            "com.facebook.katana:id/tab_bar",
            "com.facebook.katana:id/bottom_navigation",
            "com.facebook.katana:id/main_tab_bar"
        )
        
        for (navId in navBarIds) {
            try {
                val navBar = root.findAccessibilityNodeInfosByViewId(navId).firstOrNull()
                if (navBar != null && navBar.childCount > 0) {
                    // Try first tab (usually Home)
                    val homeTab = navBar.getChild(0)
                    if (homeTab != null && homeTab.isClickable) {
                        try {
                            homeTab.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            Log.d(TAG, "Facebook: Home tab clicked")
                            return
                        } catch (e: Exception) {
                            Log.e(TAG, "Facebook: Failed to click home tab", e)
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue to next navigation ID
            }
        }
        
        // Method 3: Last resort - go to home screen
        try {
            performGlobalAction(GLOBAL_ACTION_HOME)
            Log.d(TAG, "Facebook: Pressed home button as last resort")
        } catch (e: Exception) {
            Log.e(TAG, "Facebook: All redirection methods failed", e)
        }
    }

    // Mindful scroll detection (Instagram/Facebook normal feed)
    private var mindfulScrollStartMs: Long? = null
    private var mindfulLastPackage: String? = null
    private var mindfulNotificationShownForSession = false
    
    // Scroll limiter tracking - now supports multiple apps
    // Facebook scroll tracking
    private var facebookScrollStartTime: Long? = null
    private var lastFacebookScrollTime: Long? = null
    private var facebookScrollLimiterShown = false
    private var lastFacebookScrollLimiterTriggerTime: Long = 0L
    private var lastFacebookProgressNotificationSecond = 0
    
    // YouTube scroll tracking
    private var youtubeScrollStartTime: Long? = null
    private var lastYoutubeScrollTime: Long? = null
    private var youtubeScrollLimiterShown = false
    private var lastYoutubeScrollLimiterTriggerTime: Long = 0L
    private var lastYoutubeProgressNotificationSecond = 0
    
    // Instagram scroll tracking
    private var instagramScrollStartTime: Long? = null
    private var lastInstagramScrollTime: Long? = null
    private var instagramScrollLimiterShown = false
    private var lastInstagramScrollLimiterTriggerTime: Long = 0L
    private var lastInstagramProgressNotificationSecond = 0
    
    // Threads scroll tracking
    private var threadsScrollStartTime: Long? = null
    private var lastThreadsScrollTime: Long? = null
    private var threadsScrollLimiterShown = false
    private var lastThreadsScrollLimiterTriggerTime: Long = 0L
    private var lastThreadsProgressNotificationSecond = 0
    
    // Pinterest scroll tracking
    private var pinterestScrollStartTime: Long? = null
    private var lastPinterestScrollTime: Long? = null
    private var pinterestScrollLimiterShown = false
    private var lastPinterestScrollLimiterTriggerTime: Long = 0L
    private var lastPinterestProgressNotificationSecond = 0
    
    // Common scroll limiter configuration
    private val scrollLimiterCooldownMs = 10000L // 10 seconds cooldown after mandatory break ends
    private val scrollInactivityResetMs = 8000L // Reset if no interaction for 8 seconds
    private val mandatoryBreakDurationMs = 30000L // 30 seconds mandatory break
    
    // Mandatory break end times - one per app
    private var facebookMandatoryBreakEndTime: Long? = null
    private var youtubeMandatoryBreakEndTime: Long? = null
    private var instagramMandatoryBreakEndTime: Long? = null
    private var threadsMandatoryBreakEndTime: Long? = null
    private var pinterestMandatoryBreakEndTime: Long? = null
    
    // YouTube usage tracking
    private var currentYouTubeSession: YouTubeSession? = null
    private var lastYouTubeTitle: String? = null
    private var lastYouTubeChannel: String? = null
    private var youtubeSessionStartTime: Long? = null
    private val youtubeSessionDebounceMs = 15000L // 15 seconds to avoid premature session ending
    private var lastYouTubeTrackingTime: Long = 0L
    private val youtubeTrackingCooldownMs = 2000L // 2 second cooldown between tracking calls
    private var lastVideoDetectionTime: Long = 0L

    /**
     * Detects if user is currently watching a YouTube video (not scrolling feed)
     * Checks for video player UI elements in the accessibility hierarchy
     */
    private fun isUserWatchingYouTubeVideo(): Boolean {
        try {
            val rootNode = rootInActiveWindow ?: return false
            
            // Check for video player indicators in YouTube
            // These class names and IDs are used by YouTube's video player
            val videoPlayerIndicators = listOf(
                "com.google.android.youtube.player",
                "player_view",
                "player_fragment_container",
                "watch_player",
                "video_surface_view",
                "player_controls_container",
                "youtube_controls",
                "exo_player"
            )
            
            // Recursively search for video player elements
            fun searchForVideoPlayer(node: android.view.accessibility.AccessibilityNodeInfo?): Boolean {
                if (node == null) return false
                
                val className = node.className?.toString() ?: ""
                val viewId = node.viewIdResourceName ?: ""
                
                // Check if this node indicates a video player
                for (indicator in videoPlayerIndicators) {
                    if (className.contains(indicator, ignoreCase = true) || 
                        viewId.contains(indicator, ignoreCase = true)) {
                        Log.d(TAG, "YouTube video player detected: className=$className, viewId=$viewId")
                        return true
                    }
                }
                
                // Check children recursively
                for (i in 0 until node.childCount) {
                    if (searchForVideoPlayer(node.getChild(i))) {
                        return true
                    }
                }
                
                return false
            }
            
            val isWatching = searchForVideoPlayer(rootNode)
            rootNode.recycle()
            return isWatching
            
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting YouTube video player: ${e.message}")
            return false
        }
    }

    private fun trackMindfulScroll(packageName: String, event: AccessibilityEvent?) {
        // Only consider scroll events
        if (event?.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            // Reset if user switched app or view state
            if (mindfulLastPackage != packageName) {
                mindfulScrollStartMs = null
                mindfulNotificationShownForSession = false
            }
            mindfulLastPackage = packageName
            return
        }

        val now = System.currentTimeMillis()
        if (mindfulScrollStartMs == null || mindfulLastPackage != packageName) {
            mindfulScrollStartMs = now
            mindfulNotificationShownForSession = false
            mindfulLastPackage = packageName
            return
        }

        // 5 minutes threshold (300_000 ms)
        val elapsed = now - (mindfulScrollStartMs ?: now)
        val thresholdMs = 300_000L

        if (!mindfulNotificationShownForSession && elapsed >= thresholdMs) {
            // Only notify if reels/shorts are not active blocking redirections
            // and regardless of blocking on/off state — this is a gentle reminder
            notificationManager.showMindfulScrollNotification()
            mindfulNotificationShownForSession = true
        }
    }
    
    private fun trackScrollLimiter(event: AccessibilityEvent?, appName: String) {
        // Track multiple event types as apps don't always send TYPE_VIEW_SCROLLED
        // Accept: TYPE_VIEW_SCROLLED (4096), TYPE_WINDOW_STATE_CHANGED (32), 
        // TYPE_VIEW_CLICKED (1), TYPE_WINDOW_CONTENT_CHANGED (2048)
        val eventType = event?.eventType ?: return
        val isScrollOrInteraction = when (eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> true
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> true
            AccessibilityEvent.TYPE_VIEW_CLICKED -> true
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> true
            else -> false
        }
        
        if (!isScrollOrInteraction) {
            Log.v(TAG, "$appName scroll limiter: Event type $eventType not tracked, ignoring")
            return
        }
        
        // Special handling for YouTube: Don't track if user is watching a video
        if (appName == "YouTube" && isUserWatchingYouTubeVideo()) {
            Log.v(TAG, "YouTube scroll limiter: User is watching a video, not tracking")
            return
        }
        
        Log.d(TAG, "$appName scroll limiter: ✓ User interaction detected (event type: $eventType)!")
        
        val now = System.currentTimeMillis()
        
        // Get app-specific variables based on app name
        val (scrollStartTime, lastScrollTime, scrollLimiterShown, lastTriggerTime, lastProgressSecond, breakEndTime) = when (appName) {
            "Facebook" -> Tuple6(facebookScrollStartTime, lastFacebookScrollTime, facebookScrollLimiterShown, lastFacebookScrollLimiterTriggerTime, lastFacebookProgressNotificationSecond, facebookMandatoryBreakEndTime)
            "YouTube" -> Tuple6(youtubeScrollStartTime, lastYoutubeScrollTime, youtubeScrollLimiterShown, lastYoutubeScrollLimiterTriggerTime, lastYoutubeProgressNotificationSecond, youtubeMandatoryBreakEndTime)
            "Instagram" -> Tuple6(instagramScrollStartTime, lastInstagramScrollTime, instagramScrollLimiterShown, lastInstagramScrollLimiterTriggerTime, lastInstagramProgressNotificationSecond, instagramMandatoryBreakEndTime)
            "Threads" -> Tuple6(threadsScrollStartTime, lastThreadsScrollTime, threadsScrollLimiterShown, lastThreadsScrollLimiterTriggerTime, lastThreadsProgressNotificationSecond, threadsMandatoryBreakEndTime)
            "Pinterest" -> Tuple6(pinterestScrollStartTime, lastPinterestScrollTime, pinterestScrollLimiterShown, lastPinterestScrollLimiterTriggerTime, lastPinterestProgressNotificationSecond, pinterestMandatoryBreakEndTime)
            else -> return
        }
        
        // Check if we're in mandatory 30-second break - if so, don't track, just let blocking happen
        if (breakEndTime != null && now < breakEndTime) {
            val remainingSeconds = ((breakEndTime - now) / 1000).toInt()
            Log.v(TAG, "$appName scroll limiter: In mandatory break (${remainingSeconds}s remaining)")
            return
        } else if (breakEndTime != null && now >= breakEndTime) {
            // Break ended, clear it
            setAppBreakEndTime(appName, null)
            Log.d(TAG, "$appName scroll limiter: Mandatory break ended, resetting")
        }
        
        // Check if we're in cooldown period after a popup was shown
        if (now - lastTriggerTime < scrollLimiterCooldownMs) {
            Log.v(TAG, "$appName scroll limiter: In cooldown period")
            return
        }
        
        // Reset tracking if user stopped scrolling for too long
        val lastScroll = lastScrollTime ?: 0L
        if (lastScroll > 0 && (now - lastScroll) > scrollInactivityResetMs) {
            resetAppScrollTracking(appName)
            Log.d(TAG, "$appName scroll limiter: Reset due to inactivity")
        }
        
        // Update last scroll time
        setAppLastScrollTime(appName, now)
        
        // Initialize scroll start time if not set
        if (scrollStartTime == null) {
            setAppScrollStartTime(appName, now)
            setAppScrollLimiterShown(appName, false)
            setAppLastProgressSecond(appName, 0)
            Log.d(TAG, "$appName scroll limiter: ⏱️ Started tracking scroll time")
            
            // Show toast to inform user tracking has started
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val limitText = when (appName) {
                    "Facebook" -> "2 min limit"
                    "YouTube" -> "5 min limit"
                    else -> "1 min limit"
                }
                android.widget.Toast.makeText(
                    this,
                    "⏱️ $appName usage timer started ($limitText)",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            return
        }
        
        // Check if user has been scrolling for more than the threshold
        val scrollDuration = now - (scrollStartTime ?: now)
        
        // App-specific time thresholds
        val (thresholdMs, thresholdSeconds) = when (appName) {
            "Facebook" -> Pair(120000L, 120) // 2 minutes for Facebook
            "YouTube" -> Pair(300000L, 300) // 5 minutes for YouTube
            else -> Pair(60000L, 60) // 1 minute for other apps
        }
        
        // Show progress notifications at appropriate intervals
        val seconds = (scrollDuration / 1000).toInt()
        val shouldShowProgress = when (appName) {
            "Facebook" -> seconds >= 20 && seconds < thresholdSeconds && (seconds == 20 || seconds == 40 || seconds == 60 || seconds == 80 || seconds == 100)
            "YouTube" -> seconds >= 60 && seconds < thresholdSeconds && (seconds == 60 || seconds == 120 || seconds == 180 || seconds == 240)
            else -> seconds >= 20 && seconds < thresholdSeconds && (seconds == 20 || seconds == 40)
        }
        
        if (shouldShowProgress && lastProgressSecond != seconds) {
            setAppLastProgressSecond(appName, seconds)
            Log.d(TAG, "$appName scroll limiter: ${seconds}s elapsed")
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    this,
                    "⏱️ $appName usage: ${seconds}s / ${thresholdSeconds}s",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        Log.v(TAG, "$appName scroll limiter: Duration = ${scrollDuration/1000}s")
        
        if (!scrollLimiterShown && scrollDuration >= thresholdMs) {
            val thresholdText = when (appName) {
                "Facebook" -> "2 minute"
                "YouTube" -> "5 minute"
                else -> "1 minute"
            }
            Log.d(TAG, "$appName scroll limiter: 🚨 $thresholdText threshold reached (${scrollDuration/1000}s), showing overlay")
            
            // Activate mandatory 30-second break
            setAppBreakEndTime(appName, now + mandatoryBreakDurationMs)
            Log.d(TAG, "$appName scroll limiter: Mandatory 30-second break activated")
            
            showScrollLimiterOverlay(appName, 30)
            setAppScrollLimiterShown(appName, true)
            setAppLastTriggerTime(appName, now)
            // Reset for next session
            setAppScrollStartTime(appName, null)
            setAppLastScrollTime(appName, null)
        }
    }
    
    // Helper data class for scroll tracking
    private data class Tuple6<A, B, C, D, E, F>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F)
    
    // Helper methods to set app-specific scroll tracking variables
    private fun setAppScrollStartTime(appName: String, time: Long?) {
        when (appName) {
            "Facebook" -> facebookScrollStartTime = time
            "YouTube" -> youtubeScrollStartTime = time
            "Instagram" -> instagramScrollStartTime = time
            "Threads" -> threadsScrollStartTime = time
            "Pinterest" -> pinterestScrollStartTime = time
        }
    }
    
    private fun setAppLastScrollTime(appName: String, time: Long?) {
        when (appName) {
            "Facebook" -> lastFacebookScrollTime = time
            "YouTube" -> lastYoutubeScrollTime = time
            "Instagram" -> lastInstagramScrollTime = time
            "Threads" -> lastThreadsScrollTime = time
            "Pinterest" -> lastPinterestScrollTime = time
        }
    }
    
    private fun setAppScrollLimiterShown(appName: String, shown: Boolean) {
        when (appName) {
            "Facebook" -> facebookScrollLimiterShown = shown
            "YouTube" -> youtubeScrollLimiterShown = shown
            "Instagram" -> instagramScrollLimiterShown = shown
            "Threads" -> threadsScrollLimiterShown = shown
            "Pinterest" -> pinterestScrollLimiterShown = shown
        }
    }
    
    private fun setAppLastTriggerTime(appName: String, time: Long) {
        when (appName) {
            "Facebook" -> lastFacebookScrollLimiterTriggerTime = time
            "YouTube" -> lastYoutubeScrollLimiterTriggerTime = time
            "Instagram" -> lastInstagramScrollLimiterTriggerTime = time
            "Threads" -> lastThreadsScrollLimiterTriggerTime = time
            "Pinterest" -> lastPinterestScrollLimiterTriggerTime = time
        }
    }
    
    private fun setAppLastProgressSecond(appName: String, second: Int) {
        when (appName) {
            "Facebook" -> lastFacebookProgressNotificationSecond = second
            "YouTube" -> lastYoutubeProgressNotificationSecond = second
            "Instagram" -> lastInstagramProgressNotificationSecond = second
            "Threads" -> lastThreadsProgressNotificationSecond = second
            "Pinterest" -> lastPinterestProgressNotificationSecond = second
        }
    }
    
    private fun setAppBreakEndTime(appName: String, time: Long?) {
        when (appName) {
            "Facebook" -> facebookMandatoryBreakEndTime = time
            "YouTube" -> youtubeMandatoryBreakEndTime = time
            "Instagram" -> instagramMandatoryBreakEndTime = time
            "Threads" -> threadsMandatoryBreakEndTime = time
            "Pinterest" -> pinterestMandatoryBreakEndTime = time
        }
    }
    
    private fun resetAppScrollTracking(appName: String) {
        setAppScrollStartTime(appName, null)
        setAppScrollLimiterShown(appName, false)
        setAppLastProgressSecond(appName, 0)
    }
    
    private fun showScrollLimiterOverlay(appName: String, remainingSeconds: Int) {
        try {
            val intent = android.content.Intent(
                this, 
                com.example.socialsentry.presentation.ui.overlay.ScrollLimiterOverlayActivity::class.java
            )
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("APP_NAME", appName)
            intent.putExtra("REMAINING_SECONDS", remainingSeconds)
            
            Log.d(TAG, "Scroll limiter overlay activity started for $appName with ${remainingSeconds}s remaining")
            
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show scroll limiter overlay", e)
        }
    }

    // Utility Methods
    private fun detectVerticalVideoContent(root: AccessibilityNodeInfo, platform: String): Boolean {
        return searchForVerticalVideo(root, 0, platform)
    }

    private fun searchForVerticalVideo(node: AccessibilityNodeInfo, depth: Int, platform: String): Boolean {
        if (depth > 4) return false // Limit recursion depth
        
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        val isVertical = bounds.height() > bounds.width()
        val isLarge = bounds.width() > 300 && bounds.height() > 500
        
        if (isVertical && isLarge) {
            val className = node.className?.toString()?.lowercase() ?: ""
            val viewId = node.viewIdResourceName?.lowercase() ?: ""
            
            val videoKeywords = listOf("video", "surface", "media", "player", "image")
            val combined = "$className $viewId"
            
            if (videoKeywords.any { combined.contains(it) }) {
                Log.d(TAG, "$platform vertical video detected: ${bounds.width()}x${bounds.height()}")
                return true
            }
        }
        
        // Check children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                if (searchForVerticalVideo(child, depth + 1, platform)) {
                    return true
                }
            }
        }
        
        return false
    }

    private fun findTextInNode(root: AccessibilityNodeInfo, searchTexts: List<String>): Boolean {
        return searchTexts.any { searchText ->
            root.findAccessibilityNodeInfosByText(searchText).isNotEmpty()
        }
    }

    private fun safePerformAction(node: AccessibilityNodeInfo?, action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastBlockTime < blockDebounceMs) return
        
        lastBlockTime = now
        try {
            action()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to perform action", e)
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun getCurrentMinuteOfDay(): Int {
        val now = System.currentTimeMillis()
        val offset = TimeZone.getDefault().getOffset(now)
        return (((now + offset) / 60000) % 1440).toInt()
    }

    private fun isWithinTimeRange(startMinute: Int, endMinute: Int, currentMinute: Int): Boolean {
        return if (startMinute <= endMinute) {
            currentMinute in startMinute..endMinute
        } else {
            // Overnight range
            currentMinute >= startMinute || currentMinute <= endMinute
        }
    }
    
    // V9-style debounced action execution
    private fun exitTheDoom(
        node: AccessibilityNodeInfo?,
        extra: (() -> Unit)? = null
    ) {
        val now = System.currentTimeMillis()
        if (now - lastBlockTime < blockDebounceMs) return
        lastBlockTime = now

        if (node != null) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
        extra?.invoke()
    }

    // YouTube Usage Tracking
    private fun trackYouTubeUsage(root: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()
        
        // Cooldown to prevent duplicate tracking calls
        if (now - lastYouTubeTrackingTime < youtubeTrackingCooldownMs) {
            return
        }
        lastYouTubeTrackingTime = now
        
        Log.d(TAG, "YouTube tracking: Starting video extraction")
        
        serviceScope.launch {
            try {
                // Extract video title and channel name from YouTube UI
                val (title, channel) = extractYouTubeVideoInfo(root)
                
                if (title == null) {
                    Log.v(TAG, "YouTube tracking: No video title detected")
                    // No video detected, but don't end session immediately - wait a bit
                    val timeSinceLastVideo = now - lastVideoDetectionTime
                    
                    // If we have an active session, try to maintain it even if title detection fails
                    if (currentYouTubeSession != null) {
                        Log.v(TAG, "YouTube tracking: Maintaining existing session despite title detection failure")
                        // Update session progress without ending it
                        currentYouTubeSession?.let { session ->
                            Log.v(TAG, "YouTube tracking: Session in progress - '${session.title}' (${(now - session.startTime) / 1000}s)")
                        }
                        return@launch
                    }
                    
                    // Only end session if we've been without video detection for too long
                    if (timeSinceLastVideo > youtubeSessionDebounceMs) {
                        endCurrentYouTubeSession()
                    }
                    return@launch
                }
                
                Log.d(TAG, "YouTube tracking: Video detected - '$title' by $channel")
                
                // Update last video detection time
                lastVideoDetectionTime = now
                
                // Check if this is a new video (title changed)
                // Normalize titles for comparison to avoid false "new video" detections
                val normalizedNewTitle = normalizeTitle(title)
                val normalizedLastTitle = normalizeTitle(lastYouTubeTitle)
                
                if (normalizedNewTitle != normalizedLastTitle) {
                    Log.d(TAG, "YouTube tracking: New video detected (previous: '$lastYouTubeTitle', new: '$title')")
                    // End previous session
                    endCurrentYouTubeSession()
                    
                    // Start new session
                    val (category, confidence) = youtubeDataStore.categorizeVideo(title, channel)
                    
                    val newSession = YouTubeSession(
                        title = title,
                        channelName = channel,
                        category = category,
                        startTime = now,
                        confidence = confidence,
                        detectedBy = "accessibility"
                    )
                    
                    currentYouTubeSession = newSession
                    lastYouTubeTitle = title
                    lastYouTubeChannel = channel
                    youtubeSessionStartTime = now
                    
                    Log.d(TAG, "YouTube tracking: New video started - '$title' by $channel (Category: $category)")
                } else {
                    // Same video, just keep the session alive - don't end it
                    currentYouTubeSession?.let { session ->
                        Log.v(TAG, "YouTube tracking: Continuing session for '$title'")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error tracking YouTube usage", e)
            }
        }
    }
    
    /**
     * Normalize video title for comparison to avoid false "new video" detections
     * Removes extra spaces, punctuation variations, and normalizes Unicode characters
     */
    private fun normalizeTitle(title: String?): String {
        if (title == null) return ""
        
        return title
            .trim()
            .replace(Regex("\\s+"), " ") // Replace multiple spaces with single space
            .replace(Regex("[,،、]+"), ",") // Normalize different comma types
            .replace(Regex("[|｜]+"), "|") // Normalize different pipe characters
            .replace(Regex("[\"\"\"'']+"), "") // Remove various quote types
            .lowercase() // Convert to lowercase for case-insensitive comparison
    }
    
    private fun extractYouTubeVideoInfo(root: AccessibilityNodeInfo): Pair<String?, String?> {
        var title: String? = null
        var channel: String? = null
        
        try {
            Log.d(TAG, "YouTube tracking: Extracting video info from accessibility tree")
            
            // Method 1: Try to find title using common YouTube view IDs
            val titleIds = listOf(
                "com.google.android.youtube:id/title",
                "com.google.android.youtube:id/video_title",
                "com.google.android.youtube:id/player_title",
                "com.google.android.youtube:id/primary_text",
                "com.google.android.youtube:id/secondary_text"
            )
            
            for (id in titleIds) {
                val titleNode = root.findAccessibilityNodeInfosByViewId(id).firstOrNull()
                if (titleNode?.text != null) {
                    title = titleNode.text.toString()
                    Log.d(TAG, "YouTube tracking: Found title via ID $id: '$title'")
                    break
                }
            }
            
            // Method 2: If not found, search for text nodes that look like titles
            if (title == null) {
                title = findYouTubeTitleByText(root)
                if (title != null) {
                    Log.d(TAG, "YouTube tracking: Found title via text search: '$title'")
                }
            }
            
            // Find channel name
            val channelIds = listOf(
                "com.google.android.youtube:id/channel_name",
                "com.google.android.youtube:id/owner_name",
                "com.google.android.youtube:id/channel"
            )
            
            for (id in channelIds) {
                val channelNode = root.findAccessibilityNodeInfosByViewId(id).firstOrNull()
                if (channelNode?.text != null) {
                    channel = channelNode.text.toString()
                    Log.d(TAG, "YouTube tracking: Found channel via ID $id: '$channel'")
                    break
                }
            }
            
            if (title == null) {
                Log.w(TAG, "YouTube tracking: No video title found")
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting YouTube video info", e)
        }
        
        return Pair(title, channel)
    }
    
    private fun findYouTubeTitleByText(node: AccessibilityNodeInfo, depth: Int = 0): String? {
        if (depth > 10) return null // Increased recursion limit for better coverage
        
        // Look for text nodes that are likely video titles
        val text = node.text?.toString()
        if (!text.isNullOrBlank() && text.length > 5) { // Reduced minimum length to catch more titles
            Log.d(TAG, "YouTube tracking: Found text node: '$text' (length: ${text.length})")
            // Skip common UI elements
            val skipTexts = listOf(
                "Subscribe", "Like", "Share", "Download", "Remix", 
                "Comments", "views", "ago", "Show more", "Show less",
                "Play next in queue", "Visit advertiser", "Saved to",
                "Watch later", "Take a Break!", "Play next", "Sponsored",
                "Install", "FREE", "MyBL", "My Banglalink", "Install",
                "Install", "Install", "Install", "Install", "Install",
                "From your subscriptions", "From Redwan Hushen", "From the series",
                "View key concept", "Tap to correct category", "Recent Videos",
                "K views", "M views", "B views", "subscribers", "Subscribe",
                "months", "days", "hours", "minutes", "seconds", "years"
            )
            
            val shouldSkip = skipTexts.any { text.contains(it, ignoreCase = true) }
            if (!shouldSkip) {
                // Check if this looks like a real video title (contains study keywords or is long enough)
                val isStudyContent = text.contains("ফিজিক্স", ignoreCase = true) || 
                                   text.contains("physics", ignoreCase = true) ||
                                   text.contains("HSC", ignoreCase = true) ||
                                   text.contains("SSC", ignoreCase = true) ||
                                   text.contains("পর্ব", ignoreCase = true) ||
                                   text.contains("ক্লাস", ignoreCase = true) ||
                                   text.contains("অধ্যায়", ignoreCase = true) ||
                                   text.contains("Chapter", ignoreCase = true) ||
                                   text.contains("Lecture", ignoreCase = true) ||
                                   text.contains("Class", ignoreCase = true) ||
                                   text.contains("Study", ignoreCase = true) ||
                                   text.contains("Tutorial", ignoreCase = true) ||
                                   text.contains("Lesson", ignoreCase = true) ||
                                   text.contains("Course", ignoreCase = true) ||
                                   text.contains("Exam", ignoreCase = true) ||
                                   text.contains("Test", ignoreCase = true) ||
                                   text.contains("MCQ", ignoreCase = true) ||
                                   text.contains("CQ", ignoreCase = true) ||
                                   text.contains("One Shot", ignoreCase = true) ||
                                   text.contains("Basic", ignoreCase = true) ||
                                   text.contains("Pro", ignoreCase = true) ||
                                   text.contains("ICT", ignoreCase = true) ||
                                   text.contains("Math", ignoreCase = true) ||
                                   text.contains("Chemistry", ignoreCase = true) ||
                                   text.contains("Biology", ignoreCase = true) ||
                                   text.contains("English", ignoreCase = true) ||
                                   text.contains("Bangla", ignoreCase = true) ||
                                   text.contains("বাংলা", ignoreCase = true) ||
                                   text.contains("পদার্থবিজ্ঞান", ignoreCase = true) ||
                                   text.contains("রসায়ন", ignoreCase = true) ||
                                   text.contains("জীববিজ্ঞান", ignoreCase = true) ||
                                   text.contains("গণিত", ignoreCase = true) ||
                                   text.contains("ইংরেজি", ignoreCase = true) ||
                                   text.contains("কাজ ক্ষমতা", ignoreCase = true) ||
                                   text.contains("শক্তি", ignoreCase = true) ||
                                   text.contains("সংখ্যা পদ্ধতি", ignoreCase = true) ||
                                   text.length > 20 // Longer titles are likely real video titles
                
                if (isStudyContent) {
                    val className = node.className?.toString()?.lowercase() ?: ""
                    if (className.contains("textview") || className.contains("text") || 
                        className.contains("view") || className.contains("layout")) {
                        Log.d(TAG, "YouTube tracking: Study video title found: '$text'")
                        return text
                    }
                }
            }
        }
        
        // Recursively check children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val foundTitle = findYouTubeTitleByText(child, depth + 1)
                if (foundTitle != null) return foundTitle
            }
        }
        
        return null
    }
    
    private suspend fun endCurrentYouTubeSession() {
        currentYouTubeSession?.let { session ->
            val now = System.currentTimeMillis()
            val duration = now - session.startTime
            
            // Only save sessions that are at least 3 seconds long to filter out accidental taps
            // Increased from 2 seconds to reduce noise
            if (duration >= 3000) {
                val completedSession = session.copy(
                    endTime = now,
                    durationMs = duration
                )
                
                youtubeDataStore.addSession(completedSession)
                Log.d(TAG, "YouTube tracking: Session ended - '${session.title}' (${duration / 1000}s, Category: ${session.category})")
            } else {
                Log.v(TAG, "YouTube tracking: Session too short, not saving (${duration / 1000}s)")
            }
            
            currentYouTubeSession = null
            lastYouTubeTitle = null
            lastYouTubeChannel = null
            youtubeSessionStartTime = null
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "SocialSentry Accessibility Service interrupted")
        // End YouTube session on interrupt
        serviceScope.launch {
            endCurrentYouTubeSession()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // End YouTube session on service destroy
        serviceScope.launch {
            endCurrentYouTubeSession()
        }
        serviceScope.cancel()
        Log.d(TAG, "SocialSentry Accessibility Service destroyed")
    }
}
