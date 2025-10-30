package com.example.socialsentry.manager

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import com.example.socialsentry.data.datastore.YouTubeTrackingDataStore
import com.example.socialsentry.data.model.AppCategory
import com.example.socialsentry.data.model.AppCategorizer
import com.example.socialsentry.data.model.AppUsageSession
import com.example.socialsentry.data.model.AppUsageStats
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class AppUsageManager(
    private val context: Context,
    private val youtubeDataStore: YouTubeTrackingDataStore
) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager
    
    companion object {
        private const val TAG = "AppUsageManager"
    }
    
    /**
     * Check if usage stats permission is granted
     */
    fun hasUsageStatsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } else {
            true
        }
    }
    
    /**
     * Open usage stats settings to grant permission
     */
    fun openUsageStatsSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    
    /**
     * Get today's app usage statistics
     */
    suspend fun getTodayUsageStats(): AppUsageStats {
        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "Usage stats permission not granted")
            return AppUsageStats(date = getTodayDateString())
        }
        
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        return getUsageStats(startTime, endTime)
    }
    
    /**
     * Get usage statistics for a time range
     */
    private suspend fun getUsageStats(startTime: Long, endTime: Long): AppUsageStats {
        // Use INTERVAL_BEST to get more accurate data that matches Digital Wellbeing
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            startTime,
            endTime
        ) ?: emptyList()
        
        Log.d(TAG, "Retrieved ${usageStats.size} usage stats from $startTime to $endTime")
        
        val sessions = mutableListOf<AppUsageSession>()
        val categoryMap = mutableMapOf<AppCategory, Long>()
        val appMap = mutableMapOf<String, Long>()
        var totalScreenTime = 0L
        var youtubeTotal = 0L
        
        // Process each app's usage
        for (stats in usageStats) {
            if (stats.totalTimeInForeground <= 0) continue
            
            val packageName = stats.packageName
            val appName = getAppName(packageName)
            val duration = stats.totalTimeInForeground
            
            Log.d(TAG, "Processing app: $packageName ($appName) - Duration: ${duration}ms")
            
            // Include our own app in the statistics to match Digital Wellbeing
            // if (packageName == context.packageName) continue
            
            // Only filter out truly system apps that Digital Wellbeing also excludes
            if (AppCategorizer.isSystemApp(packageName, appName)) {
                Log.d(TAG, "Filtering out system app: $packageName ($appName)")
                continue
            }
            
            // Track YouTube separately
            if (packageName == "com.google.android.youtube") {
                youtubeTotal = duration
                // Don't add to category yet, we'll handle YouTube separately
                continue
            }
            
            val category = AppCategorizer.categorizeApp(packageName, appName)
            
            // Add to category breakdown
            categoryMap[category] = (categoryMap[category] ?: 0L) + duration
            
            // Add to top apps
            appMap[packageName] = duration
            
            // Add session
            sessions.add(
                AppUsageSession(
                    packageName = packageName,
                    appName = appName,
                    category = category,
                    startTime = stats.firstTimeStamp,
                    endTime = stats.lastTimeStamp,
                    durationMs = duration
                )
            )
            
            totalScreenTime += duration
        }
        
        // Get YouTube study time from YouTubeTrackingDataStore
        val youtubeStats = youtubeDataStore.statsFlow.first()
        val youtubeStudy = youtubeStats.categoryBreakdown[com.example.socialsentry.data.model.VideoCategory.STUDY] ?: 0L
        
        // Calculate YouTube entertainment time (total - study)
        val youtubeEntertainment = maxOf(0L, youtubeTotal - youtubeStudy)
        
        // Add YouTube study time to STUDY category
        if (youtubeStudy > 0) {
            categoryMap[AppCategory.STUDY] = (categoryMap[AppCategory.STUDY] ?: 0L) + youtubeStudy
            totalScreenTime += youtubeStudy
        }
        
        // Add YouTube entertainment time to ENTERTAINMENT category
        if (youtubeEntertainment > 0) {
            categoryMap[AppCategory.ENTERTAINMENT] = (categoryMap[AppCategory.ENTERTAINMENT] ?: 0L) + youtubeEntertainment
            totalScreenTime += youtubeEntertainment
        }
        
        // Add YouTube to top apps (with total time)
        if (youtubeTotal > 0) {
            appMap["com.google.android.youtube"] = youtubeTotal
        }
        
        // Sort top apps by duration
        val topApps = appMap.toList()
            .sortedByDescending { (_, duration) -> duration }
            .take(10)
            .toMap()
        
        // Debug logging to compare with Digital Wellbeing
        Log.d(TAG, "=== USAGE STATS SUMMARY ===")
        Log.d(TAG, "Total Screen Time: ${formatDuration(totalScreenTime)}")
        Log.d(TAG, "YouTube Total: ${formatDuration(youtubeTotal)}")
        Log.d(TAG, "YouTube Study: ${formatDuration(youtubeStudy)}")
        Log.d(TAG, "YouTube Entertainment: ${formatDuration(youtubeEntertainment)}")
        Log.d(TAG, "Category Breakdown:")
        categoryMap.forEach { (category, duration) ->
            Log.d(TAG, "  $category: ${formatDuration(duration)}")
        }
        Log.d(TAG, "Top Apps:")
        topApps.forEach { (packageName, duration) ->
            val appName = getAppName(packageName)
            Log.d(TAG, "  $appName ($packageName): ${formatDuration(duration)}")
        }
        Log.d(TAG, "=== END SUMMARY ===")
        
        return AppUsageStats(
            date = getTodayDateString(),
            totalScreenTime = totalScreenTime,
            categoryBreakdown = categoryMap,
            topApps = topApps,
            sessions = sessions.sortedByDescending { it.durationMs },
            youtubeTotal = youtubeTotal,
            youtubeStudy = youtubeStudy,
            youtubeEntertainment = youtubeEntertainment
        )
    }
    
    /**
     * Get app name from package name
     */
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
    
    /**
     * Get today's date as string (YYYY-MM-DD)
     */
    private fun getTodayDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return dateFormat.format(Date())
    }
    
    /**
     * Get raw usage stats for debugging comparison with Digital Wellbeing
     */
    fun getRawUsageStatsForDebugging(): List<UsageStats> {
        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "Usage stats permission not granted")
            return emptyList()
        }
        
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            startTime,
            endTime
        ) ?: emptyList()
        
        Log.d(TAG, "=== RAW USAGE STATS FOR DEBUGGING ===")
        Log.d(TAG, "Time range: ${startTime} to ${endTime}")
        Log.d(TAG, "Total apps found: ${usageStats.size}")
        
        var totalRawTime = 0L
        usageStats.forEach { stats ->
            if (stats.totalTimeInForeground > 0) {
                val appName = getAppName(stats.packageName)
                val duration = stats.totalTimeInForeground
                totalRawTime += duration
                Log.d(TAG, "RAW: ${stats.packageName} ($appName) - ${formatDuration(duration)}")
            }
        }
        
        Log.d(TAG, "Total raw screen time: ${formatDuration(totalRawTime)}")
        Log.d(TAG, "=== END RAW STATS ===")
        
        return usageStats
    }
    
    /**
     * Format duration in milliseconds to readable string
     */
    fun formatDuration(durationMs: Long): String {
        val hours = durationMs / (1000 * 60 * 60)
        val minutes = (durationMs % (1000 * 60 * 60)) / (1000 * 60)
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }
}

