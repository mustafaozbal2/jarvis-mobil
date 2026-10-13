package com.mustafa.jarvis.servis

import java.util.Locale

/**
 * "Jarvis" ve Türkçe tanımanın onu yanlış duyduğu halleri (bilgisayardaki jarvis_benzeri ile aynı mantık).
 * Yalnızca "Jarvis" — "Hey Jarvis" gerekmez.
 */
object UyanmaKelimesi {
    private val TR = Locale("tr", "TR")
    private val KOKLER = listOf(
        "jarvis", "jarviz", "jarvıs", "carvis", "cervis", "jervis", "jerviz", "jarv", "jerv", "carv",
        "ceviz", "servis", "çarviş", "jarbis", "garvis", "harvis", "yarvis", "javis", "jarves", "jarwis", "charvis"
    )

    fun iceriyor(metin: String): Boolean {
        val k = metin.lowercase(TR).split(Regex("[^\\p{L}]+")).filter { it.isNotEmpty() }
        return k.any { kelime -> KOKLER.any { kelime.startsWith(it) } }
    }

    /** Baştaki "Jarvis" kelimelerini atar: "Jarvis Spotify'ı aç" → "Spotify'ı aç". */
    fun temizle(metin: String): String {
        var k = metin.trim().split(Regex("\\s+")).toMutableList()
        while (k.isNotEmpty() && iceriyor(k.first()) && k.first().length <= 9) k = k.drop(1).toMutableList()
        return k.joinToString(" ").trim(' ', ',', '.', '!', '?')
    }
}
