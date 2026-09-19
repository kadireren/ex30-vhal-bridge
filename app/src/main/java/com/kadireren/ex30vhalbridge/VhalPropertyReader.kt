package com.kadireren.ex30vhalbridge

import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

class VhalPropertyReader(
    private val manager: CarPropertyManager,
    private val onValues: (Map<String, Double>, Long) -> Unit,
) {
    private val activePropertyIds = mutableSetOf<Int>()
    private val latest = ConcurrentHashMap<Int, Any>()

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            latest[value.propertyId] = value.value ?: return
            val changed = decode(value.propertyId, value.value ?: return)
            if (changed.isNotEmpty()) onValues(changed, SystemClock.elapsedRealtime())
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) = Unit
    }

    @Suppress("DEPRECATION")
    @Synchronized
    fun configure(keys: Set<String>): Set<String> {
        manager.unregisterCallback(callback)
        activePropertyIds.clear()
        val configs = runCatching { manager.propertyList.associateBy { it.propertyId } }.getOrDefault(emptyMap())
        val failures = mutableSetOf<String>()
        for (key in keys) {
            val definition = VhalCatalog.forKey(key) ?: continue
            for (propertyId in definition.propertyIds) {
                if (!activePropertyIds.add(propertyId)) continue
                val config = configs[propertyId]
                if (configs.isNotEmpty() && config == null) {
                    failures += key
                    continue
                }
                val rate = when {
                    definition.requestedHz <= 0f -> CarPropertyManager.SENSOR_RATE_ONCHANGE
                    config != null && config.maxSampleRate <= 0f -> CarPropertyManager.SENSOR_RATE_ONCHANGE
                    config == null -> definition.requestedHz
                    else -> definition.requestedHz.coerceIn(config.minSampleRate, config.maxSampleRate)
                }
                if (runCatching { manager.registerCallback(callback, propertyId, rate) }.isFailure) failures += key
            }
        }
        return failures
    }

    @Suppress("DEPRECATION")
    fun stop() {
        runCatching { manager.unregisterCallback(callback) }
        activePropertyIds.clear()
        latest.clear()
    }

    private fun decode(propertyId: Int, raw: Any): Map<String, Double> {
        val number = (raw as? Number)?.toDouble()
        val direct = when (propertyId) {
            android.car.VehiclePropertyIds.PERF_VEHICLE_SPEED_DISPLAY -> number?.let { mapOf("speed" to it * 3.6) }
            android.car.VehiclePropertyIds.PERF_VEHICLE_SPEED -> number?.let { mapOf("perf_speed" to it * 3.6) }
            android.car.VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE -> number?.let { mapOf("power" to it / 1_000_000.0) }
            android.car.VehiclePropertyIds.RANGE_REMAINING -> number?.let { mapOf("range" to it / 1000.0) }
            android.car.VehiclePropertyIds.GEAR_SELECTION -> number?.let { mapOf("gear" to it) }
            android.car.VehiclePropertyIds.CURRENT_GEAR -> number?.let { mapOf("current_gear" to it) }
            android.car.VehiclePropertyIds.IGNITION_STATE -> number?.let { mapOf("ignition" to it) }
            android.car.VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE -> number?.let { mapOf("outside_temp" to it) }
            android.car.VehiclePropertyIds.PARKING_BRAKE_ON -> mapOf("parking_brake" to if (raw == true) 1.0 else 0.0)
            android.car.VehiclePropertyIds.NIGHT_MODE -> mapOf("night_mode" to if (raw == true) 1.0 else 0.0)
            android.car.VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED -> mapOf("charge_port" to if (raw == true) 1.0 else 0.0)
            android.car.VehiclePropertyIds.EV_BATTERY_LEVEL -> number?.let { mapOf("battery_energy" to it) }
            android.car.VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY -> number?.let { mapOf("battery_capacity" to it) }
            else -> null
        }.orEmpty().toMutableMap()

        val energy = (latest[android.car.VehiclePropertyIds.EV_BATTERY_LEVEL] as? Number)?.toDouble()
        val capacity = (latest[android.car.VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY] as? Number)?.toDouble()
        if (energy != null && capacity != null && capacity > 0.0 &&
            propertyId in setOf(android.car.VehiclePropertyIds.EV_BATTERY_LEVEL, android.car.VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY)) {
            direct["soc"] = (energy / capacity * 100.0).coerceIn(0.0, 100.0)
        }
        return direct
    }
}
