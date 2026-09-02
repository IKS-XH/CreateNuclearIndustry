/**
 * 管理网页可视化的逻辑时间。该模块不依赖 DOM 或 Minecraft 服务端，输入输出单位均为秒。
 * 暂停和减少动态模式都会冻结逻辑时间，从而保证设备动画不会在界面上偷偷继续推进。
 */
export class AnimationClock {
  constructor({ timeScale = 1, paused = false, reducedMotion = false } = {}) {
    this.elapsedSeconds = 0;
    this.paused = Boolean(paused);
    this.reducedMotion = Boolean(reducedMotion);
    this.setTimeScale(timeScale);
  }

  advance(deltaSeconds) {
    if (!Number.isFinite(deltaSeconds) || deltaSeconds < 0) {
      throw new RangeError("动画帧间隔必须是非负有限秒数。");
    }
    if (this.paused || this.reducedMotion) {
      return 0;
    }
    const advancedSeconds = deltaSeconds * this.timeScale;
    this.elapsedSeconds += advancedSeconds;
    return advancedSeconds;
  }

  setPaused(paused) {
    this.paused = Boolean(paused);
  }

  setTimeScale(timeScale) {
    if (!Number.isFinite(timeScale) || timeScale < 0 || timeScale > 4) {
      throw new RangeError("动画速度必须位于 [0,4]。");
    }
    this.timeScale = timeScale;
  }

  reset() {
    this.elapsedSeconds = 0;
  }

  getElapsedSeconds() {
    return this.elapsedSeconds;
  }
}

/**
 * 连接 Three.js 渲染器与逻辑时钟。渲染器可以继续处理用户相机操作，但场景更新只消费时钟时间。
 * @param {object} options 渲染器、控制器、时钟、状态采样器和场景更新回调。
 * @returns {{stop: () => void}} 停止当前渲染循环的句柄。
 */
export function createAnimationLoop({ renderer, scene, camera, controls, clock, sampleState, updateScene }) {
  if (!renderer || typeof renderer.setAnimationLoop !== "function") {
    throw new TypeError("动画循环需要支持 setAnimationLoop 的渲染器。");
  }
  if (!clock || typeof clock.advance !== "function") {
    throw new TypeError("动画循环需要 AnimationClock。");
  }
  if (typeof sampleState !== "function" || typeof updateScene !== "function") {
    throw new TypeError("动画循环需要状态采样器和场景更新回调。");
  }

  let previousTimeMs = null;
  const frame = (timeMs) => {
    const deltaSeconds = previousTimeMs === null
      ? 0
      : Math.min(0.25, Math.max(0, (timeMs - previousTimeMs) / 1000));
    previousTimeMs = timeMs;
    const advancedSeconds = clock.advance(deltaSeconds);
    const elapsedSeconds = clock.getElapsedSeconds();
    const state = sampleState(elapsedSeconds);
    updateScene(state, elapsedSeconds, advancedSeconds);
    controls?.update();
    renderer.render(scene, camera);
  };

  renderer.setAnimationLoop(frame);
  return {
    stop() {
      renderer.setAnimationLoop(null);
      previousTimeMs = null;
    },
  };
}
