package com.mustafa.jarvis.nebula

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView

/** Uygulamanın arka planındaki canlı Nebula. Telefonu eğince bulutlar derinlik hissiyle kayar. */
class NebulaGorunum(ctx: Context) : GLSurfaceView(ctx), SensorEventListener {
    val renderer = NebulaRenderer(duvarKagidi = false)
    private val sensorler = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) holder.setFixedSize(w / 2, h / 2)      // yarım çözünürlük: pil dostu, gözle fark edilmez
    }

    fun devam() {
        onResume()
        sensorler.getDefaultSensor(Sensor.TYPE_GRAVITY)?.let { sensorler.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun duraklat() {
        sensorler.unregisterListener(this)
        onPause()
    }

    override fun onSensorChanged(event: SensorEvent) {
        renderer.egimX = (-event.values[0] / 9.81f).coerceIn(-1f, 1f)
        renderer.egimY = ((event.values[1] - 6.5f) / 9.81f).coerceIn(-1f, 1f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
