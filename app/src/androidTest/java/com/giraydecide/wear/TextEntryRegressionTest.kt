package com.giraydecide.wear

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Run on a Wear OS emulator: covers the returned ✓ result and Activity recreation. */
@RunWith(AndroidJUnit4::class)
class TextEntryRegressionTest {
    private lateinit var context: Context
    private var draftBackup: Map<String, *> = emptyMap<String, Any>()

    @Before fun preserveDraft() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("editor_draft", Context.MODE_PRIVATE)
        draftBackup = prefs.all.toMap()
        prefs.edit().clear().commit()
    }
    @After fun restoreDraft() {
        val edit = context.getSharedPreferences("editor_draft", Context.MODE_PRIVATE).edit().clear()
        draftBackup.forEach { (key, value) ->
            when (value) { is String -> edit.putString(key, value); is Int -> edit.putInt(key, value) }
        }
        edit.commit()
    }
    private fun views(root: View): List<View> = listOf(root) +
        if (root is ViewGroup) (0 until root.childCount).flatMap { views(root.getChildAt(it)) } else emptyList()
    private fun text(activity: MainActivity, value: String): TextView =
        views(activity.window.decorView).filterIsInstance<TextView>().first { it.text.toString() == value }
    private fun returnCheckedText(activity: MainActivity, request: Int, value: String) {
        val data = Intent().putExtra(TextEntryActivity.EXTRA_TEXT, value)

        activity.onActivityResult(request, Activity.RESULT_OK, data)
    }
    @Test fun checkedNameAndOptionsSurviveRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                text(activity, "+ Crear ruleta").performClick()
                returnCheckedText(activity, 201, "Comida de prueba")
                assertEquals("Comida de prueba", text(activity, "Comida de prueba").text.toString())
                returnCheckedText(activity, 202, "Pizza de queso")
                assertNotNull(text(activity, "Pizza de queso"))
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                assertNotNull(text(activity, "Comida de prueba"))
                assertNotNull(text(activity, "Pizza de queso"))
                returnCheckedText(activity, 202, "Sopa")
                assertNotNull(text(activity, "Sopa"))
            }
        }
    }
    @Test fun cancelledKeyboardKeepsPreviousName() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                text(activity, "+ Crear ruleta").performClick()
                returnCheckedText(activity, 201, "Mi ruleta")
                activity.onActivityResult(201, Activity.RESULT_CANCELED, null)
                assertNotNull(text(activity, "Mi ruleta"))
            }
        }
    }
    @Test fun homeHasUsableWheelAndTranslucentSwipeHint() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val wheel = views(activity.window.decorView).filterIsInstance<WheelView>().first()
                assertTrue(wheel.isClickable)
                assertEquals("Girar ruleta del 1 al 6", wheel.contentDescription)
                assertEquals(.45f, text(activity, "⌃ Desliza para ver opciones").alpha, .01f)
            }
        }
    }
    private fun confirmUncommittedWord(mode: String) {
        val intent = Intent(context, TextEntryActivity::class.java)
            .putExtra(TextEntryActivity.EXTRA_TITLE, "Nueva opción")
        ActivityScenario.launchActivityForResult<TextEntryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val edit = activity.findViewById<ComposingEditText>(android.R.id.edit)
                val connection = edit.onCreateInputConnection(EditorInfo())!!
                // No suggestion is selected: the entire word is still under composition.
                connection.setComposingText("pizza", 1)
                assertTrue(BaseInputConnection.getComposingSpanStart(edit.text) >= 0)
                when (mode) {
                    "ime" -> connection.performEditorAction(EditorInfo.IME_ACTION_DONE)
                    "enter" -> {
                        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                    }
                    else -> views(activity.window.decorView).filterIsInstance<TextView>()
                        .first { it.text.toString() == "Ingresar" }.performClick()
                }
            }
            assertEquals(Activity.RESULT_OK, scenario.result.resultCode)
            assertEquals("pizza", scenario.result.resultData.getStringExtra(TextEntryActivity.EXTRA_TEXT))
        }
    }
    @Test fun imeDoneAcceptsPizzaWithoutAutocomplete() = confirmUncommittedWord("ime")
    @Test fun enterAcceptsPizzaWithoutAutocomplete() = confirmUncommittedWord("enter")
    @Test fun inputButtonAcceptsPizzaWithoutAutocomplete() = confirmUncommittedWord("button")
    @Test fun editingNikeToUppercaseReplacesRatherThanDuplicates() {
        val intent = Intent(context, TextEntryActivity::class.java)
            .putExtra(TextEntryActivity.EXTRA_TITLE, "Editar opción")
            .putExtra(TextEntryActivity.EXTRA_TEXT, "nike")
        ActivityScenario.launchActivityForResult<TextEntryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val edit = activity.findViewById<ComposingEditText>(android.R.id.edit)
                assertEquals(0, edit.selectionStart)
                assertEquals(4, edit.selectionEnd)
                val connection = edit.onCreateInputConnection(EditorInfo())!!
                connection.setComposingText("NI", 1)
                connection.setComposingText("NIKE", 1)
                connection.performEditorAction(EditorInfo.IME_ACTION_DONE)
                // A late IME commit after submission must not append another copy.
                connection.commitText("NIKE", 1)
                assertEquals("NIKE", edit.text.toString())
            }
            assertEquals("NIKE", scenario.result.resultData.getStringExtra(TextEntryActivity.EXTRA_TEXT))
        }
    }
    @Test fun mixedHardwareTypingAndSuggestionsPreservePanConQueso() {
        val intent = Intent(context, TextEntryActivity::class.java)
        ActivityScenario.launchActivityForResult<TextEntryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val edit = activity.findViewById<ComposingEditText>(android.R.id.edit)
                val connection = edit.onCreateInputConnection(EditorInfo())!!
                connection.setComposingText("Pan", 1)
                connection.commitText("Pan ", 1)
                val keys = android.view.KeyCharacterMap.load(android.view.KeyCharacterMap.VIRTUAL_KEYBOARD)
                    .getEvents("con ".toCharArray())!!
                keys.forEach { edit.onKeyPreIme(it.keyCode, it) }
                connection.setComposingText("queso", 1)
                connection.commitText("queso", 1)
                connection.performEditorAction(EditorInfo.IME_ACTION_DONE)
            }
            assertEquals("Pan con queso", scenario.result.resultData.getStringExtra(TextEntryActivity.EXTRA_TEXT))
        }
    }
}