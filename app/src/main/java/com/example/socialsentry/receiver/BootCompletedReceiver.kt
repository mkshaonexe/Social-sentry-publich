package com.example.socialsentry.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.socialsentry.data.datastore.SocialSentryDataStore
import com.example.socialsentry.domain.UnblockAllowanceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
	private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
			receiverScope.launch {
				val dataStore = SocialSentryDataStore(context)
				val manager = UnblockAllowanceManager(context, dataStore)
				manager.rescheduleAll()
			}
		}
	}
}


