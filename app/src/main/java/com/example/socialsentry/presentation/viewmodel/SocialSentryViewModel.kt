package com.example.socialsentry.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialsentry.data.datastore.SocialSentryDataStore
import com.example.socialsentry.data.model.BlockableFeature
import com.example.socialsentry.data.model.SocialApp
import com.example.socialsentry.data.model.SocialSentrySettings
import com.example.socialsentry.data.model.GameDashboard
import com.example.socialsentry.domain.UnblockAllowanceManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SocialSentryViewModel(
    private val dataStore: SocialSentryDataStore,
    private val unblockManager: UnblockAllowanceManager? = null
) : ViewModel() {

    val settings: StateFlow<SocialSentrySettings> = dataStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SocialSentrySettings()
        )

    fun updateInstagram(app: SocialApp) {
        viewModelScope.launch {
            dataStore.updateInstagram(app)
        }
    }

    fun updateYoutube(app: SocialApp) {
        viewModelScope.launch {
            dataStore.updateYoutube(app)
        }
    }

    fun updateTiktok(app: SocialApp) {
        viewModelScope.launch {
            dataStore.updateTiktok(app)
        }
    }

    fun updateFacebook(app: SocialApp) {
        viewModelScope.launch {
            dataStore.updateFacebook(app)
        }
    }

    fun toggleAppBlocking(appName: String, isBlocked: Boolean) {
        val currentSettings = settings.value
        viewModelScope.launch {
            when (appName) {
                "Instagram" -> updateInstagram(currentSettings.instagram.copy(isBlocked = isBlocked))
                "YouTube" -> updateYoutube(currentSettings.youtube.copy(isBlocked = isBlocked))
                "TikTok" -> updateTiktok(currentSettings.tiktok.copy(isBlocked = isBlocked))
                "Facebook" -> updateFacebook(currentSettings.facebook.copy(isBlocked = isBlocked))
            }
        }
    }

    fun toggleFeatureBlocking(appName: String, featureName: String, isEnabled: Boolean) {
        val currentSettings = settings.value
        viewModelScope.launch {
            when (appName) {
                "Instagram" -> {
                    val updatedFeatures = currentSettings.instagram.features.map { feature ->
                        if (feature.name == featureName) feature.copy(isEnabled = isEnabled) else feature
                    }
                    updateInstagram(currentSettings.instagram.copy(features = updatedFeatures))
                }
                "YouTube" -> {
                    val updatedFeatures = currentSettings.youtube.features.map { feature ->
                        if (feature.name == featureName) feature.copy(isEnabled = isEnabled) else feature
                    }
                    updateYoutube(currentSettings.youtube.copy(features = updatedFeatures))
                }
                "Facebook" -> {
                    val updatedFeatures = currentSettings.facebook.features.map { feature ->
                        if (feature.name == featureName) feature.copy(isEnabled = isEnabled) else feature
                    }
                    updateFacebook(currentSettings.facebook.copy(features = updatedFeatures))
                }
            }
        }
    }

    fun updateAppTimeRange(appName: String, startMinute: Int, endMinute: Int) {
        val currentSettings = settings.value
        viewModelScope.launch {
            when (appName) {
                "Instagram" -> {
                    updateInstagram(
                        currentSettings.instagram.copy(
                            blockTimeStart = startMinute,
                            blockTimeEnd = endMinute
                        )
                    )
                }
                "YouTube" -> {
                    updateYoutube(
                        currentSettings.youtube.copy(
                            blockTimeStart = startMinute,
                            blockTimeEnd = endMinute
                        )
                    )
                }
                "TikTok" -> {
                    updateTiktok(
                        currentSettings.tiktok.copy(
                            blockTimeStart = startMinute,
                            blockTimeEnd = endMinute
                        )
                    )
                }
                "Facebook" -> {
                    updateFacebook(
                        currentSettings.facebook.copy(
                            blockTimeStart = startMinute,
                            blockTimeEnd = endMinute
                        )
                    )
                }
            }
        }
    }

    // Temporary unblock API
    fun startTemporaryUnblock(onInsufficientTime: (() -> Unit)? = null) {
        viewModelScope.launch {
            val manager = unblockManager ?: UnblockAllowanceManager(
                context = dataStoreContext(),
                dataStore = dataStore
            )
            val ok = manager.startTemporaryUnblockNow()
            if (!ok) {
                onInsufficientTime?.invoke()
            }
        }
    }

    fun endTemporaryUnblock() {
        viewModelScope.launch {
            android.util.Log.d("SocialSentryViewModel", "endTemporaryUnblock called")
            val manager = unblockManager ?: UnblockAllowanceManager(
                context = dataStoreContext(),
                dataStore = dataStore
            )
            manager.endTemporaryUnblockAndDecrementAllowance()
            android.util.Log.d("SocialSentryViewModel", "endTemporaryUnblock completed")
        }
    }

    fun addManualUnblockMinutes(minutes: Int) {
        viewModelScope.launch {
            android.util.Log.d("SocialSentryViewModel", "addManualUnblockMinutes called with $minutes minutes")
            val manager = unblockManager ?: UnblockAllowanceManager(
                context = dataStoreContext(),
                dataStore = dataStore
            )
            manager.addManualMinutes(minutes)
            android.util.Log.d("SocialSentryViewModel", "addManualUnblockMinutes completed")
        }
    }

    fun addMinutesFromWalking(walkingMinutes: Int) {
        addManualUnblockMinutes(walkingMinutes)
    }

    fun addMinutesFromStudy(studyMinutes: Int) {
        addManualUnblockMinutes(studyMinutes)
    }

    fun rescheduleAllowanceWork() {
        viewModelScope.launch {
            val manager = unblockManager ?: UnblockAllowanceManager(
                context = dataStoreContext(),
                dataStore = dataStore
            )
            manager.rescheduleAll()
        }
    }
    
    fun updateScrollLimiterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = settings.value
            dataStore.updateSettings(currentSettings.copy(scrollLimiterEnabled = enabled))
        }
    }
    
    fun updateScrollLimiterYoutubeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = settings.value
            dataStore.updateSettings(currentSettings.copy(scrollLimiterYoutubeEnabled = enabled))
        }
    }
    
    fun updateScrollLimiterFacebookEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = settings.value
            dataStore.updateSettings(currentSettings.copy(scrollLimiterFacebookEnabled = enabled))
        }
    }
    
    fun updateScrollLimiterInstagramEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = settings.value
            dataStore.updateSettings(currentSettings.copy(scrollLimiterInstagramEnabled = enabled))
        }
    }

    fun updateScrollLimiterThreadsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = settings.value
            dataStore.updateSettings(currentSettings.copy(scrollLimiterThreadsEnabled = enabled))
        }
    }
    
    fun updateScrollLimiterPinterestEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = settings.value
            dataStore.updateSettings(currentSettings.copy(scrollLimiterPinterestEnabled = enabled))
        }
    }

    // Internal utility to write entire settings snapshot (used by settings UI)
    suspend fun updateSettingsDirect(updated: SocialSentrySettings) {
        dataStore.updateSettings(updated)
    }

    fun updateGameDashboard(transform: (GameDashboard) -> GameDashboard) {
        val current = settings.value.gameDashboard
        val updated = transform(current)
        viewModelScope.launch {
            dataStore.updateGameDashboard(updated)
        }
    }

    // Tutorial management methods
    fun updateTutorialStep(step: Int) {
        viewModelScope.launch {
            dataStore.updateTutorialStep(step)
        }
    }
    
    fun completeTutorial() {
        viewModelScope.launch {
            dataStore.completeTutorial()
        }
    }
    
    fun markTutorialShown() {
        viewModelScope.launch {
            dataStore.markTutorialShown()
        }
    }
    
    fun resetTutorial() {
        viewModelScope.launch {
            dataStore.resetTutorial()
        }
    }

    private fun dataStoreContext(): android.content.Context {
        // SocialSentryDataStore already holds androidContext via Koin, but we need a context
        // The simplest approach is to use reflection via the stored DataStore context itself.
        // SocialSentryDataStore currently only exposes update methods; construct via context again.
        // However, to avoid leaks, we can downcast to Application context when possible.
        return try {
            val field = SocialSentryDataStore::class.java.getDeclaredField("context")
            field.isAccessible = true
            val ctx = field.get(dataStore) as android.content.Context
            ctx.applicationContext
        } catch (e: Exception) {
            throw IllegalStateException("Unable to access DataStore context", e)
        }
    }
}

