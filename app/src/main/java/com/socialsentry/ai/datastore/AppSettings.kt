package com.socialsentry.ai.datastore

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val instagram: App = App(
        name = "Instagram",
        blocked = true
    ),
    val youtube: App = App(
        name = "YouTube",
        blocked = true
    ),
    val tiktok: App = App(
        name = "TikTok",
        blocked = true
    ),
    val facebook: App = App(
        name = "Facebook",
        blocked = true
    ),
    val facebookLite: App = App(
        name = "Facebook Lite",
        blocked = true
    ),
    val masterBlocking: Boolean = false,
    val autoHideAds: Boolean = false,  // Auto-hide Facebook ads feature (OFF by default)
    val developerModeUnlocked: Boolean = false  // Developer mode unlock status
)

