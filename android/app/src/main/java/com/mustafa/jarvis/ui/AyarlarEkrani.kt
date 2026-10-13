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
        Alan("Bağlantıyı buraya yapıştır (https://....trycloudflare.com/?t=...)", pcAdres) { yeni ->
            // Tam bağlantı yapıştırıldıysa adresi ve anahtarı kendimiz ayırırız
            val ayri = baglantiAyir(yeni)
            if (ayri != null) { pcAdres = ayri.first; pcAnahtar = ayri.second } else { pcAdres = yeni }
        }
        Alan("Telefon anahtarı (token)", pcAnahtar) { pcAnahtar = it }
        Dugme("Anahtarları bilgisayardan al") {
            Depo.yaz(Depo.PC_ADRES, pcAdres.trim()); Depo.yaz(Depo.PC_ANAHTAR, pcAnahtar.trim())
            kapsam.launch {
                val s = Sarjor.anahtarlariAl()
                gemini = Depo.al(Depo.GEMINI); groq = Depo.al(Depo.GROQ)
                claude = Depo.al(Depo.CLAUDE); openai = Depo.al(Depo.OPENAI)
                haber(s)
            }
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
