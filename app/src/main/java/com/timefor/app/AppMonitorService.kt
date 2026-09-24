package com.timefor.app

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.HandlerCompat

/**
 * Watches which app comes to the foreground. When a limited app opens without a running
 * session, it shows [TimePickerActivity]; when a session runs out, it sends the user home
 * and terminates the app, so the next launch asks for a duration again.
 */
class AppMonitorService : AccessibilityService() {

    private lateinit var store: LimitStore
    private val handler = Handler(Looper.getMainLooper())

    /** Session end each package's timers were scheduled for, to avoid rescheduling. */
    private val scheduledEnds = mutableMapOf<String, Long>()

    private var lastWindowPackage: String? = null
    private var lastPromptPackage: String? = null
    private var lastPromptAt = 0L

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null && key.startsWith(LimitStore.SESSION_PREFIX)) {
            syncSession(key.removePrefix(LimitStore.SESSION_PREFIX))
        }
    }

    // Handler timers are paused while the device sleeps, so re-check when it wakes up.
    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = syncAllSessions()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        store = LimitStore(this)
        store.prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, wakeReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        syncAllSessions()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        lastWindowPackage = pkg
        if (pkg == packageName || !::store.isInitialized || !store.isLimited(pkg)) return

        if (store.hasActiveSession(pkg)) {
            syncSession(pkg)
        } else {
            store.endSession(pkg)
            promptForDuration(pkg)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::store.isInitialized) {
            store.prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
            unregisterReceiver(wakeReceiver)
        }
        super.onDestroy()
    }

    private fun promptForDuration(pkg: String) {
        // Opening an app fires several window events in a row; show the picker only once.
        val now = SystemClock.elapsedRealtime()
        if (pkg == lastPromptPackage && now - lastPromptAt < PROMPT_DEBOUNCE_MS) return
        lastPromptPackage = pkg
        lastPromptAt = now

        startActivity(
            Intent(this, TimePickerActivity::class.java)
                .putExtra(TimePickerActivity.EXTRA_PACKAGE, pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        )
    }

    private fun syncAllSessions() {
        if (!::store.isInitialized) return
        (store.packagesWithSessions() + scheduledEnds.keys).toSet().forEach(::syncSession)
    }

    /** Brings the timers for [pkg] in line with its stored session end. */
    private fun syncSession(pkg: String) {
        val end = store.sessionEnd(pkg)
        if (end == 0L) {
            cancelTimers(pkg)
            return
        }
        val remaining = end - System.currentTimeMillis()
        if (remaining <= 0) {
            onSessionExpired(pkg)
            return
        }
        if (scheduledEnds[pkg] == end) return

        cancelTimers(pkg)
        scheduledEnds[pkg] = end
        HandlerCompat.postDelayed(handler, { onSessionExpired(pkg) }, pkg, remaining)
        if (remaining > WARNING_BEFORE_MS) {
            HandlerCompat.postDelayed(handler, { warnOneMinuteLeft(pkg) }, pkg, remaining - WARNING_BEFORE_MS)
        }
    }

    private fun cancelTimers(pkg: String) {
        handler.removeCallbacksAndMessages(pkg)
        scheduledEnds.remove(pkg)
    }

    private fun warnOneMinuteLeft(pkg: String) {
        if (foregroundPackage() == pkg) {
            Toast.makeText(this, getString(R.string.toast_one_minute_left, labelOf(pkg)), Toast.LENGTH_LONG).show()
        }
    }

    private fun onSessionExpired(pkg: String) {
        cancelTimers(pkg)
        store.endSession(pkg)

        if (foregroundPackage() == pkg) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            Toast.makeText(this, getString(R.string.toast_time_up, labelOf(pkg)), Toast.LENGTH_LONG).show()
        }
        // Once the app is in the background, end its process so it starts fresh next time.
        handler.postDelayed({ killInBackground(pkg) }, KILL_DELAY_MS)
    }

    private fun killInBackground(pkg: String) {
        if (store.hasActiveSession(pkg) || foregroundPackage() == pkg) return
        getSystemService(ActivityManager::class.java)?.killBackgroundProcesses(pkg)
    }

    private fun foregroundPackage(): String? =
        rootInActiveWindow?.packageName?.toString() ?: lastWindowPackage

    private fun labelOf(pkg: String): CharSequence = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0))
    } catch (e: Exception) {
        pkg
    }

    private companion object {
        const val PROMPT_DEBOUNCE_MS = 1_500L
        const val WARNING_BEFORE_MS = 60_000L
        const val KILL_DELAY_MS = 1_500L
    }
}
