# RStudio Theme Maintenance

Most of the themes the RStudio IDE bundles are maintained by pulling the CSS versions of the themes from ACE and regenerating them using the `compile-themes.R` script in the `src/gwt/tools` folder. In general, changes should not be made to `.rstheme` files manually, as they will be overwritten each time that the themes are regenerated. To (re)generate themes:

1. Naviagate to `src/gwt/tools`
2. Run `./sync-ace-commits`
3. Run `Rscript compile-themes.R`

## Manually Maintained Themes

Some themes were contributed to the RStudio IDE as already created `.rstheme` files, and do not have an associated source `.css` file. The following is the current list of themes which need to be updated manually if any changes are required:

* material.rstheme

## Test Cases

The tests cases validate that `tmTheme` files can be converted into Ace CSS  and that the same Ace CSS can then be converted into `rstheme` files . The Ace CSS files are the expected output for the `tmTheme -> Ace CSS` conversion, and the input for the `Ace CSS -> rstheme` conversion.

In addition, the test cases also:
  * validate that invalid `tmTheme`s will produce an error and not an Ace CSS file.
  * validate that files are installed to the global and local theme directories correctly.
  * validate that themes will not be installed to folders which have restrictive permissions.
  * validate that themes will be installed (or not installed) over existing `rsthemes` with the same name based on the options specified.
  * validate that the IDE bundled themes exist and the API returns the correct information about them.

The folder structure of the tests cases is described below:
 * `src/cpp/tests/testthat`
    * `regenerate-css.R` - script which regenerates expected results
    * `test-themes.R` - the themes test cases
    * `themes` - root folder for expected input and output
        * `acecss` - expected output for `tmTheme -> Ace CSS` and input for `Ace CSS -> rsthemes`
        * `errorthemes` - invalid `tmTheme` files that should generate an error
        * `globalInstall` - folder that will be used as the "global" install location. Should be empty after tests complete.
        * `localInstall` - folder that will be used as the "local" install location. Should be empty after tests complete.
        * `nopermission` - folder with restrictive permissions. Should always be empty.
        * `rsthemes` - expected output for `Ace CSS -> rstheme` conversion
        * `thmThemes` - input for `tmTheme -> Ace CSS` conversion

### Fixing the Test Cases

The most common cause of failing test cases is that new fields were added to (or extra fields were removed from) the generated `rstheme` files. The expected results can be updated using the `regenerate-css.R` script by loading the `themes` list from `test-themes.R` into the local R environment and then sourcing `regenerate-css.R`. Note that `xml2` is required, as it would be to install a custom theme to the IDE. After regenerating the expected results, it is a good idea to manually check that the changes in the expected results are only include the changes you expected.

Another easy-to-fix scenario is when a new bundled theme is added to the IDE. To resolve this, add a new line to the `defaultThemes` list in `test-themes.R` (in alphabetical order) for the new theme and set the `fileName` and `isDark` fields as appropriate.

Other problems in the test cases may require debugging. The easiest way to debug the test cases is to add a `browser` call to the failing test case  and then run `testthat::test_file("test-themes.R")` from the R console. Note that this requires the working directory of the R console to be `src/cpp/tests/testthat`. Alternately, a full or relative path to the `test-themes.R` file may be provided.

NOTE: Changes to `src/cpp/session/modules/resources/compile-themes.R` and `src/cpp/session/modules/SessionThemes.R` require the CMake project to be reloaded before they will be observed in the test results.
