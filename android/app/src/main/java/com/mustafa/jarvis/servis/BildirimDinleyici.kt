package com.mustafa.jarvis.servis

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Bir bildirimin JARVIS'in okuyabileceği kısa hali. */
data class GelenBildirim(val paket: String, val uygulama: String, val baslik: String, val metin: String, val zaman: Long)

/**
 * Bildirim erişimi (14). "Bildirimlerimi oku", "Gmail'de yeni ne var?", "Instagram'dan mesaj var mı?" için
 * son 50 bildirimi YALNIZCA telefonun belleğinde tutar; hiçbir yere gönderilmez, kaydedilmez.
 * İzin: Ayarlar → Bildirim erişimi → JARVIS (izin sihirbazı oraya götürür).
 */
class BildirimDinleyici : NotificationListenerService() {
    override fun onListenerConnected() { bagli = true }
    override fun onListenerDisconnected() { bagli = false }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName || sbn.isOngoing) return
        val e = sbn.notification.extras
        val baslik = e.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val metin = (e.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: e.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (baslik.isBlank() && metin.isBlank()) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val etiket = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (_: Exception) { sbn.packageName }
        synchronized(son) {
            son.removeAll { it.paket == sbn.packageName && it.baslik == baslik && it.metin == metin }
            son.addLast(GelenBildirim(sbn.packageName, etiket, baslik, metin.take(400), sbn.postTime))
            while (son.size > 50) son.removeFirst()
        }
    }

    companion object {
        @Volatile var bagli = false
        val son = ArrayDeque<GelenBildirim>()

        fun liste(paket: String? = null, adet: Int = 5): List<GelenBildirim> = synchronized(son) {
            son.filter { paket == null || it.paket == paket }.takeLast(adet).reversed()
        }
    }
}
