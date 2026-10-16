package com.giraydecide.wear

import android.content.Context
import android.graphics.Color

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("appearance_preferences", Context.MODE_PRIVATE)
    var hapticsEnabled: Boolean
        get() = prefs.getBoolean("haptics", true)
        set(value) { prefs.edit().putBoolean("haptics", value).apply() }
    var celebrationEnabled: Boolean
        get() = prefs.getBoolean("celebration", true)
        set(value) { prefs.edit().putBoolean("celebration", value).apply() }
    var multicolor: Boolean
        get() = prefs.getBoolean("multicolor", true)
        set(value) { prefs.edit().putBoolean("multicolor", value).apply() }
    var singleColor: Int
        get() = prefs.getInt("singleColor", Color.rgb(184,243,107))
        set(value) { prefs.edit().putInt("singleColor", value).apply() }
    companion object {
        val palette = listOf(
            "Verde" to Color.rgb(184,243,107), "Morado" to Color.rgb(184,161,255),
            "Azul" to Color.rgb(105,209,245), "Naranja" to Color.rgb(255,188,114),
            "Rosa" to Color.rgb(251,145,177), "Turquesa" to Color.rgb(90,219,185)
        )
    }
}