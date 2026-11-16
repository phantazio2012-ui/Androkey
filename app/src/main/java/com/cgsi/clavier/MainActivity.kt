package com.cgsi.clavier

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONObject

class MainActivity : Activity() {

    private lateinit var web: WebView
    private lateinit var hid: HidKeyboard

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        hid = HidKeyboard(this) { connected, name ->
            runOnUiThread {
                web.evaluateJavascript(
                    "window.onBtState($connected, ${JSONObject.quote(name)})", null
                )
            }
        }

        web = WebView(this).apply {
            setBackgroundColor(0xFF0B3D91.toInt())
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(Bridge(), "Android")
            loadUrl("file:///android_asset/keyboard.html")
        }
        setContentView(web)
        hideSystemBars()

        if (hasBtPermission()) hid.start() else requestBtPermission()
    }

    // ---- Pont JavaScript -> Android (objet "Android" côté page) ----
    inner class Bridge {
        @JavascriptInterface fun sendKey(code: Int, mods: Int) = hid.sendKey(code, mods)
        @JavascriptInterface fun getDevices(): String = hid.pairedDevices()
        @JavascriptInterface fun connect(address: String) = hid.connect(address)
        @JavascriptInterface fun disconnect() = hid.disconnect()
        @JavascriptInterface fun discoverable() {
            runOnUiThread {
                startActivity(
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                        .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
                )
            }
        }
    }

    // ---- Permissions Bluetooth (Android 12+) ----
    private fun hasBtPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun requestBtPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE), 1
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (hasBtPermission()) {
            hid.start()
            web.evaluateJavascript("refreshDevices()", null)
        }
    }

    // ---- Plein écran immersif ----
    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onDestroy() {
        hid.stop()
        super.onDestroy()
    }
}
