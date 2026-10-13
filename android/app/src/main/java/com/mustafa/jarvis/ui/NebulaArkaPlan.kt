package com.mustafa.jarvis.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.mustafa.jarvis.nebula.NebulaGorunum
import com.mustafa.jarvis.ses.Seviye

/** Uygulama arka planındaki canlı Nebula; [enerji] JARVIS'in hâline göre değişir. */
@Composable
fun NebulaArkaPlan(enerji: Float, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val gorunum = remember { NebulaGorunum(ctx) }
    AndroidView(
        modifier = modifier,
        factory = { gorunum },
        update = { it.renderer.hedefEnerji = enerji }
    )
    LaunchedEffect(Unit) {
        gorunum.devam()
        while (true) {
            gorunum.renderer.sesSeviyesi = Seviye.deger
            kotlinx.coroutines.delay(33)
        }
    }
    DisposableEffect(Unit) { onDispose { gorunum.duraklat() } }
}
