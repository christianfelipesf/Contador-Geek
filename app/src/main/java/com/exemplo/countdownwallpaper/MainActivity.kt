package com.exemplo.countdownwallpaper

import android.app.Activity
import android.app.LocaleManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.exemplo.countdownwallpaper.CountdownWallpaperService.Prefs
import java.util.Locale

/**
 * Tela de ajustes. Todo texto/medida/cor vem de recursos Android
 * (strings.xml, dimens.xml, colors.xml) — inclusive nos 3 idiomas — pra
 * funcionar com o idioma do app escolhido no sistema.
 */
class MainActivity : Activity() {

    private fun prefs() = getSharedPreferences(Prefs.FILE, MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = resources.getDimensionPixelSize(R.dimen.espaco_tela)
            setPadding(p, p, p, p)
            setBackgroundColor(getColor(R.color.tema_fundo))
        }

        fun TextView.tamanho(id: Int) =
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(id))

        fun titulo(txt: String) = TextView(this).apply {
            text = txt; tamanho(R.dimen.txt_titulo); setTextColor(getColor(R.color.texto))
            gravity = Gravity.CENTER
        }
        fun subtitulo(txt: String) = TextView(this).apply {
            text = txt; tamanho(R.dimen.txt_subtitulo); setTextColor(getColor(R.color.texto_secundario))
            gravity = Gravity.CENTER; setPadding(0, dp(6), 0, dp(10))
        }
        fun secao(txt: String) = TextView(this).apply {
            text = txt; tamanho(R.dimen.txt_secao); setTextColor(getColor(R.color.destaque))
            setPadding(0, dp(14), 0, dp(4))
        }
        fun rotulo(txt: String) = TextView(this).apply {
            text = txt; tamanho(R.dimen.txt_rotulo); setTextColor(getColor(R.color.texto))
            setPadding(0, dp(6), 0, 0)
        }
        fun ajuda(txt: String) = TextView(this).apply {
            text = txt; tamanho(R.dimen.txt_ajuda); setTextColor(getColor(R.color.texto_terciario))
            setPadding(0, dp(2), 0, 0)
        }
        fun caixa(txtRes: Int, valor: Boolean, aoMudar: (Boolean) -> Unit) = CheckBox(this).apply {
            text = getString(txtRes); setTextColor(getColor(R.color.texto))
            isChecked = valor
            setOnCheckedChangeListener { _, v -> aoMudar(v) }
        }

        layout.addView(titulo(getString(R.string.app_name)))
        layout.addView(subtitulo(getString(R.string.cfg_sub)))

        layout.addView(rotulo(getString(R.string.cfg_previa)))
        // Largura limitada (e centralizada): num tablet a prévia não estica
        // pela tela toda — ela imita a proporção de um celular.
        val margem = resources.getDimensionPixelSize(R.dimen.espaco_tela)
        val larguraPrev = minOf(
            resources.displayMetrics.widthPixels - margem * 2,
            resources.getDimensionPixelSize(R.dimen.previa_largura_max)
        )
        layout.addView(BannerPreviewView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                larguraPrev,
                resources.getDimensionPixelSize(R.dimen.previa_altura)
            ).apply {
                setMargins(0, dp(4), 0, dp(4))
                gravity = Gravity.CENTER_HORIZONTAL
            }
        })

        layout.addView(Button(this).apply {
            text = getString(R.string.cfg_definir)
            setOnClickListener { abrirSeletorWallpaper() }
        })
        layout.addView(TextView(this).apply {
            text = getString(R.string.cfg_dica)
            tamanho(R.dimen.txt_ajuda); setTextColor(getColor(R.color.texto_terciario))
            gravity = Gravity.CENTER; setPadding(0, dp(8), 0, 0)
        })

        // ---------- IDIOMA ----------
        layout.addView(secao(getString(R.string.sec_idioma)))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            layout.addView(seletorIdioma())
        } else {
            layout.addView(ajuda(getString(R.string.cfg_idioma_antigo)))
        }

        // ---------- ECONOMIA ----------
        layout.addView(secao(getString(R.string.sec_economia)))

        layout.addView(caixa(R.string.cfg_saver, prefs().getBoolean(Prefs.SAVER, false)) { v ->
            prefs().edit().putBoolean(Prefs.SAVER, v).apply()
            Toast.makeText(
                this@MainActivity,
                getString(if (v) R.string.cfg_saver_on else R.string.cfg_saver_off),
                Toast.LENGTH_SHORT
            ).show()
        })
        layout.addView(ajuda(getString(R.string.cfg_saver_desc)))

        // ---------- ANIMAÇÃO ----------
        layout.addView(secao(getString(R.string.sec_animacao)))

        val p = prefs()
        val lblVel = rotulo("")
        val skVel = SeekBar(this).apply { max = 175 } // 25..200%
        fun mostraVel(v: Int) { lblVel.text = getString(R.string.cfg_velocidade, v + 25) }
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
        // Engine V2: vsync + throttle (equilíbrio 30fps). 16 = performance 60fps.
        val frames = longArrayOf(16L, 33L, 50L, 66L)
        val nomes = resources.getStringArray(R.array.fps_nomes)
        val skFps = SeekBar(this).apply { max = 3 }
        fun mostraFps(i: Int) { lblFps.text = getString(R.string.cfg_fluidez, nomes[i]) }
        val salvo = try { p.getLong(Prefs.FRAME_TARGET_MS, p.getInt(Prefs.FRAME_MS, 33).toLong()) } catch (e: Exception) { 33L }
        val idxSalvo = frames.indexOfFirst { it == salvo }
        val fiPadrao = if (idxSalvo >= 0) idxSalvo else 1
        skFps.progress = fiPadrao; mostraFps(fiPadrao)
        skFps.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {
                mostraFps(v)
                p.edit().putLong(Prefs.FRAME_TARGET_MS, frames[v]).apply()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        layout.addView(lblFps); layout.addView(skFps)

        val lblPart = rotulo("")
        val skPart = SeekBar(this).apply { max = 80 }
        fun mostraPart(v: Int) { lblPart.text = getString(R.string.cfg_particulas, v) }
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
        layout.addView(secao(getString(R.string.sec_selecao)))

        layout.addView(caixa(R.string.cfg_hover, p.getBoolean(Prefs.HOVER, true)) { v ->
            p.edit().putBoolean(Prefs.HOVER, v).apply()
        })

        val lblHover = rotulo("")
        val skHover = SeekBar(this).apply { max = 12 } // 3..15s parado
        fun mostraHover(v: Int) { lblHover.text = getString(R.string.cfg_hover_ms, v + 3) }
        skHover.progress = ((p.getLong(Prefs.HOVER_MS, 9000L) / 1000).toInt().coerceIn(3, 15) - 3)
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
        fun mostraTr(v: Int) { lblTr.text = getString(R.string.cfg_hover_tr, v * 100) }
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

        layout.addView(caixa(R.string.cfg_zoom, p.getBoolean(Prefs.ZOOM, true)) { v ->
            p.edit().putBoolean(Prefs.ZOOM, v).apply()
        })

        layout.addView(secao(getString(R.string.sec_banners)))

        layout.addView(caixa(R.string.cfg_banners, p.getBoolean(Prefs.BANNERS, true)) { v ->
            p.edit().putBoolean(Prefs.BANNERS, v).apply()
        })

        val lblBms = rotulo("")
        val skBms = SeekBar(this).apply { max = 22 } // 8..30s parado
        fun mostraBms(v: Int) { lblBms.text = getString(R.string.cfg_banner_ms, v + 8) }
        skBms.progress = (p.getLong(Prefs.BANNER_MS, 18000L) / 1000).toInt().coerceIn(8, 30) - 8
        mostraBms(skBms.progress)
        skBms.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {
                mostraBms(v)
                p.edit().putLong(Prefs.BANNER_MS, (v + 8) * 1000L).apply()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        layout.addView(lblBms); layout.addView(skBms)

        val lblBtr = rotulo("")
        val skBtr = SeekBar(this).apply { max = 5 } // 0.5..3.0s de transição
        fun mostraBtr(v: Int) { lblBtr.text = getString(R.string.cfg_banner_tr, (v + 1) * 500) }
        skBtr.progress = (p.getLong(Prefs.BANNER_TR, 1500L) / 500).toInt().coerceIn(1, 6) - 1
        mostraBtr(skBtr.progress)
        skBtr.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {
                mostraBtr(v)
                p.edit().putLong(Prefs.BANNER_TR, (v + 1) * 500L).apply()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        layout.addView(lblBtr); layout.addView(skBtr)

        val lblPan = rotulo("")
        val skPan = SeekBar(this).apply { max = 60 } // 0..60 px/s (0 = parado)
        fun mostraPan(v: Int) {
            lblPan.text = if (v == 0) getString(R.string.cfg_pan_parado)
            else getString(R.string.cfg_pan, v)
        }
        skPan.progress = p.getInt(Prefs.BANNER_PAN, 10).coerceIn(0, 60)
        mostraPan(skPan.progress)
        skPan.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {
                mostraPan(v)
                p.edit().putInt(Prefs.BANNER_PAN, v).apply()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        layout.addView(lblPan); layout.addView(skPan)

        val lblShade = rotulo("")
        val skShade = SeekBar(this).apply { max = 65 } // 30..95% de cobertura roxa
        fun mostraShade(v: Int) { lblShade.text = getString(R.string.cfg_shade, v + 30) }
        skShade.progress = p.getInt(Prefs.BANNER_SHADE, 68).coerceIn(30, 95) - 30
        mostraShade(skShade.progress)
        skShade.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {
                mostraShade(v)
                p.edit().putInt(Prefs.BANNER_SHADE, v + 30).apply()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        layout.addView(lblShade); layout.addView(skShade)

        // ---------- CORES ----------
        layout.addView(secao(getString(R.string.sec_cores)))

        var etTop: EditText? = null
        var etMid: EditText? = null
        var etBot: EditText? = null

        fun preset(nome: String, top: String, mid: String, bot: String) = Button(this).apply {
            text = nome
            setOnClickListener {
                p.edit().putString(Prefs.C_TOP, top).putString(Prefs.C_MID, mid).putString(Prefs.C_BOT, bot).apply()
                etTop?.setText(top); etMid?.setText(mid); etBot?.setText(bot)
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.cfg_tema_aplicado, nome),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Nomes vindos de temas_nomes (traduzidos); os hex são os mesmos em
        // qualquer idioma, então ficam aqui.
        val nomesTemas = resources.getStringArray(R.array.temas_nomes)
        val hexTemas = listOf(
            arrayOf("#150826", "#6D28D9", "#2E1065"),
            arrayOf("#020617", "#0369A1", "#082F49"),
            arrayOf("#1C0A00", "#C2410C", "#431407"),
            arrayOf("#02120A", "#16A34A", "#052E16"),
            arrayOf("#20063B", "#EC4899", "#F59E0B"),
            arrayOf("#000000", "#1D4ED8", "#020617")
        )
        for (linha in 0 until 2) {
            val rowLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (col in 0 until 3) {
                val t = linha * 3 + col
                rowLayout.addView(preset(nomesTemas[t], hexTemas[t][0], hexTemas[t][1], hexTemas[t][2]).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )
                })
            }
            layout.addView(rowLayout)
        }

        fun etCor(valor: String) = EditText(this).apply {
            setText(valor); setTextColor(getColor(R.color.texto)); tamanho(R.dimen.txt_campo_cor)
        }
        val etTopV = etCor(p.getString(Prefs.C_TOP, Prefs.DEF_TOP)!!)
        val etMidV = etCor(p.getString(Prefs.C_MID, Prefs.DEF_MID)!!)
        val etBotV = etCor(p.getString(Prefs.C_BOT, Prefs.DEF_BOT)!!)
        etTop = etTopV; etMid = etMidV; etBot = etBotV
        layout.addView(rotulo(getString(R.string.cfg_hex)))
        layout.addView(etTopV); layout.addView(etMidV); layout.addView(etBotV)
        layout.addView(Button(this).apply {
            text = getString(R.string.cfg_aplicar)
            setOnClickListener {
                try {
                    Color.parseColor(etTopV.text.toString())
                    Color.parseColor(etMidV.text.toString())
                    Color.parseColor(etBotV.text.toString())
                    p.edit().putString(Prefs.C_TOP, etTopV.text.toString())
                        .putString(Prefs.C_MID, etMidV.text.toString())
                        .putString(Prefs.C_BOT, etBotV.text.toString()).apply()
                    Toast.makeText(this@MainActivity, getString(R.string.cfg_cores_aplicadas), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, getString(R.string.cfg_hex_invalido), Toast.LENGTH_SHORT).show()
                }
            }
        })

        layout.addView(Button(this).apply {
            text = getString(R.string.cfg_restaurar)
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
                text = getString(R.string.cfg_versao, pkg.versionName, code)
                tamanho(R.dimen.txt_versao)
                setTextColor(getColor(R.color.texto_quaternario))
                gravity = Gravity.CENTER
                setPadding(0, dp(16), 0, 0)
            })
        } catch (e: Exception) {
        }

        val scroll = ScrollView(this).apply { addView(layout) }
        setContentView(scroll)
    }

    /**
     * Troca de idioma por app (Android 13+ / One UI 5+). Usa a API do sistema
     * com o @xml/locales_config do manifesto — sem biblioteca extra. O sistema
     * avisa o wallpaper, que reassa os textos no idioma novo.
     */
    private fun seletorIdioma(): View {
        val lm = getSystemService(LocaleManager::class.java)
        val idiomas: List<Locale?> = listOf(null, Locale("en"), Locale("pt"), Locale("es"))
        val nomes = idiomas.map { loc ->
            if (loc == null) getString(R.string.cfg_idioma_sistema)
            else loc.getDisplayLanguage(loc).replaceFirstChar { c -> c.titlecase(loc) }
        }
        val atual = lm?.applicationLocales?.get(0)
        val idx = idiomas.indexOfFirst { it != null && atual != null && it.language == atual.language }

        return Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                nomes
            )
            setSelection(if (idx >= 0) idx else 0)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val escolhido = idiomas[position]
                    val atualAgora = lm?.applicationLocales?.get(0)
                    if (atualAgora?.language == escolhido?.language) return
                    lm?.setApplicationLocales(
                        if (escolhido == null) LocaleList.getEmptyLocaleList()
                        else LocaleList.forLanguageTags(escolhido.toLanguageTag())
                    )
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

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
