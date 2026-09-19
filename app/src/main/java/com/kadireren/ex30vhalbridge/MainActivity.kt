package com.kadireren.ex30vhalbridge

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
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
        startBridge()
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
            text = "Uygulama açıldığında yayın otomatik başlar.\nCrowPanel hangi sensörleri isterse yalnız onlar VHAL'den okunur."
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
                    startService(Intent(this@MainActivity, VhalBridgeService::class.java).setAction(VhalBridgeService.ACTION_STOP))
                }
            }, layout(dp(180), dp(64), 0f))
        }, layout(-1, dp(90), 0f))
    }

    private fun layout(width: Int, height: Int, weight: Float) = LinearLayout.LayoutParams(width, height, weight)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
