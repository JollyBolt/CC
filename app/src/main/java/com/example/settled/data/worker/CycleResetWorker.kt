package com.example.settled.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.settled.core.Result
import com.example.settled.data.local.CardDao
import com.example.settled.data.local.PaymentLogEntity
import com.example.settled.domain.repository.CardRepository
import com.example.settled.ui.widget.SettledDashboardWidget
import com.example.settled.ui.widget.SettledMiniWidget
import com.example.settled.ui.widget.SettledStatusWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth

@HiltWorker
class CycleResetWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cardDao: CardDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val cards = cardDao.getAllCards().first()
        val allLogs = cardDao.getAllPaymentLogs().first()

        cards.forEach { entity ->
            val safeStatementDay = entity.statementDay
                .coerceAtMost(YearMonth.of(today.year, today.monthValue).lengthOfMonth())
            if (today.dayOfMonth == safeStatementDay) {
                // Today is this card's statement day — check if previous cycle was settled
                val prevMonth = today.minusMonths(1)
                val prevCycleMonth = prevMonth.monthValue
                val prevCycleYear  = prevMonth.year
                val hasLog = allLogs.any {
                    it.cardId == entity.id &&
                        it.cycleMonth == prevCycleMonth &&
                        it.cycleYear == prevCycleYear
                }
                if (!hasLog) {
                    cardDao.insertPaymentLog(
                        PaymentLogEntity(
                            cardId     = entity.id,
                            type       = "MISSED",
                            platform   = "NONE",
                            timestamp  = System.currentTimeMillis(),
                            cycleMonth = prevCycleMonth,
                            cycleYear  = prevCycleYear
                        )
                    )
                }
            }
        }

        refreshWidgets()
        return Result.success()
    }

    private suspend fun refreshWidgets() {
        val manager = GlanceAppWidgetManager(applicationContext)
        listOf(
            SettledMiniWidget()      to manager.getGlanceIds(SettledMiniWidget::class.java),
            SettledStatusWidget()    to manager.getGlanceIds(SettledStatusWidget::class.java),
            SettledDashboardWidget() to manager.getGlanceIds(SettledDashboardWidget::class.java)
        ).forEach { (widget, ids) ->
            ids.forEach { id -> widget.update(applicationContext, id) }
        }
    }
}
