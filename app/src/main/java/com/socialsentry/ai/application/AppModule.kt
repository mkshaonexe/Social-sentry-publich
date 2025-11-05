package com.socialsentry.ai.application

import com.socialsentry.ai.SocialSentryViewModel
import com.socialsentry.ai.datastore.DataStoreManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

object AppModule {
    fun modules() = listOf(commonModule, viewModelModule)
}

val viewModelModule = module {
    viewModel { SocialSentryViewModel(dataStoreManager = get()) }
}

val commonModule = module {
    single { DataStoreManager(androidContext()) }
}

