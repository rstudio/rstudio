# RStudio User Guide

-   Source code: <https://github.com/rstudio/rstudio/tree/main/docs/user/rstudio>
-   Public URL: <https://docs.posit.co/ide/user/>

> If you’re interested in getting involved with open source, contributing to documentation is a good—and helpful—place to start. - [James Turnbill](https://increment.com/documentation/documentation-as-a-gateway-to-open-source/)

If you have found issues with the RStudio User Guide, please check for existing duplicate issues/pull requests. 
If a matching Issue/Pull Request doesn't exist, please then open an [Issue](https://github.com/rstudio/rstudio/issues/new/choose) or Pull Request. 
Each page in the RStudio User Guide docs have an option to view the source which is useful for specifying a line of code in an Issue. 
Alternatively, selecting "Edit this Page" will prepare you to open a quick Pull Request from your browser.

## Development

To locally test changes to the User Guide, you will need to do the following:

1.  [Install Quarto](https://quarto.org/docs/getting-started/installation.html) or RStudio 2022.07.2 or later, which bundles Quarto.

-   Quarto is bundled with RStudio. Download RStudio from [posit.co/downloads/](https://posit.co/downloads/)
-   If not using RStudio, download the CLI directly from [github.com/quarto-dev](https://github.com/quarto-dev/quarto-cli/releases/latest) for your operating system
-   Optionally install the Quarto R Package with `install.packages("quarto")`

2.  Clone the [rstudio](https://github.com/rstudio/rstudio) repo
3.  Open up the `docs/user/rstudio/` directory in your favorite IDE (RStudio!).
4.  Serve the project in one of the following ways.

This will serve the site to the RStudio Viewer or your web browser. Quarto will detect when you make changes to any of the `qmd` files and rerender that page. 

-   From an open `.qmd` file, click the **Render** button

-   From the command line, `quarto preview` from within the `docs/user/rstudio` directory:

```bash
cd docs/user/rstudio
quarto preview
```

-   From the R console, enter `quarto::quarto_preview()` on the file you've changed like below:

```r
quarto::quarto_preview(file = "docs/user/rstudio/index.qmd")
```

Regardless of the method - you are likely to see the below error, because you don't have the environment variables set that we use in the build process. You can safely ignore these as they are resolved during our render/publish of the guide to docs.posit.co.

```
WARNING: Unknown var buildType specified in a var shortcode.
WARNING: Unknown var version specified in a var shortcode.
```

5. Once you have confirmed that the changes are technically accurate and that the rendered output is visually correct, you can either open a Pull Request with your changes or an Issue with the suggested fix.

Thank you for your contribution to open source - it will definitely help future users (and probably you)!

## Images

Screenshots taken on a high-DPI display are often several megabytes each, and every version of an image stays in the repository's history permanently. Before committing new or updated PNGs, optimize them from the root of the repository:

```bash
scripts/optimize-docs-images.sh docs/user/rstudio/path/to/image.png
```

The script requires [ImageMagick](https://imagemagick.org) 7. It converts each image to sRGB, caps its width at 2000 pixels, and reduces it to a 256-colour palette unless that would noticeably degrade it. Run it with no arguments to check every PNG in the guide; images that are already optimized are left alone.
