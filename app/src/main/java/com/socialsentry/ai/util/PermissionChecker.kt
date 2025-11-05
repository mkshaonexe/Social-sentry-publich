package com.socialsentry.ai.util

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.provider.Settings

/**
 * Data class to hold permission status
 */
data class PermissionStatus(
    val isAccessibilityEnabled: Boolean = false,
    val isUsageStatsEnabled: Boolean = false,
    val isOverlayEnabled: Boolean = false
) {
    val hasAllPermissions: Boolean
        get() = isAccessibilityEnabled
    
    val missingPermissions: List<String>
        get() = buildList {
            if (!isAccessibilityEnabled) add("Accessibility Service")
        }
    
    val missingCount: Int
        get() = missingPermissions.size
}

/**
 * Utility class to check various app permissions
 */
object PermissionChecker {
    
    /**
     * Check if accessibility service is enabled
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedServiceName = "${context.packageName}/.accessibility.SocialSentryAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        // Check if accessibility is enabled at all
        val accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        ) == 1
        
        if (!accessibilityEnabled) return false
        
        // Check if our specific service is enabled
        return enabledServices.split(':').any { service ->
            service.equals(expectedServiceName, ignoreCase = true) ||
            service.contains("SocialSentryAccessibilityService", ignoreCase = true)
        }
    }
    
    /**
     * Check if usage stats permission is granted
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if overlay permission is granted
     */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }
    
    /**
     * Get complete permission status (simplified for version 0.1.2)
     */
    fun getPermissionStatus(context: Context): PermissionStatus {
        return PermissionStatus(
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context),
            isUsageStatsEnabled = true, // Not required for version 0.1.2
            isOverlayEnabled = true // Not required for version 0.1.2
        )
    }
}

