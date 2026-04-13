package com.example.settled.ui.screens.details.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.settled.domain.model.PaymentLog
import com.example.settled.ui.theme.SettledTheme

@Composable
fun PaymentHistorySection(
    paymentLogs: List<PaymentLog>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Payment History",
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
                Text("No payment history yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Preview(showBackground = true)
@Composable
fun PaymentHistorySectionPreview() {
    SettledTheme {
        PaymentHistorySection(
            paymentLogs = listOf(
                PaymentLog("1", "FULL", "Mobiquick", System.currentTimeMillis(), 10, 2026),
                PaymentLog("2", "MINIMUM", "CRED", System.currentTimeMillis() - 86400000, 9, 2026)
            )
        )
    }
}
