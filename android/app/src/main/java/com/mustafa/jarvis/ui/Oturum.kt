package com.mustafa.jarvis.ui

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mustafa.jarvis.beyin.Mesaj
import com.mustafa.jarvis.beyin.Sarjor
import com.mustafa.jarvis.eylem.Eylemler
import com.mustafa.jarvis.ses.Dinleme
import com.mustafa.jarvis.ses.Konusma
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** JARVIS'in o anki hâli — Nebula bu duruma göre canlanır. */
enum class Hal { BEKLEME, DINLEME, DUSUNME, KONUSMA }

/**
 * Bir sesli konuşma turunu yürütür (14.0):
 * "Efendim?" → dinle → (telefon eylemi mi? yap) değilse (beyne sor) → söyle → en çok 3 kez takip et.
 * Kullanıcı 2 kez sessiz kalırsa tur biter. Ekranda altyazı ve geçmiş gösterilir.
 */
class Oturum(
    private val aktivite: Activity,
    private val kapsam: CoroutineScope,
    private val konusma: Konusma,
    private val dinleme: Dinleme,
) {
    var hal by mutableStateOf(Hal.BEKLEME)
        private set
    var altyazi by mutableStateOf("")
        private set
    var enerji by mutableStateOf(0.15f)
        private set
    val gecmis = mutableStateListOf<Mesaj>()

    private var is_: Job? = null

    private fun hald(h: Hal) {
        hal = h
        enerji = when (h) { Hal.BEKLEME -> 0.15f; Hal.DINLEME -> 0.6f; Hal.DUSUNME -> 0.75f; Hal.KONUSMA -> 1f }
    }

    /** "Jarvis" ile uyanınca (ekran arka plandan açıldığında) çağrılır. İlk söz zaten duyulduysa onu geçir. */
    fun uyan(ilkSoz: String? = null) {
        if (is_?.isActive == true) return
        is_ = kapsam.launch { tur(ilkSoz) }
    }

    /** Ekrandaki mikrofon düğmesi: anında dinlemeye geç (selam vermeden). */
    fun elleDinle() {
        if (is_?.isActive == true) return
        is_ = kapsam.launch { tur(null, selamVer = false) }
    }

    fun durdur() {
        is_?.cancel()
        konusma.sus()
        dinleme.durdur()
        hald(Hal.BEKLEME)
        altyazi = ""
    }

    private suspend fun tur(ilkSoz: String?, selamVer: Boolean = true) {
        var ilk = ilkSoz?.takeIf { it.isNotBlank() }
        var sessizlik = 0
        var adim = 0
        if (selamVer && ilk == null) {
            hald(Hal.KONUSMA); altyazi = "Efendim?"
            konusma.konus("Efendim?")
        }
        while (adim < 4) {
            adim++
            val soz = ilk ?: run {
                hald(Hal.DINLEME); altyazi = "Dinliyorum…"
                dinleme.dinle { ara -> altyazi = ara; enerji = 0.6f }
            }
            ilk = null
            if (soz.isBlank()) {
                sessizlik++
                if (sessizlik >= 2) { konusma.konus("Buralardayım efendim."); break }
                continue
            }
            sessizlik = 0
            gecmis.add(Mesaj(true, soz)); altyazi = soz

            // 1) Telefon eylemi mi?
            val eylem = Eylemler.dene(aktivite, soz)
            val cevap: String
            val kaynak: String
            if (eylem.islendi) {
                cevap = eylem.soyle; kaynak = "telefon"
            } else {
                hald(Hal.DUSUNME); altyazi = "…"
                val c = Sarjor.sor(soz)
                cevap = c.metin; kaynak = c.kaynak
            }
            if (cevap.isNotBlank()) {
                gecmis.add(Mesaj(false, cevap))
                hald(Hal.KONUSMA); altyazi = cevap
                konusma.konus(cevap)
            }
            // Döngü kendiliğinden bir tur daha dinler (en çok 3 takip): sohbet akışı sürer.
        }
        hald(Hal.BEKLEME); altyazi = ""
    }

    companion object
}
