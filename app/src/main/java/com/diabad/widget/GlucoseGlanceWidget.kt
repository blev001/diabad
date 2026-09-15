package com.diabad.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.diabad.MainActivity
import com.diabad.R
import com.diabad.core.glucose.GlucoseLockDisplay
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.model.GlucoseZone
import com.diabad.domain.model.isApproachingHypo
import com.diabad.domain.model.previousOf
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.domain.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class GlucoseGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SIZE_COMPACT, SIZE_WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = GlucoseWidgetSnapshot.load(context)
        provideContent {
            GlanceTheme {
                GlucoseWidgetContent(snapshot)
            }
        }
    }

    companion object {
        /** Home-screen compact pill (2×1). Lock screen uses Face Widget / keyguard provider. */
        val SIZE_COMPACT = DpSize(110.dp, 40.dp)

        /** Wider home-screen slot. */
        val SIZE_WIDE = DpSize(180.dp, 48.dp)
    }
}

class GlucoseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlucoseGlanceWidget()
}

data class GlucoseWidgetSnapshot(
    val line: String,
    val zone: GlucoseZone,
    val waiting: Boolean,
) {
    companion object {
        suspend fun load(context: Context): GlucoseWidgetSnapshot {
            val entry = EntryPointAccessors.fromApplication(
                context.applicationContext,
                GlucoseWidgetEntryPoint::class.java,
            )
            val latest = entry.glucoseRepository().observeLatest().first()
            val history = entry.glucoseRepository().observeHistory().first()
            val settings = entry.settingsRepository().observe().first()
            return from(context, latest, history.previousOf(latest), settings)
        }

        fun from(
            context: Context,
            latest: GlucoseReading?,
            previous: GlucoseReading?,
            settings: AppSettings,
        ): GlucoseWidgetSnapshot {
            if (latest == null) {
                return GlucoseWidgetSnapshot(
                    line = context.getString(R.string.widget_waiting),
                    zone = GlucoseZone.UNKNOWN,
                    waiting = true,
                )
            }
            val delta = previous?.let { latest.mmol - it.mmol }
            val zone = GlucoseZone.classify(
                latest.mmol,
                settings.hypoThresholdMmol,
                settings.hyperThresholdMmol,
            )
            val displayZone =
                if (settings.isApproachingHypo(latest.mmol)) GlucoseZone.HIGH else zone
            return GlucoseWidgetSnapshot(
                line = GlucoseLockDisplay.title(latest.mmol, latest.trend.glyph, delta),
                zone = displayZone,
                waiting = false,
            )
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GlucoseWidgetEntryPoint {
    fun glucoseRepository(): GlucoseRepository
    fun settingsRepository(): SettingsRepository
}

@Composable
private fun GlucoseWidgetContent(snapshot: GlucoseWidgetSnapshot) {
    val context = LocalContext.current
    val compact = LocalSize.current.width < 150.dp
    val accent = ColorProvider(zoneColor(snapshot.zone))
    val onPill = ColorProvider(Color.White)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_pill_background))
            .cornerRadius(100.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
            .padding(
                horizontal = if (compact) 10.dp else 16.dp,
                vertical = if (compact) 4.dp else 8.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_stat_glucose),
                contentDescription = context.getString(R.string.widget_name),
                modifier = GlanceModifier.size(if (compact) 16.dp else 22.dp),
            )
            Spacer(GlanceModifier.width(if (compact) 6.dp else 10.dp))
            Text(
                text = snapshot.line,
                style = TextStyle(
                    color = if (snapshot.waiting) onPill else accent,
                    fontSize = if (compact) 16.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }
}

private fun zoneColor(zone: GlucoseZone): Color = when (zone) {
    GlucoseZone.VERY_LOW, GlucoseZone.LOW -> Color(0xFFFF453A)
    GlucoseZone.HIGH, GlucoseZone.VERY_HIGH -> Color(0xFFFF9F0A)
    GlucoseZone.IN_RANGE -> Color(0xFF34C759)
    GlucoseZone.UNKNOWN -> Color.White
}
