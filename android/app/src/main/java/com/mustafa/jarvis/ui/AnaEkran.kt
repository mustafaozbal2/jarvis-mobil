package com.mustafa.jarvis.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Ana ekran: tüm ekranı kaplayan canlı Nebula, ortada hâl etiketi, altta altyazı ve
 * mikrofon düğmesi ile Sohbet / Ayarlar kısayolları. "Jarvis" demek ya da mikrofona dokunmak JARVIS'i uyandırır.
 */
@Composable
fun AnaEkran(
    hal: Hal,
    altyazi: String,
    enerji: Float,
    dinlemeDe: () -> Unit,
    durdur: () -> Unit,
    sohbet: () -> Unit,
    ayarlar: () -> Unit,
) {
    val yumusakEnerji by animateFloatAsState(enerji, tween(600), label = "enerji")
    Box(Modifier.fillMaxSize().background(UzayZemin)) {
        NebulaArkaPlan(yumusakEnerji, Modifier.fillMaxSize())

        // Üst bar: Sohbet · Ayarlar
        Row(
            Modifier.fillMaxWidth().padding(16.dp).padding(top = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("JARVIS", color = NebulaYesil, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Row {
                MetinDugme("Sohbet", sohbet)
                Spacer(Modifier.size(12.dp))
                MetinDugme("Ayarlar", ayarlar)
            }
        }

        // Orta: hâl
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when (hal) {
                    Hal.BEKLEME -> "\"Jarvis\" de"
                    Hal.DINLEME -> "Dinliyorum"
                    Hal.DUSUNME -> "Düşünüyorum"
                    Hal.KONUSMA -> "…"
                },
                color = Color.White.copy(alpha = 0.92f), fontSize = 15.sp, letterSpacing = 2.sp
            )
        }

        // Alt: altyazı + mikrofon
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (altyazi.isNotBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)
                ) {
                    Text(
                        altyazi, color = Color.White, fontSize = 17.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp).fillMaxWidth()
                    )
                }
            }
            val konusuyor = hal != Hal.BEKLEME
            Box(
                Modifier.size(84.dp).clip(CircleShape)
                    .background(if (konusuyor) NebulaYesil else NebulaYesil.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { if (konusuyor) durdur() else dinlemeDe() }, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(
                            if (konusuyor) android.R.drawable.ic_media_pause else android.R.drawable.ic_btn_speak_now
                        ),
                        contentDescription = if (konusuyor) "Durdur" else "Konuş",
                        tint = if (konusuyor) UzayZemin else NebulaYesil,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (konusuyor) "Durdurmak için dokun" else "Konuşmak için dokun",
                color = Soluk.copy(alpha = 0.8f), fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun MetinDugme(yazi: String, tikla: () -> Unit) {
    Text(
        yazi, color = Color.White, fontSize = 14.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .clickable(onClick = tikla)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
