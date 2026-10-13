package com.mustafa.jarvis.eylem

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.mustafa.jarvis.servis.BildirimDinleyici
import java.util.Locale

/** Bir eylemin sonucu: JARVIS'in sesli söyleyeceği kısa cümle; iş bittiyse artık cevap beklenmez. */
data class EylemSonucu(val soyle: String, val islendi: Boolean)

/**
 * TELEFON EYLEMLERİ (14.0). Cümle bir telefon işine uyuyorsa onu yapar; uymuyorsa islendi=false döner
 * ve konuşma Sarjor'a (beyin) gider. Uygulama açmalar kilitliyse requestDismissKeyguard'dan sonra yapılır.
 */
object Eylemler {
    private val TR = Locale("tr", "TR")

    private val PAKETLER = mapOf(
        "spotify" to "com.spotify.music",
        "instagram" to "com.instagram.android",
        "whatsapp" to "com.whatsapp",
        "youtube" to "com.google.android.youtube",
        "telegram" to "org.telegram.messenger",
        "gmail" to "com.google.android.gm",
        "chrome" to "com.android.chrome",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android",
        "tiktok" to "com.zhiliaoapp.musically",
        "ayarlar" to "ACTION_SETTINGS",
        "kamera" to "ACTION_CAMERA"
    )

    fun dene(aktivite: Activity, cumle: String): EylemSonucu {
        val t = cumle.lowercase(TR).trim()
        if (t.isBlank()) return EylemSonucu("", false)

        // ——— Bildirimleri oku
        if (Regex("bildirim").containsMatchIn(t) && Regex("oku|neler|ne var|göster|söyle|bak").containsMatchIn(t)) {
            return bildirimleriOku(aktivite, t)
        }

        // ——— Spotify'da X çal
        Regex("spotify'?(?:da|de|ta|te)?\\s+(.+?)\\s*(?:çal|aç|dinlet|başlat)").find(t)?.let {
            val parca = it.groupValues[1].trim()
            return spotifyCal(aktivite, parca)
        }
        if (Regex("(?:bir )?(?:şarkı|müzik) (?:çal|aç)").containsMatchIn(t)) return spotifyCal(aktivite, "")

        // ——— X'i ara (kişi)
        Regex("(.+?)['’]?(?:i|ı|u|ü|yi|yı|yu|yü)?\\s*ara(?:r mısın)?$").find(t)?.let {
            val kelimeler = it.groupValues[1].trim()
            if (kelimeler.isNotBlank() && !Regex("^(beni|interneti|google)").containsMatchIn(kelimeler)) {
                return kisiAra(aktivite, kelimeler)
            }
        }

        // ——— X'i aç (uygulama)
        Regex("(.+?)['’]?(?:ı|i|u|ü|yı|yi|yu|yü|'ı|'i)?\\s*(?:aç|başlat|çalıştır)$").find(t)?.let {
            val ad = it.groupValues[1].trim().trim('\'', '’', ' ')
            uygulamaAc(aktivite, ad)?.let { s -> return s }
        }

        return EylemSonucu("", false)
    }

    private fun kilidiAc(aktivite: Activity) {
        try {
            val km = aktivite.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            if (km.isKeyguardLocked) km.requestDismissKeyguard(aktivite, null)
        } catch (_: Exception) { }
    }

    private fun baslat(aktivite: Activity, niyet: Intent): Boolean {
        return try {
            kilidiAc(aktivite)
            niyet.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            aktivite.startActivity(niyet)
            true
        } catch (e: ActivityNotFoundException) { false } catch (e: Exception) { false }
    }

    private fun uygulamaAc(aktivite: Activity, ad: String): EylemSonucu? {
        val anahtar = PAKETLER.keys.firstOrNull { ad.contains(it) } ?: return null
        when (val paket = PAKETLER[anahtar]) {
            "ACTION_SETTINGS" -> { baslat(aktivite, Intent(android.provider.Settings.ACTION_SETTINGS)); return EylemSonucu("Ayarları açıyorum efendim.", true) }
            "ACTION_CAMERA" -> {
                val ok = baslat(aktivite, Intent("android.media.action.STILL_IMAGE_CAMERA"))
                return EylemSonucu(if (ok) "Kamera açılıyor." else "Kamerayı açamadım.", true)
            }
            else -> {
                val niyet = aktivite.packageManager.getLaunchIntentForPackage(paket!!)
                return if (niyet != null && baslat(aktivite, niyet))
                    EylemSonucu("${anahtar.replaceFirstChar { it.uppercase(TR) }} açılıyor efendim.", true)
                else EylemSonucu("$anahtar telefonda kurulu değil gibi görünüyor efendim.", true)
            }
        }
    }

    private fun spotifyCal(aktivite: Activity, parca: String): EylemSonucu {
        val paket = "com.spotify.music"
        if (parca.isNotBlank()) {
            val ara = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:" + Uri.encode(parca))).setPackage(paket)
            if (baslat(aktivite, ara)) return EylemSonucu("Spotify'da \"$parca\" aratıyorum efendim.", true)
        }
        val niyet = aktivite.packageManager.getLaunchIntentForPackage(paket)
        return if (niyet != null && baslat(aktivite, niyet))
            EylemSonucu(if (parca.isBlank()) "Spotify açılıyor efendim." else "Spotify'ı açtım; \"$parca\" diye aratabilirsin.", true)
        else EylemSonucu("Spotify kurulu değil gibi efendim.", true)
    }

    /** Kişi adını rehberde arar; numarayı bulursa aramayı başlatır (CALL_PHONE izni varsa), yoksa arama ekranını açar. */
    private fun kisiAra(aktivite: Activity, isim: String): EylemSonucu {
        val numara = rehberdenBul(aktivite, isim)
        if (numara == null) {
            // Rehberde yoksa: numara tuşlanmış arama ekranını aç (kullanıcı onaylasın)
            return EylemSonucu("$isim adını rehberde bulamadım efendim.", true)
        }
        val izinli = aktivite.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val eylem = if (izinli) Intent.ACTION_CALL else Intent.ACTION_DIAL
        val ok = baslat(aktivite, Intent(eylem, Uri.parse("tel:" + Uri.encode(numara))))
        return EylemSonucu(if (ok) "$isim aranıyor efendim." else "Aramayı başlatamadım efendim.", true)
    }

    private fun rehberdenBul(ctx: Context, isim: String): String? {
        if (ctx.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return null
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val sutun = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER)
        return try {
            ctx.contentResolver.query(uri, sutun,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?", arrayOf("%$isim%"), null)?.use { c ->
                if (c.moveToFirst()) c.getString(1) else null
            }
        } catch (e: Exception) { null }
    }

    private fun bildirimleriOku(ctx: Context, t: String): EylemSonucu {
        if (!BildirimDinleyici.bagli) {
            return EylemSonucu("Bildirimleri okuyabilmem için bildirim erişimi iznin lazım efendim; Ayarlar'dan açabilirsin.", true)
        }
        val paket = when {
            t.contains("instagram") -> "com.instagram.android"
            t.contains("whatsapp") -> "com.whatsapp"
            t.contains("gmail") || t.contains("mail") || t.contains("e-posta") || t.contains("eposta") -> "com.google.android.gm"
            else -> null
        }
        val liste = BildirimDinleyici.liste(paket, 5)
        if (liste.isEmpty()) return EylemSonucu("Yeni bir bildirim göremiyorum efendim.", true)
        val sb = StringBuilder(if (liste.size == 1) "Bir bildirim var efendim: " else "Son ${liste.size} bildirim efendim: ")
        liste.forEachIndexed { i, b ->
            sb.append("${b.uygulama}'dan ${b.baslik}")
            if (b.metin.isNotBlank()) sb.append(": ${b.metin}")
            sb.append(if (i == liste.lastIndex) "." else " · ")
        }
        return EylemSonucu(sb.toString(), true)
    }
}
