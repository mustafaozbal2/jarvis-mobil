package com.mustafa.jarvis.ses

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.mustafa.jarvis.beyin.Sarjor
import com.mustafa.jarvis.depo.Depo
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Konuşma (TTS). "jarvis" ses türünde bilgisayardaki JARVIS'in sesi (Ses Stüdyosu'nda seçtiğin ses) çalınır;
 * bilgisayar yoksa ya da "telefon" seçiliyse telefonun Türkçe sesi kullanılır. Kesilebilir: sus().
 */
class Konusma(private val ctx: Context) {
    private var tts: TextToSpeech? = null
    private var hazir = false
    private var oynatici: MediaPlayer? = null
    @Volatile private var surum = 0

    init {
        tts = TextToSpeech(ctx.applicationContext) { durum ->
            if (durum == TextToSpeech.SUCCESS) {
                val tr = Locale("tr", "TR")
                tts?.language = tr
                tts?.voices?.filter { it.locale.language == "tr" && !it.isNetworkConnectionRequired }
                    ?.sortedBy { if (it.name.contains("female", true)) 0 else 1 }
                    ?.firstOrNull()?.let { tts?.voice = it }
                tts?.setSpeechRate(1.03f)
                hazir = true
            }
        }
    }

    fun sus() {
        surum++
        try { tts?.stop() } catch (_: Exception) { }
        try { oynatici?.stop(); oynatici?.release() } catch (_: Exception) { }
        oynatici = null
    }

    suspend fun konus(metin: String) {
        val t = temizle(metin)
        if (t.isEmpty()) return
        sus()
        val benim = ++surum
        if (Depo.al(Depo.SES, "jarvis") == "jarvis") {
            val ses = Sarjor.pcSesi(t)
            if (ses != null && ses.size > 200 && benim == surum) {
                if (cal(ses, benim)) return
            }
        }
        if (benim != surum) return
        telefonSesi(t, benim)
    }

    private suspend fun cal(veri: ByteArray, benim: Int): Boolean {
        val dosya = File(ctx.cacheDir, "jarvis_ses_${System.nanoTime()}")
        return try {
            dosya.writeBytes(veri)
            suspendCancellableCoroutine<Boolean> { dev ->
                val mp = MediaPlayer()
                oynatici = mp
                mp.setDataSource(dosya.absolutePath)
                mp.setOnCompletionListener { if (dev.isActive) dev.resume(true) }
                mp.setOnErrorListener { _, _, _ -> if (dev.isActive) dev.resume(false); true }
                mp.prepare()
                if (benim != surum) { if (dev.isActive) dev.resume(true); return@suspendCancellableCoroutine }
                mp.start()
                dev.invokeOnCancellation { try { mp.stop() } catch (_: Exception) { } }
            }
        } catch (e: Exception) {
            false
        } finally {
            try { oynatici?.release() } catch (_: Exception) { }
            oynatici = null
            dosya.delete()
        }
    }

    private suspend fun telefonSesi(t: String, benim: Int) {
        val motor = tts ?: return
        var bekle = 0
        while (!hazir && bekle < 30) { kotlinx.coroutines.delay(100); bekle++ }
        if (!hazir || benim != surum) return
        suspendCancellableCoroutine<Unit> { dev ->
            val id = "j$benim"
            motor.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { if (utteranceId == id && dev.isActive) dev.resume(Unit) }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { if (dev.isActive) dev.resume(Unit) }
                override fun onStop(utteranceId: String?, interrupted: Boolean) { if (dev.isActive) dev.resume(Unit) }
            })
            motor.speak(t, TextToSpeech.QUEUE_FLUSH, null, id)
            dev.invokeOnCancellation { motor.stop() }
        }
    }

    fun kapat() {
        sus()
        tts?.shutdown()
        tts = null
    }

    companion object {
        fun temizle(t: String): String = t
            .replace(Regex("```[\\s\\S]*?```"), " ")
            .replace(Regex("\\[[A-ZÇĞİÖŞÜ_]+:?[^\\]]*]"), " ")
            .replace(Regex("[*#_`>|]"), "")
            .replace(Regex("https?://\\S+"), "bağlantı")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(900)
    }
}
