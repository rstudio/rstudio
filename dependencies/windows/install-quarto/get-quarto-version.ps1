# Retrieve latest Quarto version from GitHub

$response = Invoke-WebRequest -UseBasicParsing -Uri https://quarto.org/docs/download/_download.json
$releases = ConvertFrom-Json $response.content
$version = $releases.version
$quartoVersion = $version | Select-String -Pattern '[0-9]+\.[0-9]+\.[0-9]+'
$quartoVersion.Matches.Value
