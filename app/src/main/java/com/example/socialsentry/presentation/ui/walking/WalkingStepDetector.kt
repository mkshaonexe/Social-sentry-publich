package com.example.socialsentry.presentation.ui.walking

import kotlin.math.sqrt

class WalkingStepDetector {
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var stepCount = 0
    private var lastStepTime = 0L
    private var stepThreshold = 0.5f // Adjust sensitivity
    private var minStepInterval = 200L // Minimum time between steps (ms)
    
    fun onAccelerometerData(values: FloatArray) {
        val currentTime = System.currentTimeMillis()
        val x = values[0]
        val y = values[1]
        val z = values[2]
        
        // Calculate magnitude of acceleration
        val magnitude = sqrt(x * x + y * y + z * z)
        
        // Simple step detection based on acceleration changes
        if (lastX != 0f && lastY != 0f && lastZ != 0f) {
            val deltaX = x - lastX
            val deltaY = y - lastY
            val deltaZ = z - lastZ
            val deltaMagnitude = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
            
            // Detect step if magnitude change is significant and enough time has passed
            if (deltaMagnitude > stepThreshold && 
                currentTime - lastStepTime > minStepInterval) {
                stepCount++
                lastStepTime = currentTime
            }
        }
        
        lastX = x
        lastY = y
        lastZ = z
    }
    
    fun getStepCount(): Int = stepCount
    
    fun reset() {
        stepCount = 0
        lastStepTime = 0L
    }
}
