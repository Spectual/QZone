package com.qzone.util

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt
import com.qzone.util.QLog

class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {

    private var lastUpdate: Long = 0
    private var lastShakeTime: Long = 0
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f
    
    // Increased threshold to reduce sensitivity (was 800)
    private val shakeThreshold = 1500 
    // Cooldown to prevent multiple triggers from a single shake action
    private val minTimeBetweenShakesMs = 1500L 

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val curTime = System.currentTimeMillis()
            // only allow one update every 100ms.
            if ((curTime - lastUpdate) > 100) {
                val diffTime = (curTime - lastUpdate)
                lastUpdate = curTime

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val speed = Math.abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000

                if (speed > shakeThreshold) {
                    if ((curTime - lastShakeTime) > minTimeBetweenShakesMs) {
                        lastShakeTime = curTime
                        QLog.d(TAG) { "Shake detected speed=$speed" }
                        onShake()
                    } else {
                        QLog.d(TAG) { "Shake ignored due to cooldown speed=$speed" }
                    }
                }

                lastX = x
                lastY = y
                lastZ = z
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    companion object {
        private const val TAG = "ShakeDetector"
    }
}

