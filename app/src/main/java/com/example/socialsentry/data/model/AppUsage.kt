package com.example.socialsentry.data.model

import kotlinx.serialization.Serializable

/**
 * App category for smart categorization
 */
enum class AppCategory {
    STUDY,           // Educational apps, study-related
    ENTERTAINMENT,   // YouTube (non-study), games, streaming
    SOCIAL_MEDIA,    // Facebook, Instagram, TikTok, etc.
    PRODUCTIVITY,    // Office, email, calendar, etc.
    COMMUNICATION,   // Messaging, calls
    UTILITIES,       // Settings, file managers, etc.
    OTHER           // Everything else
}

/**
 * Individual app usage session
 */
@Serializable
data class AppUsageSession(
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val startTime: Long,
    val endTime: Long = 0L,
    val durationMs: Long = 0L,
    val isStudyRelated: Boolean = false // For YouTube study sessions
)

/**
 * Daily app usage statistics
 */
@Serializable
data class AppUsageStats(
    val date: String, // Format: YYYY-MM-DD
    val totalScreenTime: Long = 0L, // Total screen time in ms
    val categoryBreakdown: Map<AppCategory, Long> = emptyMap(), // Category -> duration in ms
    val topApps: Map<String, Long> = emptyMap(), // Package name -> duration in ms
    val sessions: List<AppUsageSession> = emptyList(),
    
    // YouTube specific
    val youtubeTotal: Long = 0L, // Total YouTube time
    val youtubeStudy: Long = 0L, // YouTube study time (detected by accessibility)
    val youtubeEntertainment: Long = 0L // YouTube entertainment time (total - study)
)

/**
 * App categorization rules
 */
object AppCategorizer {
    
    // System apps that should be filtered out from user-facing statistics
    // Reduced list to match Digital Wellbeing behavior - only filter truly system apps
    private val systemAppPatterns = setOf(
        // Core system UI and launchers only
        "com.android.systemui", "com.google.android.launcher",
        "com.google.android.apps.nexuslauncher", "com.google.android.apps.pixel.launcher",
        
        // Essential system services that Digital Wellbeing excludes
        "com.android.settings", "com.google.android.apps.wellbeing",
        
        // System frameworks that should never be counted
        "com.android.", "android.", "com.google.android.gms", "com.google.android.gsf",
        "com.google.android.gmscore", "com.google.android.setupwizard",
        
        // System accessibility services
        "com.android.accessibility", "com.google.android.accessibility",
        "com.android.providers.", "com.android.server.", "com.android.bluetooth",
        
        // System notifications and status bar
        "com.android.notification", "com.android.statusbar", "com.android.incallui"
    )
    
    private val categoryRules = mapOf(
        // Study & Education
        AppCategory.STUDY to listOf(
            "edu", "school", "university", "learn", "course", "tutorial",
            "academy", "classroom", "study", "book", "library", "khan",
            "duolingo", "coursera", "udemy", "edx", "skillshare"
        ),
        
        // Social Media
        AppCategory.SOCIAL_MEDIA to listOf(
            "facebook", "instagram", "tiktok", "twitter", "snapchat",
            "reddit", "pinterest", "linkedin", "whatsapp", "messenger",
            "telegram", "discord", "threads", "barcelona"
        ),
        
        // Entertainment (excluding YouTube which is handled separately)
        AppCategory.ENTERTAINMENT to listOf(
            "game", "play", "video", "movie", "music", "spotify",
            "netflix", "prime", "hotstar", "disney", "hulu", "twitch",
            "stream", "player", "media"
        ),
        
        // Productivity
        AppCategory.PRODUCTIVITY to listOf(
            "office", "docs", "sheets", "slides", "notion", "evernote",
            "calendar", "todo", "task", "note", "drive", "dropbox",
            "gmail", "outlook", "mail", "pdf", "scanner"
        ),
        
        // Communication
        AppCategory.COMMUNICATION to listOf(
            "phone", "dialer", "contacts", "sms", "message", "chat",
            "call", "meet", "zoom", "teams", "skype", "hangouts"
        ),
        
        // Utilities
        AppCategory.UTILITIES to listOf(
            "settings", "camera", "gallery", "file",
            "manager", "cleaner", "battery", "clock", "calculator"
        )
    )
    
    /**
     * Check if an app is a system app that should be filtered out
     */
    fun isSystemApp(packageName: String, appName: String): Boolean {
        val searchText = "$packageName $appName".lowercase()
        
        return systemAppPatterns.any { pattern ->
            when {
                pattern.endsWith(".") -> packageName.startsWith(pattern)
                else -> searchText.contains(pattern)
            }
        }
    }
    
    /**
     * Categorize an app based on its package name and app name
     */
    fun categorizeApp(packageName: String, appName: String): AppCategory {
        val searchText = "$packageName $appName".lowercase()
        
        // Check each category's keywords
        for ((category, keywords) in categoryRules) {
            if (keywords.any { keyword -> searchText.contains(keyword) }) {
                return category
            }
        }
        
        return AppCategory.OTHER
    }
}

