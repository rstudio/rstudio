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

inputFileLocation <- file.path(path.expand("~"), "GitRepos", "rstudio", "src", "cpp", "tests", "testthat", "themes")

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
test_that("convertTmTheme works correctly", {
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
   
   # expected results
   tomorrowCss <- .rs.convertTmTheme(tomorrowTmTheme)
})

# Test countXmlChildren ============================================================================
test_that("countXmlChildren works correctly", {
   # Set up objects for the test case
   emptyNode <- XML::newXMLNode("empty")
   textNode <- XML::newXMLNode("string", "value")
   oneChild <- XML::newXMLNode("one", .children = list(XML::newXMLNode("child")))
   oneChildAndText <- XML::newXMLNode("one", "value", .children = list(XML::newXMLNode("child")))
   nineChildren <- XML::newXMLNode(
      "nine",
      .children = list(
         XML::newXMLNode("child1"),
         XML::newXMLNode("child2"),
         XML::newXMLNode("child3"),
         XML::newXMLNode("child4"),
         XML::newXMLNode("child5"),
         XML::newXMLNode("child6"),
         XML::newXMLNode("child7"),
         XML::newXMLNode("child8"),
         XML::newXMLNode("child9")))

   # Test cases
   expect_equal(.rs.countXmlChildren(emptyNode), 0)
   expect_equal(.rs.countXmlChildren(textNode), 0)
   expect_equal(.rs.countXmlChildren(oneChild), 1)
   expect_equal(.rs.countXmlChildren(oneChildAndText), 1)
   expect_equal(.rs.countXmlChildren(nineChildren), 9)
})

# Test parseKeyElement =============================================================================
test_that("parseKeyElement works correctly", {
   library("XML")

   # Setup objects for the test cases
   settingsNode <- XML::newXMLNode("key", "settings")
   valueNode <- XML::newXMLNode("key", "VALUE")
   emptyNode <- XML::newXMLNode("key", "")
   noTextNode <- XML::newXMLNode("key")

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
   library("XML")

   # Setup objects for the test cases
   nameNode <- XML::newXMLNode("string", "Tomorrow")
   colorNode <- XML::newXMLNode("string", "#8e908c")
   emptyNode <- XML::newXMLNode("string", "")
   noTextNode <- XML::newXMLNode("string")

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
   library("XML")

   # Setup input objects for the test cases
   simpleDictEl <- XML::newXMLNode("dict")
   XML::newXMLNode("key", "fontStyle", parent = simpleDictEl)
   XML::newXMLNode("string", parent = simpleDictEl)
   XML::newXMLNode("key", "foreground", parent = simpleDictEl)
   XML::newXMLNode("string", "#F5871F", parent = simpleDictEl)

   simpleDictEl2 <- XML::newXMLNode("dict")
   XML::newXMLNode("key", "background", parent = simpleDictEl2)
   XML::newXMLNode("string", "#FFFFFF", parent = simpleDictEl2)
   XML::newXMLNode("key", "caret", parent = simpleDictEl2)
   XML::newXMLNode("string", "#AEAFAD", parent = simpleDictEl2)
   XML::newXMLNode("key", "foreground", parent = simpleDictEl2)
   XML::newXMLNode("string", "#4D4D4C", parent = simpleDictEl2)
   XML::newXMLNode("key", "invisibles", parent = simpleDictEl2)
   XML::newXMLNode("string", "#D1D1D1", parent = simpleDictEl2)
   XML::newXMLNode("key", "lineHighlight", parent = simpleDictEl2)
   XML::newXMLNode("string", "#EFEFEF", parent = simpleDictEl2)
   XML::newXMLNode("key", "selection", parent = simpleDictEl2)
   XML::newXMLNode("string", "#D6D6D6", parent = simpleDictEl2)

   badDictEl <- XML::newXMLNode("dict")
   XML::newXMLNode("key", "caret", parent = badDictEl)
   XML::newXMLNode("string", "#AEAFAD", parent = badDictEl)
   XML::newXMLNode("string", "#FFFFFF", parent = badDictEl)

   badDictEl2 <- XML::newXMLNode("dict")
   XML::newXMLNode("string", "#4D4D4C", parent = badDictEl2)
   XML::newXMLNode("key", "caret", parent = badDictEl2)
   XML::newXMLNode("string", "#AEAFAD", parent = badDictEl2)

   badDictEl3 <- XML::newXMLNode("dict")
   XML::newXMLNode("key", "caret", parent = badDictEl3)
   XML::newXMLNode("string", "#AEAFAD", parent = badDictEl3)
   XML::newXMLNode("other", "#000000", parent = badDictEl3)

   badDictEl4 <- XML::newXMLNode("dict")
   XML::newXMLNode("bad", "#1D1D1D", parent = badDictEl4)
   XML::newXMLNode("key", "caret", parent = badDictEl4)
   XML::newXMLNode("string", "#AEAFAD", parent = badDictEl4)

   emptyDictEl <- XML::newXMLNode("dict")

   recursiveDictEl <- XML::newXMLNode("dict")
   XML::newXMLNode("key", "settings", parent = recursiveDictEl)
   XML::addChildren(recursiveDictEl, kids = list(simpleDictEl2))

   recursiveDictEl2 <- XML::newXMLNode("dict")
   XML::newXMLNode("key", "name", parent = recursiveDictEl2)
   XML::newXMLNode(
      "string",
      "Number, Constant, Function Argument, Tag Attribute, Embedded",
      parent = recursiveDictEl2)
   XML::newXMLNode("key", "scope", parent = recursiveDictEl2)
   XML::newXMLNode(
      "string",
      "constant.numeric, constant.language, support.constant, constant.character, variable.parameter, punctuation.section.embedded, keyword.other.unit",
      parent = recursiveDictEl2)
   XML::newXMLNode("key", "settings", parent = recursiveDictEl2)
   XML::addChildren(recursiveDictEl2, kids = list(simpleDictEl))

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

   # Setup test case input objects
   arrayEl <- XML::newXMLNode("array")
   emptyArrayEl <- XML::newXMLNode("array")
   textArrayEl <- XML::newXMLNode("array", "some text")
   badArrayEl <- XML::newXMLNode("array")
   badArrayEl2 <- XML::newXMLNode("array")

   # Setup standard expected array input
   recursiveDictEl <- XML::newXMLNode("dict", parent = arrayEl)
   XML::newXMLNode("key", "settings", parent = recursiveDictEl)

   recursiveDictEl2 <- XML::newXMLNode("dict", parent = arrayEl)
   XML::newXMLNode("key", "name", parent = recursiveDictEl2)
   XML::newXMLNode(
      "string",
      "Number, Constant, Function Argument, Tag Attribute, Embedded",
      parent = recursiveDictEl2)
   XML::newXMLNode("key", "scope", parent = recursiveDictEl2)
   XML::newXMLNode(
      "string",
      "constant.numeric, constant.language, support.constant, constant.character, variable.parameter, punctuation.section.embedded, keyword.other.unit",
      parent = recursiveDictEl2)
   XML::newXMLNode("key", "settings", parent = recursiveDictEl2)

   simpleDictEl <- XML::newXMLNode("dict", parent = recursiveDictEl2)
   XML::newXMLNode("key", "fontStyle", parent = simpleDictEl)
   XML::newXMLNode("string", parent = simpleDictEl)
   XML::newXMLNode("key", "foreground", parent = simpleDictEl)
   XML::newXMLNode("string", "#F5871F", parent = simpleDictEl)

   simpleDictEl2 <- XML::newXMLNode("dict", parent = recursiveDictEl)
   XML::newXMLNode("key", "background", parent = simpleDictEl2)
   XML::newXMLNode("string", "#FFFFFF", parent = simpleDictEl2)
   XML::newXMLNode("key", "caret", parent = simpleDictEl2)
   XML::newXMLNode("string", "#AEAFAD", parent = simpleDictEl2)
   XML::newXMLNode("key", "foreground", parent = simpleDictEl2)
   XML::newXMLNode("string", "#4D4D4C", parent = simpleDictEl2)
   XML::newXMLNode("key", "invisibles", parent = simpleDictEl2)
   XML::newXMLNode("string", "#D1D1D1", parent = simpleDictEl2)
   XML::newXMLNode("key", "lineHighlight", parent = simpleDictEl2)
   XML::newXMLNode("string", "#EFEFEF", parent = simpleDictEl2)
   XML::newXMLNode("key", "selection", parent = simpleDictEl2)
   XML::newXMLNode("string", "#D6D6D6", parent = simpleDictEl2)

   # Setup error array input
   XML::addChildren(badArrayEl, XML::xmlClone(simpleDictEl))
   XML::newXMLNode("bad", parent = badArrayEl)

   XML::newXMLNode("notGood", parent = badArrayEl2)
   XML::addChildren(badArrayEl2, XML::xmlClone(simpleDictEl))

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
      "Unable to convert the tmtheme to an rstheme. Unable to find a key for array value.")
   expect_error(
      .rs.parseArrayElement(arrayEl, "notSettings"),
      "Unable to convert the tmtheme to an rstheme. Incorrect key for array element. Expected: \"settings\"; Actual: \"notSettings\".")
   expect_error(
      .rs.parseArrayElement(emptyArrayEl, "settings"),
      "Unable to convert the tmtheme to an rstheme. \"array\" element cannot be empty.")
   expect_error(
      .rs.parseArrayElement(textArrayEl, "settings"),
      "Unable to convert the tmtheme to an rstheme. \"array\" element cannot be empty.")
   expect_error(
      .rs.parseArrayElement(badArrayEl, "settings"),
      "Unable to convert the tmtheme to an rstheme. Expecting \"dict\" element; found \"bad\".")
   expect_error(
      .rs.parseArrayElement(badArrayEl2, "settings"),
      "Unable to convert the tmtheme to an rstheme. Expecting \"dict\" element; found \"notGood\".")
})

# Test parseTmTheme ================================================================================
test_that("parseTmTheme handles correct input", {

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
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "EmptyBody.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Expected 1 non-text child of the root, found: 0")
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "EmptyDictEl.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. \"dict\" element cannot be empty.")
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "ExtraChildAfter.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Expected 1 non-text child of the root, found: 2")
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "ExtraChildBefore.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Expected 1 non-text child of the root, found: 2")
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "ExtraChildMid.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Expecting \"dict\" element; found \"otherChild\".")
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "Malformed1.tmTheme")),
      sprintf(
         "Unable to convert the tmtheme to an rstheme. An error occurred while parsing %s/errorthemes/Malformed1.tmTheme at line 14: error parsing attribute name\n", 
         inputFileLocation))
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "Malformed2.tmTheme")),
      sprintf(
         "Unable to convert the tmtheme to an rstheme. An error occurred while parsing %s/errorthemes/Malformed2.tmTheme at line 223: Opening and ending tag mismatch: string line 223 and notstring\n", 
         inputFileLocation))
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "Malformed3.tmTheme")),
      sprintf(
         "Unable to convert the tmtheme to an rstheme. An error occurred while parsing %s/errorthemes/Malformed3.tmTheme at line 9: StartTag: invalid element name\n", 
         inputFileLocation))
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "Malformed4.tmTheme")),
      sprintf(
         "Unable to convert the tmtheme to an rstheme. An error occurred while parsing %s/errorthemes/Malformed4.tmTheme at line 2: StartTag: invalid element name\n", 
         inputFileLocation))
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "MissingKeyEnd.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Unable to find a key for the \"string\" element with value \"sRGB\".")
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "MissingKeyMid.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Unable to find a key for the \"string\" element with value \"keyword.operator, constant.other.color\".")
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "MissingKeyStart.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Unable to find a key for the \"string\" element with value \"http://chriskempson.com\".")
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "MissingValueEnd.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Unable to find a value for the key \"colorSpaceName\".")
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "MissingValueMid.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Unable to find a value for the key \"settings\".")
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "MissingValueStart.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Unable to find a value for the key \"comment\".")
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "NoBody.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Expected 1 non-text child of the root, found: 0")
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "WrongArrayKey.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Incorrect key for array element. Expected: \"settings\"; Actual: \"notSettings\".")
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "WrongTagEnd.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Encountered unexpected element as a child of the current \"dict\" element: \"other\". Expected \"key\", \"string\", \"array\", or \"dict\".")
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "WrongTagMid.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Encountered unexpected element as a child of the current \"dict\" element: \"something\". Expected \"key\", \"string\", \"array\", or \"dict\".")
   expect_error(
      .rs.parseTmTheme(file.path(inputFileLocation, "errorthemes", "WrongTagStart.tmTheme")),
      "Unable to convert the tmtheme to an rstheme. Encountered unexpected element as a child of the current \"dict\" element: \"a-tag\". Expected \"key\", \"string\", \"array\", or \"dict\".")
})
