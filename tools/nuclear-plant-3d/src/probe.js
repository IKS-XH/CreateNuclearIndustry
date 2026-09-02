import * as THREE from "../vendor/three.module.js";
import { OrbitControls } from "../vendor/OrbitControls.js";
import { GLTFLoader } from "../vendor/GLTFLoader.js";

const canvas = document.querySelector("#plant-canvas");
const viewport = document.querySelector(".viewport-frame");
const loadingStatus = document.querySelector("#loading-status");
const errorMessage = document.querySelector("#error-message");
const runtimeStatus = document.querySelector("#runtime-status");
const resetViewButton = document.querySelector("#reset-view");
const autoRotateCheckbox = document.querySelector("#auto-rotate");
const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
const fileProtocolMessage = "请通过本地静态服务器打开：在工具目录执行 npm run serve，然后访问 http://127.0.0.1:4173/。直接打开 file:// 会被浏览器安全策略拦截资源加载。";

const scene = new THREE.Scene();
scene.background = new THREE.Color(0x172326);

const camera = new THREE.PerspectiveCamera(42, 1, 0.1, 100);
const defaultCameraPosition = new THREE.Vector3(7.4, 5.4, 8.2);
camera.position.copy(defaultCameraPosition);

let renderer;
let controls;

function showError(message) {
  loadingStatus.hidden = true;
  errorMessage.hidden = false;
  errorMessage.textContent = message;
  runtimeStatus.textContent = "探针失败";
}

function loadTexture() {
  return new Promise((resolve, reject) => {
    new THREE.TextureLoader().load(
      "./assets/probe-texture.svg",
      (texture) => resolve(texture),
      undefined,
      reject,
    );
  });
}

function loadGltf() {
  return new Promise((resolve, reject) => {
    new GLTFLoader().load("./assets/probe-piece.gltf", resolve, undefined, reject);
  });
}

function resizeRenderer() {
  if (!renderer) {
    return;
  }
  const width = Math.max(1, viewport.clientWidth);
  const height = Math.max(1, canvas.clientHeight);
  renderer.setSize(width, height, false);
  camera.aspect = width / height;
  camera.updateProjectionMatrix();
}

function restoreView() {
  camera.position.copy(defaultCameraPosition);
  controls.target.set(0, 0.3, 0);
  controls.update();
}

function configureGltfPiece(gltf) {
  const piece = gltf.scene;
  piece.name = "GLTFProbePiece";
  piece.position.set(2, 0.45, 0);
  piece.scale.setScalar(1.35);
  piece.traverse((node) => {
    if (!node.isMesh) {
      return;
    }
    node.material = new THREE.MeshStandardMaterial({
      color: 0xd3a257,
      metalness: 0.25,
      roughness: 0.5,
      flatShading: true,
    });
  });
  scene.add(piece);
}

function buildScene(texture, gltf) {
  texture.colorSpace = THREE.SRGBColorSpace;
  texture.magFilter = THREE.NearestFilter;
  texture.minFilter = THREE.NearestMipmapNearestFilter;
  texture.anisotropy = renderer.capabilities.getMaxAnisotropy();
  texture.needsUpdate = true;

  const cube = new THREE.Mesh(
    new THREE.BoxGeometry(2.5, 2.5, 2.5),
    new THREE.MeshStandardMaterial({
      map: texture,
      metalness: 0.15,
      roughness: 0.58,
    }),
  );
  cube.name = "TexturedProbeCube";
  cube.position.set(-1.9, 0.1, 0);
  scene.add(cube);

  configureGltfPiece(gltf);
  scene.add(new THREE.AxesHelper(3.5));

  const floor = new THREE.Mesh(
    new THREE.PlaneGeometry(14, 8),
    new THREE.MeshStandardMaterial({
      color: 0x243538,
      metalness: 0.05,
      roughness: 0.92,
    }),
  );
  floor.rotation.x = -Math.PI / 2;
  floor.position.y = -1.25;
  scene.add(floor);
}

function animate() {
  controls.update();
  renderer.render(scene, camera);
}

function initialiseRenderer() {
  const webglContext = canvas.getContext("webgl2") || canvas.getContext("webgl");
  if (!webglContext) {
    throw new Error("当前浏览器未提供 WebGL，上层页面无法运行三维探针。");
  }

  renderer = new THREE.WebGLRenderer({ canvas, antialias: true, powerPreference: "high-performance" });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.outputColorSpace = THREE.SRGBColorSpace;
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = 1.05;

  scene.add(new THREE.HemisphereLight(0xd6e5e2, 0x172326, 2.1));
  const keyLight = new THREE.DirectionalLight(0xffe3b1, 3.2);
  keyLight.position.set(-4, 7, 5);
  scene.add(keyLight);
  const rimLight = new THREE.DirectionalLight(0x72c6c8, 1.7);
  rimLight.position.set(5, 3, -4);
  scene.add(rimLight);

  controls = new OrbitControls(camera, renderer.domElement);
  controls.target.set(0, 0.3, 0);
  controls.enableDamping = !reducedMotion;
  controls.minDistance = 4;
  controls.maxDistance = 18;
  controls.autoRotate = autoRotateCheckbox.checked && !reducedMotion;
  controls.autoRotateSpeed = 0.8;
  controls.update();

  resetViewButton.addEventListener("click", restoreView);
  autoRotateCheckbox.addEventListener("change", () => {
    controls.autoRotate = autoRotateCheckbox.checked && !reducedMotion;
  });
  window.addEventListener("resize", resizeRenderer);
  resizeRenderer();
  renderer.setAnimationLoop(animate);
}

if (window.location.protocol === "file:") {
  showError(fileProtocolMessage);
} else {
  try {
    initialiseRenderer();
    Promise.all([loadTexture(), loadGltf()])
      .then(([texture, gltf]) => {
        buildScene(texture, gltf);
        loadingStatus.hidden = true;
        runtimeStatus.textContent = reducedMotion
          ? "已加载 · 减少动态模式"
          : "已加载 · OrbitControls 可用";
      })
      .catch((error) => {
        showError(`本地资源加载失败：${error.message || "未知错误"}`);
      });
  } catch (error) {
    showError(error.message || "三维探针初始化失败。");
  }
}
