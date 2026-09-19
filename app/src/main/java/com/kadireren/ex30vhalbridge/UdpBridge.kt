package com.kadireren.ex30vhalbridge

import android.os.SystemClock
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class UdpBridge(
    private val onSubscription: (Subscription) -> Unit,
    private val onStatus: (String) -> Unit,
) {
    private val running = AtomicBoolean(false)
    private val executor = Executors.newFixedThreadPool(2)
    private val dirtyValues = ConcurrentHashMap<String, Double>()
    private val sequence = AtomicLong(0)
    private val sessionId = UUID.randomUUID().toString()
    @Volatile private var destination: InetAddress? = null
    @Volatile private var lastSubscriptionMs = 0L

    fun start() {
        if (!running.compareAndSet(false, true)) return
        executor.execute(::receiveLoop)
        executor.execute(::sendLoop)
    }

    fun offer(values: Map<String, Double>) {
        dirtyValues.putAll(values)
    }

    fun stop() {
        running.set(false)
        executor.shutdownNow()
    }

    private fun receiveLoop() {
        DatagramSocket(null).use { socket ->
            socket.reuseAddress = true
            socket.broadcast = true
            socket.soTimeout = 1000
            socket.bind(InetSocketAddress(BridgeProtocol.SUBSCRIPTION_PORT))
            val buffer = ByteArray(4096)
            while (running.get()) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val subscription = BridgeProtocol.parseSubscription(String(packet.data, 0, packet.length, Charsets.UTF_8)) ?: continue
                    destination = packet.address
                    lastSubscriptionMs = SystemClock.elapsedRealtime()
                    onSubscription(subscription)
                    onStatus("CrowPanel bağlı · ${subscription.keys.size} VHAL sensörü")
                } catch (_: SocketTimeoutException) {
                    if (SystemClock.elapsedRealtime() - lastSubscriptionMs > 4_000) onStatus("CrowPanel aboneliği bekleniyor")
                } catch (error: Exception) {
                    if (running.get()) onStatus("UDP dinleme hatası: ${error.message}")
                }
            }
        }
    }

    private fun sendLoop() {
        DatagramSocket().use { socket ->
            while (running.get()) {
                val started = SystemClock.elapsedRealtime()
                val address = destination
                if (address != null && started - lastSubscriptionMs < 4_000 && dirtyValues.isNotEmpty()) {
                    val values = dirtyValues.toMap()
                    values.keys.forEach(dirtyValues::remove)
                    val payload = BridgeProtocol.telemetry(sessionId, sequence.incrementAndGet(), started, values)
                    runCatching { socket.send(DatagramPacket(payload, payload.size, address, BridgeProtocol.TELEMETRY_PORT)) }
                        .onFailure { onStatus("UDP gönderim hatası: ${it.message}") }
                }
                Thread.sleep((34 - (SystemClock.elapsedRealtime() - started)).coerceAtLeast(1))
            }
        }
    }
}
