package com.mustafa.jarvis.depo

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Ayarlar ve anahtarlar. Anahtarlar Android Keystore ile şifrelenmiş depoda durur (telefondan çıkarılamaz,
 * yedeklere girmez). Hiçbir anahtar koda gömülü değildir.
 */
object Depo {
    private var sp: SharedPreferences? = null

    fun baslat(ctx: Context) {
        if (sp != null) return
        sp = try {
            val anahtar = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(
                ctx, "jarvis_gizli", anahtar,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            ctx.getSharedPreferences("jarvis_yedek", Context.MODE_PRIVATE)
        }
    }

    fun al(ad: String, varsayilan: String = ""): String = sp?.getString(ad, varsayilan) ?: varsayilan
    fun yaz(ad: String, deger: String) { sp?.edit()?.putString(ad, deger)?.apply() }
    fun bayrak(ad: String): Boolean = sp?.getBoolean(ad, false) ?: false
    fun bayrakYaz(ad: String, deger: Boolean) { sp?.edit()?.putBoolean(ad, deger)?.apply() }

    // Bilinen ayar adları
    const val PC_ADRES = "pc_adres"           // https://....trycloudflare.com
    const val PC_ANAHTAR = "pc_anahtar"       // bilgisayarın telefon anahtarı (token)
    const val GEMINI = "gemini_anahtarlari"   // virgülle ayrılmış
    const val GROQ = "groq_anahtari"
    const val CLAUDE = "claude_anahtari"
    const val OPENAI = "openai_anahtari"
    const val SARJOR = "sarjor_sirasi"        // groq,gemini,claude,openai
    const val SES = "ses_turu"                // telefon | jarvis
    const val KURULUM_TAMAM = "kurulum_tamam"
    const val XIAOMI_ONAY = "xiaomi_onay"
    const val XIAOMI_PENCERE = "xiaomi_pencere"
    const val UYANMA = "uyanma_kapali"        // true = arka planda "Jarvis" dinleme KAPALI (varsayılan: açık)
    const val SAHIP_ADI = "sahip_adi"
}
