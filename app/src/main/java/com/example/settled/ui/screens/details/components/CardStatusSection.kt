package com.example.settled.ui.screens.details.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.settled.R
import com.example.settled.domain.model.Card
import com.example.settled.domain.model.CardStatus
import com.example.settled.domain.model.PaymentLog
import com.example.settled.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CardStatusSection(card: Card, modifier: Modifier = Modifier) {
    val statusColor = when (card.status) {
        CardStatus.PAID -> StatusPaid
        CardStatus.DUE -> StatusDue
        CardStatus.OVERDUE -> StatusOverdue
    }

    val statusText = when (card.status) {
        CardStatus.PAID -> {
            val date = card.lastPaymentInfo?.timestamp?.let {
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(it))
            } ?: stringResource(R.string.date_fallback_today)
            stringResource(R.string.status_paid, date, card.lastPaymentInfo?.platform ?: "CRED")
        }
        CardStatus.DUE -> when {
            card.daysUntilDue > 1 -> stringResource(R.string.status_due_days, card.daysUntilDue)
            card.daysUntilDue == 1 -> stringResource(R.string.status_due_one_day)
            else -> stringResource(R.string.status_due_today)
        }
        CardStatus.OVERDUE -> stringResource(R.string.status_overdue_days, -card.daysUntilDue)
    }

    Text(
        text = statusText,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = statusColor,
        textAlign = TextAlign.Center,
        fontSize = 20.sp,
        modifier = modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
fun CardStatusSectionPreview() {
    SettledTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CardStatusSection(
                card = Card(
                    id = "1",
                    bankName = "HDFC",
                    cardName = "Regalia",
                    lastFourDigits = "1234",
                    statementDay = 10,
                    dueDay = 28,
                    status = CardStatus.PAID,
                    minimumDueLastCycle = false,
                    daysUntilDue = 18,
                    lastPaymentInfo = PaymentLog("1", "FULL", "CRED", System.currentTimeMillis(), 10, 2026)
                )
            )
            CardStatusSection(
                card = Card(
                    id = "2",
                    bankName = "SBI",
                    cardName = "Cashback",
                    lastFourDigits = "9012",
                    statementDay = 22,
                    dueDay = 11,
                    status = CardStatus.OVERDUE,
                    minimumDueLastCycle = false,
                    daysUntilDue = -3
                )
            )
        }
    }
}
