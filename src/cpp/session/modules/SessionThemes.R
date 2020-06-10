#
# SessionThemes.R
#
# Copyright (C) 2020 by RStudio, PBC
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

# Determines the luma of a color. This refers to the perceived luminance of the color. More 
# information can be found at https://en.wikipedia.org/wiki/Relative_luminance.
# 
# @param color    The color for which to determine the luma.
#
# Returns the luma of the specified color.
.rs.addFunction("getLuma", function(color) {
   rgb <- .rs.getRgbColor(color)
   (0.21 * rgb[[1]] + 0.72 * rgb[[2]] + 0.07 * rgb[[3]]) / 255
})

# Parses a color.
# 
# @param color    The color to parse.
#
# Returns the parsed color.
.rs.addFunction("parseColor", function(color) {
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
.rs.addFunction("parseStyles", function(styles) {
   css <- list()
   fontStyle <- if (is.null(styles$fontStyle)) "" else styles$fontStyle
   
   if (grepl("underline", fontStyle))
   {
      # Not the most efficient, but this shouldn't be an overly common operation.
      css[["text-decoration"]] = "underline"
   }
   if (grepl("italic", fontStyle))
   {
      css[["font-style"]] <- "italic"
   }
   if (grepl("bold", fontStyle))
   {
      css[["font-weight"]] <- "bold"
   }
   
   if (!is.null(styles$foreground))
   {
      css[["color"]] <- .rs.parseColor(styles$foreground)
   }
   if (!is.null(styles$background))
   {
      css[["background-color"]] <- .rs.parseColor(styles$background)
   }
   
   css
})

# Extracts the style information from a parsed tmTheme object.
#
# @param theme             The parsed tmTheme object.
# @param supportScopes     tmTheme scopes that are supported by ace and a mapping to their ace
#                          value.
# 
# Returns a list which contains the styles and unsupportedScopes in named elements.
.rs.addFunction("extractStyles", function(theme, supportedScopes) {
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
      "invisible" = invisColor)
   
   if (!("background" %in% names(styles))) styles$background <- defaultGlobals$background
   if (!("foreground" %in% names(styles))) styles$foreground <- defaultGlobals$foreground
   if (!("selection" %in% names(styles))) styles$selection <- defaultGlobals$selection
   if (!("active_line" %in% names(styles))) styles$active_line <- defaultGlobals$active_line
   if (!("cursor" %in% names(styles))) styles$cursor <- defaultGlobals$cursor
   
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
            haveStyle <- !is.null(style) && !is.na(style) && (length(style) > 0)
            if ((scope %in% supportedScopeNames) && haveStyle)
            {
               styles[[ supportedScopes[[scope]] ]] <- style
            }
            else if (haveStyle)
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
   
   if (!("fold" %in% names(styles)))
   {
      if (("entity.name.function" %in% names(styles)) 
          && ("color" %in% names(styles[["entity.name.function"]]))
          && (styles[["entity.name.function"]][["color"]] != ""))
      {
         styles[["fold"]] <- styles[["entity.name.function"]][["color"]]
      }
      else if (("keyword" %in% names(styles))
               && ("color" %in% names(styles[["keyword"]]))
               && (styles[["keyword"]][["color"]] != ""))
      {
         styles[["fold"]] <- styles[["keyword"]][["color"]]
      }
      else
      {
         styles[["fold"]] <- defaultGlobals$fold
      }
   }
   
   styles[["gutterBg"]] <- styles$background
   styles[["gutterFg"]] <- .rs.mixColors(styles$foreground, styles$background, 0.5)
   styles[["selected_word_highlight"]] <- paste0("border: 1px solid ", styles$selection)
   
   styles$isDark = tolower(as.character(.rs.getLuma(styles$background) <  0.5))
   
   fScopeNames <- names(fallbackScopes)
   for (i in 1:length(fallbackScopes))
   {
      name <- fScopeNames[i]
      scope <- fallbackScopes[[i]]
      if (!(name %in% names(styles)) || !("color" %in% names(styles[[name]])))
      {
         if (!(scope %in% names(styles)) || !("color" %in% names(styles[[scope]])))
         {
            # All fallback elements are foreground for now.
            if (!(name %in% names(styles))) styles[[name]] <- list()
            styles[[name]][["color"]] <- styles$foreground
         }
         else if (name %in% names(styles))
         {
            styles[[name]][["color"]] <- styles[[scope]][["color"]]
         }
         else
         {
            styles[[name]] <- styles[[scope]]
         }
      }
   }
   
   list("styles" = styles, "unsupportedScopes" = unsupportedScopes)
})

# Converts a theme from a tmTheme to an Ace css theme.
#
# @param tmTheme     The parsed tmTheme object.
# 
# Returns Ace css theme as a string.
.rs.addFunction("convertTmTheme", function(tmTheme) {
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
   
   # RStudio Supported Scopes
   supportedScopes[["marker-layer.active_debug_line"]] <- "marker-layer .active_debug_line"
   # Read the template files
   conn <- file(
      description = file.path(.Call("rs_rResourcesPath", PACKAGE = "(embedding)"), "templates", "ace_theme_template.css"),
      open = "rt")
   on.exit(close(conn), add = TRUE)
   cssTemplate <- paste0(readLines(conn, encoding = "UTF-8"), collapse ="\n")
   
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
      if (scope %in% names(styles))
      {
         if (grepl("active_debug_line", scope, fixed = TRUE))
         {
            css = paste0(
               css,
               "\n\n.",
               styles$cssClass,
               " ",
               gsub("^|\\.", ".ace_", scope),
               " {\n")
            for (rule in names(styles[[scope]]))
            {
               if (!grepl("^\\s*$", styles[[scope]][[rule]], perl = TRUE))
               {
                  css = paste0(css, "  ", rule, ": ", styles[[scope]][[rule]], ";\n")
               }
            }
            
            css = paste0(
               css,
               "\n  position: absolute;",
               "\n  z-index: -1;",
               "\n}")
         }
         else if (length(styles[[scope]]) > 0)
         {
            css = paste0(
               css,
               "\n\n.",
               styles$cssClass,
               " ",
               gsub("^|\\.", ".ace_", scope),
               " {\n")
            for (rule in names(styles[[scope]]))
            {
               if (!grepl("^\\s*$", styles[[scope]][[rule]], perl = TRUE))
               {
                  css = paste0(css, "  ", rule, ": ", styles[[scope]][[rule]], ";\n")
               }
            }
            
            css = paste0(css, "\n}")
         }
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
.rs.addFunction("parseKeyElement", function(element) {
   key <- xml2::xml_text(element)
   if (key == "")
   {
      stop("The value of a \"key\" element may not be empty.", call. = FALSE)
   }
   
   key
})

# Parses a "string" element from a tmtheme document and raises appropriate errors.
#
# @param element     The element to parse.
# @param keyName     The name of the key for this value.
#
# Returns the text value of the element.
.rs.addFunction("parseStringElement", function(element, keyName) {
   value <- xml2::xml_text(element)
   
   # The key can only be null if there was no <key> element immediately preceding the <string>
   # element in the provided xml. If any <key> element was found (even a <key/>), the key name
   # will at least be "".
   if (is.null(keyName))
   {
      stop(
         "Unable to find a key for the \"string\" element with value \"",
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
.rs.addFunction("parseDictElement", function(dictElement, keyName) {
   if (is.null(keyName))
   {
      stop("Unable to find a key for the current \"dict\" element.", call. = FALSE)
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
                     "Unable to find a value for the key \"",
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
                  "Encountered unexpected element as a child of the current \"dict\" element: \"",
                  elName,
                  "\". Expected \"key\", \"string\", \"array\", or \"dict\".",
                  call. = FALSE)
            }
         }
      }
      
      if (!is.null(key))
      {
         stop(
            "Unable to find a value for the key \"",
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
.rs.addFunction("parseArrayElement", function(arrayElement, keyName) {
   if (xml2::xml_length(arrayElement) < 1)
   {
      stop("\"array\" element cannot be empty.", call. = FALSE)
   }
   
   if (is.null(keyName))
   {
      stop("Unable to find a key for array value.", call. = FALSE)
   }
   if (keyName != "settings")
   {
      stop(
         "Incorrect key for array element. Expected: \"settings\"; Actual: \"",
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
               "Expecting \"dict\" element; found \"",
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
.rs.addFunction("parseTmTheme", function(filePath) {
   if (!file.exists(filePath))
   {
      stop(
         "The specified file, \"",
         filePath,
         "\", does not exist.",
         call. = FALSE)
   }
   xmlStr <- paste(readLines(filePath, encoding = "UTF-8", warn = FALSE), collapse = "\n")
   
   tmThemeDoc <- xml2::xml_root(xml2::read_xml(
      xmlStr, 
      error = function(msg, code, domain, line, col, level, filename) {
         stop(
            "An error occurred while parsing ",
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
      stop("Expected 1 non-text child of the root, found: ",
           childrenCount,
           call. = FALSE)
   }
   
   # Check the structure at the root is correct before continuing.
   if (xml2::xml_name(xml2::xml_child(tmThemeDoc, 1)) != "dict")
   {
      stop("Expecting \"dict\" element; found \"",
           xml2::xml_name(xml2::xml_child(tmThemeDoc, 1)),
           "\".",
           call. = FALSE)
   }
   if (xml2::xml_length(xml2::xml_child(tmThemeDoc, 1)) < 1)
   {
      stop(
         "\"dict\" element cannot be empty.",
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
.rs.addFunction("convertAceTheme", function(name, aceCss, isDark) {
   rsTheme <- .rs.compile_theme(aceCss, isDark, name = name)
   if (length(rsTheme) == 0)
   {
      stop("Please see above for warnings.",
           .call = FALSE)
   }
   
   c(
      paste0("/* rs-theme-name: ", name, " */"),
      paste0("/* rs-theme-is-dark: ", isDark, " */"),
      rsTheme,
      "\n")
})

# Worker Functions =================================================================================
.rs.addFunction("isGlobalTheme", function(themeUrl)
{
   grepl("^theme/custom/global/.*?\\.rstheme$", themeUrl, ignore.case = TRUE)
})

.rs.addFunction("isLocalTheme", function(themeUrl)
{
   grepl("^theme/custom/local/.*\\.rstheme$", themeUrl, ignore.case = TRUE)
})

.rs.addFunction("isDefaultTheme", function(themeUrl)
{
   grepl("^theme/default/.*\\.rstheme$", themeUrl, ignore.case = TRUE)
})

.rs.addFunction("getThemeName", function(themeLines, fileName)
{
   tmThemeNameRegex <- "<key>name</key>\\s*<string>([^>]*)</string>"
   rsthemeNameRegex <- "rs-theme-name\\s*:\\s*([^\\*]+?)\\s*(?:\\*|$)"
   nameRegex <- tmThemeNameRegex
   nameLine <- themeLines[grep(tmThemeNameRegex, themeLines, perl = TRUE, ignore.case = TRUE)]
   if (length(nameLine) == 0)
   {
      nameRegex <- rsthemeNameRegex
      nameLine <- themeLines[grep(rsthemeNameRegex, themeLines, perl = TRUE, ignore.case = TRUE)]
   }
   if (length(nameLine) == 0)
   {
      regmatches(
         basename(fileName),
         regexec(
            "^([^\\.]*)(?:\\.[^\\.]*)?",
            basename(fileName),
            perl = TRUE))[[1]][2]
   }
   else
   {
      sub(
         "^\\s*(.+?)\\s*$",
         "\\1",
         regmatches(
            nameLine,
            regexec(
               nameRegex,
               nameLine,
               perl = TRUE))[[1]][2],
         perl = TRUE)
   }
})

# Gets the install location.
#
# @param global    Whether to get the global or local install dir.
#
# Returns the install location.
.rs.addFunction("getThemeInstallDir", function(global) 
{
   if (global)
      .Call("rs_getGlobalThemeDir", PACKAGE = "(embedding)")
   else
      .Call("rs_getLocalThemeDir", PACKAGE = "(embedding)")
})

.rs.addFunction("getThemeDirFromUrl", function(url) 
{
   decodedUrl <- URLdecode(url)
   if (.rs.isGlobalTheme(decodedUrl))
   {
      file.path(.rs.getThemeInstallDir(TRUE), basename(decodedUrl))
   }
   else if (.rs.isLocalTheme(decodedUrl))
   {
      .Call("rs_getLocalThemePath", basename(decodedUrl), PACKAGE="(embedding)")
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
.rs.addFunction("convertTheme", function(themePath, add, outputLocation, apply, force, globally) {
   tmTheme <- .rs.parseTmTheme(themePath)
   name <- tmTheme$name
   fileName <- paste0(tools::file_path_sans_ext(basename(themePath)), ".rstheme")
   
   
   aceTheme <- .rs.convertTmTheme(tmTheme)
   rsTheme <- .rs.convertAceTheme(name, aceTheme$theme, aceTheme$isDark)
   
   
   isTemp <- is.null(outputLocation)
   location <- if (is.null(outputLocation)) file.path(tempdir(), fileName)
   else file.path(outputLocation, fileName)
   
   if (!file.create(location))
   {
      stop(
         "Unable to create the theme file in the requested location: ",
         location,
         ". Please see above for relevant warnings.",
         call. = FALSE)
   }
   
   cat(paste(rsTheme, collapse="\n"), file = location)
   
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
.rs.addFunction("addTheme", function(themePath, apply, force, globally) {
   # Get the name of the file without extension.
   fileName <- basename(themePath)
   
   # Get the name of the theme either from the first occurence of "rs-theme-name:" in the css or 
   # the name of the file.
   themeLines <- readLines(themePath, encoding = "UTF-8", warn = FALSE)
   name <- .rs.getThemeName(paste0(themeLines, collapse = "\n"), fileName)
   
   if (is.na(name) || (name == "") || is.null(name))
   {
      stop(
         "Unable to find a name for the new theme. Please check that the file \"",
         themePath,
         "\" is valid.",
         call. = FALSE)
   }
   
   if (length(themeLines) == 0)
   {
      stop("The theme file is empty.", call. = FALSE);
   }
      
   # Check if a theme with the same name already exists in the current location.
   dupTheme <- .rs.getThemes()[[tolower(name)]]
   if (!is.null(dupTheme) && 
       ((globally && .rs.isGlobalTheme(dupTheme$url)) ||
        (!globally && .rs.isLocalTheme(dupTheme$url))) &&
       !force)
   {
      stop(
         "The specified theme, \"",
         name,
         "\", already exists in the target location. Please delete the existing theme and try again.",
         call. = FALSE)
   }
   else if (!is.null(dupTheme))
   {
      willBeOverridden <- if (!globally || .rs.isDefaultTheme(dupTheme$url)) "The existing theme will be overridden by the new theme."
      else "The newly added theme will be overridden by the existing theme."
      warning("There is another theme with the same name, \"",
              name,
              "\". ",
              willBeOverridden,
              call. = FALSE)
   }
   
   outputDir <- .rs.getThemeInstallDir(globally)
   if (!dir.exists(outputDir))
   {
      if (globally)
      {
         stop(
            "The global installation directory does not exist: \"",
            outputDir,
            "\".",
            call. = FALSE)
      }
      if (!dir.create(outputDir, recursive = TRUE))
      {
         stop(
            "Please check file system permissions.",
            call. = FALSE)
      }
   }
   
   addedTheme <- file.path(outputDir, fileName)
   if (file.exists(addedTheme) && !force)
   {
      stop(
         "A file with the same name, \"",
         fileName,
         "\", already exists in the target location. To add the theme anyway, try again with `force = TRUE`.",
         call. = FALSE)
   }
   
   if (!file.copy(
      themePath,
      addedTheme,
      overwrite = force))
   {
      msg <- "Please check file system permissions"
      if (!force)
      {
         msg <- paste0(msg, " or try again with `force = TRUE`.")
      }
      
      stop(msg, ".", call. = FALSE)
   }
   
   if (apply)
   {
      .rs.applyTheme(name, .rs.getThemes())
   }
   
   name
})

# Applies a theme to RStudio.
#
# @param name     The name of the theme to apply.
.rs.addFunction("applyTheme", function(name, themeList) {
   theme <- themeList[[tolower(name)]]
   if (is.null(theme))
   {
      stop("The specified theme \"", name, "\" does not exist.", call. = FALSE)
   }
   
   themeValue <- list(
      "name"= .rs.scalar(theme$name),
      "isDark" = .rs.scalar(theme$isDark),
      "url" = .rs.scalar(theme$url))

   # Save theme details to user state
   .rs.writeUserState("theme", themeValue)

   # Save theme itself as a user pref
   .rs.writeUserPref("editor_theme", name)
})

# Removes a theme from RStudio. If the removed theme is the current theme, the current theme will be
# set to the default theme.
#
# @param The name of the theme to remove.
.rs.addFunction("removeTheme", function(name, themeList) {
   currentTheme <- .rs.api.getThemeInfo()$editor
   lowerCaseName <- tolower(name)
   
   if (is.null(themeList[[lowerCaseName]]))
   {
      stop("The specified theme \"", name, "\" does not exist.")
   }
   
   if (identical(lowerCaseName, tolower(currentTheme)))
   {
      nextTheme <- if (themeList[[lowerCaseName]]$isDark) "Tomorrow Night" else "TextMate (default)"
      warning("Removing the active theme - setting the current theme to ", nextTheme)
      .rs.applyTheme(nextTheme, themeList)
   }

   filePath <- .rs.getThemeDirFromUrl(themeList[[lowerCaseName]]$url)
   if (is.null(filePath))
   {
      stop("Please verify that the theme is installed as a custom theme.")
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
            stop("The specified theme is installed for all users. Please contact your system administrator to remove the theme.")
         }
         else
         {
            stop("Please check your file system permissions.")
         }
      }
   }
})

.rs.addFunction("getThemes", function() {
   themeList <- .Call("rs_getThemes", PACKAGE = "(embedding)")
   for (i in seq_along(themeList))
   {
      Encoding(themeList[[i]]$name) <- "UTF-8"
      Encoding(themeList[[i]]$url) <- "UTF-8"
   }
   themeList
})

# C++ Wrappers =====================================================================================
.rs.addFunction("internal.convertTheme", function(themePath) {
   Encoding(themePath) <- "UTF-8"
   
   warnings <- c()
   tryCatch(
      withCallingHandlers(
         .rs.convertTheme(
            themePath,
            add = TRUE,
            outputLocation = NULL,
            apply = FALSE,
            force = TRUE,
            globally = FALSE),
         warning = function(w) 
         { 
            warnings <<- conditionMessage(w)
            invokeRestart("muffleWarning")
         }),
      error = function(e) 
      { 
         if (length(warnings) > 0)
            e$message <- paste(
               e$message,
               paste0(warnings, collapse = "\n    "),
               sep = "\nAlso, warnings:\n    ")
         e 
      })
})

.rs.addFunction("internal.addTheme", function(themePath) {
   Encoding(themePath) <- "UTF-8"
   
   warnings <- c()
   tryCatch(
      withCallingHandlers(
         .rs.addTheme(themePath, apply = FALSE, force = FALSE, globally = FALSE),
         warning = function(w) 
         { 
            warnings <<- conditionMessage(w)
            invokeRestart("muffleWarning")
         }),
      error = function(e) 
      {
         if (length(warnings) > 0)
            e$message <- paste(
               e$message,
               paste0(warnings, collapse = "\n    "),
               sep = "\nAlso, warnings:\n    ")
         e 
      })
})

.rs.addFunction("internal.removeTheme", function(name, themeList) {
   Encoding(name) <- "UTF-8"
   
   warnings <- c()
   tryCatch(
      withCallingHandlers(
         .rs.removeTheme(name, themeList),
         warning = function(w) 
         { 
            warnings <<- c(warnings, conditionMessage(w))
            invokeRestart("muffleWarning")
         }),
      error = function(e) 
      { 
         if (length(warnings) > 0)
            e$message <- paste(
               e$message,
               paste0(warnings, collapse = "\n    "),
               sep = "\nAlso, warnings:\n    ")
         e 
      })
})

# API Functions ====================================================================================

# Convert a tmtheme to rstheme and optionally add it to RStudio.
.rs.addApiFunction("convertTheme", function(themePath, add = TRUE, outputLocation = NULL, apply = FALSE, force = FALSE, globally = FALSE) {
   # Require XML package for parsing the tmtheme files.
   missingLibraryMsg <- "Taking this action requires the %pkg% library. Please run 'install.packages(\"%pkg%\")' before continuing."
   if (!suppressWarnings(require("xml2", quietly = TRUE)))
   {
      stop(gsub("%pkg%", "xml2", missingLibraryMsg, fixed = TRUE))
   }
   
   tryCatch(
      .rs.convertTheme(themePath, add, outputLocation, apply, force, globally),
      error = function(e) { stop("Unable to convert the tmTheme to an rstheme. ", e$message) })
})

# Add a theme to RStudio.
.rs.addApiFunction("addTheme", function(themePath, apply = FALSE, force = FALSE, globally = FALSE) {
   tryCatch(
      .rs.addTheme(themePath, apply, force, globally),
      error = function(e) 
      {
         stop("Unable to add the theme file \"", themePath, "\". ", e$message)
      })
})

# Apply a theme to RStudio.
.rs.addApiFunction("applyTheme", function(name) {
   tryCatch(
      .rs.applyTheme(name, .rs.getThemes()),
      error = function(e) { stop("Unable to apply the theme \"", name, "\". ", e$message) })
})

# Remove a theme from RStudio.
.rs.addApiFunction("removeTheme", function(name) {
   tryCatch(
      .rs.removeTheme(name, .rs.getThemes()),
      error = function(e) { stop("Unable to remove the theme \"", name, "\". ", e$message) })
})

# Get the list of installed themes.
.rs.addApiFunction("getThemes", function() {
   lapply(.rs.getThemes(), function(theme) {
      theme[names(theme) != "url"]
   })
})

# RPC Functions ====================================================================================
.rs.addJsonRpcHandler("get_theme_name", function(themeFile) {
   Encoding(themeFile) <- "UTF-8"
   lines <- readLines(themeFile, encoding = "UTF-8")
   .rs.scalar(.rs.getThemeName(paste0(lines, collapse = "\n"), themeFile))
})
