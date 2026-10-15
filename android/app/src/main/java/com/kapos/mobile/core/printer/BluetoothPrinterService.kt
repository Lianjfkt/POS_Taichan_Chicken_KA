package com.kapos.mobile.core.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

data class BluetoothDeviceInfo(
    val name: String,
    val address: String
)

class BluetoothPrinterService(private val context: Context) {

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDeviceInfo> {
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return try {
            adapter.bondedDevices.map { device ->
                BluetoothDeviceInfo(
                    name = device.name ?: "Unknown Device",
                    address = device.address
                )
            }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    suspend fun printBytes(macAddress: String, data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter ?: return@withContext Result.failure(Exception("Bluetooth tidak didukung pada perangkat ini"))
        if (!adapter.isEnabled) return@withContext Result.failure(Exception("Bluetooth belum aktif. Silakan hidupkan Bluetooth."))
        if (macAddress.isBlank()) return@withContext Result.failure(Exception("Printer belum dipilih di Pengaturan."))

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(macAddress)
            try {
                adapter.cancelDiscovery()
            } catch (_: SecurityException) {}

            socket = device.createRfcommSocketToServiceRecord(sppUuid)
            socket.connect()

            outputStream = socket.outputStream
            outputStream.write(data)
            outputStream.flush()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mencetak: ${e.localizedMessage ?: "Koneksi printer terputus"}"))
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }
}
