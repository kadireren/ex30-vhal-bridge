package com.kadireren.ex30vhalbridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.Intent
import android.os.IBinder

class VhalBridgeService : Service() {
    private var car: Car? = null
    private var reader: VhalPropertyReader? = null
    private var requestedKeys: Set<String> = emptySet()
    private lateinit var udp: UdpBridge

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification("Araç bağlantısı kuruluyor"))
        udp = UdpBridge(
            onSubscription = { subscription ->
                if (subscription.keys != requestedKeys) {
                    requestedKeys = subscription.keys
                    val failures = reader?.configure(requestedKeys).orEmpty()
                    setStatus(if (failures.isEmpty()) "Yayın aktif · ${requestedKeys.size} sensör" else "${failures.size} sensör okunamadı")
                }
            },
            onStatus = ::setStatus,
        ).also { it.start() }
        connectCar()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        reader?.stop()
        udp.stop()
        runCatching { car?.disconnect() }
        setStatus("Yayın durduruldu")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun connectCar() {
        runCatching {
            car = Car.createCar(this, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { readyCar, ready ->
                if (!ready) {
                    setStatus("Araç VHAL bağlantısı bekleniyor")
                    return@createCar
                }
                car = readyCar
                val manager = readyCar.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
                if (manager == null) {
                    setStatus("CarPropertyManager kullanılamıyor")
                    return@createCar
                }
                reader = VhalPropertyReader(manager) { values, _ -> udp.offer(values) }
                val failures = reader?.configure(requestedKeys).orEmpty()
                setStatus(if (requestedKeys.isEmpty()) "VHAL hazır · CrowPanel bekleniyor" else "Yayın aktif · ${requestedKeys.size - failures.size} sensör")
            }
        }.onFailure { setStatus("VHAL bağlantı hatası: ${it.message}") }
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
        private const val CHANNEL_ID = "vhal_bridge"
        private const val NOTIFICATION_ID = 30
    }
}
