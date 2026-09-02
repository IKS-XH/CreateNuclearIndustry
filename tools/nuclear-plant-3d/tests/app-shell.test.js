import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";

const toolRoot = dirname(dirname(fileURLToPath(import.meta.url)));

test("PLANT3D-02 页面包含控制、选择和可读性降级入口", async () => {
  const page = await readFile(join(toolRoot, "index.html"), "utf8");
  const stylesheet = await readFile(join(toolRoot, "styles.css"), "utf8");
  const app = await readFile(join(toolRoot, "src/app.js"), "utf8");
  for (const id of ["toggle-play", "time-scale", "reset-view", "auto-rotate", "selection-name", "selection-status", "selection-flow"]) {
    assert.match(page, new RegExp(`id="${id}"`));
  }
  assert.match(app, /prefers-reduced-motion/);
  assert.match(stylesheet, /touch-action: none/);
  assert.match(stylesheet, /grid-template-columns/);
});
