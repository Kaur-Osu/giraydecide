package com.giraydecide.wear

import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup

/** Semantic feedback respects both the app preference and the system setting. */
object Haptics {
    fun tap(view: View) = feedback(view, HapticFeedbackConstants.VIRTUAL_KEY)
    fun confirm(view: View) = feedback(view, HapticFeedbackConstants.CONFIRM)
    private fun feedback(view: View, effect: Int) {
        if (AppPreferences(view.context).hapticsEnabled) view.performHapticFeedback(effect)
    }
    fun updateTree(view: View) {
        view.isHapticFeedbackEnabled = AppPreferences(view.context).hapticsEnabled
        if (view is ViewGroup) for (i in 0 until view.childCount) updateTree(view.getChildAt(i))
    }
}