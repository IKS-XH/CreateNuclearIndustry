# DEVICE-PONDER-01 R3 回水管路布局交付

- 基线：`f0550dda86a993697b8eaaa6631d4876d6004108`，工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。
- 范围：仅迁移回水支路、对应选择/速度方向提示和模板校验；两只粉末漏斗及接收斗坐标不变。

## 实现

- 回水改为南向直线：流体管 `(4,2,5)` 南北连通；机械泵 `(4,2,6)` 朝南；水罐 `(4,2,7)`。
- 驱动改为 `(3,2,6)` Z轴齿轮、`(3,2,7)` Z轴轴、`(3,2,8)` 朝北创造马达。按锁定 Create `6.0.10-280` 的 `PumpBlock`/`DirectionalKineticBlock`/`CreativeMotorBlock` 源码核对：泵的旋转轴由 facing 轴决定，泵开放流体端沿该轴；朝南的前侧为排液侧，与南侧回水罐相接。轴与齿轮沿 Z 轴传动，创造马达朝北输出 Z 轴动力。
- Java 展示选择、泵速度选择、罐指向和旋转方向提示已同步；回水轮廓只选回水线路及其驱动，不包含两路粉末输出；增加沿支路的蓝色流向线；旋转方向标记朝西指向新齿轮。没有新增字幕。
- 生成器校验新坐标、方块方向、管泵连接和旧东向回水坐标不存在。

## 技能

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：依仓库 Create/Ponder 接入及锁定版本核对布局和动力关系。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：按任务卡仅执行针对性模板校验与构建，未运行 JUnit/GameTest。
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：核对模板资源生成、NBT 解码及打包内容。
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/systematic-debugging/SKILL.md`：生成器首次运行暴露旧坐标断言；修正该过期校验后重跑。
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/verification-before-completion/SKILL.md`：以新鲜生成器和构建输出、JAR 内字节比对作为交付证据。
- 使用项目锁定版本：Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6；没有升级依赖。

## 验证

- `C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe tools/ponder/centrifuge_scene.py`：成功；NBT gzip 流与根 Compound 完整解码，尺寸 `[9,9,9]`，105 个坐标均为合法三整数列表且无重复/越界，配对机身、泵方向、动力构件、过滤漏斗及接收斗校验通过。生成器结果使用当时工具输出，未单独保存日志；`build/reports/extension/DEVICE-PONDER-01/layout-r3/candidate-assemble.log`仅记录首次构建，退出码`candidate-assemble.exit`为0。PM交付前另行只读解码实际NBT并确认105个合法位置、新支路和旧折线移除。
- `./gradlew.bat assemble --console=plain`：首次构建 `candidate-assemble.log` 成功（exit 0）。PM 审查发现旋转方向标记应指向新齿轮，改为朝西后执行一次增量复建，`final-assemble.log` 成功（exit 0，2项任务执行、2项 up-to-date）；两次退出码分别记录在对应 `.exit` 文件。未运行 JUnit/GameTest，也未启动游戏客户端。
- Java 基线与现稿比对：8 段 `.text(...)` 正文、8个关键帧及14项 `scene.idle(...)` 序列完全一致；8段轮廓时长数值序列仍为 `90,90,90,100,105,150,105,100`。PM复核确认上述序列与基线一致；粉末输出和反应堆教学未改。
- NBT：820 bytes，SHA-256 `cf2843746e654682789c163415892ea95c495444a558067c9cc6156e1d965f0d`。复建后成品 `build/libs/create_nuclear_industry-0.1.0.jar`：2,163,989 bytes，SHA-256 `2510f856393d4743c96c0a5d1ff3daa4a69bf34ea66996d6ceebb9d55660b494`。JAR 中的模板为 820 bytes，SHA-256 与源码模板相同，字节逐一匹配。

## 尚待人工播放

此布局仍须并入离心机原播放门，由用户检查回水与两路粉末端的实际可辨认度及关键帧表现；本报告不代表客户端验收通过。
