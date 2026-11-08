package com.diabad.car

import android.text.format.DateFormat
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.core.graphics.drawable.IconCompat
import androidx.car.app.model.CarIcon
import com.diabad.core.glucose.formatDeltaMmol
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

    init {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(glucose.observeLatest(), settings.observe(), glucose.observeHistory()) { reading, prefs, samples ->
                        Triple(reading, prefs, samples)
                    }.collect { (reading, prefs, samples) ->
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
        val display = CarGlucoseDisplay.from(latest, preferences, now)
        val pane = Pane.Builder()
            .setImage(CarIcon.Builder(IconCompat.createWithBitmap(
                CarGlucoseArtwork.render(latest, history, preferences, now),
            )).build())
            .addRow(Row.Builder().setTitle(display.value).addText(display.status).build())
        latest?.let {
            val time = DateFormat.getTimeFormat(carContext).format(Date(it.timestampMillis))
            pane.addRow(Row.Builder().setTitle("Измерено в $time")
                .addText("${display.ageMinutes} мин назад · OtTai").build())
        }
        val previous = history.previousOf(latest)
        if (latest != null && previous != null && now - latest!!.timestampMillis < preferences.connectionLossGraceMinutes * 60_000L) {
            pane.addRow(Row.Builder().setTitle("Изменение ${formatDeltaMmol(latest!!.mmol, previous.mmol)}")
                .addText("За ${(latest!!.timestampMillis - previous.timestampMillis).coerceAtLeast(0) / 60_000} мин · цель ${com.diabad.core.glucose.formatMmol(preferences.hypoThresholdMmol)}–${com.diabad.core.glucose.formatMmol(preferences.hyperThresholdMmol)}")
                .build())
        }
        return PaneTemplate.Builder(pane.build())
            .setTitle("DiaBAD · Сахар")
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}
