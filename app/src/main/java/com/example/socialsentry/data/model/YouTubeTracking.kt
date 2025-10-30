package com.example.socialsentry.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class VideoCategory {
    STUDY,           // Educational content, lectures, tutorials
    ENTERTAINMENT,   // Music, movies, comedy, vlogs
    GAMING,          // Gaming content, walkthroughs
    PRODUCTIVE,      // How-to, career, coding (not study)
    OTHER           // Unknown or uncategorized
}

@Serializable
data class YouTubeSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val channelName: String? = null,
    val category: VideoCategory = VideoCategory.OTHER,
    val startTime: Long,        // Epoch millis
    val endTime: Long? = null,  // Epoch millis (null if still playing)
    val durationMs: Long = 0L,  // Calculated duration
    val confidence: Float = 0f,  // 0.0 to 1.0 - how confident we are in categorization
    val detectedBy: String = "accessibility" // "accessibility", "media_session", or "usage_stats"
)

@Serializable
data class YouTubeStats(
    val sessions: List<YouTubeSession> = emptyList(),
    val totalWatchTimeMs: Long = 0L,
    val categoryBreakdown: Map<VideoCategory, Long> = emptyMap(), // category -> total time in ms
    val topChannels: Map<String, Long> = emptyMap(), // channel name -> total time in ms
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class CategoryKeyword(
    val keyword: String,
    val category: VideoCategory,
    val weight: Float = 1f,
    val isChannelName: Boolean = false
)

// Storage for user corrections and learned patterns
@Serializable
data class YouTubeTrackingSettings(
    val isEnabled: Boolean = true,
    val keywords: List<CategoryKeyword> = getDefaultKeywords(),
    val userCorrectedVideos: Map<String, VideoCategory> = emptyMap(), // title -> category
    val lastResetDate: String? = null // ISO-8601 date for daily stats reset
)

// Default keyword list for categorization
fun getDefaultKeywords(): List<CategoryKeyword> = listOf(
    // Study keywords
    CategoryKeyword("lecture", VideoCategory.STUDY, 1f),
    CategoryKeyword("class", VideoCategory.STUDY, 1f),
    CategoryKeyword("tutorial", VideoCategory.STUDY, 0.8f),
    CategoryKeyword("chapter", VideoCategory.STUDY, 1f),
    CategoryKeyword("solve", VideoCategory.STUDY, 0.9f),
    CategoryKeyword("exam", VideoCategory.STUDY, 1f),
    CategoryKeyword("hsc", VideoCategory.STUDY, 1f),
    CategoryKeyword("ssc", VideoCategory.STUDY, 1f),
    CategoryKeyword("bangla", VideoCategory.STUDY, 0.7f),
    CategoryKeyword("math", VideoCategory.STUDY, 1f),
    CategoryKeyword("physics", VideoCategory.STUDY, 1f),
    CategoryKeyword("chemistry", VideoCategory.STUDY, 1f),
    CategoryKeyword("biology", VideoCategory.STUDY, 1f),
    CategoryKeyword("history", VideoCategory.STUDY, 0.8f),
    CategoryKeyword("notes", VideoCategory.STUDY, 0.8f),
    CategoryKeyword("solution", VideoCategory.STUDY, 0.9f),
    CategoryKeyword("viva", VideoCategory.STUDY, 1f),
    CategoryKeyword("problem", VideoCategory.STUDY, 0.7f),
    CategoryKeyword("question", VideoCategory.STUDY, 0.7f),
    CategoryKeyword("lesson", VideoCategory.STUDY, 0.9f),
    CategoryKeyword("learn", VideoCategory.STUDY, 0.8f),
    CategoryKeyword("education", VideoCategory.STUDY, 0.9f),
    CategoryKeyword("university", VideoCategory.STUDY, 0.8f),
    CategoryKeyword("college", VideoCategory.STUDY, 0.8f),
    // Additional study keywords
    CategoryKeyword("frb", VideoCategory.STUDY, 1f),
    CategoryKeyword("one", VideoCategory.STUDY, 0.6f),
    CategoryKeyword("short", VideoCategory.STUDY, 0.7f),
    CategoryKeyword("organic", VideoCategory.STUDY, 1f),
    CategoryKeyword("vector", VideoCategory.STUDY, 1f),
    
    // Gaming keywords
    CategoryKeyword("gameplay", VideoCategory.GAMING, 1f),
    CategoryKeyword("walkthrough", VideoCategory.GAMING, 1f),
    CategoryKeyword("let's play", VideoCategory.GAMING, 1f),
    CategoryKeyword("lets play", VideoCategory.GAMING, 1f),
    CategoryKeyword("game", VideoCategory.GAMING, 0.7f),
    CategoryKeyword("overwatch", VideoCategory.GAMING, 1f),
    CategoryKeyword("pubg", VideoCategory.GAMING, 1f),
    CategoryKeyword("freefire", VideoCategory.GAMING, 1f),
    CategoryKeyword("free fire", VideoCategory.GAMING, 1f),
    CategoryKeyword("mobile legends", VideoCategory.GAMING, 1f),
    CategoryKeyword("valorant", VideoCategory.GAMING, 1f),
    CategoryKeyword("minecraft", VideoCategory.GAMING, 1f),
    CategoryKeyword("fortnite", VideoCategory.GAMING, 1f),
    CategoryKeyword("gta", VideoCategory.GAMING, 1f),
    CategoryKeyword("cod", VideoCategory.GAMING, 1f),
    CategoryKeyword("call of duty", VideoCategory.GAMING, 1f),
    CategoryKeyword("gaming", VideoCategory.GAMING, 1f),
    
    // Entertainment keywords
    CategoryKeyword("music", VideoCategory.ENTERTAINMENT, 0.9f),
    CategoryKeyword("song", VideoCategory.ENTERTAINMENT, 0.9f),
    CategoryKeyword("mv", VideoCategory.ENTERTAINMENT, 0.8f),
    CategoryKeyword("official video", VideoCategory.ENTERTAINMENT, 0.9f),
    CategoryKeyword("movie", VideoCategory.ENTERTAINMENT, 1f),
    CategoryKeyword("trailer", VideoCategory.ENTERTAINMENT, 0.9f),
    CategoryKeyword("comedy", VideoCategory.ENTERTAINMENT, 1f),
    CategoryKeyword("funny", VideoCategory.ENTERTAINMENT, 0.9f),
    CategoryKeyword("vlog", VideoCategory.ENTERTAINMENT, 0.9f),
    CategoryKeyword("reaction", VideoCategory.ENTERTAINMENT, 0.8f),
    CategoryKeyword("prank", VideoCategory.ENTERTAINMENT, 1f),
    CategoryKeyword("meme", VideoCategory.ENTERTAINMENT, 1f),
    CategoryKeyword("tiktok", VideoCategory.ENTERTAINMENT, 0.9f),
    CategoryKeyword("shorts", VideoCategory.ENTERTAINMENT, 0.7f),
    CategoryKeyword("dance", VideoCategory.ENTERTAINMENT, 0.8f),
    CategoryKeyword("entertainment", VideoCategory.ENTERTAINMENT, 1f),
    
    // Productive keywords (not study but useful)
    CategoryKeyword("coding", VideoCategory.PRODUCTIVE, 1f),
    CategoryKeyword("how to", VideoCategory.PRODUCTIVE, 0.8f),
    CategoryKeyword("tips", VideoCategory.PRODUCTIVE, 0.7f),
    CategoryKeyword("career", VideoCategory.PRODUCTIVE, 0.9f),
    CategoryKeyword("productivity", VideoCategory.PRODUCTIVE, 1f),
    CategoryKeyword("programming", VideoCategory.PRODUCTIVE, 1f),
    CategoryKeyword("developer", VideoCategory.PRODUCTIVE, 0.8f),
    CategoryKeyword("tech", VideoCategory.PRODUCTIVE, 0.6f),
    CategoryKeyword("review", VideoCategory.PRODUCTIVE, 0.5f),
    CategoryKeyword("guide", VideoCategory.PRODUCTIVE, 0.7f),
    CategoryKeyword("documentary", VideoCategory.PRODUCTIVE, 0.8f),
    CategoryKeyword("news", VideoCategory.PRODUCTIVE, 0.7f)
)

