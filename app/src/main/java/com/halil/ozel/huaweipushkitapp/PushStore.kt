package com.halil.ozel.huaweipushkitapp

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class PushStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun token(): String = prefs.getString(KEY_TOKEN, "").orEmpty()

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun addMessage(kind: String, channelId: String, title: String, body: String) {
        val existing = JSONArray(prefs.getString(KEY_MESSAGES, "[]"))
        val next = JSONArray()
        next.put(
            JSONObject()
                .put("at", System.currentTimeMillis())
                .put("kind", kind)
                .put("channel", channelId)
                .put("title", title)
                .put("body", body),
        )
        val kept = minOf(existing.length(), MAX_MESSAGES - 1)
        for (index in 0 until kept) {
            next.put(existing.getJSONObject(index))
        }
        prefs.edit().putString(KEY_MESSAGES, next.toString()).apply()
    }

    fun messages(): List<PushMessage> {
        val array = JSONArray(prefs.getString(KEY_MESSAGES, "[]"))
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            PushMessage(
                atMillis = item.getLong("at"),
                kind = item.getString("kind"),
                channelId = item.getString("channel"),
                title = item.getString("title"),
                body = item.getString("body"),
            )
        }
    }

    fun clearMessages() {
        prefs.edit().putString(KEY_MESSAGES, "[]").apply()
    }

    fun listen(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unlisten(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        const val PREFS = "push_kit"
        const val KEY_TOKEN = "token"
        const val KEY_MESSAGES = "messages"
        private const val MAX_MESSAGES = 30
    }
}

data class PushMessage(
    val atMillis: Long,
    val kind: String,
    val channelId: String,
    val title: String,
    val body: String,
)
