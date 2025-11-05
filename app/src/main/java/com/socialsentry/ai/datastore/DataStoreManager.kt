package com.socialsentry.ai.datastore

import android.content.Context
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.first

val Context.appSettingsStore by dataStore("app_settings.json", AppSettingsSerializer)

class DataStoreManager(private val context: Context) {

    val appSettingsFlow = context.appSettingsStore.data

    suspend fun updateAppSettings(settings: AppSettings) {
        context.appSettingsStore.updateData { settings }
    }

    suspend fun updateMasterBlocking(blocked: Boolean) {
        context.appSettingsStore.updateData { current ->
            current.copy(masterBlocking = blocked)
        }
    }

    suspend fun updateInstagram(blocked: Boolean) {
        context.appSettingsStore.updateData { current ->
            current.copy(instagram = current.instagram.copy(blocked = blocked))
        }
    }

    suspend fun updateYoutube(blocked: Boolean) {
        context.appSettingsStore.updateData { current ->
            current.copy(youtube = current.youtube.copy(blocked = blocked))
        }
    }

    suspend fun updateTikTok(blocked: Boolean) {
        context.appSettingsStore.updateData { current ->
            current.copy(tiktok = current.tiktok.copy(blocked = blocked))
        }
    }

    suspend fun updateFacebook(blocked: Boolean) {
        context.appSettingsStore.updateData { current ->
            current.copy(facebook = current.facebook.copy(blocked = blocked))
        }
    }

    suspend fun updateFacebookLite(blocked: Boolean) {
        context.appSettingsStore.updateData { current ->
            current.copy(facebookLite = current.facebookLite.copy(blocked = blocked))
        }
    }

    suspend fun updateAutoHideAds(enabled: Boolean) {
        context.appSettingsStore.updateData { current ->
            current.copy(autoHideAds = enabled)
        }
    }

    suspend fun updateDeveloperModeUnlocked(unlocked: Boolean) {
        context.appSettingsStore.updateData { current ->
            current.copy(developerModeUnlocked = unlocked)
        }
    }

    suspend fun getAppSettings(): AppSettings {
        return appSettingsFlow.first()
    }
}

