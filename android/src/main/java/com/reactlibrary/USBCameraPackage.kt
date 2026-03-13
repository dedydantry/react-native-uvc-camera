package com.reactlibrary

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import com.reactlibrary.uvc.UVCCameraViewManager
import com.reactlibrary.uvc.UVCDeviceModule

class USBCameraPackage : ReactPackage {
    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return listOf(UVCCameraViewManager())
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return listOf(UVCDeviceModule(reactContext))
    }
} 