package com.diabad.car

import android.text.format.DateFormat
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.core.graphics.drawable.IconCompat
import androidx.car.app.model.CarIcon
import com.diabad.core.glucose.formatDeltaMmol
import com.diabad.core.glucose.formatMmol
import com.diabad.domain.model.previousOf
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.domain.repository.SettingsRepository
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GlucoseCarScreen(
    context: CarContext,
    glucose: GlucoseRepository,
    settings: SettingsRepository,
) : Screen(context) {
    private var latest: GlucoseReading? = null
    private var preferences = AppSettings()
    private var history = emptyList<GlucoseReading>()
    private var chartIcon: CarIcon? = null
    private var chartMinute = Long.MIN_VALUE
    private var chartWasStale = false

    init {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(glucose.observeLatest(), settings.observe(), glucose.observeHistory()) { reading, prefs, samples ->
                        Triple(reading, prefs, samples)
                    }.collect { (reading, prefs, samples) ->
                        chartIcon = null
                        history = samples
                        latest = reading
                        preferences = prefs
                        invalidate()
                    }
                }
                launch {
                    while (isActive) {
                        delay(30_000)
                        invalidate()
                    }
                }
            }
        }
    }

    override fun onGetTemplate(): Template {
        val now = System.currentTimeMillis()
        val reading = latest
        val display = CarGlucoseDisplay.from(reading, preferences, now)
        val measurementTime = reading?.let {
            DateFormat.getTimeFormat(carContext).format(Date(it.timestampMillis))
        }
        val previous = history.previousOf(reading)
        val change = if (reading != null && previous != null &&
            now - reading.timestampMillis < preferences.connectionLossGraceMinutes * 60_000L
        ) {
            val delta = formatDeltaMmol(reading.mmol, previous.mmol)
            val minutes = (reading.timestampMillis - previous.timestampMillis).coerceAtLeast(0) / 60_000
            "$delta за $minutes мин"
        } else {
            "Нет свежих данных для сравнения"
        }
        val points = history.filter {
            it.timestampMillis in (now - CarGlucoseArtwork.HISTORY_MS)..now && it.mmol.isFinite()
        }.sortedBy { it.timestampMillis }
        val stale = reading == null || display.ageMinutes >= preferences.connectionLossGraceMinutes
        val minute = now / 60_000L
        val artwork = chartIcon?.takeIf { chartMinute == minute && chartWasStale == stale }
            ?: CarIcon.Builder(IconCompat.createWithBitmap(
                CarGlucoseArtwork.render(reading, points, preferences, now),
            )).build().also {
                chartIcon = it
                chartMinute = minute
                chartWasStale = stale
            }
        // Keep row count and titles constant so updates are template refreshes.
        val pane = Pane.Builder()
            .setImage(artwork)
            .addRow(Row.Builder().setTitle("Глюкоза")
                .addText(display.value)
                .addText(display.status).build())
            .addRow(Row.Builder().setTitle("Динамика")
                .addText(change)
                .addText("Цель ${formatMmol(preferences.hypoThresholdMmol)}–${formatMmol(preferences.hyperThresholdMmol)} ммоль/л")
                .build())
            .addRow(Row.Builder().setTitle("Последнее измерение")
                .addText(if (measurementTime == null) "Ожидание OtTai"
                    else "$measurementTime · ${display.ageMinutes} мин назад · OtTai").build())
            .addRow(Row.Builder().setTitle("За последние 3 часа")
                .addText(if (points.size < 2) "Пока мало измерений"
                    else "Мин. ${formatMmol(points.minOf { it.mmol })} · макс. ${formatMmol(points.maxOf { it.mmol })} ммоль/л")
                .build())
        return PaneTemplate.Builder(pane.build())
            .setTitle("DiaBAD")
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}
