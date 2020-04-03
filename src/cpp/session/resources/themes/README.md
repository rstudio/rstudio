# RStudio Theme Maintenance

Most of the themes the RStudio IDE bundles are maintained by pulling the CSS versions of the themes from ACE and regenerating them using the `compile-themes.R` script in the `src/gwt/tools` folder. In general, changes should not be made to `.rstheme` files manually, as they will be overwritten each time that the themes are regenerated. To (re)generate themes:

1. Naviagate to `src/gwt/tools`
2. Run `./sync-ace-commits`
3. Run `Rscript compile-themes.R`

## Manually Maintained Themes

Some themes were contributed to the RStudio IDE as already created `.rstheme` files, and do not have an associated source `.css` file. The following is the current list of themes which need to be updated manually if any changes are required:

* material.rstheme