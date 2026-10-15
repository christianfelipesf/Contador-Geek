package com.exemplo.countdownwallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Sprite de textura: bitmap assado uma vez (texto/card), redesenhado como quad barato. */
class Sprite(var tex: Int = 0, var w: Float = 0f, var h: Float = 0f, var base: Float = 0f)

data class Baked(val tex: Int, val w: Float, val h: Float, val base: Float)

/** Núcleo EGL + shaders + bake de texturas. Tudo roda na thread de desenho. */
object GlEngine {

    // ---------------- EGL ----------------

    class GlCore {
        private var display: EGLDisplay? = null
        private var context: EGLContext? = null
        private var surface: EGLSurface? = null

        fun init(surface: Surface): Boolean {
            try {
                val d = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                    ?: return false
                display = d
                val ver = IntArray(2)
                if (!EGL14.eglInitialize(d, ver, 0, ver, 1)) {
                    release()
                    return false
                }
                val cfgAttr = intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_NONE
                )
                val cfgs = arrayOfNulls<EGLConfig>(1)
                val n = IntArray(1)
                if (!EGL14.eglChooseConfig(d, cfgAttr, 0, cfgs, 0, 1, n, 0) || n[0] == 0) {
                    release()
                    return false
                }
                val ctxAttr = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
                val ctx = EGL14.eglCreateContext(d, cfgs[0], EGL14.EGL_NO_CONTEXT, ctxAttr, 0)
                if (ctx == null || ctx == EGL14.EGL_NO_CONTEXT) {
                    release()
                    return false
                }
                context = ctx
                val surf = EGL14.eglCreateWindowSurface(d, cfgs[0], surface, intArrayOf(EGL14.EGL_NONE), 0)
                if (surf == null || surf == EGL14.EGL_NO_SURFACE) {
                    release()
                    return false
                }
                this.surface = surf
                if (!EGL14.eglMakeCurrent(d, surf, surf, ctx)) {
                    release()
                    return false
                }
                EGL14.eglSwapInterval(d, 1) // vsync: sem tearing
                return true
            } catch (e: Exception) {
                release()
                return false
            }
        }

        fun makeCurrent(): Boolean = try {
            EGL14.eglMakeCurrent(display, surface, surface, context)
        } catch (e: Exception) {
            false
        }

        fun swap(): Boolean = try {
            EGL14.eglSwapBuffers(display, surface)
        } catch (e: Exception) {
            false
        }

        fun release() {
            try {
                val d = display
                if (d != null && d != EGL14.EGL_NO_DISPLAY) {
                    EGL14.eglMakeCurrent(
                        d,
                        EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_CONTEXT
                    )
                    if (surface != null && surface != EGL14.EGL_NO_SURFACE) {
                        EGL14.eglDestroySurface(d, surface)
                    }
                    if (context != null && context != EGL14.EGL_NO_CONTEXT) {
                        EGL14.eglDestroyContext(d, context)
                    }
                    EGL14.eglTerminate(d)
                }
            } catch (e: Exception) {
            }
            display = null
            context = null
            surface = null
        }
    }

    // ---------------- shaders ----------------

    private fun shader(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        return s
    }

    private fun program(vs: String, fs: String): Int {
        val p = GLES20.glCreateProgram()
        if (p == 0) return 0
        GLES20.glAttachShader(p, shader(GLES20.GL_VERTEX_SHADER, vs))
        GLES20.glAttachShader(p, shader(GLES20.GL_FRAGMENT_SHADER, fs))
        GLES20.glLinkProgram(p)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) {
            GLES20.glDeleteProgram(p)
            return 0
        }
        return p
    }

    private fun unitQuadVbo(): Int {
        val data = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)
        val buf = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buf.put(data).position(0)
        val ids = IntArray(1)
        GLES20.glGenBuffers(1, ids, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ids[0])
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.size * 4, buf, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        return ids[0]
    }

    /** Fundo animado inteiro num fragment shader: gradiente + 2 glows + faixa + grid + estrelas + vinheta. */
    class BgRenderer {
        private var prog = 0
        private var vbo = 0
        private var uRes = -1
        private var uTime = -1
        private var uTop = -1
        private var uMid = -1
        private var uBot = -1
        private var uGlowA = -1
        private var uGlowB = -1
        private var uStars = -1
        private var aPos = -1

        fun init(): Boolean {
            prog = program(VS_BG, FS_BG)
            if (prog == 0) return false
            aPos = GLES20.glGetAttribLocation(prog, "aPos")
            uRes = GLES20.glGetUniformLocation(prog, "uRes")
            uTime = GLES20.glGetUniformLocation(prog, "uTime")
            uTop = GLES20.glGetUniformLocation(prog, "uTop")
            uMid = GLES20.glGetUniformLocation(prog, "uMid")
            uBot = GLES20.glGetUniformLocation(prog, "uBot")
            uGlowA = GLES20.glGetUniformLocation(prog, "uGlowA")
            uGlowB = GLES20.glGetUniformLocation(prog, "uGlowB")
            uStars = GLES20.glGetUniformLocation(prog, "uStars")
            val data = floatArrayOf(-1f, -1f, 3f, -1f, -1f, 3f)
            val buf = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            buf.put(data).position(0)
            val ids = IntArray(1)
            GLES20.glGenBuffers(1, ids, 0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ids[0])
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.size * 4, buf, GLES20.GL_STATIC_DRAW)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            vbo = ids[0]
            return true
        }

        fun draw(w: Float, h: Float, t: Float, top: Int, mid: Int, bot: Int, stars: Float) {
            if (prog == 0) return
            GLES20.glUseProgram(prog)
            GLES20.glUniform2f(uRes, w, h)
            GLES20.glUniform1f(uTime, t)
            GLES20.glUniform3f(uTop, Color.red(top) / 255f, Color.green(top) / 255f, Color.blue(top) / 255f)
            GLES20.glUniform3f(uMid, Color.red(mid) / 255f, Color.green(mid) / 255f, Color.blue(mid) / 255f)
            GLES20.glUniform3f(uBot, Color.red(bot) / 255f, Color.green(bot) / 255f, Color.blue(bot) / 255f)
            GLES20.glUniform3f(uGlowA, 192f / 255f, 132f / 255f, 252f / 255f)
            GLES20.glUniform3f(uGlowB, 240f / 255f, 120f / 255f, 200f / 255f)
            GLES20.glUniform1f(uStars, stars)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
            GLES20.glEnableVertexAttribArray(aPos)
            GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 0, 0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
            GLES20.glDisableVertexAttribArray(aPos)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
    }

    /** Quad texturizado com tint: cards, textos, halo. */
    class QuadRenderer {
        private var prog = 0
        private var vbo = 0
        private var aPos = -1
        private var uP = -1
        private var uS = -1
        private var uRes = -1
        private var uTex = -1
        private var uTint = -1

        fun init(): Boolean {
            prog = program(VS_QUAD, FS_QUAD)
            if (prog == 0) return false
            aPos = GLES20.glGetAttribLocation(prog, "aPos")
            uP = GLES20.glGetUniformLocation(prog, "uP")
            uS = GLES20.glGetUniformLocation(prog, "uS")
            uRes = GLES20.glGetUniformLocation(prog, "uRes")
            uTex = GLES20.glGetUniformLocation(prog, "uTex")
            uTint = GLES20.glGetUniformLocation(prog, "uTint")
            vbo = unitQuadVbo()
            return true
        }

        fun draw(tex: Int, x: Float, y: Float, w: Float, h: Float, resW: Float, resH: Float,
                 r: Float, g: Float, b: Float, a: Float) {
            if (prog == 0 || tex == 0) return
            GLES20.glUseProgram(prog)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex)
            GLES20.glUniform1i(uTex, 0)
            GLES20.glUniform2f(uP, x, y)
            GLES20.glUniform2f(uS, w, h)
            GLES20.glUniform2f(uRes, resW, resH)
            GLES20.glUniform4f(uTint, r, g, b, a)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
            GLES20.glEnableVertexAttribArray(aPos)
            GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 0, 0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(aPos)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }

        fun drawWhite(tex: Int, x: Float, y: Float, w: Float, h: Float, resW: Float, resH: Float) {
            draw(tex, x, y, w, h, resW, resH, 1f, 1f, 1f, 1f)
        }
    }

    // ---------------- texturas ----------------

    fun uploadBitmap(bmp: Bitmap): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        return ids[0]
    }

    fun deleteTex(vararg ids: Int) {
        val valid = ids.filter { it != 0 }.toIntArray()
        if (valid.isNotEmpty()) GLES20.glDeleteTextures(valid.size, valid, 0)
    }

    /** Assa um texto em textura (roda 1x; redesenhar o quad por frame é de graça). */
    fun uploadText(text: String, sizePx: Float, color: Int, bold: Boolean): Baked {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizePx
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        val fm = p.fontMetrics
        val pad = 8f
        val w = (p.measureText(text) + pad * 2).coerceAtLeast(2f)
        val h = (fm.descent - fm.ascent + pad * 2 + 8f).coerceAtLeast(2f)
        val bmp = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawText(text, pad, pad - fm.ascent + 4f, p)
        val tex = uploadBitmap(bmp)
        bmp.recycle()
        return Baked(tex, w, h, pad - fm.ascent + 4f)
    }

    /** Fundo do card: roundRect escuro + barrinha de destaque (igual ao original). */
    fun bakeCardBmp(wPx: Int, hPx: Int, barColor: Int): Bitmap {
        val bmp = Bitmap.createBitmap(wPx.coerceAtLeast(2), hPx.coerceAtLeast(2), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(200, 30, 16, 60) }
        c.drawRoundRect(0f, 0f, w, h, 32f, 32f, card)
        val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = barColor }
        c.drawRoundRect(0f, 0f, 14f, h, 14f, 14f, bar)
        return bmp
    }

    /** Selo radial branco (o halo tinge via tint). */
    fun radialBmp(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint().apply {
            shader = RadialGradient(
                size / 2f, size / 2f, size / 2f,
                Color.argb(255, 255, 255, 255), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), p)
        return bmp
    }

    private const val VS_BG = """
        attribute vec2 aPos;
        void main() { gl_Position = vec4(aPos, 0.0, 1.0); }
    """

    private const val FS_BG = """
        precision mediump float;
        uniform vec2 uRes;
        uniform float uTime;
        uniform vec3 uTop;
        uniform vec3 uMid;
        uniform vec3 uBot;
        uniform vec3 uGlowA;
        uniform vec3 uGlowB;
        uniform float uStars;

        float hash21(vec2 p) {
          vec3 p3 = fract(vec3(p.xyx) * 0.1031);
          p3 += dot(p3, p3.yzx + 33.33);
          return fract((p3.x + p3.y) * p3.z);
        }

        void main() {
          vec2 uv = gl_FragCoord.xy / uRes;
          float y = 1.0 - uv.y;
          float aspect = uRes.x / uRes.y;

          float mid = 0.45 + 0.12 * sin(uTime * 0.6);
          vec3 col = mix(uTop, uMid, smoothstep(0.0, mid, y));
          col = mix(col, uBot, smoothstep(mid, 1.0, y));

          float pulse = 0.75 + 0.25 * sin(uTime * 2.0);

          vec2 c1 = vec2(0.5 + 0.38 * sin(uTime * 0.6), 0.75 - 0.14 * cos(uTime * 0.5));
          float d1 = length((uv - c1) * vec2(aspect, 1.0));
          col += uGlowA * smoothstep(0.55, 0.0, d1) * (0.55 * pulse);

          vec2 c2 = vec2(0.5 + 0.38 * cos(uTime * 0.5 + 1.5), 0.28 - 0.14 * sin(uTime * 0.55 + 0.7));
          float d2 = length((uv - c2) * vec2(aspect, 1.0));
          col += uGlowB * smoothstep(0.6, 0.0, d2) * (0.5 * (1.6 - pulse));

          float px = mod(uTime * 0.12, 1.6) - 0.3;
          float bd = abs(uv.x - px + (uv.y - 0.5) * 0.32);
          col += vec3(1.0) * smoothstep(0.14, 0.0, bd) * 0.05;

          float gx = smoothstep(0.985, 1.0, fract(uv.x * 12.0));
          float gy = smoothstep(0.992, 1.0, fract(uv.y * 24.0));
          col += vec3(1.0) * (gx + gy) * 0.02;

          if (uStars > 0.5) {
            vec2 sp = vec2(uv.x * 36.0, uv.y * 64.0 + uTime * 0.8);
            vec2 cell = floor(sp);
            vec2 f = fract(sp);
            float h = hash21(cell);
            float on = step(1.0 - uStars * 0.004, h);
            vec2 spos = vec2(hash21(cell + 7.13), hash21(cell + 3.71));
            float sd = length(f - spos);
            float tw = 0.5 + 0.5 * sin(uTime * 3.0 + h * 40.0);
            col += vec3(0.9, 0.86, 1.0) * (smoothstep(0.11, 0.0, sd) * on * tw);
          }

          float vg = smoothstep(1.1, 0.35, length((uv - 0.5) * vec2(aspect, 1.0)));
          col *= mix(0.55, 1.0, vg);

          gl_FragColor = vec4(col, 1.0);
        }
    """

    private const val VS_QUAD = """
        attribute vec2 aPos;
        uniform vec2 uP;
        uniform vec2 uS;
        uniform vec2 uRes;
        varying vec2 vUv;
        void main() {
          vec2 px = uP + aPos * uS;
          vec2 clip = vec2(px.x / uRes.x * 2.0 - 1.0, 1.0 - px.y / uRes.y * 2.0);
          gl_Position = vec4(clip, 0.0, 1.0);
          vUv = aPos;
        }
    """

    private const val FS_QUAD = """
        precision mediump float;
        uniform sampler2D uTex;
        uniform vec4 uTint;
        varying vec2 vUv;
        void main() {
          gl_FragColor = texture2D(uTex, vUv) * uTint;
        }
    """
}
