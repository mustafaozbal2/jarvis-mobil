package com.mustafa.jarvis.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustafa.jarvis.beyin.Sarjor
import com.mustafa.jarvis.depo.Depo
import com.mustafa.jarvis.izin.Izinler
import kotlinx.coroutines.launch

/**
 * Ayarlar: bilgisayarla eşleşme, "anahtarları bilgisayardan al", elle anahtar girme, ses seçimi,
 * kilit ekranı testi, Nebula'yı duvar kâğıdı yapma, izin sihirbazını yeniden açma.
 */
@Composable
fun AyarlarEkrani(sihirbaziAc: () -> Unit, geri: () -> Unit, haber: (String) -> Unit) {
    val ctx = LocalContext.current
    val aktivite = ctx as Activity
    val kapsam = rememberCoroutineScope()

    var pcAdres by remember { mutableStateOf(Depo.al(Depo.PC_ADRES)) }
    var pcAnahtar by remember { mutableStateOf(Depo.al(Depo.PC_ANAHTAR)) }
    var gemini by remember { mutableStateOf(Depo.al(Depo.GEMINI)) }
    var groq by remember { mutableStateOf(Depo.al(Depo.GROQ)) }
    var claude by remember { mutableStateOf(Depo.al(Depo.CLAUDE)) }
    var openai by remember { mutableStateOf(Depo.al(Depo.OPENAI)) }
    var jarvisSes by remember { mutableStateOf(Depo.al(Depo.SES, "jarvis") == "jarvis") }
    var arkaDinle by remember { mutableStateOf(!Depo.bayrak(Depo.UYANMA)) }
    var pin by remember { mutableStateOf("") }
    var durum by remember { mutableStateOf("") }
    var mesgul by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(UzayZemin).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                Depo.yaz(Depo.PC_ADRES, pcAdres.trim()); Depo.yaz(Depo.PC_ANAHTAR, pcAnahtar.trim())
                Depo.yaz(Depo.GEMINI, gemini.trim()); Depo.yaz(Depo.GROQ, groq.trim())
                Depo.yaz(Depo.CLAUDE, claude.trim()); Depo.yaz(Depo.OPENAI, openai.trim())
                geri()
            }) { Text("‹ Kaydet ve dön", color = NebulaYesil, fontSize = 16.sp) }
        }
        Baslik("Bilgisayar")
        Not("Bilgisayarda \"Jarvis, telefonu bağla\" de. Sohbet penceresinde çıkan adresi ve 6 haneli kodu buraya yaz. " +
            "Ya da tam bağlantıyı (…/?t=…) yapıştır; kod gerekmez.")
        Alan("Adres ya da tam bağlantı (https://….trycloudflare.com)", pcAdres) { yeni ->
            // Tam bağlantı yapıştırıldıysa adresi ve anahtarı kendimiz ayırırız
            val ayri = baglantiAyir(yeni)
            if (ayri != null) { pcAdres = ayri.first; pcAnahtar = ayri.second } else { pcAdres = yeni }
        }
        if (pcAnahtar.isBlank()) {
            Alan("Eşleşme kodu (6 hane)", pin) { pin = it.filter { c -> c.isDigit() }.take(6) }
        } else {
            Not("✓ Bu telefonun bilgisayar anahtarı var.")
        }
        Dugme(if (mesgul) "Bağlanıyor…" else "Bağlan ve anahtarları al") {
            val adres = pcAdres.trim().trimEnd('/')
            if (mesgul) {
                // zaten çalışıyor
            } else if (!adres.startsWith("http")) {
                durum = "Adres https:// ile başlamalı efendim. Bilgisayardaki sohbet penceresinden kopyala."
            } else if (pcAnahtar.isBlank() && pin.length != 6) {
                durum = "Bilgisayardaki 6 haneli eşleşme kodunu yaz, ya da tam bağlantıyı yapıştır."
            } else {
                mesgul = true
                durum = "Bilgisayara bağlanıyorum…"
                kapsam.launch {
                    var tamam = true
                    if (pcAnahtar.isBlank()) {
                        val (ok, sonuc) = Sarjor.esles(adres, pin)
                        if (ok) { pcAnahtar = sonuc; pin = "" } else { durum = sonuc; tamam = false }
                    }
                    if (tamam) {
                        pcAdres = adres
                        Depo.yaz(Depo.PC_ADRES, adres); Depo.yaz(Depo.PC_ANAHTAR, pcAnahtar.trim())
                        val s = Sarjor.anahtarlariAl()
                        gemini = Depo.al(Depo.GEMINI); groq = Depo.al(Depo.GROQ)
                        claude = Depo.al(Depo.CLAUDE); openai = Depo.al(Depo.OPENAI)
                        durum = s
                        haber(s)
                    }
                    mesgul = false
                }
            }
        }
        if (durum.isNotBlank()) {
            Surface(color = NebulaYesil.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(durum, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(12.dp))
            }
        }
        if (pcAnahtar.isNotBlank()) {
            TextButton(onClick = {
                pcAnahtar = ""; Depo.yaz(Depo.PC_ANAHTAR, ""); Sarjor.sifirla()
                durum = "Bilgisayar bağlantısı silindi. Yeniden bağlanmak için adres ve yeni kodu gir."
            }) { Text("Bilgisayar bağlantısını sıfırla", color = Soluk, fontSize = 13.sp) }
        }

        Baslik("Yapay zekâ anahtarları (elle)")
        Not("Arkadaşına kurduysan buraya dokunma; bilgisayardan alman yeter. Anahtarlar yalnızca bu telefonda, şifreli durur.")
        Alan("Gemini (virgülle birden çok)", gemini) { gemini = it }
        Alan("Groq", groq) { groq = it }
        Alan("Claude (Anthropic)", claude) { claude = it }
        Alan("OpenAI", openai) { openai = it }

        Baslik("Ses")
        Secim("JARVIS'in sesi (bilgisayardan)", jarvisSes) {
            jarvisSes = it; Depo.yaz(Depo.SES, if (it) "jarvis" else "telefon")
        }
        Not(if (jarvisSes) "Bilgisayar açıkken onun sesiyle konuşurum; kapalıyken telefonun sesine düşerim."
            else "Telefonun kendi Türkçe sesini kullanırım (bilgisayar gerekmez).")

        Baslik("Uyanma")
        Secim("Arka planda \"Jarvis\" dinle", arkaDinle) {
            arkaDinle = it; Depo.bayrakYaz(Depo.UYANMA, !it)
        }
        Dugme("Kilit ekranı testi (5 sn)") {
            haber("5 saniye içinde ekranı kilitle, efendim.")
            com.mustafa.jarvis.servis.JarvisServisi.baslat(ctx, com.mustafa.jarvis.servis.JarvisServisi.EYLEM_KILIT_TESTI)
        }

        Baslik("Nebula")
        Dugme("Nebula'yı duvar kâğıdı yap") {
            Depo.bayrakYaz("duvar_secildi", true)
            try { aktivite.startActivity(Izinler.duvarNiyeti(ctx)) } catch (_: Exception) { haber("Duvar kâğıdı ekranı açılamadı.") }
        }

        Baslik("İzinler")
        Dugme("İzin sihirbazını yeniden aç") { sihirbaziAc() }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable private fun Baslik(t: String) {
    Spacer(Modifier.height(20.dp))
    Text(t, color = NebulaYesil, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
}

@Composable private fun Not(t: String) {
    Text(t, color = Soluk.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable private fun Alan(etiket: String, deger: String, degis: (String) -> Unit) {
    OutlinedTextField(
        value = deger, onValueChange = degis,
        label = { Text(etiket, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = true
    )
}

@Composable private fun Dugme(t: String, tikla: () -> Unit) {
    Button(
        onClick = tikla,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = UzayYuzey, contentColor = NebulaYesil)
    ) { Text(t, fontSize = 15.sp) }
}

@Composable private fun Secim(t: String, acik: Boolean, degis: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(t, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked = acik, onCheckedChange = degis)
    }
}

/** "https://x.trycloudflare.com/?t=ANAHTAR" → ("https://x.trycloudflare.com", "ANAHTAR"); bağlantı değilse null. */
internal fun baglantiAyir(metin: String): Pair<String, String>? {
    val m = Regex("""(https?://[^/?#\s]+)[^\s]*?[?&]t=([A-Za-z0-9_\-]+)""").find(metin.trim()) ?: return null
    return m.groupValues[1] to m.groupValues[2]
}
