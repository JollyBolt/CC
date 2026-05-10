package com.example.settled.data.repository

import com.example.settled.data.local.CardEntity
import com.example.settled.data.local.PaymentLogEntity
import com.example.settled.domain.model.Card
import com.example.settled.domain.model.CardStatus
import com.example.settled.domain.model.PaymentLog
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

internal fun calculateCardStatus(
    entity: CardEntity,
    logs: List<PaymentLogEntity>,
    today: LocalDate = LocalDate.now()
): Card {
    val currentMonth = today.monthValue
    val currentYear  = today.year

    val safeStatementDay = entity.statementDay
        .coerceAtMost(YearMonth.of(currentYear, currentMonth).lengthOfMonth())
    val statementThisMonth = LocalDate.of(currentYear, currentMonth, safeStatementDay)

    val activeStatementDate: LocalDate = if (today.isBefore(statementThisMonth)) {
        val prev = today.minusMonths(1)
        val prevSafeDay = entity.statementDay
            .coerceAtMost(YearMonth.of(prev.year, prev.monthValue).lengthOfMonth())
        LocalDate.of(prev.year, prev.monthValue, prevSafeDay)
    } else {
        statementThisMonth
    }

    val activeDueDate: LocalDate = if (entity.dueDay >= entity.statementDay) {
        val safeDueDay = entity.dueDay
            .coerceAtMost(YearMonth.of(activeStatementDate.year, activeStatementDate.monthValue).lengthOfMonth())
        LocalDate.of(activeStatementDate.year, activeStatementDate.monthValue, safeDueDay)
    } else {
        val next = activeStatementDate.plusMonths(1)
        val safeDueDay = entity.dueDay
            .coerceAtMost(YearMonth.of(next.year, next.monthValue).lengthOfMonth())
        LocalDate.of(next.year, next.monthValue, safeDueDay)
    }

    val cardLogs = logs.filter { it.cardId == entity.id }

    val latestLog = cardLogs.filter {
        it.cycleMonth == activeStatementDate.monthValue &&
            it.cycleYear == activeStatementDate.year
    }.maxByOrNull { it.timestamp }

    val lastUsedPlatform = cardLogs.maxByOrNull { it.timestamp }?.platform

    val daysUntilDue = ChronoUnit.DAYS.between(today, activeDueDate).toInt()

    val status = when {
        latestLog != null         -> CardStatus.PAID
        today.isAfter(activeDueDate) -> CardStatus.OVERDUE
        else                      -> CardStatus.DUE
    }

    val lastPaymentInfo = latestLog?.let {
        PaymentLog(
            id         = it.id,
            type       = it.type,
            platform   = it.platform,
            timestamp  = it.timestamp,
            cycleMonth = it.cycleMonth,
            cycleYear  = it.cycleYear
        )
    }

    return Card(
        id                 = entity.id,
        bankName           = entity.bankName,
        cardName           = entity.cardName,
        lastFourDigits     = entity.lastFourDigits,
        statementDay       = entity.statementDay,
        dueDay             = entity.dueDay,
        status             = status,
        minimumDueLastCycle = latestLog?.type == "MINIMUM",
        daysUntilDue       = daysUntilDue,
        activeStatementDate = activeStatementDate,
        activeDueDate      = activeDueDate,
        isLocked           = false,
        lastPaymentInfo    = lastPaymentInfo,
        lastUsedPlatform   = lastUsedPlatform
    )
}
