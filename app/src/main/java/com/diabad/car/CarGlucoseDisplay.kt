package com.diabad.car

import com.diabad.core.glucose.formatMmol
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseReading

data class CarGlucoseDisplay(val value: String, val status: String, val ageMinutes: Long) {
    companion object {
        fun from(reading: GlucoseReading?, settings: AppSettings, now: Long): CarGlucoseDisplay {
            if (reading == null) return CarGlucoseDisplay(
                "Нет данных", "Ожидание OtTai. Настройте источник на телефоне после остановки.", 0,
            )
            val age = (now - reading.timestampMillis).coerceAtLeast(0) / 60_000
            val stale = age >= settings.connectionLossGraceMinutes
            val value = "${formatMmol(reading.mmol)} ммоль/л"
            val status = when {
                stale -> "Нет свежих данных — последнее измерение"
                reading.mmol < settings.hypoThresholdMmol -> "Низкий сахар"
                reading.mmol > settings.hyperThresholdMmol -> "Высокий сахар"
                settings.approachingHypoEnabled && reading.mmol <= settings.approachingHypoThresholdMmol ->
                    "Приближение к низкому сахару"
                else -> "В целевом диапазоне"
            }
            return CarGlucoseDisplay(if (stale) value else "$value ${reading.trend.glyph}", status, age)
        }
    }
}
