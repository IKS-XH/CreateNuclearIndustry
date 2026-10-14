param([switch]$Reviewed)
$ErrorActionPreference = 'Stop'
if (-not $Reviewed) { throw '须负责人GPU实看及原同次组合审查无必改后，才执行本批唯一assemble' }
$assembly15CWorkspace = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../../..'))
$assembly15CClient = Join-Path $assembly15CWorkspace 'build/reports/art/ART-SHIELDED-ASSEMBLY-15C/client'
$assembly15CJar = Join-Path $assembly15CWorkspace 'build/libs/create_nuclear_industry-0.1.0.jar'
$assembly15CBackup = Join-Path $assembly15CClient 'backup/create_nuclear_industry-0.1.0-602985f8.jar'
$assembly15CBaseSha = '602985f85d2522c7914d7a4cef0b1c822f8c50ce11687e60af2be697428e3cd3'
foreach ($assembly15CFile in @($assembly15CJar,$assembly15CBackup)) {
    if ((Get-Item -LiteralPath $assembly15CFile).Length -ne 2812546 -or (Get-FileHash -LiteralPath $assembly15CFile -Algorithm SHA256).Hash.ToLower() -ne $assembly15CBaseSha) { throw '602当前包或精确备份身份不符，禁止覆盖' }
}
$assembly15CHttp = Get-Content -LiteralPath (Join-Path $assembly15CClient '../preview/http-identity.json') -Raw | ConvertFrom-Json
if (-not $assembly15CHttp.passed) { throw '本批HTTP/同源姿态未通过' }
$assembly15CIdentity = Get-Content -LiteralPath (Join-Path $assembly15CClient 'pre-review-identity.json') -Raw | ConvertFrom-Json
foreach ($assembly15CEntry in $assembly15CIdentity.sources) {
    if ((Get-FileHash -LiteralPath (Join-Path $assembly15CWorkspace $assembly15CEntry.path) -Algorithm SHA256).Hash.ToLower() -ne $assembly15CEntry.sha256) { throw "已审来源漂移 $($assembly15CEntry.path)" }
}
if (Test-Path -LiteralPath (Join-Path $assembly15CClient 'assemble-attempt.json')) { throw '本批已有真实assemble尝试，不机械重跑' }
$assembly15CTemp = (Join-Path $assembly15CClient 'tmp').Replace('\','/')
$assembly15CArgs = @('assemble','--no-daemon',"-Djava.io.tmpdir=$assembly15CTemp","-Dorg.gradle.jvmargs=-Xmx3G -Djava.io.tmpdir=$assembly15CTemp")
@{task='ART-SHIELDED-ASSEMBLY-15C';cwd=$assembly15CWorkspace;command='./gradlew.bat';args=$assembly15CArgs;backup=$assembly15CBackup;baselineSha256=$assembly15CBaseSha;processTemp=$assembly15CTemp;cFree=(Get-PSDrive -Name C).Free;eFree=(Get-PSDrive -Name E).Free;reviewed=$true;cache='原缓存和系统环境保持'} | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $assembly15CClient 'assemble-attempt.json') -Encoding utf8
$assembly15COldTemp = $env:TEMP
$assembly15COldTmp = $env:TMP
$assembly15CStart = Get-Date
$assembly15CExit = -1
# 只本次构建进程树使用E盘tmp；退出恢复进程环境，不改系统/全局Gradle配置。
try {
    $env:TEMP = $assembly15CTemp
    $env:TMP = $assembly15CTemp
    Push-Location -LiteralPath $assembly15CWorkspace
    try { & './gradlew.bat' @assembly15CArgs *> (Join-Path $assembly15CClient 'assemble.log'); $assembly15CExit = $LASTEXITCODE } finally { Pop-Location }
} finally {
    $env:TEMP = $assembly15COldTemp
    $env:TMP = $assembly15COldTmp
    $assembly15CExit | Set-Content -LiteralPath (Join-Path $assembly15CClient 'assemble.exit') -Encoding utf8
    @{exit=$assembly15CExit;elapsedSeconds=((Get-Date)-$assembly15CStart).TotalSeconds;jarBytes=(Get-Item -LiteralPath $assembly15CJar).Length;jarSha256=(Get-FileHash -LiteralPath $assembly15CJar -Algorithm SHA256).Hash.ToLower();backupSha256=(Get-FileHash -LiteralPath $assembly15CBackup -Algorithm SHA256).Hash.ToLower()} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $assembly15CClient 'assemble-result.json') -Encoding utf8
}
Get-Content -LiteralPath (Join-Path $assembly15CClient 'assemble.log') -Tail 28
if ($assembly15CExit -ne 0) { throw "15C真实assemble失败exit=$assembly15CExit，保日志及602原包，不重复或手工造包" }
