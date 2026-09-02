import * as THREE from "../../vendor/three.module.js";

/**
 * 将画布坐标转换成 Three.js 射线所需的标准化设备坐标。
 * @param {number} clientX 指针在视口中的横坐标。
 * @param {number} clientY 指针在视口中的纵坐标。
 * @param {{left:number,top:number,width:number,height:number}} rect 画布边界。
 * @returns {{x:number,y:number}} 标准化坐标，范围为 [-1,1]。
 */
export function getNormalizedPointer(clientX, clientY, rect) {
  if (!rect || rect.width <= 0 || rect.height <= 0) {
    return { x: 0, y: 0 };
  }
  return {
    x: ((clientX - rect.left) / rect.width) * 2 - 1,
    y: -((clientY - rect.top) / rect.height) * 2 + 1,
  };
}

/**
 * 从射线命中的网格向父级查找设备 ID，避免把管线箭头和地面误认为设备。
 * @param {object|null} object 射线命中的 Three.js 对象。
 * @returns {string|null} 设备 ID，未命中设备时返回 null。
 */
export function findDeviceIdFromObject(object) {
  let current = object;
  while (current) {
    if (typeof current.userData?.deviceId === "string") {
      return current.userData.deviceId;
    }
    current = current.parent;
  }
  return null;
}

function setHighlight(object, enabled) {
  object.traverse((node) => {
    if (!node.isMesh || !node.material) {
      return;
    }
    const materials = Array.isArray(node.material) ? node.material : [node.material];
    for (const material of materials) {
      if (!material.emissive) {
        continue;
      }
      if (enabled) {
        if (!node.userData.selectionOriginal) {
          node.userData.selectionOriginal = {
            emissive: material.emissive.getHex(),
            intensity: material.emissiveIntensity,
          };
        }
        material.emissive.set(0xf2c14e);
        material.emissiveIntensity = 0.7;
      } else if (node.userData.selectionOriginal) {
        material.emissive.set(node.userData.selectionOriginal.emissive);
        material.emissiveIntensity = node.userData.selectionOriginal.intensity;
        delete node.userData.selectionOriginal;
      }
    }
  });
}

/**
 * 实现设备射线选取并管理高亮。输入 pickables 只应包含占位设备网格，不改变设备状态。
 */
export class SelectionController {
  constructor({ camera, domElement, pickables, onSelectionChange = () => {} }) {
    this.camera = camera;
    this.domElement = domElement;
    this.pickables = pickables;
    this.onSelectionChange = onSelectionChange;
    this.raycaster = new THREE.Raycaster();
    this.pointerDown = null;
    this.selectedObject = null;
    this.handlePointerDown = (event) => {
      this.pointerDown = { x: event.clientX, y: event.clientY };
    };
    this.handlePointerUp = (event) => {
      if (!this.pointerDown) {
        return;
      }
      const distance = Math.hypot(event.clientX - this.pointerDown.x, event.clientY - this.pointerDown.y);
      this.pointerDown = null;
      if (distance > 6) {
        return;
      }
      this.selectAt(event.clientX, event.clientY);
    };
    domElement.addEventListener("pointerdown", this.handlePointerDown);
    domElement.addEventListener("pointerup", this.handlePointerUp);
  }

  selectAt(clientX, clientY) {
    const pointer = getNormalizedPointer(clientX, clientY, this.domElement.getBoundingClientRect());
    this.raycaster.setFromCamera(pointer, this.camera);
    const intersections = this.raycaster.intersectObjects(this.pickables, true);
    const hit = intersections.find((intersection) => findDeviceIdFromObject(intersection.object));
    const nextObject = hit ? this.findDeviceRoot(hit.object) : null;
    if (nextObject === this.selectedObject) {
      return;
    }
    if (this.selectedObject) {
      setHighlight(this.selectedObject, false);
    }
    this.selectedObject = nextObject;
    if (this.selectedObject) {
      setHighlight(this.selectedObject, true);
    }
    this.onSelectionChange(this.selectedObject ? findDeviceIdFromObject(this.selectedObject) : null, this.selectedObject);
  }

  findDeviceRoot(object) {
    let current = object;
    while (current.parent && !current.parent.userData?.deviceId) {
      current = current.parent;
    }
    return current.parent?.userData?.deviceId ? current.parent : current;
  }

  clear() {
    if (!this.selectedObject) {
      return;
    }
    setHighlight(this.selectedObject, false);
    this.selectedObject = null;
    this.onSelectionChange(null, null);
  }

  dispose() {
    this.domElement.removeEventListener("pointerdown", this.handlePointerDown);
    this.domElement.removeEventListener("pointerup", this.handlePointerUp);
  }
}
