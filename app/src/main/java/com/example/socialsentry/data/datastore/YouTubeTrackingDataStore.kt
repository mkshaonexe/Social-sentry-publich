package com.example.socialsentry.data.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.example.socialsentry.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Context.youtubeTrackingDataStore: DataStore<YouTubeStats> by dataStore(
    fileName = "youtube_tracking.json",
    serializer = YouTubeStatsSerializer
)

private val Context.youtubeSettingsDataStore: DataStore<YouTubeTrackingSettings> by dataStore(
    fileName = "youtube_settings.json",
    serializer = YouTubeSettingsSerializer
)

object YouTubeStatsSerializer : Serializer<YouTubeStats> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val defaultValue: YouTubeStats
        get() = YouTubeStats()

    override suspend fun readFrom(input: InputStream): YouTubeStats {
        return try {
            json.decodeFromString(
                YouTubeStats.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: YouTubeStats, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                json.encodeToString(YouTubeStats.serializer(), t)
                    .encodeToByteArray()
            )
        }
    }
}

object YouTubeSettingsSerializer : Serializer<YouTubeTrackingSettings> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val defaultValue: YouTubeTrackingSettings
        get() = YouTubeTrackingSettings()

    override suspend fun readFrom(input: InputStream): YouTubeTrackingSettings {
        return try {
            json.decodeFromString(
                YouTubeTrackingSettings.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: YouTubeTrackingSettings, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                json.encodeToString(YouTubeTrackingSettings.serializer(), t)
                    .encodeToByteArray()
            )
        }
    }
}

class YouTubeTrackingDataStore(private val context: Context) {
    val statsFlow: Flow<YouTubeStats> = context.youtubeTrackingDataStore.data
    val settingsFlow: Flow<YouTubeTrackingSettings> = context.youtubeSettingsDataStore.data

    suspend fun addSession(session: YouTubeSession) {
        context.youtubeTrackingDataStore.updateData { currentStats ->
            val updatedSessions = currentStats.sessions.toMutableList()
            
            // Normalize title for comparison
            val normalizedNewTitle = normalizeTitle(session.title)
            
            // Check if we need to merge with a recent session (within 30 seconds)
            val recentSessionIndex = updatedSessions.indexOfLast { existingSession ->
                val normalizedExistingTitle = normalizeTitle(existingSession.title)
                val timeDiff = session.startTime - (existingSession.endTime ?: existingSession.startTime)
                
                // Same video within 30 seconds = likely continuation of same viewing session
                normalizedExistingTitle == normalizedNewTitle && 
                timeDiff >= 0 && 
                timeDiff <= 30000 // 30 seconds
            }
            
            if (recentSessionIndex != -1) {
                // Merge with existing session - extend the duration
                val existingSession = updatedSessions[recentSessionIndex]
                val mergedSession = existingSession.copy(
                    endTime = session.endTime ?: session.startTime,
                    durationMs = (session.endTime ?: session.startTime) - existingSession.startTime
                )
                updatedSessions[recentSessionIndex] = mergedSession
                Log.d("YouTubeTracking", "Merged session: '${session.title}' - extended to ${mergedSession.durationMs / 1000}s")
            } else {
                // Check if we need to end a previous session with the same title
                val existingIndex = updatedSessions.indexOfLast { 
                    normalizeTitle(it.title) == normalizedNewTitle && it.endTime == null 
                }
                
                if (existingIndex != -1 && session.endTime != null) {
                    // Update existing session with end time
                    updatedSessions[existingIndex] = updatedSessions[existingIndex].copy(
                        endTime = session.endTime,
                        durationMs = session.endTime - updatedSessions[existingIndex].startTime
                    )
                } else {
                    // Add new session
                    updatedSessions.add(session)
                }
            }
            
            // Recalculate stats
            recalculateStats(updatedSessions)
        }
    }
    
    /**
     * Normalize title for comparison to avoid duplicates
     */
    private fun normalizeTitle(title: String): String {
        return title
            .trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[,،、]+"), ",")
            .replace(Regex("[|｜]+"), "|")
            .replace(Regex("[\"\"\"'']+"), "")
            .lowercase()
    }

    suspend fun endSession(title: String, endTime: Long) {
        context.youtubeTrackingDataStore.updateData { currentStats ->
            val updatedSessions = currentStats.sessions.map { session ->
                if (session.title == title && session.endTime == null) {
                    session.copy(
                        endTime = endTime,
                        durationMs = endTime - session.startTime
                    )
                } else {
                    session
                }
            }
            
            recalculateStats(updatedSessions)
        }
    }

    suspend fun updateSessionCategory(sessionId: String, category: VideoCategory) {
        context.youtubeTrackingDataStore.updateData { currentStats ->
            val updatedSessions = currentStats.sessions.map { session ->
                if (session.id == sessionId) {
                    session.copy(category = category, confidence = 1f)
                } else {
                    session
                }
            }
            
            recalculateStats(updatedSessions)
        }
    }

    suspend fun saveUserCorrection(title: String, category: VideoCategory) {
        context.youtubeSettingsDataStore.updateData { settings ->
            settings.copy(
                userCorrectedVideos = settings.userCorrectedVideos + (title to category)
            )
        }
    }

    suspend fun addCustomKeyword(keyword: CategoryKeyword) {
        context.youtubeSettingsDataStore.updateData { settings ->
            settings.copy(
                keywords = settings.keywords + keyword
            )
        }
    }

    suspend fun getTodaysSessions(): List<YouTubeSession> {
        val stats = context.youtubeTrackingDataStore.data.first()
        val todayStart = LocalDate.now().atStartOfDay()
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        return stats.sessions.filter { it.startTime >= todayStart }
    }

    suspend fun resetDailyStats() {
        context.youtubeTrackingDataStore.updateData {
            YouTubeStats()
        }
        context.youtubeSettingsDataStore.updateData { settings ->
            settings.copy(
                lastResetDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
        }
    }

    suspend fun checkAndResetIfNewDay() {
        val settings = context.youtubeSettingsDataStore.data.first()
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        if (settings.lastResetDate != today) {
            resetDailyStats()
        }
    }

    private fun recalculateStats(sessions: List<YouTubeSession>): YouTubeStats {
        val categoryBreakdown = mutableMapOf<VideoCategory, Long>()
        val channelBreakdown = mutableMapOf<String, Long>()
        var totalTime = 0L

        sessions.forEach { session ->
            val duration = session.durationMs
            if (duration > 0) {
                totalTime += duration
                categoryBreakdown[session.category] = 
                    (categoryBreakdown[session.category] ?: 0L) + duration
                
                session.channelName?.let { channel ->
                    channelBreakdown[channel] = 
                        (channelBreakdown[channel] ?: 0L) + duration
                }
            }
        }

        // Get top 10 channels by watch time
        val topChannels = channelBreakdown.entries
            .sortedByDescending { it.value }
            .take(10)
            .associate { it.key to it.value }

        return YouTubeStats(
            sessions = sessions,
            totalWatchTimeMs = totalTime,
            categoryBreakdown = categoryBreakdown,
            topChannels = topChannels,
            lastUpdated = System.currentTimeMillis()
        )
    }

    suspend fun categorizeVideo(
        title: String,
        channelName: String?
    ): Pair<VideoCategory, Float> {
        val settings = context.youtubeSettingsDataStore.data.first()
        
        // Check if user has manually corrected this video before
        settings.userCorrectedVideos[title]?.let {
            return it to 1f
        }
        
        val normalizedTitle = title.lowercase()
        val normalizedChannel = channelName?.lowercase()
        
        // Check channel-based keywords first
        normalizedChannel?.let { channel ->
            settings.keywords.filter { it.isChannelName }.forEach { keyword ->
                if (channel.contains(keyword.keyword.lowercase())) {
                    return keyword.category to keyword.weight
                }
            }
        }
        
        // Check title-based keywords
        val matches = mutableMapOf<VideoCategory, Float>()
        settings.keywords.filter { !it.isChannelName }.forEach { keyword ->
            if (normalizedTitle.contains(keyword.keyword.lowercase())) {
                matches[keyword.category] = 
                    (matches[keyword.category] ?: 0f) + keyword.weight
            }
        }
        
        // Return category with highest weight, or OTHER if no matches
        return if (matches.isEmpty()) {
            VideoCategory.OTHER to 0f
        } else {
            val best = matches.maxByOrNull { it.value }!!
            best.key to (best.value / settings.keywords.size).coerceIn(0f, 1f)
        }
    }
}

