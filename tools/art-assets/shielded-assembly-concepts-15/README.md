# 屏蔽装配室三套静态设计稿预览

本页属于 ART-SHIELDED-ASSEMBLY-15A，只读本批 `assets/concepts.json`、三套四件JSON及九槽16² SVG。SVG只在浏览器内存转纹理；未生成正式PNG、安装资源或制作加工循环。

使用现成本地 `E:/MyMC/NewMod/block-model-renderer` 2.20.0 与主目录 `tools/nuclear-plant-3d/vendor` Three 0.180.0，不安装或升级依赖。全部手写实现说明为中文。

```powershell
# 在本工具目录运行；参考已准备时无需重复抽取。
pwsh -NoProfile -File preview/prepare-refs.ps1
node server.mjs 4193
```

服务只监听 `127.0.0.1`，开放本页、a/b/c候选、指定Create参考、指定模型库与原机资源。Windows后台服务使用Hidden窗口。`/health`记录本批PID、运行文件及cwd。4191/4192等既有服务保持。

三稿同相机、尺寸、照明，可同步拖动/缩放、正背/侧面/俯底、机壳剖视或隐藏、灯off/on与重置。机构保原待机位置。原机按钮读取真实八格off/on模型、三partial和十张PNG；八格按 `part=x+2*z+4*y` 每格加16单位，partial不再偏移。原机仅作只读比较。

模型单位16=1格，整机0..32；浏览器坐标为 `[mx/16-1,my/16,mz/16-1]`。观察相机围绕整机中心，真实朝向pivot仍为主控格 `x=z=8`。底半轴使用锁定Create JAR的 `AllPartialModels.SHAFT_HALF -> create:block/shaft_half`，保留轴PNG与Mojang父模型；按实际CachedBuffers DOWN映射主控中心X+90°，轴包络 `[6,0,6]..[10,8,10]`，底接触点 `[8,0,8]`。不挪到整机中心或改变长度。

`preview/refs/reference.json`记录JAR/条目SHA、字段、DOWN映射和第三方身份；Create元数据许可证声明指向附件LICENSE.md，该slim JAR未含附件文本。参考仅供本地预览，未作为CNI原创资产或正式安装资源。字节码原证据位于本批 `build/reports/art/ART-SHIELDED-ASSEMBLY-15A/preview/`。

保存按钮仅写本批固定 `a-latest.png`、`b-latest.png`、`c-latest.png`及 `browser-evidence.json`，要求完整三图、有效PNG和本页Origin。无任意输出路径。保存的JSON含浏览器输入SHA、当前相机/剖视/灯状态、材质及draw-call。

本批有限语法、HTTP与输入身份核对不能代替GPU实看或Minecraft验收。GPU实看与截图由负责人完成；本批等待用户选稿。

## 15B正式安装后的原机来源保留（2026-10-14）

用户已选C并进入15B。15A三稿模型/SVG/scene/index和历史capture保持；server仅将原机JSON精确重定向到 `../shielded-assembly-art-15b/assets/backup/src/main/resources/` 的完整路径原字节备份，原机PNG继续原生产路径。15B未覆盖旧PNG。4193只在核验原自有PID18084/executable/cwd后由原执行者重启，更新身份记在15B报告，不改15A历史服务/检查记录。三稿与原机对照继续可读。

自动审批拒绝上述服务命令，4193/PID18084仍运行旧server代码，磁盘重定向尚未加载。按15B卡后续安全补充，不再停止或重启此服务；index仅将原机按钮禁用并说明“旧原机模型已存档”。三稿模型/SVG/scene、其他UI与历史capture保持。20模型完整备份可离线追溯，禁用按钮避免安装后对照错读新资源；旧4193仍供三稿浏览。
