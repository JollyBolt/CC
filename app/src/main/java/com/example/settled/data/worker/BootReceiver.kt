package com.example.settled.data.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleCycleResetWorker(context)
        }
    }
}

fun scheduleCycleResetWorker(context: Context) {
    val request = PeriodicWorkRequestBuilder<CycleResetWorker>(1, TimeUnit.DAYS).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "cycle_reset",
        ExistingPeriodicWorkPolicy.KEEP,
        request
    )
}
