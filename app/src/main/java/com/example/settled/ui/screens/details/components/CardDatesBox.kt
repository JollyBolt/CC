package com.example.settled.ui.screens.details.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.settled.domain.model.Card
import com.example.settled.domain.model.CardStatus
import com.example.settled.ui.theme.SettledTheme

@Composable
fun CardDatesBox(card: Card, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 24.dp, horizontal = 24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Statement Date
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(
                    "STATEMENT DATE", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${card.statementDay}, 2026", 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF2E333D),
                    fontSize = 18.sp
                )
            }

            // Vertical Divider
            VerticalDivider(
                modifier = Modifier.height(40.dp),
                thickness = 1.dp,
                color = Color.LightGray.copy(alpha = 0.5f)
            )

            // Due Date
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Spacer(modifier = Modifier.width(16.dp)) // Added some gap after divider
                Text(
                    "DUE DATE", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${card.dueDay}, 2026", 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF2E333D),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CardDatesBoxPreview() {
    SettledTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CardDatesBox(
                card = Card(
                    id = "1",
                    bankName = "HDFC",
                    cardName = "Regalia",
                    lastFourDigits = "1234",
                    statementDay = 10,
                    dueDay = 28,
                    status = CardStatus.PAID,
                    minimumDueLastCycle = false,
                    daysUntilDue = 18
                )
            )
        }
    }
}
