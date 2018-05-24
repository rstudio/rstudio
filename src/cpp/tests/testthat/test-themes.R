#
# test-themes.R
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
#

context("themes")

inputFileLocation <- file.path(path.expand("."), "themes")

# Helpers ==========================================================================================
parseCss <- function(cssLines)
{
   css <- list()
   
   currKey = NULL
   for (line in cssLines)
   {
      orgLine <- line
      if (!is.null(currKey))
      {
         isLastDescForKey <- FALSE
         if (grepl("\\}", line))
         {
            isLastDescForKey <- TRUE
            line <- sub("^([^\\}]*)\\}\\s*$", "\\1", line)
            if (grepl("\\}", line))
            {
               warning(sprintf("Maformed CSS: %s", orgLine))
            }
         }
         
         if (grepl(":", line))
         {
            descValues <- strsplit(line, "\\s*;\\s*")[[1]]
            for (value in descValues)
            {
               if (value != "")
               {
                  desc <- strsplit(sub("^\\s*([^;]+);?\\s*$", "\\1", line), "\\s*:\\s*")[[1]]
                  if (length(desc) != 2)
                  {
                     warning(sprintf("Malformed CSS: %s", orgLine))
                  }
                  else
                  {
                     css[[currKey]][[ desc[1] ]] <- tolower(desc[2])
                  }
               }
            }
         }
         else if (grepl("^\\s*$", orgLine))
         {
            warning(sprintf("Malformd CSS: %s", orgLine))
         }
         
         if (isLastDescForKey)
         {
            currKey <- NULL
         }
      }
      else if (grepl("\\.[^\\{]+\\{$", line))
      {
         currKey = regmatches(line, regexec("^\\s*([^\\{]+?)\\s*\\{\\s*$", line))[[1]][2]
         css[[currKey]] <- list()
      }
   }
   
   css
}

compareCss <- function(actual, expected, parent = NULL)
{
   equal <- TRUE
   msgStart <- "\nCSS"
   if (!is.null(parent)) msgStart <- paste0("\nElement \"", parent, "\" ")
   
   if (!all(actual %in% expected) || !all(expected %in% actual))
   {
      # Check length
      acLen <- length(actual)
      exLen <- length(expected)
      msg <- sprintf("elements than exepected. Actual: %d, Expected: %d", acLen, exLen)
      if (acLen < exLen)
      {
         cat(msgStart, "has fewer", msg, "\n")
         equal <- FALSE
      }
      else if (acLen > exLen)
      {
         cat(msgStart, "has more", msg, "\n")
         equal <- FALSE
      }
      
      acNames <- names(actual)
      exNames <- names(expected)
      if (!all(acNames %in% exNames) || !all(exNames %in% acNames))
      {
         equal <- FALSE
         extraNames <- c()
         missingNames <- c()
         # Check that all the names are included
         for (name in acNames)
         {
            if (!(name %in% exNames))
            {
               extraNames <- c(extraNames, name)
            }
         }
         for (name in exNames)
         {
            if(!(name %in% acNames))
            {
               missingNames <- c(missingNames, name)
            }
         }
         
         extraMsg <- sprintf(
            "had %d unexepected elements with names: \n   \"%s\"",
            length(extraNames),
            paste0(extraNames, collapse = "\",\n   \""))
         missingMsg <- sprintf(
            "was missing %d elements with names: \"%s\"",
            length(missingNames),
            paste0(missingNames, collapse = "\",\n   \""))

         if (length(extraNames) > 0)
         {
            cat(msgStart, extraMsg, "\n")
         }
         if (length(missingNames) > 0)
         {
            cat(msgStart, missingMsg, "\n")
         }
      }
      
      # Handle the CSS contents
      for (name in acNames)
      {
         if (name %in% exNames)
         {
            acVal <- actual[[name]]
            exVal <- expected[[name]]
            if (is.list(acVal) && is.list(exVal))
            {
               equal <- equal && compareCss(acVal, exVal, name)
            }
            else if (is.list(acVal) || is.list(exVal))
            {
               msg <- sprintf("value type (%s) does not match expected (%s)", typeof(acVal), typeof(exVal))
               cat(msgStart, msg, "\n")
               equal <- FALSE
            }
            else if (acVal != exVal)
            {
               match <- "^(#[a-fA-F\\d]{6}|rgb\\(\\s*[0-9]{1-3}\\s*,\\s*[0-9]{1-3}\\s*,\\s*[0-9]{1-3}\\s*\\))$"
               if (grepl(match, acVal) && grepl(match, exVal))
               {
                  acRgb <- .rs.getRgbColor(acVal)
                  exRgb <- .rs.getRgbColor(exVal)
                  if (!all.equal(acRgb, exRgb))
                  {
                     msg <- sprintf("value doesn't match. Actual: %s, Expected: %s", acVal, exVal)
                     cat(msgStart, msg, "\n")
                     equal <- FALSE
                  }
               }
            }
         }
      }
   }
   
   equal
}

# Test getRgbColor =================================================================================
test_that("rgb coversion from hex format works", {
   # All lowercase
   expect_equal(.rs.getRgbColor("#ffffff"), c(255, 255, 255))

   # All digits
   expect_equal(.rs.getRgbColor("#000000"), c(0, 0, 0))

   # Uppercase & digits
   expect_equal(.rs.getRgbColor("#DD8EBD"), c(221, 142, 189))

   # Mix case & digits
   expect_equal(.rs.getRgbColor("#569cD6"), c(86, 156, 214))
})

test_that("rgb conversion from rgb string format works", {
   expect_equal(.rs.getRgbColor("rgb(86, 156, 214)"), c(86, 156, 214))
   expect_equal(.rs.getRgbColor("rgb(0, 0, 0)"), c(0, 0, 0))
   expect_equal(.rs.getRgbColor("rgb(255,255,255)"), c(255, 255, 255))
})

test_that("rgb conversion from rgba string format works", {
   # 'a' value shouldn't impact outcome.
   expect_equal(.rs.getRgbColor("rgba(86, 156, 214, 10)"), c(86, 156, 214))
   expect_equal(.rs.getRgbColor("rgba(86,156,214,90)"), c(86, 156, 214))

   expect_equal(.rs.getRgbColor("rgba(0, 0, 0, 55)"), c(0, 0, 0))
   expect_equal(.rs.getRgbColor("rgba(255, 255, 255, 72)"), c(255, 255, 255))
})

test_that("rgb conversion handles out of bounds values correctly", {
   # Negatvie RGB value.
   expect_error(
      .rs.getRgbColor("rgb(-10, 156, 214)"),
      "invalid color supplied: rgb(-10, 156, 214). RGB value cannot be negative",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("rgb(86, -1, 214)"),
      "invalid color supplied: rgb(86, -1, 214). RGB value cannot be negative",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("rgb(86, 156, -214)"),
      "invalid color supplied: rgb(86, 156, -214). RGB value cannot be negative",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("rgb(-86, -156, -214)"),
      "invalid color supplied: rgb(-86, -156, -214). RGB value cannot be negative",
      fixed = TRUE)

   # RGB value > 255
   expect_error(
      .rs.getRgbColor("rgb(300, 156, 214)"),
      "invalid color supplied: rgb(300, 156, 214). RGB value cannot be greater than 255",
      fixed = TRUE
   )
   expect_error(
      .rs.getRgbColor("rgb(86, 455, 214)"),
      "invalid color supplied: rgb(86, 455, 214). RGB value cannot be greater than 255",
      fixed = TRUE
   )
   expect_error(
      .rs.getRgbColor("rgb(86, 156, 1024)"),
      "invalid color supplied: rgb(86, 156, 1024). RGB value cannot be greater than 255",
      fixed = TRUE
   )
   expect_error(
      .rs.getRgbColor("rgb(300, 455, 1024)"),
      "invalid color supplied: rgb(300, 455, 1024). RGB value cannot be greater than 255",
      fixed = TRUE
   )

   # Negative & too large values
   expect_error(
      .rs.getRgbColor("rgb(-300, 455, 1024)"),
      "invalid color supplied: rgb(-300, 455, 1024). RGB value cannot be negative",
      fixed = TRUE
   )
   expect_error(
      .rs.getRgbColor("rgb(300, -455, 1024)"),
      "invalid color supplied: rgb(300, -455, 1024). RGB value cannot be greater than 255",
      fixed = TRUE
   )

   # Negatvie RGBA value.
   expect_error(
      .rs.getRgbColor("rgba(-10, 156, 214, 92)"),
      "invalid color supplied: rgba(-10, 156, 214, 92). RGB value cannot be negative",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("rgba(86, -1, 214, 14)"),
      "invalid color supplied: rgba(86, -1, 214, 14). RGB value cannot be negative",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("rgba(86, 156, -214, 100)"),
      "invalid color supplied: rgba(86, 156, -214, 100). RGB value cannot be negative",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("rgba(-86, -156, -214, 39)"),
      "invalid color supplied: rgba(-86, -156, -214, 39). RGB value cannot be negative",
      fixed = TRUE)

   # RGB value > 255
   expect_error(
      .rs.getRgbColor("rgba(300, 156, 214, 100)"),
      "invalid color supplied: rgba(300, 156, 214, 100). RGB value cannot be greater than 255",
      fixed = TRUE
   )
   expect_error(
      .rs.getRgbColor("rgba(86, 455, 214, 0)"),
      "invalid color supplied: rgba(86, 455, 214, 0). RGB value cannot be greater than 255",
      fixed = TRUE
   )
   expect_error(
      .rs.getRgbColor("rgba(86, 156, 1024, 40)"),
      "invalid color supplied: rgba(86, 156, 1024, 40). RGB value cannot be greater than 255",
      fixed = TRUE
   )
   expect_error(
      .rs.getRgbColor("rgba(300, 455, 1024, 60)"),
      "invalid color supplied: rgba(300, 455, 1024, 60). RGB value cannot be greater than 255",
      fixed = TRUE
   )

   # Negative & too large values
   expect_error(
      .rs.getRgbColor("rgba(-300, 455, 1024, 55)"),
      "invalid color supplied: rgba(-300, 455, 1024, 55). RGB value cannot be negative",
      fixed = TRUE
   )
   expect_error(
      .rs.getRgbColor("rgba(300, -455, 1024, 55)"),
      "invalid color supplied: rgba(300, -455, 1024, 55). RGB value cannot be greater than 255",
      fixed = TRUE
   )

   # Non-integer RGB values
   expect_error(
      .rs.getRgbColor("rgb(30,10f,5)"),
      "invalid color supplied: rgb(30,10f,5). One or more RGB values could not be converted to an integer",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("rgb(2.1,10,5)"),
      "invalid color supplied: rgb(2.1,10,5). One or more RGB values could not be converted to an integer",
      fixed = TRUE)

   # Non-integer RGBA values
   expect_error(
      .rs.getRgbColor("rgba(30,10f,5,67)"),
      "invalid color supplied: rgba(30,10f,5,67). One or more RGB values could not be converted to an integer",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("rgba(2.1,10,5,45)"),
      "invalid color supplied: rgba(2.1,10,5,45). One or more RGB values could not be converted to an integer",
      fixed = TRUE)

   # Non-hex hex represntation values
   expect_error(
      .rs.getRgbColor("#afga01"),
      "invalid color supplied: #afga01. One or more RGB values could not be converted to an integer",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("#af1a6T"),
      "invalid color supplied: #af1a6T. One or more RGB values could not be converted to an integer",
      fixed = TRUE)
})

test_that("rgb conversion handles invalid values correctly", {
   # Too many characters in hex representation
   expect_error(
      .rs.getRgbColor("#123456ab"),
      "hex represntation of RGB values should have format \"#[0-9a-fA-F]{6}\". Found: #123456ab",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("#FfFfFf0"),
      "hex represntation of RGB values should have format \"#[0-9a-fA-F]{6}\". Found: #FfFfFf0",
      fixed = TRUE)

   # Too few values in rgb/rgba representation
   expect_error(
      .rs.getRgbColor("rgb(1, 10)"), 
      "expected RGB color with format \"rgba?\\([0-9]+, [0-9]+, [0-9]+(, [0-9]+)\\)\". Found: rgb(1, 10)",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("rgba(1, 10)"), 
      "expected RGB color with format \"rgba?\\([0-9]+, [0-9]+, [0-9]+(, [0-9]+)\\)\". Found: rgba(1, 10)",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("rgb(255)"), 
      "expected RGB color with format \"rgba?\\([0-9]+, [0-9]+, [0-9]+(, [0-9]+)\\)\". Found: rgb(255)",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("rgba(255)"), 
      "expected RGB color with format \"rgba?\\([0-9]+, [0-9]+, [0-9]+(, [0-9]+)\\)\". Found: rgba(255)",
      fixed = TRUE)

   # Completely wrong format
   expect_error(
      .rs.getRgbColor("86, 154, 214"),
      "supplied color has an invalid format: 86, 154, 214. Expected \"#[0-9a-fA-F]{6}\" or \"rgba?\\([0-9]+, [0-9]+, [0-9]+(, [0-9]+)\\)\"",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("Not a color"),
      "supplied color has an invalid format: Not a color. Expected \"#[0-9a-fA-F]{6}\" or \"rgba?\\([0-9]+, [0-9]+, [0-9]+(, [0-9]+)\\)\"",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("0xaaaaaa"),
      "supplied color has an invalid format: 0xaaaaaa. Expected \"#[0-9a-fA-F]{6}\" or \"rgba?\\([0-9]+, [0-9]+, [0-9]+(, [0-9]+)\\)\"",
      fixed = TRUE)
})

# Test mixColors ===================================================================================
test_that("mixColors works correctly", {
   expect_equal(.rs.mixColors("#aa11bb", "rgb(21, 140, 231)", 0.6, 0.2), "rgb(106,38,158)")
   expect_equal(.rs.mixColors("rgb(0, 0, 0)", "rgb(255, 255, 255)", 0.5), "rgb(128,128,128)")
   expect_equal(.rs.mixColors("rgb(0, 0, 0)", "rgb(255, 255, 255)", 0.51), "rgb(125,125,125)")
   expect_equal(.rs.mixColors("rgb(10,10,10)", "rgb(30,30,30)", 1, 1), "rgb(40,40,40)")
   expect_equal(.rs.mixColors("rgb(10,10,10)", "rgb(28,230,57)", 1), "rgb(10,10,10)")
})

# Test getLuma =====================================================================================
test_that("getLuma works correctly", {
   expect_equal(.rs.getLuma("rgb(0,0,0)"), 0)
   expect_equal(.rs.getLuma("#FFFFFF"), 1)
   expect_equal(.rs.getLuma("rgb(128, 128, 128)"), 0.5019608, tolerance = 5e-7)
   expect_equal(.rs.getLuma("#569cD6"), 0.5700392, tolerance = 5e-7)
})

# Test parseColor ==================================================================================
test_that("parseColor works correctly", {
   # Non-error cases
   expect_equal(.rs.parseColor("#aBc"), "#aaBBcc")
   expect_equal(.rs.parseColor("#123"), "#112233")
   expect_equal(.rs.parseColor("#F5d"), "#FF55dd")
   expect_equal(.rs.parseColor("#86D4C2"), "#86D4C2")
   expect_equal(.rs.parseColor("#86D4C2FF"), "rgba(134, 212, 194, 1.00)")
   expect_equal(.rs.parseColor("#1B22A7D6"), "rgba(27, 34, 167, 0.84)")
   expect_null(.rs.parseColor(""))
   
   # Error cases
   expect_error(.rs.parseColor("1a2b"), "Unable to parse color: 11aa22bb", fixed = TRUE)
   expect_error(.rs.parseColor("654"), "Unable to parse color: 654", fixed = TRUE)
   expect_error(
      .rs.parseColor("rgb(10, 223, 186)"), "Unable to parse color: rgb(10, 223, 186)",
      fixed = TRUE)
   expect_error(.rs.parseColor("1234567"), "Unable to parse color: 1234567", fixed = TRUE)
})

# Test parseStyles =================================================================================
test_that("parseStyles works correctly", {
   # Setup objects for the test case
   allValues <- list(
      "foreground" = "#a12d96",
      "fontStyle" = "underline italic",
      "background" = "#1ad269")
   noFont <- list("foreground" = "#863021", "background" = "#A1A1A1")
   onlyItalic <- list("fontStyle" = "italic")
   underLineBg <- list("fontStyle" = "underline", "background" = "#A1A1A1FF")
   nonsense <- list("fontStyle" = "afgaeda", "background" = "afdafag", "foreground" = "asdafwegwr")
   nonsenseFont <- list("fontStyle" = "afsafefaf", "background" = "#123", "foreground" = "#1A2B3C4D")
   nonsenseFont2 <- list(
      "background" = "#1a2b3c",
      "fontStyle" = "asdafafeaunderlineadafweglkj;op",
      "foreground" = "#ccc")
   emptyList <- list()
   
   # Expected Values
   allValuesEx <- "text-decoration:underline;font-style:italic;color:#a12d96;background-color:#1ad269;"
   noFontEx <- "color:#863021;background-color:#A1A1A1;"
   onlyItalicEx <- "font-style:italic;"
   underLineBgEx <- "text-decoration:underline;background-color:rgba(161, 161, 161, 1.00);"
   nonsenseFontEx <- "color:rgba(26, 43, 60, 0.30);background-color:#112233;"
   nonsenseFont2Ex <- "text-decoration:underline;color:#cccccc;background-color:#1a2b3c;"
   emptyListEx <- ""
   
   # Test cases (no error)
   expect_equal(.rs.parseStyles(allValues), allValuesEx)
   expect_equal(.rs.parseStyles(noFont), noFontEx)
   expect_equal(.rs.parseStyles(onlyItalic), onlyItalicEx)
   expect_equal(.rs.parseStyles(underLineBg), underLineBgEx)
   expect_equal(.rs.parseStyles(nonsenseFont), nonsenseFontEx)
   expect_equal(.rs.parseStyles(nonsenseFont2), nonsenseFont2Ex)
   expect_equal(.rs.parseStyles(emptyList), emptyListEx)
   
   # Test cases (error)
   expect_error(.rs.parseStyles(nonsense), "Unable to parse color: asdafwegwr", fixed = TRUE)
})

# Test extractStyles ===============================================================================
test_that("extractStyles works correctly", {
   # Static supported scopes list that extractStyle requires. It's defined in the function that
   # calls extractStyles (.rs.convertTmTheme)
   supportedScopes <- list()
   
   supportedScopes[["keyword"]] <- "keyword"
   supportedScopes[["keyword.operator"]] <- "keyword.operator"
   supportedScopes[["keyword.other.unit"]] <- "keyword.other.unit"
   
   supportedScopes[["constant"]] <- "constant"
   supportedScopes[["constant.language"]] <- "constant.language"
   supportedScopes[["constant.library"]] <- "constant.library"
   supportedScopes[["constant.numeric"]] <- "constant.numeric"
   supportedScopes[["constant.character"]] <- "constant.character"
   supportedScopes[["constant.character.escape"]] <- "constant.character.escape"
   supportedScopes[["constant.character.entity"]] <- "constant.character.entity"
   
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
   
   supportedScopes[["function"]] <- "function"
   supportedScopes[["function.buildin"]] <- "function.buildin"
   
   supportedScopes[["storage"]] <- "storage"
   supportedScopes[["storage.type"]] <- "storage.type"
   
   supportedScopes[["invalid"]] <- "invalid"
   supportedScopes[["invalid.illegal"]] <- "invalid.illegal"
   supportedScopes[["invalid.deprecated"]] <- "invalid.deprecated"
   
   supportedScopes[["string"]] <- "string"
   supportedScopes[["string.regexp"]] <- "string.regexp"
   
   supportedScopes[["comment"]] <- "comment"
   supportedScopes[["comment.documentation"]] <- "comment.doc"
   supportedScopes[["comment.documentation.tag"]] <- "comment.doc.tag"
   
   supportedScopes[["variable"]] <- "variable"
   supportedScopes[["variable.language"]] <- "variable.language"
   supportedScopes[["variable.parameter"]] <- "variable.parameter"
   
   supportedScopes[["meta"]] <- "meta"
   supportedScopes[["meta.tag.sgml.doctype"]] <- "xml-pe"
   supportedScopes[["meta.tag"]] <- "meta.tag"
   supportedScopes[["meta.selector"]] <- "meta.selector"
   
   supportedScopes[["entity.other.attribute-name"]] <- "entity.other.attribute-name"
   supportedScopes[["entity.name.function"]] <- "entity.name.function"
   supportedScopes[["entity.name"]] <- "entity.name"
   supportedScopes[["entity.name.tag"]] <- "entity.name.tag"
   
   supportedScopes[["markup.heading"]] <- "markup.heading"
   supportedScopes[["markup.heading.1"]] <- "markup.heading.1"
   supportedScopes[["markup.heading.2"]] <- "markup.heading.2"
   supportedScopes[["markup.heading.3"]] <- "markup.heading.3"
   supportedScopes[["markup.heading.4"]] <- "markup.heading.4"
   supportedScopes[["markup.heading.5"]] <- "markup.heading.5"
   supportedScopes[["markup.heading.6"]] <- "markup.heading.6"
   supportedScopes[["markup.list"]] <- "markup.list"
   
   supportedScopes[["collab.user1"]] <- "collab.user1"
   
   supportedScopes[["marker-layer.bracket"]] <- "marker-layer.bracket"
   
   # Setup objects for test cases
   tomorrowTmTheme <- list()
   tomorrowTmTheme$comment <- "http://chriskempson.com"
   tomorrowTmTheme$name <- "Tomorrow"
   tomorrowTmTheme$settings <- list()
   
   tomorrowTmTheme$settings[[1]] <- list()
   tomorrowTmTheme$settings[[1]]$settings <- list(
      "background" = "#FFFFFF",
      "caret" ="#AEAFAD",
      "foreground" = "#4D4D4C",
      "invisibles" = "#D1D1D1",
      "lineHighlight" = "#EFEFEF",
      "selection" = "#D6D6D6")
   
   tomorrowTmTheme$settings[[2]] <- list()
   tomorrowTmTheme$settings[[2]]$name <- "Comment"
   tomorrowTmTheme$settings[[2]]$scope <- "comment"
   tomorrowTmTheme$settings[[2]]$settings <- list("foreground" = "#8E908C")
   
   tomorrowTmTheme$settings[[3]] <- list()
   tomorrowTmTheme$settings[[3]]$name <- "Foreground"
   tomorrowTmTheme$settings[[3]]$scope <- "keyword.operator.class, constant.other, source.php.embedded.line"
   tomorrowTmTheme$settings[[3]]$settings <- list()
   tomorrowTmTheme$settings[[3]]$settings$fontStyle <- ""
   tomorrowTmTheme$settings[[3]]$settings$foreground <- "#666969"
   
   tomorrowTmTheme$settings[[4]] <- list()
   tomorrowTmTheme$settings[[4]]$name <- "Variable, String Link, Regular Expression, Tag Name, GitGutter deleted"
   tomorrowTmTheme$settings[[4]]$scope <- "variable, support.other.variable, string.other.link, string.regexp, entity.name.tag, entity.other.attribute-name, meta.tag, declaration.tag, markup.deleted.git_gutter"
   tomorrowTmTheme$settings[[4]]$settings <- list("foreground" = "#C82829")
   
   tomorrowTmTheme$settings[[5]] <- list()
   tomorrowTmTheme$settings[[5]]$name <- "Number, Constant, Function Argument, Tag Attribute, Embedded"
   tomorrowTmTheme$settings[[5]]$scope <- "constant.numeric, constant.language, support.constant, constant.character, variable.parameter, punctuation.section.embedded, keyword.other.unit"
   tomorrowTmTheme$settings[[5]]$settings <- list("fontStyle" = "", "foreground" = "#F5871F")
   
   tomorrowTmTheme$settings[[6]] <- list()
   tomorrowTmTheme$settings[[6]]$name <- "Class, Support"
   tomorrowTmTheme$settings[[6]]$scope <- "entity.name.class, entity.name.type.class, support.type, support.class"
   tomorrowTmTheme$settings[[6]]$settings <- list("fontStyle" = "", "foreground" = "#C99E00")
   
   tomorrowTmTheme$settings[[7]] <- list()
   tomorrowTmTheme$settings[[7]]$name <- "String, Symbols, Inherited Class, Markup Heading, GitGutter inserted"
   tomorrowTmTheme$settings[[7]]$scope <- "string, constant.other.symbol, entity.other.inherited-class, entity.name.filename, markup.heading, markup.inserted.git_gutter"
   tomorrowTmTheme$settings[[7]]$settings <- list("fontStyle" = "", "foreground" = "#718C00")
   
   tomorrowTmTheme$settings[[8]] <- list()
   tomorrowTmTheme$settings[[8]]$name <- "Operator, Misc"
   tomorrowTmTheme$settings[[8]]$scope <- "keyword.operator, constant.other.color"
   tomorrowTmTheme$settings[[8]]$settings <- list("foreground" = "#3E999F")
   
   tomorrowTmTheme$settings[[9]] <- list()
   tomorrowTmTheme$settings[[9]]$name <- "Function, Special Method, Block Level, GitGutter changed"
   tomorrowTmTheme$settings[[9]]$scope <- "entity.name.function, meta.function-call, support.function, keyword.other.special-method, meta.block-level, markup.changed.git_gutter"
   tomorrowTmTheme$settings[[9]]$settings <- list("fontStyle" = "", "foreground" = "#4271AE")
   
   tomorrowTmTheme$settings[[10]] <- list()
   tomorrowTmTheme$settings[[10]]$name <- "Keyword, Storage"
   tomorrowTmTheme$settings[[10]]$scope <- "keyword, storage, storage.type"
   tomorrowTmTheme$settings[[10]]$settings <- list("fontStyle" = "", "foreground" = "#8959A8")
   
   tomorrowTmTheme$settings[[11]] <- list()
   tomorrowTmTheme$settings[[11]]$name <- "Invalid"
   tomorrowTmTheme$settings[[11]]$scope <- "invalid"
   tomorrowTmTheme$settings[[11]]$settings <- list("background" = "#C82829", "fontStyle" = "", "foreground" = "#FFFFFF")
   
   tomorrowTmTheme$settings[[12]] <- list()
   tomorrowTmTheme$settings[[12]]$name <- "Separator"
   tomorrowTmTheme$settings[[12]]$scope <- "meta.separator"
   tomorrowTmTheme$settings[[12]]$settings <- list("background" = "#4271AE", "foreground" = "#FFFFFF")
   
   tomorrowTmTheme$settings[[13]] <- list()
   tomorrowTmTheme$settings[[13]]$name <- "Deprecated"
   tomorrowTmTheme$settings[[13]]$scope <- "invalid.deprecated"
   tomorrowTmTheme$settings[[13]]$settings <- list("background" = "#8959A8", "fontStyle" = "", "foreground" = "#FFFFFF")
   
   tomorrowTmTheme$settings[[14]] <- list()
   tomorrowTmTheme$settings[[14]]$name <- "Diff foreground"
   tomorrowTmTheme$settings[[14]]$scope <- "markup.inserted.diff, markup.deleted.diff, meta.diff.header.to-file, meta.diff.header.from-file"
   tomorrowTmTheme$settings[[14]]$settings <- list("foreground" = "#FFFFFF")
   
   tomorrowTmTheme$settings[[15]] <- list()
   tomorrowTmTheme$settings[[15]]$name <- "Diff insertion"
   tomorrowTmTheme$settings[[15]]$scope <- "markup.inserted.diff, meta.diff.header.to-file"
   tomorrowTmTheme$settings[[15]]$settings <- list("background" = "#718c00")
   
   tomorrowTmTheme$settings[[16]] <- list()
   tomorrowTmTheme$settings[[16]]$name <- "Diff deletion"
   tomorrowTmTheme$settings[[16]]$scope <- "markup.deleted.diff, meta.diff.header.from-file"
   tomorrowTmTheme$settings[[16]]$settings <- list("background" = "#c82829")
   
   tomorrowTmTheme$settings[[17]] <- list()
   tomorrowTmTheme$settings[[17]]$name <- "Diff header"
   tomorrowTmTheme$settings[[17]]$scope <- "meta.diff.header.from-file, meta.diff.header.to-file"
   tomorrowTmTheme$settings[[17]]$settings <- list("foreground" = "#FFFFFF", "background" = "#4271ae")
   
   tomorrowTmTheme$settings[[18]] <- list()
   tomorrowTmTheme$settings[[18]]$name <- "Diff range"
   tomorrowTmTheme$settings[[18]]$scope <- "meta.diff.range"
   tomorrowTmTheme$settings[[18]]$settings <- list("fontStyle" = "italic", "foreground" = "#3e999f")
   
   tomorrowTmTheme$uuid <- "82CCD69C-F1B1-4529-B39E-780F91F07604"
   tomorrowTmTheme$colorSpaceName <- "sRGB"
   
   # Expected Results
   tomorrowStyles <- list()
   tomorrowStyles$printMargin = "#e8e8e8"
   tomorrowStyles$background = "#FFFFFF"
   tomorrowStyles$foreground = "#4D4D4C"
   tomorrowStyles$gutter = "#f0f0f0"
   tomorrowStyles$selection = "#D6D6D6"
   tomorrowStyles$step = "rgb(198, 219, 174)"
   tomorrowStyles$bracket = "#D1D1D1"
   tomorrowStyles$active_line = "#EFEFEF"
   tomorrowStyles$cursor = "#AEAFAD"
   tomorrowStyles$invisible = "color:#D1D1D1;"
   tomorrowStyles$comment = "color:#8E908C;"
   tomorrowStyles$variable = "color:#C82829;"
   tomorrowStyles$string.regexp = "color:#C82829;"
   tomorrowStyles$entity.name.tag = "color:#C82829;"
   tomorrowStyles$`entity.other.attribute-name` = "color:#C82829;"
   tomorrowStyles$meta.tag = "color:#C82829;"
   tomorrowStyles$constant.numeric = "color:#F5871F;"
   tomorrowStyles$constant.language = "color:#F5871F;"
   tomorrowStyles$support.constant = "color:#F5871F;"
   tomorrowStyles$constant.character = "color:#F5871F;"
   tomorrowStyles$variable.parameter = "color:#F5871F;"
   tomorrowStyles$keyword.other.unit = "color:#F5871F;"
   tomorrowStyles$support.type = "color:#C99E00;"
   tomorrowStyles$support.class =  "color:#C99E00;"
   tomorrowStyles$string = "color:#718C00;"
   tomorrowStyles$markup.heading = "color:#718C00;"
   tomorrowStyles$keyword.operator = "color:#3E999F;"
   tomorrowStyles$entity.name.function = "color:#4271AE;"
   tomorrowStyles$support.function = "color:#4271AE;"
   tomorrowStyles$keyword = "color:#8959A8;"
   tomorrowStyles$storage = "color:#8959A8;"
   tomorrowStyles$storage.type = "color:#8959A8;"
   tomorrowStyles$invalid = "color:#FFFFFF;background-color:#C82829;"
   tomorrowStyles$invalid.deprecated = "color:#FFFFFF;background-color:#8959A8;"
   tomorrowStyles$fold = "#4271AE"
   tomorrowStyles$gutterBg = "#FFFFFF"
   tomorrowStyles$gutterFg = "rgb(166,166,166)"
   tomorrowStyles$selected_word_highlight = "border: 1px solid #D6D6D6;"
   tomorrowStyles$isDark = "false"
      
   unsupportedScopes = list(
      "keyword.operator.class" = 0,
      "constant.other" = 0,
      "source.php.embedded.line" = 0,
      "support.other.variable" = 0,
      "string.other.link" = 0,
      "declaration.tag" = 0,
      "markup.deleted.git_gutter" = 0,
      "punctuation.section.embedded" = 0,
      "entity.name.class" = 0,
      "entity.name.type.class" = 0,
      "constant.other.symbol" = 0,
      "entity.other.inherited-class" = 0,
      "entity.name.filename" = 0,
      "markup.inserted.git_gutter" = 0,
      "constant.other.color" = 0,
      "meta.function-call" = 0,
      "keyword.other.special-method" = 0,
      "meta.block-level" = 0,
      "markup.changed.git_gutter" = 0,
      "meta.separator" = 0,
      "markup.inserted.diff" = 1,
      "markup.deleted.diff" = 1,
      "meta.diff.header.to-file" = 2,
      "meta.diff.header.from-file" = 2,
      "meta.diff.range" = 0)
   
   expect_equal(
      .rs.extractStyles(tomorrowTmTheme, supportedScopes),
      list(
         "styles" = tomorrowStyles,
         "unsupportedScopes" = unsupportedScopes))
})

# Test convertTmTheme ==============================================================================
test_that("convertTmTheme works correctly without parseTmTheme", {
   library("xml2")
   
   # Setup objects for test cases
   tomorrowTmTheme <- list()
   tomorrowTmTheme$comment <- "http://chriskempson.com"
   tomorrowTmTheme$name <- "Tomorrow"
   tomorrowTmTheme$settings <- list()
   
   tomorrowTmTheme$settings[[1]] <- list()
   tomorrowTmTheme$settings[[1]]$settings <- list(
      "background" = "#FFFFFF",
      "caret" ="#AEAFAD",
      "foreground" = "#4D4D4C",
      "invisibles" = "#D1D1D1",
      "lineHighlight" = "#EFEFEF",
      "selection" = "#D6D6D6")
   
   tomorrowTmTheme$settings[[2]] <- list()
   tomorrowTmTheme$settings[[2]]$name <- "Comment"
   tomorrowTmTheme$settings[[2]]$scope <- "comment"
   tomorrowTmTheme$settings[[2]]$settings <- list("foreground" = "#8E908C")
   
   tomorrowTmTheme$settings[[3]] <- list()
   tomorrowTmTheme$settings[[3]]$name <- "Foreground"
   tomorrowTmTheme$settings[[3]]$scope <- "keyword.operator.class, constant.other, source.php.embedded.line"
   tomorrowTmTheme$settings[[3]]$settings <- list()
   tomorrowTmTheme$settings[[3]]$settings$fontStyle <- ""
   tomorrowTmTheme$settings[[3]]$settings$foreground <- "#666969"
   
   tomorrowTmTheme$settings[[4]] <- list()
   tomorrowTmTheme$settings[[4]]$name <- "Variable, String Link, Regular Expression, Tag Name, GitGutter deleted"
   tomorrowTmTheme$settings[[4]]$scope <- "variable, support.other.variable, string.other.link, string.regexp, entity.name.tag, entity.other.attribute-name, meta.tag, declaration.tag, markup.deleted.git_gutter"
   tomorrowTmTheme$settings[[4]]$settings <- list("foreground" = "#C82829")
   
   tomorrowTmTheme$settings[[5]] <- list()
   tomorrowTmTheme$settings[[5]]$name <- "Number, Constant, Function Argument, Tag Attribute, Embedded"
   tomorrowTmTheme$settings[[5]]$scope <- "constant.numeric, constant.language, support.constant, constant.character, variable.parameter, punctuation.section.embedded, keyword.other.unit"
   tomorrowTmTheme$settings[[5]]$settings <- list("fontStyle" = "", "foreground" = "#F5871F")
   
   tomorrowTmTheme$settings[[6]] <- list()
   tomorrowTmTheme$settings[[6]]$name <- "Class, Support"
   tomorrowTmTheme$settings[[6]]$scope <- "entity.name.class, entity.name.type.class, support.type, support.class"
   tomorrowTmTheme$settings[[6]]$settings <- list("fontStyle" = "", "foreground" = "#C99E00")
   
   tomorrowTmTheme$settings[[7]] <- list()
   tomorrowTmTheme$settings[[7]]$name <- "String, Symbols, Inherited Class, Markup Heading, GitGutter inserted"
   tomorrowTmTheme$settings[[7]]$scope <- "string, constant.other.symbol, entity.other.inherited-class, entity.name.filename, markup.heading, markup.inserted.git_gutter"
   tomorrowTmTheme$settings[[7]]$settings <- list("fontStyle" = "", "foreground" = "#718C00")
   
   tomorrowTmTheme$settings[[8]] <- list()
   tomorrowTmTheme$settings[[8]]$name <- "Operator, Misc"
   tomorrowTmTheme$settings[[8]]$scope <- "keyword.operator, constant.other.color"
   tomorrowTmTheme$settings[[8]]$settings <- list("foreground" = "#3E999F")
   
   tomorrowTmTheme$settings[[9]] <- list()
   tomorrowTmTheme$settings[[9]]$name <- "Function, Special Method, Block Level, GitGutter changed"
   tomorrowTmTheme$settings[[9]]$scope <- "entity.name.function, meta.function-call, support.function, keyword.other.special-method, meta.block-level, markup.changed.git_gutter"
   tomorrowTmTheme$settings[[9]]$settings <- list("fontStyle" = "", "foreground" = "#4271AE")
   
   tomorrowTmTheme$settings[[10]] <- list()
   tomorrowTmTheme$settings[[10]]$name <- "Keyword, Storage"
   tomorrowTmTheme$settings[[10]]$scope <- "keyword, storage, storage.type"
   tomorrowTmTheme$settings[[10]]$settings <- list("fontStyle" = "", "foreground" = "#8959A8")
   
   tomorrowTmTheme$settings[[11]] <- list()
   tomorrowTmTheme$settings[[11]]$name <- "Invalid"
   tomorrowTmTheme$settings[[11]]$scope <- "invalid"
   tomorrowTmTheme$settings[[11]]$settings <- list("background" = "#C82829", "fontStyle" = "", "foreground" = "#FFFFFF")
   
   tomorrowTmTheme$settings[[12]] <- list()
   tomorrowTmTheme$settings[[12]]$name <- "Separator"
   tomorrowTmTheme$settings[[12]]$scope <- "meta.separator"
   tomorrowTmTheme$settings[[12]]$settings <- list("background" = "#4271AE", "foreground" = "#FFFFFF")
   
   tomorrowTmTheme$settings[[13]] <- list()
   tomorrowTmTheme$settings[[13]]$name <- "Deprecated"
   tomorrowTmTheme$settings[[13]]$scope <- "invalid.deprecated"
   tomorrowTmTheme$settings[[13]]$settings <- list("background" = "#8959A8", "fontStyle" = "", "foreground" = "#FFFFFF")
   
   tomorrowTmTheme$settings[[14]] <- list()
   tomorrowTmTheme$settings[[14]]$name <- "Diff foreground"
   tomorrowTmTheme$settings[[14]]$scope <- "markup.inserted.diff, markup.deleted.diff, meta.diff.header.to-file, meta.diff.header.from-file"
   tomorrowTmTheme$settings[[14]]$settings <- list("foreground" = "#FFFFFF")
   
   tomorrowTmTheme$settings[[15]] <- list()
   tomorrowTmTheme$settings[[15]]$name <- "Diff insertion"
   tomorrowTmTheme$settings[[15]]$scope <- "markup.inserted.diff, meta.diff.header.to-file"
   tomorrowTmTheme$settings[[15]]$settings <- list("background" = "#718c00")
   
   tomorrowTmTheme$settings[[16]] <- list()
   tomorrowTmTheme$settings[[16]]$name <- "Diff deletion"
   tomorrowTmTheme$settings[[16]]$scope <- "markup.deleted.diff, meta.diff.header.from-file"
   tomorrowTmTheme$settings[[16]]$settings <- list("background" = "#c82829")
   
   tomorrowTmTheme$settings[[17]] <- list()
   tomorrowTmTheme$settings[[17]]$name <- "Diff header"
   tomorrowTmTheme$settings[[17]]$scope <- "meta.diff.header.from-file, meta.diff.header.to-file"
   tomorrowTmTheme$settings[[17]]$settings <- list("foreground" = "#FFFFFF", "background" = "#4271ae")
   
   tomorrowTmTheme$settings[[18]] <- list()
   tomorrowTmTheme$settings[[18]]$name <- "Diff range"
   tomorrowTmTheme$settings[[18]]$scope <- "meta.diff.range"
   tomorrowTmTheme$settings[[18]]$settings <- list("fontStyle" = "italic", "foreground" = "#3e999f")
   
   tomorrowTmTheme$uuid <- "82CCD69C-F1B1-4529-B39E-780F91F07604"
   tomorrowTmTheme$colorSpaceName <- "sRGB"
   
   tomorrowConverted <- .rs.convertTmTheme(tomorrowTmTheme)
   
   # expected results
   conn <- file(file.path(inputFileLocation, "acecss", "tomorrow.css"))
   
   expect_true(compareCss(parseCss(tomorrowConverted$theme), parseCss(readLines(conn))))
   expect_false(tomorrowConverted$isDark)
   
   close(conn)
})

test_that("convertTmTheme works correctly with parseTmTheme", {
   library("xml2")
   
   # Actual results
   active4dConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Active4D.tmTheme")))
   
   allHallowsEveConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "All Hallows Eve.tmTheme")))
   
   amyConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Amy.tmTheme")))
   
   blackboardConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Blackboard.tmTheme")))
   
   brillianceBlackConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Brilliance Black.tmTheme")))
   
   brillianceDullConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Brilliance Dull.tmTheme")))
   
   chromeDevToolsConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Chrome DevTools.tmTheme")))
   
   cloudsMidnightConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Clouds Midnight.tmTheme")))
   
   cloudsConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Clouds.tmTheme")))
   
   cobaltConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Cobalt.tmTheme")))
   
   dawnConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Dawn.tmTheme")))
   
   dreamweaverConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Dreamweaver.tmTheme")))
   
   eiffelConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Eiffel.tmTheme")))
   
   espressoLibreConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Espresso Libre.tmTheme")))
   
   gitHubConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "GitHub.tmTheme")))
   
   idleConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "IDLE.tmTheme")))
   
   katzenmilchConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Katzenmilch.tmTheme")))
   
   kuroirConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Kuroir Theme.tmTheme")))
   
   lazyConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "LAZY.tmTheme")))
   
   magicWBConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "MagicWB (Amiga).tmTheme")))
   
   merbivoreSoftConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Merbivore Soft.tmTheme")))
   
   merbivoreConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Merbivore.tmTheme")))
   
   monokaiConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Monokai.tmTheme")))
   
   pastelsOnDarkConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Pastels on Dark.tmTheme")))
   
   slushAndPoppiesConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Slush and Poppies.tmTheme")))
   
   solarizedDarkConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Solarized-dark.tmTheme")))
   
   solarizedLightConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Solarized-light.tmTheme")))
   
   sunburstConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Sunburst.tmTheme")))
   
   textmateConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Textmate (Mac Classic).tmTheme")))
   
   tomorrowNightBlueConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Tomorrow-Night-Blue.tmTheme")))
   
   tomorrowNightBrightConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Tomorrow-Night-Bright.tmTheme")))
   
   tomorrowNightEightiesConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Tomorrow-Night-Eighties.tmTheme")))
   
   tomorrowNightConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Tomorrow-Night.tmTheme")))
   
   tomorrowConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Tomorrow.tmTheme")))
   
   twilightConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Twilight.tmTheme")))
   
   vibrantInkConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Vibrant Ink.tmTheme")))
   
   xcodeDefaultConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Xcode_default.tmTheme")))
   
   zenburnesqueConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "Zenburnesque.tmTheme")))
   
   iPlasticConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "iPlastic.tmTheme")))
   
   idleFingersConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "idleFingers.tmTheme")))
   
   krThemeConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "krTheme.tmTheme")))
   
   monoindustrialConverted <- .rs.convertTmTheme(
      .rs.parseTmTheme(
         file.path(
            inputFileLocation,
            "tmThemes",
            "monoindustrial.tmTheme")))

   # Expected results
   f <- file(file.path(inputFileLocation, "acecss", "active4d.css"))
   active4dCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "all_hallows_eve.css"))
   allHallowsEveCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "amy.css"))
   amyCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "blackboard.css"))
   blackboardCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "brilliance_black.css"))
   brillianceBlackCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "brilliance_dull.css"))
   brillianceDullCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "chrome_dev_tools.css"))
   chromeDevToolsCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "clouds_midnight.css"))
   cloudsMidnightCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "clouds.css"))
   cloudsCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "cobalt.css"))
   cobaltCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "dawn.css"))
   dawnCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "dreamweaver.css"))
   dreamweaverCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "eiffel.css"))
   eiffelCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "espresso_libre.css"))
   espressoLibreCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "git_hub.css"))
   gitHubCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "idle.css"))
   idleCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "katzenmilch.css"))
   katzenmilchCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "kuroir_theme.css"))
   kuroirCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "lazy.css"))
   lazyCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "magic_wb_amiga.css"))
   magicWBCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "merbivore_soft.css"))
   merbivoreSoftCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "merbivore.css"))
   merbivoreCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "monokai.css"))
   monokaiCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "pastels_on_dark.css"))
   pastelsOnDarkCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "slush_and_poppies.css"))
   slushAndPoppiesCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "solarized_dark.css"))
   solarizedDarkCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "solarized_light.css"))
   solarizedLightCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "sunburst.css"))
   sunburstCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "textmate.css"))
   textmateCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "tomorrow_night_blue.css"))
   tomorrowNightBlueCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "tomorrow_night_bright.css"))
   tomorrowNightBrightCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "tomorrow_night_eighties.css"))
   tomorrowNightEightiesCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "tomorrow_night.css"))
   tomorrowNightCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "tomorrow.css"))
   tomorrowCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "twilight.css"))
   twilightCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "vibrant_ink.css"))
   vibrantInkCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "xcode_default.css"))
   xcodeDefaultCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "zenburnesque.css"))
   zenburnesqueCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "i_plastic.css"))
   iPlasticCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "idle_fingers.css"))
   idleFingersCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "kr_theme.css"))
   krThemeCssEx <- parseCss(readLines(f))
   close(f)
   
   f <- file(file.path(inputFileLocation, "acecss", "mono_industrial.css"))
   monoindustrialCssEx <- parseCss(readLines(f))
   close(f)
   
   # Tests
   expect_true(compareCss(parseCss(active4dConverted$theme), active4dCssEx))
   expect_false(active4dConverted$isDark)
   
   expect_true(compareCss(parseCss(allHallowsEveConverted$theme), allHallowsEveCssEx))
   expect_true(allHallowsEveConverted$isDark)
   
   expect_true(compareCss(parseCss(amyConverted$theme), amyCssEx))
   expect_true(amyConverted$isDark)
   
   expect_true(compareCss(parseCss(blackboardConverted$theme), blackboardCssEx))
   expect_true(blackboardConverted$isDark)
   
   expect_true(compareCss(parseCss(brillianceBlackConverted$theme), brillianceBlackCssEx))
   expect_true(brillianceBlackConverted$isDark)
   
   expect_true(compareCss(parseCss(brillianceDullConverted$theme), brillianceDullCssEx))
   expect_true(brillianceDullConverted$isDark)
   
   expect_true(compareCss(parseCss(chromeDevToolsConverted$theme), chromeDevToolsCssEx))
   expect_false(chromeDevToolsConverted$isDark)
   
   expect_true(compareCss(parseCss(cloudsMidnightConverted$theme), cloudsMidnightCssEx))
   expect_true(cloudsMidnightConverted$isDark)
   
   expect_true(compareCss(parseCss(cloudsConverted$theme), cloudsCssEx))
   expect_false(cloudsConverted$isDark)
   
   expect_true(compareCss(parseCss(cobaltConverted$theme), cobaltCssEx))
   expect_true(cobaltConverted$isDark)
   
   expect_true(compareCss(parseCss(dawnConverted$theme), dawnCssEx))
   expect_false(dawnConverted$isDark)
   
   expect_true(compareCss(parseCss(dreamweaverConverted$theme), dreamweaverCssEx))
   expect_false(dreamweaverConverted$isDark)
   
   expect_true(compareCss(parseCss(eiffelConverted$theme), eiffelCssEx))
   expect_false(eiffelConverted$isDark)
   
   expect_true(compareCss(parseCss(espressoLibreConverted$theme), espressoLibreCssEx))
   expect_true(espressoLibreConverted$isDark)
   
   expect_true(compareCss(parseCss(gitHubConverted$theme), gitHubCssEx))
   expect_false(gitHubConverted$isDark)
   
   expect_true(compareCss(parseCss(idleConverted$theme), idleCssEx))
   expect_false(idleConverted$isDark)
   
   expect_true(compareCss(parseCss(katzenmilchConverted$theme), katzenmilchCssEx))
   expect_false(katzenmilchConverted$isDark)
   
   expect_true(compareCss(parseCss(kuroirConverted$theme), kuroirCssEx))
   expect_false(kuroirConverted$isDark)
   
   expect_true(compareCss(parseCss(lazyConverted$theme), lazyCssEx))
   expect_false(lazyConverted$isDark)
   
   expect_true(compareCss(parseCss(magicWBConverted$theme), magicWBCssEx))
   expect_false(magicWBConverted$isDark)
   
   expect_true(compareCss(parseCss(merbivoreSoftConverted$theme), merbivoreSoftCssEx))
   expect_true(merbivoreSoftConverted$isDark)
   
   expect_true(compareCss(parseCss(merbivoreConverted$theme), merbivoreCssEx))
   expect_true(merbivoreConverted$isDark)
   
   expect_true(compareCss(parseCss(monokaiConverted$theme), monokaiCssEx))
   expect_true(monokaiConverted$isDark)
   
   expect_true(compareCss(parseCss(pastelsOnDarkConverted$theme), pastelsOnDarkCssEx))
   expect_true(pastelsOnDarkConverted$isDark)
   
   expect_true(compareCss(parseCss(slushAndPoppiesConverted$theme), slushAndPoppiesCssEx))
   expect_false(slushAndPoppiesConverted$isDark)
   
   expect_true(compareCss(parseCss(solarizedDarkConverted$theme), solarizedDarkCssEx))
   expect_true(solarizedDarkConverted$isDark)
   
   expect_true(compareCss(parseCss(solarizedLightConverted$theme), solarizedLightCssEx))
   expect_false(solarizedLightConverted$isDark)
   
   expect_true(compareCss(parseCss(sunburstConverted$theme), sunburstCssEx))
   expect_true(sunburstConverted$isDark)
   
   expect_true(compareCss(parseCss(textmateConverted$theme), textmateCssEx))
   expect_false(textmateConverted$isDark)
   
   expect_true(compareCss(parseCss(tomorrowNightBlueConverted$theme), tomorrowNightBlueCssEx))
   expect_true(tomorrowNightBlueConverted$isDark)
   
   expect_true(compareCss(parseCss(tomorrowNightBrightConverted$theme), tomorrowNightBrightCssEx))
   expect_true(tomorrowNightBrightConverted$isDark)
   
   expect_true(compareCss(parseCss(tomorrowNightEightiesConverted$theme), tomorrowNightEightiesCssEx))
   expect_true(tomorrowNightEightiesConverted$isDark)
   
   expect_true(compareCss(parseCss(tomorrowNightConverted$theme), tomorrowNightCssEx))
   expect_true(tomorrowNightConverted$isDark)
   
   expect_true(compareCss(parseCss(tomorrowConverted$theme), tomorrowCssEx))
   expect_false(tomorrowConverted$isDark)
   
   expect_true(compareCss(parseCss(twilightConverted$theme), twilightCssEx))
   expect_true(twilightConverted$isDark)
   
   expect_true(compareCss(parseCss(vibrantInkConverted$theme), vibrantInkCssEx))
   expect_true(vibrantInkConverted$isDark)
   
   expect_true(compareCss(parseCss(xcodeDefaultConverted$theme), xcodeDefaultCssEx))
   expect_false(xcodeDefaultConverted$isDark)
   
   expect_true(compareCss(parseCss(zenburnesqueConverted$theme), zenburnesqueCssEx))
   expect_true(zenburnesqueConverted$isDark)
   
   expect_true(compareCss(parseCss(iPlasticConverted$theme), iPlasticCssEx))
   expect_false(iPlasticConverted$isDark)
   
   expect_true(compareCss(parseCss(idleFingersConverted$theme), idleFingersCssEx))
   expect_true(idleFingersConverted$isDark)
   
   expect_true(compareCss(parseCss(krThemeConverted$theme), krThemeCssEx))
   expect_true(krThemeConverted$isDark)
   
   expect_true(compareCss(parseCss(monoindustrialConverted$theme), monoindustrialCssEx))
   expect_true(monoindustrialConverted$isDark)
})

# Test parseKeyElement =============================================================================
test_that("parseKeyElement works correctly", {
   library("xml2")

   # Setup objects for the test cases
   settingsNode <- xml2::read_xml("<key>settings</key>")
   valueNode <- xml2::read_xml("<key>VALUE</key>")
   emptyNode <- xml2::read_xml("<key></key>")
   noTextNode <- xml2::read_xml("<key/>")

   # Test cases
   expect_equal(.rs.parseKeyElement(settingsNode), "settings")
   expect_equal(.rs.parseKeyElement(valueNode), "VALUE")
   expect_error(
      .rs.parseKeyElement(emptyNode),
      "Unable to convert the tmtheme to an rstheme. The value of a \"key\" element may not be empty.")
   expect_error(
      .rs.parseKeyElement(noTextNode),
      "Unable to convert the tmtheme to an rstheme. The value of a \"key\" element may not be empty.")
})

# Test parseStringElement ==========================================================================
test_that("parseStringElement works correctly", {
   library("xml2")

   # Setup objects for the test cases
   nameNode <- xml2::read_xml("<string>Tomorrow</string>")
   colorNode <- xml2::read_xml("<string>#8e908c</string>")
   emptyNode <- xml2::read_xml("<string></string>")
   noTextNode <- xml2::read_xml("<string/>")

   # Test cases (no errors)
   expect_equal(.rs.parseStringElement(nameNode, "name"), "Tomorrow")
   expect_equal(.rs.parseStringElement(colorNode, "foreground"), "#8e908c")
   expect_equal(.rs.parseStringElement(nameNode, ""), "Tomorrow")
   expect_equal(.rs.parseStringElement(emptyNode, "scope"), "")
   expect_equal(.rs.parseStringElement(emptyNode, "settings"), "")

   # Test cases (errors)
   expect_error(
      .rs.parseStringElement(colorNode, NULL),
      "Unable to convert the tmtheme to an rstheme. Unable to find a key for the \"string\" element with value \"#8e908c\".")
})

# Test parseDictElement ============================================================================
test_that("parseDictElement works correctly", {
   library("xml2")

   # Setup input objects for the test cases
   simpleDictEl <- xml2::read_xml(
      "<dict>
         <key>fontStyle</key>
         <string/>
         <key>foreground</key>
         <string>#F5871F</string>
      </dict>")

   simpleDictEl2 <- xml2::read_xml(
      "<dict>
         <key>background</key>
         <string>#FFFFFF</string>
         <key>caret</key>
         <string>#AEAFAD</string>
         <key>foreground</key>
         <string>#4D4D4C</string>
         <key>invisibles</key>
         <string>#D1D1D1</string>
         <key>lineHighlight</key>
         <string>#EFEFEF</string>
         <key>selection</key>
         <string>#D6D6D6</string>
      </dict>")

   badDictEl <- xml2::read_xml(
      "<dict>
         <key>caret</key>
         <string>#AEAFAD</string>
         <string>#FFFFFF</string>
      </dict>")

   badDictEl2 <- xml2::read_xml(
      "<dict>
         <string>#4D4D4C</string>
         <key>caret</key>
         <string>#AEAFAD</string>
      </dict>")

   badDictEl3 <- xml2::read_xml(
      "<dict>
         <key>caret</key>
         <string>#AEAFAD</string>
         <other>#000000</other>
      </dict>")

   badDictEl4 <- xml2::read_xml(
      "<dict>
         <bad>#1D1D1D</bad>
         <key>caret</key>
         <string>#AEAFAD</string>
      </dict>")

   emptyDictEl <- xml2::read_xml("<dict/>")

   recursiveDictEl <- xml2::read_xml(
      "<dict>
         <key>settings</key>
         <dict>
            <key>background</key>
            <string>#FFFFFF</string>
            <key>caret</key>
            <string>#AEAFAD</string>
            <key>foreground</key>
            <string>#4D4D4C</string>
            <key>invisibles</key>
            <string>#D1D1D1</string>
            <key>lineHighlight</key>
            <string>#EFEFEF</string>
            <key>selection</key>
            <string>#D6D6D6</string>
         </dict>
      </dict>")

   recursiveDictEl2 <- xml2::read_xml(
      "<dict>
         <key>name</key> 
         <string>Number, Constant, Function Argument, Tag Attribute, Embedded</string>
         <key>scope</key>
         <string>constant.numeric, constant.language, support.constant, constant.character, variable.parameter, punctuation.section.embedded, keyword.other.unit</string>
         <key>settings</key>
         <dict>
            <key>fontStyle</key>
            <string/>
            <key>foreground</key>
            <string>#F5871F</string>
         </dict>
      </dict>")

   # Setup expected objects for the test cases
   simpleExpect <- list()
   simpleExpect[["fontStyle"]] <- ""
   simpleExpect[["foreground"]] <- "#F5871F"

   simpleExpect2 <- list()
   simpleExpect2[["background"]] <- "#FFFFFF"
   simpleExpect2[["caret"]] <-"#AEAFAD"
   simpleExpect2[["foreground"]] <- "#4D4D4C"
   simpleExpect2[["invisibles"]] <- "#D1D1D1"
   simpleExpect2[["lineHighlight"]] <-"#EFEFEF"
   simpleExpect2[["selection"]] <- "#D6D6D6"

   recursiveExpect <- list()
   recursiveExpect[["settings"]] <- simpleExpect2

   recursiveExpect2 <- list()
   recursiveExpect2[["name"]] <- "Number, Constant, Function Argument, Tag Attribute, Embedded"
   recursiveExpect2[["scope"]] <- "constant.numeric, constant.language, support.constant, constant.character, variable.parameter, punctuation.section.embedded, keyword.other.unit"
   recursiveExpect2[["settings"]] <- simpleExpect

   # Test Cases (no error)
   expect_equal(.rs.parseDictElement(simpleDictEl, "settings"), simpleExpect)
   expect_equal(.rs.parseDictElement(simpleDictEl2, "settings"), simpleExpect2)
   expect_equal(.rs.parseDictElement(recursiveDictEl, ""), recursiveExpect)
   expect_equal(.rs.parseDictElement(recursiveDictEl2, "someName"), recursiveExpect2)
   expect_equal(.rs.parseDictElement(emptyDictEl, "settings"), list())

   # Test Cases (errors)
   expect_error(
      .rs.parseDictElement(simpleDictEl, NULL),
      "Unable to convert the tmtheme to an rstheme. Unable to find a key for the current \"dict\" element.",
      fixed = TRUE)
   expect_error(
      .rs.parseDictElement(badDictEl, "settings"),
      "Unable to convert the tmtheme to an rstheme. Unable to find a key for the \"string\" element with value \"#FFFFFF\".",
      fixed = TRUE)
   expect_error(
      .rs.parseDictElement(badDictEl2, "settings"),
      "Unable to convert the tmtheme to an rstheme. Unable to find a key for the \"string\" element with value \"#4D4D4C\".",
      fixed = TRUE)
   expect_error(
      .rs.parseDictElement(badDictEl3, "settings"),
      "Unable to convert the tmtheme to an rstheme. Encountered unexpected element as a child of the current \"dict\" element: \"other\". Expected \"key\", \"string\", \"array\", or \"dict\".",
      fixed = TRUE)
   expect_error(
      .rs.parseDictElement(badDictEl4, "settings"),
      "Unable to convert the tmtheme to an rstheme. Encountered unexpected element as a child of the current \"dict\" element: \"bad\". Expected \"key\", \"string\", \"array\", or \"dict\".",
      fixed = TRUE)
})

# Test parseArrayElement ===========================================================================
test_that("parseArrayElement works correctly", {
   library("xml2")
   
   # Setup test case input objects
   emptyArrayEl <- xml2::read_xml("<array/>")
   textArrayEl <- xml2::read_xml("<array>some text</array>")
   badArrayEl <- xml2::read_xml(
      "<array>
         <dict>
            <key>fontStyle</key>
            <string/>
            <key>foreground</key>
            <string>#F5871F</string>
         </dict>
         <bad/>
      </array>")
   badArrayEl2 <- xml2::read_xml(
      "<array>
         <notGood/>
         <dict>
            <key>fontStyle</key>
            <string/>
            <key>foreground</key>
            <string>#F5871F</string>
         </dict>
      </array>")
   
   arrayEl <- xml2::read_xml(
      "<array>
         <dict>
            <key>settings</key>
            <dict>
               <key>background</key>
               <string>#FFFFFF</string>
               <key>caret</key>
               <string>#AEAFAD</string>
               <key>foreground</key>
               <string>#4D4D4C</string>
               <key>invisibles</key>
               <string>#D1D1D1</string>
               <key>lineHighlight</key>
               <string>#EFEFEF</string>
               <key>selection</key>
               <string>#D6D6D6</string>
            </dict>
         </dict>
         <dict>
            <key>name</key>
            <string>Number, Constant, Function Argument, Tag Attribute, Embedded</string>
            <key>scope</key>
            <string>constant.numeric, constant.language, support.constant, constant.character, variable.parameter, punctuation.section.embedded, keyword.other.unit</string>
            <key>settings</key>
            <dict>
               <key>fontStyle</key>
               <string/>
               <key>foreground</key>
               <string>#F5871F</string>
            </dict>
         </dict>
      </array>")

   # Setup expected output
   arrayExpected <- list()
   arrayExpected[[1]] <- list()
   arrayExpected[[1]][["settings"]] <- list()
   arrayExpected[[1]][["settings"]][["background"]] <- "#FFFFFF"
   arrayExpected[[1]][["settings"]][["caret"]] <- "#AEAFAD"
   arrayExpected[[1]][["settings"]][["foreground"]] <- "#4D4D4C"
   arrayExpected[[1]][["settings"]][["invisibles"]] <- "#D1D1D1"
   arrayExpected[[1]][["settings"]][["lineHighlight"]] <- "#EFEFEF"
   arrayExpected[[1]][["settings"]][["selection"]] <- "#D6D6D6"
   arrayExpected[[2]] <- list()
   arrayExpected[[2]][["name"]] <- "Number, Constant, Function Argument, Tag Attribute, Embedded"
   arrayExpected[[2]][["scope"]] <- "constant.numeric, constant.language, support.constant, constant.character, variable.parameter, punctuation.section.embedded, keyword.other.unit"
   arrayExpected[[2]][["settings"]] <- list()
   arrayExpected[[2]][["settings"]][["fontStyle"]] <- ""
   arrayExpected[[2]][["settings"]][["foreground"]] <- "#F5871F"

   # Test cases (no error)
   expect_equal(.rs.parseArrayElement(arrayEl, "settings"), arrayExpected)

   # Test cases (error)
   expect_error(
      .rs.parseArrayElement(arrayEl, NULL),
      "Unable to convert the tmtheme to an rstheme. Unable to find a key for array value.",
      fixed = TRUE)
   expect_error(
      .rs.parseArrayElement(arrayEl, "notSettings"),
      "Unable to convert the tmtheme to an rstheme. Incorrect key for array element. Expected: \"settings\"; Actual: \"notSettings\".",
      fixed = TRUE)
   expect_error(
      .rs.parseArrayElement(emptyArrayEl, "settings"),
      "Unable to convert the tmtheme to an rstheme. \"array\" element cannot be empty.",
      fixed = TRUE)
   expect_error(
      .rs.parseArrayElement(textArrayEl, "settings"),
      "Unable to convert the tmtheme to an rstheme. \"array\" element cannot be empty.",
      fixed = TRUE)
   expect_error(
      .rs.parseArrayElement(badArrayEl, "settings"),
      "Unable to convert the tmtheme to an rstheme. Expecting \"dict\" element; found \"bad\".",
      fixed = TRUE)
   expect_error(
      .rs.parseArrayElement(badArrayEl2, "settings"),
      "Unable to convert the tmtheme to an rstheme. Expecting \"dict\" element; found \"notGood\".",
      fixed = TRUE)
})

# Test parseTmTheme ================================================================================
test_that("parseTmTheme handles correct input", {
   library("xml2")
   
   # Setup expected output
   expected <- list()
   expected$comment <- "http://chriskempson.com"
   expected$name <- "Tomorrow"
   expected$settings <- list()

   expected$settings[[1]] <- list()
   expected$settings[[1]]$settings <- list(
      "background" = "#FFFFFF",
      "caret" ="#AEAFAD",
      "foreground" = "#4D4D4C",
      "invisibles" = "#D1D1D1",
      "lineHighlight" = "#EFEFEF",
      "selection" = "#D6D6D6")

   expected$settings[[2]] <- list()
   expected$settings[[2]]$name <- "Comment"
   expected$settings[[2]]$scope <- "comment"
   expected$settings[[2]]$settings <- list("foreground" = "#8E908C")

   expected$settings[[3]] <- list()
   expected$settings[[3]]$name <- "Foreground"
   expected$settings[[3]]$scope <- "keyword.operator.class, constant.other, source.php.embedded.line"
   expected$settings[[3]]$settings <- list()
   expected$settings[[3]]$settings$fontStyle <- ""
   expected$settings[[3]]$settings$foreground <- "#666969"

   expected$settings[[4]] <- list()
   expected$settings[[4]]$name <- "Variable, String Link, Regular Expression, Tag Name, GitGutter deleted"
   expected$settings[[4]]$scope <- "variable, support.other.variable, string.other.link, string.regexp, entity.name.tag, entity.other.attribute-name, meta.tag, declaration.tag, markup.deleted.git_gutter"
   expected$settings[[4]]$settings <- list("foreground" = "#C82829")

   expected$settings[[5]] <- list()
   expected$settings[[5]]$name <- "Number, Constant, Function Argument, Tag Attribute, Embedded"
   expected$settings[[5]]$scope <- "constant.numeric, constant.language, support.constant, constant.character, variable.parameter, punctuation.section.embedded, keyword.other.unit"
   expected$settings[[5]]$settings <- list("fontStyle" = "", "foreground" = "#F5871F")
   
   expected$settings[[6]] <- list()
   expected$settings[[6]]$name <- "Class, Support"
   expected$settings[[6]]$scope <- "entity.name.class, entity.name.type.class, support.type, support.class"
   expected$settings[[6]]$settings <- list("fontStyle" = "", "foreground" = "#C99E00")
   
   expected$settings[[7]] <- list()
   expected$settings[[7]]$name <- "String, Symbols, Inherited Class, Markup Heading, GitGutter inserted"
   expected$settings[[7]]$scope <- "string, constant.other.symbol, entity.other.inherited-class, entity.name.filename, markup.heading, markup.inserted.git_gutter"
   expected$settings[[7]]$settings <- list("fontStyle" = "", "foreground" = "#718C00")
   
   expected$settings[[8]] <- list()
   expected$settings[[8]]$name <- "Operator, Misc"
   expected$settings[[8]]$scope <- "keyword.operator, constant.other.color"
   expected$settings[[8]]$settings <- list("foreground" = "#3E999F")
   
   expected$settings[[9]] <- list()
   expected$settings[[9]]$name <- "Function, Special Method, Block Level, GitGutter changed"
   expected$settings[[9]]$scope <- "entity.name.function, meta.function-call, support.function, keyword.other.special-method, meta.block-level, markup.changed.git_gutter"
   expected$settings[[9]]$settings <- list("fontStyle" = "", "foreground" = "#4271AE")
   
   expected$settings[[10]] <- list()
   expected$settings[[10]]$name <- "Keyword, Storage"
   expected$settings[[10]]$scope <- "keyword, storage, storage.type"
   expected$settings[[10]]$settings <- list("fontStyle" = "", "foreground" = "#8959A8")
   
   expected$settings[[11]] <- list()
   expected$settings[[11]]$name <- "Invalid"
   expected$settings[[11]]$scope <- "invalid"
   expected$settings[[11]]$settings <- list("background" = "#C82829", "fontStyle" = "", "foreground" = "#FFFFFF")
   
   expected$settings[[12]] <- list()
   expected$settings[[12]]$name <- "Separator"
   expected$settings[[12]]$scope <- "meta.separator"
   expected$settings[[12]]$settings <- list("background" = "#4271AE", "foreground" = "#FFFFFF")
   
   expected$settings[[13]] <- list()
   expected$settings[[13]]$name <- "Deprecated"
   expected$settings[[13]]$scope <- "invalid.deprecated"
   expected$settings[[13]]$settings <- list("background" = "#8959A8", "fontStyle" = "", "foreground" = "#FFFFFF")
   
   expected$settings[[14]] <- list()
   expected$settings[[14]]$name <- "Diff foreground"
   expected$settings[[14]]$scope <- "markup.inserted.diff, markup.deleted.diff, meta.diff.header.to-file, meta.diff.header.from-file"
   expected$settings[[14]]$settings <- list("foreground" = "#FFFFFF")
   
   expected$settings[[15]] <- list()
   expected$settings[[15]]$name <- "Diff insertion"
   expected$settings[[15]]$scope <- "markup.inserted.diff, meta.diff.header.to-file"
   expected$settings[[15]]$settings <- list("background" = "#718c00")
   
   expected$settings[[16]] <- list()
   expected$settings[[16]]$name <- "Diff deletion"
   expected$settings[[16]]$scope <- "markup.deleted.diff, meta.diff.header.from-file"
   expected$settings[[16]]$settings <- list("background" = "#c82829")
   
   expected$settings[[17]] <- list()
   expected$settings[[17]]$name <- "Diff header"
   expected$settings[[17]]$scope <- "meta.diff.header.from-file, meta.diff.header.to-file"
   expected$settings[[17]]$settings <- list("foreground" = "#FFFFFF", "background" = "#4271ae")
   
   expected$settings[[18]] <- list()
   expected$settings[[18]]$name <- "Diff range"
   expected$settings[[18]]$scope <- "meta.diff.range"
   expected$settings[[18]]$settings <- list("fontStyle" = "italic", "foreground" = "#3e999f")
   
   expected$uuid <- "82CCD69C-F1B1-4529-B39E-780F91F07604"
   expected$colorSpaceName <- "sRGB"
   
   # Test cases (no error)
   expect_equal(.rs.parseTmTheme(file.path(inputFileLocation, "tmThemes", "Tomorrow.tmTheme")), expected)
})

test_that("parseTmTheme handles incorrect input", {
   library("xml2")
   
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "EmptyBody.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Expected 1 non-text child of the root, found: 0",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "EmptyDictEl.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. \"dict\" element cannot be empty.",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "ExtraChildAfter.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Expected 1 non-text child of the root, found: 2",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "ExtraChildBefore.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Expected 1 non-text child of the root, found: 2",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "ExtraChildMid.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Expecting \"dict\" element; found \"otherChild\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "Malformed1.tmTheme")),
      sprintf(
         "error parsing attribute name [68]", 
         inputFileLocation),
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "Malformed2.tmTheme")),
      sprintf(
         "Opening and ending tag mismatch: string line 223 and notstring [76]", 
         inputFileLocation),
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "Malformed3.tmTheme")),
      sprintf(
         "StartTag: invalid element name [68]", 
         inputFileLocation),
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "Malformed4.tmTheme")),
      sprintf(
         "StartTag: invalid element name [68]", 
         inputFileLocation),
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "MissingKeyEnd.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Unable to find a key for the \"string\" element with value \"sRGB\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "MissingKeyMid.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Unable to find a key for the \"string\" element with value \"keyword.operator, constant.other.color\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "MissingKeyStart.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Unable to find a key for the \"string\" element with value \"http://chriskempson.com\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "MissingValueEnd.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Unable to find a value for the key \"colorSpaceName\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "MissingValueMid.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Unable to find a value for the key \"settings\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "MissingValueStart.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Unable to find a value for the key \"comment\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "NoBody.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Expected 1 non-text child of the root, found: 0",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "WrongArrayKey.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Incorrect key for array element. Expected: \"settings\"; Actual: \"notSettings\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "WrongTagEnd.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Encountered unexpected element as a child of the current \"dict\" element: \"other\". Expected \"key\", \"string\", \"array\", or \"dict\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "WrongTagMid.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Encountered unexpected element as a child of the current \"dict\" element: \"something\". Expected \"key\", \"string\", \"array\", or \"dict\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "WrongTagStart.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Encountered unexpected element as a child of the current \"dict\" element: \"a-tag\". Expected \"key\", \"string\", \"array\", or \"dict\".",
      fixed = TRUE)
})