package com.example.settled.ui.screens.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.settled.domain.model.PaymentLog
import com.example.settled.ui.theme.SettledTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaymentLogItem(log: PaymentLog, modifier: Modifier = Modifier) {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateString = formatter.format(Date(log.timestamp))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(log.type, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("Via ${log.platform}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(dateString, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
fun PaymentLogItemPreview() {
    SettledTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PaymentLogItem(
                log = PaymentLog(
                    id = "1",
                    type = "FULL PAYMENT",
                    platform = "Mobiquick",
                    timestamp = System.currentTimeMillis(),
                    cycleMonth = 10,
                    cycleYear = 2026
                )
            )
        }
    }
}
