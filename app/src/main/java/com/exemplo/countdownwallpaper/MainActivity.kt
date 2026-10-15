package com.exemplo.countdownwallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.exemplo.countdownwallpaper.CountdownWallpaperService.Prefs

class MainActivity : Activity() {

    private fun prefs() = getSharedPreferences(Prefs.FILE, MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.parseColor("#0B1020"))
        }

        fun titulo(txt: String) = TextView(this).apply {
            text = txt; textSize = 26f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        }
        fun subtitulo(txt: String) = TextView(this).apply {
            text = txt; textSize = 14f; setTextColor(Color.parseColor("#CBD5E1"))
            gravity = Gravity.CENTER; setPadding(0, 16, 0, 24)
        }
        fun secao(txt: String) = TextView(this).apply {
            text = txt; textSize = 16f; setTextColor(Color.parseColor("#A78BFA"))
            setPadding(0, 32, 0, 8)
        }
        fun rotulo(txt: String) = TextView(this).apply {
            text = txt; textSize = 13f; setTextColor(Color.WHITE); setPadding(0, 12, 0, 0)
        }

        layout.addView(titulo("Countdown Geek"))
        layout.addView(subtitulo("Vision Quest • Zelda OOT • GTA 6 • Vingadores Doomsday"))

        layout.addView(Button(this).apply {
            text = "Definir como papel de parede"
            setOnClickListener { abrirSeletorWallpaper() }
        })
        layout.addView(TextView(this).apply {
            text = "Ou segure na tela inicial > Papel de parede > Papéis de parede animados > Countdown Geek"
            textSize = 12f; setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER; setPadding(0, 16, 0, 0)
        })

        // ---------- ANIMAÇÃO ----------
        layout.addView(secao("Animação"))

        val p = prefs()
        val lblVel = rotulo("")
        val skVel = SeekBar(this).apply { max = 175 } // 25..200%
        fun mostraVel(v: Int) { lblVel.text = "Velocidade: ${v + 25}%" }
        skVel.progress = (p.getFloat(Prefs.SPEED, 1f) * 100).toInt() - 25
        mostraVel(skVel.progress)
        skVel.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {
                mostraVel(v)
                p.edit().putFloat(Prefs.SPEED, (v + 25) / 100f).apply()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        layout.addView(lblVel); layout.addView(skVel)

        val lblFps = rotulo("")
        val frames = intArrayOf(16, 33, 66, 120) // ~60/30/15/8 fps (fundo em GPU aguenta 60)
        val skFps = SeekBar(this).apply { max = 3 }
        fun mostraFps(i: Int) { lblFps.text = "Fluidez: ~${1000 / frames[i]} fps" }
        val fiPadrao = frames.indexOf(p.getInt(Prefs.FRAME_MS, 33)).coerceAtLeast(0)
        skFps.progress = fiPadrao; mostraFps(fiPadrao)
        skFps.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {
                mostraFps(v)
                p.edit().putInt(Prefs.FRAME_MS, frames[v]).apply()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        layout.addView(lblFps); layout.addView(skFps)

        val lblPart = rotulo("")
        val skPart = SeekBar(this).apply { max = 80 }
        fun mostraPart(v: Int) { lblPart.text = "Partículas: $v" }
        skPart.progress = p.getInt(Prefs.PARTICLES, 30); mostraPart(skPart.progress)
        skPart.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {
                mostraPart(v)
                p.edit().putInt(Prefs.PARTICLES, v).apply()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        layout.addView(lblPart); layout.addView(skPart)

        // ---------- SELEÇÃO (HOVER) ----------
        layout.addView(secao("Seleção dos cards"))

        layout.addView(CheckBox(this).apply {
            text = "Destacar um card por vez"; setTextColor(Color.WHITE)
            isChecked = p.getBoolean(Prefs.HOVER, true)
            setOnCheckedChangeListener { _, v -> p.edit().putBoolean(Prefs.HOVER, v).apply() }
        })

        val lblHover = rotulo("")
        val skHover = SeekBar(this).apply { max = 12 } // 3..15s parado
        fun mostraHover(v: Int) { lblHover.text = "Tempo parado por card: ${v + 3}s" }
        skHover.progress = (p.getLong(Prefs.HOVER_MS, 3000L) / 1000).toInt() - 3
        mostraHover(skHover.progress)
        skHover.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {
                mostraHover(v)
                p.edit().putLong(Prefs.HOVER_MS, (v + 3) * 1000L).apply()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        layout.addView(lblHover); layout.addView(skHover)

        val lblTr = rotulo("")
        val skTr = SeekBar(this).apply { max = 20 } // 0..2000ms de transição
        fun mostraTr(v: Int) { lblTr.text = "Transição entre cards: ${v * 100}ms" }
        skTr.progress = (p.getLong(Prefs.HOVER_TR, 600L) / 100).toInt().coerceIn(0, 20)
        mostraTr(skTr.progress)
        skTr.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {
                mostraTr(v)
                p.edit().putLong(Prefs.HOVER_TR, v * 100L).apply()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        layout.addView(lblTr); layout.addView(skTr)

        layout.addView(CheckBox(this).apply {
            text = "Card cresce ao selecionar (zoom)"; setTextColor(Color.WHITE)
            isChecked = p.getBoolean(Prefs.ZOOM, true)
            setOnCheckedChangeListener { _, v -> p.edit().putBoolean(Prefs.ZOOM, v).apply() }
        })

        // ---------- CORES ----------
        layout.addView(secao("Cores do fundo (topo / meio / base)"))

        var etTop: EditText? = null
        var etMid: EditText? = null
        var etBot: EditText? = null

        fun preset(nome: String, top: String, mid: String, bot: String) = Button(this).apply {
            text = nome
            setOnClickListener {
                p.edit().putString(Prefs.C_TOP, top).putString(Prefs.C_MID, mid).putString(Prefs.C_BOT, bot).apply()
                etTop?.setText(top); etMid?.setText(mid); etBot?.setText(bot)
                Toast.makeText(this@MainActivity, "Tema $nome aplicado", Toast.LENGTH_SHORT).show()
            }
        }

        val rowTemas = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        rowTemas.addView(preset("Roxo", "#150826", "#6D28D9", "#2E1065"))
        rowTemas.addView(preset("Oceano", "#020617", "#0369A1", "#082F49"))
        rowTemas.addView(preset("Fogo", "#1C0A00", "#C2410C", "#431407"))
        layout.addView(rowTemas)

        fun etCor(valor: String) = EditText(this).apply {
            setText(valor); setTextColor(Color.WHITE); textSize = 13f
        }
        val etTopV = etCor(p.getString(Prefs.C_TOP, Prefs.DEF_TOP)!!)
        val etMidV = etCor(p.getString(Prefs.C_MID, Prefs.DEF_MID)!!)
        val etBotV = etCor(p.getString(Prefs.C_BOT, Prefs.DEF_BOT)!!)
        etTop = etTopV; etMid = etMidV; etBot = etBotV
        layout.addView(rotulo("Cor personalizada (hex, ex: #6D28D9):"))
        layout.addView(etTopV); layout.addView(etMidV); layout.addView(etBotV)
        layout.addView(Button(this).apply {
            text = "Aplicar cores"
            setOnClickListener {
                try {
                    Color.parseColor(etTopV.text.toString())
                    Color.parseColor(etMidV.text.toString())
                    Color.parseColor(etBotV.text.toString())
                    p.edit().putString(Prefs.C_TOP, etTopV.text.toString())
                        .putString(Prefs.C_MID, etMidV.text.toString())
                        .putString(Prefs.C_BOT, etBotV.text.toString()).apply()
                    Toast.makeText(this@MainActivity, "Cores aplicadas", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Hex inválido! Ex: #6D28D9", Toast.LENGTH_SHORT).show()
                }
            }
        })

        layout.addView(Button(this).apply {
            text = "Restaurar padrão"
            setOnClickListener {
                p.edit().clear().apply()
                recreate()
            }
        })

        try {
            val pkg = packageManager.getPackageInfo(packageName, 0)
            @Suppress("DEPRECATION")
            val code = pkg.versionCode
            layout.addView(TextView(this).apply {
                text = "Versão ${pkg.versionName} (${code})"
                textSize = 12f
                setTextColor(Color.parseColor("#64748B"))
                gravity = Gravity.CENTER
                setPadding(0, 32, 0, 0)
            })
        } catch (e: Exception) {
        }

        val scroll = ScrollView(this).apply { addView(layout) }
        setContentView(scroll)
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
            try {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }
}
