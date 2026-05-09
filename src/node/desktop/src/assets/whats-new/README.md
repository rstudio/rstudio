# What's New Content

This directory contains the per-release content shown in the "What's New" window
on first launch of each RStudio Desktop release.

## Directory Structure

```
whats-new/
├── whats-new-base.css              # Shared base stylesheet
├── globemaster-allium/             # Content for "Globemaster Allium" release
│   ├── index.html
│   └── images/                     # Optional images
│       └── feature-screenshot.png
└── <next-release-slug>/
    └── index.html
```

## Adding Content for a New Release

1. **Create a folder** named after the release's flower name, converted to a slug:
   - Lowercase the name
   - Remove apostrophes
   - Replace spaces and non-alphanumeric characters with hyphens

   Examples: "Globemaster Allium" → `globemaster-allium`, "King's Crown" → `kings-crown`

2. **Create `index.html`** in that folder using the template below.

3. **Add images** (optional) in the same folder or an `images/` subfolder.
   Use relative paths in the HTML (e.g., `<img src="images/screenshot.png">`).

4. **Run tests** to verify the required tags are present:
   ```
   cd src/node/desktop && npm test
   ```

## Template

Copy this into your new `index.html` and edit the content. Replace
`YYYY.MM.PATCH` in the release notes URL with the actual version
(e.g., `2026.04.0`):

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta http-equiv="Content-Security-Policy" content="default-src 'self' 'unsafe-inline' file:;">
  <link rel="stylesheet" href="../whats-new-base.css">
  <title>What's New in RStudio</title>
</head>
<body>
  <div class="feature-section">
    <h2>New Features</h2>
    <ul>
      <li>Feature description here</li>
    </ul>
  </div>

  <div class="feature-section">
    <h2>Fixes</h2>
    <p>
      For a full list of bug fixes in this release, see the
      <a href="https://www.rstudio.org/links/release_notes#rstudio-YYYY.MM.PATCH">release notes</a>.
    </p>
  </div>
</body>
</html>
```

## Requirements

Each `index.html` **must** include:

- The `Content-Security-Policy` meta tag (as shown in the template)
- The `whats-new-base.css` stylesheet link

These are enforced by automated tests.

## Styling

The base stylesheet (`whats-new-base.css`) provides:

- System font stack, light background, base typography
- Heading styles (`h2`, `h3`)
- List and paragraph spacing
- Image styling (max-width, rounded corners, border)
- Link colors
- `.feature-section` class for spacing between sections

You can add a `<style>` block in your `index.html` to extend or override the
base styles.

## How It Works

The What's New window is a wrapper page with a fixed header (release name and
version), a scrollable iframe that loads your `index.html`, and a footer with a
Close button. Each release page must be a complete HTML document including the
required `<head>` tags (CSP and stylesheet). The wrapper provides the outer
window chrome (header, footer, close/escape behavior) around the iframe.

External links (`https://`, `http://`) open in the user's default browser.

## Environment Variables

| Variable | Effect |
|----------|--------|
| `RSTUDIO_SHOW_WHATS_NEW` | Force the What's New window at startup (any value). Bypasses build type and seen-state checks. Content must still exist. |
| `RSTUDIO_DISABLE_WHATS_NEW` | Suppress the What's New window at startup (any value). The **Help** > **What's New** command still works. |

These must be set in your shell environment before launching RStudio.
Setting them in `.Renviron` will not work because the What's New check
runs in the Electron process before R starts.

## Testing Locally

Set `RSTUDIO_SHOW_WHATS_NEW=1` when launching RStudio Desktop to force the
What's New window to appear at startup, regardless of build type or seen state.

Use **Help** > **What's New** to open it on demand.
