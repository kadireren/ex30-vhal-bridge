package com.kadireren.ex30vhalbridge

import android.Manifest
import android.app.Activity
import android.car.Car
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private val refresh = object : Runnable {
        override fun run() {
            status.text = BridgeState.read(this@MainActivity)
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        ensurePermissions()
    }

    override fun onStart() {
        super.onStart()
        handler.post(refresh)
    }

    override fun onStop() {
        handler.removeCallbacks(refresh)
        super.onStop()
    }

    private fun startBridge() {
        startForegroundService(Intent(this, VhalBridgeService::class.java))
    }

    private fun reconnectBridge() {
        startForegroundService(Intent(this, VhalBridgeService::class.java).setAction(VhalBridgeService.ACTION_RECONNECT))
    }

    private fun requiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Car.PERMISSION_SPEED,
            Car.PERMISSION_ENERGY,
            Car.PERMISSION_POWERTRAIN,
            Car.PERMISSION_CAR_INFO,
            Car.PERMISSION_ENERGY_PORTS,
            Car.PERMISSION_EXTERIOR_ENVIRONMENT,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_SCAN
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        return permissions.toTypedArray()
    }

    private fun hasBluetoothPermissions(): Boolean {
        val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return required.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun ensurePermissions() {
        if (hasBluetoothPermissions()) startBridge()
        val missing = requiredPermissions().filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 30)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 30) return
        if (hasBluetoothPermissions()) {
            startBridge()
            reconnectBridge()
        } else {
            BridgeState.save(this, "Bluetooth izni gerekli")
        }
    }

    private fun buildContent(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(56), dp(36), dp(56), dp(36))
        setBackgroundColor(Color.BLACK)
        addView(TextView(this@MainActivity).apply {
            text = "EX30 VHAL BRIDGE"
            textSize = 34f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }, layout(-1, 0, 1f))
        status = TextView(this@MainActivity).apply {
            text = "Başlatılıyor"
            textSize = 24f
            setTextColor(Color.rgb(214, 168, 74))
            gravity = Gravity.CENTER
        }
        addView(status, layout(-1, 0, 1f))
        addView(TextView(this@MainActivity).apply {
            text = "Yayın arka planda sürer; ekranı kapatabilirsiniz.\nCrowPanel hangi sensörleri isterse yalnız onlar VHAL'den okunur."
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }, layout(-1, 0, 1f))
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(Button(this@MainActivity).apply {
                text = "YENİDEN BAŞLAT"
                setOnClickListener {
                    stopService(Intent(this@MainActivity, VhalBridgeService::class.java))
                    handler.postDelayed(::startBridge, 500)
                }
            }, layout(dp(240), dp(64), 0f).apply { marginEnd = dp(20) })
            addView(Button(this@MainActivity).apply {
                text = "DURDUR"
                setOnClickListener {
                    startForegroundService(Intent(this@MainActivity, VhalBridgeService::class.java).setAction(VhalBridgeService.ACTION_STOP))
                }
            }, layout(dp(180), dp(64), 0f))
        }, layout(-1, dp(90), 0f))
    }

    private fun layout(width: Int, height: Int, weight: Float) = LinearLayout.LayoutParams(width, height, weight)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
