package com.example.settled.ui.screens.details.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.settled.R
import com.example.settled.domain.model.PaymentLog
import com.example.settled.ui.theme.SettledTheme

@Composable
fun PaymentHistorySection(
    paymentLogs: List<PaymentLog>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.payment_history_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (paymentLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.payment_history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            // Note: In a real app, this should probably be part of a larger LazyColumn
            // but for a separate component, a simple Column or local LazyColumn is fine.
            // Using Column here to avoid nested LazyColumns if the parent is a LazyColumn.
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                paymentLogs.forEach { log ->
                    PaymentLogItem(log)
                }
            }
        }
    }
}

@Preview(name = "Payment history — with logs", showBackground = true)
@Composable
private fun PaymentHistorySectionWithLogsPreview() {
    SettledTheme {
        PaymentHistorySection(paymentLogs = listOf(
            PaymentLog("1", "FULL", "CRED", System.currentTimeMillis(), 5, 2026),
            PaymentLog("2", "MINIMUM", "GPAY", System.currentTimeMillis() - 86400000L * 32, 4, 2026),
            PaymentLog("3", "FULL", "BANK APP", System.currentTimeMillis() - 86400000L * 63, 3, 2026),
        ))
    }
}

@Preview(name = "Payment history — empty", showBackground = true)
@Composable
private fun PaymentHistorySectionEmptyPreview() {
    SettledTheme {
        PaymentHistorySection(paymentLogs = emptyList())
    }
}
