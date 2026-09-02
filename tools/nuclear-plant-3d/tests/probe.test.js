import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { readFile } from "node:fs/promises";
import { createServer } from "node:net";
import { dirname, join } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import test from "node:test";

const testRoot = dirname(fileURLToPath(import.meta.url));
const toolRoot = dirname(testRoot);
const vendorRoot = join(toolRoot, "vendor");

async function readText(relativePath) {
  return readFile(join(toolRoot, relativePath), "utf8");
}

async function readJson(relativePath) {
  return JSON.parse(await readText(relativePath));
}

async function getFreePort() {
  const socket = createServer();
  await new Promise((resolve) => socket.listen(0, "127.0.0.1", resolve));
  const address = socket.address();
  const port = typeof address === "object" && address ? address.port : 0;
  await new Promise((resolve) => socket.close(resolve));
  return port;
}

async function waitForServer(url, child) {
  const deadline = Date.now() + 5000;
  let lastError;
  while (Date.now() < deadline) {
    try {
      return await fetch(url);
    } catch (error) {
      lastError = error;
      await new Promise((resolve) => setTimeout(resolve, 50));
    }
  }
  throw new Error(`静态服务器未能启动：${lastError?.message ?? "未知错误"}`);
}

test("固定版本的 Three.js、OrbitControls 和 GLTFLoader 可由 Node 解析", async () => {
  const three = await import(pathToFileURL(join(vendorRoot, "three.module.js")));
  const controls = await import(pathToFileURL(join(vendorRoot, "OrbitControls.js")));
  const gltf = await import(pathToFileURL(join(vendorRoot, "GLTFLoader.js")));

  assert.equal(three.REVISION, "180");
  assert.equal(typeof controls.OrbitControls, "function");
  assert.equal(typeof gltf.GLTFLoader, "function");
  assert.equal(new three.BoxGeometry(1, 1, 1).attributes.position.count, 24);

  const packageJson = await readJson("package.json");
  assert.equal(packageJson.dependencies.three, "0.180.0");
  assert.match(await readText("vendor/OrbitControls.js"), /\.\/three\.module\.js/);
  assert.match(await readText("vendor/GLTFLoader.js"), /\.\/three\.module\.js/);
  assert.match(await readText("vendor/utils/BufferGeometryUtils.js"), /\.\.\/three\.module\.js/);
  assert.doesNotMatch(await readText("vendor/OrbitControls.js"), /from ["']three["']/);
  assert.doesNotMatch(await readText("vendor/GLTFLoader.js"), /from ["']three["']/);
  assert.doesNotMatch(await readText("vendor/utils/BufferGeometryUtils.js"), /from ["']three["']/);
  assert.ok((await readFile(join(vendorRoot, "LICENSE-three.txt"))).length > 0);
});

test("探针页面只引用本地页面资源和本地 GLTF 测试件", async () => {
  const page = await readText("index.html");
  const script = await readText("src/probe.js");
  const app = await readText("src/app.js");
  const gltf = await readJson("assets/probe-piece.gltf");

  assert.match(page, /\.\/src\/app\.js/);
  assert.match(page, /window\.location\.protocol === "file:"/);
  assert.match(page, /npm run serve/);
  assert.match(script, /window\.location\.protocol === "file:"/);
  assert.match(app, /当前浏览器未提供 WebGL/);
  assert.match(app, /prefers-reduced-motion/);
  assert.match(app, /ResizeObserver/);
  assert.match(script, /\.\/assets\/probe-texture\.svg/);
  assert.match(script, /\.\/assets\/probe-piece\.gltf/);
  assert.doesNotMatch(page, /(?:src|href)=["']https?:\/\//);
  assert.equal(gltf.asset.version, "2.0");
  assert.equal(gltf.buffers[0].byteLength, 72);
  assert.equal(gltf.meshes[0].primitives.length, 1);
});

test("本地静态服务器能够提供页面、模块、纹理和 GLTF 资源", async () => {
  const port = await getFreePort();
  const child = spawn(process.execPath, ["scripts/serve.mjs"], {
    cwd: toolRoot,
    env: { ...process.env, NUCLEAR_PLANT_3D_PORT: String(port) },
    stdio: ["ignore", "pipe", "pipe"],
  });

  try {
    const baseUrl = `http://127.0.0.1:${port}`;
    const pageResponse = await waitForServer(`${baseUrl}/`, child);
    assert.equal(pageResponse.status, 200);
    assert.match(pageResponse.headers.get("content-type") ?? "", /text\/html/);
    assert.match(await pageResponse.text(), /PLANT3D-03 · 实验反应堆视觉提案/);

    for (const [path, contentType] of [
      ["/vendor/three.module.js", /javascript/],
      ["/vendor/OrbitControls.js", /javascript/],
      ["/vendor/GLTFLoader.js", /javascript/],
      ["/vendor/utils/BufferGeometryUtils.js", /javascript/],
      ["/src/app.js", /javascript/],
      ["/scenes/standard-running-plant.json", /application\/json/],
      ["/assets/probe-texture.svg", /image\/svg\+xml/],
      ["/assets/probe-piece.gltf", /model\/gltf\+json/],
    ]) {
      const response = await fetch(`${baseUrl}${path}`);
      assert.equal(response.status, 200, path);
      assert.match(response.headers.get("content-type") ?? "", contentType, path);
    }

    const traversalResponse = await fetch(`${baseUrl}/%2e%2e%2fpackage.json`);
    assert.equal(traversalResponse.status, 404);
  } finally {
    child.kill();
    await new Promise((resolve) => child.once("exit", resolve));
  }
});
