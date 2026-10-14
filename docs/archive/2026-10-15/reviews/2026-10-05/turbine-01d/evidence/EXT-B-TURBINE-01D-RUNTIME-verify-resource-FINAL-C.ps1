$ErrorActionPreference = 'Stop'

$report = 'build/reports/extension/EXT-B-TURBINE-01D-RUNTIME'
$before = Import-Csv -LiteralPath "$report-resource-hashes-FINAL-C-before.csv"
$after = foreach ($row in $before) {
    $item = Get-Item -LiteralPath $row.Path
    [pscustomobject]@{
        Path = $row.Path
        Length = $item.Length
        LastWriteTimeUtc = $item.LastWriteTimeUtc.ToString('o')
        SHA256 = (Get-FileHash -LiteralPath $row.Path -Algorithm SHA256).Hash
    }
}
$after | Export-Csv -LiteralPath "$report-resource-hashes-FINAL-C-after.csv" -NoTypeInformation -Encoding UTF8

$jar = Get-Item -LiteralPath 'build/libs/create_nuclear_industry-0.1.0.jar'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
try {
    $jarRows = foreach ($row in $before) {
        $entryPath = $row.Path.Substring('src/main/resources/'.Length)
        $entry = $zip.GetEntry($entryPath)
        $actualHash = ''
        if ($null -ne $entry) {
            $stream = $entry.Open()
            $sha = [System.Security.Cryptography.SHA256]::Create()
            try {
                $actualHash = -join ($sha.ComputeHash($stream) | ForEach-Object { $_.ToString('X2') })
            } finally {
                $sha.Dispose()
                $stream.Dispose()
            }
        }
        [pscustomobject]@{
            Path = $row.Path
            JarPath = $entryPath
            SourceSHA256 = $row.SHA256
            JarSHA256 = $actualHash
            Packaged = ($null -ne $entry)
            ByteEqual = ($actualHash -eq $row.SHA256)
        }
    }
} finally {
    $zip.Dispose()
}
$jarRows | Export-Csv -LiteralPath "$report-jar-resources-FINAL-C.csv" -NoTypeInformation -Encoding UTF8

$drift = @($after | Where-Object {
    $original = $before | Where-Object Path -eq $_.Path | Select-Object -First 1
    $_.SHA256 -ne $original.SHA256 -or $_.Length -ne $original.Length
}).Count
$missing = @($jarRows | Where-Object { -not $_.Packaged }).Count
$mismatch = @($jarRows | Where-Object { $_.Packaged -and -not $_.ByteEqual }).Count
$png = @($before | Where-Object Path -like '*.png').Count
$summary = @(
    "jar=$($jar.FullName)",
    "jar_sha256=$((Get-FileHash -LiteralPath $jar.FullName -Algorithm SHA256).Hash)",
    "changed_or_new=$($before.Count)",
    "new_png=$png",
    "source_drift=$drift",
    "jar_missing=$missing",
    "jar_byte_mismatch=$mismatch"
)
$summary | Set-Content -LiteralPath "$report-jar-resources-FINAL-C.txt" -Encoding UTF8
$summary
if ($drift -or $missing -or $mismatch) { exit 1 }
exit 0
