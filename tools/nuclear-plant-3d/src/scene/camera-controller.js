import { OrbitControls } from "../../vendor/OrbitControls.js";

function assertSceneData(sceneData) {
  if (!sceneData || !Array.isArray(sceneData.devices) || sceneData.devices.length === 0) {
    throw new TypeError("相机总览需要包含设备的场景数据。");
  }
}

function normalize(vector) {
  const length = Math.hypot(...vector);
  return vector.map((value) => value / length);
}

/**
 * 根据设备包围盒计算斜俯视总览。坐标单位是 Minecraft 方块，结果可被不同宽高比的客户端复用。
 * @param {object} sceneData 已通过场景合同校验的数据。
 * @param {{aspectRatio?: number, fovDegrees?: number}} options 取景参数。
 * @returns {{min:number[], max:number[], center:number[], target:number[], cameraPosition:number[], distance:number}}
 */
export function calculateSceneOverview(sceneData, { aspectRatio = 16 / 9, fovDegrees = 42 } = {}) {
  assertSceneData(sceneData);
  if (!Number.isFinite(aspectRatio) || aspectRatio <= 0 || !Number.isFinite(fovDegrees) || fovDegrees <= 0 || fovDegrees >= 180) {
    throw new RangeError("相机取景参数无效。");
  }

  const min = [Infinity, Infinity, Infinity];
  const max = [-Infinity, -Infinity, -Infinity];
  for (const device of sceneData.devices) {
    for (let axis = 0; axis < 3; axis += 1) {
      min[axis] = Math.min(min[axis], device.position[axis]);
      max[axis] = Math.max(max[axis], device.position[axis] + device.dimensions[axis]);
    }
  }

  const center = min.map((value, axis) => (value + max[axis]) / 2);
  const size = max.map((value, axis) => value - min[axis]);
  const target = [center[0], Math.max(min[1] + 1.5, center[1] - size[1] * 0.16), center[2]];
  const verticalFovRadians = (fovDegrees * Math.PI) / 180;
  const horizontalRequirement = size[0] / Math.max(aspectRatio, 0.35);
  const verticalRequirement = Math.max(size[1], size[2] * 0.8);
  const distance = Math.max(14, (Math.max(horizontalRequirement, verticalRequirement) / (2 * Math.tan(verticalFovRadians / 2))) * 1.35);
  const viewDirection = normalize([0.16, 0.72, 0.66]);
  const cameraPosition = target.map((value, axis) => value + viewDirection[axis] * distance);

  return { min, max, center, target, cameraPosition, distance };
}

/**
 * 创建只负责用户视角的 OrbitControls。相机状态属于客户端渲染层，不参与机组状态计算。
 * @param {object} options Three.js PerspectiveCamera、画布节点和场景数据。
 * @returns {{controls: OrbitControls, reset: () => void, resize: (aspectRatio: number) => void, overview: object}}
 */
export function createCameraController({ camera, domElement, sceneData, reducedMotion = false }) {
  assertSceneData(sceneData);
  const controls = new OrbitControls(camera, domElement);
  controls.enablePan = true;
  controls.enableZoom = true;
  controls.enableDamping = !reducedMotion;
  controls.dampingFactor = 0.08;
  controls.minDistance = 5;
  controls.maxDistance = 120;
  controls.autoRotate = false;
  controls.autoRotateSpeed = 0.7;

  let overview = calculateSceneOverview(sceneData, { aspectRatio: camera.aspect || 16 / 9, fovDegrees: camera.fov });
  const reset = () => {
    overview = calculateSceneOverview(sceneData, { aspectRatio: camera.aspect || 16 / 9, fovDegrees: camera.fov });
    camera.position.fromArray(overview.cameraPosition);
    camera.near = 0.1;
    camera.far = Math.max(160, overview.distance * 5);
    camera.updateProjectionMatrix();
    controls.target.fromArray(overview.target);
    controls.update();
  };

  reset();
  return {
    controls,
    reset,
    resize(aspectRatio) {
      camera.aspect = aspectRatio;
      camera.updateProjectionMatrix();
    },
    get overview() {
      return overview;
    },
  };
}
