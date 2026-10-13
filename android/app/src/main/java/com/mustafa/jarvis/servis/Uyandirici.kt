package com.mustafa.jarvis.servis

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import com.mustafa.jarvis.JarvisUygulama
import com.mustafa.jarvis.MainActivity
import com.mustafa.jarvis.R

/**
 * KİLİT EKRANI PROTOKOLÜ (14 · onaylı protokol 1).
 * "Jarvis" duyulunca: ekranı yakan WakeLock (ACQUIRE_CAUSES_WAKEUP) → JARVIS ekranı kilidin ÜSTÜNDE açılır
 * (MainActivity: setShowWhenLocked + setTurnScreenOn) → Nebula döner, JARVIS "Efendim?" der.
 * Arka plandan ekran açmanın iki yolu:
 *   a) "Diğer uygulamaların üstünde göster" izni varsa etkinlik doğrudan başlatılır (Android bu izni muafiyet sayar).
 *   b) Yoksa tam ekran bildirim (full-screen intent): kilitliyken arama ekranı gibi direkt açılır.
 */
object Uyandirici {
    const val EKSTRA_UYANDIR = "jarvis_uyandir"
    private const val BILDIRIM_ID = 4114

    @SuppressLint("WakelockTimeout")
    @Suppress("DEPRECATION")
    fun uyandir(ctx: Context) {
        val app = ctx.applicationContext
        // 1) Ekranı yak (10 sn; etkinlik açılınca kendi FLAG_KEEP_SCREEN_ON'u devralır)
        try {
            val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "jarvis:uyan"
            ).acquire(10_000L)
        } catch (_: Exception) { }

        val niyet = Intent(app, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(EKSTRA_UYANDIR, true)
        }
        // 2a) Doğrudan aç
        var acildi = false
        if (Settings.canDrawOverlays(app)) {
            try { app.startActivity(niyet); acildi = true } catch (_: Exception) { }
        }
        // 2b) Tam ekran bildirim (kilitliyken ekranı kaplar; açıkken üstten kayar, dokununca açılır)
        val km = app.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!acildi || km.isKeyguardLocked) {
            val pi = PendingIntent.getActivity(
                app, 41, niyet, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val b = Notification.Builder(app, JarvisUygulama.KANAL_UYAN)
                .setSmallIcon(R.drawable.ic_bildirim)
                .setContentTitle("JARVIS")
                .setContentText("Efendim?")
                .setCategory(Notification.CATEGORY_CALL)
                .setFullScreenIntent(pi, true)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setTimeoutAfter(15_000L)
            try {
                app.getSystemService(NotificationManager::class.java).notify(BILDIRIM_ID, b.build())
            } catch (_: SecurityException) { }
        }
    }

    fun bildirimiKaldir(ctx: Context) {
        try { ctx.getSystemService(NotificationManager::class.java).cancel(BILDIRIM_ID) } catch (_: Exception) { }
    }
}
