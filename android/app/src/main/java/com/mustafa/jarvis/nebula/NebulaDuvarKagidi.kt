package com.mustafa.jarvis.nebula

import android.content.Context
import android.opengl.GLSurfaceView
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

/**
 * Nebula canlı duvar kâğıdı (14). Ana ekranda (ve telefon izin veriyorsa kilit ekranında) yaşayan Nebula.
 * Yalnızca ekran görünürken çizilir; ana ekran sayfası kaydırıldıkça bulutlar yavaşça kayar.
 * Ayarlar → "Nebula'yı duvar kâğıdı yap" ya da izin sihirbazının son adımı.
 */
class NebulaDuvarKagidi : WallpaperService() {
    override fun onCreateEngine(): Engine = NebulaMotoru()

    inner class NebulaMotoru : Engine() {
        private var gorunum: DuvarGorunumu? = null
        private val renderer = NebulaRenderer(duvarKagidi = true).apply { cekirdekGorunur = 0.75f }

        /** GLSurfaceView'i duvar kâğıdının yüzeyine çizdirmenin bilinen yolu: getHolder'ı motorun yüzeyine bağlamak. */
        inner class DuvarGorunumu(ctx: Context) : GLSurfaceView(ctx) {
            override fun getHolder(): SurfaceHolder = this@NebulaMotoru.surfaceHolder
            fun yokEt() = super.onDetachedFromWindow()
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            gorunum = DuvarGorunumu(this@NebulaDuvarKagidi).apply {
                setEGLContextClientVersion(2)
                preserveEGLContextOnPause = true
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) gorunum?.onResume() else gorunum?.onPause()
        }

        override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xStep: Float, yStep: Float, xPixels: Int, yPixels: Int) {
            renderer.kaydirma = xOffset
        }

        override fun onDestroy() {
            super.onDestroy()
            gorunum?.yokEt()
            gorunum = null
        }
    }
}
