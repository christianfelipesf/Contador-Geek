package com.exemplo.countdownwallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.parseColor("#0B1020"))
        }

        val titulo = TextView(this).apply {
            text = "Countdown Geek"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitulo = TextView(this).apply {
            text = "Vision Quest • Zelda OOT • GTA 6 • Vingadores Doomsday"
            textSize = 14f
            setTextColor(Color.parseColor("#CBD5E1"))
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 48)
        }

        val botao = Button(this).apply {
            text = "Definir como papel de parede"
            setOnClickListener { abrirSeletorWallpaper() }
        }

        val dica = TextView(this).apply {
            text = "Ou segure na tela inicial > Papel de parede > Papéis de parede animados > Countdown Geek"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, 48, 0, 0)
        }

        layout.addView(titulo)
        layout.addView(subtitulo)
        layout.addView(botao)
        layout.addView(dica)
        setContentView(layout)
    }

    private fun abrirSeletorWallpaper() {
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, CountdownWallpaperService::class.java)
                )
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback: abre a lista de live wallpapers do sistema
            try {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }
}
