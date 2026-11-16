package com.cgsi.clavier

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * Fait apparaître le téléphone/la tablette comme un clavier Bluetooth (profil HID Device, Android 9+).
 * L'ordinateur interprète les codes de touches selon SA disposition (AZERTY/QWERTY/QWERTZ) :
 * choisissez dans l'appli la même disposition que celle réglée sur l'ordinateur.
 */
@SuppressLint("MissingPermission")
class HidKeyboard(
    private val context: Context,
    private val onState: (connected: Boolean, name: String) -> Unit
) {
    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var hid: BluetoothHidDevice? = null
    private var host: BluetoothDevice? = null
    private val executor = Executors.newSingleThreadExecutor()

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    host = device
                    onState(true, runCatching { device.name }.getOrNull() ?: device.address)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (host?.address == device.address) host = null
                    onState(false, "")
                }
            }
        }
    }

    /** À appeler une fois la permission Bluetooth accordée. */
    fun start() {
        adapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile != BluetoothProfile.HID_DEVICE) return
                val h = proxy as BluetoothHidDevice
                hid = h
                val sdp = BluetoothHidDeviceAppSdpSettings(
                    "Clavier CGSI", "Clavier plein écran", "CGSI",
                    BluetoothHidDevice.SUBCLASS1_KEYBOARD, DESCRIPTOR
                )
                h.registerApp(sdp, null, null, executor, callback)
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hid = null
                    host = null
                }
            }
        }, BluetoothProfile.HID_DEVICE)
    }

    fun pairedDevices(): String {
        val arr = JSONArray()
        runCatching {
            adapter?.bondedDevices?.forEach {
                arr.put(JSONObject().put("name", it.name ?: it.address).put("address", it.address))
            }
        }
        return arr.toString()
    }

    fun connect(address: String) {
        val dev = runCatching { adapter?.getRemoteDevice(address) }.getOrNull() ?: return
        hid?.connect(dev)
    }

    fun disconnect() {
        host?.let { hid?.disconnect(it) }
    }

    /** Envoie une pression puis un relâchement de touche. mods : bits HID (Maj = 0x02, AltGr = 0x40). */
    fun sendKey(code: Int, mods: Int) {
        val h = hid ?: return
        val d = host ?: return
        h.sendReport(d, REPORT_ID, byteArrayOf(mods.toByte(), 0, code.toByte(), 0, 0, 0, 0, 0))
        h.sendReport(d, REPORT_ID, ByteArray(8))
    }

    fun stop() {
        runCatching {
            hid?.unregisterApp()
            adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid)
        }
        hid = null
        host = null
    }

    companion object {
        private const val REPORT_ID = 1

        // Descripteur HID d'un clavier standard (8 octets : modificateurs, réservé, 6 touches) + LED
        private val DESCRIPTOR: ByteArray = intArrayOf(
            0x05, 0x01, 0x09, 0x06, 0xA1, 0x01, 0x85, REPORT_ID,
            0x05, 0x07, 0x19, 0xE0, 0x29, 0xE7, 0x15, 0x00, 0x25, 0x01,
            0x75, 0x01, 0x95, 0x08, 0x81, 0x02,
            0x95, 0x01, 0x75, 0x08, 0x81, 0x01,
            0x95, 0x05, 0x75, 0x01, 0x05, 0x08, 0x19, 0x01, 0x29, 0x05, 0x91, 0x02,
            0x95, 0x01, 0x75, 0x03, 0x91, 0x01,
            0x95, 0x06, 0x75, 0x08, 0x15, 0x00, 0x25, 0x65,
            0x05, 0x07, 0x19, 0x00, 0x29, 0x65, 0x81, 0x00,
            0xC0
        ).map { it.toByte() }.toByteArray()
    }
}
