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
const reports = path.join(repo, 'build/reports/art/ART-LOGO-12-HD');
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
  await fs.writeFile(path.join(reports, 'protection-before.json'), JSON.stringify({ task: 'ART-LOGO-12-HD', files }, null, 2) + '\n', { flag: 'wx' });
}

/** 一次导出各尺寸；density随尺寸增长，librsvg直接栅格化连续原稿。 */
async function exportAll() {
  await protectBefore();
  const svg = await fs.readFile(path.join(root, 'source/mod_logo_hd.svg'));
  const generated = path.join(root, 'generated');
  await fs.mkdir(generated, { recursive: true });
  const exports = [];
  for (const size of [1024, 2048, 4096]) {
    const density = 72 * size / 1024;
    const png = await sharp(svg, { density }).ensureAlpha().png({ compressionLevel: 9, adaptiveFiltering: false, palette: false }).toBuffer();
    const metadata = await sharp(png).metadata();
    if (metadata.width !== size || metadata.height !== size || metadata.channels !== 4) throw new Error('渲染尺寸或RGBA合同不符');
    const filename = `mod_logo_hd_${size}.png`;
    await fs.writeFile(path.join(generated, filename), png, { flag: 'wx' });
    exports.push({ path: 'generated/' + filename, size, density, bytes: png.length, sha256: hash(png), source_sha256: hash(svg) });
    console.log(`EXPORT READY ${size}px RGBA density=${density} bytes=${png.length} sha256=${hash(png)}`);
  }
  await fs.writeFile(path.join(reports, 'export-origin.json'), JSON.stringify({ task: 'ART-LOGO-12-HD', svg_sha256: hash(svg), node: process.version, renderer: sharp.versions, exports }, null, 2) + '\n');
  console.log('EXPORT direct-svg-render=3 source=unique legacy-write=0 mod-build=0');
}

await exportAll();
