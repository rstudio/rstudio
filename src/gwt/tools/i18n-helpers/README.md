# Summary

This README describes the internationalization (i18n) development workflow and helper tools available.

# i18n in RStudio

## Implementation Details

i18n is implemented using GWT's [i18n](http://www.gwtproject.org/doc/latest/DevGuideI18n.html) features, typically through static string internationalization.  Until i18n has been enabled across RStudio, non-English locales are enabled only when running `ant` in `SuperDevMode`.  

To localize part of the codebase, define interfaces that extend `com.google.gwt.i18n.client.Constants` and `.Messages`.  Include `String`s with `@DefaultStringValue`s in the interfaces for all localized text, and cite these `String` objects in your code to get their text values.  To add locales to your application, add properties files called `INTERFACENAME_LOCALE.properties`.  An example of this implemented is available in `src/org/rstudio/studio/client/application/ui/AboutDialog.java`, which cites:
* `AboutDialogConstants.java` (interface for constants)
* `AboutDialogConstants_en.properties` (English locale for constants)
* `AboutDialogConstants.java` (interface for messages)
* `AboutDialogMessages_en.properties` (English locale for messages)

When debugging, access non-English locales from `SuperDevMode` by adding `?locale=yourLocale` to the RStudio URL (for example, `http://localhost:8787/?locale=yourLocale`).

When serving localized content, GWT will serve it in the following order:
* Locale matching the locale selected, if available (if `?locale=en`, serve `*_en.properties` if it is available)
* Default locale (defined in `RStudio.gwt.xml` or other XML files), if available
* The `@DefaultStringValue` text

GWT suggests you always include both a `@DefaultStringValue` and at least one `.properties` file.

## Development Workflow 

When implementing i18n and going from hard-coded English text to text from an English properties file, it can be hard to know what is/is not translated and whether your translations are actually working because you are "translating" without changing the visible content.  One workflow to help with this problem is to use a "dev" locale which applies easily visible changes to the text for development purposes.  The "dev" locale is:
* a copy of the current English locale's `.properties` files, with "@" prepended to all constants and messages to make them clearly visible in the UI
* enabled when in SuperDevMode (server or desktop) and accessible at `http://localhost:8787/?locale=dev`, but not accessible during production use (see `RStudioSuperDevMode.gwt.xml`/`RStudioDesktopSuperDevMode.gwt.xml`)
* intended to be generated when needed during development and to be committed with the codebase (files are ignored in `.gitignore`)

For example, below shows a "dev" locale where menus and commands have i18n support but other text does not:

![Example of partly-translated dev locale](./rstudio-dev-locale-example.png)

# Tools

## create_dev_locale.sh

Useful for debugging i18n and visually confirming what is/is not i18n-enabled by creating `*_dev.properties` files from existing `*_en.properties` files.  The script copies the English properties files and prefixes their texts with `@`. 

Run this script from `/src/gwt/src` with syntax `./create_dev_locale.sh`
