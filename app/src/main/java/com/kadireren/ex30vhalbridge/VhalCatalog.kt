package com.kadireren.ex30vhalbridge

import android.car.Car
import android.car.VehiclePropertyIds

data class VhalDefinition(
    val key: String,
    val propertyIds: Set<Int>,
    val permission: String,
    val requestedHz: Float,
)

object VhalCatalog {
    val definitions = listOf(
        VhalDefinition("speed", setOf(VehiclePropertyIds.PERF_VEHICLE_SPEED_DISPLAY), Car.PERMISSION_SPEED, 10f),
        VhalDefinition("perf_speed", setOf(VehiclePropertyIds.PERF_VEHICLE_SPEED), Car.PERMISSION_SPEED, 10f),
        VhalDefinition("power", setOf(VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE), Car.PERMISSION_ENERGY, 30f),
        VhalDefinition("soc", setOf(VehiclePropertyIds.EV_BATTERY_LEVEL, VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY), Car.PERMISSION_ENERGY, 0.5f),
        VhalDefinition("range", setOf(VehiclePropertyIds.RANGE_REMAINING), Car.PERMISSION_ENERGY, 0.5f),
        VhalDefinition("gear", setOf(VehiclePropertyIds.GEAR_SELECTION), Car.PERMISSION_POWERTRAIN, 0f),
        VhalDefinition("current_gear", setOf(VehiclePropertyIds.CURRENT_GEAR), Car.PERMISSION_POWERTRAIN, 0f),
        VhalDefinition("ignition", setOf(VehiclePropertyIds.IGNITION_STATE), Car.PERMISSION_POWERTRAIN, 0f),
        VhalDefinition("parking_brake", setOf(VehiclePropertyIds.PARKING_BRAKE_ON), Car.PERMISSION_POWERTRAIN, 0f),
        VhalDefinition("outside_temp", setOf(VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE), Car.PERMISSION_EXTERIOR_ENVIRONMENT, 0.5f),
        VhalDefinition("night_mode", setOf(VehiclePropertyIds.NIGHT_MODE), Car.PERMISSION_EXTERIOR_ENVIRONMENT, 0f),
        VhalDefinition("charge_port", setOf(VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED), Car.PERMISSION_ENERGY_PORTS, 0f),
        VhalDefinition("battery_energy", setOf(VehiclePropertyIds.EV_BATTERY_LEVEL), Car.PERMISSION_ENERGY, 0.5f),
        VhalDefinition("battery_capacity", setOf(VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY), Car.PERMISSION_CAR_INFO, 0f),
    )
    val keys = definitions.mapTo(mutableSetOf()) { it.key }
    fun forKey(key: String) = definitions.firstOrNull { it.key == key }
}
