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
    [string]$pat,

    # The release channel of the build, Hourly, Daily, Preview or Release
    # Default value if it's not supplied is the contents of the version/BUILDTYPE file
    # Otherwise we validate input and use that
    [Parameter(Mandatory=$false)]
	[ValidateSet("Hourly", "Daily", "Preview", "Release", IgnoreCase=$false)]
    [string]$channel = (Get-Content (Join-Path -Path "version" -ChildPath "BUILDTYPE") | Out-String).Trim()
)

$ErrorActionPreference = 'Stop'

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

# The HTTP status of a failed Invoke-RestMethod call, or $null when no response arrived
function Get-StatusCode {
    param(
        $errorRecord
    )

    $response = $errorRecord.Exception.Response
    if ($null -eq $response) {
        return $null
    }
    return [int]$response.StatusCode
}

# Commit conflicts (409, e.g. interleaving with another publish), timeouts, rate limits,
# server errors, and transport failures with no response are worth retrying.
function Test-Retryable {
    param(
        $statusCode
    )

    return ($null -eq $statusCode) -or ($statusCode -in 408, 409, 429) -or ($statusCode -ge 500)
}

# Create the build file. A 422 means the file already exists -- a re-publish, or an earlier
# attempt that GitHub committed before returning an error -- so fetch the existing file's sha
# and send an update instead. Throws if any request fails.
function Publish-BuildFile {
    param(
        $uri,
        $headers,
        $createMessage,
        $updateMessage,
        $content
    )

    $createPayload = @{ message = $createMessage; content = $content } | ConvertTo-Json -Compress
    Write-Host "Sending to Github: $createPayload"
    try {
        return Invoke-RestMethod -Body $createPayload -Method 'PUT' -Headers $headers -Uri $uri -UseBasicParsing
    } catch {
        if ((Get-StatusCode $_) -ne 422) {
            throw
        }
    }

    Write-Host "Received a 422, assuming the file already exists. Getting existing file's SHA"
    $existing = Invoke-RestMethod -Method 'GET' -Headers $headers -Uri $uri -UseBasicParsing
    $updatePayload = @{ message = $updateMessage; content = $content; sha = $existing.sha } |
        ConvertTo-Json -Compress
    Write-Host "Sending to Github: $updatePayload"
    return Invoke-RestMethod -Body $updatePayload -Method 'PUT' -Headers $headers -Uri $uri -UseBasicParsing
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

# Prepare headers
$headers = @{}
$headers.Add("Accept", "application/vnd.github.v3+json")
$headers.Add("Authorization", "token $pat")

# Prepare the product name, redirects hourly builds to a different page
$product = "rstudio"
if ($channel -eq "Hourly")
{
    $product = $product + "-hourly"
}

$url = "https://api.github.com/repos/rstudio/latest-builds/contents/content/$product/$flower/$build/$versionStem.md"
# Send to Github! We have to use basic parsing here because this script runs on SKU of Windows that
# doesn't contain a working copy of IE (and, incredibly, without -UseBasicParsing, Invoke-WebRequest
# has a dendency on the IE DOM engine).

Write-Host "Writing content to $url"

# The random 3-8 second sleep keeps concurrent publishers from retrying in lockstep.
$maxRetries = 5
for ($retry = 0; ; $retry++) {
    try {
        $response = Publish-BuildFile -uri $url -headers $headers `
            -createMessage "Add $flower build $version in $build" -updateMessage "Update $flower build $version in $build" -content $base64
        break
    } catch {
        $statusCode = Get-StatusCode $_
        $status = if ($null -eq $statusCode) { "no response" } else { "HTTP $statusCode" }
        Write-Host "Request failed ($status): $($_.Exception.Message) $($_.ErrorDetails.Message)"
        if (($retry -ge $maxRetries) -or -not (Test-Retryable $statusCode)) {
            throw "Failed to publish $flower build $version to $url ($status); see the log above"
        }
        $delay = Get-Random -Minimum 3 -Maximum 9
        Write-Host "Retrying in $delay seconds (retry $($retry + 1) of $maxRetries)"
        Start-Sleep -Seconds $delay
    }
}

Write-Host "Published $($response.content.path) in commit $($response.commit.sha)"
