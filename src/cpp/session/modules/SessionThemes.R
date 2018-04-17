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

# Helper Functions
# Converts a colour to an array of the RGB values of the colour.
#
# @param the colur to convert.
#
# Returns the RGB color.
.rs.addFunction("getRgbColor", function(color) {
   if (is.vector(color) && is.integer(color[1])) 
   {
      if (length(color) != 3) 
      {
         stop(sprintf("expected 3 values for RGB color, not %d", length(color)), call. = FALSE)
      }
      colorVec <- color
   }
   else if (substr(color, 0, 1) == "#") 
   {
      if (nchar(color) != 7)
      {
         stop(
            sprintf(
               "hex represntation of RGB values should have format \"#[0-9a-fA-F]{6}\". Found: %s",
               color),
            call. = FALSE)
      }
      else
      {
         colorVec <- sapply(
            c(substr(color, 2, 3), substr(color, 4, 5), substr(color, 6, 7)),
            function(s) { strtoi(paste0("0x", s)) },
            USE.NAMES = FALSE)
      }
   }
   else if (grepl("^rgba?", color))
   {
      matches = regmatches(color, regexec("\\(([^,\\)]+),([^,\\)]+),([^,\\)]+)", color))[[1]]
      if (length(matches) != 4)
      {
         stop(
            sprintf(
               "expected RGB color with format \"rgba?\\([0-9]+, [0-9]+, [0-9]+(, [0-9]+)\\)\". Found: %s",
               color),
            call. = FALSE)
      }
      colorVec <- strtoi(matches[2:4])
   }
   else
   {
      stop(
         sprintf(
            "supplied color has an invalid format: %s. Expected \"#[0-9a-fA-F]{6}\" or \"rgba?\\([0-9]+, [0-9]+, [0-9]+(, [0-9]+)\\)\"",
            color),
         call. = FALSE)
   }
   
   # Check for inconsistencies.
   for (c in colorVec)
   {
      if (is.na(c))
      {
         stop(
            sprintf(
               "invalid color supplied: %s. One or more RGB values could not be converted to an integer",
               color),
            call. = FALSE)
      }
      if (c < 0)
      {
         stop(
            sprintf("invalid color supplied: %s. RGB value cannot be negative", color),
            call. = FALSE)
      }
      if (c > 255)
      {
         stop(
            sprintf("invalid color supplied: %s. RGB value cannot be greater than 255", color),
            call. = FALSE)
      }
   }
   
   colorVec
})

# Converts a theme from a tmtheme to an Ace file. Portions of this function (and sub-functions)
# are ported from https://github.com/ajaxorg/ace/blob/master/tool/tmtheme.js.
.rs.addFunction("convertTmTheme", function(lines) {
   # Syntax highlighting scopes
   scopes <- list()
   
   # Keywords
   scopes[["keyword"]] <- "keyword"
   scopes[["keyword.operator"]] <- "keyword.operator"
   scopes[["keyword.other.unit"]] <- "keyword.other.unit"
   
   # Constants
   scopes[["constant"]] <- "constant"
   scopes[["constant.language"]] <- "constant.language"
   scopes[["constant.library"]] <- "constant.library"
   scopes[["constant.numeric"]] <- "constant.numeric"
   scopes[["constant.character"]] <- "constant.character"
   scopes[["constant.character.escape"]] <- "constant.character.escape"
   scopes[["constant.character.entity"]] <- "constant.character.entity"
   
   # Supports
   scopes[["support"]] <- "support"
   scopes[["support.function"]] <- "support.function"
   scopes[["support.function.dom"]] <- "support.function.dom"
   scopes[["support.function.firebug"]] <- "support.firebug"
   scopes[["support.function.constant"]] <- "support.function.constant"
   scopes[["support.constant"]] <- "support.constant"
   scopes[["support.constant.property-value"]] <- "support.constant.property-value"
   scopes[["support.class"]] <- "support.class"
   scopes[["support.type"]] <- "support.type"
   scopes[["support.other"]] <- "support.other"
   
   # Functions
   scopes[["function"]] <- "function"
   scopes[["function.buildin"]] <- "function.buildin"
   
   # Storages
   scopes[["storage"]] <- "storage"
   scopes[["storage.type"]] <- "storage.type"
   
   # Invalids
   scopes[["invalid"]] <- "invalid"
   scopes[["invalid.illegal"]] <- "invalid.illegal"
   scopes[["invalid.deprecated"]] <- "invalid.deprecated"
   
   # Strings
   scopes[["string"]] <- "string"
   scopes[["string.regexp"]] <- "string.regexp"
   
   # Comments
   scopes[["comment"]] <- "comment"
   scopes[["comment.documentation"]] <- "comment.doc"
   scopes[["comment.documentation.tag"]] <- "comment.doc.tag"
   
   # Variables
   scopes[["variable"]] <- "variable"
   scopes[["variable.language"]] <- "variable.language"
   scopes[["variable.parameter"]] <- "variable.parameter"
   
   # Meta
   scopes[["meta"]] <- "meta"
   scopes[["meta.tag.sgml.doctype"]] <- "xml-pe"
   scopes[["meta.tag"]] <- "meta.tag"
   scopes[["meta.selector"]] <- "meta.selector"
   
   # Entities
   scopes[["entity.other.attribute-name"]] <- "entity.other.attribute-name"
   scopes[["entity.name.function"]] <- "entity.name.function"
   scopes[["entity.name"]] <- "entity.name"
   scopes[["entity.name.tag"]] <- "entity.name.tag"
   
   # Markup
   scopes[["markup.heading"]] <- "markup.heading"
   scopes[["markup.heading.1"]] <- "markup.heading.1"
   scopes[["markup.heading.2"]] <- "markup.heading.2"
   scopes[["markup.heading.3"]] <- "markup.heading.3"
   scopes[["markup.heading.4"]] <- "markup.heading.4"
   scopes[["markup.heading.5"]] <- "markup.heading.5"
   scopes[["markup.heading.6"]] <- "markup.heading.6"
   scopes[["markup.list"]] <- "markup.list"
   
   # Collaborators
   scopes[["collab.user1"]] <- "collab.user1"
   
   # Fallback Scopes
   fallbackScopes <- list()
   fallbackScopes[["keyword"]] <- "meta"
   fallbackScopes[["support.type"]] <- "storage.type"
   fallbackScopes[["variable"]] <- "entity.name.function"
   
   # Default global colours
   defaultGlobals <- list()
   defaultGlobals[["printMargin"]] <- "#e8e8e8"
   defaultGlobals[["background"]] <- "#ffffff"
   defaultGlobals[["foreground"]] <- "#000000"
   defaultGlobals[["gutter"]] <- "#f0f0f0"
   defaultGlobals[["selection"]] <- "rgb(181, 213, 255)"
   defaultGlobals[["step"]] <- "rgb(198, 219, 174)"
   defaultGlobals[["bracket"]] <- "rgb(192, 192, 192)"
   defaultGlobals[["active_line"]] <- "rgba(0, 0, 0, 0.07)"
   defaultGlobals[["cursor"]] <- "#000000"
   defaultGlobals[["invisible"]] <- "rgb(191, 191, 191)"
   defaultGlobals[["fold"]] <- "#6b72e6"
   
   # TODO: return value.
   invisible(NULL)
})

# Worker functions

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
