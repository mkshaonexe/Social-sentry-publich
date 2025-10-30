package com.example.socialsentry

import android.app.Application
import com.example.socialsentry.domain.UnblockAllowanceManager
import com.example.socialsentry.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.get
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SocialSentryApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger()
            androidContext(this@SocialSentryApplication)
            modules(appModule)
        }

        // Ensure midnight reset is scheduled and today's allowance is initialized
        try {
            val manager: UnblockAllowanceManager = get(UnblockAllowanceManager::class.java)
            // Fire-and-forget scheduling
            GlobalScope.launch {
                manager.ensureAllowanceDateReset()
                manager.scheduleMidnightResetWorker()
            }
        } catch (_: Throwable) {
            // Ignore; app will still function, and BootReceiver covers reboots
        }
    }
}

