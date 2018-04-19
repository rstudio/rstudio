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
# @param color    The color to convert.
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

# Mixes two colours together.
#
# @param color1   The first colour.
# @param color2   The second colour.
# @param alpha1   The alpha of the first colour.
# @param alpha2   The alpha of the second colour.
# 
# Returns the mixed colour in string format.
.rs.addFunction("mixColors", function(color1, color2, alpha1, alpha2 = NULL) {
   c1rgb <- .rs.getRgbColor(color1)
   c2rgb <- .rs.getRgbColor(color2)
   
   if (is.null(alpha2))
   {
      alpha2 = 1 - alpha1
   }
   
   paste0(
      "rgb(",
      paste(
         round(alpha1 * c1rgb[[1]] + alpha2 * c2rgb[[1]]),
         round(alpha1 * c1rgb[[2]] + alpha2 * c2rgb[[2]]),
         round(alpha1 * c1rgb[[3]] + alpha2 * c2rgb[[3]]),
         sep = ","),
      ")")
})

# Determines the luma of a colour. It's not quite clear what a "luma" is.
# 
# @param color    The colour for which to determine the luma.
#
# Returns the luma of the specified colour.
.rs.addFunction("getLuma", function(color) {
   # The numbers used in this calculation are taken from
   # https://github.com/ajaxorg/ace/blob/master/tool/tmtheme.js#L191. It is not entirely clear
   # why they were chosen, as there are no comments in the orginal.
   rgb <- .rs.getRgbColor(color)
   (0.21 * rgb[[1]] + 0.72 * rgb[[2]] + 0.07 * rgb[[3]]) / 255
})

# Parses a "key" element from a tmtheme document and raises appropriate error.
#
# @param element     The element to parse.
#
# Returns the text value of the element.
.rs.addFunction("parseKeyElement", function(element) {
   parseError <- "Unable to convert the tmtheme to an rstheme."
   key <- XML::xmlValue(element)
   if (key == "")
   {
      stop(
         sprintf("%s The value of a \"key\" element may not be empty.", parseError),
         call. = FALSE)
   }
   
   key
})

# Parses a "string" element from a tmtheme document and raises appropriate errors.
#
# @param element     The element to parse.
# @param keyName     The name of the key for this value.
#
# Returns the text value of the element.
.rs.addFunction("parseStringElement", function(element, keyName)
{
   parseError <- "Unable to convert the tmtheme to an rstheme."
   value <- XML::xmlValue(element)
   
   # The key can only be null if there was no <key> element immediately preceding the <string>
   # element in the provided xml. If any <key> element was found (even a <key/>), the key name
   # will at least be "".
   if (is.null(keyName))
   {
      stop(
         sprintf(
            "%s Unable to find a key for the \"string\" element with value \"%s\".",
            parseError,
            value),
         call. = FALSE)
   }
   
   value
})

# Recursivley parses a dictionary element from a tmtheme document and raises the correct errors.
#
# @param dictElement    The element to parse.
# @param keyName        The name of the dictionary.
#
# Returns a list with named values.
.rs.addFunction("parseDictElement", function(dictElement, keyName) {
   parseError <- "Unable to convert the tmtheme to an rstheme."
   if (is.null(keyName))
   {
      stop(
         sprintf("%s Unable to find a key for the current \"dict\" element.", parseError),
         call. = FALSE)
   }
   if (XML::xmlSize(dictElement) < 1)
   {
      stop(
         sprintf("%s \"dict\" element cannot be empty.", parseError),
         call. = FALSE)
   }
   
   values <- list()
   key <- NULL
   for (element in XML::xmlChildren(dictElement))
   {
      elName <- XML::xmlName(element)
      if (elName == "key")
      {
         key <- .rs.parseKeyElement(element)
      }
      else if (elName == "string")
      {
         # Add the key-value pair to the theme object and reset the key to NULL to avoid erroneously
         # using the same key twice.
         values[[key]] <- .rs.parseStringElement(element, key)
         key <- NULL
      }
      else if (elName == "dict")
      {
         values[[key]] <- .rs.parseDictElement(element, key)
         key <- NULL
      }
      else if (elName == "array")
      {
         values[[key]] <- .rs.parseArrayElement(element, key)
         key <- NULL
      }
      else
      {
         stop(
            sprintf(
               "%s Encountered unexpected element as a child of the current \"dict\" element: \"%s\". Expected \"key\", \"string\", \"array\", or \"dict\".",
               parseError,
               elName),
            call. = FALSE)
      }
   }
   
   values
})

# Recursively parses the list of settings from a tmtheme document.
#
# @param arrayElement      The <array> element to parse.
# @param keyName           The name of the key for this array element.
#
# Returns a list() of named settings.
.rs.addFunction("parseArrayElement", function(arrayElement, keyName) {
   parseError <- "Unable to convert the tmtheme to an rstheme."
   if (XML::xmlSize(arrayElement) < 1)
   {
      stop(
         sprintf("%s \"array\" element cannot be empty.", parseError),
         call. = FALSE)
   }
   
   if (is.null(keyName))
   {
      stop(
         sprintf("%s Unable to find a key for array value.", parseError),
         call. = FALSE)
   }
   if (keyName != "settings")
   {
      stop(
         sprintf(
            "%s Incorrect key for array element. Expected: \"settings\"; Actual: \"%s\".",
            parseError,
            keyName),
         call. = FALSE)
   }
   
   values <- list()
   index <- 1
   for (element in XML::xmlChildren(arrayElement))
   {
      elName <- XML::xmlName(element)
      if (elName != "dict")
      {
         stop(
            sprintf("%s Expecting \"dict\" element; found \"%s\".", parseError, elName),
            call. = FALSE)
      }
      
      # Intentionally empty key here
      values[[index]] <- .rs.parseDictElement(element, "")
      index <- index + 1
   }
   
   values
})

# Parses a tmtheme document and stores the relevant values in a list with named values.
#
# @param file     The file to parse.
# 
# Returns a list with named values.
.rs.addFunction("parseTmTheme", function(file) {
   parseError <- "Unable to convert the tmtheme to an rstheme."
   
   tmThemeDoc <- XML::xmlRoot(
      XML::xmlParse(
         file, 
         error = function(msg, code, domain, line, col, level, filename) {
            stop(
               sprintf(
                  "%s An error occurred while parsing %s at line %d: %s",
                  parseError,
                  filename,
                  line,
                  msg),
               call. = FALSE)
   }))

   # Skip the plist (root) and first level of dict.
   if (XML::xmlSize(tmThemeDoc) != 1)
   {
      stop(
         sprintf(
            "%s Expected 1 child of the root, found: %d",
            parseError,
            XML::xmlSize(tmThemeDoc)),
         call. = FALSE)
   }

   # Intentionally empty key here
   .rs.parseDictElement(tmThemeDoc[[1]], "")
})

# Converts a theme from a tmtheme to an Ace file.
.rs.addFunction("convertTmTheme", function(file) {
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
   # Require XML package for parsing the tmtheme files.
   if (!suppressWarnings(require("XML", quietly = TRUE)))
   {
      stop("Taking this action requires the XML library. Please run 'install.packages(\"XML\")' before continuing.")
   }
   
   .rs.convertTheme(file, add, outputLocation, apply)
})

# Add a theme to RStudio.
.rs.addApiFunction("addTheme", function(file, apply = FALSE) {
   .rs.addTheme(file, apply)
})

# Apply a theme to RStudio.
.rs.addApiFunction("applyTheme", function(name) {
   .rs.applyTheme(name)
})

# Remove a theme from RStudio.()
.rs.addApiFunction("removeTheme", function(name) {
   .rs.removeTheme(name)
})
