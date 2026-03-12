package com.reactlibrary.uvc

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.widget.Toast
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.usb.USBMonitor

class UVCDeviceModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private var mCameraClient: MultiCameraClient? = null
    private var pendingPermissionPromise: Promise? = null
    private var pendingPermissionDeviceId: Int? = null

    companion object {
        private val deviceCtrlBlockMap = mutableMapOf<Int, USBMonitor.UsbControlBlock>()

        fun getCtrlBlock(deviceId: Int): USBMonitor.UsbControlBlock? {
            return deviceCtrlBlockMap[deviceId]
        }
    }

    init {
        initMultiCamera()
    }

    override fun getName() = "UVCDeviceModule"

    private fun initMultiCamera() {
        mCameraClient = MultiCameraClient(reactApplicationContext, object : IDeviceConnectCallBack {
            override fun onAttachDev(device: UsbDevice?) {
                device?.let {
                    val params = Arguments.createMap().apply {
                        putInt("deviceId", it.deviceId)
                        putString("deviceName", it.deviceName)
                        putInt("productId", it.productId)
                        putInt("vendorId", it.vendorId)
                    }
                    sendEvent("onDeviceAttached", params)
                }
            }

            override fun onDetachDec(device: UsbDevice?) {
                device?.let {
                    val params = Arguments.createMap().apply {
                        putInt("deviceId", it.deviceId)
                    }
                    sendEvent("onDeviceDetached", params)
                }
            }

            override fun onConnectDev(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                device?.let {
                    ctrlBlock?.let { block ->
                        deviceCtrlBlockMap[device.deviceId] = block
                    }

                    if (device.deviceId == pendingPermissionDeviceId) {
                        pendingPermissionPromise?.resolve(true)
                        pendingPermissionPromise = null
                        pendingPermissionDeviceId = null
                    }

                    val params = Arguments.createMap().apply {
                        putInt("deviceId", it.deviceId)
                    }
                    sendEvent("onDeviceConnected", params)
                }
            }

            override fun onDisConnectDec(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                device?.let {
                    deviceCtrlBlockMap.remove(device.deviceId)

                    val params = Arguments.createMap().apply {
                        putInt("deviceId", it.deviceId)
                    }
                    sendEvent("onDeviceDisconnected", params)
                }
            }

            override fun onCancelDev(device: UsbDevice?) {
                device?.let {
                    if (device.deviceId == pendingPermissionDeviceId) {
                        pendingPermissionPromise?.resolve(false)
                        pendingPermissionPromise = null
                        pendingPermissionDeviceId = null
                    }

                    val params = Arguments.createMap().apply {
                        putInt("deviceId", it.deviceId)
                    }
                    sendEvent("onDevicePermissionDenied", params)
                }
            }
        })
        mCameraClient?.register()
    }

    // ======================== Helper ========================

    private fun getCameraOrReject(deviceId: Int, promise: Promise): UVCCameraView? {
        val camera = UVCCameraView.getCamera(deviceId)
        if (camera == null) {
            promise.reject("CAMERA_NOT_FOUND", "No active camera for deviceId: $deviceId")
        }
        return camera
    }

    // ======================== Device Management ========================

    @ReactMethod
    fun getDeviceList(promise: Promise) {
        try {
            val usbManager = reactApplicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
            val devices = usbManager.deviceList.values.filter {
                when (it.deviceClass) {
                    UsbConstants.USB_CLASS_VIDEO -> true
                    UsbConstants.USB_CLASS_MISC -> {
                        var hasVideoInterface = false
                        for (i in 0 until it.interfaceCount) {
                            val intf = it.getInterface(i)
                            if (intf.interfaceClass == UsbConstants.USB_CLASS_VIDEO) {
                                hasVideoInterface = true
                                break
                            }
                        }
                        hasVideoInterface
                    }
                    else -> false
                }
            }

            val deviceArray = Arguments.createArray()
            devices.forEach { device ->
                val deviceInfo = Arguments.createMap().apply {
                    putInt("deviceId", device.deviceId)
                    putString("deviceName", device.deviceName)
                    putInt("productId", device.productId)
                    putInt("vendorId", device.vendorId)
                }
                deviceArray.pushMap(deviceInfo)
            }
            promise.resolve(deviceArray)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun requestPermission(deviceId: Int, promise: Promise) {
        try {
            val usbManager = reactApplicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
            val device = usbManager.deviceList.values.find { it.deviceId == deviceId }

            if (device == null) {
                promise.reject("ERROR", "Device not found")
                return
            }

            pendingPermissionPromise = promise
            pendingPermissionDeviceId = deviceId

            mCameraClient?.requestPermission(device)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun hasPermission(deviceId: Int, promise: Promise) {
        try {
            val usbManager = reactApplicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
            val device = usbManager.deviceList.values.find { it.deviceId == deviceId }

            if (device == null) {
                promise.reject("ERROR", "Device not found")
                return
            }

            promise.resolve(usbManager.hasPermission(device))
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    // ======================== Camera Operations ========================

    @ReactMethod
    fun captureImage(deviceId: Int, savePath: String?, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.takePicture(object : ICaptureCallBack {
                override fun onBegin() {}
                override fun onError(error: String?) {
                    promise.reject("CAPTURE_ERROR", error ?: "Unknown error")
                }
                override fun onComplete(path: String?) {
                    promise.resolve(path)
                }
            }, savePath)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun captureVideoStart(deviceId: Int, savePath: String?, durationInSec: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.startVideoRecording(object : ICaptureCallBack {
                override fun onBegin() {}
                override fun onError(error: String?) {
                    sendEvent("onCaptureVideoError", Arguments.createMap().apply {
                        putInt("deviceId", deviceId)
                        putString("error", error)
                    })
                }
                override fun onComplete(path: String?) {
                    sendEvent("onCaptureVideoComplete", Arguments.createMap().apply {
                        putInt("deviceId", deviceId)
                        putString("path", path)
                    })
                }
            }, savePath, durationInSec.toLong())
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun captureVideoStop(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.stopVideoRecording()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun captureAudioStart(deviceId: Int, savePath: String?, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.startAudioRecording(object : ICaptureCallBack {
                override fun onBegin() {}
                override fun onError(error: String?) {
                    sendEvent("onCaptureAudioError", Arguments.createMap().apply {
                        putInt("deviceId", deviceId)
                        putString("error", error)
                    })
                }
                override fun onComplete(path: String?) {
                    sendEvent("onCaptureAudioComplete", Arguments.createMap().apply {
                        putInt("deviceId", deviceId)
                        putString("path", path)
                    })
                }
            }, savePath)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun captureAudioStop(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.stopAudioRecording()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun isCameraOpened(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            promise.resolve(camera.checkCameraOpened())
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun closeCamera(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doCloseCamera()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun updateResolution(deviceId: Int, width: Int, height: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doUpdateResolution(width, height)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun getAllPreviewSizes(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            val sizes = camera.doGetAllPreviewSizes()
            if (sizes == null) {
                promise.resolve(Arguments.createArray())
                return
            }
            val result = Arguments.createArray()
            sizes.forEach { size ->
                val sizeMap = Arguments.createMap().apply {
                    putInt("width", size.width)
                    putInt("height", size.height)
                }
                result.pushMap(sizeMap)
            }
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun getCurrentPreviewSize(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            val size = camera.doGetCurrentPreviewSize()
            if (size == null) {
                promise.resolve(null)
                return
            }
            val result = Arguments.createMap().apply {
                putInt("width", size.width)
                putInt("height", size.height)
            }
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun setRotateType(deviceId: Int, angle: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            val rotateType = when (angle) {
                0 -> RotateType.ANGLE_0
                90 -> RotateType.ANGLE_90
                180 -> RotateType.ANGLE_180
                270 -> RotateType.ANGLE_270
                else -> {
                    promise.reject("ERROR", "Invalid angle: $angle. Use 0, 90, 180, or 270.")
                    return
                }
            }
            camera.doSetRotateType(rotateType)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun sendCameraCommand(deviceId: Int, command: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doSendCameraCommand(command)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    // ======================== Streaming ========================

    @ReactMethod
    fun captureStreamStart(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doStartCaptureStream()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun captureStreamStop(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doStopCaptureStream()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun startPlayMic(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doStartPlayMic()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun stopPlayMic(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doStopPlayMic()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    // ======================== Auto Focus ========================

    @ReactMethod
    fun setAutoFocus(deviceId: Int, focus: Boolean, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doSetAutoFocus(focus)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun getAutoFocus(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            promise.resolve(camera.doGetAutoFocus() ?: false)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun resetAutoFocus(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doResetAutoFocus()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    // ======================== Brightness ========================

    @ReactMethod
    fun setBrightness(deviceId: Int, value: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doSetBrightness(value)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun getBrightness(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            val value = camera.doGetBrightness()
            promise.resolve(value)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun resetBrightness(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doResetBrightness()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    // ======================== Contrast ========================

    @ReactMethod
    fun setContrast(deviceId: Int, value: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doSetContrast(value)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun getContrast(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            val value = camera.doGetContrast()
            promise.resolve(value)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun resetContrast(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doResetContrast()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    // ======================== Gain ========================

    @ReactMethod
    fun setGain(deviceId: Int, value: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doSetGain(value)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun getGain(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            val value = camera.doGetGain()
            promise.resolve(value)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun resetGain(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doResetGain()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    // ======================== Gamma ========================

    @ReactMethod
    fun setGamma(deviceId: Int, value: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doSetGamma(value)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun getGamma(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            val value = camera.doGetGamma()
            promise.resolve(value)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun resetGamma(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doResetGamma()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    // ======================== Hue ========================

    @ReactMethod
    fun setHue(deviceId: Int, value: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doSetHue(value)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun getHue(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            val value = camera.doGetHue()
            promise.resolve(value)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun resetHue(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doResetHue()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    // ======================== Zoom ========================

    @ReactMethod
    fun setZoom(deviceId: Int, value: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doSetZoom(value)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun getZoom(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            val value = camera.doGetZoom()
            promise.resolve(value)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun resetZoom(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doResetZoom()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    // ======================== Sharpness ========================

    @ReactMethod
    fun setSharpness(deviceId: Int, value: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doSetSharpness(value)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun getSharpness(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            val value = camera.doGetSharpness()
            promise.resolve(value)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun resetSharpness(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doResetSharpness()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    // ======================== Saturation ========================

    @ReactMethod
    fun setSaturation(deviceId: Int, value: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doSetSaturation(value)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun getSaturation(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            val value = camera.doGetSaturation()
            promise.resolve(value)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun resetSaturation(deviceId: Int, promise: Promise) {
        try {
            val camera = getCameraOrReject(deviceId, promise) ?: return
            camera.doResetSaturation()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    // ======================== Events ========================

    private fun sendEvent(eventName: String, params: WritableMap?) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    @ReactMethod
    fun addListener(eventName: String) {
        // Keep: Required for RN built in Event Emitter Calls
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        // Keep: Required for RN built in Event Emitter Calls
    }

    override fun invalidate() {
        super.invalidate()
        mCameraClient?.unRegister()
        mCameraClient?.destroy()
        mCameraClient = null
        pendingPermissionPromise = null
        pendingPermissionDeviceId = null
    }
}
