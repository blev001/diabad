package com.diabad.car

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.color.ColorProvider as DayNightColorProvider
import com.diabad.core.glucose.formatMmol
import com.diabad.core.glucose.formatDeltaMmol
import com.diabad.domain.model.previousOf
import com.diabad.widget.GlucoseWidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

/** A separate responsive widget for hosts offering Android Auto's widget picker. */
class CarGlanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(
        DpSize(140.dp, 110.dp), DpSize(200.dp, 160.dp), DpSize(280.dp, 200.dp),
    ))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, GlucoseWidgetEntryPoint::class.java)
        val latest = entry.glucoseRepository().getLatest()
        val previous = entry.glucoseRepository().observeHistory().first().previousOf(latest)
        val prefs = entry.settingsRepository().observe().first()
        val now = System.currentTimeMillis()
        val display = CarGlucoseDisplay.from(latest, prefs, now)
        val stale = latest == null || now - latest.timestampMillis >= prefs.connectionLossGraceMinutes * 60_000L
        val accent = ColorProvider(when {
            stale -> Color(0xFF9DAABA)
            latest!!.mmol < prefs.hypoThresholdMmol -> Color(0xFFFF6570)
            latest.mmol > prefs.hyperThresholdMmol -> Color(0xFFFFBD61)
            prefs.approachingHypoEnabled && latest.mmol <= prefs.approachingHypoThresholdMmol -> Color(0xFFFFBD61)
            else -> Color(0xFF57E0B4)
        })
        provideContent {
            val large = LocalSize.current.height >= 150.dp
            val foreground = DayNightColorProvider(day = Color(0xFF142233), night = Color(0xFFF1F5FA))
            val secondary = DayNightColorProvider(day = Color(0xFF475B70), night = Color(0xFFB7C6D8))
            Column(
                modifier = GlanceModifier.fillMaxSize().cornerRadius(24.dp)
                    .background(DayNightColorProvider(day = Color(0xFFE7EEF5), night = Color(0xFF101925)))
                    .padding(if (large) 16.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (large) "DiaBAD · ммоль/л" else if (latest == null) "DiaBAD · OtTai" else "ммоль/л · ${display.ageMinutes} мин", style = TextStyle(color = secondary, fontSize = 12.sp), maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(latest?.let { formatMmol(it.mmol) } ?: "—", style = TextStyle(
                        color = if (stale) secondary else foreground,
                        fontSize = if (large) 48.sp else 34.sp, fontWeight = FontWeight.Bold,
                    ), maxLines = 1)
                    if (!stale) {
                        Spacer(GlanceModifier.width(8.dp))
                        Text(latest!!.trend.glyph, style = TextStyle(color = accent,
                            fontSize = if (large) 36.sp else 26.sp, fontWeight = FontWeight.Bold))
                    }
                }
                Text(display.status, style = TextStyle(color = if (stale) secondary else foreground,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                if (large) {
                    Spacer(GlanceModifier.height(5.dp))
                    Text(if (latest == null) "Ожидание OtTai" else "${display.ageMinutes} мин назад" +
                        if (!stale && previous != null) " · ${formatDeltaMmol(latest!!.mmol, previous.mmol)}" else "",
                        style = TextStyle(color = secondary, fontSize = 12.sp), maxLines = 1)
                }
                Spacer(GlanceModifier.height(6.dp))
                Box(GlanceModifier.fillMaxWidth().height(4.dp).background(accent)) {}
            }
        }
    }
}

class CarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CarGlanceWidget()
}
