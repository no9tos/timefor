package com.timefor.app

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists which apps are limited and, for each one, when its current session ends.
 * A session exists only between the user picking a duration and that duration running out.
 */
class LimitStore(context: Context) {

    val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun limitedPackages(): Set<String> = prefs.getStringSet(KEY_LIMITED, emptySet()).orEmpty()

    fun isLimited(packageName: String): Boolean = packageName in limitedPackages()

    fun setLimited(packageName: String, limited: Boolean) {
        val updated = limitedPackages().toMutableSet()
        if (limited) updated += packageName else updated -= packageName
        prefs.edit().putStringSet(KEY_LIMITED, updated).apply()
        if (!limited) endSession(packageName)
    }

    /** End of the running session in epoch millis, or 0 if there is none. */
    fun sessionEnd(packageName: String): Long = prefs.getLong(sessionKey(packageName), 0L)

    fun hasActiveSession(packageName: String, now: Long = System.currentTimeMillis()): Boolean =
        sessionEnd(packageName) > now

    fun startSession(packageName: String, minutes: Int) {
        val end = System.currentTimeMillis() + minutes * 60_000L
        prefs.edit().putLong(sessionKey(packageName), end).apply()
    }

    fun endSession(packageName: String) {
        prefs.edit().remove(sessionKey(packageName)).apply()
    }

    /** Packages that currently have a session stored (active or overdue). */
    fun packagesWithSessions(): List<String> =
        prefs.all.keys.filter { it.startsWith(SESSION_PREFIX) }.map { it.removePrefix(SESSION_PREFIX) }

    var lastChosenMinutes: Int
        get() = prefs.getInt(KEY_LAST_MINUTES, DEFAULT_MINUTES)
        set(value) = prefs.edit().putInt(KEY_LAST_MINUTES, value).apply()

    companion object {
        private const val PREFS_NAME = "timefor"
        private const val KEY_LIMITED = "limited_packages"
        private const val KEY_LAST_MINUTES = "last_minutes"
        const val SESSION_PREFIX = "session_end:"
        const val DEFAULT_MINUTES = 5

        fun sessionKey(packageName: String) = SESSION_PREFIX + packageName
    }
}
