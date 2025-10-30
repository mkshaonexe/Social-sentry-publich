package com.example.socialsentry.util

import android.content.Context
import android.provider.Settings

/**
 * Data class to hold permission status
 */
data class PermissionStatus(
    val isAccessibilityEnabled: Boolean = false
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
        val expectedServiceName = "${context.packageName}/.service.SocialSentryAccessibilityService"
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
     * Get complete permission status
     */
    fun getPermissionStatus(context: Context): PermissionStatus {
        return PermissionStatus(
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
        )
    }
}

