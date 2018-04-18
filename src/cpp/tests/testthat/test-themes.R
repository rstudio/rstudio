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
   addChildren(recursiveDictEl, kids = list(simpleDictEl2))
   
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
   addChildren(recursiveDictEl2, kids = list(simpleDictEl))
   
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
   
   # Test Cases (errors)
   expect_error(
      .rs.parseDictElement(simpleDictEl, NULL),
      "Unable to convert the tmtheme to an rstheme. Unable to find a key for the current \"dict\" element.")
   expect_error(
      .rs.parseDictElement(badDictEl, "settings"),
      "Unable to convert the tmtheme to an rstheme. Unable to find a key for the \"string\" element with value \"#FFFFFF\".")
   expect_error(
      .rs.parseDictElement(badDictEl2, "settings"),
      "Unable to convert the tmtheme to an rstheme. Unable to find a key for the \"string\" element with value \"#4D4D4C\".")
   expect_error(
      .rs.parseDictElement(badDictEl3, "settings"),
      "Unable to convert the tmtheme to an rstheme. Encountered unexpected element as a child of the current \"dict\" element: \"other\". Expected \"key\", \"string\", or \"dict\".")
   expect_error(
      .rs.parseDictElement(badDictEl4, "settings"),
      "Unable to convert the tmtheme to an rstheme. Encountered unexpected element as a child of the current \"dict\" element: \"bad\". Expected \"key\", \"string\", or \"dict\".")
   expect_error(
      .rs.parseDictElement(emptyDictEl, "settings"),
      "Unable to convert the tmtheme to an rstheme. \"dict\" element cannot be empty.")
})