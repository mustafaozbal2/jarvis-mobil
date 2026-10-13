package com.mustafa.jarvis

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.mustafa.jarvis.depo.Depo
import com.mustafa.jarvis.servis.Durum
import com.mustafa.jarvis.servis.JarvisServisi
import com.mustafa.jarvis.servis.Uyandirici
import com.mustafa.jarvis.ses.Dinleme
import com.mustafa.jarvis.ses.Konusma
import com.mustafa.jarvis.ui.AnaEkran
import com.mustafa.jarvis.ui.AyarlarEkrani
import com.mustafa.jarvis.ui.IzinSihirbazi
import com.mustafa.jarvis.ui.JarvisTema
import com.mustafa.jarvis.ui.Oturum
import com.mustafa.jarvis.ui.SohbetEkrani
import kotlinx.coroutines.launch

/** Ekranlar. */
private enum class Ekran { SIHIRBAZ, ANA, SOHBET, AYARLAR }

class MainActivity : ComponentActivity() {
    private lateinit var konusma: Konusma
    private lateinit var dinleme: Dinleme
    private var oturum: Oturum? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Depo.baslat(this)
        kilitUstu()
        konusma = Konusma(this)
        dinleme = Dinleme(this)
        val ot = Oturum(this, lifecycleScope, konusma, dinleme).also { oturum = it }

        eslesmeyiOku(intent)   // QR ile açıldıysa bilgisayara bağlan

        setContent {
            JarvisTema {
                var ekran by remember {
                    mutableStateOf(if (Depo.bayrak(Depo.KURULUM_TAMAM)) Ekran.ANA else Ekran.SIHIRBAZ)
                }
                var bildiri by remember { mutableStateOf("") }

                when (ekran) {
                    Ekran.SIHIRBAZ -> IzinSihirbazi(bitti = {
                        ekran = Ekran.ANA
                        JarvisServisi.baslat(this)
                        if (intent?.getBooleanExtra(Uyandirici.EKSTRA_UYANDIR, false) == true) ot.uyan(Durum.sonDuyulan)
                    })
                    Ekran.ANA -> AnaEkran(
                        hal = ot.hal, altyazi = if (bildiri.isNotBlank()) bildiri else ot.altyazi, enerji = ot.enerji,
                        dinlemeDe = { bildiri = ""; ot.elleDinle() },
                        durdur = { ot.durdur() },
                        sohbet = { ekran = Ekran.SOHBET },
                        ayarlar = { ekran = Ekran.AYARLAR }
                    )
                    Ekran.SOHBET -> SohbetEkrani(gecmis = ot.gecmis, geri = { ekran = Ekran.ANA })
                    Ekran.AYARLAR -> AyarlarEkrani(
                        sihirbaziAc = { ekran = Ekran.SIHIRBAZ },
                        geri = { ekran = Ekran.ANA },
                        haber = { bildiri = it; lifecycleScope.launch { kotlinx.coroutines.delay(4000); if (bildiri == it) bildiri = "" } }
                    )
                }
            }
        }

        // Servisin "Jarvis" ile uyandırdığı açılış
        if (savedInstanceState == null && intent?.getBooleanExtra(Uyandirici.EKSTRA_UYANDIR, false) == true
            && Depo.bayrak(Depo.KURULUM_TAMAM)) {
            ot.uyan(Durum.sonDuyulan)
        }
    }

    /** Kilit ekranının üstünde göster ve ekranı yak (protokol 1). */
    private fun kilitUstu() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        eslesmeyiOku(intent)
        if (intent.getBooleanExtra(Uyandirici.EKSTRA_UYANDIR, false)) {
            Uyandirici.bildirimiKaldir(this)
            oturum?.uyan(Durum.sonDuyulan)
        }
    }

    override fun onResume() {
        super.onResume()
        Durum.onPlanda = true          // ekran öndeyken servis dinlemeyi bırakır (mikrofon çakışmasın)
    }

    override fun onPause() {
        super.onPause()
        Durum.onPlanda = false
        oturum?.durdur()
    }

    /** QR bağlantısı: https://xxx.trycloudflare.com/?t=TOKEN → adres + anahtar olarak kaydet. */
    private fun eslesmeyiOku(intent: Intent?) {
        val veri: Uri = intent?.data ?: return
        val t = veri.getQueryParameter("t") ?: return
        if (t.isBlank()) return
        val adres = if (veri.scheme == "jarvis") (veri.getQueryParameter("pc") ?: "") else "${veri.scheme}://${veri.host}"
        if (adres.isNotBlank()) Depo.yaz(Depo.PC_ADRES, adres)
        Depo.yaz(Depo.PC_ANAHTAR, t)
    }

    override fun onDestroy() {
        konusma.kapat()
        dinleme.kapat()
        super.onDestroy()
    }
}
