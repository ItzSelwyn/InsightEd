package com.example.insighted.session

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate

object SessionManager {

    private const val PREF_NAME = "insighted_session"
    private const val KEY_LOGIN_DATE = "login_date"
    private const val KEY_UUID = "user_uuid"

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveLoginToday(context: Context, uuid: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LOGIN_DATE, LocalDate.now().toString())
            .putString(KEY_UUID, uuid)
            .apply()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun isLoggedInToday(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedDate = prefs.getString(KEY_LOGIN_DATE, null)
        return savedDate == LocalDate.now().toString()
    }

    fun getSavedUUID(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_UUID, null)
    }

    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}