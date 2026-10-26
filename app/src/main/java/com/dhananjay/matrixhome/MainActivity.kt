package com.dhananjay.matrixhome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var state: HomeState

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = state.reloadApps()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = HomeState(applicationContext)
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, pkgReceiver, f, ContextCompat.RECEIVER_EXPORTED)
        setContent { HomeRoot(state, ::launchTile, ::openSettings, ::voice) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        state.homePressed++ // Home pressed while already on the launcher: reset to top
    }

    override fun onResume() {
        super.onResume()
        state.reloadInputs()
    }

    override fun onDestroy() {
        unregisterReceiver(pkgReceiver)
        super.onDestroy()
    }

    private fun launchTile(t: Tile) {
        try {
            startActivity(Intent(t.intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            if (t.isApp) state.markLaunched(t.key)
        } catch (_: Exception) {
            Toast.makeText(this, "Can't open ${t.label}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSettings(action: String, pkg: String? = null) {
        try {
            val i = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (pkg != null) i.data = Uri.parse("package:$pkg")
            startActivity(i)
        } catch (_: Exception) {
            Toast.makeText(this, "Not available on this TV", Toast.LENGTH_SHORT).show()
        }
    }

    private fun voice() {
        for (a in listOf(Intent.ACTION_VOICE_COMMAND, Intent.ACTION_ASSIST)) {
            try { startActivity(Intent(a).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return } catch (_: Exception) { }
        }
        Toast.makeText(this, "Voice assistant not available", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val WIFI = Settings.ACTION_WIFI_SETTINGS
    }
}
