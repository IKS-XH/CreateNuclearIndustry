import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const modularUi = await readFile(new URL("../src/ui.js", import.meta.url), "utf8");
const singleFile = await readFile(new URL("../reactor-simulator-single.html", import.meta.url), "utf8");

test("fuel detail displays zero-integrity damage multiplier as 2.0x", () => {
  const displayExpression = "${formatNumber(2 - integrity, 5)}×";
  assert.ok(modularUi.includes(displayExpression));
  assert.ok(singleFile.includes(displayExpression));
  assert.doesNotMatch(modularUi, /integrity <= 1e-12 \? 0 : 2 - integrity/);
  assert.doesNotMatch(singleFile, /integrity <= 1e-12 \? 0 : 2 - integrity/);
});

test("live control editing and explicit brush stop are present in both UI builds", () => {
  for (const source of [modularUi, singleFile]) {
    assert.match(source, /function applyLiveControlRodDepth/);
    assert.match(source, /function setAllControlRodDepth/);
    assert.match(source, /apply-all-rod-depth/);
    assert.match(source, /统一深度/);
    assert.match(source, /模拟继续运行/);
    assert.match(source, /stop-painting/);
    assert.match(source, /function stopPainting/);
    assert.doesNotMatch(source, /setPointerCapture/);
  }
});

test("coolant port counts use scene inputs while actual flows remain parameter caps", () => {
  for (const source of [modularUi, singleFile]) {
    assert.match(source, /cold-port-count-input/);
    assert.match(source, /hot-port-count-input/);
    assert.match(source, /sceneInputId/);
    assert.match(source, /function setConfigFormValues/);
  }
  assert.match(singleFile, /actualColdIn/);
  assert.match(singleFile, /actualHotOut/);
  assert.match(singleFile, /实际输入\/接收仍可在参数区单独限制/);
});
