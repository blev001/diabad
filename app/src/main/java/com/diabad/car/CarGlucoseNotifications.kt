package com.diabad.car

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.car.app.connection.CarConnection
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.lifecycle.Observer
import com.diabad.MainActivity
import com.diabad.R
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseReading
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarGlucoseNotifications @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val connection = CarConnection(context).type
    private val policy = CarNotificationPolicy()
    private var connected = false
    private var latest: GlucoseReading? = null
    private var settings = AppSettings()
    private var postedSample: Long? = null
    private var observing = false
    private var actions = emptyList<PendingIntent>()
    private val observer = Observer<Int> { type ->
        synchronized(this) {
            connected = type == CarConnection.CONNECTION_TYPE_PROJECTION
            refresh()
        }
    }

    // Called from the monitoring service lifecycle on the main thread.
    fun start() {
        if (observing) return
        observing = true
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL, "Сахар в Android Auto", NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Карточки сахара во время подключения к Android Auto"
            setSound(null, null)
            enableVibration(false)
        })
        connection.observeForever(observer)
    }

    fun stop() {
        connection.removeObserver(observer)
        observing = false
        synchronized(this) {
            connected = false
            refresh()
        }
    }

    @Synchronized
    fun update(reading: GlucoseReading?, preferences: AppSettings) {
        latest = reading
        settings = preferences
        refresh()
    }

    @Synchronized
    fun testNotification(reading: GlucoseReading?, preferences: AppSettings): String {
        if (!connected) return "Сначала подключите Android Auto к автомобилю"
        if (!preferences.carNotificationsEnabled) return "Сначала включите карточки сахара"
        if (reading == null) return "Нет измерений OtTai для проверки"
        if (!manager.areNotificationsEnabled() || manager.getNotificationChannel(CHANNEL)?.importance == 0) {
            return "Разрешите уведомления DiaBAD и канал «Сахар в Android Auto»"
        }
        latest = reading
        settings = preferences
        policy.reset()
        refresh()
        return "Карточка отправлена. Проверьте уведомления Android Auto"
    }

    @Synchronized
    fun dismiss(sample: Long) {
        // A delayed action for the previous sample must not remove the new one.
        if (postedSample == sample) {
            clearPosted()
        }
    }

    private fun refresh() {
        val reading = latest
        val now = System.currentTimeMillis()
        val post = policy.shouldPost(connected, reading, settings, now)
        if (!connected || !settings.carNotificationsEnabled || reading == null) {
            clearPosted()
            return
        }
        if (!post) return
        val display = CarGlucoseDisplay.from(reading, settings, now)
        val time = DateFormat.getTimeFormat(context).format(Date(reading.timestampMillis))
        val body = "${display.value} · ${display.status}. Измерено в $time, ${display.ageMinutes} мин назад."
        val sender = Person.Builder().setName("DiaBAD · Сахар").setKey("diabad-sensor").setBot(true).build()
        val style = NotificationCompat.MessagingStyle(Person.Builder().setName("Водитель").build())
            .addMessage(body, now, sender)
        actions.forEach { it.cancel() }
        val read = actionIntent(CarNotificationActionService.ACTION_READ, reading.timestampMillis, 460,
            PendingIntent.FLAG_IMMUTABLE)
        val reply = actionIntent(CarNotificationActionService.ACTION_REPLY, reading.timestampMillis, 461,
            PendingIntent.FLAG_MUTABLE)
        actions = listOf(read, reply)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_glucose)
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setLocalOnly(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(PendingIntent.getActivity(context, 462, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .addInvisibleAction(NotificationCompat.Action.Builder(0, "Прочитано", read)
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                .setShowsUserInterface(false).build())
            .addAction(NotificationCompat.Action.Builder(0, "Подтвердить", reply)
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                .setShowsUserInterface(false)
                .addRemoteInput(RemoteInput.Builder("ack").setLabel("Подтвердить получение").build())
                .build())
            .build()
        try {
            manager.notify(ID, notification)
            postedSample = reading.timestampMillis
        } catch (_: SecurityException) {
            // Notification permission may be revoked while the service is alive.
        }
    }

    private fun clearPosted() {
        manager.cancel(ID)
        postedSample = null
        actions.forEach { it.cancel() }
        actions = emptyList()
    }

    private fun actionIntent(action: String, sample: Long, code: Int, mutability: Int): PendingIntent =
        PendingIntent.getService(context, code,
            Intent(context, CarNotificationActionService::class.java).setAction(action)
                .setData(android.net.Uri.parse("diabad://car/$action/$sample"))
                .putExtra(CarNotificationActionService.EXTRA_SAMPLE, sample),
            PendingIntent.FLAG_UPDATE_CURRENT or mutability)

    companion object {
        const val ID = 1046
        private const val CHANNEL = "diabad_car_glucose"
    }
}
