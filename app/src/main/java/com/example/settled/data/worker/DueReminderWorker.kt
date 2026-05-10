package com.example.settled.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.settled.core.notifications.NotificationHelper
import com.example.settled.data.local.CardDao
import com.example.settled.domain.model.CardStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class DueReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cardDao: CardDao,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        notificationHelper.createChannel(applicationContext)

        val allCards  = cardDao.getAllCards().first()
        val allLogs   = cardDao.getAllPaymentLogs().first()

        allCards.forEachIndexed { index, entity ->
            val log = allLogs.firstOrNull { it.cardId == entity.id }
            if (log == null) {
                val (title, body) = buildMessage(entity.bankName, entity.cardName, entity.dueDay)
                notificationHelper.showDueReminder(
                    context        = applicationContext,
                    title          = title,
                    body           = body,
                    notificationId = index + 1000
                )
            }
        }
        return Result.success()
    }

    private fun buildMessage(bank: String, card: String, dueDay: Int): Pair<String, String> {
        return "Payment Due" to "$bank $card payment is due on day $dueDay — don't miss it!"
    }
}

fun scheduleDueReminderWorker(context: Context) {
    val request = PeriodicWorkRequestBuilder<DueReminderWorker>(1, TimeUnit.DAYS).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "due_reminder",
        ExistingPeriodicWorkPolicy.KEEP,
        request
    )
}
