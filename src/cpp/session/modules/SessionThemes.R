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

# ACE/tools/tmThemes.js Functions ==================================================================

# Converts a color to an array of the RGB values of the color.
#
# @param color    The color to convert.
#
# Returns the RGB color.
.rs.addFunction("getRgbColor", getRgbColor <- function(color) {
   if (is.vector(color) && any(is.integer(color))) 
   {
      if (length(color) != 3) 
      {
         stop(
            "expected 3 values for RGB color, not ",
            length(color),
            call. = FALSE)
      }
      colorVec <- color
   }
   else if (substr(color, 0, 1) == "#") 
   {
      if (nchar(color) != 7)
      {
         stop(
            "hex representation of RGB values should have the format \"#RRGGBB\", where `RR`, `GG` and `BB` are in [0x00, 0xFF]. Found: ",
            color,
            call. = FALSE)
      }
      else
      {
         colorVec <- sapply(
            c(substr(color, 2, 3), substr(color, 4, 5), substr(color, 6, 7)),
            hexStrToI <- function(str) { strtoi(str, 16L) },
            USE.NAMES = FALSE)
      }
   }
   else if (grepl("^rgba?", color))
   {
      matches = regmatches(color, regexec("\\(([^,\\)]+),([^,\\)]+),([^,\\)]+)", color))[[1]]
      if (length(matches) != 4)
      {
         stop(
            "non-hex representation of RGB values should have the format \"rgb(R, G, B)\" or \"rgba(R, G, B, A)\" where `R`, `G`, and `B` are integer values in [0, 255] and `A` is decimal value in [0, 1.0]. Found: ",
            color,
            call. = FALSE)
      }
      colorVec <- strtoi(matches[2:4])
   }
   else
   {
      stop(
         "supplied color has an invalid format: ",
         color,
         ". Expected \"#RRGGBB\", \"rgb(R, G, B) or \"rgba(R, G, B, A)\", where `RR`, `GG` and `BB` are in [0x00, 0xFF], `R`, `G`, and `B` are integer values in [0, 255], and `A` is decimal value in [0, 1.0]",
         call. = FALSE)
   }
   
   # Check for inconsistencies.
   invalidMsg <- paste0("invalid color supplied: ", color, ". ")
   if (any(is.na(colorVec)) || any(!is.integer(colorVec)))
   {
      stop(
         invalidMsg,
         "One or more RGB values could not be converted to an integer",
         call. = FALSE)
   }
   if (any(colorVec < 0))
   {
      stop(invalidMsg, "RGB value cannot be negative", call. = FALSE)
   }
   if (any(colorVec > 255))
   {
      stop(invalidMsg, "RGB value cannot be greater than 255", call. = FALSE)
   }
   
   colorVec
})

# Mixes two colors together.
#
# @param color1   The first color.
# @param color2   The second color.
# @param alpha1   The alpha of the first color.
# @param alpha2   The alpha of the second color.
# 
# Returns the mixed color in string format.
.rs.addFunction("mixColors", mixColors <- function(color1, color2, alpha1, alpha2 = NULL) {
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

# Determines the luma of a color. This refers to the perceived luminance of the color. More 
# information can be found at https://en.wikipedia.org/wiki/Relative_luminance.
# 
# @param color    The color for which to determine the luma.
#
# Returns the luma of the specified color.
.rs.addFunction("getLuma", getLuma <- function(color) {
   # The numbers used in this calculation are taken from
   # https://github.com/ajaxorg/ace/blob/master/tool/tmtheme.js#L191. It is not entirely clear
   # why they were chosen, as there are no comments in the orginal.
   rgb <- .rs.getRgbColor(color)
   (0.21 * rgb[[1]] + 0.72 * rgb[[2]] + 0.07 * rgb[[3]]) / 255
})

# Parses a color.
# 
# @param color    The color to parse.
#
# Returns the parsed color.
.rs.addFunction("parseColor", parseColor <- function(color) {
   colorLen <- nchar(color)
   if (colorLen < 1)
   {
      return(NULL)
   }
   
   if (colorLen == 4)
   {
      color <- gsub("([a-fA-F0-9])", "\\1\\1", color)
   }
   if (!grepl("^#[a-fA-F0-9]{6}$", color))
   {
      if (!grepl("^#[a-fA-F0-9]{8}$", color))
      {
         stop(
            "Unable to parse color: ",
            color, 
            "it must have format \"#RGB\", \"#RRGGBB\" or \"#RRGGBBAA\" where `R`, `G`, `B`, and `A` are in [0x00, 0xFF].",
            call. = FALSE)
      }
      
      aVal <- format(
         round(strtoi(substr(color, 8, 9), base = 16L) / 255, digits = 2),
         nsmall = 2)
      color <- paste0(
         "rgba(",
         paste(
            c(.rs.getRgbColor(substr(color, 1, 7)), aVal),
            collapse = ", "),
         ")")
   }
   
   color
})

# Parses a font style and converts it to CSS.
#
# @param styles      The style(s) to convert.
#
# Returns the converted font style.
.rs.addFunction("parseStyles", parseStyles <- function(styles) {
   cssLines <- character(0)
   fontStyle <- if (is.null(styles$fontStyle)) "" else styles$fontStyle
   
   if (grepl("underline", fontStyle))
   {
      # Not the most efficient, but this shouldn't be an overly common operation.
      cssLines <- c(cssLines, "text-decoration:underline;")
   }
   if (grepl("italic", fontStyle))
   {
      cssLines <- c(cssLines, "font-style:italic;")
   }
   
   if (!is.null(styles$foreground))
   {
      cssLines <- c(cssLines, paste0("color:", .rs.parseColor(styles$foreground), ";"))
   }
   if (!is.null(styles$background))
   {
      cssLines <- c(cssLines, paste0("background-color:", .rs.parseColor(styles$background), ";"))
   }
   
   paste0(cssLines, collapse = "")
})

# Extracts the style information from a parsed tmTheme object.
#
# @param theme             The parsed tmTheme object.
# @param supportScopes     tmTheme scopes that are supported by ace and a mapping to their ace
#                          value.
# 
# Returns a list which contains the styles and unsupportedScopes in named elements.
.rs.addFunction("extractStyles", extractStyles <- function(theme, supportedScopes) {
   # Fallback Scopes
   fallbackScopes <- list(
      "keyword" = "meta",
      "keyword.operator" = "keyword",
      "support.type" = "storage.type",
      "variable" = "entity.name.function")
   
   # Default Global Values
   defaultGlobals <- list(
      "printMargin" = "#e8e8e8",
      "background" = "#ffffff",
      "foreground" = "#000000",
      "gutter" = "#f0f0f0",
      "selection" = "rgb(181, 213, 255)",
      "step" = "rgb(198, 219, 174)",
      "bracket" = "rgb(192, 192, 192)",
      "active_line" = "rgba(0, 0, 0, 0.07)",
      "cursor" = "#000000",
      "invisible" = "rgb(191, 191, 191)",
      "fold" = "#6b72e6")
   
   globalSettings <- theme$settings[[1]]$settings
   
   # Get the global colors
   invisColor <- .rs.parseColor(globalSettings$invisibles)
   if (is.null(invisColor)) invisColor <- defaultGlobals$invisibles
   
   styles <- list(
      "printMargin" = defaultGlobals$printMargin,
      "background" = .rs.parseColor(globalSettings$background),
      "foreground" = .rs.parseColor(globalSettings$foreground),
      "gutter" = defaultGlobals$gutter,
      "selection" = .rs.parseColor(globalSettings$selection),
      "step" = defaultGlobals$step,
      "bracket" =  invisColor,
      "active_line" = .rs.parseColor(globalSettings$lineHighlight),
      "cursor" = .rs.parseColor(globalSettings$caret),
      "invisible" = paste0("color:", invisColor, ";"))
   
   if (is.null(styles$background)) styles$background <- defaultGlobals$background
   if (is.null(styles$foreground)) styles$foreground <- defaultGlobals$foreground
   if (is.null(styles$selection)) styles$selection <- defaultGlobals$selection
   if (is.null(styles$active_line)) styles$active_line <- defaultGlobals$active_line
   if (is.null(styles$cursor)) styles$cursor <- defaultGlobals$cursor
   
   # Get the specified scopes
   unsupportedScopes <- list()
   supportedScopeNames <- names(supportedScopes)
   for (element in theme$settings)
   {
      if (!is.null(element$scope) && !is.null(element$settings))
      {
         # Split on commas and strip whitespace.
         scopes <- strsplit(gsub("^\\s*|\\s*$", "", element$scope), "\\s*(,|\\|\\s)\\s*")[[1]]
         for (scope in scopes)
         {
            style <- .rs.parseStyles(element$settings)
            if (scope %in% supportedScopeNames)
            {
               styles[[ supportedScopes[[scope]] ]] <- style
            }
            else if (!is.null(style))
            {
               if (!(scope %in% names(unsupportedScopes)))
               {
                  unsupportedScopes[[scope]] <- 0
               }
               else 
               {
                  unsupportedScopes[[scope]] <- unsupportedScopes[[scope]] + 1
               }
            }
         }
      }
   }
   
   fScopeNames <- names(fallbackScopes)
   for (i in 1:length(fallbackScopes))
   {
      name <- fScopeNames[i]
      scope <- fallbackScopes[[i]]
      if (is.null(styles[[name]]) || (styles[[name]] == ""))
      {
         if (is.null(styles[[scope]]) || (styles[[scope]] == ""))
         {
            # All fallback elements are foreground for now.
            styles[[name]] <- paste0("color:", styles$foreground, ";")
         }
         else 
         {
            styles[[name]] <- styles[[scope]]
         }
      }
   }
   
   if (is.null(styles$fold))
   {
      foldSource <- styles$entity.name.function
      if (is.null(foldSource) || (foldSource == "")) foldSource <- styles$keyword
      
      if (!is.null(foldSource) && (foldSource != ""))
      {
         styles$fold <- regmatches(foldSource, regexec("\\:([^;]+)", foldSource))[[1]][2]
      }
      else
      {
         styles$fold <- defaultGlobals$fold
      }
   }
   
   styles$gutterBg = styles$background
   styles$gutterFg = .rs.mixColors(styles$foreground, styles$background, 0.5)
   
   if (is.null(styles$selected_word_highlight))
   {
      styles$selected_word_highlight <- paste0("border: 1px solid ", styles$selection, ";")
   }
   
   styles$isDark = tolower(as.character(.rs.getLuma(styles$background) <  0.5))
   
   list("styles" = styles, "unsupportedScopes" = unsupportedScopes)
})

# Converts a theme from a tmTheme to an Ace css theme.
#
# @param tmTheme     The parsed tmTheme object.
# 
# Returns Ace css theme as a string.
.rs.addFunction("convertTmTheme", convertTmTheme <- function(tmTheme) {
   # Helper functions 
   
   # Fills a template value with the given replacements.
   #
   # @param template          The template to fill.
   # @param replacements      The replacements to fill the template with.
   #
   # Returns a string of the filled template.
   fillTemplate <- function(template, replacements) {
      repeat
      {
         matches <- regmatches(template, regexec("%(.+?)%", template))[[1]]
         if (length(matches) != 2)
         {
            break
         }
         
         replacement <- replacements[[matches[2]]]
         if (is.null(replacement)) replacement <- ""
         template <- sub(matches[1], replacement, template)
      }
      
      template
   }
   
   # Hyphenates a title or camel case string, separating words and replacing "_" with "-".
   # 
   # @param string      The string to convert to lower case with hyphens.
   #
   # Returns the converted string.
   hyphenate <- function(string) {
      tolower(
            gsub("-+", "-", # Get rid of duplicate hyphens
               sub(
                  "^-", "", # Get rid of leading hyphen
                  gsub(
                     "_",
                     "-", # Replace _ with hyphen
                     gsub(
                        "([^0-9A-Z])([A-Z])", # Insert hyphen before capital letters which are not immediately preceded by a digit or another capital letter
                        "\\1-\\2",
                        gsub("(?:'|\"|\\(([^\\)]*)\\))", "\\1", # Remove special characters and antyhing encased in parantheses
                             gsub("&","-and-", # Replace & with And
                                 gsub("\\s", "-", string)))))))) # Replace whitespace with hyphen
   }
   
   # Quotes a string and adds extra escapes to escape characters.
   #
   # @param string      The string to quoted.
   #
   # Returns the processed string
   quoteStr <- function(string) {
      paste0(
         "\"",
         gsub("\n", "\\n", gsub("\"", "\\\"", gsub("\\", "\\\\", string))),
         "\"")
   }
   
   # Syntax highlighting scopes
   supportedScopes <- list()
   
   # Keywords
   supportedScopes[["keyword"]] <- "keyword"
   supportedScopes[["keyword.operator"]] <- "keyword.operator"
   supportedScopes[["keyword.other.unit"]] <- "keyword.other.unit"
   
   # Constants
   supportedScopes[["constant"]] <- "constant"
   supportedScopes[["constant.language"]] <- "constant.language"
   supportedScopes[["constant.library"]] <- "constant.library"
   supportedScopes[["constant.numeric"]] <- "constant.numeric"
   supportedScopes[["constant.character"]] <- "constant.character"
   supportedScopes[["constant.character.escape"]] <- "constant.character.escape"
   supportedScopes[["constant.character.entity"]] <- "constant.character.entity"
   supportedScopes[["constant.other"]] <- "constant.other"
   
   # Supports
   supportedScopes[["support"]] <- "support"
   supportedScopes[["support.function"]] <- "support.function"
   supportedScopes[["support.function.dom"]] <- "support.function.dom"
   supportedScopes[["support.function.firebug"]] <- "support.firebug"
   supportedScopes[["support.function.constant"]] <- "support.function.constant"
   supportedScopes[["support.constant"]] <- "support.constant"
   supportedScopes[["support.constant.property-value"]] <- "support.constant.property-value"
   supportedScopes[["support.class"]] <- "support.class"
   supportedScopes[["support.type"]] <- "support.type"
   supportedScopes[["support.other"]] <- "support.other"
   
   # Functions
   supportedScopes[["function"]] <- "function"
   supportedScopes[["function.buildin"]] <- "function.buildin"
   
   # Storages
   supportedScopes[["storage"]] <- "storage"
   supportedScopes[["storage.type"]] <- "storage.type"
   
   # Invalids
   supportedScopes[["invalid"]] <- "invalid"
   supportedScopes[["invalid.illegal"]] <- "invalid.illegal"
   supportedScopes[["invalid.deprecated"]] <- "invalid.deprecated"
   
   # Strings
   supportedScopes[["string"]] <- "string"
   supportedScopes[["string.regexp"]] <- "string.regexp"
   
   # Comments
   supportedScopes[["comment"]] <- "comment"
   supportedScopes[["comment.documentation"]] <- "comment.doc"
   supportedScopes[["comment.documentation.tag"]] <- "comment.doc.tag"
   
   # Variables
   supportedScopes[["variable"]] <- "variable"
   supportedScopes[["variable.language"]] <- "variable.language"
   supportedScopes[["variable.parameter"]] <- "variable.parameter"
   
   # Meta
   supportedScopes[["meta"]] <- "meta"
   supportedScopes[["meta.tag.sgml.doctype"]] <- "xml-pe"
   supportedScopes[["meta.tag"]] <- "meta.tag"
   supportedScopes[["meta.selector"]] <- "meta.selector"
   
   # Entities
   supportedScopes[["entity.other.attribute-name"]] <- "entity.other.attribute-name"
   supportedScopes[["entity.name.function"]] <- "entity.name.function"
   supportedScopes[["entity.name"]] <- "entity.name"
   supportedScopes[["entity.name.tag"]] <- "entity.name.tag"
   
   # Markup
   supportedScopes[["markup.heading"]] <- "markup.heading"
   supportedScopes[["markup.heading.1"]] <- "markup.heading.1"
   supportedScopes[["markup.heading.2"]] <- "markup.heading.2"
   supportedScopes[["markup.heading.3"]] <- "markup.heading.3"
   supportedScopes[["markup.heading.4"]] <- "markup.heading.4"
   supportedScopes[["markup.heading.5"]] <- "markup.heading.5"
   supportedScopes[["markup.heading.6"]] <- "markup.heading.6"
   supportedScopes[["markup.list"]] <- "markup.list"
   
   # Collaborators
   supportedScopes[["collab.user1"]] <- "collab.user1"
   
   # Read the template files
   conn <- file(
      description = file.path(.Call("rs_rResourcesPath"), "templates", "ace_theme_template.css"),
      open = "rt")
   cssTemplate <- paste0(readLines(conn), collapse ="\n")
   close(conn)
   
   # Extract styles
   name <- tmTheme$name
   if (is.null(name)) name <- ""
   styleRes <- .rs.extractStyles(tmTheme, supportedScopes)
   styles <- styleRes$styles
   unsupportedScopes <- styleRes$unsupportedScopes

   # Fill template
   styles$cssClass = paste0("ace-", hyphenate(name))
   styles$uuid <- tmTheme$uuid
   css <- fillTemplate(cssTemplate, styles)
   
   for (scope in supportedScopes)
   {
      if (!is.null(styles[[scope]]))
      {
         css = paste0(
            css,
            "\n\n.",
            styles$cssClass,
            " ",
            gsub("^|\\.", ".ace_", scope),
            " {\n  ",
            gsub(":([^ ])", ": \\1", gsub(";([^\n])", ";\n\\1", styles[[scope]])),
            "}")
      }
   }
   
   list(
      "theme" = strsplit(gsub("[^\\{\\}]+\\{\\s*\\}", "", css, perl = TRUE), "\n")[[1]],
      "isDark" = as.logical(styles$isDark))
})

# TmTheme XML Parsing functions ====================================================================

# Parses a "key" element from a tmtheme document and raises appropriate error.
#
# @param element     The element to parse.
#
# Returns the text value of the element.
.rs.addFunction("parseKeyElement", parseKeyElement <- function(element) {
   parseError <- "Unable to convert the tmtheme to an rstheme."
   key <- xml2::xml_text(element)
   if (key == "")
   {
      stop(parseError, " The value of a \"key\" element may not be empty.", call. = FALSE)
   }
   
   key
})

# Parses a "string" element from a tmtheme document and raises appropriate errors.
#
# @param element     The element to parse.
# @param keyName     The name of the key for this value.
#
# Returns the text value of the element.
.rs.addFunction("parseStringElement", parseStringElement <- function(element, keyName) {
   parseError <- "Unable to convert the tmtheme to an rstheme."
   value <- xml2::xml_text(element)
   
   # The key can only be null if there was no <key> element immediately preceding the <string>
   # element in the provided xml. If any <key> element was found (even a <key/>), the key name
   # will at least be "".
   if (is.null(keyName))
   {
      stop(
         parseError,
         " Unable to find a key for the \"string\" element with value \"",
         value,
         "\".",
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
.rs.addFunction("parseDictElement", parseDictElement <- function(dictElement, keyName) {
   parseError <- "Unable to convert the tmtheme to an rstheme."
   if (is.null(keyName))
   {
      stop(parseError, " Unable to find a key for the current \"dict\" element.", call. = FALSE)
   }
   
   values <- list()
   if (xml2::xml_length(dictElement) >= 1)
   {
      key <- NULL
      for (element in xml2::xml_children(dictElement))
      {
         elName <- xml2::xml_name(element)
         if (elName != "comment")
         {
            if (elName == "key")
            {
               if (!is.null(key))
               {
                  stop(
                     parseError,
                     " Unable to find a value for the key \"",
                     key,
                     "\".",
                     call. = FALSE)
               }
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
                  parseError,
                  " Encountered unexpected element as a child of the current \"dict\" element: \"",
                  elName,
                  "\". Expected \"key\", \"string\", \"array\", or \"dict\".",
                  call. = FALSE)
            }
         }
      }
      
      if (!is.null(key))
      {
         stop(
            parseError,
            " Unable to find a value for the key \"",
            key,
            "\".",
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
.rs.addFunction("parseArrayElement", parseArrayElement <- function(arrayElement, keyName) {
   parseError <- "Unable to convert the tmtheme to an rstheme."
   if (xml2::xml_length(arrayElement) < 1)
   {
      stop(parseError, " \"array\" element cannot be empty.", call. = FALSE)
   }
   
   if (is.null(keyName))
   {
      stop(parseError, " Unable to find a key for array value.", call. = FALSE)
   }
   if (keyName != "settings")
   {
      stop(
         parseError,
         " Incorrect key for array element. Expected: \"settings\"; Actual: \"",
         keyName,
         "\".",
         call. = FALSE)
   }
   
   values <- list()
   index <- 1
   for (element in xml2::xml_children(arrayElement))
   {
      elName <- xml2::xml_name(element)
      if (elName != "comment")
      {
         if (elName != "dict")
         {
            stop(
               parseError,
               " Expecting \"dict\" element; found \"",
               elName,
               "\".",
               call. = FALSE)
         }
         
         # Intentionally empty key here
         values[[index]] <- .rs.parseDictElement(element, "")
         index <- index + 1
      }
   }
   
   values
})

# Parses a tmtheme document and stores the relevant values in a list with named values.
#
# @param filePath     The path of the file to parse.
# 
# Returns a list with named values.
.rs.addFunction("parseTmTheme", parseTmTheme <- function(filePath) {
   parseError <- "Unable to convert the tmtheme to an rstheme."
   
   xmlFile <- file(filePath)
   xmlStr <- paste0(readLines(xmlFile), collapse = "\n")
   close(xmlFile)
   
   tmThemeDoc <- xml2::xml_root(xml2::read_xml(
      xmlStr, 
      error = function(msg, code, domain, line, col, level, filename) {
         stop(
            parseError,
            " An error occurred while parsing ",
            filename,
            " at line ",
            line,
            ": ",
            msg,
            call. = FALSE)
      },
      encoding = "UTF-8"))
   
   # Check for the right number of children
   childrenCount <- xml2::xml_length(tmThemeDoc)
   if (childrenCount != 1)
   {
      stop(parseError,
         " Expected 1 non-text child of the root, found: ",
         childrenCount,
         call. = FALSE)
   }
   
   # Check the structure at the root is correct before continuing.
   if (xml2::xml_name(xml2::xml_child(tmThemeDoc, 1)) != "dict")
   {
      stop(
         parseError,
         " Expecting \"dict\" element; found \"",
         xml2::xml_name(xml2::xml_child(tmThemeDoc, 1)),
         "\".",
         call. = FALSE)
   }
   if (xml2::xml_length(xml2::xml_child(tmThemeDoc, 1)) < 1)
   {
      stop(
         parseError,
         " \"dict\" element cannot be empty.",
         call. = FALSE)
   }
   
   # Skip the plist (root), first child is a <dict>.
   # Intentionally empty key here
   .rs.parseDictElement(xml2::xml_child(tmThemeDoc, 1), "")
})

# compile-themes Functions =========================================================================

# Converts an ace theme to an RStudio theme.
# 
# @param aceCss      The ace CSS to convert.
# @param name        The name 
.rs.addFunction("convertAceTheme", convertAceTheme <- function(name, aceCss, isDark) {
   library("highlight")
   source(file.path(.Call("rs_rResourcesPath"), "themes", "compile-themes.R"))
   
   rsTheme <- .rs.compile_theme(aceCss, isDark)
   if (length(rsTheme) == 0)
   {
      stop(
         "Unable to convert \"",
         name,
         "\" to RStudio theme. Please see above for warnings.",
         .call = FALSE)
   }
   
   c(
      paste0("/* rs-theme-name: ", name, " */"),
      paste0("/* rs-theme-is-dark: ", isDark, " */"),
      rsTheme,
      "\n")
})

# Worker Functions =================================================================================
# Gets the install location.
#
# @param global    Whether to get the global or local install dir.
#
# Returns the install location.
.rs.addFunction("getThemeInstallDir", getThemeInstallDir <- function(global) 
{
   # Copy the file to the correct location.
   installLocation <- ""
   if (global)
   {
      installLocation <- Sys.getenv("RS_THEME_GLOBAL_HOME", unset = NA)
      if (is.na(installLocation))
      {
         if (grepl("windows", Sys.info()[["sysname"]], ignore.case = TRUE))
         {
            installLocation <- file.path(Sys.getenv("ProgramData"), "RStudio", "themes")
         }
         else 
         {
            installLocation <- file.path("/etc", "rstudio", "themes")
         }
      }
   }
   else
   {
      installLocation <- Sys.getenv("RS_THEME_LOCAL_HOME", unset = NA)
      installLocation <- if (is.na(installLocation)) file.path("~", ".R", "rstudio", "themes") 
                         else installLocation
   }
   
   installLocation
})

.rs.addFunction("getThemeDirFromUrl", getThemeDirFromUrl <- function(url) {
   if (grepl("^/theme/custom/global.*?\\.rstheme$", url, ignore.case = TRUE))
   {
      file.path(.rs.getThemeInstallDir(TRUE), basename(url))
   }
   else if (grepl("^/theme/custom/local.*\\.rstheme$", url, ignore.case= TRUE))
   {
      file.path(.rs.getThemeInstallDir(FALSE), basename(url))
   }
   else
   {
      NULL
   }
})

# Converts a tmtheme file into an rstheme file.
# 
# @param themePath         The tmtheme file to convert.
# @param add               Whether to add the converted custom theme to RStudio.
# @param outputLocation    Where to place a local copy of the converted theme.
# @param apply             Whether to immediately apply the new custom theme.
# @param force             Whether to force the operation when it may involve an overwrite.
# @param globally          Whether to add the theme for all users (true) or just this user (false).
#
# Returns the name of the theme on success.
.rs.addFunction("convertTheme", convertTheme <- function(themePath, add, outputLocation, apply, force, globally) {
   tmTheme <- .rs.parseTmTheme(themePath)
   name <- tmTheme$name
   fileName <- paste0(tools::file_path_sans_ext(basename(themePath)), ".rstheme")
   
   aceTheme <- .rs.convertTmTheme(tmTheme)
   rsTheme <- .rs.convertAceTheme(name, aceTheme$theme, aceTheme$isDark)
   
   isTemp <- is.null(outputLocation)
   location <- if (is.null(outputLocation)) tempfile(pattern = fileName)
   else file.path(outputLocation, fileName)
   
   if (!file.create(location))
   {
      stop(
         "Unable to create the theme file in the requested location: ",
         location,
         ". Please see above for relevant warnings.",
         call. = FALSE)
   }
      
   cat(rsTheme, file = location)

   if (add)
   {
      .rs.addTheme(location, apply, force, globally)
   }
   else if (apply)
   {
      stop("Invalid input: unable to apply a theme which has not been added.", call. = FALSE)
   }
   
   name
})

# Adds a custom rstheme to RStudio.
#
# @param themePath   The full or relative path of the rstheme file to add.
# @param apply       Whether to immediately apply the newly added theme.
# @param force       Whether to force the operation when it may involve an overwrite.
# @param globally    Whether to add the theme for all users (true) or for the current user (false).
#
# Returns the name of the theme on success.
.rs.addFunction("addTheme", addTheme <- function(themePath, apply, force, globally) {
   # Get the name of the file without extension.
   fileName <- regmatches(
      basename(themePath),
      regexec(
         "^([^\\.]*)(?:\\.[^\\.]*)?",
         basename(themePath),
         perl = TRUE))[[1]][2]
   
   # Get the name of the theme either from the first occurence of "rs-theme-name:" in the css or 
   # the name of the file.
   conn <- file(themePath)
   themeLines <- readLines(conn)
   close(conn)
   
   nameRegex <- "rs-theme-name\\s*:\\s*([^\\*]+?)\\s*(?:\\*|$)"
   nameLine <- themeLines[grep(nameRegex, themeLines, perl = TRUE)]
   name <- sub(
      "^\\s*(.+?)\\s*$",
      "\\1",
      regmatches(
         nameLine,
         regexec(
            nameRegex,
            nameLine,
            perl = TRUE))[[1]][2],
      perl = TRUE)
   
   # If there's no name in the file, use the name of the file.
   if (is.na(name) || (name == "")) name <- fileName
   
   if (is.na(name) || (name == ""))
   {
      stop(
         "Unable to find a name for the new theme. Please check that the file \"",
         themePath,
         "\" is valid.",
         call. = FALSE)
   }
   
   outputDir <- .rs.getThemeInstallDir(globally)
   if (!dir.exists(outputDir))
   {
      if (globally)
      {
         stop(
            "Unable to add the theme file. The global installation directory does not exist: \"",
            outputDir,
            "\".",
            call. = FALSE)
      }
      if (!dir.create(outputDir, recursive = TRUE))
      {
         stop(
            "Unable to add the theme file. Please check file system permissions.",
            call. = FALSE)
      }
   }

   addedTheme <- file.path(outputDir, paste0(fileName, ".rstheme"))
   if (file.exists(addedTheme) && !force)
   {
      stop(
         "Unable to add the theme. A file with the same name, \"",
         fileName,
         ".rstheme\", already exists in the target location. To add the theme anyway, try again with `force = TRUE`.",
         call. = FALSE)
   }

   if (!file.copy(
      themePath,
      addedTheme,
      overwrite = force))
   {
      msg <- "Unable to add the theme file. Please check file system permissions"
      if (!force)
      {
         msg <- paste0(msg, " or try again with `force = TRUE`.")
      }
      
      stop(msg, ".", call. = FALSE)
   }
   
   if (apply)
   {
      .rs.applyTheme(name, .Call("rs_getThemes"))
   }
   
   name
})

# Applies a theme to RStudio.
#
# @param name     The name of the theme to apply.
.rs.addFunction("applyTheme", applyTheme <- function(name, themeList) {
   theme <- themeList[[tolower(name)]]
   if (is.null(theme))
   {
      stop("The specified theme \"", name, "\" does not exist.")
   }
   
   themeValue <- list(
      "name"= .rs.scalar(theme$name),
      "isDark" = .rs.scalar(theme$isDark),
      "url" = .rs.scalar(theme$url))
   .rs.writeUiPref("rstheme", themeValue);
})

# Removes a theme from RStudio. If the removed theme is the current theme, the current theme will be
# set to the default theme.
#
# @param The name of the theme to remove.
.rs.addFunction("removeTheme", removeTheme <- function(name, themeList) {
   currentTheme <- .rs.api.getThemeInfo()$editor
   
   if (is.null(themeList[[tolower(name)]]))
   {
      stop("The specified theme \"", name, "\" does not exist.")
   }
   
   if (identical(tolower(name), tolower(currentTheme)))
   {
      .rs.applyTheme("TextMate")
   }

   filePath <- .rs.getThemeDirFromUrl(themeList[[tolower(name)]]$url)
   if (is.null(filePath))
   {
      stop(
         "Unable to remove the specified theme: ",
         name,
         ". Please verify that the theme is installed as a custom theme.")
   }
   if (!file.remove(filePath))
   {
      if (file.exists(filePath))
      {
         justFileName <- basename(filePath)
         actualPath <- normalizePath(filePath, mustWork = FALSE, winslash = "/")
         globalPath <- normalizePath(.rs.getThemeInstallDir(TRUE), mustWork = FALSE, winslash = "/")
         if (identical(file.path(globalPath, justFileName), actualPath))
         {
            stop(
               "Unable to remove the specified theme \"",
               name,
               "\", which is installed for all users. Please contact your system administrator.")
         }
         else
         {
            stop(
               "Unable to remove the specified theme \"",
               name,
               "\". Please check your file system permissions.")
         }
      }
   }
})


# API Functions

# Convert a tmtheme to rstheme and optionally add it to RStudio.
.rs.addApiFunction("convertTheme", api.convertTheme <- function(themePath, add = TRUE, outputLocation = NULL, apply = FALSE, force = FALSE, globally = FALSE) {
   # Require XML package for parsing the tmtheme files.
   if (!suppressWarnings(require("xml2", quietly = TRUE)))
   {
      stop("Taking this action requires the xml2 library. Please run 'install.packages(\"xml2\")' before continuing.")
   }
   
   .rs.convertTheme(themePath, add, outputLocation, apply, force, globally)
})

# Add a theme to RStudio.
.rs.addApiFunction("addTheme", api.addTheme <- function(themePath, apply = FALSE, force = FALSE, globally = FALSE) {
   .rs.addTheme(themePath, apply, force, globally)
})

# Apply a theme to RStudio.
.rs.addApiFunction("applyTheme", api.applyTheme <-  function(name) {
   .rs.applyTheme(name, .Call("rs_getThemes"))
})

# Remove a theme from RStudio.()
.rs.addApiFunction("removeTheme", api.removeTheme <- function(name) {
   .rs.removeTheme(name, .Call("rs_getThemes"))
})
