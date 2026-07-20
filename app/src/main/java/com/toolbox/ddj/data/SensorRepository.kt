package com.toolbox.ddj.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * 单个传感器的静态信息（免权限，可直接读取）。
 */
data class SensorItem(
    val name: String,
    val typeLabel: String,
    val vendor: String,
    val power: String,
    val resolution: String,
    val maxRange: String
)

/**
 * 传感器 Repository。
 *
 * - [list] 一次性读取设备全部传感器的静态信息。
 * - [liveReadings] 通过 callbackFlow 订阅主要传感器的实时读数，随 collect 生命周期自动注册 / 注销，
 *   避免监听泄漏与无谓耗电。
 */
class SensorRepository(private val context: Context) {

    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /** 读取全部传感器的静态信息列表。 */
    fun list(): List<SensorItem> =
        sm.getSensorList(Sensor.TYPE_ALL).map { sensor ->
            SensorItem(
                name = sensor.name,
                // stringType 形如 "android.sensor.accelerometer"，去掉前缀更易读；私有类型可能为 null，回退到 type 值
                typeLabel = sensor.stringType?.removePrefix("android.sensor.") ?: sensor.type.toString(),
                vendor = sensor.vendor,
                power = "${sensor.power} mA",
                resolution = sensor.resolution.toString(),
                maxRange = sensor.maximumRange.toString()
            )
        }

    /**
     * 订阅主要传感器的实时读数。
     *
     * 每当任一传感器数据变化，就发射一份「传感器中文名 -> 格式化数值」的最新快照 Map。
     * collect 时注册监听，取消时 [awaitClose] 自动注销。
     */
    fun liveReadings(): Flow<Map<String, String>> = callbackFlow {
        // 用一个可变 map 累积各传感器的最新值，onSensorChanged 每次只更新其中一项
        val latest = mutableMapOf<String, String>()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val v = event.values
                when (event.sensor.type) {
                    // 三轴传感器：格式化为 "x, y, z"
                    Sensor.TYPE_ACCELEROMETER -> latest["加速度"] = formatTriple(v)
                    Sensor.TYPE_GYROSCOPE -> latest["陀螺仪"] = formatTriple(v)
                    Sensor.TYPE_MAGNETIC_FIELD -> latest["磁场"] = formatTriple(v)
                    // 单值传感器：直接格式化
                    Sensor.TYPE_LIGHT -> latest["光线"] = "%.1f lx".format(v[0])
                    Sensor.TYPE_PROXIMITY -> latest["距离"] = "%.1f cm".format(v[0])
                }
                // 发射不可变副本，避免下游读取到后续被修改的同一实例
                trySend(latest.toMap())
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // 精度变化无需处理
            }
        }

        // 主要传感器：getDefaultSensor 存在才注册（部分设备缺失某些传感器）
        val types = intArrayOf(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PROXIMITY
        )
        types.forEach { type ->
            sm.getDefaultSensor(type)?.let { sensor ->
                sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            }
        }

        awaitClose { sm.unregisterListener(listener) }
    }

    /** 三轴数据格式化为 "x, y, z"，各保留两位小数。 */
    private fun formatTriple(v: FloatArray): String =
        "%.2f, %.2f, %.2f".format(v[0], v[1], v[2])
}
