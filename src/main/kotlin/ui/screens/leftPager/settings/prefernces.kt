package org.example.ui.screens.leftPager.settings

import java.util.prefs.Preferences

object AppPrefs {

    private val prefs = Preferences.userRoot().node("wizplay")

    fun getBool(key: String, default: Boolean = false): Boolean =
        prefs.getBoolean(key, default)

    fun setBool(key: String, value: Boolean) {
        prefs.putBoolean(key, value)
    }

    fun getInt(key: String, default: Int = 0): Int =
        prefs.getInt(key, default)

    fun getFloat(key: String, default: Float = 0f): Float =
        prefs.getFloat(key, default)

    fun setInt(key: String, value: Int) {
        prefs.putInt(key, value)
    }

    fun setFloat(key: String, value: Float) {
        prefs.putFloat(key, value)
    }

    fun getString(key: String, default: String = ""): String =
        prefs.get(key, default)

    fun setString(key: String, value: String) {
        prefs.put(key, value)
    }
}