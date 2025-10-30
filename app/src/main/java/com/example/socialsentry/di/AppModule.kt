package com.example.socialsentry.di

import com.example.socialsentry.data.datastore.SocialSentryDataStore
import com.example.socialsentry.data.datastore.YouTubeTrackingDataStore
import com.example.socialsentry.domain.UnblockAllowanceManager
import com.example.socialsentry.manager.AppUsageManager
import com.example.socialsentry.presentation.viewmodel.SocialSentryViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // DataStore
    single { SocialSentryDataStore(androidContext()) }
    single { YouTubeTrackingDataStore(androidContext()) }

    // Managers
    single { UnblockAllowanceManager(androidContext(), get()) }
    single { AppUsageManager(androidContext(), get()) }

    // ViewModels
    viewModel { SocialSentryViewModel(get(), get()) }
}

