package com.example.settled.ui.screens.details.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.settled.R
import com.example.settled.domain.model.Card
import com.example.settled.domain.model.CardStatus
import com.example.settled.ui.theme.SettledTheme

@Composable
fun CardVisual(
    card: Card,
    modifier: Modifier = Modifier
) {
    val gradient = when {
        card.bankName.contains("HDFC", ignoreCase = true) -> {
            Brush.linearGradient(
                colors = listOf(Color(0xFF424242), Color(0xFF000000)),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }
        card.bankName.contains("ICICI", ignoreCase = true) -> {
            Brush.linearGradient(
                colors = listOf(Color(0xFF212121), Color(0xFF000000)),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }
        card.bankName.contains("SBI", ignoreCase = true) -> {
            Brush.linearGradient(
                colors = listOf(Color(0xFF1A73E8), Color(0xFF0D47A1)),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }
        else -> {
            Brush.linearGradient(
                colors = listOf(Color(0xFF6200EE), Color(0xFF3700B3)),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }
    }

    val bankLogo: Int? = null /* when {
        card.bankName.contains("HDFC", ignoreCase = true) -> R.drawable.logo_hdfc
        card.bankName.contains("ICICI", ignoreCase = true) -> R.drawable.logo_icici
        card.bankName.contains("SBI", ignoreCase = true) -> R.drawable.logo_sbi
        card.bankName.contains("AXIS", ignoreCase = true) -> R.drawable.logo_axis
        card.bankName.contains("KOTAK", ignoreCase = true) -> R.drawable.logo_kotak
        else -> null
    } */

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .padding(24.dp)
    ) {
        // Top Row: Logo & Chip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            if (bankLogo != null) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = bankLogo as Int),
                        contentDescription = card.bankName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                Text(
                    text = card.bankName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            CardChip()
        }

        // Bottom Content
        Column(
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            val cleanBankName = card.bankName.replace("Bank", "", ignoreCase = true).trim()
            val cleanCardName = card.cardName.replace("Bank", "", ignoreCase = true).trim()
            
            Text(
                text = "${cleanBankName.uppercase()} ${cleanCardName.uppercase()}",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 17.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "XXXX XXXX XXXX ${card.lastFourDigits}",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun CardChip() {
    Box(
        modifier = Modifier
            .size(width = 45.dp, height = 32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFB8860B), // Dark Gold
                        Color(0xFFDAA520), // Goldenrod
                        Color(0xFFFFD700), // Gold
                        Color(0xFFDAA520), 
                        Color(0xFFB8860B)
                    )
                )
            )
    )
}

@Preview
@Composable
fun CardVisualPreview() {
    SettledTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CardVisual(
                card = Card(
                    id = "1",
                    bankName = "HDFC",
                    cardName = "Regalia Gold",
                    lastFourDigits = "1234",
                    statementDate = 10,
                    dueDate = 28,
                    status = CardStatus.PAID,
                    minimumDueLastCycle = false,
                    daysUntilDue = 18
                )
            )
            CardVisual(
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
                )
            )
            CardVisual(
                card = Card(
                    id = "3",
                    bankName = "SBI",
                    cardName = "Cashback Card",
                    lastFourDigits = "9012",
                    statementDate = 22,
                    dueDate = 11,
                    status = CardStatus.SOON,
                    minimumDueLastCycle = false,
                    daysUntilDue = 0
                )
            )
        }
    }
}
