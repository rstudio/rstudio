# Retrieve latest Quarto version from GitHub

$response = Invoke-WebRequest -Uri https://api.github.com/repos/quarto-dev/quarto-cli/releases
$releases = ConvertFrom-Json $response.content
$tag = $releases.tag_name[0]
$quartoVersion = $tag | Select-String -Pattern '[0-9]+\.[0-9]+\.[0-9]+'
$quartoVersion.Matches.Value
