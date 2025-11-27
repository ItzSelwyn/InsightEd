package com.example.insighted.ble

import android.Manifest
import android.content.Context
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.insighted.viewmodels.UserViewModel

import java.util.UUID

class BleAdvertiser(private val context: Context) {
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
    private val TAG = "BleAdvertiser"
    private lateinit var advertiseCallback: AdvertiseCallback

    fun startAdvertising(serviceUuidString: String) {
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            return
        }
        if (advertiser == null) {
            Toast.makeText(context, "BLE advertising not supported", Toast.LENGTH_SHORT).show()
            return
        }

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                Log.d(TAG, "Advertising started with service UUID: $serviceUuidString")
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                Log.e(TAG, "Advertising failed: $errorCode")
                Toast.makeText(context, "Advertising failed: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val serviceUuid = try {
            UUID.fromString(serviceUuidString)
        } catch (e: IllegalArgumentException) {
            Toast.makeText(context, "Invalid UUID format", Toast.LENGTH_SHORT).show()
            return
        }

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(serviceUuid))
            .setIncludeDeviceName(false)
            .build()

        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun stopAdvertising() {
        advertiser?.stopAdvertising(advertiseCallback)
        Toast.makeText(context, "Advertising stopped", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Advertising stopped")
    }
}
