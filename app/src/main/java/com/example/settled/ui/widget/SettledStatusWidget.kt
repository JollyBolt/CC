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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.settled.core.Result
import com.example.settled.domain.model.CardStatus
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

private val GlancePaid    = Color(0xFF2E7D32)
private val GlanceDue     = Color(0xFFCC8800)
private val GlanceOverdue = Color(0xFFB71C1C)
private val GlanceSurface = Color(0xFF1E2028)

class SettledStatusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .cardRepository()

        val cards = (repo.getAllCards().first() as? Result.Success)?.data ?: emptyList()

        val paid    = cards.count { it.status == CardStatus.PAID }
        val due     = cards.count { it.status == CardStatus.DUE }
        val overdue = cards.count { it.status == CardStatus.OVERDUE }

        provideContent { StatusWidgetContent(paid, due, overdue) }
    }
}

@Composable
private fun StatusWidgetContent(paid: Int, due: Int, overdue: Int) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceSurface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusBubble(count = paid,    color = GlancePaid,    label = "PAID",    modifier = GlanceModifier.defaultWeight())
        StatusBubble(count = due,     color = GlanceDue,     label = "DUE",     modifier = GlanceModifier.defaultWeight())
        StatusBubble(count = overdue, color = GlanceOverdue, label = "OVERDUE", modifier = GlanceModifier.defaultWeight())
    }
}

@Composable
private fun StatusBubble(count: Int, color: Color, label: String, modifier: GlanceModifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .background(color)
                .cornerRadius(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(Color.White.copy(alpha = 0.7f)),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
