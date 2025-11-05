package com.socialsentry.ai.application

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class SocialSentryApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@SocialSentryApplication)
            androidLogger()
            modules(AppModule.modules())
        }
    }
}

