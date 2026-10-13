// 本工具仅生成宣传封面；不读取或写入游戏世界。默认输出品牌草案，正式模式必须绑定真实背景PNG。
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const deps = 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules';
const sharp = require(path.join(deps, 'sharp'));
const { GlobalFonts, createCanvas, SvgExportFlag } = require(path.join(deps, '@napi-rs/canvas'));
const JSZip = require(path.join(deps, 'jszip'));
const root = __dirname;
const bundledLogo = path.join(root, 'sources/mod_logo_hd.svg');
const logoPath = fs.existsSync(bundledLogo) ? bundledLogo : path.resolve(root, '../mod-logo-12-hd/source/mod_logo_hd.svg');
const fontPath = 'C:/Windows/Fonts/msyhbd.ttc';
const sha = data => crypto.createHash('sha256').update(data).digest('hex');
const expectedLogo = 'cd763c08bf7f29ce21aef613230fd9dc37c06b826a5dbd2f7ec776e862b16c2c';
const logo = fs.readFileSync(logoPath);
if (sha(logo) !== expectedLogo) throw new Error('Logo来源与任务绑定不一致');
if (!GlobalFonts.registerFromPath(fontPath, 'Cover YaHei')) throw new Error('无法注册本机微软雅黑粗体');
const titles = [
  { text: '机械动力：', x: 104, y: 748, size: 136, color: '#F0F0E6' },
  { text: '核工业', x: 104, y: 910, size: 136, color: '#F0F0E6' },
  { text: 'Create:', x: 110, y: 1036, size: 64, color: '#A8CDD8' },
  { text: 'Nuclear Industry', x: 110, y: 1123, size: 64, color: '#A8CDD8' },
];
// 使用已注册系统字体生成轮廓；只分发路径和文字源，不分发字体文件。
function titleMarkup(outlined) {
  if (!outlined) return titles.map(t => `<text x="${t.x}" y="${t.y}" font-family="Microsoft YaHei" font-weight="700" font-size="${t.size}" fill="${t.color}">${t.text}</text>`).join('\n');
  const canvas = createCanvas(1920, 1200, SvgExportFlag.ConvertTextToPaths);
  const ctx = canvas.getContext('2d');
  for (const t of titles) { ctx.font = `${t.size}px "Cover YaHei"`; ctx.fillStyle = t.color; ctx.fillText(t.text, t.x, t.y); }
  return canvas.getContent().toString().replace(/^[\s\S]*?<svg[^>]*>/, '').replace(/<\/svg>\s*$/, '');
}
function composition(background, outlined) {
  const reactor = background ? `<image id="reactor-render" x="755" y="72" width="1140" height="1070" preserveAspectRatio="xMidYMid meet" href="data:image/png;base64,${background.toString('base64')}"/>` : '';
  const nestedLogo = logo.toString().replace('<svg ', '<svg x="123" y="14" ').replace('width="1024" height="1024"', 'width="586" height="586"');
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1920" height="1200" viewBox="0 0 1920 1200">
<title>机械动力：核工业 — Create: Nuclear Industry</title>
<desc>${background ? '真实5×5×5模型渲染背景；原R2 Logo矢量；系统字体标题轮廓。不是游戏截图。' : '品牌排版草案；未嵌入反应堆背景，不是最终交付。'}</desc>
<defs><linearGradient id="cover-base" x2="1" y2="1"><stop stop-color="#111B24"/><stop offset=".6" stop-color="#202D35"/><stop offset="1" stop-color="#10191F"/></linearGradient><radialGradient id="cover-light" cx=".78" cy=".43" r=".65"><stop stop-color="#235465" stop-opacity=".25"/><stop offset="1" stop-color="#13242C" stop-opacity="0"/></radialGradient></defs>
<rect width="1920" height="1200" fill="url(#cover-base)"/><rect width="1920" height="1200" fill="url(#cover-light)"/>
<path d="M 845 0 L 520 1200 M 1870 0 L 1545 1200" stroke="#68808A" stroke-opacity=".055" stroke-width="36"/>
<path d="M 52 52 H 1868 M 52 1148 H 1868" stroke="#754832" stroke-opacity=".75" stroke-width="4"/>
${reactor}
${nestedLogo}
<path d="M 110 573 H 704" stroke="#B87949" stroke-width="4"/>
${titleMarkup(outlined)}
</svg>`;
}
async function main() {
  const final = process.argv.includes('--final');
  const bgArg = process.argv.find(a => a.startsWith('--background='));
  if (final && !bgArg) throw new Error('正式导出必须提供已交付背景');
  const backgroundPath = bgArg ? path.resolve(bgArg.slice(13)) : null;
  const background = backgroundPath ? fs.readFileSync(backgroundPath) : null;
  if (final && sha(background) !== 'c4493e3b6953478e0361aa3afb39c92a1be9ff8da598778eff8d9c39bebca249') throw new Error('背景与本批冻结交付身份不一致');
  const dir = path.join(root, final ? 'output' : 'draft');
  fs.mkdirSync(dir, { recursive: true });
  const editable = composition(background, false);
  const source = composition(background, true);
  fs.writeFileSync(path.join(dir, final ? 'mcmod-cover-editable.svg' : 'brand-layout-editable.svg'), editable);
  fs.writeFileSync(path.join(dir, final ? 'mcmod-cover.svg' : 'brand-layout.svg'), source);
  const sizes = final ? [[1920, 1200], [960, 600], [240, 150]] : [[960, 600], [240, 150]];
  const exported = [];
  for (const [width, height] of sizes) {
    const name = `${final ? 'mcmod-cover' : 'brand-layout'}-${width}x${height}.png`;
    const data = await sharp(Buffer.from(source)).resize(width, height).png().toBuffer();
    fs.writeFileSync(path.join(dir, name), data);
    exported.push({ name, width, height, sha256: sha(data) });
  }
  const manifest = { stage: final ? 'final-candidate' : 'brand-layout-draft', names: ['机械动力：核工业', 'Create: Nuclear Industry'], logoSource: logoPath, logoSha256: sha(logo), font: { path: fontPath, sha256: sha(fs.readFileSync(fontPath)), redistribution: false }, background: background ? { path: backgroundPath, sha256: sha(background) } : null, exported };
  fs.writeFileSync(path.join(dir, 'manifest.json'), JSON.stringify(manifest, null, 2));
  if (final) {
    // ZIP引用全部内嵌；原Logo复制保持逐字身份，并排除草案和系统字体。
    const zip = new JSZip();
    for (const name of fs.readdirSync(dir)) zip.file(`output/${name}`, fs.readFileSync(path.join(dir, name)));
    zip.file('sources/mod_logo_hd.svg', logo);
    zip.file('sources/reactor-render.png', background);
    zip.file('export-cover.cjs', fs.readFileSync(__filename));
    zip.file('verify-cover.cjs', fs.readFileSync(path.join(root, 'verify-cover.cjs')));
    zip.file('README.md', fs.readFileSync(path.join(root, 'README.md')));
    const data = await zip.generateAsync({ type: 'nodebuffer', compression: 'DEFLATE' });
    fs.writeFileSync(path.join(root, 'mcmod-cover-13.zip'), data);
    manifest.zip = { bytes: data.length, sha256: sha(data), entries: Object.keys(zip.files) };
  }
  console.log(JSON.stringify(manifest, null, 2));
}
main().catch(e => { console.error(e); process.exitCode = 1; });
