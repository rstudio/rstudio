# PowerShell script to publish a daily Windows binary

param(
    # The type of build, e.g. desktop-pro/windows
    [Parameter(Mandatory)]
    [string]$build, 

    # The URL to the location where the build can be downloaded
    [Parameter(Mandatory)]
    [string]$url, 

    # A path to a file on disk where the build is located
    [Parameter(Mandatory)]
    [string]$file, 

    # The version of the build
    [Parameter(Mandatory)]
    [string]$version,

    # The Github Personal Access PAT to use to publish the build
    [Parameter(Mandatory)]
    [string]$pat
)

# Function to urlize a string
function URLize {
    param(
        $string
    )

    # Convert to lower case and trim all whitespace
    $string = $string.ToLower()
    $string = $string.Trim()

    # Replace all non-ASCII characters with dashes
    $string = $string -replace "[^a-zA-Z0-9-]", "-"

    # Replace consecutive dashes with single dashes
    $string = $string -replace "--*", "-"

    return $string
}

# Extract file metadata
$size = (Get-Item $file).length
$filename = (Get-Item $file).Name
$timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss")
$sha256 = (Get-FileHash -Algorithm SHA256 $file).Hash.ToLower()

# Compute path from script directory to version metadata directory
$parent = Split-Path -Path $PSScriptRoot -Parent
$root = Split-Path -Path $parent -Parent
$versionMeta = Join-Path -Path $root -ChildPath "version"

# Extract build metadata
$flower = Get-Content (Join-Path -Path $versionMeta -ChildPath "RELEASE") | Out-String
$flower = URLize -string $flower
$channel = Get-Content (Join-Path -Path $versionMeta -ChildPath "BUILDTYPE") | Out-String
$channel = $channel.Trim()
$commit = git rev-parse HEAD

$versionStem = URLize -string $version

# Clean up plus symbols in URL
$url = $url.Replace("+", "%2B")

# Form the Markdown file's YAML header
$mdContents = @"
---
type: build
date: $timestamp
link: "$url"
filename: "$filename"
architecture: x86_64
sha256: "$sha256"
channel: "$channel"
version: "$version"
commit: "$commit"
size: $size
---
"@

Write-Host "Creating $flower/$build/$versionStem.md..."
Write-Host $mdContents

# Base64 encode the Markdown/YAML file
$bytes = [System.Text.Encoding]::UTF8.GetBytes($mdContents)
$base64 = [System.Convert]::ToBase64String($bytes)

# Prepare the payload for the Github API
$payload = @"
{ "message": "Add $flower build $version in $build", "content": "$base64" }
"@
Write-Host "Sending to Github: $payload"

# Prepare headers
$headers = @{}
$headers.Add("Accept", "application/vnd.github.v3+json")
$headers.Add("Authorization", "token $pat")

$url = "https://api.github.com/repos/AndrewMcClain/test-release-repo/contents/content/rstudio/$flower/$build/$versionStem.md"

# Send to Github! We have to use basic parsing here because this script runs on SKU of Windows that
# doesn't contain a working copy of IE (and, incredibly, without -UseBasicParsing, Invoke-WebRequest
# has a dendency on the IE DOM engine).
try
{
    $createResponse = Invoke-RestMethod -Body $payload -Method 'PUT' -Headers $headers -Uri $url -UseBasicParsing
    Write-Host "Response :"
    Write-Host $createResponse.Content
} catch {
    $StatusCode = $_.Exception.Response.StatusCode.value__
    # Assume the file already exists and we need to do an update
    if ($StatusCode = 422)
    {
        Write-Host "Received an error, assuming it's an issue updating. Getting existing file's SHA"
        $getSha = Invoke-RestMethod -Method 'GET' -Headers $headers -Uri $url -UseBasicParsing
        Write-Host "Github Response:"
        Write-Host $getSha
        $updateSha = $getSha.sha

        # This looks messy but the whitespace is meaningful
        $updatePayload = @"
{ "message": "Add $flower build $version in $build", "content": "$base64", "sha": "$updateSha" }
"@
        Write-Host "Updating version file..."
        $updateResponse = Invoke-RestMethod -Body $updatePayload -Method 'PUT' -Headers $headers -Uri $url -UseBasicParsing
        Write-Host $updateResponse
    }
}

