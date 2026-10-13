// 只核本批封面的尺寸、引用、名称、输入身份和ZIP；真实设备布局由background交付绑定。
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const deps = 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules';
const sharp = require(path.join(deps, 'sharp'));
const JSZip = require(path.join(deps, 'jszip'));
const root = __dirname;
const sha = data => crypto.createHash('sha256').update(data).digest('hex');
async function main() {
  const results = [];
  const check = (name, passed) => { results.push({ name, passed }); if (!passed) throw new Error(name); };
  const output = path.join(root, 'output');
  const svg = fs.readFileSync(path.join(output, 'mcmod-cover.svg'), 'utf8');
  const textSvg = fs.readFileSync(path.join(output, 'mcmod-cover-editable.svg'), 'utf8');
  const manifest = JSON.parse(fs.readFileSync(path.join(output, 'manifest.json')));
  check('正式阶段并绑定Logo R2', manifest.stage === 'final-candidate' && manifest.logoSha256 === 'cd763c08bf7f29ce21aef613230fd9dc37c06b826a5dbd2f7ec776e862b16c2c');
  check('准确中英名称', textSvg.includes('>机械动力：</text>') && textSvg.includes('>核工业</text>') && textSvg.includes('>Create:</text>') && textSvg.includes('>Nuclear Industry</text>'));
  check('成品标题全部转路径', !svg.includes('<text '));
  const refs = [...svg.matchAll(/\bhref="([^"]*)"/g)].map(m => m[1]);
  check('成品SVG全部自包含', refs.length > 0 && refs.every(ref => ref.startsWith('data:') || ref.startsWith('#')));
  const bg = refs.find(ref => ref.startsWith('data:image/png;base64,'));
  check('内嵌背景与绑定来源一致', sha(Buffer.from(bg.slice('data:image/png;base64,'.length), 'base64')) === manifest.background.sha256);
  const bgData = Buffer.from(bg.slice('data:image/png;base64,'.length), 'base64');
  const bgMeta = await sharp(bgData).metadata();
  const bgStats = await sharp(bgData).stats();
  check('背景2048方图和透明边缘', bgMeta.width === 2048 && bgMeta.height === 2048 && bgMeta.hasAlpha && bgStats.channels[3].min === 0 && bgStats.channels[3].max === 255);
  for (const item of manifest.exported) {
    const file = fs.readFileSync(path.join(output, item.name));
    const meta = await sharp(file).metadata();
    check(`${item.name}尺寸比例和身份`, meta.width === item.width && meta.height === item.height && meta.width / meta.height === 1.6 && sha(file) === item.sha256);
  }
  const zipData = fs.readFileSync(path.join(root, 'mcmod-cover-13.zip'));
  const zip = await JSZip.loadAsync(zipData);
  const entries = Object.keys(zip.files).filter(name => !zip.files[name].dir);
  check('ZIP不含草案或字体文件', entries.every(name => !name.includes('draft') && !/\.(ttf|ttc|otf)$/i.test(name)));
  for (const name of fs.readdirSync(output)) check(`ZIP逐字身份 ${name}`, (await zip.file(`output/${name}`).async('nodebuffer')).equals(fs.readFileSync(path.join(output, name))));
  check('ZIP原Logo完整身份', sha(await zip.file('sources/mod_logo_hd.svg').async('nodebuffer')) === manifest.logoSha256);
  check('ZIP背景完整身份', sha(await zip.file('sources/reactor-render.png').async('nodebuffer')) === manifest.background.sha256);
  const record = { results, count: results.length, failed: results.filter(r => !r.passed).length, zip: { bytes: zipData.length, sha256: sha(zipData), entries } };
  const evidence = path.resolve(root, '../../../build/reports/art/ART-MCMOD-COVER-13/cover');
  fs.mkdirSync(evidence, { recursive: true });
  fs.writeFileSync(path.join(evidence, 'verification.json'), JSON.stringify(record, null, 2));
  console.log(JSON.stringify(record, null, 2));
}
main().catch(e => { console.error(e); process.exitCode = 1; });
