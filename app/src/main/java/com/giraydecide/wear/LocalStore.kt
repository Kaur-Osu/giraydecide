package com.giraydecide.wear

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Small, private local store. Removing a roulette preserves past result snapshots. */
class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("gira_y_decide_v1", Context.MODE_PRIVATE)
    val roulettes = mutableListOf<Roulette>()
    val history = mutableListOf<SpinRecord>()
    var readError = false
        private set
    init {
        try {
            val array = JSONArray(prefs.getString("roulettes", "[]"))
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val options = item.getJSONArray("options")
                roulettes.add(Roulette(item.getString("id"), item.getString("name"),
                    List(options.length()) { options.getString(it) }, item.optLong("createdAt", 0L)))
            }
            val records = JSONArray(prefs.getString("history", "[]"))
            for (i in 0 until minOf(records.length(), 20)) {
                val item = records.getJSONObject(i)
                history.add(SpinRecord(item.getString("id"), item.getString("name"), item.getString("result"), item.getLong("time")))
            }
        } catch (_: Exception) { readError = true }
    }
    fun save(): Boolean {
        val a = JSONArray()
        roulettes.forEach { r -> a.put(JSONObject().put("id", r.id).put("name", r.name).put("options", JSONArray(r.options)).put("createdAt", r.createdAt)) }
        val h = JSONArray()
        history.take(20).forEach { r -> h.put(JSONObject().put("id", r.id).put("name", r.name).put("result", r.result).put("time", r.time)) }
        return prefs.edit().putString("roulettes", a.toString()).putString("history", h.toString()).commit()
    }
    fun record(roulette: Roulette, result: String) {
        history.add(0, SpinRecord(name = roulette.name, result = result))
        while (history.size > 20) history.removeAt(history.lastIndex)
    }
}