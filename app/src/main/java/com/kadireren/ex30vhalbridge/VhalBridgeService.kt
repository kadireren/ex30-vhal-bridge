package com.kadireren.ex30vhalbridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class VhalBridgeService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val reconnectCar = Runnable { connectCar() }
    private var carGeneration = 0
    private var car: Car? = null
    private var reader: VhalPropertyReader? = null
    private var requestedKeys: Set<String> = emptySet()
    private lateinit var ble: BleBridge
    private lateinit var udp: UdpBridge

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(
            NOTIFICATION_ID,
            notification("Araç bağlantısı kuruluyor"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
        )
        val subscriptionHandler: (Subscription) -> Unit = { subscription ->
            if (subscription.keys != requestedKeys) {
                requestedKeys = subscription.keys
                val failures = reader?.configure(requestedKeys).orEmpty()
                setStatus(if (failures.isEmpty()) "BLE yayın aktif · ${requestedKeys.size} sensör" else "${failures.size} sensör okunamadı")
            }
        }
        ble = BleBridge(applicationContext, subscriptionHandler, ::setStatus).also { it.start() }
        udp = UdpBridge(
            onSubscription = { subscription ->
                subscriptionHandler(subscription)
            },
            onStatus = { status ->
                if (status.contains("hata") || status.startsWith("CrowPanel bağlı")) setStatus("UDP fallback · $status")
            },
        ).also { it.start() }
        connectCar()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RECONNECT -> connectCar()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(reconnectCar)
        carGeneration++
        reader?.stop()
        ble.stop()
        udp.stop()
        runCatching { car?.disconnect() }
        car = null
        setStatus("Yayın durduruldu")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun connectCar() {
        handler.removeCallbacks(reconnectCar)
        val generation = ++carGeneration
        runCatching { car?.disconnect() }
        car = null
        reader?.stop()
        reader = null
        runCatching {
            car = Car.createCar(this, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { readyCar, ready ->
                if (generation != carGeneration) return@createCar
                if (!ready) {
                    setStatus("Araç VHAL bağlantısı bekleniyor")
                    handler.postDelayed(reconnectCar, RECONNECT_MS)
                    return@createCar
                }
                car = readyCar
                val manager = readyCar.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
                if (manager == null) {
                    setStatus("CarPropertyManager kullanılamıyor")
                    handler.postDelayed(reconnectCar, RECONNECT_MS)
                    return@createCar
                }
                reader = VhalPropertyReader(manager) { values, _ ->
                    ble.offer(values)
                    udp.offer(values)
                }
                val failures = reader?.configure(requestedKeys).orEmpty()
                setStatus(
                    if (requestedKeys.isEmpty()) "VHAL hazır · CrowPanel BLE bekleniyor"
                    else "BLE yayın aktif · ${requestedKeys.size - failures.size} sensör"
                )
            }
        }.onFailure { setStatus("VHAL bağlantı hatası: ${it.message}") }
        if (car == null) handler.postDelayed(reconnectCar, RECONNECT_MS)
    }

    private fun setStatus(status: String) {
        BridgeState.save(this, status)
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(status))
    }

    private fun notification(status: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("EX30 VHAL Bridge")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "VHAL yayını", NotificationManager.IMPORTANCE_LOW)
        )
    }

    companion object {
        const val ACTION_STOP = "com.kadireren.ex30vhalbridge.STOP"
        const val ACTION_RECONNECT = "com.kadireren.ex30vhalbridge.RECONNECT"
        private const val CHANNEL_ID = "vhal_bridge"
        private const val NOTIFICATION_ID = 30
        private const val RECONNECT_MS = 2_000L
    }
}
