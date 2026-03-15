package com.reactlibrary.uvc

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.camera.bean.PreviewSize
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio

class UVCCameraView : CameraFragment() {
    private lateinit var mCameraContainer: FrameLayout
    private var currentDeviceId: Int? = null

    companion object {
        private val activeCameras = mutableMapOf<Int, UVCCameraView>()

        fun getCamera(deviceId: Int): UVCCameraView? {
            return activeCameras[deviceId]
        }

        fun getFirstCamera(): UVCCameraView? {
            return activeCameras.values.firstOrNull()
        }

        fun getAllCameras(): Map<Int, UVCCameraView> {
            return activeCameras.toMap()
        }
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View? {
        mCameraContainer = FrameLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return mCameraContainer
    }

    override fun getGravity(): Int = Gravity.CENTER

    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        when (code) {
            ICameraStateCallBack.State.OPENED -> handleCameraOpened()
            ICameraStateCallBack.State.CLOSED -> handleCameraClosed()
            ICameraStateCallBack.State.ERROR -> handleCameraError(msg)
        }
    }

    private fun handleCameraError(msg: String?) {}

    private fun handleCameraClosed() {}

    private fun handleCameraOpened() {}

    override fun getCameraView(): IAspectRatio? {
        return AspectRatioTextureView(requireContext())
    }

    override fun getCameraViewContainer(): ViewGroup? {
        return mCameraContainer
    }

    @SuppressLint("ServiceCast")
    private fun getUsbDeviceList(): List<UsbDevice>? {
        val usbManager = requireContext().getSystemService(Context.USB_SERVICE) as? UsbManager
        return usbManager?.deviceList?.values?.toList()
    }

    fun setDeviceId(deviceId: Int?) {
        Log.d("UVCCameraView", "setDeviceId: $deviceId (current: $currentDeviceId)")
        // Same device — nothing to do
        if (currentDeviceId == deviceId) return

        // Unregister old deviceId
        currentDeviceId?.let { activeCameras.remove(it) }
        currentDeviceId = deviceId

        // Register new deviceId
        deviceId?.let { activeCameras[it] = this }
        Log.d("UVCCameraView", "activeCameras keys: ${activeCameras.keys}")

        var isSet = false
        getUsbDeviceList()?.forEach { device ->
            if (device.deviceId == deviceId) {
                isSet = true
                setDevice(device)
            }
        }
        if (!isSet) {
            Log.w("UVCCameraView", "setDeviceId: USB device not found in system device list")
            setDevice(null)
        }
    }

    override fun getDefaultCamera(): UsbDevice? {
        return this.getUsbDeviceList()?.find { it.deviceId == currentDeviceId }
    }

    override fun onDestroyView() {
        currentDeviceId?.let { activeCameras.remove(it) }
        super.onDestroyView()
    }

    // ======================== Public API methods ========================

    /** Capture image */
    fun takePicture(callBack: ICaptureCallBack, savePath: String? = null) {
        captureImage(callBack, savePath)
    }

    /** Start video recording */
    fun startVideoRecording(callBack: ICaptureCallBack, path: String? = null, durationInSec: Long = 0L) {
        captureVideoStart(callBack, path, durationInSec)
    }

    /** Stop video recording */
    fun stopVideoRecording() {
        captureVideoStop()
    }

    /** Start audio recording */
    fun startAudioRecording(callBack: ICaptureCallBack, path: String? = null) {
        captureAudioStart(callBack, path)
    }

    /** Stop audio recording */
    fun stopAudioRecording() {
        captureAudioStop()
    }

    /** Check if camera is opened */
    fun checkCameraOpened(): Boolean {
        return isCameraOpened()
    }

    /** Close the camera */
    fun doCloseCamera() {
        closeCamera()
    }

    /** Update preview resolution */
    fun doUpdateResolution(width: Int, height: Int) {
        updateResolution(width, height)
    }

    /** Get all supported preview sizes */
    fun doGetAllPreviewSizes(aspectRatio: Double? = null): List<PreviewSize>? {
        return getAllPreviewSizes(aspectRatio)
    }

    /** Get current preview size */
    fun doGetCurrentPreviewSize(): PreviewSize? {
        return getCurrentPreviewSize()
    }

    /** Set rotation type */
    fun doSetRotateType(type: RotateType) {
        setRotateType(type)
    }

    /** Start capture stream (H264 & AAC) */
    fun doStartCaptureStream() {
        captureStreamStart()
    }

    /** Stop capture stream */
    fun doStopCaptureStream() {
        captureStreamStop()
    }

    /** Start mic playback */
    fun doStartPlayMic() {
        startPlayMic()
    }

    /** Stop mic playback */
    fun doStopPlayMic() {
        stopPlayMic()
    }

    /** Send camera command */
    fun doSendCameraCommand(command: Int) {
        sendCameraCommand(command)
    }

    // ---- Auto Focus ----
    fun doSetAutoFocus(focus: Boolean) { setAutoFocus(focus) }
    fun doGetAutoFocus(): Boolean? { return getAutoFocus() }
    fun doResetAutoFocus() { resetAutoFocus() }

    // ---- Brightness ----
    fun doSetBrightness(value: Int) { setBrightness(value) }
    fun doGetBrightness(): Int? { return getBrightness() }
    fun doResetBrightness() { resetBrightness() }

    // ---- Contrast ----
    fun doSetContrast(value: Int) { setContrast(value) }
    fun doGetContrast(): Int? { return getContrast() }
    fun doResetContrast() { resetContrast() }

    // ---- Gain ----
    fun doSetGain(value: Int) { setGain(value) }
    fun doGetGain(): Int? { return getGain() }
    fun doResetGain() { resetGain() }

    // ---- Gamma ----
    fun doSetGamma(value: Int) { setGamma(value) }
    fun doGetGamma(): Int? { return getGamma() }
    fun doResetGamma() { resetGamma() }

    // ---- Hue ----
    fun doSetHue(value: Int) { setHue(value) }
    fun doGetHue(): Int? { return getHue() }
    fun doResetHue() { resetHue() }

    // ---- Zoom ----
    fun doSetZoom(value: Int) { setZoom(value) }
    fun doGetZoom(): Int? { return getZoom() }
    fun doResetZoom() { resetZoom() }

    // ---- Sharpness ----
    fun doSetSharpness(value: Int) { setSharpness(value) }
    fun doGetSharpness(): Int? { return getSharpness() }
    fun doResetSharpness() { resetSharpness() }

    // ---- Saturation ----
    fun doSetSaturation(value: Int) { setSaturation(value) }
    fun doGetSaturation(): Int? { return getSaturation() }
    fun doResetSaturation() { resetSaturation() }
}
