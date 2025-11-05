package com.socialsentry.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialsentry.ai.datastore.AppSettings
import com.socialsentry.ai.datastore.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppState(
    val isBlocked: Boolean = false,
    val isUnblocking: Boolean = false,
    val statusMessage: String = ""
)

class SocialSentryViewModel(private val dataStoreManager: DataStoreManager) : ViewModel() {
    
    // UI state for animations
    private val _appState = MutableStateFlow(AppState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    // App settings from DataStore
    val appSettings: StateFlow<AppSettings> =
        dataStoreManager.appSettingsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppSettings()
            )

    init {
        // Sync initial blocking state with DataStore
        viewModelScope.launch {
            dataStoreManager.appSettingsFlow.collect { settings ->
                _appState.value = _appState.value.copy(
                    isBlocked = settings.masterBlocking
                )
            }
        }
    }

    // Master toggle for ALL apps at once
    fun toggleBlockState() {
        viewModelScope.launch {
            val currentSettings = appSettings.value
            val newBlockingState = !currentSettings.masterBlocking

            if (currentSettings.masterBlocking) {
                // Start unblocking
                _appState.value = _appState.value.copy(
                    isBlocked = true,
                    isUnblocking = true,
                    statusMessage = "Starting unblock session..."
                )
                kotlinx.coroutines.delay(2000)
                
                // Update DataStore
                dataStoreManager.updateMasterBlocking(false)
                
                _appState.value = _appState.value.copy(
                    isBlocked = false,
                    isUnblocking = false,
                    statusMessage = ""
                )
            } else {
                // Block immediately
                dataStoreManager.updateMasterBlocking(true)
                _appState.value = _appState.value.copy(
                    isBlocked = true,
                    isUnblocking = false,
                    statusMessage = ""
                )
            }
        }
    }

    // Individual app toggles for settings screen
    fun updateInstagram(blocked: Boolean) {
        viewModelScope.launch {
            dataStoreManager.updateInstagram(blocked)
        }
    }

    fun updateYoutube(blocked: Boolean) {
        viewModelScope.launch {
            dataStoreManager.updateYoutube(blocked)
        }
    }

    fun updateTikTok(blocked: Boolean) {
        viewModelScope.launch {
            dataStoreManager.updateTikTok(blocked)
        }
    }

    fun updateFacebook(blocked: Boolean) {
        viewModelScope.launch {
            dataStoreManager.updateFacebook(blocked)
        }
    }

    fun updateFacebookLite(blocked: Boolean) {
        viewModelScope.launch {
            dataStoreManager.updateFacebookLite(blocked)
        }
    }

    // Master blocking toggle for settings screen
    fun updateMasterBlocking(blocked: Boolean) {
        viewModelScope.launch {
            dataStoreManager.updateMasterBlocking(blocked)
        }
    }

    // Auto-hide ads toggle for settings screen
    fun updateAutoHideAds(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.updateAutoHideAds(enabled)
        }
    }

    // Developer mode unlock
    fun updateDeveloperModeUnlocked(unlocked: Boolean) {
        viewModelScope.launch {
            dataStoreManager.updateDeveloperModeUnlocked(unlocked)
        }
    }
}

