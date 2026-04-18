package com.stepserve.app.data.auth

import android.content.Context
import android.content.SharedPreferences

object TokenManager {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("stepserve_auth", Context.MODE_PRIVATE)
    }

    fun getToken(): String? = prefs.getString("access_token", null)

    fun setToken(token: String) {
        prefs.edit().putString("access_token", token).apply()
    }

    fun setUser(email: String, role: String) {
        prefs.edit()
            .putString("user_email", email)
            .putString("user_role", role)
            .apply()
    }

    fun getEmail(): String? = prefs.getString("user_email", null)
    fun getRole(): String? = prefs.getString("user_role", null)

    fun isLoggedIn(): Boolean = getToken() != null

    fun clear() {
        prefs.edit().clear().apply()
    }
}
