import { copyFileSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join, relative } from "node:path";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const packageRoot = dirname(dirname(require.resolve("three")));
const toolRoot = fileURLToPath(new URL("../", import.meta.url));
const vendorRoot = join(toolRoot, "vendor");

mkdirSync(vendorRoot, { recursive: true });
copyFileSync(join(packageRoot, "build", "three.module.js"), join(vendorRoot, "three.module.js"));
copyFileSync(join(packageRoot, "build", "three.core.js"), join(vendorRoot, "three.core.js"));

function copyExampleModule(sourcePath, destinationName) {
  const source = readFileSync(join(packageRoot, sourcePath), "utf8");
  const destinationPath = join(vendorRoot, destinationName);
  const threeModulePath = relative(dirname(destinationPath), join(vendorRoot, "three.module.js"))
    .replaceAll("\\", "/");
  const localThreeModulePath = threeModulePath.startsWith(".") ? threeModulePath : `./${threeModulePath}`;
  const localSource = source
    .replaceAll("from 'three'", `from '${localThreeModulePath}'`)
    .replaceAll('from "three"', `from "${localThreeModulePath}"`)
    .replaceAll("from '../utils/BufferGeometryUtils.js'", "from './utils/BufferGeometryUtils.js'")
    .replaceAll('from "../utils/BufferGeometryUtils.js"', 'from "./utils/BufferGeometryUtils.js"');
  mkdirSync(dirname(destinationPath), { recursive: true });
  writeFileSync(destinationPath, localSource, "utf8");
}

copyExampleModule("examples/jsm/controls/OrbitControls.js", "OrbitControls.js");
copyExampleModule("examples/jsm/loaders/GLTFLoader.js", "GLTFLoader.js");
copyExampleModule("examples/jsm/utils/BufferGeometryUtils.js", "utils/BufferGeometryUtils.js");
copyFileSync(join(packageRoot, "LICENSE"), join(vendorRoot, "LICENSE-three.txt"));

console.log("已将 three@0.180.0、OrbitControls 和 GLTFLoader 准备到本地 vendor/。");
