param(
    [Parameter(Mandatory = $true)][string]$RepoRoot,
    [Parameter(Mandatory = $true)][string]$JarPath
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression

$root = [System.IO.Path]::GetFullPath($RepoRoot)
$resources = Join-Path $root 'src/main/resources'
$jar = [System.IO.Path]::GetFullPath($JarPath)
$files = @(
    Get-ChildItem -LiteralPath (Join-Path $resources 'assets') -Recurse -File
    Get-ChildItem -LiteralPath (Join-Path $resources 'data') -Recurse -File
)
$sourcePaths = @{}
$differences = [System.Collections.Generic.List[string]]::new()
$archive = [System.IO.Compression.ZipFile]::OpenRead($jar)
$hasher = [System.Security.Cryptography.SHA256]::Create()

try {
    foreach ($file in $files) {
        $relative = [System.IO.Path]::GetRelativePath($resources, $file.FullName).Replace('\', '/')
        $sourcePaths[$relative] = $true
        $entry = $archive.GetEntry($relative)
        if ($null -eq $entry) {
            $differences.Add("MISSING:$relative")
            continue
        }
        $sourceStream = [System.IO.File]::OpenRead($file.FullName)
        $jarStream = $entry.Open()
        try {
            $sourceHash = [Convert]::ToHexString($hasher.ComputeHash($sourceStream))
            $jarHash = [Convert]::ToHexString($hasher.ComputeHash($jarStream))
            if ($sourceHash -ne $jarHash) {
                $differences.Add("DIFFERENT:$relative")
            }
        } finally {
            $jarStream.Dispose()
            $sourceStream.Dispose()
        }
    }

    $jarPaths = @($archive.Entries | Where-Object {
        -not $_.FullName.EndsWith('/') -and
        ($_.FullName.StartsWith('assets/') -or $_.FullName.StartsWith('data/'))
    } | ForEach-Object { $_.FullName })
    foreach ($path in $jarPaths) {
        if (-not $sourcePaths.ContainsKey($path)) {
            $differences.Add("EXTRA:$path")
        }
    }
} finally {
    $hasher.Dispose()
    $archive.Dispose()
}

$names = Get-Content -LiteralPath (Join-Path $resources 'assets/create_nuclear_industry/lang/zh_cn.json') -Raw | ConvertFrom-Json -AsHashtable
$steel = [ordered]@{
    SteelDust = $names['item.create_nuclear_industry.steel_dust']
    SteelIngot = $names['item.create_nuclear_industry.steel_ingot']
    SteelPlate = $names['item.create_nuclear_industry.steel_plate']
}
$pngCount = @($files | Where-Object { $_.FullName -match '[\\/]assets[\\/].*[\\/]textures[\\/].*\.png$' }).Count
$result = [ordered]@{
    RepoRoot = $root
    JarPath = $jar
    JarSha256 = (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash
    SourceAssetsAndData = $files.Count
    JarAssetsAndData = $jarPaths.Count
    GamePng = $pngCount
    SteelNames = $steel
    Differences = @($differences)
}
$result | ConvertTo-Json -Depth 5

if ($files.Count -ne 264 -or $jarPaths.Count -ne 264 -or $pngCount -ne 60 -or
        $steel.SteelDust -ne '钢粉' -or $steel.SteelIngot -ne '钢锭' -or $steel.SteelPlate -ne '钢板' -or
        $differences.Count -ne 0) {
    throw 'EXT-A-MATERIAL-03B artifact verification failed'
}
