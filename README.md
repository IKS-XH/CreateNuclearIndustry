# Create: Nuclear Industry

Minecraft 1.21.1 + NeoForge + Create 6.0.10 的首个可运行样例。当前内容包括独立创造模式标签页和可放置的“实验反应堆外壳”。

## 环境

- JDK 21（当前电脑：`C:\Program Files\jdk-21.0.12.8-hotspot`）
- NeoForge 21.1.219（满足 Create 6.0.10 的最低版本要求）
- Gradle 9.0.0 Wrapper；正常开发请优先使用仓库中的 `gradlew.bat`
- Visual Studio Code + Microsoft `Extension Pack for Java`、`Gradle for Java`

仓库中的 `.vscode/settings.json` 已将 Java Runtime 指向当前电脑的 JDK 21。如果以后更换 JDK 安装位置，请同步修改其中两个路径。

## 在 VS Code 中打开

1. 使用“文件 → 打开文件夹”打开本仓库根目录。
2. 根据扩展推荐安装 Java 与 Gradle 扩展。
3. 等待右下角 Java/Gradle 项目导入结束。
4. 打开 VS Code 集成终端执行以下命令。

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
```

如果首次运行 Wrapper 时网络无法下载 Gradle，而本机 Gradle 9 已经可用，可以临时执行：

```powershell
gradle runClient
```

## 游戏内检查

1. 等待开发客户端进入 Minecraft 主菜单。
2. 打开“模组”页面，确认存在 Create、Ponder/Flywheel 依赖和 Create: Nuclear Industry。
3. 创建或进入开启作弊的创造模式世界。
4. 打开创造模式物品栏，进入“机械动力：工业核电”标签页。
5. 取出“实验反应堆外壳”，确认可以放置，并能用铁镐或更高级镐正确采集。

## 构建产物

执行 ` .\gradlew.bat clean build ` 后，可发布 JAR 位于：

```text
build/libs/create_nuclear_industry-0.1.0.jar
```

当前样例尚未实现反应堆状态机、方块实体、GUI、流体、辐射或 Create 应力输出；这些将在后续垂直切片中逐步加入。
