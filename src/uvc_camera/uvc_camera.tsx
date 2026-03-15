import {
  Text,
  View,
  findNodeHandle,
  UIManager,
  StyleSheet,
} from "react-native";
import React, {
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";
import { UVCDeviceModule } from "./uvc_device_module";
import { TaskQueue, useDevices } from "./help";
import { useDeviceEvent } from "./help";
import { UVCCameraView, ViewCommands } from "./native_view";

const isDev = __DEV__;

const taskQueue = new TaskQueue();

const BaseUVCCamera = ({ deviceId }: { deviceId: number }) => {
  const viewRef = useRef<View>(null);
  const cameraViewRef = useRef(null);

  const doConnect = useCallback(async () => {
    console.log("doConnect", deviceId);
    try {
      // 获取权限，排队执行
      const isGranted = (await taskQueue.addTask(() =>
        UVCDeviceModule.requestPermission(deviceId)
      )) as boolean;

      console.log("isGranted", isGranted);

      // 权限请求成功后，设置设备ID，即可显示摄像头预览
      if (isGranted) {
        const node = findNodeHandle(cameraViewRef.current);
        if (node) {
          console.log("setDeviceId", deviceId);
          UIManager.dispatchViewManagerCommand(node, ViewCommands.setDeviceId, [
            deviceId,
          ]);
        }
      }
    } catch (error) {
      console.error("Failed to request device permission:", error);
    }
  }, [deviceId]);

  const { state } = useDeviceEvent({
    deviceId,
  });

  useEffect(() => {
    console.log("state", state);

    // 问题：切页面后重新回来就不显示了。
    // 方案：每次都 connect 能解决，不清楚为什么。
    doConnect();

    // // 只要是没有链接，就重新链接。
    // // 比如 一开始 和 attached 后，需要重新链接
    // if (state === "" || state === "attached") {
    //   doConnect();
    // }
    // // 比如 disconnected 后，需要重新链接
    // else if (state === "disconnected") {
    //   doConnect();
    // }
    // // 比如 permissionDenied 后，需要重新链接
    // else if (state === "permissionDenied") {
    //   doConnect();
    // }
  }, [state, doConnect]);

  return (
    <View ref={viewRef} style={styles.full}>
      <UVCCameraView
        ref={cameraViewRef}
        // @ts-ignore
        style={StyleSheet.absoluteFill}
      />
      {isDev && <Text style={styles.rightTop}>{state}</Text>}
    </View>
  );
};

const UVCCamera = ({ deviceId }: { deviceId?: number }) => {
  return (
    <View style={styles.full}>
      {deviceId ? (
        <BaseUVCCamera key={deviceId} deviceId={deviceId} />
      ) : (
        <Text style={styles.text}>等待连接</Text>
      )}
      {isDev && <Text style={styles.leftTop}>deviceId:{deviceId}</Text>}
    </View>
  );
};

/** 通过位置来调用，更便捷 */
const UVCCameraWithIndex = ({ index }: { index: number }) => {
  const { devices } = useDevices();

  return (
    <View style={styles.full}>
      <UVCCamera deviceId={devices[index]?.deviceId} />
    </View>
  );
};

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
  },
  rightTop: {
    position: "absolute",
    right: 5,
    top: 5,
    color: "red",
  },
});

export { UVCCamera, UVCCameraWithIndex };
