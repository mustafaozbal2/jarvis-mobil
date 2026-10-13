package com.mustafa.jarvis

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.mustafa.jarvis.depo.Depo

/** Uygulama açılırken: şifreli ayar deposu ve bildirim kanalları. */
class JarvisUygulama : Application() {
    override fun onCreate() {
        super.onCreate()
        Depo.baslat(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(KANAL_SERVIS, "JARVIS dinliyor", NotificationManager.IMPORTANCE_LOW).apply {
                description = "JARVIS arka planda hazır beklerken görünen kalıcı bildirim"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(KANAL_UYAN, "JARVIS uyandı", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "\"Jarvis\" deyince kilit ekranında açılan ekran"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(KANAL_BILGI, "JARVIS bilgileri", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    companion object {
        const val KANAL_SERVIS = "jarvis_servis"
        const val KANAL_UYAN = "jarvis_uyan"
        const val KANAL_BILGI = "jarvis_bilgi"
    }
}
