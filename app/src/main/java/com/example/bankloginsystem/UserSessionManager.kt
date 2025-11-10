package com.example.bankloginsystem

import android.content.Context
import android.content.SharedPreferences

class UserSessionManager(context: Context) {

    private val sharedPreference: SharedPreferences = context.getSharedPreferences("LoggedIN", Context.MODE_PRIVATE)
    private val prefEditor = sharedPreference.edit()

    companion object {
         const val PREF_FULL_NAME = "fullName"
         const val PREF_EMAIL = "email"
         const val PREF_IS_LOGIN = "isLogin"
    }

    fun saveUser(fullName: String, email: String) {
        prefEditor.putString(PREF_FULL_NAME, fullName)
        prefEditor.putString(PREF_EMAIL, email)
        prefEditor.putBoolean(PREF_IS_LOGIN, true)
        prefEditor.apply()
    }

    fun isLoggedIn(): Boolean {
        return sharedPreference.getBoolean(PREF_IS_LOGIN, false)
    }

    fun getUserDetails(): HashMap<String, String> {
        val userData = HashMap<String, String>()
        userData[PREF_FULL_NAME] = sharedPreference.getString(PREF_FULL_NAME, "") ?: ""
        userData[PREF_EMAIL] = sharedPreference.getString(PREF_EMAIL, "") ?: ""
        return userData
    }

    fun logoutUser() {
        prefEditor.clear()
        prefEditor.apply()
    }
}