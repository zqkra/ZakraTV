package com.zakratv.app.player

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Runs the REAL Android Activity lifecycle on the JVM (Robolectric) to functionally verify:
 *  - the app manifest declares WAKE_LOCK (the missing permission that made playback crash), and
 *  - opening the player with a real Real-Debrid style URL never throws / closes the activity.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PlayerActivityRobolectricTest {

    @Test
    fun manifestDeclaresWakeLock_soPlaybackDoesNotSecurityException() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val perms = app.packageManager
            .getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions ?: emptyArray()
        assertTrue(
            "WAKE_LOCK debe estar en el manifest: ExoPlayer setWakeMode(WAKE_MODE_NETWORK) " +
                "llama a wakeLock.acquire() y sin el permiso lanza SecurityException al reproducir.",
            perms.contains(android.Manifest.permission.WAKE_LOCK),
        )
    }

    @Test
    fun openingPlayerWithRealDebridStyleUrlDoesNotCrash() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val intent = Intent(ctx, PlayerActivity::class.java).apply {
            putExtra(
                PlayerActivity.EXTRA_URL,
                "https://bgt1-4.download.real-debrid.com/d/ABCDEF/Batman.LATINO.1080p.mkv",
            )
            putExtra(PlayerActivity.EXTRA_TITLE, "Prueba Latino")
        }
        // setup() drives onCreate -> onStart -> onResume for real.
        val controller = Robolectric.buildActivity(PlayerActivity::class.java, intent).setup()
        val activity = controller.get()
        assertNotNull("La activity del reproductor debe crearse", activity)
        assertFalse(
            "Abrir una película NUNCA debe cerrar la app (activity finishing)",
            activity.isFinishing,
        )
        // Pump the main looper so any posted player callbacks run without crashing.
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        controller.pause().stop().destroy()
    }

    @Test
    fun blankUrlShowsErrorButDoesNotCrash() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val intent = Intent(ctx, PlayerActivity::class.java) // sin EXTRA_URL
        val controller = Robolectric.buildActivity(PlayerActivity::class.java, intent).setup()
        assertNotNull(controller.get())
        assertFalse(controller.get().isFinishing)
    }
}
