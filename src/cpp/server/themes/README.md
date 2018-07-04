RStudio Global Custom Themes
=============================================================================

Add .rstheme files to this directory in order to install themes for all users.

Creating Custom Themes from a tmTheme File
-----------------------------------------------------------------------------
Custom themes can be created and installed globally by calling rstudioapi::convertTheme(<path to tmTheme file>, add = TRUE, globally = TRUE). This function will generate a name for the theme from the input when the theme is created. The theme name can be edited by changing the value of 	`rs-theme-name` inside the generated "rstheme" file.

Adding an Existing rstheme File
-----------------------------------------------------------------------------
"rstheme" files can be installed globally directly by calling rstudioapi::addTheme(<path to rstheme file>, globally = TRUE).

Setting a Custom Theme
-----------------------------------------------------------------------------
Once a custom theme has been installed locally or globally, users connecting to that server can see the custom theme in the list of themes in the Appearance Preferences pane. Alternately, the theme can be set by calling rstudioapi::applyTheme(<unique theme name>).

More Information
-----------------------------------------------------------------------------
For more information about working with the theme management API, see TODO.
