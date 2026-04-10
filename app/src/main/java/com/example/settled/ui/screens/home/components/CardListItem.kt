package com.example.settled.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.settled.domain.model.Card
import com.example.settled.domain.model.CardStatus
import com.example.settled.ui.theme.*

@Composable
fun CardListItem(
    card: Card,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (card.status) {
        CardStatus.OVERDUE -> StatusOverdue
        CardStatus.DUE_SOON -> StatusDueSoon
        CardStatus.PENDING -> StatusPending
        CardStatus.PAID -> StatusPaid
    }
    
    val statusText = when (card.status) {
        CardStatus.PAID -> "Paid via ${card.lastPaymentInfo?.platform ?: "App"}"
        CardStatus.PENDING -> "Due ${card.dueDate} · ${card.daysUntilDue} days left"
        CardStatus.DUE_SOON -> "Due ${card.dueDate} · Due in ${card.daysUntilDue} day(s)"
        CardStatus.OVERDUE -> "Due ${card.dueDate} · Overdue"
    }

    val opacity = if (card.isLocked) 0.5f else 1f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Ring
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = card.cardName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = opacity)
                )
                if (card.minimumDueLastCycle) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Partial payment warning",
                        tint = StatusDueSoon,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = if (card.status == CardStatus.OVERDUE) StatusOverdue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = opacity)
            )
        }
        
        Text(
            text = "•••${card.lastFourDigits}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = opacity)
        )
    }
}
