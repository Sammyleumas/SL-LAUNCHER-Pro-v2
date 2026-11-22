package com.sllauncher.pro.device

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Flashlight through Android's torch API (no camera permission needed). */
@Singleton
class TorchController @Inject constructor(@param:ApplicationContext context: Context) {
    private val cameras = context.getSystemService(CameraManager::class.java)
    private val torchId: String? = runCatching {
        cameras.cameraIdList.firstOrNull { id ->
            cameras.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()

    private val _on = MutableStateFlow(false)
    val on: StateFlow<Boolean> = _on.asStateFlow()

    val available: Boolean get() = torchId != null

    init {
        if (torchId != null) {
            cameras.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    if (cameraId == torchId) _on.value = enabled
                }
            }, Handler(Looper.getMainLooper()))
        }
    }

    /** Returns false when there is no flash or another app is using the camera. */
    fun set(enabled: Boolean): Boolean {
        val id = torchId ?: return false
        return try {
            cameras.setTorchMode(id, enabled)
            true
        } catch (e: Exception) {
            Log.w("Torch", "Torch unavailable", e)
            false
        }
    }
}
