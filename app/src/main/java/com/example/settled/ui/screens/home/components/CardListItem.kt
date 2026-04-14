package com.example.settled.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.example.settled.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.settled.domain.model.Card
import com.example.settled.domain.model.CardStatus
import com.example.settled.domain.model.PaymentLog
import com.example.settled.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CardListItem(
    card: Card,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (card.status) {
        CardStatus.SOON -> StatusSoon
        CardStatus.DUE -> StatusDue
        CardStatus.PAID -> StatusPaid
    }
    
    val statusText = when (card.status) {
        CardStatus.PAID -> {
            val date = card.lastPaymentInfo?.timestamp?.let { 
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(it))
            } ?: "today"
            "Status: Paid on $date via ${card.lastPaymentInfo?.platform ?: "CRED"}"
        }
        CardStatus.DUE -> "Status: Due in ${card.daysUntilDue} days"
        CardStatus.SOON -> {
            val daysStr = when {
                card.daysUntilDue > 1 -> "${card.daysUntilDue} days"
                card.daysUntilDue == 1 -> "1 day"
                else -> "today"
            }
            "Status: Due $daysStr"
        }
    }

    val hasLogo = when {
        card.bankName.contains("HDFC", ignoreCase = true) -> true
        card.bankName.contains("ICICI", ignoreCase = true) -> true
        card.bankName.contains("SBI", ignoreCase = true) -> true
        card.bankName.contains("AXIS", ignoreCase = true) -> true
        card.bankName.contains("KOTAK", ignoreCase = true) -> true
        else -> false
    }

    val bankLogo = if (hasLogo) {
        when {
            card.bankName.contains("HDFC", ignoreCase = true) -> R.drawable.logo_bank_hdfc_sq
            card.bankName.contains("ICICI", ignoreCase = true) -> R.drawable.logo_bank_icici_sq
            card.bankName.contains("SBI", ignoreCase = true) -> R.drawable.logo_bank_sbi_sq
            card.bankName.contains("AXIS", ignoreCase = true) -> R.drawable.logo_bank_axis_sq
            card.bankName.contains("KOTAK", ignoreCase = true) -> R.drawable.logo_bank_kotak_sq
            card.bankName.contains("HSBC", ignoreCase = true) -> R.drawable.logo_bank_hsbc_sq
            card.bankName.contains("YES", ignoreCase = true) -> R.drawable.logo_bank_yes_sq
            card.bankName.contains("RBL", ignoreCase = true) -> R.drawable.logo_bank_rbl_sq
            else -> null
        }
    } else null

    val fallbackIcon: ImageVector = when {
        card.bankName.contains("HDFC", ignoreCase = true) -> Icons.Default.AccountBalance
        card.bankName.contains("ICICI", ignoreCase = true) -> Icons.Default.ShoppingCart
        card.bankName.contains("SBI", ignoreCase = true) -> Icons.Default.CreditCard
        else -> Icons.AutoMirrored.Filled.ListAlt
    }

    val opacity = if (card.isLocked) 0.5f else 1f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White ),
//                .background(if (bankLogo != null) Color.White else statusColor.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            if (bankLogo != null) {
                Image(
                    painter = painterResource(id = bankLogo as Int),
                    contentDescription = card.bankName,
                    modifier = Modifier.size(50.dp).padding(0.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(5.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.cardName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black.copy(alpha = opacity),
                    fontSize = 19.sp
                )
                
                Text(
                    text = "XX${card.lastFourDigits}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray.copy(alpha = opacity),
                    fontSize = 17.sp
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = statusColor.copy(alpha = opacity),
                fontSize = 15.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CardListItemPreview() {
    SettledTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(0.dp).background(Color(0xFFF1F1F1))) {
            CardListItem(
                card = Card(
                    id = "1",
                    bankName = "HDFC",
                    cardName = "Regalia Gold",
                    lastFourDigits = "1234",
                    statementDate = 10,
                    dueDate = 28,
                    status = CardStatus.PAID,
                    minimumDueLastCycle = false,
                    daysUntilDue = 18,
                    lastPaymentInfo = PaymentLog(
                        id = "log1",
                        type = "FULL",
                        platform = "Mobiquick",
                        timestamp = System.currentTimeMillis(),
                        cycleMonth = 10,
                        cycleYear = 2026
                    )
                ),
                onClick = {}
            )
            CardListItem(
                card = Card(
                    id = "2",
                    bankName = "ICICI",
                    cardName = "Amazon Pay",
                    lastFourDigits = "5678",
                    statementDate = 26,
                    dueDate = 15,
                    status = CardStatus.DUE,
                    minimumDueLastCycle = false,
                    daysUntilDue = 4
                ),
                onClick = {}
            )
            CardListItem(
                card = Card(
                    id = "3",
                    bankName = "SBI",
                    cardName = "Cashback",
                    lastFourDigits = "9012",
                    statementDate = 22,
                    dueDate = 11,
                    status = CardStatus.SOON,
                    minimumDueLastCycle = false,
                    daysUntilDue = 0
                ),
                onClick = {}
            )
        }
    }
}

