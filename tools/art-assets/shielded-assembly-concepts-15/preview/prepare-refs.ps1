$ErrorActionPreference = 'Stop'
$assemblyPreviewRoot = Split-Path -Parent $PSScriptRoot
$assemblyPreviewReport = [IO.Path]::GetFullPath((Join-Path $assemblyPreviewRoot '../../../build/reports/art/ART-SHIELDED-ASSEMBLY-15A/preview'))
$assemblyCreateJar = 'C:/Users/IKSXH/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280/92471e8fc5ed4e3c2279001d77ebcec6fd38bcab/create-1.21.1-6.0.10-280-slim.jar'
$assemblyPonderJar = 'C:/Users/IKSXH/.gradle/caches/modules-2/files-2.1/net.createmod.ponder/ponder-neoforge/1.0.82+mc1.21.1/b10a04f40b9d6d33c346fb38df33f10f7e51b713/ponder-neoforge-1.0.82+mc1.21.1.jar'
$assemblyMinecraftJar = 'C:/Users/IKSXH/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar'
$assemblyRefRoot = Join-Path $PSScriptRoot 'refs'
New-Item -ItemType Directory -Path $assemblyRefRoot,$assemblyPreviewReport -Force | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$assemblyRefEntries = @()
# 仅逐条抽取本批实际依赖，目标始终在新preview/refs内，禁止整包解压。
foreach ($assemblySource in @(
    @{jar=$assemblyCreateJar; entries=@('assets/create/models/block/shaft_half.json','assets/create/textures/block/axis.png','assets/create/textures/block/axis_top.png','META-INF/neoforge.mods.toml')},
    @{jar=$assemblyMinecraftJar; entries=@('assets/minecraft/models/block/block.json')}
)) {
    $assemblyZip = [IO.Compression.ZipFile]::OpenRead($assemblySource.jar)
    try {
        foreach ($assemblyEntryName in $assemblySource.entries) {
            $assemblyEntry = $assemblyZip.GetEntry($assemblyEntryName)
            if ($null -eq $assemblyEntry) { throw "缺少实际依赖：$($assemblySource.jar)!/$assemblyEntryName" }
            $assemblyOutput = [IO.Path]::GetFullPath((Join-Path $assemblyRefRoot $assemblyEntryName))
            if (-not $assemblyOutput.StartsWith([IO.Path]::GetFullPath($assemblyRefRoot) + [IO.Path]::DirectorySeparatorChar)) { throw '目标超出refs白名单' }
            New-Item -ItemType Directory -Path (Split-Path -Parent $assemblyOutput) -Force | Out-Null
            $assemblyInputStream = $assemblyEntry.Open()
            $assemblyOutputStream = [IO.File]::Create($assemblyOutput)
            try { $assemblyInputStream.CopyTo($assemblyOutputStream) } finally { $assemblyInputStream.Dispose(); $assemblyOutputStream.Dispose() }
            $assemblyRefEntries += @{archive=$assemblySource.jar; archiveSha256=(Get-FileHash -LiteralPath $assemblySource.jar -Algorithm SHA256).Hash.ToLower(); entry=$assemblyEntryName; bytes=(Get-Item -LiteralPath $assemblyOutput).Length; sha256=(Get-FileHash -LiteralPath $assemblyOutput -Algorithm SHA256).Hash.ToLower(); url='/preview/refs/' + $assemblyEntryName}
        }
    } finally { $assemblyZip.Dispose() }
}
# 保存锁定依赖的映射原证据；DOWN不是凭印象选择的轴向。
$assemblyJavap = 'C:/Program Files/Java/jdk-21/bin/javap.exe'
& $assemblyJavap -classpath $assemblyCreateJar -c -p com.simibubi.create.AllPartialModels | Set-Content -LiteralPath (Join-Path $assemblyPreviewReport 'create-partials-bytecode.txt') -Encoding utf8
if ($LASTEXITCODE -ne 0) { throw 'Create字节码读取失败' }
& $assemblyJavap -classpath $assemblyPonderJar -c -p net.createmod.catnip.render.CachedBuffers net.createmod.catnip.math.AngleHelper | Set-Content -LiteralPath (Join-Path $assemblyPreviewReport 'partial-facing-bytecode.txt') -Encoding utf8
if ($LASTEXITCODE -ne 0) { throw 'Catnip映射读取失败' }
$assemblyReference = @{
    task='ART-SHIELDED-ASSEMBLY-15A'; schemaVersion=1; entries=$assemblyRefEntries
    shaft=@{id='create:block/shaft_half'; field='AllPartialModels.SHAFT_HALF'; sourceFacing='SOUTH'; facing='DOWN'; rotationDegrees=@(90,0,0); rotationPivotModel=@(8,8,8); contactModel=@(8,0,8); boundsModel=@(@(6,0,6),@(10,8,10)); sceneTranslation=@(-0.5,0.5,-0.5); scale=0.0625}
    mapping=@{archive=$assemblyPonderJar; archiveSha256=(Get-FileHash -LiteralPath $assemblyPonderJar -Algorithm SHA256).Hash.ToLower(); chain='CachedBuffers.partialFacing -> rotateToFace -> center -> rotateYDegrees(horizontalAngle) -> rotateXDegrees(verticalAngle) -> uncenter'; downHorizontal=0; downVertical=90}
    attribution=@{create='Creators of Create / simibubi'; projectUrl='https://github.com/Creators-of-Create/Create'; licenseDeclaration='META-INF/neoforge.mods.toml: Read attached LICENSE.md'; licenseText='该slim JAR未含LICENSE.md；本批只读引用原资源并保留第三方身份，未宣称CNI原创或重新许可'; minecraft='Mojang Minecraft 1.21.1原始block/block父模型；仅本地预览依赖，不安装或发布'}
}
$assemblyReference | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $assemblyRefRoot 'reference.json') -Encoding utf8
Write-Output "已抽取$($assemblyRefEntries.Count)项实际参考，保留Create身份与DOWN映射。"
