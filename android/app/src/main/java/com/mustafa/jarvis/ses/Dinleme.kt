package com.mustafa.jarvis.ses

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Konuşmayı yazıya döker (Android'in Türkçe ses tanıması). Ana iş parçacığında çağrılmalı.
 * araSonuc: konuşurken ekranda görünen ara metin.
 */
class Dinleme(private val ctx: Context) {
    private var tanici: SpeechRecognizer? = null

    fun varMi(): Boolean = SpeechRecognizer.isRecognitionAvailable(ctx)

    suspend fun dinle(araSonuc: (String) -> Unit = {}): String = suspendCancellableCoroutine { dev ->
        val sr = try {
            tanici?.destroy()
            SpeechRecognizer.createSpeechRecognizer(ctx).also { tanici = it }
        } catch (e: Exception) {
            if (dev.isActive) dev.resume("")
            return@suspendCancellableCoroutine
        }
        var son = ""
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) { Seviye.deger = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { Seviye.deger = 0f; if (dev.isActive) dev.resume(son) }
            override fun onResults(results: Bundle?) {
                Seviye.deger = 0f
                val l = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (dev.isActive) dev.resume(l?.firstOrNull() ?: son)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val l = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val t = l?.firstOrNull()
                if (!t.isNullOrBlank()) { son = t; araSonuc(t) }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val niyet = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        sr.startListening(niyet)
        dev.invokeOnCancellation { try { sr.cancel() } catch (_: Exception) { } }
    }

    fun durdur() { try { tanici?.cancel() } catch (_: Exception) { } }

    fun kapat() { try { tanici?.destroy() } catch (_: Exception) { }; tanici = null }
}

/** Mikrofon ses seviyesi (0..1): Nebula küresi konuşurken nabız gibi atar. */
object Seviye {
    @Volatile var deger: Float = 0f
}
