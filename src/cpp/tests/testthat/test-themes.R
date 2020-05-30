#
# test-themes.R
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
#

context("themes")

# We need this file for .rs.parseCss
source(file.path("..", "..", "session", "resources", "themes", "compile-themes.R"))

inputFileLocation <- file.path(path.expand("."), "themes")
tempOutputDir <- file.path(inputFileLocation, "temp")
localInstallDir <- file.path(inputFileLocation, "localInstall")
globalInstallDir <- file.path(inputFileLocation, "globalInstall")
noPermissionDir <- file.path(inputFileLocation, "nopermission")

themes <- list(
   "Active4D" = list("fileName" ="active4d",isDark = FALSE),
   "All Hallow's Eve" = list("fileName" ="all_hallows_eve", isDark = TRUE),
   "Amy" = list("fileName" ="amy", isDark = TRUE),
   "Blackboard" = list("fileName" ="blackboard", isDark = TRUE),
   "Brilliance Black" = list("fileName" ="brilliance_black", isDark = TRUE),
   "Brilliance Dull" = list("fileName" ="brilliance_dull", isDark = TRUE),
   "Chrome DevTools" = list("fileName" ="chrome_dev_tools", isDark = FALSE),
   "Clouds Midnight" = list("fileName" ="clouds_midnight", isDark = TRUE),
   "Clouds" = list("fileName" ="clouds", isDark = FALSE),
   "Cobalt" = list("fileName" ="cobalt", isDark = TRUE),
   "Dawn" = list("fileName" ="dawn", isDark = FALSE),
   "Debug Line" = list("fileName" = "DebugLine", isDark = TRUE),
   "Dreamweaver" = list("fileName" ="dreamweaver", isDark = FALSE),
   "Eiffel" = list("fileName" ="eiffel", isDark = FALSE),
   "Espresso Libre" = list("fileName" ="espresso_libre", isDark = TRUE),
   "GitHub" = list("fileName" ="git_hub", isDark = FALSE),
   "IDLE" = list("fileName" ="idle", isDark = FALSE),
   "idleFingers" = list("fileName" ="idle_fingers", isDark = TRUE),
   "iPlastic" = list("fileName" ="i_plastic", isDark = FALSE),
   "Katzenmilch" = list("fileName" ="katzenmilch", isDark = FALSE),
   "krTheme" = list("fileName" ="kr_theme", isDark = TRUE),
   "Kuroir Theme" = list("fileName" ="kuroir_theme", isDark = FALSE),
   "LAZY" = list("fileName" ="lazy", isDark = FALSE),
   "MagicWB (Amiga)" = list("fileName" ="magic_wb_amiga", isDark = FALSE),
   "Merbivore Soft" = list("fileName" ="merbivore_soft", isDark = TRUE),
   "Merbivore" = list("fileName" ="merbivore", isDark = TRUE),
   "mono industrial" = list("fileName" ="mono_industrial", isDark = TRUE),
   "Monokai" = list("fileName" ="monokai", isDark = TRUE),
   "Pastels on Dark" = list("fileName" ="pastels_on_dark", isDark = TRUE),
   "Slush & Poppies" = list("fileName" ="slush_and_poppies", isDark = FALSE),
   "Solarized (dark)" = list("fileName" ="solarized_dark", isDark = TRUE),
   "Solarized (light)" = list("fileName" ="solarized_light", isDark = FALSE),
   "Sunburst" = list("fileName" ="sunburst", isDark = TRUE),
   "Textmate" = list("fileName" ="textmate", isDark = FALSE),
   "Tomorrow Night - Blue" = list("fileName" ="tomorrow_night_blue", isDark = TRUE),
   "Tomorrow Night - Bright" = list("fileName" ="tomorrow_night_bright", isDark = TRUE),
   "Tomorrow Night - Eighties" = list("fileName" ="tomorrow_night_eighties", isDark = TRUE),
   "Tomorrow Night" = list("fileName" ="tomorrow_night", isDark = TRUE),
   "Tomorrow" = list("fileName" ="tomorrow", isDark = FALSE),
   "Twilight" = list("fileName" ="twilight", isDark = TRUE),
   "Vibrant Ink" = list("fileName" ="vibrant_ink", isDark = TRUE),
   "Xcode default" = list("fileName" ="xcode_default", isDark = FALSE),
   "Zenburnesque" = list("fileName" ="zenburnesque", isDark = TRUE))

defaultThemes <- list(
   "Ambiance" = list("fileName" = "ambiance", isDark =  TRUE),
   "Chaos" = list("fileName" = "chaos", isDark = TRUE),
   "Chrome" = list("fileName" = "chrome", isDark = FALSE),
   "Clouds Midnight" = list("fileName" = "clouds_midnight", isDark = TRUE),
   "Clouds" = list("fileName" = "clouds", isDark = FALSE),
   "Cobalt" = list("fileName" = "cobalt", isDark = TRUE),
   "Crimson Editor" = list("fileName" = "crimson_editor", isDark = FALSE),
   "Dawn" = list("fileName" = "dawn", isDark = FALSE),
   "Dracula" = list("fileName" = "dracula", isDark = TRUE),
   "Dreamweaver" = list("fileName" = "dreamweaver", isDark = FALSE),
   "Eclipse" = list("fileName" = "eclipse", isDark = FALSE),
   "Gob" = list("fileName" = "gob", isDark = TRUE),
   "Idle Fingers" = list("fileName" = "idle_fingers", isDark = TRUE),
   "iPlastic" = list("fileName" = "iplastic", isDark = FALSE),
   "Katzenmilch" = list("fileName" = "katzenmilch", isDark = FALSE),
   "Kr Theme" = list("fileName" = "kr_theme", isDark = TRUE),
   "Material" = list("fileName" = "material", isDark = TRUE),
   "Merbivore" = list("fileName" = "merbivore", isDark = TRUE),
   "Merbivore Soft" = list("fileName" = "merbivore_soft", isDark = TRUE),
   "Mono Industrial" = list("fileName" = "mono_industrial", isDark = TRUE),
   "Monokai" = list("fileName" = "monokai", isDark = TRUE),
   "Pastel On Dark" = list("fileName" = "pastel_on_dark", isDark = TRUE),
   "Solarized Dark" = list("fileName" = "solarized_dark", isDark = TRUE),
   "Solarized Light" = list("fileName" = "solarized_light", isDark = FALSE),
   "SQL Server" = list("fileName" = "sqlserver", isDark = FALSE),
   "Textmate (default)" = list("fileName" = "textmate", isDark = FALSE),
   "Tomorrow Night Blue" = list("fileName" = "tomorrow_night_blue", isDark = TRUE),
   "Tomorrow Night Bright" = list("fileName" = "tomorrow_night_bright", isDark = TRUE),
   "Tomorrow Night 80s" = list("fileName" = "tomorrow_night_eighties", isDark = TRUE),
   "Tomorrow Night" = list("fileName" = "tomorrow_night", isDark = TRUE),
   "Tomorrow" = list("fileName" = "tomorrow", isDark = FALSE),
   "Twilight" = list("fileName" = "twilight", isDark = TRUE),
   "Vibrant Ink" = list("fileName" = "vibrant_ink", isDark = TRUE),
   "Xcode" = list("fileName" = "xcode", isDark = FALSE))

# Helpers ==========================================================================================
compareCss <- function(actual, expected, infoStr, parent = NULL, shouldBreak = FALSE)
{
   browser(expr = shouldBreak)
   equal <- TRUE
   msgStart <- paste0("\n", infoStr, "\nCSS")
   if (!is.null(parent)) msgStart <- paste0("\n", infoStr, "\nElement \"", parent, "\"")

   if (!all(actual %in% expected) || !all(expected %in% actual))
   {
      # Check length
      acLen <- length(actual)
      exLen <- length(expected)
      msg <- sprintf("elements than expected. Actual: %d, Expected: %d", acLen, exLen)
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
            "had %d unexpected elements with names: \n   \"%s\"",
            length(extraNames),
            paste0(extraNames, collapse = "\",\n   \""))
         missingMsg <- sprintf(
            "was missing %d elements with names: \n   \"%s\"",
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
            if (!is.list(acVal)) acVal <- gsub(" +", " ", gsub("^\\s*|\\s*$", "", acVal, perl = TRUE), perl = TRUE)

            if (is.list(acVal) && is.list(exVal))
            {
               equal <- equal && compareCss(acVal, exVal, infoStr, name)
            }
            else if (is.list(acVal) || is.list(exVal))
            {
               msg <- sprintf("value type (%s) does not match expected (%s)", typeof(acVal), typeof(exVal))
               cat(msgStart, msg, "\n")
               equal <- FALSE
            }
            else if (acVal != exVal)
            {
               colorRegex <- "^(#[a-fA-F\\d]{6}|rgba?\\(\\s*\\d{1,3}\\s*(?:,\\s*\\d{1,3}\\s*){2}(?:,\\s*[01](?:\\.\\d+)?)?\\s*\\))$"
               if (grepl(colorRegex, acVal, perl = TRUE) && grepl(colorRegex, exVal, perl = TRUE))
               {
                  acRgb <- .rs.getRgbColor(acVal)
                  exRgb <- .rs.getRgbColor(exVal)
                  if (!identical(acRgb, exRgb))
                  {
                     msg <- sprintf("\"%s\" rule doesn't match. Actual: %s, Expected: %s", name, acVal, exVal)
                     cat(msgStart, msg, "\n")
                     equal <- FALSE
                  }
               }
               else
               {
                  msg <- sprintf("\"%s\" rule doesn't match. Actual: %s, Expected: %s", name, acVal, exVal)
                  cat(msgStart, msg, "\n")
                  equal <- FALSE
               }
            }
         }
      }
   }

   equal
}

getFirstMatchInLines <- function(regex, lines)
{
   matchingLine <- lines[grep(regex, lines, perl = TRUE)[1]]
   regmatches(matchingLine, regexec(regex, matchingLine, perl = TRUE))[[1]][2]
}

getRsThemeName <- function(lines)
{
   getFirstMatchInLines("rs-theme-name\\s*:\\s*([^\\*]+?)\\s*(?:\\*|$)", lines)
}

getRsIsDark <- function(lines)
{
   isDarkStr <- getFirstMatchInLines("rs-theme-is-dark\\s*:\\s*([^\\*]+?)\\s*(?:\\*|$)", lines)
   if(!grepl("^(?:true|false)$", isDarkStr, ignore.case = TRUE))
   {
      stop("Unable to convert isDark value to logical.")
   }
   as.logical(toupper(isDarkStr))
}

test_that_wrapped <- function(desc, FUN, BEFORE_FUN = NULL, AFTER_FUN = NULL)
{
   if (!is.null(BEFORE_FUN))
   {
      BEFORE_FUN()
   }

   test_that(desc, FUN)

   if (!is.null(AFTER_FUN))
   {
      AFTER_FUN()
   }
}

setThemeLocations <- function()
{
   Sys.setenv(
      RS_THEME_GLOBAL_HOME = globalInstallDir,
      RS_THEME_LOCAL_HOME = localInstallDir)
}

unsetThemeLocations <- function()
{
   Sys.unsetenv("RS_THEME_GLOBAL_HOME")
   Sys.unsetenv("RS_THEME_LOCAL_HOME")
}

makeNoPermissionDir <- function()
{
   if (dir.exists(noPermissionDir))
      Sys.chmod(noPermissionDir, mode = "0555")
   else
      dir.create(noPermissionDir, mode = "0555")
}

makeGlobalThemeDir <- function()
{
   if (!dir.exists(globalInstallDir))
      dir.create(globalInstallDir, recursive = TRUE)
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
   negOrTooLarge = "(?:negative|greater than 255)"
   expect_error(
      .rs.getRgbColor("rgb(-300, 455, 1024)"),
      paste0(
         "invalid color supplied: rgb\\(-300, 455, 1024\\). RGB value cannot be ",
         negOrTooLarge))
   expect_error(
      .rs.getRgbColor("rgb(300, -455, 1024)"),
      paste0(
         "invalid color supplied: rgb\\(300, -455, 1024\\). RGB value cannot be ",
         negOrTooLarge))

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
      paste0(
         "invalid color supplied: rgba\\(-300, 455, 1024, 55\\). RGB value cannot be ",
         negOrTooLarge))
   expect_error(
      .rs.getRgbColor("rgba(300, -455, 1024, 55)"),
      paste0(
         "invalid color supplied: rgba\\(300, -455, 1024, 55\\). RGB value cannot be ",
         negOrTooLarge))

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

   # Non-hex hex representation values
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
      "hex representation of RGB values should have the format \"#RRGGBB\", where `RR`, `GG` and `BB` are in [0x00, 0xFF]. Found: #123456ab",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("#FfFfFf0"),
      "hex representation of RGB values should have the format \"#RRGGBB\", where `RR`, `GG` and `BB` are in [0x00, 0xFF]. Found: #FfFfFf0",
      fixed = TRUE)

   # Too few values in rgb/rgba representation
   expect_error(
      .rs.getRgbColor("rgb(1, 10)"),
      "non-hex representation of RGB values should have the format \"rgb(R, G, B)\" or \"rgba(R, G, B, A)\" where `R`, `G`, and `B` are integer values in [0, 255] and `A` is decimal value in [0, 1.0]. Found: rgb(1, 10)",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("rgba(1, 10)"),
      "non-hex representation of RGB values should have the format \"rgb(R, G, B)\" or \"rgba(R, G, B, A)\" where `R`, `G`, and `B` are integer values in [0, 255] and `A` is decimal value in [0, 1.0]. Found: rgba(1, 10)",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("rgb(255)"),
      "non-hex representation of RGB values should have the format \"rgb(R, G, B)\" or \"rgba(R, G, B, A)\" where `R`, `G`, and `B` are integer values in [0, 255] and `A` is decimal value in [0, 1.0]. Found: rgb(255)",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("rgba(255)"),
      "non-hex representation of RGB values should have the format \"rgb(R, G, B)\" or \"rgba(R, G, B, A)\" where `R`, `G`, and `B` are integer values in [0, 255] and `A` is decimal value in [0, 1.0]. Found: rgba(255)",
      fixed = TRUE)

   # Completely wrong format
   expect_error(
      .rs.getRgbColor("86, 154, 214"),
      "supplied color has an invalid format: 86, 154, 214. Expected \"#RRGGBB\", \"rgb(R, G, B) or \"rgba(R, G, B, A)\", where `RR`, `GG` and `BB` are in [0x00, 0xFF], `R`, `G`, and `B` are integer values in [0, 255], and `A` is decimal value in [0, 1.0]",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("Not a color"),
      "supplied color has an invalid format: Not a color. Expected \"#RRGGBB\", \"rgb(R, G, B) or \"rgba(R, G, B, A)\", where `RR`, `GG` and `BB` are in [0x00, 0xFF], `R`, `G`, and `B` are integer values in [0, 255], and `A` is decimal value in [0, 1.0]",
      fixed = TRUE)
   expect_error(
      .rs.getRgbColor("0xaaaaaa"),
      "supplied color has an invalid format: 0xaaaaaa. Expected \"#RRGGBB\", \"rgb(R, G, B) or \"rgba(R, G, B, A)\", where `RR`, `GG` and `BB` are in [0x00, 0xFF], `R`, `G`, and `B` are integer values in [0, 255], and `A` is decimal value in [0, 1.0]",
      fixed = TRUE)
})

# Test mixColors ===================================================================================
test_that("mixColors works correctly", {
   expect_equal(.rs.mixColors("#aa11bb", "rgb(21, 140, 231)", 0.6, 0.2), "#6B279F")
   expect_equal(.rs.mixColors("rgb(0, 0, 0)", "rgb(255, 255, 255)", 0.5), "#808080")
   expect_equal(.rs.mixColors("rgb(0, 0, 0)", "rgb(255, 255, 255)", 0.51), "#7D7D7D")
   expect_equal(.rs.mixColors("rgb(10,10,10)", "rgb(30,30,30)", 1, 1), "#282828")
   expect_equal(.rs.mixColors("rgb(10,10,10)", "rgb(28,230,57)", 1), "#0A0A0A")
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
      "fontStyle" = "underline italic bold",
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
   allValuesEx <- list(
      "text-decoration" = "underline",
      "font-style" = "italic",
      "font-weight" = "bold",
      "color" = "#a12d96",
      "background-color" = "#1ad269")
   noFontEx <- list("color" = "#863021", "background-color" = "#A1A1A1")
   onlyItalicEx <- list("font-style" = "italic")
   underLineBgEx <- list(
      "text-decoration" = "underline",
      "background-color" = "rgba(161, 161, 161, 1.00)")
   nonsenseFontEx <- list("color" = "rgba(26, 43, 60, 0.30)", "background-color" = "#112233")
   nonsenseFont2Ex <- list(
      "text-decoration" = "underline",
      "color" = "#cccccc",
      "background-color" = "#1a2b3c")
   emptyListEx <- list()

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
   tomorrowStyles$invisible =  "#D1D1D1"
   tomorrowStyles$comment = list("color" = "#8E908C")
   tomorrowStyles$variable = list("color" = "#C82829")
   tomorrowStyles$string.regexp = list("color" = "#C82829")
   tomorrowStyles$entity.name.tag = list("color" = "#C82829")
   tomorrowStyles$`entity.other.attribute-name` = list("color" = "#C82829")
   tomorrowStyles$meta.tag = list("color" = "#C82829")
   tomorrowStyles$constant.numeric = list("color" = "#F5871F")
   tomorrowStyles$constant.language = list("color" = "#F5871F")
   tomorrowStyles$support.constant = list("color" = "#F5871F")
   tomorrowStyles$constant.character = list("color" = "#F5871F")
   tomorrowStyles$variable.parameter = list("color" = "#F5871F")
   tomorrowStyles$keyword.other.unit = list("color" = "#F5871F")
   tomorrowStyles$support.type = list("color" = "#C99E00")
   tomorrowStyles$support.class =  list("color" = "#C99E00")
   tomorrowStyles$string = list("color" = "#718C00")
   tomorrowStyles$markup.heading = list("color" = "#718C00")
   tomorrowStyles$keyword.operator = list("color" = "#3E999F")
   tomorrowStyles$entity.name.function = list("color" = "#4271AE")
   tomorrowStyles$support.function = list("color" = "#4271AE")
   tomorrowStyles$keyword = list("color" = "#8959A8")
   tomorrowStyles$storage = list("color" = "#8959A8")
   tomorrowStyles$storage.type = list("color" = "#8959A8")
   tomorrowStyles$invalid = list("color" = "#FFFFFF", "background-color" = "#C82829")
   tomorrowStyles$invalid.deprecated = list("color" = "#FFFFFF", "background-color" = "#8959A8")
   tomorrowStyles$fold = "#4271AE"
   tomorrowStyles$gutterBg = "#FFFFFF"
   tomorrowStyles$gutterFg = "#A6A6A6"
   tomorrowStyles$selected_word_highlight = "border: 1px solid #D6D6D6"
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
   expect_true(
      compareCss(
         .rs.parseCss(tomorrowConverted$theme),
         .rs.parseCss(
            readLines(
               file.path(inputFileLocation, "acecss", "tomorrow.css"),
               encoding = "UTF-8")),
         "Theme: Tomorrow"))
   expect_false(tomorrowConverted$isDark)
})

test_that("convertTmTheme works correctly with parseTmTheme", {
   library("xml2")

   .rs.enumerate(themes, function(key, value) {
      # Actual Results
      actualConverted <- .rs.convertTmTheme(
         .rs.parseTmTheme(
            file.path(
               inputFileLocation,
               "tmThemes",
               paste0(key, ".tmTheme"))))

      # Expected Results
      expectedCss <- .rs.parseCss(
         readLines(file.path(inputFileLocation, "acecss", paste0(value$fileName, ".css")),
         encoding = "UTF-8"))

      infoStr <- paste("Theme:", key)
      expect_true(compareCss(.rs.parseCss(actualConverted$theme), expectedCss, infoStr), info = infoStr)
      expect_equal(actualConverted$isDark, value$isDark, info = infoStr)
   })
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
      "The value of a \"key\" element may not be empty.")
   expect_error(
      .rs.parseKeyElement(noTextNode),
      "The value of a \"key\" element may not be empty.")
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
      "Unable to find a key for the \"string\" element with value \"#8e908c\".")
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
      "Unable to find a key for the current \"dict\" element.",
      fixed = TRUE)
   expect_error(
      .rs.parseDictElement(badDictEl, "settings"),
      "Unable to find a key for the \"string\" element with value \"#FFFFFF\".",
      fixed = TRUE)
   expect_error(
      .rs.parseDictElement(badDictEl2, "settings"),
      "Unable to find a key for the \"string\" element with value \"#4D4D4C\".",
      fixed = TRUE)
   expect_error(
      .rs.parseDictElement(badDictEl3, "settings"),
      "Encountered unexpected element as a child of the current \"dict\" element: \"other\". Expected \"key\", \"string\", \"array\", or \"dict\".",
      fixed = TRUE)
   expect_error(
      .rs.parseDictElement(badDictEl4, "settings"),
      "Encountered unexpected element as a child of the current \"dict\" element: \"bad\". Expected \"key\", \"string\", \"array\", or \"dict\".",
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
      "Unable to find a key for array value.",
      fixed = TRUE)
   expect_error(
      .rs.parseArrayElement(arrayEl, "notSettings"),
      "Incorrect key for array element. Expected: \"settings\"; Actual: \"notSettings\".",
      fixed = TRUE)
   expect_error(
      .rs.parseArrayElement(emptyArrayEl, "settings"),
      "\"array\" element cannot be empty.",
      fixed = TRUE)
   expect_error(
      .rs.parseArrayElement(textArrayEl, "settings"),
      "\"array\" element cannot be empty.",
      fixed = TRUE)
   expect_error(
      .rs.parseArrayElement(badArrayEl, "settings"),
      "Expecting \"dict\" element; found \"bad\".",
      fixed = TRUE)
   expect_error(
      .rs.parseArrayElement(badArrayEl2, "settings"),
      "Expecting \"dict\" element; found \"notGood\".",
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
      "Expected 1 non-text child of the root, found: 0",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "EmptyDictEl.tmTheme")),
      "\"dict\" element cannot be empty.",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "ExtraChildAfter.tmTheme")),
      "Expected 1 non-text child of the root, found: 2",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "ExtraChildBefore.tmTheme")),
      "Expected 1 non-text child of the root, found: 2",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "ExtraChildMid.tmTheme")),
      "Expecting \"dict\" element; found \"otherChild\".",
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
      "Unable to find a key for the \"string\" element with value \"sRGB\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "MissingKeyMid.tmTheme")),
      "Unable to find a key for the \"string\" element with value \"keyword.operator, constant.other.color\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "MissingKeyStart.tmTheme")),
      "Unable to find a key for the \"string\" element with value \"http://chriskempson.com\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "MissingValueEnd.tmTheme")),
      "Unable to find a value for the key \"colorSpaceName\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "MissingValueMid.tmTheme")),
      "Unable to find a value for the key \"settings\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "MissingValueStart.tmTheme")),
      "Unable to find a value for the key \"comment\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "NoBody.tmTheme")),
      "Expected 1 non-text child of the root, found: 0",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "WrongArrayKey.tmTheme")),
      "Incorrect key for array element. Expected: \"settings\"; Actual: \"notSettings\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "WrongTagEnd.tmTheme")),
      "Encountered unexpected element as a child of the current \"dict\" element: \"other\". Expected \"key\", \"string\", \"array\", or \"dict\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "WrongTagMid.tmTheme")),
      "Encountered unexpected element as a child of the current \"dict\" element: \"something\". Expected \"key\", \"string\", \"array\", or \"dict\".",
      fixed = TRUE)
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "WrongTagStart.tmTheme")),
      "Encountered unexpected element as a child of the current \"dict\" element: \"a-tag\". Expected \"key\", \"string\", \"array\", or \"dict\".",
      fixed = TRUE)
})

# Test convertAceTheme =============================================================================
test_that("convertAceTheme works correctly", {
   .rs.enumerate(themes, function(key, value) {
      inputAceFile <- file.path(inputFileLocation, "acecss", paste0(value$fileName, ".css"))
      expectedResultFile <- file.path(inputFileLocation, "rsthemes", paste0(value$fileName, ".rstheme"))

      expected <- readLines(expectedResultFile, encoding = "UTF-8")
      aceActualLines <- readLines(inputAceFile, encoding = "UTF-8")

      actual <- .rs.convertAceTheme(key, aceActualLines, value$isDark)

      # Check the css values
      infoStr = paste0("Theme: ", key)
      expect_true(compareCss(.rs.parseCss(actual), .rs.parseCss(expected), infoStr), info = infoStr)

      # Check the metadata values
      expect_equal(getRsThemeName(actual), getRsThemeName(expected), fixed = TRUE, info = infoStr)
      expect_equal(getRsIsDark(actual), getRsIsDark(expected), fixed = TRUE, info = infoStr)
   })
})

# Test convertTheme ================================================================================
test_that_wrapped("convertTheme works correctly", {
   .rs.enumerate(themes, function(themeName, themeDesc) {
      name <- .rs.convertTheme(
         file.path(inputFileLocation, "tmThemes", paste0(themeName, ".tmTheme")),
         FALSE,
         tempOutputDir,
         FALSE,
         FALSE,
         FALSE)

      actualCssLines <- readLines(
         file.path(tempOutputDir, paste0(themeName, ".rstheme")),
         encoding = "UTF-8")
      expectedCssLines <- readLines(
         file.path(inputFileLocation, "rsthemes", paste0(themeDesc$fileName, ".rstheme")),
         encoding = "UTF-8")

      infoStr <- paste("Theme:", themeName)
      expect_true(compareCss(.rs.parseCss(actualCssLines), .rs.parseCss(expectedCssLines), infoStr), info = infoStr)

      # Check name values
      expectedName <- getRsThemeName(expectedCssLines)
      expect_equal(name, expectedName, info = infoStr)
      expect_equal(getRsThemeName(actualCssLines), expectedName, info = infoStr)

      # Check dark value
      expect_equal(getRsIsDark(actualCssLines), getRsIsDark(expectedCssLines), info = infoStr)
      expect_equal(getRsIsDark(actualCssLines), themeDesc$isDark, info = infoStr)
   })
},
BEFORE_FUN = function() {
   # Make an output location.
   dir.create(tempOutputDir)
},
AFTER_FUN = function() {
   # Clean up the output location.
   if (unlink(tempOutputDir, recursive = TRUE, force = TRUE) != 0)
   {
      warning("Unable to clean up the actual results in: ", tempOutputDir)
   }
})

test_that_wrapped("convertTheme gives error for file permission issues", {
   expect_error(
      suppressWarnings(.rs.convertTheme(
         file.path(inputFileLocation, "tmThemes", paste0(names(themes)[1], ".tmTheme")),
         FALSE,
         file.path(inputFileLocation, "nopermission"),
         FALSE,
         FALSE,
         FALSE)),
      sprintf(
         "Unable to create the theme file in the requested location: %s. Please see above for relevant warnings.",
         file.path(inputFileLocation, "nopermission", paste0(names(themes)[1], ".rstheme"))),
      fixed = TRUE)
},
BEFORE_FUN = makeNoPermissionDir)

# Test addTheme ====================================================================================
test_that_wrapped("addTheme works correctly with local install", {
   themeName <- names(themes)[4]
   themeDesc <- themes[[themeName]]

   fileName <- paste0(themeDesc$fileName, ".rstheme")
   installPath <- file.path(localInstallDir, fileName)

   if (file.exists(installPath))
   {
      skip(
         paste0(
            "Skipping addTheme(",
            themeName,
            ") because it already exists in the local install location."))
   }

   actualName <- .rs.addTheme(
      file.path(inputFileLocation, "rsthemes", fileName),
      FALSE,
      FALSE,
      FALSE)

   infoStr <- paste("Theme:", themeName)
   expect_equal(actualName, themeName, info = infoStr)
   expect_true(file.exists(installPath), info = infoStr)

   actualLines <- readLines(installPath, encoding = "UTF-8")
   expectedLines <- readLines(file.path(inputFileLocation, "rsthemes", fileName), encoding = "UTF-8")
   expect_equal(actualLines, expectedLines, info = infoStr)
},
BEFORE_FUN = setThemeLocations,
AFTER_FUN = function() {
   unsetThemeLocations()
   if (!all(file.remove(file.path(localInstallDir, dir(localInstallDir)))))
   {
      if (length(dir(localInstallDir)) != 0)
      {
         warning(
            "Unable to remove the following files: \"",
            paste0(
               file.path(localInstallDir, dir(localInstallDir)),
               collapse = "\", \""),
            '\"')
      }
   }
})

test_that_wrapped("addTheme works correctly with global install", {
   themeName <- names(themes)[4]
   themeDesc <- themes[[themeName]]

   fileName <- paste0(themeDesc$fileName, ".rstheme")
   installPath <- file.path(globalInstallDir, fileName)

   if (file.exists(installPath))
   {
      skip(
         paste0(
            "Skipping addTheme(",
            themeName,
            ") because it already exists in the local install location."))
   }

   actualName <- .rs.addTheme(
      file.path(inputFileLocation, "rsthemes", fileName),
      FALSE,
      FALSE,
      TRUE)

   infoStr <- paste("Theme:", themeName)
   expect_equal(actualName, themeName, info = infoStr)
   expect_true(file.exists(installPath), info = infoStr)

   actualLines <- readLines(installPath, encoding = "UTF-8")
   expectedLines <- readLines(file.path(inputFileLocation, "rsthemes", fileName), encoding = "UTF-8")
   expect_equal(actualLines, expectedLines, info = infoStr)

   if (file.exists(installPath))
   {
      if (!file.remove(installPath))
      {
         warning(
            "Unable to remove \"",
            installPath,
            "\" from the system. Please check file system permissions.")
      }
   }
},
BEFORE_FUN = function() {
   setThemeLocations()
   makeGlobalThemeDir()
},
AFTER_FUN = function() {
   unsetThemeLocations()
   if (!all(file.remove(dir(globalInstallDir))))
   {
      if (length(dir(globalInstallDir)) != 0)
      {
         warning(
            "Unable to remove the following files: \"",
            paste0(
               file.path(globalInstallDir, dir(globalInstallDir)),
               collapse = "\", \""),
            '\"')
      }
   }
})

test_that("addTheme gives an error when adding an empty theme", {
   themePath <- file.path(inputFileLocation, "rsthemes", "empty.rstheme")
   expect_error(
      .rs.addTheme(themePath, FALSE, FALSE, FALSE),
      paste0("The theme file is empty."))
})

test_that_wrapped("addTheme gives error when the theme already exists", {
   themePath <- file.path(inputFileLocation, "rsthemes", paste0(themes[[40]]$fileName, ".rstheme"))
   .rs.addTheme(themePath, FALSE, FALSE, FALSE)
   expect_error(
      .rs.addTheme(themePath, FALSE, FALSE, FALSE),
      paste0(
         "The specified theme, \"",
         names(themes)[40],
         "\", already exists in the target location. Please delete the existing theme and try again."))
},
BEFORE_FUN = setThemeLocations,
AFTER_FUN = function()
{
   .rs.removeTheme(names(themes)[40], .rs.getThemes())
   unsetThemeLocations()
})

test_that_wrapped("addTheme works correctly with force = TRUE", {
   inputThemePath <- file.path(inputFileLocation, "rsthemes", paste0(themes[[14]]$fileName, ".rstheme"))
   name <- .rs.addTheme(inputThemePath, FALSE, TRUE, FALSE)
   exLines <- readLines(inputThemePath, encoding = "UTF-8")

   installedTheme <- .rs.getThemes()[[tolower(name)]]
   acLines <- readLines(.rs.getThemeDirFromUrl(installedTheme$url), encoding = "UTF-8")

   expect_equal(name, names(themes)[14])
   expect_equal(acLines, exLines)
},
BEFORE_FUN = function() {
   setThemeLocations()
   file.create(file.path(localInstallDir, paste0(themes[[14]]$fileName, ".rstheme")))
},
AFTER_FUN = function() {
   .rs.removeTheme(names(themes)[14], .rs.getThemes())
   unsetThemeLocations()
})

test_that_wrapped("addTheme gives error when permission are bad", {
   expect_error(
      suppressWarnings(.rs.addTheme(
         file.path(inputFileLocation, "rsthemes", paste0(themes[[20]]$fileName, ".rstheme")),
         FALSE,
         FALSE,
         FALSE)),
      message = "Unable to create the theme file. Please check file system permissions.",
      FIXED = TRUE)
},
BEFORE_FUN = function() {
   makeNoPermissionDir()
   Sys.setenv(RS_THEME_LOCAL_HOME = noPermissionDir)
},
AFTER_FUN = unsetThemeLocations)

# Test rs_getThemes ================================================================================
test_that_wrapped("rs_getThemes gets default themes correctly", {
   themeList <- .rs.getThemes()
   expect_equal(length(themeList), length(defaultThemes))
   .rs.enumerate(themeList, function(themeName, themeDetails) {
      infoStr <- paste("Theme:", themeDetails$name)
      expect_true(themeDetails$name %in% names(defaultThemes), info = infoStr)
      expect_equal(themeName, tolower(themeDetails$name))

      expectedTheme <- defaultThemes[[themeDetails$name]]
      expect_equal(
         themeDetails$url,
         paste0("theme/default/", expectedTheme$fileName, ".rstheme"),
         info = infoStr)
      expect_equal(themeDetails$isDark, expectedTheme$isDark, info = infoStr)
   })
},
BEFORE_FUN = setThemeLocations,
AFTER_FUN = unsetThemeLocations)

test_that_wrapped("rs_getThemes works correctly", {
   addedThemes <- list()
   .rs.enumerate(themes, function(themeName, themeDesc) {
      fileName <- paste0(themeDesc$fileName, ".rstheme")
      isGlobal <- (sample.int(2, 1) > 1)
      themeLocation <- if (isGlobal) file.path(globalInstallDir, fileName)
                       else file.path(localInstallDir, fileName)
      file.copy(file.path(inputFileLocation, "rsthemes", fileName), themeLocation)
      addedThemes[[themeName]] <<- if (isGlobal) paste0("theme/custom/global/", fileName)
                                   else paste0("theme/custom/local/", fileName)
      })

   themeList <- .rs.getThemes()
   .rs.enumerate(addedThemes, function(themeName, themeLocation)
   {
      infoStr <- paste("Theme:", themeName)
      expect_true(tolower(themeName) %in% names(themeList), info = infoStr)
      expect_equal(themeList[[tolower(themeName)]]$url, themeLocation, info = infoStr)
      expect_equal(themeList[[tolower(themeName)]]$isDark, themes[[themeName]]$isDark, info = infoStr)
   })
},
BEFORE_FUN = function() {
   setThemeLocations()
   makeGlobalThemeDir()
},
AFTER_FUN = function() {
   toRemove <- c(
      file.path(localInstallDir, dir(localInstallDir)),
      file.path(globalInstallDir, dir(globalInstallDir)))
   foundLen <- length(toRemove)
   removed <- file.remove(toRemove)
   for (i in 1:foundLen)
   {
      if (!removed[i])
      {
         warning(
            "Unable to remove ",
            file.path(path.expand(localInstallDir), toRemove[i]),
            call. = FALSE)
      }
   }
})

test_that_wrapped("rs_getThemes location override works correctly", {
   # Nothing installed
   themeName <- "Dawn"
   dawnTheme <- .rs.getThemes()[[tolower(themeName)]]
   expect_equal(
      dawnTheme$url,
      paste0("theme/default/", defaultThemes[[themeName]]$fileName, ".rstheme"),
      info = "default location")

   # Install globally
   expectedDawn <- themes[[themeName]]
   .rs.addTheme(
      file.path(inputFileLocation, "rsthemes", paste0(expectedDawn$fileName, ".rstheme")),
      FALSE,
      FALSE,
      TRUE)
   dawnTheme <- .rs.getThemes()[[tolower(themeName)]]
   expect_equal(
      dawnTheme$url,
      paste0("theme/custom/global/", expectedDawn$fileName, ".rstheme"),
      info = "global location")

   # Install locally
   expectedDawn <- themes[[themeName]]
   .rs.addTheme(
      file.path(inputFileLocation, "rsthemes", paste0(expectedDawn$fileName, ".rstheme")),
      FALSE,
      FALSE,
      FALSE)
   dawnTheme <- .rs.getThemes()[[tolower(themeName)]]
   expect_equal(
      dawnTheme$url,
      paste0("theme/custom/local/", expectedDawn$fileName, ".rstheme"),
      info = "local location")
},
BEFORE_FUN = function() {
   setThemeLocations()
   makeGlobalThemeDir()
},
AFTER_FUN = function() {
   # Remove the theme from the local & global locations.
   .rs.removeTheme(themeName, .rs.getThemes())
   .rs.removeTheme(themeName, .rs.getThemes())
   unsetThemeLocations()
})

# Test removeTheme =================================================================================
test_that_wrapped("removeTheme works correctly locally", {
   .rs.removeTheme(names(themes)[19], .rs.getThemes())
   expect_false(file.exists(file.path(localInstallDir, paste0(themes[[19]]$fileName, ".rstheme"))))
},
BEFORE_FUN = function() {
   setThemeLocations()
   toAdd <- paste0(themes[[19]]$fileName, ".rstheme")
   file.copy(file.path(inputFileLocation, "rsthemes", toAdd), file.path(localInstallDir, toAdd))
},
AFTER_FUN = function() {
   unsetThemeLocations()
   toRemove <- file.path(localInstallDir, paste0(themes[[19]]$fileName, ".rstheme"))
   if (file.exists(toRemove))
   {
      if (!file.remove(toRemove))
      {
         warning("Unable to remove the file ", toRemove)
      }
   }
})

test_that_wrapped("removeTheme works correctly globally", {
   .rs.removeTheme(names(themes)[22], .rs.getThemes())
   expect_false(file.exists(file.path(globalInstallDir, paste0(themes[[22]]$fileName, ".rstheme"))))
},
BEFORE_FUN = function() {
   setThemeLocations()
   makeGlobalThemeDir()
   toAdd <- paste0(themes[[22]]$fileName, ".rstheme")
   file.copy(file.path(inputFileLocation, "rsthemes", toAdd), file.path(globalInstallDir, toAdd))
},
AFTER_FUN = function() {
   unsetThemeLocations()
   toRemove <- file.path(globalInstallDir, paste0(themes[[22]]$fileName, ".rstheme"))
   if (file.exists(toRemove))
   {
      if (!file.remove(toRemove))
      {
         warning("Unable to remove the file ", toRemove)
      }
   }
})

test_that_wrapped("removeTheme gives correct error when unable to remove locally", {
   expect_error(
      suppressWarnings(.rs.removeTheme(names(themes)[5], .rs.getThemes())),
      "Please check your file system permissions.",
      fixed = TRUE)
},
BEFORE_FUN = function() {
   makeNoPermissionDir()
   Sys.setenv(RS_THEME_LOCAL_HOME = noPermissionDir)
   Sys.chmod(noPermissionDir, mode = "0777")
   .rs.addTheme(
      file.path(inputFileLocation, "rsthemes", paste0(themes[[5]]$fileName, ".rstheme")),
      FALSE,
      FALSE,
      FALSE)
   Sys.chmod(noPermissionDir, mode = "0555")
   Sys.chmod(dir(noPermissionDir, full.names = TRUE, all.files = TRUE), mode = "0555")
},
AFTER_FUN = function() {
   unsetThemeLocations()
   Sys.chmod(dir(noPermissionDir, full.names = TRUE, all.files = TRUE), mode = "0777")
   file.remove(file.path(noPermissionDir, paste0(themes[[5]]$fileName, ".rstheme")))
})

test_that_wrapped("removeTheme gives correct error when unable to remove globally", {
   expect_error(
      suppressWarnings(.rs.removeTheme(names(themes)[32], .rs.getThemes())),
      "The specified theme is installed for all users. Please contact your system administrator to remove the theme.",
      fixed = TRUE)
},
BEFORE_FUN = function() {
   makeNoPermissionDir()
   Sys.setenv(RS_THEME_GLOBAL_HOME = noPermissionDir)
   Sys.chmod(noPermissionDir, mode = "0777")
   .rs.addTheme(
      file.path(inputFileLocation, "rsthemes", paste0(themes[[32]]$fileName, ".rstheme")),
      FALSE,
      FALSE,
      TRUE)
   Sys.chmod(dir(noPermissionDir, full.names = TRUE, all.files = TRUE), mode = "0555")
},
AFTER_FUN = function() {
   unsetThemeLocations()
   Sys.chmod(dir(noPermissionDir, full.names = TRUE, all.files = TRUE), mode = "0777")
   file.remove(file.path(noPermissionDir, paste0(themes[[32]]$fileName, ".rstheme")))
})
