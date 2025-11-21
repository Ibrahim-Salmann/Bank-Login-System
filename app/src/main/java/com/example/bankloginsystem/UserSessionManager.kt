package com.example.bankloginsystem

import android.content.Context
import android.content.SharedPreferences

/**
 * This class manages the user session using SharedPreferences, providing a simple and efficient
 * way to persist user login state and basic information across app launches.
 *
 * SharedPreferences is used to store a small collection of key-value pairs. It's ideal for saving
 * lightweight data like user preferences or, in this case, session details.
 *
 * @param context The application context, which is required to access SharedPreferences.
 */
class UserSessionManager(context: Context) {

    // The SharedPreferences file is named "LoggedIN" and is private to this app.
    private val sharedPreference: SharedPreferences = context.getSharedPreferences("LoggedIN", Context.MODE_PRIVATE)
    // The editor is used to make changes to the SharedPreferences file.
    private val prefEditor = sharedPreference.edit()

    /**
     * A companion object to hold the keys for the key-value pairs stored in SharedPreferences.
     * Using constants for keys prevents typos and makes the code more maintainable.
     */
    companion object {
         const val PREF_FULL_NAME = "fullName"
         const val PREF_EMAIL = "email"
         const val PREF_IS_LOGIN = "isLogin" // A boolean flag to check login status.
         const val PREF_USER_ID = "id" // The user's ID from the local SQLite database.
    }

    /**
     * Saves the user's session data to SharedPreferences upon a successful login.
     * This method stores the user's full name, email, local database ID, and sets the login flag to true.
     *
     * `apply()` is used to save the changes asynchronously in the background, which is more
     * efficient than `commit()`, as it doesn't block the main thread.
     *
     * @param fullName The full name of the user.
     * @param email The email of the user.
     * @param id The user's ID from the local SQLite database.
     */
    fun saveUser(fullName: String, email: String, id: Int) {
        prefEditor.putString(PREF_FULL_NAME, fullName)
        prefEditor.putString(PREF_EMAIL, email)
        prefEditor.putBoolean(PREF_IS_LOGIN, true)
        prefEditor.putString(PREF_USER_ID, id.toString())
        prefEditor.apply() // Save the changes.
    }

    /**
     * Checks whether a user is currently logged in by reading the `PREF_IS_LOGIN` flag.
     * This is the primary method for guarding authenticated routes in the app.
     *
     * @return `true` if the user is logged in, `false` otherwise.
     */
    fun isLoggedIn(): Boolean {
        return sharedPreference.getBoolean(PREF_IS_LOGIN, false)
    }

    /**
     * Retrieves the stored details of the currently logged-in user.
     *
     * @return A `HashMap` containing the user's full name, email, and local ID.
     *         Returns empty strings or "0" if the data is not found.
     */
    fun getUserDetails(): HashMap<String, String> {
        val userData = HashMap<String, String>()
        userData[PREF_FULL_NAME] = sharedPreference.getString(PREF_FULL_NAME, "") ?: ""
        userData[PREF_EMAIL] = sharedPreference.getString(PREF_EMAIL, "") ?: ""
        userData[PREF_USER_ID] = sharedPreference.getString(PREF_USER_ID, "0") ?: "0"
        return userData
    }

    /**
     * Logs the user out by clearing all data from the SharedPreferences file.
     * This effectively ends the user's session and requires them to log in again.
     */
    fun logoutUser() {
        prefEditor.clear() // Remove all preferences.
        prefEditor.apply() // Apply the changes.
    }
}
