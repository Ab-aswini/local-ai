package com.example.hybridai.core

import android.app.ActivityManager
import android.content.Context
import android.util.Log

enum class InferenceStrategy {
    CLOUD_ONLY,
    LOCAL_QUANTIZED_4BIT,
    LOCAL_HIGH_PRECISION
}

class SystemHealthMonitor(private val context: Context) {

    companion object {
        private const val TAG = "SystemHealthMonitor"
        // 6GB threshold for low-end devices
        private const val LOW_RAM_THRESHOLD_MB = 6000L 
        // 12GB threshold for high-end devices
        private const val HIGH_RAM_THRESHOLD_MB = 12000L
    }

    /**
     * Profiles RAM to determine the optimal inference strategy.
     */
    fun determineOptimalStrategy(): InferenceStrategy {
        val memoryInfo = getMemoryInfo()
        val totalRamMb = memoryInfo.totalMem / (1024 * 1024)
        val availRamMb = memoryInfo.availMem / (1024 * 1024)

        Log.d(TAG, "Device RAM: Total = ${totalRamMb}MB, Available = ${availRamMb}MB")

        return when {
            totalRamMb < LOW_RAM_THRESHOLD_MB -> {
                Log.i(TAG, "Low RAM detected. Selecting LOCAL_QUANTIZED_4BIT with mmap.")
                InferenceStrategy.LOCAL_QUANTIZED_4BIT
            }
            totalRamMb > HIGH_RAM_THRESHOLD_MB -> {
                Log.i(TAG, "High RAM detected. Selecting LOCAL_HIGH_PRECISION.")
                InferenceStrategy.LOCAL_HIGH_PRECISION
            }
            else -> {
                Log.i(TAG, "Mid-range RAM detected. Defaulting to LOCAL_QUANTIZED_4BIT.")
                InferenceStrategy.LOCAL_QUANTIZED_4BIT
            }
        }
    }

    /**
     * Helper to get system memory information.
     */
    private fun getMemoryInfo(): ActivityManager.MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo
    }
}
