package com.timefor.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.concurrent.Executors

/** Settings screen: accessibility permission status and the list of apps to limit. */
class MainActivity : AppCompatActivity() {

    private lateinit var store: LimitStore
    private lateinit var adapter: AppAdapter
    private val loader = Executors.newSingleThreadExecutor()
    private var allApps: List<AppEntry> = emptyList()
    private var query = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.root).padForSystemBars()
        store = LimitStore(this)

        findViewById<Button>(R.id.enableServiceButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        adapter = AppAdapter { app ->
            store.setLimited(app.packageName, !store.isLimited(app.packageName))
            adapter.notifyItemChanged(adapter.apps.indexOf(app))
        }
        findViewById<RecyclerView>(R.id.appList).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        findViewById<EditText>(R.id.search).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString().orEmpty().trim()
                applyFilter()
            }
        })

        loadApps()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    override fun onDestroy() {
        loader.shutdownNow()
        super.onDestroy()
    }

    private fun updateServiceStatus() {
        val enabled = isServiceEnabled()
        findViewById<TextView>(R.id.serviceStatus).setText(
            if (enabled) R.string.service_enabled else R.string.service_disabled
        )
        findViewById<Button>(R.id.enableServiceButton).visibility = if (enabled) View.GONE else View.VISIBLE
    }

    private fun isServiceEnabled(): Boolean {
        val ours = ComponentName(this, AppMonitorService::class.java)
        val manager = getSystemService(AccessibilityManager::class.java) ?: return false
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.let { s -> ComponentName(s.packageName, s.name) == ours } }
    }

    private fun loadApps() {
        loader.execute {
            val pm = packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val limited = store.limitedPackages()
            val apps = pm.queryIntentActivities(launcherIntent, 0)
                .map { it.activityInfo.applicationInfo }
                .filter { it.packageName != packageName }
                .distinctBy { it.packageName }
                .map { AppEntry(it.packageName, pm.getApplicationLabel(it).toString(), it.loadIcon(pm)) }
                // Already-limited apps first, then alphabetical.
                .sortedWith(compareBy<AppEntry> { it.packageName !in limited }.thenBy { it.label.lowercase() })
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                allApps = apps
                applyFilter()
            }
        }
    }

    private fun applyFilter() {
        adapter.apps = if (query.isEmpty()) allApps else allApps.filter { it.label.contains(query, ignoreCase = true) }
    }

    data class AppEntry(val packageName: String, val label: String, val icon: Drawable)

    private inner class AppAdapter(
        private val onToggle: (AppEntry) -> Unit,
    ) : RecyclerView.Adapter<AppAdapter.Holder>() {

        var apps: List<AppEntry> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.appIcon)
            val name: TextView = view.findViewById(R.id.appName)
            val switch: MaterialSwitch = view.findViewById(R.id.appSwitch)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false))

        override fun getItemCount() = apps.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val app = apps[position]
            holder.icon.setImageDrawable(app.icon)
            holder.name.text = app.label
            holder.switch.isChecked = store.isLimited(app.packageName)
            holder.itemView.setOnClickListener { onToggle(app) }
        }
    }
}
