package com.kadireren.ex30vhalbridge

import android.content.Context

object BridgeState {
    private const val PREFS = "bridge_state"
    private const val STATUS = "status"
    fun save(context: Context, status: String) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putString(STATUS, status).apply()
    fun read(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(STATUS, "Başlatılıyor").orEmpty()
}
