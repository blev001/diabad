package com.diabad.car

import android.text.format.DateFormat
import androidx.car.app.CarContext
import androidx.car.app.Screen
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

    init {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(glucose.observeLatest(), settings.observe()) { reading, prefs ->
                        reading to prefs
                    }.collect { (reading, prefs) ->
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
        val display = CarGlucoseDisplay.from(latest, preferences, System.currentTimeMillis())
        val pane = Pane.Builder()
            .addRow(Row.Builder().setTitle(display.value).addText(display.status).build())
        latest?.let {
            val time = DateFormat.getTimeFormat(carContext).format(Date(it.timestampMillis))
            pane.addRow(Row.Builder().setTitle("Измерено в $time")
                .addText("${display.ageMinutes} мин назад · OtTai").build())
        }
        return PaneTemplate.Builder(pane.build())
            .setTitle("DiaBAD · Сахар")
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}
