param([switch]$Reviewed)
$ErrorActionPreference = 'Stop'
if (-not $Reviewed) { throw '须先完成R1原生层诊断、最小安装及同次组合审查，再由负责人明确调用本批唯一assemble' }
$assemblyBRoot = Split-Path -Parent $PSScriptRoot
$assemblyWorkspace = [IO.Path]::GetFullPath((Join-Path $assemblyBRoot '../../..'))
$assemblyClientReport = Join-Path $assemblyWorkspace 'build/reports/art/ART-SHIELDED-ASSEMBLY-15B-R1/client'
$assemblyManifest = Get-Content -LiteralPath (Join-Path $assemblyBRoot 'assets.json') -Raw | ConvertFrom-Json
if ($assemblyManifest.renderLayerR1.task -ne 'ART-SHIELDED-ASSEMBLY-15B-R1' -or $assemblyManifest.renderLayerR1.changedTargets.Count -lt 1) { throw '缺少已审R1变更契约' }
foreach ($assemblyTarget in $assemblyManifest.renderLayerR1.changedTargets) {
    $assemblyEntry = $assemblyManifest.productionMappings | Where-Object {$_.target -eq $assemblyTarget}
    if ($null -eq $assemblyEntry -or (Get-FileHash -LiteralPath (Join-Path $assemblyWorkspace $assemblyEntry.target) -Algorithm SHA256).Hash.ToLower() -ne $assemblyEntry.sha256) { throw 'R1批准模型未同源安装，禁止构建' }
}
$assemblyOriginalJar = Join-Path $assemblyWorkspace 'build/libs/create_nuclear_industry-0.1.0.jar'
$assemblyOldSha = 'a92a0bed0f948fc000b58800cad34f84c03ffb314b167fea3a839f117b12ff7f'
if ((Get-Item -LiteralPath $assemblyOriginalJar).Length -ne 2812280 -or (Get-FileHash -LiteralPath $assemblyOriginalJar -Algorithm SHA256).Hash.ToLower() -ne $assemblyOldSha) { throw 'a92故障原包身份不同，禁止覆盖或构建' }
New-Item -ItemType Directory -Path $assemblyClientReport,(Join-Path $assemblyClientReport 'backup'),(Join-Path $assemblyClientReport 'tmp') -Force | Out-Null
$assemblyBackupJar = Join-Path $assemblyClientReport 'backup/create_nuclear_industry-0.1.0-a92a0bed.jar'
if (-not (Test-Path -LiteralPath $assemblyBackupJar)) { Copy-Item -LiteralPath $assemblyOriginalJar -Destination $assemblyBackupJar }
if ((Get-FileHash -LiteralPath $assemblyBackupJar -Algorithm SHA256).Hash.ToLower() -ne $assemblyOldSha) { throw '原包备份身份错误' }
if (Test-Path -LiteralPath (Join-Path $assemblyClientReport 'assemble-attempt.json')) { throw '本批已有assemble记录，禁止机械重复构建' }
$assemblyTemp = (Join-Path $assemblyClientReport 'tmp').Replace('\','/')
$assemblyArgs = @('assemble','--no-daemon',"-Djava.io.tmpdir=$assemblyTemp","-Dorg.gradle.jvmargs=-Xmx3G -Djava.io.tmpdir=$assemblyTemp")
@{task='ART-SHIELDED-ASSEMBLY-15B-R1'; cwd=$assemblyWorkspace; command='./gradlew.bat'; args=$assemblyArgs; oldJar=$assemblyOriginalJar; backupJar=$assemblyBackupJar; oldSha256=$assemblyOldSha; changedTargets=$assemblyManifest.renderLayerR1.changedTargets; processTemp=$assemblyTemp; cFree=(Get-PSDrive -Name C).Free; eFree=(Get-PSDrive -Name E).Free; cache='保持原Gradle缓存，不移动/清理'; reviewed=$true} | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $assemblyClientReport 'assemble-attempt.json') -Encoding utf8
$assemblyOldTemp = $env:TEMP
$assemblyOldTmp = $env:TMP
$assemblyStart = Get-Date
$assemblyExit = -1
# 仅本次PowerShell及其构建子进程继承E盘临时目录；finally恢复，不改注册表或全局环境。
try {
    $env:TEMP = $assemblyTemp
    $env:TMP = $assemblyTemp
    Push-Location -LiteralPath $assemblyWorkspace
    try { & './gradlew.bat' @assemblyArgs *> (Join-Path $assemblyClientReport 'assemble.log'); $assemblyExit = $LASTEXITCODE } finally { Pop-Location }
} finally {
    $env:TEMP = $assemblyOldTemp
    $env:TMP = $assemblyOldTmp
    $assemblyExit | Set-Content -LiteralPath (Join-Path $assemblyClientReport 'assemble.exit') -Encoding utf8
    @{exit=$assemblyExit; elapsedSeconds=((Get-Date)-$assemblyStart).TotalSeconds; originalBackupSha256=(Get-FileHash -LiteralPath $assemblyBackupJar -Algorithm SHA256).Hash.ToLower(); jarSha256=(Get-FileHash -LiteralPath $assemblyOriginalJar -Algorithm SHA256).Hash.ToLower(); jarBytes=(Get-Item -LiteralPath $assemblyOriginalJar).Length} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $assemblyClientReport 'assemble-result.json') -Encoding utf8
}
if ($assemblyExit -ne 0) { Get-Content -LiteralPath (Join-Path $assemblyClientReport 'assemble.log') -Tail 45; throw "真实R1 assemble失败exit=$assemblyExit，保留完整日志与a92故障原包，不手工造JAR" }
Write-Output "R1唯一增量assemble退出0，用时$([math]::Round(((Get-Date)-$assemblyStart).TotalSeconds,3))秒；a92故障原包已备份。"
