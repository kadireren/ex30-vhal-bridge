package com.kadireren.ex30vhalbridge

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.ParcelUuid
import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@SuppressLint("MissingPermission")
class BleBridge(
    private val context: Context,
    private val onSubscription: (Subscription) -> Unit,
    private val onStatus: (String) -> Unit,
) {
    private val adapter: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val dirtyValues = ConcurrentHashMap<String, Double>()
    private val sequence = AtomicLong()
    @Volatile private var gatt: BluetoothGatt? = null
    @Volatile private var telemetry: BluetoothGattCharacteristic? = null
    @Volatile private var running = false
    @Volatile private var scanning = false
    @Volatile private var timeSyncPending = false
    private var sendTask: ScheduledFuture<*>? = null
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED &&
                intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) == BluetoothAdapter.STATE_ON) {
                startScan()
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (gatt != null) return
            adapter?.bluetoothLeScanner?.stopScan(this)
            scanning = false
            onStatus("Dashboard bulundu · BLE bağlanıyor")
            val pendingGatt = result.device.connectGatt(context, false, gattCallback, BluetoothDeviceTransport.TRANSPORT_LE)
            gatt = pendingGatt
            executor.schedule({
                if (gatt === pendingGatt && telemetry == null) {
                    onStatus("BLE bağlantısı zaman aşımına uğradı · tekrar deneniyor")
                    pendingGatt.disconnect()
                    closeGatt(pendingGatt)
                    scheduleScan()
                }
            }, CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        }
        override fun onScanFailed(errorCode: Int) {
            scanning = false
            onStatus("BLE tarama hatası: $errorCode")
            scheduleScan()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                timeSyncPending = true
                onStatus("Dashboard bağlı · servisler okunuyor")
                if (!gatt.requestMtu(185)) gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS) {
                closeGatt(gatt)
                onStatus("Dashboard BLE bağlantısı bekleniyor")
                scheduleScan()
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(BleProtocol.SERVICE_UUID)
            telemetry = service?.getCharacteristic(BleProtocol.TELEMETRY_UUID)
            val subscription = service?.getCharacteristic(BleProtocol.SUBSCRIPTION_UUID)
            if (status != BluetoothGatt.GATT_SUCCESS || telemetry == null || subscription == null) {
                onStatus("Dashboard VHAL servisi bulunamadı")
                gatt.disconnect()
                return
            }
            gatt.setCharacteristicNotification(subscription, true)
            subscription.getDescriptor(CLIENT_CONFIG_UUID)?.let {
                it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(it)
            } ?: gatt.readCharacteristic(subscription)
            onStatus("Dashboard BLE bağlı")
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            gatt.getService(BleProtocol.SERVICE_UUID)
                ?.getCharacteristic(BleProtocol.SUBSCRIPTION_UUID)
                ?.let(gatt::readCharacteristic)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) consumeSubscription(characteristic)
            sendTimeSync()
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            consumeSubscription(characteristic)
        }
    }

    fun start() {
        if (running) return
        running = true
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(bluetoothReceiver, filter)
        }
        sendTask = executor.scheduleAtFixedRate(::sendPending, 0, 34, TimeUnit.MILLISECONDS)
        startScan()
    }

    fun offer(values: Map<String, Double>) = dirtyValues.putAll(values)

    fun stop() {
        running = false
        sendTask?.cancel(true)
        runCatching { context.unregisterReceiver(bluetoothReceiver) }
        adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        scanning = false
        gatt?.let(::closeGatt)
        executor.shutdownNow()
    }

    private fun consumeSubscription(characteristic: BluetoothGattCharacteristic) {
        BleProtocol.parseSubscription(characteristic.value ?: return)?.let(onSubscription)
    }

    private fun sendTimeSync(): Boolean {
        val characteristic = telemetry ?: return false
        val activeGatt = gatt ?: return false
        if (!timeSyncPending) return false
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        characteristic.value = BleProtocol.timeSyncNow()
        val ok = activeGatt.writeCharacteristic(characteristic)
        if (ok) timeSyncPending = false
        return ok
    }

    private fun sendPending() {
        if (!running) return
        if (timeSyncPending && sendTimeSync()) return
        if (dirtyValues.isEmpty()) return
        val characteristic = telemetry ?: return
        val activeGatt = gatt ?: return
        val values = dirtyValues.toMap()
        val payload = BleProtocol.telemetry(sequence.incrementAndGet(), SystemClock.elapsedRealtime(), values)
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        characteristic.value = payload
        if (activeGatt.writeCharacteristic(characteristic)) {
            values.forEach { (key, value) -> dirtyValues.remove(key, value) }
        }
    }

    private fun startScan() {
        if (!running || adapter?.isEnabled != true || gatt != null || scanning) {
            if (adapter?.isEnabled != true) onStatus("Bluetooth kapalı")
            return
        }
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(BleProtocol.SERVICE_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            onStatus("BLE tarayıcı kullanılamıyor")
            scheduleScan()
            return
        }
        scanning = true
        scanner.startScan(listOf(filter), settings, scanCallback)
        onStatus("iPhone/CrowPanel BLE aranıyor")
        executor.schedule({
            if (running && scanning && gatt == null) {
                scanner.stopScan(scanCallback)
                scanning = false
                scheduleScan()
            }
        }, SCAN_WINDOW_SECONDS, TimeUnit.SECONDS)
    }

    private fun scheduleScan() {
        if (running) executor.schedule(::startScan, 1500, TimeUnit.MILLISECONDS)
    }

    private fun closeGatt(target: BluetoothGatt) {
        if (gatt === target) gatt = null
        telemetry = null
        target.close()
    }

    private object BluetoothDeviceTransport { const val TRANSPORT_LE = 2 }
    companion object {
        private const val SCAN_WINDOW_SECONDS = 12L
        private const val CONNECTION_TIMEOUT_SECONDS = 15L
        private val CLIENT_CONFIG_UUID = java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
