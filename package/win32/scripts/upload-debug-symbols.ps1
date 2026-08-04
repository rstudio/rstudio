# PowerShell script to collect and upload Windows debug symbols (PDBs).
#
# Collects the linker PDBs for the binaries we ship, archives them, and
# uploads the archive to the rstudio-debug-symbols S3 bucket, following the
# naming convention established by package/linux/scripts/upload-debug-symbols
# (<version>/<product>-<os>-<arch>). A zip is used rather than a tar.xz since
# these symbols are consumed on Windows.

param(
    # The product name, e.g. "electron"
    [Parameter(Mandatory)]
    [string]$product,

    # The full RStudio version, e.g. "2026.08.0-daily+321"
    [Parameter(Mandatory)]
    [string]$version,

    # The architecture of the primary build
    [Parameter(Mandatory=$false)]
    [string]$arch = "x86_64",

    # The 64-bit build directory
    [Parameter(Mandatory=$false)]
    [string]$buildDir = (Join-Path -Path (Split-Path -Path $PSScriptRoot -Parent) -ChildPath "build"),

    # The 32-bit (multiarch) build directory
    [Parameter(Mandatory=$false)]
    [string]$buildDir32 = (Join-Path -Path (Split-Path -Path $PSScriptRoot -Parent) -ChildPath "build32"),

    # Create the archive but skip the S3 upload
    [Parameter(Mandatory=$false)]
    [switch]$dryRun
)

$ErrorActionPreference = "Stop"

$bucket = "s3://rstudio-debug-symbols"

# The PDBs for the binaries we ship; keep in sync with the executables signed
# in Jenkinsfile.windows. Note that rsession-shared.pdb belongs to rsession.dll
# (see src/cpp/session/CMakeLists.txt), and carries most of the session debug
# info. The archive keeps one folder per architecture, since the 32-bit and
# 64-bit PDBs share file names.
$pdbs = @(
    @{ Dir = $buildDir;   Path = "src\cpp\session\rsession.pdb";            Dest = "x64" },
    @{ Dir = $buildDir;   Path = "src\cpp\session\rsession-utf8.pdb";       Dest = "x64" },
    @{ Dir = $buildDir;   Path = "src\cpp\session\rsession-shared.pdb";     Dest = "x64" },
    @{ Dir = $buildDir;   Path = "src\cpp\session\consoleio\consoleio.pdb"; Dest = "x64" },
    @{ Dir = $buildDir;   Path = "src\cpp\session\postback\rpostback.pdb";  Dest = "x64" },
    @{ Dir = $buildDir;   Path = "src\cpp\diagnostics\diagnostics.pdb";     Dest = "x64" },
    @{ Dir = $buildDir32; Path = "src\cpp\session\rsession.pdb";            Dest = "x86" },
    @{ Dir = $buildDir32; Path = "src\cpp\session\rsession-shared.pdb";     Dest = "x86" }
)

# The 32-bit session is only present in multiarch builds (the default on CI;
# see make-package.bat), so skip it with a warning when absent.
if (-not (Test-Path $buildDir32)) {
    Write-Warning "32-bit build directory $buildDir32 not found; skipping x86 PDBs"
    $pdbs = @($pdbs | Where-Object { $_.Dir -ne $buildDir32 })
}

# Verify all expected PDBs are present before staging any of them.
$missing = @($pdbs | Where-Object { -not (Test-Path (Join-Path $_.Dir $_.Path)) })
if ($missing.Count -gt 0) {
    $missing | ForEach-Object { Write-Host "Missing PDB: $(Join-Path $_.Dir $_.Path)" }
    Write-Error "Some expected PDBs were not found; was this a RelWithDebInfo package build?"
}

# Stage the PDBs into a scratch folder with the archive's layout.
$prefix = "$product-windows-$arch"
$stageDir = Join-Path $buildDir "$prefix-symbols"
if (Test-Path $stageDir) {
    Remove-Item -Recurse -Force $stageDir
}

foreach ($pdb in $pdbs) {
    $destDir = Join-Path $stageDir $pdb.Dest
    New-Item -ItemType Directory -Force -Path $destDir | Out-Null
    Copy-Item (Join-Path $pdb.Dir $pdb.Path) $destDir
}

# Compress. Use ZipFile directly: Compress-Archive on Windows PowerShell 5.1
# buffers whole files in memory and fails on files larger than 2 GB, which
# rsession-shared.pdb can exceed.
$archive = Join-Path $buildDir "$prefix.zip"
if (Test-Path $archive) {
    Remove-Item -Force $archive
}

Write-Host "Creating $archive..."
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory(
    $stageDir,
    $archive,
    [System.IO.Compression.CompressionLevel]::Optimal,
    $false)

Remove-Item -Recurse -Force $stageDir
Write-Host ("Archive size: {0:N0} bytes" -f (Get-Item $archive).Length)

# Upload to S3.
$target = "$bucket/$version/$prefix.zip"
if ($dryRun) {
    Write-Host "Dry run; skipping upload to $target"
    exit 0
}

Write-Host "Uploading to $target..."
aws s3 cp $archive $target
if ($LASTEXITCODE -ne 0) {
    Write-Error "aws s3 cp failed with exit code $LASTEXITCODE"
}
