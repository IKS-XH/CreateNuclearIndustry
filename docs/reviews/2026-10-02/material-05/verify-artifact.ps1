param(
    [string]$ProjectRoot = (Resolve-Path '.').Path
)

$ErrorActionPreference = 'Stop'
$resourceRoot = Join-Path $ProjectRoot 'src/main/resources'
$jarPath = Join-Path $ProjectRoot 'build/libs/create_nuclear_industry-0.1.0.jar'
$textureRoot = Join-Path $resourceRoot 'assets/create_nuclear_industry/textures'

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
    if ($sourceFiles.Count -ne 292 -or $jarResources.Count -ne 292 -or $pngCount -ne 69 -or $mismatches.Count -ne 0) {
        throw "资源校验失败: source=$($sourceFiles.Count) jar=$($jarResources.Count) png=$pngCount mismatches=$($mismatches -join ',')"
    }
} finally {
    $sha.Dispose()
    $archive.Dispose()
}

# 独立美术任务的开工快照保护65张旧图，并锁定本批四图的交接字节。
$oldHashes = Get-Content -Raw -LiteralPath (Join-Path $ProjectRoot 'build/reports/extension/EXT-ART-07/preexisting-game-png-sha256.json') | ConvertFrom-Json -AsHashtable
if ($oldHashes.Count -ne 65) { throw "旧PNG基线数量不符: $($oldHashes.Count)" }
foreach ($relative in $oldHashes.Keys) {
    $path = Join-Path $textureRoot $relative
    if (-not (Test-Path -LiteralPath $path) -or
            (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -ne $oldHashes[$relative]) {
        throw "旧PNG字节变化: $relative"
    }
}
$newHashes = @{
    'item/quartz_dust.png' = '85297409E459E942681DEA8688C703ABB43AFC49FBF7ADFD3FC5734A53AA6DBD'
    'item/refractory_brick.png' = '9F965DAC40D4E9A1696170AF082202CDC870127C447B9047784CF12BAEB861D3'
    'item/heavy_bearing.png' = '6E4C42B261F5864CDE2714DCDA72D2E53397E49CD1C3B64F4CA90DB2396AFB92'
    'item/incomplete_heavy_bearing.png' = '41C811A511A43B0580A3A83DEA9CB8E4A47D7045DB7013181E003B6C5568E1AC'
}
foreach ($relative in $newHashes.Keys) {
    $path = Join-Path $textureRoot $relative
    if (-not (Test-Path -LiteralPath $path) -or
            (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -cne $newHashes[$relative]) {
        throw "本批PNG交接字节变化: $relative"
    }
}

# 单独核对四个玩家可见中英文名，避免资源虽同字节但内容偏离任务卡。
$names = @{
    quartz_dust = @('石英粉', 'Quartz Dust')
    refractory_brick = @('耐火砖', 'Refractory Brick')
    heavy_bearing = @('重型轴承', 'Heavy Bearing')
    incomplete_heavy_bearing = @('重型轴承半成品', 'Incomplete Heavy Bearing')
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
    ProtectedOldPngFiles = $oldHashes.Count
    LockedNewPngFiles = $newHashes.Count
    LanguageEntries = 8
    JarSha256 = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash
    Result = 'PASS'
}
