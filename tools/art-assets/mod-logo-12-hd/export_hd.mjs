/**
 * ART-LOGO-12-HD专属导出：只读1024 SVG，在对应密度直接渲染三个透明PNG。
 * 图形坐标只存在于SVG；不把1024位图放大为2048/4096，不安装新依赖。
 */
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const sharp = require('C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root = path.dirname(fileURLToPath(import.meta.url));
const repo = path.resolve(root, '../../..');
// R2按用户厚重反馈独立记录，旧R1导出与证据保留，不覆盖历史PNG。
const phase = process.argv[2] ?? 'r2';
if (phase !== 'r2') throw new Error('当前入口只接受已派发R2；R1工具见history/r1');
const requestedSizes = (process.argv[3] ?? '1024,2048,4096').split(',').map(Number);
if (requestedSizes.some(size => ![1024, 2048, 4096].includes(size))) throw new Error('导出尺寸未授权');
const reports = path.join(repo, 'build/reports/art/ART-LOGO-12-HD/r2');
const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const protectedPaths = [
  'tools/art-assets/mod-logo-11/source/mod_logo.svg',
  'src/main/resources/assets/create_nuclear_industry/models/item/mod_logo.json',
  'src/main/resources/assets/create_nuclear_industry/textures/item/mod_logo.png',
  'src/main/java/com/iksxh/create_nuclear_industry/content/ModItems.java',
  'src/main/java/com/iksxh/create_nuclear_industry/content/ModCreativeTabs.java',
  'build/libs/create_nuclear_industry-0.1.0.jar'
];

/** 只读旧六件并冻结本次开工身份；后续校验必须逐字保持。 */
async function protectBefore() {
  const files = [];
  for (const relative of protectedPaths) {
    const bytes = await fs.readFile(path.join(repo, relative));
    files.push({ path: relative, bytes: bytes.length, sha256: hash(bytes) });
  }
  await fs.mkdir(reports, { recursive: true });
  const target = path.join(reports, 'protection-before.json');
  try {
    const previous = JSON.parse(await fs.readFile(target, 'utf8'));
    if (JSON.stringify(previous.files) !== JSON.stringify(files)) throw new Error('R2旧件保护基线漂移');
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    await fs.writeFile(target, JSON.stringify({ task: 'ART-LOGO-12-HD', phase, files }, null, 2) + '\n', { flag: 'wx' });
  }
}

/** 一次导出各尺寸；density随尺寸增长，librsvg直接栅格化连续原稿。 */
async function exportAll() {
  await protectBefore();
  const svg = await fs.readFile(path.join(root, 'source/mod_logo_hd.svg'));
  const generated = path.join(root, 'generated/r2');
  await fs.mkdir(generated, { recursive: true });
  let exports = [];
  try {
    const previous = JSON.parse(await fs.readFile(path.join(reports, 'export-origin.json'), 'utf8'));
    if (previous.svg_sha256 !== hash(svg)) throw new Error('已交R2源稿变化，须保留原导出再处理');
    exports = previous.exports;
  } catch (error) { if (error.code !== 'ENOENT') throw error; }
  for (const size of requestedSizes) {
    const density = 72 * size / 1024;
    const png = await sharp(svg, { density }).ensureAlpha().png({ compressionLevel: 9, adaptiveFiltering: false, palette: false }).toBuffer();
    const metadata = await sharp(png).metadata();
    if (metadata.width !== size || metadata.height !== size || metadata.channels !== 4) throw new Error('渲染尺寸或RGBA合同不符');
    const filename = `mod_logo_hd_${size}.png`;
    await fs.writeFile(path.join(generated, filename), png, { flag: 'wx' });
    // 稳定公开PNG入口只覆盖已完整归档的R1原件；R2阶段原图仍单独保留。
    const publicTarget = path.join(root, 'generated', filename);
    const archivedR1 = await fs.readFile(path.join(root, 'history/r1/generated', filename));
    if (hash(await fs.readFile(publicTarget)) !== hash(archivedR1)) throw new Error('公开PNG不再是已归档R1，停止覆盖');
    await fs.writeFile(publicTarget, png);
    exports.push({ path: 'generated/r2/' + filename, size, density, bytes: png.length, sha256: hash(png), source_sha256: hash(svg) });
    console.log(`EXPORT READY ${size}px RGBA density=${density} bytes=${png.length} sha256=${hash(png)} public=stage-byte-identical`);
  }
  await fs.writeFile(path.join(reports, 'export-origin.json'), JSON.stringify({ task: 'ART-LOGO-12-HD', phase, svg_sha256: hash(svg), node: process.version, renderer: sharp.versions, exports }, null, 2) + '\n');
  console.log(`EXPORT R2 direct-svg-render=${requestedSizes.length} total_sizes=${exports.length} source=unique legacy-write=0 mod-build=0`);
}

await exportAll();
