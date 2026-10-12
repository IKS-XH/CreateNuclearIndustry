# ART-TURBINE-10B 独立候选素材

本目录实现已确认的B双列错相短叶盘与箱式端座。仅生成本目录的`candidate/`；不调用旧`turbine_models.py`、不安装正式资源、不写共享`turbine.mtl`或机壳/窗口。正式资源精确映射见`resource-map.json`。

当前为R1恢复原B材质的冻结候选；原来源/映射/候选/安装身份保存在`build/reports/art/ART-TURBINE-10B/assets/r1/pre-r1/`。R1使用`check_r1.py`核必要色源/UV/切分与旧几何保护，旧`check_assets.py`的完整矩阵仅属于第一阶段旧证据，不对R1重复运行。原17份旧源备份保留，R1生产安装需负责人另批准变化名单。

使用包含Pillow的Python运行：

```powershell
python tools/art-assets/turbine-rotor-bearing-art-10b/produce.py
python tools/art-assets/turbine-rotor-bearing-art-10b/check_r1.py
python -m http.server 8766 --bind 127.0.0.1 --directory tools/art-assets/turbine-rotor-bearing-art-10b
```

打开`http://127.0.0.1:8766/`。预览实际读取10份候选OBJ、1份同算法旧厚度对照OBJ、专属MTL及4份PNG；不以另画的概念几何替代候选。前/后外侧、前/后内侧、三档整组、薄厚比较和未成型完整单件可切换；旋转/停止与拆件仅为离线演示。Canvas按OBJ绕序做背面剔除与深度排序，相机side/end/inside沿用10A/R1，后端视角为对应反向观察。

`sources/textures/`保存4份16×16整数rect限色SVG；`produce.py`直接按这些rect的像素覆盖顺序生成RGBA PNG。`check_assets.py`独立解析实际候选引用、OBJ组闭合边/有向绕序/法向/正体积、三档扫掠、24叶片及15°错相、厚度/中心、孔径/孔壁、固定旋转拆件和SVG/PNG逐像素一致。它仅只读核两份旧输出轴的Z边界，不扫描旧资产全库。

R1叶片/外板源逐字复制原`B_rotor.svg/B_bearing.svg`，原12/10色保持；钢PNG是原B17个纯钢色的采样表，铜PNG为原`#b27a4e`，不把色块表拉满实体。叶片复用原1格纹理投影，只在必要V=0及D5/D7的U=1共面切分以把sprite UV限制在0..1；其余实体位置/法向保持。预览与锁定NeoForge的`flip_v=false`一致，直接取V；纯色完整face一次填色，同色共面内部边补亚像素覆盖，纹理quad一次仿射提交，避免Canvas裁剪与三角分解造成假线框。参照/生成/检查与当前候选身份见`docs/art/reports/ART-TURBINE-10B-R1-ASSETS.md`。

`comparison/`恢复旧稿实际0.34格厚度，其他参数复用同一独立网格算法；对照不进入资源映射。正式叶片每列0.125格，中心0.30/0.70；D3轮毂外R0.52，D5/D7外形比例放大，真实轴孔R0.155及轴R0.14保持。候选所有静态/动态几何使用localZ和(.5,.5)中心，由客户端消费者处理MACHINE_FACING与运行相位。

冻结后不要为展示或安装重新执行producer；安装须等美术负责人补充精确名单，逐项使用现有candidate和映射SHA256。客户端/Gradle/存档不属于本素材工具职责。
