import {NativeModules, NativeEventEmitter} from 'react-native';

interface UVCDevice {
  deviceId: number;
  deviceName: string;
  productId: number;
  vendorId: number;
}

interface PreviewSize {
  width: number;
  height: number;
}

type RotateAngle = 0 | 90 | 180 | 270;

const eventEmitter = new NativeEventEmitter(NativeModules.UVCDeviceModule);

const UVCDeviceModule = {
  // ======================== Device Management ========================

  getDeviceList: async (): Promise<UVCDevice[]> => {
    return NativeModules.UVCDeviceModule.getDeviceList();
  },

  requestPermission: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.requestPermission(deviceId);
  },

  hasPermission: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.hasPermission(deviceId);
  },

  // ======================== Camera Operations ========================

  /** Capture image. Returns the saved file path. */
  captureImage: async (
    deviceId: number,
    savePath?: string | null,
  ): Promise<string> => {
    return NativeModules.UVCDeviceModule.captureImage(deviceId, savePath ?? null);
  },

  /** Start video recording. Video completion/error emitted via events. */
  captureVideoStart: async (
    deviceId: number,
    savePath?: string | null,
    durationInSec: number = 0,
  ): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.captureVideoStart(
      deviceId,
      savePath ?? null,
      durationInSec,
    );
  },

  /** Stop video recording */
  captureVideoStop: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.captureVideoStop(deviceId);
  },

  /** Start audio recording. Audio completion/error emitted via events. */
  captureAudioStart: async (
    deviceId: number,
    savePath?: string | null,
  ): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.captureAudioStart(
      deviceId,
      savePath ?? null,
    );
  },

  /** Stop audio recording */
  captureAudioStop: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.captureAudioStop(deviceId);
  },

  /** Check if the camera is currently opened */
  isCameraOpened: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.isCameraOpened(deviceId);
  },

  /** Close the camera */
  closeCamera: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.closeCamera(deviceId);
  },

  /** Update preview resolution */
  updateResolution: async (
    deviceId: number,
    width: number,
    height: number,
  ): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.updateResolution(deviceId, width, height);
  },

  /** Get all supported preview sizes */
  getAllPreviewSizes: async (deviceId: number): Promise<PreviewSize[]> => {
    return NativeModules.UVCDeviceModule.getAllPreviewSizes(deviceId);
  },

  /** Get current preview size */
  getCurrentPreviewSize: async (
    deviceId: number,
  ): Promise<PreviewSize | null> => {
    return NativeModules.UVCDeviceModule.getCurrentPreviewSize(deviceId);
  },

  /** Set rotation angle (0, 90, 180, 270) */
  setRotateType: async (
    deviceId: number,
    angle: RotateAngle,
  ): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.setRotateType(deviceId, angle);
  },

  /** Send a raw camera command (hex value) */
  sendCameraCommand: async (
    deviceId: number,
    command: number,
  ): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.sendCameraCommand(deviceId, command);
  },

  // ======================== Streaming ========================

  /** Start H264 & AAC capture stream */
  captureStreamStart: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.captureStreamStart(deviceId);
  },

  /** Stop capture stream */
  captureStreamStop: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.captureStreamStop(deviceId);
  },

  /** Start mic playback */
  startPlayMic: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.startPlayMic(deviceId);
  },

  /** Stop mic playback */
  stopPlayMic: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.stopPlayMic(deviceId);
  },

  // ======================== Auto Focus ========================

  setAutoFocus: async (deviceId: number, focus: boolean): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.setAutoFocus(deviceId, focus);
  },

  getAutoFocus: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.getAutoFocus(deviceId);
  },

  resetAutoFocus: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.resetAutoFocus(deviceId);
  },

  // ======================== Brightness ========================

  setBrightness: async (deviceId: number, value: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.setBrightness(deviceId, value);
  },

  getBrightness: async (deviceId: number): Promise<number | null> => {
    return NativeModules.UVCDeviceModule.getBrightness(deviceId);
  },

  resetBrightness: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.resetBrightness(deviceId);
  },

  // ======================== Contrast ========================

  setContrast: async (deviceId: number, value: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.setContrast(deviceId, value);
  },

  getContrast: async (deviceId: number): Promise<number | null> => {
    return NativeModules.UVCDeviceModule.getContrast(deviceId);
  },

  resetContrast: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.resetContrast(deviceId);
  },

  // ======================== Gain ========================

  setGain: async (deviceId: number, value: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.setGain(deviceId, value);
  },

  getGain: async (deviceId: number): Promise<number | null> => {
    return NativeModules.UVCDeviceModule.getGain(deviceId);
  },

  resetGain: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.resetGain(deviceId);
  },

  // ======================== Gamma ========================

  setGamma: async (deviceId: number, value: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.setGamma(deviceId, value);
  },

  getGamma: async (deviceId: number): Promise<number | null> => {
    return NativeModules.UVCDeviceModule.getGamma(deviceId);
  },

  resetGamma: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.resetGamma(deviceId);
  },

  // ======================== Hue ========================

  setHue: async (deviceId: number, value: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.setHue(deviceId, value);
  },

  getHue: async (deviceId: number): Promise<number | null> => {
    return NativeModules.UVCDeviceModule.getHue(deviceId);
  },

  resetHue: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.resetHue(deviceId);
  },

  // ======================== Zoom ========================

  setZoom: async (deviceId: number, value: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.setZoom(deviceId, value);
  },

  getZoom: async (deviceId: number): Promise<number | null> => {
    return NativeModules.UVCDeviceModule.getZoom(deviceId);
  },

  resetZoom: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.resetZoom(deviceId);
  },

  // ======================== Sharpness ========================

  setSharpness: async (deviceId: number, value: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.setSharpness(deviceId, value);
  },

  getSharpness: async (deviceId: number): Promise<number | null> => {
    return NativeModules.UVCDeviceModule.getSharpness(deviceId);
  },

  resetSharpness: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.resetSharpness(deviceId);
  },

  // ======================== Saturation ========================

  setSaturation: async (deviceId: number, value: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.setSaturation(deviceId, value);
  },

  getSaturation: async (deviceId: number): Promise<number | null> => {
    return NativeModules.UVCDeviceModule.getSaturation(deviceId);
  },

  resetSaturation: async (deviceId: number): Promise<boolean> => {
    return NativeModules.UVCDeviceModule.resetSaturation(deviceId);
  },

  // ======================== Device Events ========================

  onDeviceAttached: (callback: (device: UVCDevice) => void) => {
    return eventEmitter.addListener('onDeviceAttached', callback);
  },

  onDeviceDetached: (
    callback: (device: Pick<UVCDevice, 'deviceId'>) => void,
  ) => {
    return eventEmitter.addListener('onDeviceDetached', callback);
  },

  onDeviceConnected: (
    callback: (device: Pick<UVCDevice, 'deviceId'>) => void,
  ) => {
    return eventEmitter.addListener('onDeviceConnected', callback);
  },

  onDeviceDisconnected: (
    callback: (device: Pick<UVCDevice, 'deviceId'>) => void,
  ) => {
    return eventEmitter.addListener('onDeviceDisconnected', callback);
  },

  onDevicePermissionDenied: (
    callback: (device: Pick<UVCDevice, 'deviceId'>) => void,
  ) => {
    return eventEmitter.addListener('onDevicePermissionDenied', callback);
  },

  // ======================== Capture Events ========================

  onCaptureVideoComplete: (
    callback: (data: {deviceId: number; path: string}) => void,
  ) => {
    return eventEmitter.addListener('onCaptureVideoComplete', callback);
  },

  onCaptureVideoError: (
    callback: (data: {deviceId: number; error: string}) => void,
  ) => {
    return eventEmitter.addListener('onCaptureVideoError', callback);
  },

  onCaptureAudioComplete: (
    callback: (data: {deviceId: number; path: string}) => void,
  ) => {
    return eventEmitter.addListener('onCaptureAudioComplete', callback);
  },

  onCaptureAudioError: (
    callback: (data: {deviceId: number; error: string}) => void,
  ) => {
    return eventEmitter.addListener('onCaptureAudioError', callback);
  },
};

export {UVCDeviceModule};
export type {UVCDevice, PreviewSize, RotateAngle};
