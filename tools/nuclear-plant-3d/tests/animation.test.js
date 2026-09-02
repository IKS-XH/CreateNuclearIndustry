import assert from "node:assert/strict";
import test from "node:test";
import { AnimationClock } from "../src/scene/animation-loop.js";

test("动画时钟按倍速推进逻辑时间", () => {
  const clock = new AnimationClock({ timeScale: 2 });
  assert.equal(clock.advance(0.5), 1);
  assert.equal(clock.getElapsedSeconds(), 1);
});

test("暂停后动画时钟保持冻结，恢复后从原时间继续", () => {
  const clock = new AnimationClock();
  clock.advance(1);
  clock.setPaused(true);
  assert.equal(clock.advance(10), 0);
  assert.equal(clock.getElapsedSeconds(), 1);
  clock.setPaused(false);
  clock.advance(0.25);
  assert.equal(clock.getElapsedSeconds(), 1.25);
});

test("减少动态模式冻结逻辑时间但仍允许静态首屏渲染", () => {
  const clock = new AnimationClock({ reducedMotion: true, timeScale: 2 });
  assert.equal(clock.advance(2), 0);
  assert.equal(clock.getElapsedSeconds(), 0);
});

test("动画时钟拒绝越界速度和非法帧间隔", () => {
  assert.throws(() => new AnimationClock({ timeScale: 5 }), /动画速度/);
  const clock = new AnimationClock();
  assert.throws(() => clock.advance(-0.1), /帧间隔/);
  assert.throws(() => clock.advance(Number.NaN), /帧间隔/);
});
