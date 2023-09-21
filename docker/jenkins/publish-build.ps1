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

function Handle409 {
    param(
        $url,
        $payload,
        $headers
    )

    Write-Host "Received a 409, assuming it's a commit interleaving error, waiting 3 seconds and retrying".
    Start-Sleep -Seconds 3

    # Identical call to the original above
    Write-Host "Retrying..."
    $retryCreateResponse = Invoke-RestMethod -Body $payload -Method 'PUT' -Headers $headers -Uri $url -UseBasicParsing
    Write-Host "Response :"
    Write-Host $retryCreateResponse.Content
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

# Prepare the payload for the Github API
$payload = @"
{ "message": "Add $flower build $version in $build", "content": "$base64" }
"@
Write-Host "Sending to Github: $payload"

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

try
{
    $createResponse = Invoke-RestMethod -Body $payload -Method 'PUT' -Headers $headers -Uri $url -UseBasicParsing
    Write-Host "Response :"
    Write-Host $createResponse.Content
} catch {
    $StatusCode = $_.Exception.Response.StatusCode.value__
    
    if ($StatusCode -eq 422) # Assume the file already exists and we need to do an update
    {
        Write-Host "Received a 422 error, assuming it's an issue updating. Getting existing file's SHA"
        $getSha = Invoke-RestMethod -Method 'GET' -Headers $headers -Uri $url -UseBasicParsing
        Write-Host "Github Response:"
        Write-Host $getSha
        $updateSha = $getSha.sha

        # This looks messy but the whitespace is meaningful
        $payload = @"
{ "message": "Update $flower build $version in $build", "content": "$base64", "sha": "$updateSha" }
"@
        try {
            Write-Host "Updating version file..."
            $updateResponse = Invoke-RestMethod -Body $payload -Method 'PUT' -Headers $headers -Uri $url -UseBasicParsing
            Write-Host "Response :"
            Write-Host $updateResponse.Content
        } catch {
            $StatusCode = $_.Exception.Response.StatusCode.value__

            if ($StatusCode -eq 409)
            {
                Handle409 -url $url -payload $payload -headers $headers
            }
            else 
            {
                Write-Host "Received an unexpected error code: $StatusCode"
            }
        }
    }
    
    if ($StatusCode -eq 409) { # Assume the repo has been updated backoff and try again.
        Handle409 -url $url -payload $payload -headers $headers
    }
}
