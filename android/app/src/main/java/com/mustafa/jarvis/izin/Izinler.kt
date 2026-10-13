package com.mustafa.jarvis.izin

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.mustafa.jarvis.depo.Depo
import com.mustafa.jarvis.nebula.NebulaDuvarKagidi

/**
 * İZİN SİHİRBAZI ADIMLARI (14.0). Her izin TEK TEK, ne işe yaradığı söylenerek istenir.
 * calisma (runtime) izinleri sistem penceresiyle; özel izinler ilgili ayar sayfası açılarak alınır.
 * Yalnızca mikrofon zorunlu; gerisi "Şimdilik geç" ile atlanabilir ve Ayarlar'dan tekrar açılabilir.
 */
data class Adim(
    val id: String,
    val simge: String,
    val baslik: String,
    val neden: String,
    val calismaIzni: String? = null,          // sistem penceresiyle istenen izin
    val ayarNiyeti: ((Context) -> Intent)? = null,
    val elleOnay: String? = null,             // durum okunamıyorsa: kullanıcı "Yaptım" der, bu bayrak yazılır
    val zorunlu: Boolean = false,
    val tamamMi: (Context) -> Boolean,
)

object Izinler {
    private fun verildi(ctx: Context, izin: String) = ctx.checkSelfPermission(izin) == PackageManager.PERMISSION_GRANTED

    fun xiaomiMi(): Boolean = Build.MANUFACTURER.lowercase().let { it.contains("xiaomi") || it.contains("redmi") || it.contains("poco") }

    private fun uygulamaAyari(ctx: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))

    @SuppressLint("BatteryLife")
    fun adimlar(ctx: Context): List<Adim> = buildList {
        add(Adim(
            id = "mikrofon", simge = "🎙️", baslik = "Mikrofon",
            neden = "Seni duyabilmem için. JARVIS'in kalbi bu; onsuz konuşamayız efendim.",
            calismaIzni = Manifest.permission.RECORD_AUDIO, zorunlu = true,
            tamamMi = { verildi(it, Manifest.permission.RECORD_AUDIO) }
        ))
        if (Build.VERSION.SDK_INT >= 33) add(Adim(
            id = "bildirim", simge = "🔔", baslik = "Bildirimler",
            neden = "Arka planda hazır beklediğimi ve seni duyduğumu gösterebilmem için.",
            calismaIzni = Manifest.permission.POST_NOTIFICATIONS,
            tamamMi = { verildi(it, Manifest.permission.POST_NOTIFICATIONS) }
        ))
        add(Adim(
            id = "kisiler", simge = "📇", baslik = "Kişiler",
            neden = "\"Annemi ara\" dediğinde doğru numarayı bulabilmem için. Kişilerin telefonundan çıkmaz.",
            calismaIzni = Manifest.permission.READ_CONTACTS,
            tamamMi = { verildi(it, Manifest.permission.READ_CONTACTS) }
        ))
        add(Adim(
            id = "galeri", simge = "🖼️", baslik = "Galeri / Fotoğraflar",
            neden = "Fotoğraflarına bakıp \"bu kim, nerede çekilmiş\" gibi sorularına yardım edebilmem için.",
            calismaIzni = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
                          else Manifest.permission.READ_EXTERNAL_STORAGE,
            tamamMi = {
                verildi(it, if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
                            else Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        ))
        add(Adim(
            id = "arama", simge = "📞", baslik = "Telefon araması",
            neden = "\"Ali'yi ara\" dediğinde senin yerine aramayı başlatabilmem için.",
            calismaIzni = Manifest.permission.CALL_PHONE,
            tamamMi = { verildi(it, Manifest.permission.CALL_PHONE) }
        ))
        add(Adim(
            id = "ustte", simge = "🪟", baslik = "Diğer uygulamaların üstünde göster",
            neden = "Telefon kilitliyken \"Jarvis\" dediğinde ekranı kendim açabilmem için. En güçlü hâlim bu izinle.",
            ayarNiyeti = { Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${it.packageName}")) },
            tamamMi = { Settings.canDrawOverlays(it) }
        ))
        add(Adim(
            id = "bildirim_erisim", simge = "📨", baslik = "Bildirim erişimi",
            neden = "\"Bildirimlerimi oku\" ya da \"Instagram'dan mesaj var mı\" diye sorduğunda bakabilmem için. Son 50 bildirim yalnızca telefonda tutulur.",
            ayarNiyeti = { Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) },
            tamamMi = {
                NotificationManagerCompat.getEnabledListenerPackages(it).contains(it.packageName)
            }
        ))
        add(Adim(
            id = "pil", simge = "🔋", baslik = "Pil kısıtlamasını kaldır",
            neden = "Telefon beni uyutup susturmasın, saatlerce \"Jarvis\" demeni bekleyebileyim diye.",
            ayarNiyeti = { Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${it.packageName}")) },
            tamamMi = {
                val pm = it.getSystemService(Context.POWER_SERVICE) as PowerManager
                pm.isIgnoringBatteryOptimizations(it.packageName)
            }
        ))
        if (Build.VERSION.SDK_INT >= 34) add(Adim(
            id = "tam_ekran", simge = "📲", baslik = "Tam ekran bildirim",
            neden = "Üstte gösterme iznini vermezsen, kilit ekranında seni gene de karşılayabilmem için (arama ekranı gibi).",
            ayarNiyeti = { Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${it.packageName}")) },
            tamamMi = {
                val nm = it.getSystemService(NotificationManager::class.java)
                if (Build.VERSION.SDK_INT >= 34) nm.canUseFullScreenIntent() else true
            }
        ))
        if (xiaomiMi()) add(Adim(
            id = "xiaomi", simge = "⚙️", baslik = "Xiaomi: Otomatik başlat + Arka planda açılma",
            neden = "Xiaomi/Redmi/POCO telefonlar uygulamaları sıkı kapatır. \"Otomatik başlat\"ı aç ve izinlerde \"Arka planda açılma\"ya izin ver; yoksa kilitliyken açılamam.",
            ayarNiyeti = { xiaomiAyari(it) },
            elleOnay = Depo.XIAOMI_ONAY,
            tamamMi = { Depo.bayrak(Depo.XIAOMI_ONAY) }
        ))
        add(Adim(
            id = "duvar", simge = "🌌", baslik = "Nebula'yı duvar kâğıdı yap",
            neden = "Ana ekranında (telefonun izin verdiği kadarıyla kilit ekranında da) yaşayan Nebula dönsün diye. Dilersen sonra Ayarlar'dan da yapabilirsin.",
            ayarNiyeti = { duvarNiyeti(it) },
            elleOnay = "duvar_secildi",
            tamamMi = {
                val wm = WallpaperManager.getInstance(it)
                wm.wallpaperInfo?.packageName == it.packageName || Depo.bayrak("duvar_secildi")
            }
        ))
    }

    /** Bir runtime izni için Activity.requestPermissions çağrısında kullanılacak dizi. */
    fun calismaIzinleri(adim: Adim): Array<String> = adim.calismaIzni?.let { arrayOf(it) } ?: emptyArray()

    /** Xiaomi'nin otomatik başlat ekranını açmayı dener; olmazsa uygulama ayarına düşer. */
    private fun xiaomiAyari(ctx: Context): Intent {
        val adaylar = listOf(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")
        )
        for (c in adaylar) {
            val i = Intent().setComponent(c)
            if (ctx.packageManager.resolveActivity(i, 0) != null) return i
        }
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
    }

    /** Canlı duvar kâğıdı seçicisini doğrudan Nebula önizlemesinde açar. */
    fun duvarNiyeti(ctx: Context): Intent {
        val i = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(ctx, NebulaDuvarKagidi::class.java)
        )
        if (ctx.packageManager.resolveActivity(i, 0) != null) return i
        return Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
    }

    fun hepsiTamamMi(ctx: Context): Boolean = adimlar(ctx).all { it.tamamMi(ctx) || !it.zorunlu }
}
