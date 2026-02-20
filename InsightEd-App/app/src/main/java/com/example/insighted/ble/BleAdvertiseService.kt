package com.example.insighted.ble

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.insighted.R
import com.example.insighted.viewmodels.UserSession
import java.util.*
import android.os.ParcelUuid
import android.util.Log

class BleAdvertiseService : Service() {

    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("BLE_SERVICE", "Service Created")
        startForegroundService()
        startAdvertising()
    }

    override fun onDestroy() {
        Log.d("BLE_SERVICE", "Service Destroyed")
        stopAdvertising()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // 🔵 START FOREGROUND NOTIFICATION
    private fun startForegroundService() {

        val channelId = "ble_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "BLE Advertising",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("InsightEd Running")
            .setContentText("Broadcasting attendance UUID")
            .setSmallIcon(R.drawable.app_icon)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    // 🔵 START BLE ADVERTISING
    private fun startAdvertising() {

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        advertiser = bluetoothAdapter.bluetoothLeAdvertiser

        val uuidString = UserSession.uuid ?: return

        val serviceUuid = ParcelUuid(UUID.nameUUIDFromBytes(uuidString.toByteArray()))

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(serviceUuid)
            .setIncludeDeviceName(false)
            .build()

        advertiseCallback = object : AdvertiseCallback() {}

        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    // 🔴 STOP BLE ADVERTISING
    private fun stopAdvertising() {
        advertiseCallback?.let {
            advertiser?.stopAdvertising(it)
        }
    }
}