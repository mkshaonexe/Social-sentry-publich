package com.socialsentry.ai.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.socialsentry.ai.datastore.AppSettings
import com.socialsentry.ai.datastore.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SocialSentryAccessibilityService : AccessibilityService(), KoinComponent {

    private val dataStore: DataStoreManager by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var settings = AppSettings()

    // Debounce: prevent rapid repeated clicks
    private var lastActionTime = 0L
    private val debounceMillis = 500L
    
    // Pending runnable for delayed actions
    private var pendingAction: Runnable? = null
    
    // Ad blocking configuration - can be updated in future versions
    private val enableAdBlocking = false // Set to true to enable ad blocking features
    private val enableAdAutoHide = false // Set to true to enable auto-hide ads feature

    companion object {
        private const val TAG = "SocialSentryA11y"
        private const val VERBOSE_LOGGING = false // Set to true for debugging
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceScope.launch {
            dataStore.appSettingsFlow.collect { latest ->
                settings = latest
                Log.d(TAG, "Settings updated - FB Lite blocked: ${latest.facebookLite.blocked}, Master: ${latest.masterBlocking}")
            }
        }
        Log.d(TAG, "Accessibility service connected - VERBOSE_LOGGING: $VERBOSE_LOGGING")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        val pkg = event.packageName?.toString() ?: return
        
        // Process specific event types to reduce overhead
        val shouldProcessEvent = event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        
        if (!shouldProcessEvent) {
            return
        }

        // Ad blocking/auto-hide features - disabled by default (can be enabled via enableAdBlocking/enableAdAutoHide flags)
        // Implementation removed - add custom logic here for future updates
        if (enableAdBlocking || enableAdAutoHide) {
            handleAdBlocking(pkg)
        }
        
        // Master blocking must be ON for any blocking to occur
        // Individual app settings then control which apps are blocked
        val masterBlocking = settings.masterBlocking

        // Only proceed if master blocking is enabled
        if (!masterBlocking) {
            return
        }

        when (pkg) {
            "com.instagram.android" -> {
                if (settings.instagram.blocked) {
                    handleInstagram()
                }
            }
            "com.google.android.youtube" -> {
                if (settings.youtube.blocked) {
                    handleYouTube()
                }
            }
            "com.zhiliaoapp.musically" -> {
                if (settings.tiktok.blocked) {
                    handleTikTok()
                }
            }
            "com.facebook.katana" -> {
                if (settings.facebook.blocked) {
                    handleFacebook()
                }
            }
            "com.facebook.lite" -> {
                if (VERBOSE_LOGGING) {
                    Log.d(TAG, "Facebook Lite event detected - type: ${event.eventType}, blocked: ${settings.facebookLite.blocked}")
                }
                if (settings.facebookLite.blocked) {
                    handleFacebookLite()
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        handler.removeCallbacksAndMessages(null)
        pendingAction = null
        Log.d(TAG, "Accessibility service destroyed")
    }

    private fun handleInstagram() {
        // Simple placeholder - implement detection logic here
        val now = System.currentTimeMillis()
        if (now - lastActionTime < debounceMillis) return
        
        lastActionTime = now
        
        // Placeholder: If reels/shorts detected, go back
        pendingAction?.let { handler.removeCallbacks(it) }
        
        pendingAction = Runnable {
            try {
                performGlobalAction(GLOBAL_ACTION_BACK)
            } catch (e: Exception) {
                Log.e(TAG, "Error performing Instagram action", e)
            }
        }
        handler.postDelayed(pendingAction!!, 300)
    }

    private fun handleYouTube() {
        // Simple placeholder - implement detection logic here
        val now = System.currentTimeMillis()
        if (now - lastActionTime < debounceMillis) return
        
        lastActionTime = now
        
        // Placeholder: If reels/shorts detected, go back
        pendingAction?.let { handler.removeCallbacks(it) }
        
        pendingAction = Runnable {
            try {
                performGlobalAction(GLOBAL_ACTION_BACK)
            } catch (e: Exception) {
                Log.e(TAG, "Error performing YouTube action", e)
            }
        }
        handler.postDelayed(pendingAction!!, 300)
    }

    private fun handleTikTok() {
        // Simple placeholder - implement detection logic here
        val now = System.currentTimeMillis()
        if (now - lastActionTime < debounceMillis) return
        
        lastActionTime = now
        
        // Placeholder: If reels/shorts detected, go back
        pendingAction?.let { handler.removeCallbacks(it) }
        
        pendingAction = Runnable {
            try {
                performGlobalAction(GLOBAL_ACTION_BACK)
            } catch (e: Exception) {
                Log.e(TAG, "Error performing TikTok action", e)
            }
        }
        handler.postDelayed(pendingAction!!, 300)
    }

    // Ad blocking/auto-hide functionality - placeholder for future implementation
    // This function is intentionally left blank - add custom logic here when needed
    private fun handleAdBlocking(packageName: String) {
        // TODO: Implement ad blocking/auto-hide logic here
        // This is intentionally left blank for future updates
        // Use enableAdBlocking and enableAdAutoHide flags to control behavior
    }

    private fun handleFacebook() {
        // Simple placeholder - implement detection logic here
        val now = System.currentTimeMillis()
        if (now - lastActionTime < debounceMillis) return
        
        lastActionTime = now
        
        // Placeholder: If reels/shorts detected, go back
        pendingAction?.let { handler.removeCallbacks(it) }
        
        pendingAction = Runnable {
            try {
                performGlobalAction(GLOBAL_ACTION_BACK)
            } catch (e: Exception) {
                Log.e(TAG, "Error performing Facebook action", e)
            }
        }
        handler.postDelayed(pendingAction!!, 300)
    }

    private fun handleFacebookLite() {
        // Simple placeholder - implement detection logic here
        val now = System.currentTimeMillis()
        if (now - lastActionTime < debounceMillis) return
        
        lastActionTime = now
        
        // Placeholder: If reels/shorts detected, go back
        pendingAction?.let { handler.removeCallbacks(it) }
        
        pendingAction = Runnable {
            try {
                performGlobalAction(GLOBAL_ACTION_BACK)
            } catch (e: Exception) {
                Log.e(TAG, "Error performing Facebook Lite action", e)
            }
        }
        handler.postDelayed(pendingAction!!, 300)
    }
}

