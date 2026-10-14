# EXT-B-TURBINE-01D-R1 端口密封修复

根因：基线把方孔薄板放在壳体内侧，外向圆面只覆盖圆盘投影；圆盘与方孔边角间的射线会进入壳内，内侧封面法线又背向外视线。

修复：方形密封安装面移至壳体外表面，厚3/16格；外向圆面保持中心材质标识；圆管朝机内，最大内伸9/32格。方环使用连续全局UV，中央箭头独立完整映射。

验证：旧资源289条外向视线中有 24 条透空，另有 168 条要深入超过3/16格才遇到外向可见面；新模型覆盖 22 个进排汽口模型，每个外面采样17×17射线；UV保持面内全局投影。D3/D5/D7按壳外至转子扫掠边界0.3125格，短管内伸0.28125格，三档净空均为0.03125格。

组合预览：`build/reports/extension/EXT-B-TURBINE-01D-R1/R1-adjacent-shell-port-preview.png`，采用正式OBJ、MTL贴图和背面剔除，包含成型进汽口与成型排汽口的外视、内视。

资源审计（本轮已运行）：

```powershell
& 'C:\Users\IKSXH\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' -B -c "import importlib.util,json; p='tools/art-assets/turbine_01d_checks.py'; s=importlib.util.spec_from_file_location('checks',p); m=importlib.util.module_from_spec(s); s.loader.exec_module(m); print(json.dumps(m.audit_resources(),ensure_ascii=False,indent=2))"
```

退出码为 0。返回 294 个模型 JSON、294 个 OBJ 及 294 个包装引用；8739 个面绕序法线、8631 个实体面、12 种材质贴图引用全部通过；轴承净空、内向圆盖、圆端 UV、MTL 引用、贴图路径和显式法线绕序均通过。

实际读取并应用了 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack` 与 `systematic-debugging` 技能。此资源修复未运行 JUnit、GameTest、Gradle 或 Minecraft 客户端；客户端画面仍待用户实机确认。
