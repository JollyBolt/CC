package com.example.settled

import com.example.settled.data.local.CardEntity
import com.example.settled.data.local.PaymentLogEntity
import com.example.settled.data.repository.calculateCardStatus
import com.example.settled.domain.model.CardStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CardStatusCalculatorTest {

    private fun card(statementDay: Int, dueDay: Int, id: String = "card-1") = CardEntity(
        id             = id,
        bankName       = "TEST",
        cardName       = "Card",
        lastFourDigits = "1234",
        statementDay   = statementDay,
        dueDay         = dueDay
    )

    private fun log(cardId: String, month: Int, year: Int, type: String = "FULL") = PaymentLogEntity(
        cardId     = cardId,
        type       = type,
        platform   = "CRED",
        timestamp  = System.currentTimeMillis(),
        cycleMonth = month,
        cycleYear  = year
    )

    // --- PAID ---

    @Test
    fun `payment log for active cycle returns PAID`() {
        val entity = card(statementDay = 10, dueDay = 25)
        // today = May 15 → active statement = May 10
        val today = LocalDate.of(2026, 5, 15)
        val logs = listOf(log(entity.id, month = 5, year = 2026))
        val result = calculateCardStatus(entity, logs, today)
        assertEquals(CardStatus.PAID, result.status)
    }

    // --- DUE ---

    @Test
    fun `no payment and today on or before due date returns DUE`() {
        val entity = card(statementDay = 10, dueDay = 25)
        // today = May 15, due date = May 25
        val today = LocalDate.of(2026, 5, 15)
        val result = calculateCardStatus(entity, emptyList(), today)
        assertEquals(CardStatus.DUE, result.status)
    }

    @Test
    fun `no payment and today equals due date returns DUE`() {
        val entity = card(statementDay = 10, dueDay = 25)
        val today = LocalDate.of(2026, 5, 25)
        val result = calculateCardStatus(entity, emptyList(), today)
        assertEquals(CardStatus.DUE, result.status)
    }

    // --- OVERDUE ---

    @Test
    fun `no payment and today strictly after due date returns OVERDUE`() {
        val entity = card(statementDay = 10, dueDay = 25)
        // today = May 26, due = May 25 → overdue
        val today = LocalDate.of(2026, 5, 26)
        val result = calculateCardStatus(entity, emptyList(), today)
        assertEquals(CardStatus.OVERDUE, result.status)
    }

    // --- Cross-month due ---

    @Test
    fun `cross-month due date falls in the month after statement`() {
        // statementDay=25, dueDay=5 → dueDay < statementDay → due date in next month
        val entity = card(statementDay = 25, dueDay = 5)
        // today = May 27 → active statement = May 25, due = June 5
        val today = LocalDate.of(2026, 5, 27)
        val result = calculateCardStatus(entity, emptyList(), today)
        assertEquals(CardStatus.DUE, result.status)
        assertEquals(LocalDate.of(2026, 6, 5), result.activeDueDate)
    }

    @Test
    fun `overdue when past cross-month due date`() {
        val entity = card(statementDay = 25, dueDay = 5)
        // today = June 6 → past June 5 due date
        val today = LocalDate.of(2026, 6, 6)
        val result = calculateCardStatus(entity, emptyList(), today)
        assertEquals(CardStatus.OVERDUE, result.status)
    }

    // --- Statement day coercion ---

    @Test
    fun `statement day 31 in 30-day month is coerced to day 30`() {
        val entity = card(statementDay = 31, dueDay = 15)
        // April has 30 days; today = April 15 → statement = April 30
        val today = LocalDate.of(2026, 4, 15)
        val result = calculateCardStatus(entity, emptyList(), today)
        assertEquals(30, result.activeStatementDate.dayOfMonth)
    }

    // --- Active cycle = previous month ---

    @Test
    fun `today before this month statement means active cycle is previous month`() {
        val entity = card(statementDay = 20, dueDay = 5)
        // today = May 10, statement day 20 → not yet reached May 20
        // active statement = April 20, due = May 5
        // today May 10 is after May 5 → OVERDUE (no payment for April cycle)
        val today = LocalDate.of(2026, 5, 10)
        val result = calculateCardStatus(entity, emptyList(), today)
        assertEquals(CardStatus.OVERDUE, result.status)
        assertEquals(LocalDate.of(2026, 4, 20), result.activeStatementDate)
    }

    @Test
    fun `payment for previous month cycle marks card PAID before statement day`() {
        val entity = card(statementDay = 20, dueDay = 5)
        val today = LocalDate.of(2026, 5, 10)
        // Active cycle is April → log for April
        val logs = listOf(log(entity.id, month = 4, year = 2026))
        val result = calculateCardStatus(entity, logs, today)
        assertEquals(CardStatus.PAID, result.status)
    }

    // --- February / leap year ---

    @Test
    fun `statement day 29 in non-leap Feb is coerced to 28`() {
        val entity = card(statementDay = 29, dueDay = 15)
        // 2025 is not a leap year; today = Feb 15 2025 → active statement Feb 28
        val today = LocalDate.of(2025, 2, 15)
        val result = calculateCardStatus(entity, emptyList(), today)
        assertEquals(28, result.activeStatementDate.dayOfMonth)
    }

    @Test
    fun `statement day 29 in leap year Feb resolves to day 29`() {
        val entity = card(statementDay = 29, dueDay = 15)
        // 2024 is a leap year; today = Feb 15 2024 → active statement Feb 29
        val today = LocalDate.of(2024, 2, 15)
        val result = calculateCardStatus(entity, emptyList(), today)
        assertEquals(29, result.activeStatementDate.dayOfMonth)
    }
}
