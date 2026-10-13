package com.mustafa.jarvis.servis

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mustafa.jarvis.JarvisUygulama
import com.mustafa.jarvis.MainActivity
import com.mustafa.jarvis.R
import com.mustafa.jarvis.depo.Depo

/**
 * Telefon açılınca. Android 14 ve sonrası, mikrofon kullanan servisin açılışta kendiliğinden başlamasını yasaklar;
 * bu yüzden orada tek dokunuşluk bir hatırlatma bildirimi bırakılır. Daha eski sürümlerde servis direkt başlar.
 */
class AcilisAlicisi : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Depo.baslat(ctx)
        if (!Depo.bayrak(Depo.KURULUM_TAMAM)) return
        if (Build.VERSION.SDK_INT < 34) {
            JarvisServisi.baslat(ctx)
            return
        }
        val pi = PendingIntent.getActivity(ctx, 7, Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE)
        val b = Notification.Builder(ctx, JarvisUygulama.KANAL_BILGI)
            .setSmallIcon(R.drawable.ic_bildirim)
            .setContentTitle("JARVIS uyanmak için bir dokunuş bekliyor")
            .setContentText("Android, telefon açılınca mikrofonu kendiliğinden açmama izin vermiyor. Dokun, hazır olayım.")
            .setContentIntent(pi)
            .setAutoCancel(true)
        try { ctx.getSystemService(NotificationManager::class.java).notify(4101, b.build()) } catch (_: SecurityException) { }
    }
}
