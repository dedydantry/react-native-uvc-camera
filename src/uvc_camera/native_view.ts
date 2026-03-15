import { requireNativeComponent, UIManager } from "react-native";

const ComponentName = "UVCCameraView";
const UVCCameraView = requireNativeComponent(ComponentName);
const ViewCommands = UIManager.getViewManagerConfig(ComponentName)?.Commands;

export { UVCCameraView, ViewCommands, ComponentName };
