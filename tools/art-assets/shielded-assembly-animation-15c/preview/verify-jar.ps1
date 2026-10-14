$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$assembly15CWorkspace = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../../..'))
$assembly15CClient = Join-Path $assembly15CWorkspace 'build/reports/art/ART-SHIELDED-ASSEMBLY-15C/client'
$assembly15CJar = Join-Path $assembly15CWorkspace 'build/libs/create_nuclear_industry-0.1.0.jar'
$assembly15CBackup = Join-Path $assembly15CClient 'backup/create_nuclear_industry-0.1.0-602985f8.jar'
if ((Get-FileHash -LiteralPath $assembly15CBackup -Algorithm SHA256).Hash.ToLower() -ne '602985f85d2522c7914d7a4cef0b1c822f8c50ce11687e60af2be697428e3cd3') { throw '前置精确备份错误' }
if ((Get-Content -LiteralPath (Join-Path $assembly15CClient 'assemble.exit') -Raw).Trim() -ne '0') { throw '没有本批真实成功assemble' }
function Get-Assembly15CEntryHash($assembly15CEntry) {
    $assembly15CStream = $assembly15CEntry.Open()
    try { return [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($assembly15CStream)).ToLower() } finally { $assembly15CStream.Dispose() }
}
$assembly15COldZip = [IO.Compression.ZipFile]::OpenRead($assembly15CBackup)
$assembly15CNewZip = [IO.Compression.ZipFile]::OpenRead($assembly15CJar)
$assembly15CErrors = [Collections.Generic.List[string]]::new()
$assembly15CChanged = [Collections.Generic.List[string]]::new()
$assembly15CAdded = [Collections.Generic.List[string]]::new()
$assembly15CUnchanged = 0
$assembly15CClasses = 0
$assembly15CClassSame = 0
$assembly15CProcessed = [Collections.Generic.List[object]]::new()
$assembly15CSwitchName = 'com/iksxh/create_nuclear_industry/production/ShieldedAssemblyRenderer$1.class'
$assembly15COldSwitchCode = Join-Path $assembly15CClient 'jar-first/renderer-switch-old-code.txt'
$assembly15CNewSwitchCode = Join-Path $assembly15CClient 'jar-first/renderer-switch-new-code.txt'
# 唯一生成的enum切换表必须有原/新JAR实际javap opcode一致证据；不能只按类名放宽白名单。
$assembly15CSwitchOpcodesSame = (Get-FileHash -LiteralPath $assembly15COldSwitchCode -Algorithm SHA256).Hash -eq (Get-FileHash -LiteralPath $assembly15CNewSwitchCode -Algorithm SHA256).Hash
try {
    $assembly15COldMap = @{}
    $assembly15CNewMap = @{}
    foreach ($assembly15CEntry in $assembly15COldZip.Entries) { if (-not $assembly15CEntry.FullName.EndsWith('/')) { $assembly15COldMap[$assembly15CEntry.FullName] = Get-Assembly15CEntryHash $assembly15CEntry } }
    foreach ($assembly15CEntry in $assembly15CNewZip.Entries) { if (-not $assembly15CEntry.FullName.EndsWith('/')) { $assembly15CNewMap[$assembly15CEntry.FullName] = Get-Assembly15CEntryHash $assembly15CEntry } }
    foreach ($assembly15CName in $assembly15COldMap.Keys) {
        if (-not $assembly15CNewMap.ContainsKey($assembly15CName)) { $assembly15CErrors.Add("旧载荷丢失 $assembly15CName") }
        elseif ($assembly15COldMap[$assembly15CName] -eq $assembly15CNewMap[$assembly15CName]) { $assembly15CUnchanged++; if ($assembly15CName.EndsWith('.class')) { $assembly15CClassSame++ } }
        else {
            $assembly15CChanged.Add($assembly15CName)
            if ($assembly15CName -eq $assembly15CSwitchName) {
                if (-not $assembly15CSwitchOpcodesSame) { $assembly15CErrors.Add('Renderer生成切换表opcode漂移，不能按行号变化放行') }
            } elseif ($assembly15CName -notmatch '^com/iksxh/create_nuclear_industry/production/ShieldedAssembly(Renderer|ClientEvents)\.class$') { $assembly15CErrors.Add("非本批旧载荷变化 $assembly15CName") }
        }
    }
    foreach ($assembly15CName in $assembly15CNewMap.Keys) {
        if (-not $assembly15COldMap.ContainsKey($assembly15CName)) {
            $assembly15CAdded.Add($assembly15CName)
            if ($assembly15CName -notmatch '^com/iksxh/create_nuclear_industry/production/(ShieldedAssemblyAnimation(Clock)?(\$[^/]+)?|ShieldedAssemblyRenderer\$Timeline)\.class$' -and $assembly15CName -notmatch '^assets/create_nuclear_industry/(models/block/shielded_assembly_station/animation/(left_base|left_tool|right_base|right_tool|fixture_base|fixture_carriage|fixture_jaw_left|fixture_jaw_right)\.json|shielded_assembly/animation\.json)$') { $assembly15CErrors.Add("未经批准新载荷 $assembly15CName") }
        }
        if ($assembly15CName.EndsWith('.class')) {
            $assembly15CClasses++
            $assembly15CClassFile = Join-Path $assembly15CWorkspace "build/classes/java/main/$assembly15CName"
            if (-not (Test-Path -LiteralPath $assembly15CClassFile) -or (Get-FileHash -LiteralPath $assembly15CClassFile -Algorithm SHA256).Hash.ToLower() -ne $assembly15CNewMap[$assembly15CName]) { $assembly15CErrors.Add("当前编译class/JAR不同源 $assembly15CName") }
        }
    }
    $assembly15CManifest = Get-Content -LiteralPath (Join-Path $PSScriptRoot '../assets.json') -Raw | ConvertFrom-Json
    $assembly15CResources = @($assembly15CManifest.productionMappings.target) + @('src/main/resources/assets/create_nuclear_industry/shielded_assembly/animation.json')
    foreach ($assembly15CTarget in $assembly15CResources) {
        $assembly15CRelative = $assembly15CTarget.Substring(19)
        $assembly15CSourceSha = (Get-FileHash -LiteralPath (Join-Path $assembly15CWorkspace $assembly15CTarget) -Algorithm SHA256).Hash.ToLower()
        $assembly15CProcessSha = (Get-FileHash -LiteralPath (Join-Path $assembly15CWorkspace "build/resources/main/$assembly15CRelative") -Algorithm SHA256).Hash.ToLower()
        if ($assembly15CSourceSha -ne $assembly15CProcessSha -or $assembly15CSourceSha -ne $assembly15CNewMap[$assembly15CRelative]) { $assembly15CErrors.Add("新增资源production/processResources/JAR不同源 $assembly15CRelative") }
        $assembly15CProcessed.Add(@{path=$assembly15CRelative;productionSha256=$assembly15CSourceSha;processedSha256=$assembly15CProcessSha;jarSha256=$assembly15CNewMap[$assembly15CRelative]})
    }
    $assembly15CTestLeaks = @($assembly15CNewMap.Keys | Where-Object {$_ -match '(ShieldedAssemblyAnimation(Clock)?Test|NativeLayerProbe|native-probe)'})
    if ($assembly15CTestLeaks.Count -gt 0) { $assembly15CErrors.Add('本批测试/离线探针泄漏') }
    @{task='ART-SHIELDED-ASSEMBLY-15C';passed=($assembly15CErrors.Count -eq 0);baselineSha256=(Get-FileHash -LiteralPath $assembly15CBackup -Algorithm SHA256).Hash.ToLower();jarSha256=(Get-FileHash -LiteralPath $assembly15CJar -Algorithm SHA256).Hash.ToLower();jarBytes=(Get-Item -LiteralPath $assembly15CJar).Length;classes=$assembly15CClasses;unchangedOldClasses=$assembly15CClassSame;unchangedOldPayloads=$assembly15CUnchanged;changedOldPayloads=@($assembly15CChanged);addedPayloads=@($assembly15CAdded);resources=@($assembly15CProcessed);rendererSwitch=@{class=$assembly15CSwitchName;oldJarClassSha256=$assembly15COldMap[$assembly15CSwitchName];newJarClassSha256=$assembly15CNewMap[$assembly15CSwitchName];currentClassSha256=(Get-FileHash -LiteralPath (Join-Path $assembly15CWorkspace "build/classes/java/main/$assembly15CSwitchName") -Algorithm SHA256).Hash.ToLower();oldOpcodeSha256=(Get-FileHash -LiteralPath $assembly15COldSwitchCode -Algorithm SHA256).Hash.ToLower();newOpcodeSha256=(Get-FileHash -LiteralPath $assembly15CNewSwitchCode -Algorithm SHA256).Hash.ToLower();sameOpcodes=$assembly15CSwitchOpcodesSame;debugDifference='javap -c -l仅LineNumberTable line50→75';oldDebug='jar-first/renderer-switch-old-debug.txt';newDebug='jar-first/renderer-switch-new-debug.txt'};testLeaks=$assembly15CTestLeaks;errors=@($assembly15CErrors)} | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $assembly15CClient 'jar-checks.json') -Encoding utf8
} finally { $assembly15COldZip.Dispose(); $assembly15CNewZip.Dispose() }
if ($assembly15CErrors.Count -gt 0) { throw "本批JAR核验失败：$($assembly15CErrors -join '; ')" }
Write-Output "15C制品核验通过：新增资源$($assembly15CProcessed.Count)，class $assembly15CClasses，非本批旧载荷保持$assembly15CUnchanged"
