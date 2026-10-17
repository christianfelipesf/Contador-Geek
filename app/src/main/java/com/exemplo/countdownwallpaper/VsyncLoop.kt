package com.exemplo.countdownwallpaper

import android.os.Handler
import android.os.SystemClock
import android.view.Choreographer

/**
 * Loop com vsync de verdade (Choreographer) + throttle para 30fps equilíbrio.
 *
 * Antes: Thread.sleep(frameMs) fora de fase com o display -> jitter.
 * Agora: Choreographer dispara no vsync (60/120Hz) na main thread e só
 * despacha o draw pra GL thread quando passou targetFrameMs desde o último.
 */
class VsyncLoop(
    private val mainHandler: Handler,
    private val glHandler: Handler,
    private val settings: () -> RenderSettings,
    private val onDrawFrame: () -> Unit
) : Choreographer.FrameCallback {

    @Volatile var running = false
        private set

    private var lastDrawMs = 0L
    private var scheduled = false

    fun start() {
        if (running) return
        running = true
        lastDrawMs = 0L
        postNext()
    }

    fun stop() {
        running = false
        try { Choreographer.getInstance().removeFrameCallback(this) } catch (e: Exception) { }
        scheduled = false
    }

    private fun postNext() {
        if (!running || scheduled) return
        scheduled = true
        try {
            Choreographer.getInstance().postFrameCallback(this)
        } catch (e: Exception) {
            scheduled = false
            // Fallback: agenda via handler se Choreographer falhar
            mainHandler.postDelayed({ scheduled = false; doFrameFallback() }, 33L)
        }
    }

    private fun doFrameFallback() {
        if (!running) return
        dispatchIfDue()
        postNext()
    }

    override fun doFrame(frameTimeNanos: Long) {
        scheduled = false
        if (!running) return
        dispatchIfDue()
        postNext()
    }

    private fun dispatchIfDue() {
        val now = SystemClock.uptimeMillis()
        val target = settings().targetFrameMs.coerceIn(16L, 100L)
        if (lastDrawMs != 0L && now - lastDrawMs < target) return
        lastDrawMs = now
        // Desenha na GL thread; nunca bloqueia a main.
        try {
            glHandler.post { if (running) { try { onDrawFrame() } catch (e: Exception) { } } }
        } catch (e: Exception) { }
    }
}
