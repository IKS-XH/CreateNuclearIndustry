param(
    [string]$ProjectRoot = (Resolve-Path '.').Path
)

$ErrorActionPreference = 'Stop'
$resourceRoot = Join-Path $ProjectRoot 'src/main/resources'
$jarPath = Join-Path $ProjectRoot 'build/libs/create_nuclear_industry-0.1.0.jar'

# 逐个比较正式资源与 JAR 条目的字节哈希，并检查条目总数和 PNG 数量。
Add-Type -AssemblyName System.IO.Compression
$archive = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
$sha = [System.Security.Cryptography.SHA256]::Create()
try {
    $sourceFiles = @(Get-ChildItem (Join-Path $resourceRoot 'assets/create_nuclear_industry'),
        (Join-Path $resourceRoot 'data') -Recurse -File)
    $mismatches = @()
    foreach ($file in $sourceFiles) {
        $relative = $file.FullName.Substring($resourceRoot.Length + 1).Replace('\', '/')
        $entry = $archive.GetEntry($relative)
        if ($null -eq $entry) {
            $mismatches += $relative
            continue
        }
        $sourceHash = [Convert]::ToHexString($sha.ComputeHash([System.IO.File]::ReadAllBytes($file.FullName)))
        $stream = $entry.Open()
        try {
            $jarHash = [Convert]::ToHexString($sha.ComputeHash($stream))
        } finally {
            $stream.Dispose()
        }
        if ($sourceHash -ne $jarHash) {
            $mismatches += $relative
        }
    }
    $jarResources = @($archive.Entries | Where-Object {
        -not $_.FullName.EndsWith('/') -and
        ($_.FullName.StartsWith('assets/create_nuclear_industry/') -or $_.FullName.StartsWith('data/'))
    })
    $pngCount = @($sourceFiles | Where-Object Extension -eq '.png').Count
    if ($sourceFiles.Count -ne 279 -or $jarResources.Count -ne 279 -or $pngCount -ne 65 -or $mismatches.Count -ne 0) {
        throw "资源校验失败: source=$($sourceFiles.Count) jar=$($jarResources.Count) png=$pngCount mismatches=$($mismatches -join ',')"
    }
} finally {
    $sha.Dispose()
    $archive.Dispose()
}

# 单独核对五个玩家可见中英文名，避免资源虽同字节但内容偏离任务卡。
$names = @{
    tin_wire = @('锡条', 'Tin Wire')
    industrial_sensor = @('工业传感器', 'Industrial Sensor')
    radiation_sensor = @('辐射传感器', 'Radiation Sensor')
    incomplete_industrial_sensor = @('工业传感器半成品', 'Incomplete Industrial Sensor')
    incomplete_radiation_sensor = @('辐射传感器半成品', 'Incomplete Radiation Sensor')
}
foreach ($locale in @('zh_cn', 'en_us')) {
    $index = if ($locale -eq 'zh_cn') { 0 } else { 1 }
    $path = Join-Path $resourceRoot "assets/create_nuclear_industry/lang/$locale.json"
    $language = Get-Content -Raw -LiteralPath $path | ConvertFrom-Json
    foreach ($id in $names.Keys) {
        $key = "item.create_nuclear_industry.$id"
        if ($language.PSObject.Properties[$key].Value -cne $names[$id][$index]) {
            throw "语言名称不符: $locale / $key"
        }
    }
}

[pscustomobject]@{
    ResourceFiles = $sourceFiles.Count
    JarResources = $jarResources.Count
    PngFiles = $pngCount
    LanguageEntries = 10
    JarSha256 = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash
    Result = 'PASS'
}
