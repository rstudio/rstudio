#
# SessionThemes.R
#
# Copyright (C) 2018 by RStudio, Inc.
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#

# Converts a tmtheme file into an rstheme file.
# 
# @param file              The tmtheme file to convert.
# @param add               Whether to add the converted custom theme to RStudio.
# @param outputLocation    Where to place a local copy of the converted theme.
# @param apply             Whether to immediately apply the new custom theme.
#
# Returns the name of the theme on success.
.rs.addFunction("convertTheme", function (file, add, outputLocation, apply) {
   
   # TODO: validate input 
   # TODO: convert from tmtheme to CSS & extract name
   name <- ""
   
   # TODO: convert from CSS to rstheme
   
   if (add)
   {
      .rs.addTheme(name, apply)
   }
   else if (apply)
   {
      # TODO: error
   }
   
   if (!is.null(outputLocation))
   {
      # TODO: copy to outputLocation
   }
})

# Adds a custom rstheme to RStudio.
#
# @param file     The rstheme file to add.
# @param apply    Whether to immediately apply the newly added theme.
#
# Returns the name of the theme on success.
.rs.addFunction("addTheme", function(file, apply) {
   # TODO: add the theme and get the name.
   name <- ""
   
   if (apply)
   {
      .rs.applyTheme(name)
   }
})

# Applies a theme to RStudio.
#
# @param name     The name of the theme to apply.
.rs.addFunction("applyTheme", function(name) {
   themeList <- .Call("rs_getThemes")
   
   if (!(tolower(name) %in% tolower(themeList)))
   {
      # TODO: error
   }
   
   .rs.api.writePreference("theme", name);
})

# Removes a theme from RStudio. If the removed theme is the current theme, the current theme will be
# set to the default theme.
#
# @param The name of the theme to remove.
.rs.addFunction("removeTheme", function(name) {
   themeLists <- .Call("rs_getThemes")
   currentTheme <- .rs.api.getThemeInfo()$editor
   
   if (!grepl(name, themeLists, ignore.case = TRUE))
   {
      # TODO: error
   }
   
   if (grepl(name, currentTheme, ignore.case = TRUE))
   {
      applyTheme("TextMate");
   }
   
   # TODO: remove theme file
})


# API Functions

# Convert a tmtheme to rstheme and optionally add it to RStudio.
.rs.addApiFunction("convertTheme", function(file, add = TRUE, outputLocation = NULL, apply = FALSE) {
   convertTheme(file, add, outputLocation, apply)
})

# Add a theme to RStudio.
.rs.addApiFunction("addTheme", function(file, apply = FALSE) {
   addTheme(file, apply)
})

# Apply a theme to RStudio.
.rs.addApiFunction("applyTheme", function(name) {
   applyTheme(name)
})

# Remove a theme from RStudio.()
.rs.addApiFunction("removeTheme", function(name) {
   removeTheme(name)
})