package com.timefor.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Full-screen question shown on top of a limited app: "how long do you want to spend here?".
 * Choosing a duration starts a session and reveals the app; cancelling sends the user home.
 */
class TimePickerActivity : AppCompatActivity() {

    private lateinit var store: LimitStore
    private lateinit var minutesPicker: NumberPicker
    private lateinit var presets: ChipGroup
    private lateinit var startButton: Button
    private var targetPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_picker)
        findViewById<View>(R.id.root).padForSystemBars()
        store = LimitStore(this)

        minutesPicker = findViewById(R.id.minutesPicker)
        presets = findViewById(R.id.presets)
        startButton = findViewById(R.id.startButton)

        minutesPicker.minValue = MIN_MINUTES
        minutesPicker.maxValue = MAX_MINUTES
        minutesPicker.wrapSelectorWheel = false
        minutesPicker.setOnValueChangedListener { _, _, value -> onMinutesChanged(value) }

        PRESET_MINUTES.forEach { minutes ->
            val chip = Chip(this).apply {
                id = minutes
                text = getString(R.string.preset_minutes, minutes)
                isCheckable = true
            }
            presets.addView(chip)
        }
        presets.setOnCheckedStateChangeListener { _, checkedIds ->
            checkedIds.firstOrNull()?.let { minutesPicker.value = it; onMinutesChanged(it) }
        }

        startButton.setOnClickListener { startSession() }
        findViewById<Button>(R.id.cancelButton).setOnClickListener { cancel() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = cancel()
        })

        val initial = store.lastChosenMinutes.coerceIn(MIN_MINUTES, MAX_MINUTES)
        minutesPicker.value = initial
        onMinutesChanged(initial)
        bind(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        bind(intent)
    }

    private fun bind(intent: Intent) {
        val pkg = intent.getStringExtra(EXTRA_PACKAGE)
        if (pkg == null) {
            finish()
            return
        }
        targetPackage = pkg

        val (label, icon) = try {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info) to packageManager.getApplicationIcon(info)
        } catch (e: PackageManager.NameNotFoundException) {
            pkg to null
        }
        findViewById<TextView>(R.id.title).text = getString(R.string.picker_title, label)
        findViewById<ImageView>(R.id.appIcon).setImageDrawable(icon)
    }

    private fun onMinutesChanged(minutes: Int) {
        startButton.text = getString(R.string.picker_start, minutes)
        if (presets.checkedChipId != minutes) {
            if (minutes in PRESET_MINUTES) presets.check(minutes) else presets.clearCheck()
        }
    }

    private fun startSession() {
        val pkg = targetPackage ?: return finish()
        val minutes = minutesPicker.value
        store.lastChosenMinutes = minutes
        store.startSession(pkg, minutes)
        // The limited app is right underneath; finishing reveals it.
        finish()
    }

    private fun cancel() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "com.timefor.app.extra.PACKAGE"
        private const val MIN_MINUTES = 1
        private const val MAX_MINUTES = 180
        private val PRESET_MINUTES = listOf(1, 5, 10, 15, 30, 60)
    }
}
