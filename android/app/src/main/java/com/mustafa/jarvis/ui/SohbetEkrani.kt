package com.mustafa.jarvis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustafa.jarvis.beyin.Mesaj
import com.mustafa.jarvis.beyin.Sarjor
import kotlinx.coroutines.launch

/** Yazışma ekranı: sesle değil, klavyeyle de konuşmak isteyince. Aynı beyni (Sarjor) kullanır. */
@Composable
fun SohbetEkrani(gecmis: androidx.compose.runtime.snapshots.SnapshotStateList<Mesaj>, geri: () -> Unit) {
    val kapsam = rememberCoroutineScope()
    var yazi by remember { mutableStateOf("") }
    var mesgul by remember { mutableStateOf(false) }
    val liste = rememberLazyListState()

    LaunchedEffect(gecmis.size) { if (gecmis.isNotEmpty()) liste.animateScrollToItem(gecmis.size - 1) }

    Column(Modifier.fillMaxSize().background(UzayZemin)) {
        Row(Modifier.fillMaxWidth().padding(16.dp).padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = geri) { Text("‹ Geri", color = NebulaYesil, fontSize = 16.sp) }
            Spacer(Modifier.size(8.dp))
            Text("Sohbet", color = Color.White, fontSize = 18.sp)
        }
        LazyColumn(
            state = liste,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(gecmis.size) { idx ->
                val m = gecmis[idx]
                Balon(m.metin, m.benden)
            }
        }
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = yazi, onValueChange = { yazi = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Bir şeyler yaz…", color = Soluk) },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            IconButton(
                enabled = !mesgul && yazi.isNotBlank(),
                onClick = {
                    val soru = yazi.trim(); yazi = ""; mesgul = true
                    gecmis.add(Mesaj(true, soru))
                    kapsam.launch {
                        val c = Sarjor.sor(soru)
                        gecmis.add(Mesaj(false, c.metin))
                        mesgul = false
                    }
                }
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_send),
                    contentDescription = "Gönder",
                    tint = if (yazi.isNotBlank()) NebulaYesil else Soluk
                )
            }
        }
    }
}

@Composable
private fun Balon(metin: String, benden: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (benden) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (benden) NebulaYesil.copy(alpha = 0.18f) else UzayYuzey,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(metin, color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(12.dp))
        }
    }
}
