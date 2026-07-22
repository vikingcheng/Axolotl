package com.alan.axolotl.data

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject

/**
 * Stores app settings — currently just the parental-lock password.
 *
 * Behaviour is intentionally unchanged from the previous top-level
 * `getTimerPassword`/`setTimerPassword` helpers: the password is still kept in
 * the same SharedPreferences under the same key. Moving it behind this
 * interface is what lets the ViewModels depend on an abstraction (and lets a
 * future change swap in hashed / encrypted storage without touching callers).
 */
interface SettingsRepository {
    fun getTimerPassword(): String
    fun setTimerPassword(password: String)
}

class DefaultSettingsRepository @Inject constructor(
    private val prefs: SharedPreferences
) : SettingsRepository {

    override fun getTimerPassword(): String =
        prefs.getString(KEY_TIMER_PASSWORD, DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD

    override fun setTimerPassword(password: String) {
        prefs.edit { putString(KEY_TIMER_PASSWORD, password) }
    }

    companion object {
        const val PREFS_NAME = "axolotl_prefs"
        private const val KEY_TIMER_PASSWORD = "timer_password"
        private const val DEFAULT_PASSWORD = "8922"
    }
}
