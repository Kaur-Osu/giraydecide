package com.giraydecide.wear

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Owns the Editable, including composing text that has not been chosen as a suggestion. */
class TextEntryActivity : Activity() {
    companion object {
        const val EXTRA_TEXT = "roulette_text"
        const val EXTRA_TITLE = "text_title"
        const val EXTRA_MAX_LENGTH = "text_max_length"
    }
    private lateinit var input: ComposingEditText
    private var submitted = false
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val max = intent.getIntExtra(EXTRA_MAX_LENGTH, 48)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(28), dp(24), dp(28))
            setBackgroundColor(Color.rgb(9, 13, 23))
        }
        content.addView(TextView(this).apply {
            text = intent.getStringExtra(EXTRA_TITLE) ?: "Escribir texto"
            textSize = 16f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(-1, -2))
        input = ComposingEditText(this).apply {
            id = android.R.id.edit
            isSaveEnabled = false // Save only plain text below; never restore stale IME spans.
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
            imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
            filters = arrayOf(InputFilter.LengthFilter(max))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            hint = "Escribe aquí"
            textSize = 17f
            setText(savedInstanceState?.getString(EXTRA_TEXT) ?: intent.getStringExtra(EXTRA_TEXT).orEmpty())
            setSelectAllOnFocus(false)
            if (savedInstanceState == null) selectAll() else {
                val start = savedInstanceState.getInt("selection_start", text.length).coerceIn(0, text.length)
                val end = savedInstanceState.getInt("selection_end", start).coerceIn(0, text.length)
                setSelection(minOf(start, end), maxOf(start, end))
            }
            onConfirm = { submitInput() }
            setOnEditorActionListener { _, action, event ->
                if (action == EditorInfo.IME_ACTION_DONE || action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_SEND ||
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP)) {
                    submitInput(); true
                } else false
            }
        }
        content.addView(input, LinearLayout.LayoutParams(-1, dp(56)))
        content.addView(Button(this).apply {
            text = "Ingresar"; isAllCaps = false; minHeight = dp(48)
            isHapticFeedbackEnabled = AppPreferences(this@TextEntryActivity).hapticsEnabled
            setOnClickListener { Haptics.tap(this); submitInput() }
            tooltipText = "Aceptar el texto escrito y volver a la ruleta."
            setOnLongClickListener {
                android.app.AlertDialog.Builder(this@TextEntryActivity).setTitle("Ayuda")
                    .setMessage("Aceptar el texto escrito y volver a la ruleta, sin seleccionar una sugerencia.")
                    .setPositiveButton("Entendido", null).show()
                true
            }
        }, LinearLayout.LayoutParams(-1, -2))
        setContentView(ScrollView(this).apply { isFillViewport = true; addView(content) })
        input.requestFocus()
        if (savedInstanceState == null) input.post {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    private fun submitInput() {
        input.afterPendingInput { confirmText() }
    }
    private fun confirmText() {
        if (submitted) return
        // Snapshot the current Editable before ending the IME session. Finalization
        // must not trigger another commit of a word already present in the field.
        val value = input.text.toString().trim()
        if (value.isBlank()) { input.error = "Escribe un texto"; return }
        submitted = true
        setResult(RESULT_OK, Intent().putExtra(EXTRA_TEXT, value))
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(input.windowToken, 0)
        finish()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(EXTRA_TEXT, input.text.toString())
        outState.putInt("selection_start", input.selectionStart)
        outState.putInt("selection_end", input.selectionEnd)
        super.onSaveInstanceState(outState)
    }
}

/** Preserve Android's standard editing semantics; defer submission until the batch ends. */
class ComposingEditText(context: Context) : EditText(context) {
    var onConfirm: (() -> Unit)? = null
    private var batchDepth = 0
    private var extractedToken: Int? = null
    private var keyboardRefreshPosted = false
    private var editableConnection: InputConnection? = null
    private var pendingSubmit: (() -> Unit)? = null
    private var submitPosted = false
    fun afterPendingInput(action: () -> Unit) {
        pendingSubmit = action
        scheduleSubmit()
    }
    private fun scheduleSubmit() {
        if (batchDepth != 0 || pendingSubmit == null || submitPosted) return
        submitPosted = true
        postOnAnimation {
            submitPosted = false
            if (batchDepth == 0) {
                val action = pendingSubmit
                pendingSubmit = null
                action?.invoke()
            }
        }
    }
    private fun synchronizeKeyboardPreview() {
        val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val start = android.view.inputmethod.BaseInputConnection.getComposingSpanStart(text)
        val end = android.view.inputmethod.BaseInputConnection.getComposingSpanEnd(text)
        manager.updateSelection(this, selectionStart, selectionEnd, start, end)
        if (!keyboardRefreshPosted) {
            keyboardRefreshPosted = true
            postOnAnimation {
                keyboardRefreshPosted = false
                manager.restartInput(this)
            }
        }
        extractedToken?.let { token ->
            val extracted = android.view.inputmethod.ExtractedText().apply {
                text = this@ComposingEditText.text.toString()
                startOffset = 0; partialStartOffset = -1; partialEndOffset = -1
                selectionStart = this@ComposingEditText.selectionStart
                selectionEnd = this@ComposingEditText.selectionEnd
                flags = android.view.inputmethod.ExtractedText.FLAG_SINGLE_LINE
            }
            manager.updateExtractedText(this, token, extracted)
        }
    }
    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        // Hardware input must update this Editable, not only Wear Gboard's extracted preview.
        val hardware = event.flags and KeyEvent.FLAG_SOFT_KEYBOARD == 0
        val editingKey = event.unicodeChar != 0 || keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_FORWARD_DEL || keyCode == KeyEvent.KEYCODE_ENTER
        if (hardware && editingKey && !event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) onConfirm?.invoke()
                else {
                    beginBatchEdit()
                    try {
                        android.view.inputmethod.BaseInputConnection.removeComposingSpans(editableText)
                        super.onKeyDown(keyCode, event)
                        val cursor = selectionEnd
                        var start = cursor
                        while (start > 0 && Character.isLetterOrDigit(text[start - 1])) start--
                        if (start < cursor && selectionStart == cursor) editableConnection?.setComposingRegion(start, cursor)
                    } finally { endBatchEdit() }
                    synchronizeKeyboardPreview()
                }
            }
            return true
        }
        return super.onKeyPreIme(keyCode, event)
    }
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val connection = super.onCreateInputConnection(outAttrs) ?: return null
        editableConnection = connection
        return object : InputConnectionWrapper(connection, false) {
            override fun getExtractedText(request: android.view.inputmethod.ExtractedTextRequest, flags: Int): android.view.inputmethod.ExtractedText? {
                extractedToken = request.token
                return super.getExtractedText(request, flags)
            }
            override fun beginBatchEdit(): Boolean {
                val result = super.beginBatchEdit()
                if (result) batchDepth++
                return result
            }
            override fun endBatchEdit(): Boolean {
                val result = super.endBatchEdit()
                batchDepth = (batchDepth - 1).coerceAtLeast(0)
                scheduleSubmit()
                return result
            }
            override fun performEditorAction(editorAction: Int): Boolean {
                if (editorAction == EditorInfo.IME_ACTION_DONE || editorAction == EditorInfo.IME_ACTION_GO ||
                    editorAction == EditorInfo.IME_ACTION_SEND || editorAction == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    onConfirm?.invoke()
                    return true
                }
                return super.performEditorAction(editorAction)
            }
            override fun sendKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (event.action == KeyEvent.ACTION_UP) onConfirm?.invoke()
                    return true
                }
                return super.sendKeyEvent(event)
            }
        }
    }
}