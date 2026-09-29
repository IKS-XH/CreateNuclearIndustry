# EXT-ART-01 像素 SVG 样稿

四张原创候选：青金石粉 `lapis_dust`、铅锭 `lead_ingot`、铅矿石 `lead_ore`、合金钢板 `steel_plate`。仅供风格评审，未覆盖 `src/` 游戏资源，未完成客户端验收。

## 导出

在本仓库根目录运行 PowerShell：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' tools/art-assets/export.py
```

本次实际环境：Python 3.12.14、Pillow 12.3.0，均来自已捆绑运行时，无新增安装。其他机器可使用已有 Python/Pillow 替换解释器绝对路径。导出器不接受参数，不联网、不读取游戏资源、不安装依赖，也不参与 Gradle 构建。

源稿位于 `sources/item/{lapis_dust,lead_ingot,steel_plate}.svg` 和 `sources/block/lead_ore.svg`；对应输出在 `generated/item/`、`generated/block/`。修改 SVG 后再次执行同一命令即可生成四张 PNG、`preview.png` 和 `preview.html`。SVG 为正式可编辑源，证据目录中的首次字符草图不是生产导出步骤，不要用它覆盖后续 SVG 修改。

预览 PNG 给出原尺寸、10 倍最近邻、明暗背景 5 倍与矿石 3×3 平铺 6 倍；HTML 可直接在本地浏览器打开，使用相对路径 PNG，不加载网络资源、脚本或外部字体。HTML 原尺寸是 16 CSS 像素，系统/浏览器缩放可能改变实际屏幕像素。预览文字使用 Pillow 内置字体；画布中的纹理只做最近邻整数倍缩放。

## 严格 SVG 子集

- 根节点必须为 SVG 命名空间的 `svg`，且恰好有 `width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges"` 四个属性。
- 根中只允许注释和直属 `rect`；每个矩形恰好有 `x/y/width/height/fill` 五个属性。坐标和尺寸为非负十进制整数，尺寸大于零，全部像素位于 16×16 内。
- `fill` 仅接受该资源在 `palette.json` 中列出的 `#RRGGBB`，大小写均可。不支持命名颜色、透明填色、渐变、描边、圆角、分组、路径、变换、样式、滤镜、嵌入位图、链接、文本或 XML 声明/处理指令、DTD/实体声明；出现即报错，不静默漏画。
- 矩形按文件顺序覆盖整数像素。空白处是 RGBA `(0,0,0,0)`，所有绘制像素 alpha 为 255，无抗锯齿；物品四周一整圈透明，矿石全不透明。所有输入通过校验后才写出固定候选。

色板：粉末 6 色蓝色；铅锭 6 色冷灰；钢板 6 色略偏青灰；铅矿石 4 色暖灰岩底 + 4 色冷灰矿点。统一左上光照；粉末靠不规则堆状颗粒，铅锭靠厚侧面，钢板靠薄边斜板区分形态。底纹与矿点均原创，无外部纹理复制、无生图模型。

## 验证与局限

本次命令与结果保存在 `build/reports/extension/EXT-ART-01.md` 及同名证据目录。检查包含 RGBA/16×16/色板/二值 alpha、独立源稿像素对照、28 类非法 SVG 拒绝及连续两次六个输出 SHA-256 一致。依赖版本固定时可复现文件字节；跨 Pillow 版本尤其预览文字的字节一致性不保证，PNG 像素合同仍可复验。

实际查看静态 PNG 后，将首版圆整矿点改为不规则嵌岩簇，并收窄钢板暗边。单张 16×16 矿石平铺必然存在周期；此稿无边框/透明缝，仍待用户判断风格及自然程度。浏览器 HTML 未做实际渲染检查；静态 PNG 查看不是 Minecraft 客户端验收。四张样稿需 PM 和用户评审后才能另派游戏接入或后续绘制。
