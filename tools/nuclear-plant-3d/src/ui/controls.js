/**
 * 绑定网页控制条。回调只负责通知应用状态变化，避免 UI 直接修改 Three.js 场景对象。
 */
export function createControls({ elements, onTogglePlay, onTimeScale, onResetView, onAutoRotate }) {
  elements.togglePlay.addEventListener("click", onTogglePlay);
  elements.timeScale.addEventListener("change", () => onTimeScale(Number(elements.timeScale.value)));
  elements.resetView.addEventListener("click", onResetView);
  elements.autoRotate.addEventListener("change", () => onAutoRotate(elements.autoRotate.checked));

  return {
    setPaused(paused) {
      elements.togglePlay.textContent = paused ? "继续播放" : "暂停";
      elements.togglePlay.setAttribute("aria-pressed", String(paused));
    },
    setRuntimeStatus(text) {
      elements.runtimeStatus.textContent = text;
    },
  };
}
