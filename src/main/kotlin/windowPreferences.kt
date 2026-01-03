package org.example
import java.awt.Rectangle
import java.util.prefs.Preferences

val prefs: Preferences = Preferences.userRoot().node("wizplay")

private const val PREF_X = "window.x"
private const val PREF_Y = "window.y"
private const val PREF_WIDTH = "window.width"
private const val PREF_HEIGHT = "window.height"
const val PREF_FULLSCREEN = "window.fullscreen"

fun loadWindowBounds(): Rectangle {
    val x = prefs.getInt(PREF_X, -1)
    val y = prefs.getInt(PREF_Y, -1)
    val w = prefs.getInt(PREF_WIDTH, 1280)
    val h = prefs.getInt(PREF_HEIGHT, 720)

    return if (x == -1 || y == -1) {
        Rectangle(0, 0, w, h)
    } else {
        Rectangle(x, y, w, h)
    }
}

fun saveWindowBounds(bounds: Rectangle) {
    prefs.putInt(PREF_X, bounds.x)
    prefs.putInt(PREF_Y, bounds.y)
    prefs.putInt(PREF_WIDTH, bounds.width)
    prefs.putInt(PREF_HEIGHT, bounds.height)
}