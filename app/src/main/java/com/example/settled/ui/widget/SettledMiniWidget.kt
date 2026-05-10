package com.example.settled.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

private val GlancePaid    = Color(0xFF2E7D32)
private val GlanceDue     = Color(0xFFCC8800)
private val GlanceOverdue = Color(0xFFB71C1C)

class SettledMiniWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .cardRepository()

        val cards = (repo.getAllCards().first() as? com.example.settled.core.Result.Success)?.data
            ?: emptyList()

        val color = when {
            cards.any { it.status == com.example.settled.domain.model.CardStatus.OVERDUE } -> GlanceOverdue
            cards.any { it.status == com.example.settled.domain.model.CardStatus.DUE }     -> GlanceDue
            else -> GlancePaid
        }

        provideContent { MiniWidgetContent(color) }
    }
}

@Composable
private fun MiniWidgetContent(color: Color) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(color)
    ) {}
}
