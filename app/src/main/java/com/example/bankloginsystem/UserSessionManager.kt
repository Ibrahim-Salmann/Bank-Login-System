package com.example.bankloginsystem

import android.content.Context
import android.content.SharedPreferences

/**
 * This class manages the user session using SharedPreferences.
 * It provides a centralized way to handle saving, retrieving, and clearing user session data.
 */
class UserSessionManager(context: Context) {

    // SharedPreferences is a private file stored in the app's internal storage.
    // "LoggedIN" is the name of our preference file.
    private val sharedPreference: SharedPreferences = context.getSharedPreferences("LoggedIN", Context.MODE_PRIVATE)
    // The editor is used to write data to the SharedPreferences file.
    private val prefEditor = sharedPreference.edit()

    /**
     * A companion object holds constants that are shared across all instances of this class.
     * These keys are used to uniquely identify each piece of data stored in SharedPreferences.
     */
    companion object {
         const val PREF_FULL_NAME = "fullName"
         const val PREF_EMAIL = "email"
         const val PREF_IS_LOGIN = "isLogin"
    }

    /**
     * Saves the user session. This is called upon successful login.
     * It stores the user's name, email, and sets the login flag to true.
     * .apply() saves the changes asynchronously, which is better for performance.
     */
    fun saveUser(fullName: String, email: String) {
        prefEditor.putString(PREF_FULL_NAME, fullName)
        prefEditor.putString(PREF_EMAIL, email)
        prefEditor.putBoolean(PREF_IS_LOGIN, true)
        prefEditor.apply()
    }

    /**
     * Checks if a user is currently logged in.
     * This is the core function for session validation. It defaults to `false` if the key doesn't exist.
     */
    fun isLoggedIn(): Boolean {
        return sharedPreference.getBoolean(PREF_IS_LOGIN, false)
    }

    /**
     * Retrieves the details of the currently logged-in user from the session.
     * Returns a HashMap containing the user's name and email.
     */
    fun getUserDetails(): HashMap<String, String> {
        val userData = HashMap<String, String>()
        userData[PREF_FULL_NAME] = sharedPreference.getString(PREF_FULL_NAME, "") ?: ""
        userData[PREF_EMAIL] = sharedPreference.getString(PREF_EMAIL, "") ?: ""
        return userData
    }

    /**
     * Logs the user out by clearing all data from the SharedPreferences file.
     * This effectively destroys the session.
     */
    fun logoutUser() {
        prefEditor.clear()
        prefEditor.apply()
    }
}
