package com.mustafa.jarvis.nebula

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * TELEFONA ÖZEL NEBULA (14). Bilgisayardaki Nebula'nın dikey ekrana göre yeniden kurulmuş hali:
 * katmanlı gaz bulutları (alan bükmeli fbm), çapraz akan galaksi kolu, iki katman parıldayan yıldız,
 * ortada nefes alan JARVIS çekirdeği, dönen yörünge halkaları. Telefonu eğince bulutlar derinlik hissiyle kayar (paralaks).
 * Enerji: 0 bekleme · 0.6 dinleme · 0.75 düşünme · 1 konuşma. Ses seviyesi çekirdeği anlık büyütür.
 * Pil: yarım çözünürlükte çizilir (ekran büyütür), uygulamada 30, duvar kâğıdında 24 kareyle sınırlıdır.
 */
class NebulaRenderer(private val duvarKagidi: Boolean = false) : GLSurfaceView.Renderer {
    @Volatile var hedefEnerji = 0.15f
    @Volatile var sesSeviyesi = 0f
    @Volatile var egimX = 0f
    @Volatile var egimY = 0f
    @Volatile var kaydirma = 0f          // duvar kâğıdında ana ekran sayfası kaydırması (0..1)
    @Volatile var cekirdekGorunur = 1f

    private var program = 0
    private var uR = 0; private var uT = 0; private var uM = 0; private var uE = 0; private var uA = 0; private var uS = 0
    private var aPos = 0
    private var w = 1; private var h = 1
    private var enerji = 0.15f
    private var ses = 0f
    private var mx = 0f; private var my = 0f
    private var sonKare = 0L
    private val baslangic = SystemClock.elapsedRealtime()
    private val ucgen: FloatBuffer = ByteBuffer.allocateDirect(6 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(floatArrayOf(-1f, -1f, 3f, -1f, -1f, 3f)); position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        program = programKur(VS, FS)
        uR = GLES20.glGetUniformLocation(program, "r")
        uT = GLES20.glGetUniformLocation(program, "t")
        uM = GLES20.glGetUniformLocation(program, "m")
        uE = GLES20.glGetUniformLocation(program, "e")
        uA = GLES20.glGetUniformLocation(program, "ac")
        uS = GLES20.glGetUniformLocation(program, "sv")
        aPos = GLES20.glGetAttribLocation(program, "a")
        GLES20.glClearColor(0.008f, 0.05f, 0.04f, 1f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        w = maxOf(1, width); h = maxOf(1, height)
        GLES20.glViewport(0, 0, w, h)
    }

    override fun onDrawFrame(gl: GL10?) {
        // kare hızı sınırı (pil)
        val hedefMs = if (duvarKagidi) 41L else 33L
        val simdi = SystemClock.elapsedRealtime()
        val gecen = simdi - sonKare
        if (gecen < hedefMs) SystemClock.sleep(hedefMs - gecen)
        sonKare = SystemClock.elapsedRealtime()

        enerji += (hedefEnerji - enerji) * 0.06f
        ses += (sesSeviyesi - ses) * 0.3f
        val hx = egimX * 0.6f + (kaydirma - 0.5f) * 0.8f
        mx += (hx - mx) * 0.05f
        my += (egimY * 0.6f - my) * 0.05f

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (program == 0) return
        GLES20.glUseProgram(program)
        GLES20.glUniform2f(uR, w.toFloat(), h.toFloat())
        GLES20.glUniform1f(uT, (SystemClock.elapsedRealtime() - baslangic) / 1000f + 20f)
        GLES20.glUniform2f(uM, mx, my)
        GLES20.glUniform1f(uE, enerji)
        GLES20.glUniform1f(uA, cekirdekGorunur)
        GLES20.glUniform1f(uS, ses)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 0, ucgen)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
        GLES20.glDisableVertexAttribArray(aPos)
    }

    private fun derle(tur: Int, kaynak: String): Int {
        val s = GLES20.glCreateShader(tur)
        GLES20.glShaderSource(s, kaynak)
        GLES20.glCompileShader(s)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) {
            android.util.Log.e("JARVIS", "Nebula gölgelendiricisi derlenemedi: " + GLES20.glGetShaderInfoLog(s))
            GLES20.glDeleteShader(s)
            return 0
        }
        return s
    }

    private fun programKur(vs: String, fs: String): Int {
        val v = derle(GLES20.GL_VERTEX_SHADER, vs)
        val f = derle(GLES20.GL_FRAGMENT_SHADER, fs)
        if (v == 0 || f == 0) return 0
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, v)
        GLES20.glAttachShader(p, f)
        GLES20.glLinkProgram(p)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
        return if (ok[0] == 0) 0 else p
    }

    companion object {
        const val VS = "attribute vec2 a;void main(){gl_Position=vec4(a,0.,1.);}"

        const val FS = """
precision mediump float;
uniform vec2 r; uniform float t; uniform vec2 m; uniform float e; uniform float ac; uniform float sv;
float h(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
float n(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);
  return mix(mix(h(i),h(i+vec2(1.,0.)),f.x),mix(h(i+vec2(0.,1.)),h(i+vec2(1.,1.)),f.x),f.y);}
float fbm(vec2 p){float v=0.,a=.5;for(int i=0;i<5;i++){v+=a*n(p);p=p*2.03+vec2(1.7,9.2);a*=.5;}return v;}
void main(){
  vec2 uv=(gl_FragCoord.xy-.5*r)/r.x;
  vec2 par=m*vec2(.07,.05);
  float tt=t*.018;
  vec2 q=vec2(fbm(uv*1.4+par+vec2(tt,0.)),fbm(uv*1.4+par+vec2(4.2,-tt)));
  vec2 w=vec2(fbm(uv*1.9+q*1.6+vec2(-tt*.7,tt*.4)),fbm(uv*1.7+q*1.3+vec2(1.7,9.2)));
  float f=fbm(uv*1.6+w*1.5+par*.5);
  vec3 c1=vec3(0.,1.,.62); vec3 c2=vec3(0.,.8,1.); vec3 c3=vec3(.55,.36,1.);
  vec3 col=mix(vec3(.004,.022,.017),c2*.30,smoothstep(.35,.92,f));
  col=mix(col,c1*.52,pow(smoothstep(.55,1.05,f),2.2));
  col+=c3*.10*smoothstep(.55,1.,w.x)*q.y;
  float kol=exp(-pow((uv.x*.85-uv.y*.42+.05*sin(uv.y*3.+tt))*3.,2.));
  col+=mix(c1,c2,q.x)*kol*smoothstep(.4,.95,f)*.24;
  vec2 g=floor(gl_FragCoord.xy/1.6); float s=h(g);
  col+=vec3(.8,1.,.95)*step(.9978,s)*(.45+.55*sin(t*1.7+s*80.));
  vec2 g2=floor((gl_FragCoord.xy+par*40.)/3.); float s2=h(g2+7.);
  col+=vec3(.7,.95,1.)*step(.9992,s2)*.8;
  vec2 o=uv-vec2(0.,.10);
  float d=length(o);
  float R=.15+.012*sin(t*1.3)+.03*sv+.015*e*sin(t*7.);
  float cekirdek=exp(-d*d/(R*R)*2.6)*(.85+.55*e+.6*sv);
  float hale=exp(-d/R*1.25)*(.30+.2*e);
  float aci=atan(o.y,o.x);
  float h1=smoothstep(.007,0.,abs(d-R*1.85-.006*sin(t*.9)))*(.45+.25*sin(aci*3.+t*(.6+e*2.)));
  float h2=smoothstep(.004,0.,abs(d-R*2.45))*.30*(.5+.5*sin(aci*9.-t*(.4+e)));
  float h3=smoothstep(.003,0.,abs(d-R*3.2))*.14*step(.5,fract(aci*6.283+t*.05));
  vec3 orb=mix(c1,vec3(1.),.4)*cekirdek+c1*hale+mix(c1,c2,.55)*(h1+h2+h3);
  col+=orb*ac;
  col*=smoothstep(1.95,.25,length(uv*vec2(1.,.75)));
  gl_FragColor=vec4(col,1.);
}
"""
    }
}
