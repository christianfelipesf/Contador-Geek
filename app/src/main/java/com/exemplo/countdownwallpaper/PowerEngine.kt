package com.exemplo.countdownwallpaper

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.view.Surface

/**
 * Engine V2 — EGL:
 * - Tenta OpenGL ES 2.0 primeiro (config idêntica à engine antiga,
 *   compatibilidade máxima) com fallback pra ES 3.0. Os shaders são
 *   ES2 mesmo, então ES3 não traria ganho — só risco.
 * - eglMakeCurrent 1x (não por frame), vsync via eglSwapInterval(1).
 * - Reusa os shaders testados do GlEngine (visual idêntico ao preview.html).
 */
class PowerEgl {
    private var display: EGLDisplay? = null
    private var context: EGLContext? = null
    private var surface: EGLSurface? = null
    var isGles3 = false
        private set
    private var currentBound = false

    fun init(surface: Surface, preferGles3: Boolean = false): Boolean {
        release()
        // ES2 primeiro (caminho conhecido e testado), ES3 só fallback.
        if (tryInit(surface, 2)) {
            isGles3 = false
            return true
        }
        if (preferGles3 && tryInit(surface, 3)) {
            isGles3 = true
            return true
        }
        return false
    }

    private fun tryInit(surface: Surface, version: Int): Boolean {
        var d: EGLDisplay? = null
        var ctx: EGLContext? = null
        var surf: EGLSurface? = null
        try {
            d = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY) ?: return false
            val ver = IntArray(2)
            if (!EGL14.eglInitialize(d, ver, 0, ver, 1)) return false
            val renderBit = if (version >= 3) 0x40 /* EGL_OPENGL_ES3_BIT */ else EGL14.EGL_OPENGL_ES2_BIT
            val cfgAttr = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, renderBit,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_NONE
            )
            val cfgs = arrayOfNulls<EGLConfig>(1)
            val n = IntArray(1)
            if (!EGL14.eglChooseConfig(d, cfgAttr, 0, cfgs, 0, 1, n, 0) || n[0] == 0) {
                EGL14.eglTerminate(d)
                return false
            }
            val ctxAttr = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, version, EGL14.EGL_NONE)
            ctx = EGL14.eglCreateContext(d, cfgs[0], EGL14.EGL_NO_CONTEXT, ctxAttr, 0)
            if (ctx == null || ctx == EGL14.EGL_NO_CONTEXT) {
                EGL14.eglTerminate(d)
                return false
            }
            surf = EGL14.eglCreateWindowSurface(d, cfgs[0], surface, intArrayOf(EGL14.EGL_NONE), 0)
            if (surf == null || surf == EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroyContext(d, ctx)
                EGL14.eglTerminate(d)
                return false
            }
            if (!EGL14.eglMakeCurrent(d, surf, surf, ctx)) {
                EGL14.eglDestroySurface(d, surf)
                EGL14.eglDestroyContext(d, ctx)
                EGL14.eglTerminate(d)
                return false
            }
            EGL14.eglSwapInterval(d, 1) // vsync: sem tearing, Choreographer decide o ritmo
            display = d
            context = ctx
            this.surface = surf
            currentBound = true
            return true
        } catch (e: Exception) {
            try {
                if (d != null && d != EGL14.EGL_NO_DISPLAY) {
                    if (surf != null && surf != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(d, surf)
                    if (ctx != null && ctx != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(d, ctx)
                    EGL14.eglTerminate(d)
                }
            } catch (e2: Exception) { }
            return false
        }
    }

    /** Só re-binda se perdeu o current (resize, troca de superfície). Custo ~0 no steady-state. */
    fun ensureCurrent(): Boolean {
        return try {
            if (currentBound) return true
            val d = display ?: return false
            EGL14.eglMakeCurrent(d, surface, surface, context).also {
                currentBound = it
            }
        } catch (e: Exception) {
            false
        }
    }

    fun swap(): Boolean = try {
        EGL14.eglSwapBuffers(display, surface)
    } catch (e: Exception) {
        currentBound = false
        false
    }

    fun onSurfaceLost() {
        currentBound = false
    }

    fun release() {
        try {
            val d = display
            if (d != null && d != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(d, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (surface != null && surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(d, surface)
                if (context != null && context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(d, context)
                EGL14.eglTerminate(d)
            }
        } catch (e: Exception) { }
        display = null
        context = null
        surface = null
        currentBound = false
        isGles3 = false
    }
}

/**
 * Fundo em meia-resolução (FBO): o FS_BG é o maior custo por frame
 * (glows + hash de estrelas por pixel). Renderar a 0.5x corta o fillrate
 * em ~75% e o upscale bilinear sai de graça. Em GPU fraca é o que mais
 * destrava; em GPU forte nem se nota a diferença visual.
 *
 * Reusa o QuadRenderer existente pro upscale — nenhum shader novo.
 */
class HalfResFbo {
    private var fbo = 0
    private var tex = 0
    private var hw = 0
    private var hh = 0
    var ready = false
        private set

    fun ensure(fullW: Int, fullH: Int, scale: Float = 0.5f): Boolean {
        val wantW = (fullW * scale).toInt().coerceAtLeast(160)
        val wantH = (fullH * scale).toInt().coerceAtLeast(160)
        if (ready && wantW == hw && wantH == hh && fbo != 0 && tex != 0) return true
        release()
        return try {
            val fb = IntArray(1)
            GLES20.glGenFramebuffers(1, fb, 0)
            val tx = IntArray(1)
            GLES20.glGenTextures(1, tx, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tx[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                wantW, wantH, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                null as java.nio.Buffer?
            )
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb[0])
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, tx[0], 0
            )
            val ok = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) ==
                GLES20.GL_FRAMEBUFFER_COMPLETE
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            if (!ok) {
                GLES20.glDeleteFramebuffers(1, fb, 0)
                GLES20.glDeleteTextures(1, tx, 0)
                ready = false
                false
            } else {
                fbo = fb[0]
                tex = tx[0]
                hw = wantW
                hh = wantH
                ready = true
                true
            }
        } catch (e: Exception) {
            ready = false
            false
        }
    }

    fun begin(): Boolean {
        if (!ready) return false
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo)
        GLES20.glViewport(0, 0, hw, hh)
        return true
    }

    /**
     * Volta pro backbuffer e estica o FBO pra tela cheia (upscale bilinear).
     * Limpa a tela antes: garante que nunca acumula frame antigo (rastro).
     * Retorna false se o caller deve desenhar o BG direto (fallback).
     */
    fun endAndUpscale(
        quads: GlEngine.QuadRenderer,
        fullW: Float, fullH: Float
    ): Boolean {
        if (!ready) return false
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, fullW.toInt(), fullH.toInt())
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        // Fundo é opaco: desliga blend no upscale pra economizar banda.
        GLES20.glDisable(GLES20.GL_BLEND)
        quads.draw(tex, 0f, 0f, fullW, fullH, fullW, fullH, 1f, 1f, 1f, 1f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        return true
    }

    fun halfW(): Float = hw.toFloat()
    fun halfH(): Float = hh.toFloat()

    fun release() {
        try {
            if (fbo != 0) GLES20.glDeleteFramebuffers(1, intArrayOf(fbo), 0)
            if (tex != 0) GLES20.glDeleteTextures(1, intArrayOf(tex), 0)
        } catch (e: Exception) { }
        fbo = 0
        tex = 0
        hw = 0
        hh = 0
        ready = false
    }
}
