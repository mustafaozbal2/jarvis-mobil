package com.mustafa.jarvis.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustafa.jarvis.depo.Depo
import com.mustafa.jarvis.izin.Adim
import com.mustafa.jarvis.izin.Izinler

/**
 * İZİN SİHİRBAZI (Aşama 14.0). İzinleri tek tek, nedenini söyleyerek ister.
 * Yalnızca mikrofon zorunlu; gerisi "Şimdilik geç" ile atlanır. Son adım bitince kurulum tamamlanır.
 */
@Composable
fun IzinSihirbazi(bitti: () -> Unit) {
    val ctx = LocalContext.current
    val aktivite = ctx as Activity
    val adimlar = remember { Izinler.adimlar(ctx) }
    var i by remember { mutableIntStateOf(0) }
    var tetik by remember { mutableIntStateOf(0) }       // ekrana geri dönünce durumu yeniden oku
    val adim = adimlar.getOrNull(i)

    val izinSor = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tetik++ }
    val ayarDon = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { tetik++ }

    if (adim == null) {
        Depo.bayrakYaz(Depo.KURULUM_TAMAM, true)
        bitti()
        return
    }

    val tamam = tetik >= 0 && adim.tamamMi(ctx)   // tetik okunur: ayardan dönünce durum yeniden hesaplanır

    Box(Modifier.fillMaxSize().background(UzayZemin).padding(24.dp)) {
        Column(Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
            Spacer(Modifier.height(24.dp))
            LinearProgressIndicator(
                progress = { (i + 1f) / adimlar.size },
                modifier = Modifier.fillMaxWidth(),
                color = NebulaYesil, trackColor = UzayYuzey
            )
            Spacer(Modifier.height(8.dp))
            Text("${i + 1} / ${adimlar.size}", color = Soluk, fontSize = 13.sp)
        }

        Column(
            Modifier.align(Alignment.Center).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(adim.simge, fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(adim.baslik, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(adim.neden, color = Soluk, fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)
            if (tamam) {
                Spacer(Modifier.height(16.dp))
                Surface(color = NebulaYesil.copy(alpha = 0.18f), shape = RoundedCornerShape(20.dp)) {
                    Text("✓ Verildi", color = NebulaYesil, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Button(
                onClick = {
                    when {
                        tamam -> i++
                        adim.calismaIzni != null -> izinSor.launch(adim.calismaIzni!!)
                        adim.ayarNiyeti != null -> {
                            adim.elleOnay?.let { Depo.bayrakYaz(it, true) }   // ayara giderse "yapıldı" sayılır (durum okunamayabilir)
                            ayarDon.launch(adim.ayarNiyeti!!.invoke(ctx))
                        }
                        else -> i++
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NebulaYesil, contentColor = UzayZemin)
            ) {
                Text(
                    when {
                        tamam -> if (i == adimlar.lastIndex) "Hazırım, başlayalım" else "Devam"
                        adim.ayarNiyeti != null -> "Ayarı aç"
                        else -> "İzin ver"
                    },
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(6.dp))
            if (!adim.zorunlu) {
                TextButton(onClick = { i++ }, modifier = Modifier.fillMaxWidth()) {
                    Text("Şimdilik geç", color = Soluk)
                }
            } else {
                Text(
                    "Bu izin olmadan devam edemeyiz efendim.",
                    color = Soluk.copy(alpha = 0.7f), fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(8.dp), textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
