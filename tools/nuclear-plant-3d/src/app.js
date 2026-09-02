import * as THREE from "../vendor/three.module.js";
import { assertSceneContract } from "./scene/scene-contract.js";
import { AnimationClock, createAnimationLoop } from "./scene/animation-loop.js";
import { createCameraController } from "./scene/camera-controller.js";
import { createPlantScene, updatePlantScene } from "./scene/create-scene.js";
import { SelectionController } from "./scene/selection.js";
import { samplePreset } from "./state/presets.js";
import { createControls } from "./ui/controls.js";
import { formatFlowDescription, getDeviceLabel, getDeviceStatus } from "./ui/labels.js";

const FILE_PROTOCOL_MESSAGE = "请通过本地静态服务器打开：在工具目录执行 npm run serve，然后访问 http://127.0.0.1:4173/。直接打开 file:// 会被浏览器安全策略拦截资源加载。";
const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
const elements = {
  canvas: document.querySelector("#plant-canvas"),
  viewport: document.querySelector(".viewport-frame"),
  loadingStatus: document.querySelector("#loading-status"),
  errorMessage: document.querySelector("#error-message"),
  runtimeStatus: document.querySelector("#runtime-status"),
  selectionName: document.querySelector("#selection-name"),
  selectionStatus: document.querySelector("#selection-status"),
  selectionFlow: document.querySelector("#selection-flow"),
  togglePlay: document.querySelector("#toggle-play"),
  timeScale: document.querySelector("#time-scale"),
  resetView: document.querySelector("#reset-view"),
  autoRotate: document.querySelector("#auto-rotate"),
};

let renderer;
let camera;
let cameraController;
let sceneResources;
let selectionController;
let animationLoop;
let clock;
let sceneData;
let selectedDevice;
let latestState;

function showError(message) {
  elements.loadingStatus.hidden = true;
  elements.errorMessage.hidden = false;
  elements.errorMessage.textContent = message;
  elements.runtimeStatus.textContent = "场景不可用";
}

function updateSelectionDetails(device, state = latestState) {
  if (!device || !state) {
    elements.selectionName.textContent = "未选择设备";
    elements.selectionStatus.textContent = "点击占位设备查看状态";
    elements.selectionFlow.textContent = "流向说明将在选择后显示";
    return;
  }
  elements.selectionName.textContent = getDeviceLabel(device);
  elements.selectionStatus.textContent = getDeviceStatus(device, state);
  elements.selectionFlow.textContent = formatFlowDescription(device);
}

function updateRuntimeStatus() {
  const motionText = reducedMotion ? "减少动态" : clock.paused ? "已暂停" : "运行中";
  const speedText = `${clock.timeScale}×`;
  elements.runtimeStatus.textContent = `已加载 · 稳定预设 · ${motionText} · ${speedText}`;
}

function syncCameraMotion() {
  if (!cameraController) {
    return;
  }
  cameraController.controls.autoRotate = elements.autoRotate.checked && !reducedMotion && !clock.paused;
  cameraController.controls.enableDamping = !reducedMotion && !clock.paused;
}

function resizeRenderer() {
  if (!renderer || !camera) {
    return;
  }
  const width = Math.max(1, elements.viewport.clientWidth);
  const height = Math.max(1, elements.canvas.clientHeight);
  renderer.setSize(width, height, false);
  cameraController?.resize(width / height);
}

function initialiseRenderer() {
  const webglContext = elements.canvas.getContext("webgl2") || elements.canvas.getContext("webgl");
  if (!webglContext) {
    throw new Error("当前浏览器未提供 WebGL。请启用硬件加速或改用支持 WebGL 的浏览器。");
  }
  renderer = new THREE.WebGLRenderer({ canvas: elements.canvas, antialias: true, powerPreference: "high-performance" });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
  renderer.outputColorSpace = THREE.SRGBColorSpace;
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = 1.05;
}

async function loadSceneData() {
  const response = await fetch("./scenes/standard-running-plant.json", { cache: "no-store" });
  if (!response.ok) {
    throw new Error(`场景合同加载失败：HTTP ${response.status}。`);
  }
  const data = await response.json();
  assertSceneContract(data);
  return data;
}

function startApplication() {
  latestState = samplePreset(sceneData.defaultPresetId, 0);
  clock = new AnimationClock({ reducedMotion });
  const initialWidth = Math.max(1, elements.viewport.clientWidth);
  const initialHeight = Math.max(1, elements.canvas.clientHeight);
  camera = new THREE.PerspectiveCamera(42, initialWidth / initialHeight, 0.1, 180);
  sceneResources = createPlantScene(sceneData);
  cameraController = createCameraController({ camera, domElement: renderer.domElement, sceneData, reducedMotion });
  selectionController = new SelectionController({
    camera,
    domElement: renderer.domElement,
    pickables: sceneResources.pickables,
    onSelectionChange(deviceId) {
      selectedDevice = deviceId ? sceneResources.deviceMap.get(deviceId) : null;
      updateSelectionDetails(selectedDevice);
    },
  });

  const ui = createControls({
    elements,
    onTogglePlay() {
      clock.setPaused(!clock.paused);
      syncCameraMotion();
      ui.setPaused(clock.paused);
      updateRuntimeStatus();
    },
    onTimeScale(value) {
      clock.setTimeScale(value);
      updateRuntimeStatus();
    },
    onResetView() {
      cameraController.reset();
    },
    onAutoRotate() {
      syncCameraMotion();
    },
  });
  ui.setPaused(false);
  if (reducedMotion) {
    elements.autoRotate.checked = false;
    elements.autoRotate.disabled = true;
  }
  syncCameraMotion();

  const sampleCurrentState = (elapsedSeconds) => ({
    ...samplePreset(sceneData.defaultPresetId, elapsedSeconds),
    timeScale: clock.timeScale,
  });
  animationLoop = createAnimationLoop({
    renderer,
    scene: sceneResources.scene,
    camera,
    controls: cameraController.controls,
    clock,
    sampleState: sampleCurrentState,
    updateScene(state, elapsedSeconds, advancedSeconds) {
      latestState = state;
      updatePlantScene(sceneResources, state, elapsedSeconds, advancedSeconds);
      updateSelectionDetails(selectedDevice, state);
    },
  });

  resizeRenderer();
  cameraController.reset();
  const resizeObserver = typeof ResizeObserver === "function" ? new ResizeObserver(resizeRenderer) : null;
  resizeObserver?.observe(elements.viewport);
  window.addEventListener("resize", resizeRenderer);
  window.addEventListener("beforeunload", () => {
    resizeObserver?.disconnect();
    window.removeEventListener("resize", resizeRenderer);
    animationLoop?.stop();
    selectionController?.dispose();
    renderer?.dispose();
  }, { once: true });
  updateSelectionDetails(null);
  updateRuntimeStatus();
  elements.loadingStatus.hidden = true;
}

async function bootstrap() {
  if (window.location.protocol === "file:") {
    showError(FILE_PROTOCOL_MESSAGE);
    return;
  }
  try {
    initialiseRenderer();
    sceneData = await loadSceneData();
    startApplication();
  } catch (error) {
    showError(error.message || "三维场景初始化失败。");
  }
}

bootstrap();
