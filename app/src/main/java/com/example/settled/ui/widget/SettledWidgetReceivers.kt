package com.example.settled.ui.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class SettledMiniWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = SettledMiniWidget()
}

class SettledStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = SettledStatusWidget()
}

class SettledDashboardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = SettledDashboardWidget()
}
