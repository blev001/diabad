package com.diabad.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LockStarLauncherTest {

    @Test
    fun prefersCurrentLockStarPackageThenGoodLock() {
        assertEquals(
            listOf(
                "com.samsung.systemui.lockstar",
                "com.samsung.android.app.lockstar",
                "com.samsung.android.goodlock",
            ),
            LockStarLauncher.installedPackages,
        )
    }

    @Test
    fun storeUrisPointAtLockStar() {
        val uris = LockStarLauncher.storeUris()
        assertTrue(uris.first().contains("com.samsung.systemui.lockstar"))
        assertTrue(uris.any { it.startsWith("samsungapps://") })
        assertTrue(uris.any { it.contains("galaxystore.samsung.com") })
    }
}
