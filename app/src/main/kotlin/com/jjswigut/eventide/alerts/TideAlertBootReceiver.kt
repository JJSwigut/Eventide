package com.jjswigut.eventide.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jjswigut.eventide.repository.TideAlertRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class TideAlertBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val koin = GlobalContext.get()
                val alertRepository = koin.get<TideAlertRepository>()
                val scheduler = koin.get<TideAlertScheduler>()

                alertRepository.getEnabledAlertPreferences()
                    .first()
                    .forEach { scheduler.schedule(it.stationId) }
            }
            pendingResult.finish()
        }
    }
}
