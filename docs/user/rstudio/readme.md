# RStudio User Guide

-   Source code: <https://github.com/rstudio/rstudio/tree/main/docs/user/rstudio>
-   Public URL: <https://docs.rstudio.com/ide/user/>

> If you’re interested in getting involved with open source, contributing to documentation is a good—and helpful—place to start. - [James Turnbill](https://increment.com/documentation/documentation-as-a-gateway-to-open-source/)

If you have found issues with the RStudio User Guide, please check for existing duplicate issues/pull requests. 
If a matching Issue/Pull Request doesn't exist, please then open an [Issue](https://github.com/rstudio/rstudio/issues/new/choose) or Pull Request. 
Each page in the RStudio User Guide docs have an option to view the source which is useful for specifying a line of code in an Issue. 
Alternatively, selecting "Edit this Page" will prepare you to open a quick Pull Request from your browser.

## Development

To locally test changes to the User Guide, you will need to do the following:

1.  [Install Quarto](https://quarto.org/docs/getting-started/installation.html) or RStudio 2022.07.2 or later, which bundles Quarto.

-   Download RStudio from [posit.co/downloads/](https://posit.co/downloads/)
-   Download the CLI from [github.com/quarto-dev](https://github.com/quarto-dev/quarto-cli/releases/latest) for your operating system
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

5. Once you have confirmed that the changes are technically accurate and that the rendered output is visually correct, you can either open a Pull Request with your changes or an Issue with the suggested fix.

Thank you for your contribution to open source - it will definitely help future users (and probably you)!
