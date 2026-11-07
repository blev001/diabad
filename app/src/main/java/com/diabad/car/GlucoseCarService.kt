package com.diabad.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.domain.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Experimental connected-sensor screen; Play eligibility is not established. */
class GlucoseCarService : CarAppService() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun glucose(): GlucoseRepository
        fun settings(): SettingsRepository
    }

    override fun createHostValidator(): HostValidator = HostValidator.Builder(this)
        .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
        .build()

    override fun onCreateSession(): Session = object : Session() {
        override fun onCreateScreen(intent: Intent): Screen =
            EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java).let {
                GlucoseCarScreen(carContext, it.glucose(), it.settings())
            }
    }
}
