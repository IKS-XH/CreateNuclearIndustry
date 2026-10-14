$ErrorActionPreference = 'Stop'
$assemblyBRoot = Split-Path -Parent $PSScriptRoot
$assemblyWorkspace = [IO.Path]::GetFullPath((Join-Path $assemblyBRoot '../../..'))
$assemblyClientReport = Join-Path $assemblyWorkspace 'build/reports/art/ART-SHIELDED-ASSEMBLY-15B-R1/client'
$assemblyManifest = Get-Content -LiteralPath (Join-Path $assemblyBRoot 'assets.json') -Raw | ConvertFrom-Json
$assemblyResult = Get-Content -LiteralPath (Join-Path $assemblyClientReport 'assemble-result.json') -Raw | ConvertFrom-Json
if ($assemblyResult.exit -ne 0) { throw 'assemble未成功，不登记可用候选' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Get-AssemblyZipIdentity([string]$archivePath) {
    $assemblyZip = [IO.Compression.ZipFile]::OpenRead($archivePath)
    $assemblyEntries = @{}
    try { foreach ($assemblyEntry in $assemblyZip.Entries) {
        if ($assemblyEntry.FullName.EndsWith('/')) { continue }
        $assemblyStream = $assemblyEntry.Open()
        $assemblySha = [Security.Cryptography.SHA256]::Create()
        try { $assemblyEntries[$assemblyEntry.FullName] = @{bytes=$assemblyEntry.Length;sha256=([BitConverter]::ToString($assemblySha.ComputeHash($assemblyStream))).Replace('-','').ToLower()} } finally { $assemblyStream.Dispose(); $assemblySha.Dispose() }
    } } finally { $assemblyZip.Dispose() }
    return $assemblyEntries
}
# 只做真实JAR读回与旧包载荷比较，不重新生成/构建或运行Java测试。
$assemblyJar = Join-Path $assemblyWorkspace 'build/libs/create_nuclear_industry-0.1.0.jar'
$assemblyOld = Get-AssemblyZipIdentity (Join-Path $assemblyClientReport 'backup/create_nuclear_industry-0.1.0-a92a0bed.jar')
$assemblyNew = Get-AssemblyZipIdentity $assemblyJar
$assemblyExpected = @($assemblyManifest.renderLayerR1.changedTargets | ForEach-Object {$_.Substring(19)})
if ($assemblyManifest.renderLayerR1.task -ne 'ART-SHIELDED-ASSEMBLY-15B-R1' -or $assemblyExpected.Count -lt 1) { throw '缺少已审R1变更契约' }
$assemblyMappingRows = @()
$assemblyErrors = @()
foreach ($assemblyEntry in $assemblyManifest.productionMappings | Where-Object {$_.target -in $assemblyManifest.renderLayerR1.changedTargets}) {
    $assemblyPath = $assemblyEntry.target.Substring(19)
    $assemblyProductionSha = (Get-FileHash -LiteralPath (Join-Path $assemblyWorkspace $assemblyEntry.target) -Algorithm SHA256).Hash.ToLower()
    $assemblyProcessedSha = (Get-FileHash -LiteralPath (Join-Path $assemblyWorkspace ('build/resources/main/' + $assemblyPath)) -Algorithm SHA256).Hash.ToLower()
    $assemblyJarSha = $assemblyNew[$assemblyPath].sha256
    $assemblyPassed = $assemblyProductionSha -eq $assemblyEntry.sha256 -and $assemblyProcessedSha -eq $assemblyEntry.sha256 -and $assemblyJarSha -eq $assemblyEntry.sha256
    if (-not $assemblyPassed) { $assemblyErrors += "R1批准模型不同源 $assemblyPath" }
    $assemblyMappingRows += @{path=$assemblyPath;sourceSha256=$assemblyEntry.sha256;productionSha256=$assemblyProductionSha;processResourcesSha256=$assemblyProcessedSha;jarSha256=$assemblyJarSha;passed=$assemblyPassed}
}
$assemblyChanged = @($assemblyNew.Keys | Where-Object {-not $assemblyOld.ContainsKey($_) -or $assemblyNew[$_].sha256 -ne $assemblyOld[$_].sha256} | Sort-Object)
$assemblyRemoved = @($assemblyOld.Keys | Where-Object {-not $assemblyNew.ContainsKey($_)} | Sort-Object)
foreach ($assemblyPath in $assemblyChanged) { if ($assemblyPath -notin $assemblyExpected) { $assemblyErrors += "非本批载荷变化 $assemblyPath" } }
if ($assemblyRemoved.Count -ne 0) { $assemblyErrors += '旧JAR载荷被删除' }
if ($assemblyChanged.Count -ne $assemblyExpected.Count -or $assemblyMappingRows.Count -ne $assemblyExpected.Count) { $assemblyErrors += '实际变化数量与批准metadata不一致' }
@{task='ART-SHIELDED-ASSEMBLY-15B-R1';passed=($assemblyErrors.Count -eq 0);jar=$assemblyJar;sha256=(Get-FileHash -LiteralPath $assemblyJar -Algorithm SHA256).Hash.ToLower();bytes=(Get-Item -LiteralPath $assemblyJar).Length;mappings=$assemblyMappingRows;changed=$assemblyChanged;removed=$assemblyRemoved;unchangedOldPayloads=($assemblyOld.Count - ($assemblyChanged | Where-Object {$assemblyOld.ContainsKey($_)}).Count);classCount=($assemblyNew.Keys | Where-Object {$_.EndsWith('.class')}).Count;errors=$assemblyErrors} | ConvertTo-Json -Depth 7 | Set-Content -LiteralPath (Join-Path $assemblyClientReport 'jar-verification.json') -Encoding utf8
if ($assemblyErrors.Count -ne 0) { throw ($assemblyErrors -join '; ') }
Write-Output "R1 $($assemblyMappingRows.Count)模型production/processResources/JAR同源；仅$($assemblyChanged.Count)批准metadata变化，Java与其它载荷保持。"
