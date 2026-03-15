import {
  Text,
  View,
  findNodeHandle,
  UIManager,
  StyleSheet,
  ViewStyle,
} from "react-native";
import React, {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
} from "react";
import { UVCDeviceModule, PreviewSize, RotateAngle } from "./uvc_device_module";
import { TaskQueue, useDevices, useDeviceEvent } from "./help";
import { UVCCameraView, ViewCommands } from "./native_view";

const isDev = __DEV__;

const taskQueue = new TaskQueue();

/** Handle exposed by SingleUVCCamera ref */
export interface SingleUVCCameraHandle {
  /** Get the currently connected deviceId, or null if none */
  getDeviceId: () => number | null;

  /** Capture a photo. Returns saved file path. */
  captureImage: (savePath?: string | null) => Promise<string>;

  /** Start video recording */
  captureVideoStart: (
    savePath?: string | null,
    durationInSec?: number
  ) => Promise<boolean>;

  /** Stop video recording */
  captureVideoStop: () => Promise<boolean>;

  /** Start audio recording */
  captureAudioStart: (savePath?: string | null) => Promise<boolean>;

  /** Stop audio recording */
  captureAudioStop: () => Promise<boolean>;

  /** Check if camera is opened */
  isCameraOpened: () => Promise<boolean>;

  /** Close camera */
  closeCamera: () => Promise<boolean>;

  /** Update preview resolution */
  updateResolution: (width: number, height: number) => Promise<boolean>;

  /** Get all supported preview sizes */
  getAllPreviewSizes: () => Promise<PreviewSize[]>;

  /** Get current preview size */
  getCurrentPreviewSize: () => Promise<PreviewSize | null>;

  /** Set rotation (0, 90, 180, 270) */
  setRotateType: (angle: RotateAngle) => Promise<boolean>;

  /** Send raw camera command */
  sendCameraCommand: (command: number) => Promise<boolean>;

  // Streaming
  captureStreamStart: () => Promise<boolean>;
  captureStreamStop: () => Promise<boolean>;
  startPlayMic: () => Promise<boolean>;
  stopPlayMic: () => Promise<boolean>;

  // Camera parameter controls
  setAutoFocus: (focus: boolean) => Promise<boolean>;
  getAutoFocus: () => Promise<boolean>;
  resetAutoFocus: () => Promise<boolean>;

  setBrightness: (value: number) => Promise<boolean>;
  getBrightness: () => Promise<number | null>;
  resetBrightness: () => Promise<boolean>;

  setContrast: (value: number) => Promise<boolean>;
  getContrast: () => Promise<number | null>;
  resetContrast: () => Promise<boolean>;

  setGain: (value: number) => Promise<boolean>;
  getGain: () => Promise<number | null>;
  resetGain: () => Promise<boolean>;

  setGamma: (value: number) => Promise<boolean>;
  getGamma: () => Promise<number | null>;
  resetGamma: () => Promise<boolean>;

  setHue: (value: number) => Promise<boolean>;
  getHue: () => Promise<number | null>;
  resetHue: () => Promise<boolean>;

  setZoom: (value: number) => Promise<boolean>;
  getZoom: () => Promise<number | null>;
  resetZoom: () => Promise<boolean>;

  setSharpness: (value: number) => Promise<boolean>;
  getSharpness: () => Promise<number | null>;
  resetSharpness: () => Promise<boolean>;

  setSaturation: (value: number) => Promise<boolean>;
  getSaturation: () => Promise<number | null>;
  resetSaturation: () => Promise<boolean>;
}

export interface SingleUVCCameraProps {
  style?: ViewStyle;
  /** Show debug info overlay */
  debug?: boolean;
  /** Text to show while waiting for a camera */
  placeholder?: string;
  /** Called when a device is connected and camera opens */
  onCameraReady?: (deviceId: number) => void;
  /** Called when the device is disconnected */
  onCameraDisconnected?: () => void;
}

/**
 * SingleUVCCamera - Automatically connects to the first detected UVC camera.
 * Exposes all camera controls via ref.
 *
 * Usage:
 * ```tsx
 * const cameraRef = useRef<SingleUVCCameraHandle>(null);
 *
 * <SingleUVCCamera ref={cameraRef} style={{ flex: 1 }} />
 *
 * // Take photo
 * const path = await cameraRef.current?.captureImage();
 * ```
 */
const SingleUVCCamera = forwardRef<SingleUVCCameraHandle, SingleUVCCameraProps>(
  (
    {
      style,
      debug = isDev,
      placeholder = "Waiting for camera...",
      onCameraReady,
      onCameraDisconnected,
    },
    ref
  ) => {
    const { devices } = useDevices();
    const firstDevice = devices[0] ?? null;
    const deviceId = firstDevice?.deviceId ?? null;

    const viewRef = useRef<View>(null);
    const cameraViewRef = useRef(null);
    const [connected, setConnected] = useState(false);
    const permissionDeniedRef = useRef(false);
    const retryCountRef = useRef(0);
    const connectingRef = useRef(false);
    const MAX_RETRIES = 2;

    // Guard helper for ref methods
    const requireDeviceId = (): number => {
      if (deviceId == null) {
        throw new Error("No UVC camera connected");
      }
      return deviceId;
    };

    // Expose imperative API via ref
    useImperativeHandle(
      ref,
      () => ({
        getDeviceId: () => deviceId,

        // Camera operations
        captureImage: (savePath) =>
          UVCDeviceModule.captureImage(requireDeviceId(), savePath),
        captureVideoStart: (savePath, durationInSec = 0) =>
          UVCDeviceModule.captureVideoStart(
            requireDeviceId(),
            savePath,
            durationInSec
          ),
        captureVideoStop: () =>
          UVCDeviceModule.captureVideoStop(requireDeviceId()),
        captureAudioStart: (savePath) =>
          UVCDeviceModule.captureAudioStart(requireDeviceId(), savePath),
        captureAudioStop: () =>
          UVCDeviceModule.captureAudioStop(requireDeviceId()),
        isCameraOpened: () =>
          UVCDeviceModule.isCameraOpened(requireDeviceId()),
        closeCamera: () => UVCDeviceModule.closeCamera(requireDeviceId()),
        updateResolution: (width, height) =>
          UVCDeviceModule.updateResolution(requireDeviceId(), width, height),
        getAllPreviewSizes: () =>
          UVCDeviceModule.getAllPreviewSizes(requireDeviceId()),
        getCurrentPreviewSize: () =>
          UVCDeviceModule.getCurrentPreviewSize(requireDeviceId()),
        setRotateType: (angle) =>
          UVCDeviceModule.setRotateType(requireDeviceId(), angle),
        sendCameraCommand: (command) =>
          UVCDeviceModule.sendCameraCommand(requireDeviceId(), command),

        // Streaming
        captureStreamStart: () =>
          UVCDeviceModule.captureStreamStart(requireDeviceId()),
        captureStreamStop: () =>
          UVCDeviceModule.captureStreamStop(requireDeviceId()),
        startPlayMic: () => UVCDeviceModule.startPlayMic(requireDeviceId()),
        stopPlayMic: () => UVCDeviceModule.stopPlayMic(requireDeviceId()),

        // Auto Focus
        setAutoFocus: (focus) =>
          UVCDeviceModule.setAutoFocus(requireDeviceId(), focus),
        getAutoFocus: () => UVCDeviceModule.getAutoFocus(requireDeviceId()),
        resetAutoFocus: () =>
          UVCDeviceModule.resetAutoFocus(requireDeviceId()),

        // Brightness
        setBrightness: (value) =>
          UVCDeviceModule.setBrightness(requireDeviceId(), value),
        getBrightness: () => UVCDeviceModule.getBrightness(requireDeviceId()),
        resetBrightness: () =>
          UVCDeviceModule.resetBrightness(requireDeviceId()),

        // Contrast
        setContrast: (value) =>
          UVCDeviceModule.setContrast(requireDeviceId(), value),
        getContrast: () => UVCDeviceModule.getContrast(requireDeviceId()),
        resetContrast: () =>
          UVCDeviceModule.resetContrast(requireDeviceId()),

        // Gain
        setGain: (value) =>
          UVCDeviceModule.setGain(requireDeviceId(), value),
        getGain: () => UVCDeviceModule.getGain(requireDeviceId()),
        resetGain: () => UVCDeviceModule.resetGain(requireDeviceId()),

        // Gamma
        setGamma: (value) =>
          UVCDeviceModule.setGamma(requireDeviceId(), value),
        getGamma: () => UVCDeviceModule.getGamma(requireDeviceId()),
        resetGamma: () => UVCDeviceModule.resetGamma(requireDeviceId()),

        // Hue
        setHue: (value) =>
          UVCDeviceModule.setHue(requireDeviceId(), value),
        getHue: () => UVCDeviceModule.getHue(requireDeviceId()),
        resetHue: () => UVCDeviceModule.resetHue(requireDeviceId()),

        // Zoom
        setZoom: (value) =>
          UVCDeviceModule.setZoom(requireDeviceId(), value),
        getZoom: () => UVCDeviceModule.getZoom(requireDeviceId()),
        resetZoom: () => UVCDeviceModule.resetZoom(requireDeviceId()),

        // Sharpness
        setSharpness: (value) =>
          UVCDeviceModule.setSharpness(requireDeviceId(), value),
        getSharpness: () => UVCDeviceModule.getSharpness(requireDeviceId()),
        resetSharpness: () =>
          UVCDeviceModule.resetSharpness(requireDeviceId()),

        // Saturation
        setSaturation: (value) =>
          UVCDeviceModule.setSaturation(requireDeviceId(), value),
        getSaturation: () =>
          UVCDeviceModule.getSaturation(requireDeviceId()),
        resetSaturation: () =>
          UVCDeviceModule.resetSaturation(requireDeviceId()),
      }),
      [deviceId]
    );

    // Auto-connect to device
    const doConnect = useCallback(async () => {
      if (deviceId == null) return;

      // Already connected or in progress — skip
      if (connected || connectingRef.current) return;
      
      // Don't retry if permission was already denied for this device
      if (permissionDeniedRef.current) {
        console.log('SingleUVCCamera: Permission was denied, not retrying automatically');
        return;
      }
      
      // Limit retries to prevent infinite loops
      if (retryCountRef.current >= MAX_RETRIES) {
        console.log('SingleUVCCamera: Max retries reached, stopping');
        return;
      }

      connectingRef.current = true;
      retryCountRef.current += 1;
      
      try {
        const isGranted = (await taskQueue.addTask(() =>
          UVCDeviceModule.requestPermission(deviceId)
        )) as boolean;

        if (isGranted) {
          retryCountRef.current = 0;
          permissionDeniedRef.current = false;
          const node = findNodeHandle(cameraViewRef.current);
          if (node) {
            UIManager.dispatchViewManagerCommand(node, ViewCommands.setDeviceId, [
              deviceId,
            ]);
            setConnected(true);
            // Give native side time to process the command before
            // notifying JS that the camera is ready.
            setTimeout(() => {
              onCameraReady?.(deviceId);
            }, 1000);
          }
        } else {
          // Permission denied — stop retrying
          permissionDeniedRef.current = true;
          console.log('SingleUVCCamera: Permission denied by user');
        }
      } catch (error) {
        console.error("SingleUVCCamera: Failed to connect:", error);
      } finally {
        connectingRef.current = false;
      }
    }, [deviceId, connected, onCameraReady]);

    const { state } = useDeviceEvent({
      deviceId: deviceId ?? -1,
      onDisconnected: () => {
        setConnected(false);
        connectingRef.current = false;
        // Reset permission state when device is physically disconnected,
        // so next plug-in will try again
        permissionDeniedRef.current = false;
        retryCountRef.current = 0;
        onCameraDisconnected?.();
      },
      onDetached: () => {
        // Device physically removed — reset everything
        setConnected(false);
        connectingRef.current = false;
        permissionDeniedRef.current = false;
        retryCountRef.current = 0;
      },
    });

    // Reset retry counter when device changes
    useEffect(() => {
      permissionDeniedRef.current = false;
      retryCountRef.current = 0;
    }, [deviceId]);

    useEffect(() => {
      // Only attempt connection on states that make sense.
      // Do NOT retry on "permissionDenied" to avoid infinite loop.
      if (deviceId != null && state !== "permissionDenied" && !permissionDeniedRef.current) {
        doConnect();
      }
    }, [state, doConnect, deviceId]);

    // Track view dimensions via onLayout so the native camera view
    // always has a real size (a 0×0 view won’t create a Surface).
    const [viewSize, setViewSize] = useState({ width: 0, height: 0 });
    const handleLayout = useCallback((e: any) => {
      const { width, height } = e.nativeEvent.layout;
      if (width > 0 && height > 0) {
        setViewSize({ width, height });
      }
    }, []);

    if (deviceId == null) {
      return (
        <View style={[styles.full, style]}>
          <Text style={styles.text}>{placeholder}</Text>
        </View>
      );
    }

    return (
      <View ref={viewRef} onLayout={handleLayout} style={[styles.full, style]}>
        <UVCCameraView
          ref={cameraViewRef}
          // @ts-ignore
          style={StyleSheet.absoluteFill}
        />
        {debug && (
          <>
            <Text style={styles.leftTop}>
              device:{deviceId} | {firstDevice?.deviceName}
            </Text>
            <Text style={styles.rightTop}>
              {connected ? "connected" : state || "..."}
            </Text>
          </>
        )}
      </View>
    );
  }
);

SingleUVCCamera.displayName = "SingleUVCCamera";

const styles = StyleSheet.create({
  full: {
    width: "100%",
    height: "100%",
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    position: "relative",
  },
  text: {
    color: "white",
  },
  leftTop: {
    position: "absolute",
    left: 5,
    top: 5,
    color: "red",
    fontSize: 10,
  },
  rightTop: {
    position: "absolute",
    right: 5,
    top: 5,
    color: "red",
    fontSize: 10,
  },
});

export { SingleUVCCamera };
