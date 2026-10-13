package com.mustafa.jarvis.beyin

import com.mustafa.jarvis.depo.Depo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Konuşma geçmişindeki bir satır. */
data class Mesaj(val benden: Boolean, val metin: String)

/** Bir sağlayıcının cevabı ve kimin verdiği. */
data class Cevap(val metin: String, val kaynak: String)

class SaglayiciHatasi(val kod: Int, mesaj: String) : Exception(mesaj)

/**
 * Çok sağlayıcılı şarjör (14). Sıra: [bilgisayardaki JARVIS (eşleşmişse)] → Groq → Gemini → Claude → OpenAI.
 * Yalnızca anahtarı olanlar denenir. Biri 429/403/401/5xx/zaman aşımı verirse anında sıradakine geçilir.
 * Bilgisayardaki JARVIS önce gelir çünkü onun hafızası, takvimi ve bilgisayar komutları var.
 */
object Sarjor {
    private val JSON_TUR = "application/json; charset=utf-8".toMediaType()
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(75, TimeUnit.SECONDS)
        .build()
    private val gecmis = ArrayDeque<Mesaj>()

    /** Bilgisayardaki JARVIS'e: kısa bağlantı süresi (kapalıysa çabuk vazgeç). */
    private val pcHttp: OkHttpClient = http.newBuilder().connectTimeout(4, TimeUnit.SECONDS).readTimeout(45, TimeUnit.SECONDS).build()

    /** Bilgisayar ulaşılamazsa bu zamana kadar hiç denenmez (her soruda boşuna beklemesin). */
    @Volatile private var pcAtlaBitis = 0L

    fun sistem(): String =
        "Senin adın JARVIS: zeki, sıcak, zarif ve yer yer esprili, kadın sesiyle konuşan kişisel bir yapay zekâ asistanısın. " +
            "Telefonun içinde yaşıyorsun. Kullanıcıya 'efendim' diye hitap et; 'patron', 'kaptan' gibi lakaplar ASLA kullanma. " +
            "Cevapların sesli okunur: kısa, doğal Türkçe (çoğu zaman 1-3 cümle), madde/başlık/emoji kullanma. " +
            "Bilmediğini uydurma. Bir işi yapmadıysan yaptım deme."

    /** Bilgisayardaki JARVIS'e özgü işler: bunlarda bilgisayar ÖNCE denenir; normal sohbette en sona kalır (hız). */
    private val PC_ISI = Regex(
        "bilgisayar|laptop|\\bpc\\b|masaüstü|masa üstü|ekran|dosya|klasör|belge|word|excel|powerpoint|sunum|" +
            "chrome|youtube'?(u|da)? aç|indir|kaydet|hatırla|hatırlat|takvim|whatsapp'?(tan|a)? (yaz|gönder)|teşhis|kapat",
        RegexOption.IGNORE_CASE
    )

    fun siradakiler(metin: String = ""): List<String> {
        val sira = Depo.al(Depo.SARJOR, "groq,gemini,claude,openai").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val out = mutableListOf<String>()
        val pcVar = Depo.al(Depo.PC_ADRES).isNotEmpty() && Depo.al(Depo.PC_ANAHTAR).isNotEmpty() &&
            System.currentTimeMillis() > pcAtlaBitis
        val pcOnce = pcVar && (misafirMi() || PC_ISI.containsMatchIn(metin))
        if (pcOnce) out.add("pc")
        for (ad in sira) {
            val var_ = when (ad) {
                "groq" -> Depo.al(Depo.GROQ).isNotEmpty()
                "gemini" -> Depo.al(Depo.GEMINI).isNotEmpty()
                "claude" -> Depo.al(Depo.CLAUDE).isNotEmpty()
                "openai" -> Depo.al(Depo.OPENAI).isNotEmpty()
                else -> false
            }
            if (var_ && ad !in out) out.add(ad)
        }
        if (pcVar && "pc" !in out) out.add("pc")      // yedek: diğerleri olmazsa bilgisayar
        return out
    }

    suspend fun sor(metin: String): Cevap = withContext(Dispatchers.IO) {
        val sira = siradakiler(metin)
        if (sira.isEmpty()) {
            return@withContext Cevap(
                "Henüz bir beynim yok efendim. Ayarlardan bilgisayarla eşleş ya da bir yapay zekâ anahtarı ekle.", "yok"
            )
        }
        var son: Exception? = null
        for (ad in sira) {
            try {
                val t = when (ad) {
                    "pc" -> pc(metin)
                    "groq" -> openaiUyumlu("https://api.groq.com/openai/v1/chat/completions", Depo.al(Depo.GROQ),
                        "llama-3.3-70b-versatile", metin)
                    "gemini" -> gemini(metin)
                    "claude" -> claude(metin)
                    else -> openaiUyumlu("https://api.openai.com/v1/chat/completions", Depo.al(Depo.OPENAI), "gpt-4o-mini", metin)
                }.trim()
                if (t.isNotEmpty()) {
                    if (ad != "pc" || misafirMi()) {
                        gecmis.addLast(Mesaj(true, metin)); gecmis.addLast(Mesaj(false, t))
                        while (gecmis.size > 16) gecmis.removeFirst()
                    }
                    return@withContext Cevap(t, ad)
                }
            } catch (e: Exception) {
                son = e
                // Bilgisayar kapalı / adres değişmiş: 5 dk boyunca atla, doğrudan yapay zekâya git
                if (ad == "pc" && (e !is SaglayiciHatasi || e.kod >= 500 || e.kod == 404)) {
                    pcAtlaBitis = System.currentTimeMillis() + 5 * 60_000L
                }
            }
        }
        val neden = (son as? SaglayiciHatasi)?.let { if (it.kod == 429) "kotalar dolu" else "hata ${it.kod}" } ?: "internete ulaşılamadı"
        Cevap("Şu an hiçbir beyne ulaşamadım efendim ($neden). Biraz sonra yeniden dener misin?", "hata")
    }

    private fun gonder(istek: Request, istemci: OkHttpClient = http): JSONObject {
        istemci.newCall(istek).execute().use { r ->
            val govde = r.body?.string() ?: ""
            if (!r.isSuccessful) throw SaglayiciHatasi(r.code, govde.take(200))
            return JSONObject(govde)
        }
    }

    /** Bilgisayardaki JARVIS (V13 telefon köprüsü): aynı kişilik, hafıza ve bilgisayar komutları. */
    private fun pc(metin: String): String {
        val anahtar = Depo.al(Depo.PC_ANAHTAR)
        if (misafirMi()) {
            // Misafir: bilgisayarın hafızasına/komutlarına dokunmaz; yalnızca ayrı misafir sohbeti (günlük sınırlı).
            val g = JSONArray()
            for (m in gecmis) g.put(JSONObject().put("rol", if (m.benden) "ben" else "jarvis").put("metin", m.metin))
            val istek = Request.Builder()
                .url(Depo.al(Depo.PC_ADRES).trimEnd('/') + "/api/sohbet")
                .header("X-Jarvis-Token", anahtar)
                .post(JSONObject().put("metin", metin).put("gecmis", g).toString().toRequestBody(JSON_TUR))
                .build()
            return gonder(istek, pcHttp).optString("metin")
        }
        val istek = Request.Builder()
            .url(Depo.al(Depo.PC_ADRES).trimEnd('/') + "/api/komut")
            .header("X-Jarvis-Token", anahtar)
            .post(JSONObject().put("metin", metin).toString().toRequestBody(JSON_TUR))
            .build()
        return gonder(istek, pcHttp).optString("cevap")
    }

    /** V13 misafir anahtarları "m_" ile başlar. */
    fun misafirMi(): Boolean = Depo.al(Depo.PC_ANAHTAR).startsWith("m_")

    private fun mesajlar(metin: String): JSONArray {
        val dizi = JSONArray()
        dizi.put(JSONObject().put("role", "system").put("content", sistem()))
        for (m in gecmis) dizi.put(JSONObject().put("role", if (m.benden) "user" else "assistant").put("content", m.metin))
        dizi.put(JSONObject().put("role", "user").put("content", metin))
        return dizi
    }

    private fun openaiUyumlu(url: String, anahtar: String, model: String, metin: String): String {
        val govde = JSONObject().put("model", model).put("messages", mesajlar(metin)).put("temperature", 0.7)
        val istek = Request.Builder().url(url).header("Authorization", "Bearer $anahtar")
            .post(govde.toString().toRequestBody(JSON_TUR)).build()
        val j = gonder(istek)
        return j.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
    }

    private val GEMINI_MODELLER = listOf("gemini-3.6-flash", "gemini-3.8-flash", "gemini-3.5-flash-lite")

    private fun gemini(metin: String): String {
        val anahtarlar = Depo.al(Depo.GEMINI).split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val icerik = JSONArray()
        for (m in gecmis) icerik.put(JSONObject().put("role", if (m.benden) "user" else "model")
            .put("parts", JSONArray().put(JSONObject().put("text", m.metin))))
        icerik.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", metin))))
        val govde = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", sistem()))))
            .put("contents", icerik)
        var son: Exception = SaglayiciHatasi(0, "anahtar yok")
        for (model in GEMINI_MODELLER) {
            for (anahtar in anahtarlar.take(2)) {          // aynı projedeki anahtarlar kotayı paylaşır: 2'den fazlası boşuna
                try {
                    val istek = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                        .header("x-goog-api-key", anahtar)
                        .post(govde.toString().toRequestBody(JSON_TUR)).build()
                    val j = gonder(istek)
                    val parcalar = j.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                    val sb = StringBuilder()
                    if (parcalar != null) for (i in 0 until parcalar.length()) sb.append(parcalar.optJSONObject(i)?.optString("text") ?: "")
                    if (sb.isNotEmpty()) return sb.toString()
                } catch (e: SaglayiciHatasi) {
                    son = e
                    if (e.kod == 400 || e.kod == 401 || e.kod == 403) throw e
                    if (e.kod == 404) break
                }
            }
        }
        throw son
    }

    private fun claude(metin: String): String {
        val dizi = JSONArray()
        for (m in gecmis) dizi.put(JSONObject().put("role", if (m.benden) "user" else "assistant").put("content", m.metin))
        dizi.put(JSONObject().put("role", "user").put("content", metin))
        val govde = JSONObject().put("model", "claude-sonnet-5-5").put("max_tokens", 800).put("system", sistem()).put("messages", dizi)
        val istek = Request.Builder().url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", Depo.al(Depo.CLAUDE)).header("anthropic-version", "2023-06-01")
            .post(govde.toString().toRequestBody(JSON_TUR)).build()
        val j = gonder(istek)
        val c = j.optJSONArray("content") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until c.length()) sb.append(c.optJSONObject(i)?.optString("text") ?: "")
        return sb.toString()
    }

    /** Bilgisayardan anahtarları alır (yalnızca eşleşmiş SAHİP telefonu). Sonucu Türkçe söyler. */
    suspend fun anahtarlariAl(): String = withContext(Dispatchers.IO) {
        try {
            val istek = Request.Builder()
                .url(Depo.al(Depo.PC_ADRES).trimEnd('/') + "/api/anahtarlar")
                .header("X-Jarvis-Token", Depo.al(Depo.PC_ANAHTAR))
                .post("{}".toRequestBody(JSON_TUR)).build()
            val j = gonder(istek)
            val g = j.optJSONArray("gemini")
            val liste = mutableListOf<String>()
            if (g != null) for (i in 0 until g.length()) liste.add(g.optString(i))
            Depo.yaz(Depo.GEMINI, liste.joinToString(","))
            Depo.yaz(Depo.GROQ, j.optString("groq"))
            Depo.yaz(Depo.CLAUDE, j.optString("anthropic"))
            Depo.yaz(Depo.OPENAI, j.optString("openai"))
            Depo.yaz(Depo.SARJOR, j.optString("sarjor", "groq,gemini,claude,openai"))
            "Anahtarlar alındı efendim: ${siradakiler().filter { it != "pc" }.joinToString(" → ")}. Bilgisayar kapalıyken de konuşabilirim."
        } catch (e: SaglayiciHatasi) {
            if (e.kod == 401) "Anahtar geçersiz efendim. \"Bilgisayar bağlantısını sıfırla\"ya bas, bilgisayarda yeniden \"telefonu bağla\" de ve yeni kodla eşleş."
            else if (e.kod == 403) "Bu telefon misafir olarak bağlı; anahtarları yalnızca sahibin telefonu alabilir."
            else "Bilgisayardan alamadım (hata ${e.kod})."
        } catch (e: Exception) {
            "Bilgisayara ulaşamadım. Bilgisayarda JARVIS açık mı? Adres her açılışta değişir: \"telefonu bağla\" deyip yeni adresi gir."
        }
    }

    /** 6 haneli kodla eşleşir. (true, anahtar) ya da (false, Türkçe hata). */
    suspend fun esles(adres: String, pin: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val istek = Request.Builder().url(adres.trimEnd('/') + "/api/esle")
                .post(JSONObject().put("pin", pin).toString().toRequestBody(JSON_TUR)).build()
            pcHttp.newCall(istek).execute().use { r ->
                val g = r.body?.string() ?: ""
                val j = try { JSONObject(g) } catch (_: Exception) { JSONObject() }
                if (r.isSuccessful && j.optString("t").isNotEmpty()) {
                    pcAtlaBitis = 0L
                    true to j.optString("t")
                } else {
                    false to (j.optString("hata").ifEmpty { "Bilgisayar eşleşmeyi kabul etmedi (hata ${r.code})." })
                }
            }
        } catch (e: Exception) {
            false to "Bilgisayara ulaşamadım. Adresi doğru yazdın mı? Bilgisayarda JARVIS ve telefon bağlantısı açık mı?"
        }
    }

    /** Bilgisayardaki JARVIS'in sesiyle okutmak için MP3/WAV baytları; olmazsa null. */
    suspend fun pcSesi(metin: String): ByteArray? = withContext(Dispatchers.IO) {
        if (Depo.al(Depo.PC_ADRES).isEmpty() || Depo.al(Depo.PC_ANAHTAR).isEmpty()
            || System.currentTimeMillis() < pcAtlaBitis) return@withContext null
        try {
            val istek = Request.Builder()
                .url(Depo.al(Depo.PC_ADRES).trimEnd('/') + "/api/ses")
                .header("X-Jarvis-Token", Depo.al(Depo.PC_ANAHTAR))
                .post(JSONObject().put("metin", metin.take(700)).toString().toRequestBody(JSON_TUR)).build()
            pcHttp.newCall(istek).execute().use { r -> if (r.code == 200) r.body?.bytes() else null }
        } catch (e: Exception) {
            pcAtlaBitis = System.currentTimeMillis() + 5 * 60_000L   // bilgisayar yok: 5 dk telefonun kendi sesi
            null
        }
    }

    fun sifirla() = gecmis.clear()
}
