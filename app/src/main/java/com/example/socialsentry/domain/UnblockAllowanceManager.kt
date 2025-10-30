package com.example.socialsentry.domain

import android.content.Context
import androidx.work.*
import com.example.socialsentry.data.datastore.SocialSentryDataStore
import com.example.socialsentry.data.model.SocialSentrySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class UnblockAllowanceManager(
	private val context: Context,
	private val dataStore: SocialSentryDataStore
) {

	private val workManager: WorkManager = WorkManager.getInstance(context)

	suspend fun ensureAllowanceDateReset() {
		withContext(Dispatchers.IO) {
			val settings = dataStore.getCurrentSettings()
			val today = LocalDate.now(ZoneId.systemDefault()).toString()
			if (settings.lastAllowanceResetLocalDate != today) {
				val fullMs = settings.dailyTemporaryUnblockMinutes.toLong() * 60_000L
				val updated = settings.copy(
					remainingTemporaryUnblockMs = fullMs,
					lastAllowanceResetLocalDate = today,
					// On new day, session cannot be active because allowance refills
					isTemporaryUnblockActive = false,
					temporaryUnblockSessionStartEpochMs = null
				)
				dataStore.updateSettings(updated)
			}
		}
	}

	suspend fun startTemporaryUnblockNow(): Boolean {
		return withContext(Dispatchers.IO) {
			ensureAllowanceDateReset()
			val settings = dataStore.getCurrentSettings()
			if (settings.remainingTemporaryUnblockMs <= 0L) {
				return@withContext false
			}
			if (settings.isTemporaryUnblockActive) {
				// Already active, just ensure worker is scheduled correctly
				scheduleEndWorker(settings)
				return@withContext true
			}
			val now = System.currentTimeMillis()
			val updated = settings.copy(
				isTemporaryUnblockActive = true,
				temporaryUnblockSessionStartEpochMs = now
			)
			dataStore.updateSettings(updated)
			scheduleEndWorker(updated)
			true
		}
	}

	suspend fun endTemporaryUnblockAndDecrementAllowance() {
		withContext(Dispatchers.IO) {
			val settings = dataStore.getCurrentSettings()
			if (!settings.isTemporaryUnblockActive) return@withContext
			val startedAt = settings.temporaryUnblockSessionStartEpochMs ?: return@withContext
			val elapsed = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
			val sessionConsumption = elapsed
				.coerceAtMost(settings.maxTemporaryUnblockSessionMs)
				.coerceAtMost(settings.remainingTemporaryUnblockMs)
			val remaining = (settings.remainingTemporaryUnblockMs - sessionConsumption).coerceAtLeast(0L)
			val updated = settings.copy(
				isTemporaryUnblockActive = false,
				remainingTemporaryUnblockMs = remaining,
				temporaryUnblockSessionStartEpochMs = null
			)
			dataStore.updateSettings(updated)
			workManager.cancelUniqueWork(END_UNBLOCK_WORK_NAME)
			
			// Show notification that time is up and reels are blocked again
			val notificationManager = SocialSentryNotificationManager(context)
			notificationManager.showTimeUpNotification()
		}
	}

	suspend fun forceEndNowAndZeroAllowance() {
		withContext(Dispatchers.IO) {
			val settings = dataStore.getCurrentSettings()
			val updated = settings.copy(
				isTemporaryUnblockActive = false,
				remainingTemporaryUnblockMs = 0L,
				temporaryUnblockSessionStartEpochMs = null
			)
			dataStore.updateSettings(updated)
			workManager.cancelUniqueWork(END_UNBLOCK_WORK_NAME)
		}
	}

	/**
	 * Manually add minutes to today's temporary unblock allowance.
	 * If a session is active, reschedules its end accordingly.
	 */
	suspend fun addManualMinutes(minutes: Int) {
		withContext(Dispatchers.IO) {
			ensureAllowanceDateReset()
			val safeMinutes = minutes.coerceAtLeast(0)
			if (safeMinutes == 0) return@withContext
			val addMs = safeMinutes.toLong() * 60_000L
			val settings = dataStore.getCurrentSettings()
			val beforeMs = settings.remainingTemporaryUnblockMs
			val updated = settings.copy(
				remainingTemporaryUnblockMs = settings.remainingTemporaryUnblockMs + addMs
			)
			dataStore.updateSettings(updated)
			// Log for debugging
			android.util.Log.d("UnblockManager", "Added $safeMinutes minutes. Before: ${beforeMs/60000}min, After: ${updated.remainingTemporaryUnblockMs/60000}min")
			if (updated.isTemporaryUnblockActive) {
				scheduleEndWorker(updated)
			}
		}
	}

	suspend fun rescheduleAll() {
		withContext(Dispatchers.IO) {
			ensureAllowanceDateReset()
			val settings = dataStore.getCurrentSettings()
			if (settings.isTemporaryUnblockActive) {
				scheduleEndWorker(settings)
			}
			scheduleMidnightResetWorker()
		}
	}

	private fun scheduleEndWorker(settings: SocialSentrySettings) {
		val start = settings.temporaryUnblockSessionStartEpochMs ?: return
		val now = System.currentTimeMillis()
		val elapsed = (now - start).coerceAtLeast(0L)
		val remainingForSession = (settings.remainingTemporaryUnblockMs - elapsed).coerceAtLeast(0L)
		val sessionCap = settings.maxTemporaryUnblockSessionMs.coerceAtLeast(0L)
		val sessionRemainingCap = (sessionCap - elapsed).coerceAtLeast(0L)
		val delayMs = minOf(remainingForSession, sessionRemainingCap)
		if (delayMs <= 0L) {
			// Trigger immediate end to enforce cap or depleted allowance
			val immediateRequest = OneTimeWorkRequestBuilder<EndUnblockWorker>()
				.setInitialDelay(0L, TimeUnit.MILLISECONDS)
				.setConstraints(Constraints.NONE)
				.addTag(END_UNBLOCK_WORK_NAME)
				.build()
			workManager.enqueueUniqueWork(
				END_UNBLOCK_WORK_NAME,
				ExistingWorkPolicy.REPLACE,
				immediateRequest
			)
			return
		}
		val request = OneTimeWorkRequestBuilder<EndUnblockWorker>()
			.setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
			.setConstraints(Constraints.NONE)
			.addTag(END_UNBLOCK_WORK_NAME)
			.build()
		workManager.enqueueUniqueWork(
			END_UNBLOCK_WORK_NAME,
			ExistingWorkPolicy.REPLACE,
			request
		)
	}

	fun scheduleMidnightResetWorker() {
		// Calculate delay until local midnight
		val zone = ZoneId.systemDefault()
		val now = java.time.ZonedDateTime.now(zone)
		val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zone)
		val delayMs = java.time.Duration.between(now, nextMidnight).toMillis()
		val request = OneTimeWorkRequestBuilder<MidnightResetWorker>()
			.setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
			.setConstraints(Constraints.NONE)
			.addTag(MIDNIGHT_RESET_WORK_NAME)
			.build()
		workManager.enqueueUniqueWork(
			MIDNIGHT_RESET_WORK_NAME,
			ExistingWorkPolicy.REPLACE,
			request
		)
	}

	companion object {
		const val END_UNBLOCK_WORK_NAME = "EndUnblockWork"
		const val MIDNIGHT_RESET_WORK_NAME = "MidnightResetWork"
	}
}

class EndUnblockWorker(
	appContext: Context,
	params: WorkerParameters
) : CoroutineWorker(appContext, params) {
	private val dataStore = SocialSentryDataStore(appContext)
	override suspend fun doWork(): Result {
		val manager = UnblockAllowanceManager(applicationContext, dataStore)
		// When this fires, session should be ended and allowance decremented to zero
		manager.endTemporaryUnblockAndDecrementAllowance()
		
		// Show notification that time is up
		val notificationManager = SocialSentryNotificationManager(applicationContext)
		notificationManager.showTimeUpNotification()
		
		return Result.success()
	}
}

class MidnightResetWorker(
	appContext: Context,
	params: WorkerParameters
) : CoroutineWorker(appContext, params) {
	private val dataStore = SocialSentryDataStore(appContext)
	override suspend fun doWork(): Result {
		val manager = UnblockAllowanceManager(applicationContext, dataStore)
		manager.ensureAllowanceDateReset()
		// Re-schedule next midnight
		manager.scheduleMidnightResetWorker()
		return Result.success()
	}
}


