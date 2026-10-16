package com.giraydecide.wear

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var store: LocalStore
    private lateinit var preferences: AppPreferences
    private lateinit var column: LinearLayout
    private var pageScroll: ScrollView? = null
    private var inputReturnScroll = 0
    private var screen = "home"
    private var selectedId = ""
    private var draftId = ""
    private var editorReturnScreen = "home"
    private var draftName = ""
    private val draftOptions = mutableListOf<String>()
    private var lastResult = ""
    private var homeResult = ""
    private var homeHasSpun = false
    private var editingOption = -1
    private val requestName = 201
    private val requestOption = 202
    private val requestEdit = 203
    private val requestSavedEdit = 204
    private val defaultRoulette = Roulette(id = "default-numbers", name = "Números 1 al 6", options = (1..6).map { it.toString() })
    private var animator: ValueAnimator? = null
    private var optionList: LinearLayout? = null
    private val bg = Color.rgb(9, 13, 23)
    private val card = Color.rgb(26, 33, 48)
    private val accent = Color.rgb(184, 243, 107)
    private val muted = Color.rgb(161, 174, 195)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LocalStore(this)
        preferences = AppPreferences(this)
        restoreDraft()
        if (savedInstanceState != null) {
            screen = savedInstanceState.getString("screen", "home")
            inputReturnScroll = savedInstanceState.getInt("inputReturnScroll", 0)
            selectedId = savedInstanceState.getString("selected", "")
            draftId = savedInstanceState.getString("draftId", "")
            editorReturnScreen = savedInstanceState.getString("editorReturnScreen", "home")
            draftName = savedInstanceState.getString("draftName", "")
            draftOptions.clear()
            draftOptions.addAll(savedInstanceState.getStringArrayList("options") ?: arrayListOf())
            lastResult = savedInstanceState.getString("result", "")
            homeResult = savedInstanceState.getString("homeResult", "")
            homeHasSpun = savedInstanceState.getBoolean("homeHasSpun", false)
            editingOption = savedInstanceState.getInt("editingOption", -1)
        }
        render()
        if (savedInstanceState != null) restoreScroll(savedInstanceState.getInt("pageScroll", 0))
        if (store.readError) toast("No se pudieron leer algunos datos guardados.")
    }
    override fun onSaveInstanceState(outState: Bundle) {
        saveEditorDraft()
        outState.putString("screen", screen)
        outState.putInt("pageScroll", pageScroll?.scrollY ?: 0)
        outState.putInt("inputReturnScroll", inputReturnScroll)
        outState.putString("selected", selectedId)
        outState.putString("draftId", draftId)
        outState.putString("editorReturnScreen", editorReturnScreen)
        outState.putString("draftName", draftName)
        outState.putStringArrayList("options", ArrayList(draftOptions))
        outState.putString("result", lastResult)
        outState.putString("homeResult", homeResult)
        outState.putBoolean("homeHasSpun", homeHasSpun)
        outState.putInt("editingOption", editingOption)
        super.onSaveInstanceState(outState)
    }
    override fun onPause() { saveEditorDraft(); cancelSpin(); super.onPause() }
    override fun onDestroy() { cancelSpin(); super.onDestroy() }
    @Deprecated("Back navigation for this standalone Activity")
    override fun onBackPressed() {
        if (screen == "editor" && dirtyDraft()) {
            confirm("¿Salir sin guardar?", "Los cambios de esta ruleta se perderán.") { exitEditor() }
        } else if (screen == "editor") exitEditor()
        else if (screen != "home") showHome() else super.onBackPressed()
    }
    private fun exitEditor() {
        clearDraft(); screen = editorReturnScreen; render()
    }
    private fun dirtyDraft(): Boolean {
        val original = store.roulettes.find { it.id == draftId }
        return if (original == null) draftName.isNotBlank() || draftOptions.isNotEmpty()
            else original.name != draftName || original.options != draftOptions
    }
    private fun wheelSize() = dp((minOf(resources.configuration.screenWidthDp, resources.configuration.screenHeightDp) - 8).coerceAtLeast(120))
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Float = 22f) = GradientDrawable().apply {
        setColor(color); cornerRadius = radius * resources.displayMetrics.density
    }
    private fun base(eyebrow: String, title: String) {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg); isFillViewport = true; clipToPadding = false }
        column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false; clipToPadding = false
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(26), dp(36), dp(26), dp(36))
        }
        pageScroll = scroll
        scroll.addView(column)
        setContentView(scroll)
        if (title.isNotBlank() || eyebrow.isNotBlank()) {
            val heading = headerRow()
            val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            if (eyebrow.isNotBlank()) label(eyebrow.uppercase(Locale.ROOT), 10, accent, parent = titles)
            if (title.isNotBlank()) label(title, 23, Color.WHITE, true, titles)
            heading.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
            column.addView(heading, LinearLayout.LayoutParams(-1, -2))
            space(10)
        }
    }
    private fun headerRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        addView(iconButton("back", "Volver al menú anterior") { onBackPressed() }, LinearLayout.LayoutParams(dp(40), dp(48)))
    }
    private fun label(text: String, size: Int = 14, color: Int = muted, bold: Boolean = false, parent: LinearLayout = column): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, dp(3), 0, dp(3))
            parent.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
    }
    private fun space(height: Int) { column.addView(View(this), LinearLayout.LayoutParams(1, dp(height))) }
    private fun button(text: String, primary: Boolean = false, action: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 14f
            minHeight = dp(48)
            minimumHeight = dp(48)
            setTextColor(if (primary) bg else Color.WHITE)
            background = shape(if (primary) accent else card)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            column.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
            setOnClickListener { Haptics.tap(this); action() }
            help(this, buttonHelp(text))
        }
    }
    private fun help(view: View, message: String) {
        view.isHapticFeedbackEnabled = preferences.hapticsEnabled
        view.tooltipText = message
        view.setOnLongClickListener {
            val dialog = AlertDialog.Builder(this).setTitle("Ayuda").setMessage(message)
                .setPositiveButton("Entendido", null).show()
            val close = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            close.isHapticFeedbackEnabled = preferences.hapticsEnabled
            close.setOnClickListener { Haptics.tap(close); dialog.dismiss() }
            close.setOnLongClickListener { toast("Cerrar esta ayuda."); true }
            true
        }
    }
    private fun buttonHelp(text: String): String = when {
        text.startsWith("+ Crear") -> "Crear una ruleta con tus propias opciones."
        text.startsWith("Historial") -> "Consultar y borrar los últimos resultados."
        text == "Opciones" -> "Configurar la celebración, la vibración y los colores de las ruletas."
        text == "Guardar ruleta" -> "Guardar el nombre y las opciones de esta ruleta."
        text == "Girar ruleta" -> "Elegir una opción al azar con una animación."
        text == "Editar ruleta" -> "Cambiar el nombre o las opciones de esta ruleta."
        text == "Inicio" -> "Volver a la ruleta inicial y al menú."
        text == "Vaciar historial" -> "Borrar todos los resultados del historial."
        text == "Borrar resultado" -> "Borrar solamente este resultado."
        else -> "Abrir esta ruleta guardada."
    }
    private fun iconButton(icon: String, description: String, green: Boolean = false, action: () -> Unit): RoundIconButton {
        return RoundIconButton(this, icon, green).apply {
            contentDescription = description
            setOnClickListener { Haptics.tap(this); action() }
            help(this, description)
        }
    }
    private fun restoreScroll(y: Int) {
        val scroll = pageScroll ?: return
        scroll.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                scroll.viewTreeObserver.removeOnPreDrawListener(this)
                scroll.scrollTo(0, y.coerceAtLeast(0))
                return true
            }
        })
    }
    /** Use an owned editor so IME composition is included even without autocomplete. */
    private fun requestText(request: Int, title: String, current: String = "") {
        inputReturnScroll = pageScroll?.scrollY ?: 0
        saveEditorDraft()
        startActivityForResult(Intent(this, TextEntryActivity::class.java).apply {
            putExtra(TextEntryActivity.EXTRA_TITLE, title)
            putExtra(TextEntryActivity.EXTRA_TEXT, current)
            putExtra(TextEntryActivity.EXTRA_MAX_LENGTH, if (request == requestName) 32 else RouletteRules.MAX_LABEL)
        }, request)
    }
    @Deprecated("Text entry returns via the platform activity result")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode !in listOf(requestName, requestOption, requestEdit, requestSavedEdit) || resultCode != RESULT_OK || data == null) return
        val value = data.getStringExtra(TextEntryActivity.EXTRA_TEXT) ?: return
        acceptText(requestCode, value)
    }
    private fun acceptText(request: Int, raw: String): Boolean {
        if (request == requestSavedEdit) return updateSavedOption(raw)
        if (screen != "editor") return false
        val value = raw.trim()
        when (request) {
            requestName -> {
                if (value.isBlank() || value.length > 32) { toast("Usa un nombre de 1 a 32 caracteres."); return false }
                draftName = value
            }
            requestOption -> if (!addOption(value)) return false
            requestEdit -> {
                if (editingOption !in draftOptions.indices) return false
                val others = draftOptions.filterIndexed { i, _ -> i != editingOption }
                if (!RouletteRules.canAdd(others, value)) { toast("Escribe una opción distinta, de 1 a 48 caracteres."); return false }
                draftOptions[editingOption] = value
            }
            else -> return false
        }
        saveEditorDraft()
        editor()
        restoreScroll(inputReturnScroll)
        return true
    }
    private fun saveEditorDraft() {
        if (screen != "editor") return
        getSharedPreferences("editor_draft", MODE_PRIVATE).edit()
            .putString("id", draftId).putString("name", draftName)
            .putString("options", org.json.JSONArray(draftOptions).toString())
            .putInt("editingOption", editingOption).apply()
    }
    private fun restoreDraft() {
        val prefs = getSharedPreferences("editor_draft", MODE_PRIVATE)
        draftId = prefs.getString("id", "").orEmpty()
        draftName = prefs.getString("name", "").orEmpty()
        editingOption = prefs.getInt("editingOption", -1)
        draftOptions.clear()
        runCatching {
            val values = org.json.JSONArray(prefs.getString("options", "[]"))
            for (i in 0 until minOf(values.length(), RouletteRules.MAX_OPTIONS)) draftOptions.add(values.getString(i))
        }
    }
    private fun clearDraft() {
        draftId = ""; draftName = ""; draftOptions.clear(); editingOption = -1
        getSharedPreferences("editor_draft", MODE_PRIVATE).edit().clear().apply()
    }
    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    private fun persist() { if (!store.save()) toast("No se pudo guardar. Revisa el espacio del reloj.") }
    private fun confirm(title: String, message: String, action: () -> Unit) {
        val dialog = AlertDialog.Builder(this).setTitle(title).setMessage(message)
            .setNegativeButton("Cancelar", null).setPositiveButton("Aceptar") { _, _ -> Haptics.tap(window.decorView); action() }.show()
        val cancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        cancel.setOnClickListener { Haptics.tap(cancel); dialog.dismiss() }
        help(cancel, "Cancelar esta acción.")
        help(dialog.getButton(AlertDialog.BUTTON_POSITIVE), "Confirmar esta acción.")
    }
    private fun render() {
        when (screen) { "editor" -> editor(); "spin" -> spinScreen(); "history" -> history(); "settings" -> settings(); else -> home() }
    }
    private fun showHome() { cancelSpin(); screen = "home"; render() }
    private fun home() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg); isFillViewport = true }
        column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        pageScroll = scroll
        scroll.addView(column)
        setContentView(scroll)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            clipChildren = false; clipToPadding = false
            minimumHeight = dp(resources.configuration.screenHeightDp)
            setPadding(0, dp(4), 0, dp(4))
        }
        column.addView(panel, LinearLayout.LayoutParams(-1, -2))
        val wheelSize = wheelSize()
        val wheel = WheelView(this, defaultRoulette.options)
        if (homeResult in defaultRoulette.options) wheel.angle = RouletteRules.targetRotation(defaultRoulette.options.indexOf(homeResult), 6)
        wheel.contentDescription = "Girar ruleta del 1 al 6"
        wheel.isClickable = true; wheel.isFocusable = true
        val wheelPanel = FrameLayout(this)
        wheelPanel.addView(wheel, FrameLayout.LayoutParams(-1, -1))
        panel.addView(wheelPanel, LinearLayout.LayoutParams(wheelSize, wheelSize))
        val result = label(if (homeResult.isBlank()) "Toca GIRA para girar" else "Resultado: $homeResult", 14, Color.WHITE, parent = panel)
        val hint = TextView(this).apply {
            text = "⌃ Desliza para ver opciones"; textSize = 10f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            alpha = .45f
            visibility = if (homeHasSpun) View.GONE else View.VISIBLE
        }
        wheelPanel.addView(hint, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(15) })
        bindSpin(defaultRoulette, wheel, result, wheel) { homeHasSpun = true; hint.visibility = View.GONE }
        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(26), dp(16), dp(26), dp(36))
        }
        column.addView(menu, LinearLayout.LayoutParams(-1, -2))
        column = menu
        val shortcuts = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val create = button("+ Crear ruleta", true) { cancelSpin(); newDraft() }
        val settings = button("Opciones") { cancelSpin(); screen = "settings"; settings() }
        column.removeView(create); column.removeView(settings)
        create.textSize = 12f; settings.textSize = 12f
        shortcuts.addView(create, LinearLayout.LayoutParams(0, dp(48), 1.15f).apply { marginEnd = dp(6) })
        shortcuts.addView(settings, LinearLayout.LayoutParams(0, dp(48), 1f))
        column.addView(shortcuts, LinearLayout.LayoutParams(-1, -2))
        space(16)
        label("MIS RULETAS", 10, accent)
        if (store.roulettes.isEmpty()) label("Crea una ruleta con tus propias opciones.")
        val dateFormat = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale("es", "MX"))
        store.roulettes.sortedByDescending { it.createdAt }.forEach { r ->
            val date = if (r.createdAt > 0L) dateFormat.format(Date(r.createdAt)) else "Fecha no disponible"
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                background = shape(card, 18f); setPadding(dp(10), dp(8), dp(2), dp(8))
                setOnClickListener { Haptics.tap(this); cancelSpin(); selectedId = r.id; lastResult = ""; screen = "spin"; spinScreen() }
                contentDescription = "Abrir ${r.name}"
                help(this, "Abrir la ruleta ${r.name}.")
            }
            val description = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            label(r.name, 16, Color.WHITE, true, description).gravity = Gravity.START
            label(date, 10, muted, parent = description).gravity = Gravity.START
            row.addView(description, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(iconButton("trash", "Eliminar ruleta ${r.name}") {
                confirm("¿Eliminar ${r.name}?", "La ruleta se eliminará. Sus resultados seguirán en el historial.") {
                    val y = pageScroll?.scrollY ?: 0
                    store.roulettes.removeAll { it.id == r.id }; persist(); home(); restoreScroll(y)
                }
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
            column.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
        }
        space(12)
        button("Historial · ${store.history.size}") { cancelSpin(); screen = "history"; history() }
    }
    private fun newDraft(roulette: Roulette? = null) {
        editorReturnScreen = if (roulette != null) "spin" else "home"
        cancelSpin()
        if (roulette != null) {
            draftId = roulette.id; draftName = roulette.name
            draftOptions.clear(); draftOptions.addAll(roulette.options)
            editingOption = -1
        } else {
            restoreDraft()
            // A draft for an existing roulette must not replace a new roulette.
            if (draftId.isNotBlank()) clearDraft()
        }
        screen = "editor"; saveEditorDraft(); editor()
    }
    private fun editor() {
        base("", "")
        val heading = headerRow()
        val name = TextView(this).apply {
            text = draftName.ifBlank { "Nombre" }
            textSize = 24f; setTextColor(accent)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            contentDescription = "Editar nombre de la ruleta"
            setOnClickListener { Haptics.tap(this); requestText(requestName, "Nombre de la ruleta", draftName) }
            help(this, "Toca el nombre para cambiarlo.")
        }
        heading.addView(name, LinearLayout.LayoutParams(0, -2, 1f))
        heading.addView(iconButton("edit", "Editar el nombre de la ruleta") {
            requestText(requestName, "Nombre de la ruleta", draftName)
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        column.addView(heading, LinearLayout.LayoutParams(-1, -2))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; clipChildren = false; clipToPadding = false }
        actions.addView(iconButton("save", "Guardar ruleta") { saveDraft() }, LinearLayout.LayoutParams(dp(48), dp(48)))
        actions.addView(iconButton("add", "Agregar una opción escrita", true) {
            requestText(requestOption, "Nueva opción")
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        column.addView(actions, LinearLayout.LayoutParams(dp(96), -2))
        optionList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(optionList, LinearLayout.LayoutParams(-1, -2))
        refreshOptions()
    }
    private fun addOption(raw: String): Boolean {
        val value = raw.trim()
        if (!RouletteRules.canAdd(draftOptions, value)) {
            val message = when {
                value.isBlank() -> "Escribe una opción."
                value.length > RouletteRules.MAX_LABEL -> "Máximo 48 caracteres por opción."
                draftOptions.size >= RouletteRules.MAX_OPTIONS -> "Puedes agregar hasta 16 opciones."
                else -> "Esa opción ya existe."
            }
            toast(message)
            return false
        }
        draftOptions.add(value)
        saveEditorDraft()
        return true
    }
    private fun refreshOptions() {
        val list = optionList ?: return
        renderOptions(list, draftOptions.toList(), { index -> editOption(index) }, { index ->
            draftOptions.removeAt(index); saveEditorDraft(); refreshOptions()
        })
    }
    private fun renderOptions(list: LinearLayout, options: List<String>, onEdit: (Int) -> Unit, onDelete: (Int) -> Unit) {
        list.removeAllViews()
        label("OPCIONES · ${options.size}/16", 10, accent, parent = list)
        options.forEachIndexed { index, option ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                background = shape(card, 16f); setPadding(dp(4), dp(4), dp(2), dp(4))
            }
            val name = TextView(this).apply {
                text = option; textSize = 16f; setTextColor(Color.WHITE)
                setPadding(dp(4), dp(6), dp(4), dp(6))
                contentDescription = "Editar opción $option"
                setOnClickListener { Haptics.tap(this); onEdit(index) }
                help(this, "Editar esta opción: $option")
            }
            item.addView(name, LinearLayout.LayoutParams(0, -2, 1f))
            val controls = LinearLayout(this).apply { gravity = Gravity.END }
            controls.addView(iconButton("edit", "Editar opción $option") { onEdit(index) }.apply { alpha = .55f }, LinearLayout.LayoutParams(dp(40), dp(48)))
            controls.addView(iconButton("delete", "Eliminar opción $option") { onDelete(index) }.apply { alpha = .55f }, LinearLayout.LayoutParams(dp(40), dp(48)))
            item.addView(controls, LinearLayout.LayoutParams(-2, -2))
            list.addView(item, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        }
    }
    private fun editSavedOption(index: Int) {
        val roulette = store.roulettes.find { it.id == selectedId } ?: return
        if (index !in roulette.options.indices) return
        cancelSpin(); editingOption = index
        requestText(requestSavedEdit, "Editar opción", roulette.options[index])
    }
    private fun updateSavedOption(raw: String): Boolean {
        if (screen != "spin") return false
        val index = store.roulettes.indexOfFirst { it.id == selectedId }
        if (index < 0) return false
        val roulette = store.roulettes[index]
        if (editingOption !in roulette.options.indices) return false
        val value = raw.trim()
        if (!RouletteRules.canAdd(roulette.options.filterIndexed { i, _ -> i != editingOption }, value)) {
            toast("Escribe una opción distinta, de 1 a 48 caracteres."); return false
        }
        val options = roulette.options.toMutableList()
        if (lastResult == options[editingOption]) lastResult = ""
        options[editingOption] = value
        store.roulettes[index] = roulette.copy(options = options.toList())
        persist(); spinScreen()
        restoreScroll(inputReturnScroll)
        return true
    }
    private fun deleteSavedOption(index: Int) {
        val position = store.roulettes.indexOfFirst { it.id == selectedId }
        if (position < 0) return
        val roulette = store.roulettes[position]
        if (roulette.options.size <= 2) { toast("La ruleta necesita al menos dos opciones."); return }
        if (index !in roulette.options.indices) return
        cancelSpin()
        val options = roulette.options.toMutableList()
        if (lastResult == options[index]) lastResult = ""
        options.removeAt(index)
        store.roulettes[position] = roulette.copy(options = options.toList())
        persist(); spinScreen()
    }
    private fun editOption(index: Int) {
        editingOption = index
        requestText(requestEdit, "Editar opción", draftOptions[index])
    }
    private fun saveDraft() {
        if (draftName.isBlank()) { toast("Ponle un nombre a tu ruleta."); return }
        if (draftOptions.size < 2) { toast("Agrega al menos dos opciones."); return }
        val name = draftName.trim()
        if (store.roulettes.any { it.id != draftId && RouletteRules.normalized(it.name) == RouletteRules.normalized(name) }) {
            toast("Ya tienes una ruleta con ese nombre."); return
        }
        val original = store.roulettes.find { it.id == draftId }
        val r = original?.copy(name = name, options = draftOptions.toList())
            ?: Roulette(name = name, options = draftOptions.toList())
        val oldIndex = store.roulettes.indexOfFirst { it.id == r.id }
        if (oldIndex >= 0) store.roulettes[oldIndex] = r else store.roulettes.add(0, r)
        persist(); clearDraft(); selectedId = r.id; lastResult = ""; screen = "spin"; spinScreen()
    }
    private fun spinScreen() {
        val r = store.roulettes.find { it.id == selectedId } ?: return showHome()
        base("Que decida el azar", r.name)
        val wheel = WheelView(this, r.options)
        wheel.contentDescription = "Ruleta con ${r.options.size} opciones"
        column.addView(wheel, LinearLayout.LayoutParams(wheelSize(), wheelSize()))
        val resultLabel = label(if (lastResult.isBlank()) "¿Qué tocará hoy?" else lastResult, 19, accent, true)
        if (lastResult in r.options) wheel.angle = RouletteRules.targetRotation(r.options.indexOf(lastResult), r.options.size)
        bindSpin(r, wheel, resultLabel, wheel)
        space(8)
        val navigation = LinearLayout(this).apply { gravity = Gravity.CENTER }
        navigation.addView(iconButton("editDocument", "Editar ruleta") { cancelSpin(); newDraft(r) }, LinearLayout.LayoutParams(dp(48), dp(48)))
        navigation.addView(iconButton("home", "Inicio") { showHome() }, LinearLayout.LayoutParams(dp(48), dp(48)))
        column.addView(navigation, LinearLayout.LayoutParams(-1, -2))
        space(16)
        val savedOptions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(savedOptions, LinearLayout.LayoutParams(-1, -2))
        renderOptions(savedOptions, r.options, { editSavedOption(it) }, { deleteSavedOption(it) })
    }
    private fun bindSpin(r: Roulette, wheel: WheelView, resultLabel: TextView, trigger: View, onStart: () -> Unit = {}) {
        help(wheel, "Toca GIRA para elegir una opción al azar.")
        trigger.setOnClickListener {
            if (animator != null) return@setOnClickListener
            val index = RouletteRules.pick(r.options.size)
            val start = wheel.angle % 360f
            val target = RouletteRules.targetRotation(index, r.options.size)
            val delta = (target - start + 360f) % 360f
            Haptics.tap(wheel); onStart(); resultLabel.text = "Decidiendo…"
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            var cancelled = false
            animator = ValueAnimator.ofFloat(start, start + 360f * 5 + delta).apply {
                duration = 3200
                interpolator = DecelerateInterpolator(2f)
                addUpdateListener { wheel.angle = it.animatedValue as Float }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationCancel(animation: Animator) { cancelled = true }
                    override fun onAnimationEnd(animation: Animator) {
                        animator = null
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        if (!cancelled) {
                            wheel.angle = target
                            val selected = r.options[index]
                            if (r.id == defaultRoulette.id) homeResult = selected else lastResult = selected
                            resultLabel.text = if (r.id == defaultRoulette.id) "Resultado: $selected" else selected
                            resultLabel.announceForAccessibility("Resultado: ${r.options[index]}")
                            store.record(r, r.options[index]); persist()
                            Haptics.confirm(wheel)
                            wheel.celebrate()
                        } else resultLabel.text = "Toca para volver a girar"
                    }
                })
                start()
            }
        }
        if (trigger !== wheel) {
            wheel.isClickable = true
            wheel.isFocusable = true
            wheel.contentDescription = "Girar ${r.name}: toca GIRA"
            wheel.setOnClickListener { trigger.performClick() }
        }
    }
    private fun cancelSpin() {
        animator?.cancel(); animator = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    private fun settings() {
        base("", "Opciones")
        val celebration = Switch(this).apply {
            text = "Celebración"
            textSize = 15f; setTextColor(Color.WHITE)
            minHeight = dp(48)
            isChecked = preferences.celebrationEnabled
            contentDescription = "Celebración al salir un ganador"
            setOnCheckedChangeListener { _, checked -> preferences.celebrationEnabled = checked; Haptics.tap(this) }
            help(this, "Activar o desactivar el confeti al terminar un giro.")
        }
        column.addView(celebration, LinearLayout.LayoutParams(-1, -2))
        val vibration = Switch(this).apply {
            text = "Vibración"; textSize = 15f; setTextColor(Color.WHITE); minHeight = dp(48)
            isChecked = preferences.hapticsEnabled
            help(this, "Vibrar al tocar botones y al iniciar y terminar un giro, si el reloj lo permite.")
            setOnCheckedChangeListener { _, checked ->
                preferences.hapticsEnabled = checked
                Haptics.updateTree(window.decorView)
                Haptics.tap(this)
            }
        }
        column.addView(vibration, LinearLayout.LayoutParams(-1, -2))
        space(12)
        label("COLORES DE LA RULETA", 10, accent)
        val mode = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        val multiple = RadioButton(this).apply {
            id = 6101; text = "Distintos colores"; setTextColor(Color.WHITE); textSize = 14f; minHeight = dp(48)
            help(this, "Usar un color distinto para cada sector.")
        }
        val single = RadioButton(this).apply {
            id = 6102; text = "Un solo color"; setTextColor(Color.WHITE); textSize = 14f; minHeight = dp(48)
            help(this, "Usar el mismo color en todos los sectores y elegirlo abajo.")
        }
        mode.addView(multiple); mode.addView(single)
        mode.check(if (preferences.multicolor) 6101 else 6102)
        column.addView(mode, LinearLayout.LayoutParams(-1, -2))
        val colors = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(colors, LinearLayout.LayoutParams(-1, -2))
        fun showColors() {
            colors.removeAllViews()
            if (preferences.multicolor) return
            label("ELIGE UN COLOR", 10, accent, parent = colors)
            val palette = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
            AppPreferences.palette.forEachIndexed { index, (name, color) ->
                val choice = RadioButton(this).apply {
                    id = 6200 + index; text = name; textSize = 15f; minHeight = dp(48)
                    setTextColor(color)
                    buttonTintList = android.content.res.ColorStateList.valueOf(color)
                    help(this, "Pintar todos los sectores de color ${name.lowercase(Locale.ROOT)}.")
                }
                palette.addView(choice)
                if (color == preferences.singleColor) palette.check(choice.id)
            }
            palette.setOnCheckedChangeListener { group, id ->
                Haptics.tap(group)
                AppPreferences.palette.getOrNull(id - 6200)?.let { preferences.singleColor = it.second }
            }
            colors.addView(palette, LinearLayout.LayoutParams(-1, -2))
        }
        showColors()
        mode.setOnCheckedChangeListener { group, id -> Haptics.tap(group); preferences.multicolor = id == 6101; showColors() }
    }
    private fun history() {
        base("Tus últimas decisiones", "Historial")
        label("Se guardan los últimos 20 giros.", 12)
        if (store.history.isNotEmpty()) button("Vaciar historial") {
            confirm("¿Vaciar historial?", "Se borrarán todos los resultados. Tus ruletas se conservarán.") {
                store.history.clear(); persist(); history()
            }
        }
        if (store.history.isEmpty()) label("Aún no hay resultados.\n¡Dale una vuelta a tu primera ruleta!")
        val format = SimpleDateFormat("dd MMM · HH:mm", Locale("es", "MX"))
        store.history.toList().forEach { record ->
            space(8)
            label(record.name, 12)
            label(record.result, 18, accent, true)
            label(format.format(Date(record.time)), 11)
            button("Borrar resultado") {
                confirm("¿Borrar resultado?", "${record.name}: ${record.result}") {
                    store.history.removeAll { it.id == record.id }; persist(); history()
                }
            }
        }
        column.addView(iconButton("home", "Inicio") { showHome() }, LinearLayout.LayoutParams(dp(48), dp(48)))
    }
}
