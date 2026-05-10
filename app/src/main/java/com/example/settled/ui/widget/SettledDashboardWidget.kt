package com.example.settled.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.settled.core.Result
import com.example.settled.domain.model.Card
import com.example.settled.domain.model.CardStatus
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

private val GlancePaid    = Color(0xFF2E7D32)
private val GlanceDue     = Color(0xFFCC8800)
private val GlanceOverdue = Color(0xFFB71C1C)
private val GlanceSurface = Color(0xFF1E2028)
private val GlanceSurfaceCard = Color(0xFF272B36)

class SettledDashboardWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .cardRepository()

        val cards = (repo.getAllCards().first() as? Result.Success)?.data ?: emptyList()

        provideContent { DashboardWidgetContent(cards) }
    }
}

@Composable
private fun DashboardWidgetContent(cards: List<Card>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceSurface)
            .padding(8.dp)
    ) {
        Text(
            text = "SETTLED",
            style = TextStyle(
                color = ColorProvider(Color.White.copy(alpha = 0.5f)),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(6.dp))

        if (cards.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No cards",
                    style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.5f)), fontSize = 12.sp)
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(cards) { card ->
                    DashboardCardRow(card)
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun DashboardCardRow(card: Card) {
    val statusColor = when (card.status) {
        CardStatus.PAID    -> GlancePaid
        CardStatus.DUE     -> GlanceDue
        CardStatus.OVERDUE -> GlanceOverdue
    }
    val statusLabel = when (card.status) {
        CardStatus.PAID    -> "PAID"
        CardStatus.DUE     -> "DUE"
        CardStatus.OVERDUE -> "OVERDUE"
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceSurfaceCard)
            .cornerRadius(10.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = card.bankName,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = card.cardName,
                style = TextStyle(
                    color = ColorProvider(Color.White.copy(alpha = 0.6f)),
                    fontSize = 10.sp
                )
            )
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Box(
            modifier = GlanceModifier
                .background(statusColor)
                .cornerRadius(6.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = statusLabel,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
