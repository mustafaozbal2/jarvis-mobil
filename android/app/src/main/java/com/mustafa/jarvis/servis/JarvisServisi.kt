package com.mustafa.jarvis.servis

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.mustafa.jarvis.JarvisUygulama
import com.mustafa.jarvis.MainActivity
import com.mustafa.jarvis.R
import com.mustafa.jarvis.depo.Depo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume

/** Uygulama ile servis arasında paylaşılan anlık durum. */
object Durum {
    /** JARVIS ekranı açıkken mikrofonu ekran kullanır; servis kendi dinlemesini bırakır. */
    @Volatile var onPlanda = false
    @Volatile var servisCalisiyor = false
    @Volatile var sonDuyulan = ""
}

/**
 * ÖN PLAN SERVİSİ (14). Uygulama kapalıyken, ekran kilitliyken bile "Jarvis"i bekler.
 * Android kuralı gereği bildirim çubuğunda kalıcı "JARVIS hazır" bildirimi ve mikrofon simgesi (yeşil nokta) görünür;
 * bu gizlenemez, gizlenebilseydi her casus uygulama da gizlerdi.
 *
 * 14.0 uyanma motoru: Android'in ses tanımasıyla döngüsel dinleme ("Jarvis" geçerse uyanır).
 * 14.1'de bunun yerine çevrimdışı, pil dostu Porcupine "jarvis" motoru gelecek (ekran kapalıyken de kesin çalışır).
 */
class JarvisServisi : Service() {
    private val kapsam = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tanici: SpeechRecognizer? = null
    private var dongu: Job? = null
    private var hataSayisi = 0
    private var susBitis = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            EYLEM_DURDUR -> { onPlanaGec(); durdur(); return START_NOT_STICKY }
            EYLEM_KONUS -> { onPlanaGec(); Uyandirici.uyandir(this) }
            EYLEM_KILIT_TESTI -> kapsam.launch {
                bildirimGuncelle("5 saniye içinde ekranı kilitle — JARVIS kendisi açacak.")
                delay(5_000)
                Uyandirici.uyandir(this@JarvisServisi)
                delay(1_500)
                bildirimGuncelle(null)
            }
        }
        if (!onPlanaGec()) return START_NOT_STICKY
        Durum.servisCalisiyor = true
        if (dongu?.isActive != true) dongu = kapsam.launch { uyanmaDongusu() }
        return START_STICKY
    }

    /** Ön plan bildirimini kurar. Mikrofon izni yoksa (ya da Android izin vermezse) false. */
    private fun onPlanaGec(): Boolean {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf(); return false
        }
        return try {
            if (Build.VERSION.SDK_INT >= 30) {
                startForeground(BILDIRIM_ID, bildirim(null), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(BILDIRIM_ID, bildirim(null))
            }
            true
        } catch (e: Exception) {
            // Android 14+: mikrofon servisi arka plandan (ör. açılışta) başlatılamaz → kullanıcı uygulamayı bir kez açmalı
            Log.w("JARVIS", "Ön plan servisi başlatılamadı: ${e.message}")
            stopSelf(); false
        }
    }

    private fun bildirim(not: String?): Notification {
        val ac = PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        fun eylem(ad: String, kod: Int) = PendingIntent.getService(this, kod,
            Intent(this, JarvisServisi::class.java).setAction(ad), PendingIntent.FLAG_IMMUTABLE)
        val metin = not ?: if (Depo.bayrak(Depo.UYANMA)) "Arka plan dinleme kapalı. Konuş'a dokun."
                           else "\"Jarvis\" demen yeterli, efendim."
        return Notification.Builder(this, JarvisUygulama.KANAL_SERVIS)
            .setSmallIcon(R.drawable.ic_bildirim)
            .setContentTitle("JARVIS hazır")
            .setContentText(metin)
            .setContentIntent(ac)
            .setOngoing(true)
            .setShowWhen(false)
            .addAction(Notification.Action.Builder(null, "Konuş", eylem(EYLEM_KONUS, 2)).build())
            .addAction(Notification.Action.Builder(null, "Kilit testi", eylem(EYLEM_KILIT_TESTI, 3)).build())
            .addAction(Notification.Action.Builder(null, "Durdur", eylem(EYLEM_DURDUR, 4)).build())
            .build()
    }

    private fun bildirimGuncelle(not: String?) {
        try { getSystemService(android.app.NotificationManager::class.java).notify(BILDIRIM_ID, bildirim(not)) } catch (_: Exception) { }
    }

    /** Ekran JARVIS'teyken bekler; değilken sürekli kısa dinleme turları atar. */
    private suspend fun uyanmaDongusu() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            bildirimGuncelle("Bu telefonda ses tanıma yok; Google uygulamasını güncelle.")
            return
        }
        while (true) {
            if (Durum.onPlanda || Depo.bayrak(Depo.UYANMA) || System.currentTimeMillis() < susBitis) {
                taniciKapat(); delay(700); continue
            }
            val duyulan = birTur()
            if (duyulan == null) {                         // hata: yavaşla (meşgul/ağ sorunu)
                hataSayisi++
                delay((400L * hataSayisi).coerceAtMost(8_000L))
                continue
            }
            hataSayisi = 0
            if (duyulan.isNotBlank() && UyanmaKelimesi.iceriyor(duyulan)) {
                Durum.sonDuyulan = UyanmaKelimesi.temizle(duyulan)
                susBitis = System.currentTimeMillis() + 8_000  // etkinlik açılıp mikrofonu alana kadar sus
                taniciKapat()
                Uyandirici.uyandir(this)
                delay(2_500)
            }
        }
    }

    /** Bir dinleme turu: duyulan metin, sessizlikte "", hatada null. */
    private suspend fun birTur(): String? = kotlinx.coroutines.suspendCancellableCoroutine { dev ->
        val sr = tanici ?: try { SpeechRecognizer.createSpeechRecognizer(this).also { tanici = it } } catch (e: Exception) {
            dev.resume(null); return@suspendCancellableCoroutine
        }
        var son = ""
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(p: Bundle?) {
                val t = p?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                son = t
                // Ara sonuçta "Jarvis" yakalandıysa beklemeden uyan (gecikme ~0.5 sn)
                if (UyanmaKelimesi.iceriyor(t) && dev.isActive) { try { sr.cancel() } catch (_: Exception) { }; dev.resume(t) }
            }
            override fun onResults(r: Bundle?) {
                val t = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: son
                if (dev.isActive) dev.resume(t)
            }
            override fun onError(error: Int) {
                if (!dev.isActive) return
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> dev.resume(son)
                    else -> { taniciKapat(); dev.resume(null) }
                }
            }
        })
        val niyet = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try { sr.startListening(niyet) } catch (e: Exception) { taniciKapat(); if (dev.isActive) dev.resume(null) }
        dev.invokeOnCancellation { try { sr.cancel() } catch (_: Exception) { } }
    }

    private fun taniciKapat() {
        try { tanici?.destroy() } catch (_: Exception) { }
        tanici = null
    }

    private fun durdur() {
        Durum.servisCalisiyor = false
        dongu?.cancel()
        taniciKapat()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        Durum.servisCalisiyor = false
        taniciKapat()
        kapsam.cancel()
        super.onDestroy()
    }

    companion object {
        const val BILDIRIM_ID = 4100
        const val EYLEM_KONUS = "jarvis.KONUS"
        const val EYLEM_KILIT_TESTI = "jarvis.KILIT_TESTI"
        const val EYLEM_DURDUR = "jarvis.DURDUR"

        fun baslat(ctx: Context, eylem: String? = null) {
            val i = Intent(ctx, JarvisServisi::class.java).apply { if (eylem != null) action = eylem }
            try { ctx.startForegroundService(i) } catch (e: Exception) { Log.w("JARVIS", "Servis başlatılamadı: ${e.message}") }
        }

        fun durdur(ctx: Context) { ctx.stopService(Intent(ctx, JarvisServisi::class.java)) }
    }
}
