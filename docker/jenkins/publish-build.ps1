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

# Transport failures with no response, commit conflicts (409, e.g. interleaving with another
# publish), timeouts, rate limits, and server errors are worth retrying. Other errors,
# including bugs in this script, are not.
function Test-Retryable {
    param(
        $errorRecord
    )

    $statusCode = Get-StatusCode $errorRecord
    if ($null -eq $statusCode) {
        # WebException on Windows PowerShell; HttpRequestException on PowerShell 7
        return $errorRecord.Exception.GetType().FullName -in
            'System.Net.WebException', 'System.Net.Http.HttpRequestException'
    }
    return ($statusCode -in 408, 409, 429) -or ($statusCode -ge 500)
}

# Create the build file. A 422 means the file already exists -- a re-publish, or an earlier
# attempt that GitHub committed before returning an error -- so fetch the existing file's sha
# and send an update instead. Throws if any request fails.
#
# -UseBasicParsing is required: without it, Windows PowerShell's web cmdlets depend on the IE
# DOM engine, which the Windows SKU this script runs on doesn't have. Without -TimeoutSec they
# wait forever for a response; a timeout throws a WebException with no response, which the
# caller retries.
function Publish-BuildFile {
    param(
        $uri,
        $headers,
        $createMessage,
        $updateMessage,
        $content
    )

    $request = @{ Headers = $headers; Uri = $uri; UseBasicParsing = $true; TimeoutSec = 60 }

    $createPayload = @{ message = $createMessage; content = $content } | ConvertTo-Json -Compress
    Write-Host "Sending to Github: $createPayload"
    try {
        return Invoke-RestMethod -Method 'PUT' -Body $createPayload @request
    } catch {
        if ((Get-StatusCode $_) -ne 422) {
            throw
        }
        Write-Host "Received a 422: $($_.ErrorDetails.Message)"
    }

    Write-Host "Assuming the file already exists. Getting existing file's SHA"
    $existing = Invoke-RestMethod -Method 'GET' @request
    $updatePayload = @{ message = $updateMessage; content = $content; sha = $existing.sha } |
        ConvertTo-Json -Compress
    Write-Host "Sending to Github: $updatePayload"
    return Invoke-RestMethod -Method 'PUT' -Body $updatePayload @request
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

Write-Host "Writing content to $url"

# The random 3-8 second sleep keeps concurrent publishers from retrying in lockstep.
$maxRetries = 5
for ($retry = 0; ; $retry++) {
    try {
        $response = Publish-BuildFile -uri $url -headers $headers `
            -createMessage "Add $flower build $version in $build" `
            -updateMessage "Update $flower build $version in $build" `
            -content $base64
        break
    } catch {
        $statusCode = Get-StatusCode $_
        $status = if ($null -eq $statusCode) { "no HTTP response" } else { "HTTP $statusCode" }
        Write-Host "Request failed ($status): $($_.Exception.Message) $($_.ErrorDetails.Message)"
        if (($retry -ge $maxRetries) -or -not (Test-Retryable $_)) {
            throw "Failed to publish $flower build $version to $url ($status): " +
                "$($_.Exception.Message)`nCheck the log above, the PAT's access to " +
                "rstudio/latest-builds, and githubstatus.com."
        }
        $delay = Get-Random -Minimum 3 -Maximum 9
        Write-Host "Retrying in $delay seconds (retry $($retry + 1) of $maxRetries)"
        Start-Sleep -Seconds $delay
    }
}

Write-Host "Published $($response.content.path) in commit $($response.commit.sha)"
