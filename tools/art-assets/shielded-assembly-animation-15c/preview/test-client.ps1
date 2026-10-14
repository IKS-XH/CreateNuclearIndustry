param([Parameter(Mandatory=$true)][string]$Stage, [switch]$ClockOnly, [switch]$PoseOnly, [string]$ClockMethod, [string]$PoseMethod)
$ErrorActionPreference = 'Stop'
$assembly15CRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../../..'))
$assembly15CReport = Join-Path $assembly15CRoot "build/reports/art/ART-SHIELDED-ASSEMBLY-15C/client/$Stage"
if (Test-Path -LiteralPath (Join-Path $assembly15CReport 'test-attempt.json')) { throw '此阶段已真实运行；保留日志，不覆盖或机械重跑' }
New-Item -ItemType Directory -Path $assembly15CReport,(Join-Path $assembly15CRoot 'build/reports/art/ART-SHIELDED-ASSEMBLY-15C/client/tmp') -Force | Out-Null
$assembly15CTemp = (Join-Path $assembly15CRoot 'build/reports/art/ART-SHIELDED-ASSEMBLY-15C/client/tmp').Replace('\','/')
$assembly15CArgs = @('test','--tests','com.iksxh.create_nuclear_industry.production.ShieldedAssemblyAnimationClockTest')
if ($ClockMethod) {
    if ($ClockMethod -notmatch '^[A-Za-z]+$') { throw '只能选择本批Clock专属方法' }
    $assembly15CArgs[2] += ".$ClockMethod"
    $ClockOnly = $true
}
if (-not $ClockOnly) { $assembly15CArgs += @('--tests','com.iksxh.create_nuclear_industry.production.ShieldedAssemblyAnimationTest') }
if ($PoseOnly) { $assembly15CArgs = @('test','--tests','com.iksxh.create_nuclear_industry.production.ShieldedAssemblyAnimationTest') }
if ($PoseMethod) {
    if ($PoseMethod -notmatch '^[A-Za-z]+$') { throw '只能选择本批Pose专属方法' }
    $assembly15CArgs = @('test','--tests',"com.iksxh.create_nuclear_industry.production.ShieldedAssemblyAnimationTest.$PoseMethod")
}
$assembly15CArgs += @('--no-daemon',"-Djava.io.tmpdir=$assembly15CTemp","-Dorg.gradle.jvmargs=-Xmx3G -Djava.io.tmpdir=$assembly15CTemp")
@{task='ART-SHIELDED-ASSEMBLY-15C';stage=$Stage;cwd=$assembly15CRoot;command='./gradlew.bat';args=$assembly15CArgs;processTemp=$assembly15CTemp} | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $assembly15CReport 'test-attempt.json') -Encoding utf8
$assembly15COldTemp = $env:TEMP
$assembly15COldTmp = $env:TMP
$assembly15CExit = -1
$assembly15CStart = Get-Date
# 临时目录仅作用于此测试进程树；原Gradle缓存、系统环境与其它服务保持。
try {
    $env:TEMP = $assembly15CTemp
    $env:TMP = $assembly15CTemp
    Push-Location -LiteralPath $assembly15CRoot
    try { & './gradlew.bat' @assembly15CArgs *> (Join-Path $assembly15CReport 'test.log'); $assembly15CExit = $LASTEXITCODE } finally { Pop-Location }
} finally {
    $env:TEMP = $assembly15COldTemp
    $env:TMP = $assembly15COldTmp
    $assembly15CExit | Set-Content -LiteralPath (Join-Path $assembly15CReport 'test.exit') -Encoding utf8
    @{exit=$assembly15CExit;elapsedSeconds=((Get-Date)-$assembly15CStart).TotalSeconds} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $assembly15CReport 'test-result.json') -Encoding utf8
    foreach ($assembly15CTest in @('ShieldedAssemblyAnimationClockTest','ShieldedAssemblyAnimationTest')) {
        $assembly15CXml = Join-Path $assembly15CRoot "build/test-results/test/TEST-com.iksxh.create_nuclear_industry.production.$assembly15CTest.xml"
        if (Test-Path -LiteralPath $assembly15CXml) { Copy-Item -LiteralPath $assembly15CXml -Destination $assembly15CReport }
    }
}
Get-Content -LiteralPath (Join-Path $assembly15CReport 'test.log') -Tail 30
exit $assembly15CExit
