package com.diabad.notification

import com.diabad.domain.model.GlucoseZone

/** How often the shade Kolobok should be redrawn for a given mood. */
fun kolobokAnimationIntervalMs(zone: GlucoseZone, alarming: Boolean): Long = when {
    alarming || zone == GlucoseZone.VERY_LOW -> 280L
    zone == GlucoseZone.LOW -> 360L
    zone == GlucoseZone.VERY_HIGH -> 350L
    zone == GlucoseZone.HIGH -> 420L
    else -> 700L
}
