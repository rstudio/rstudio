#
# test-pkg-deps.R
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

context("package dependencies")

test_that("package dependencies are discovered in R scripts", {
   contents <- paste(
      "library(ggplot2)",
      "require(dplyr)",
      "",
      "# Digest something",
      "digest::digest(something)", sep = "\n")
   packages <- .rs.parsePackageDependencies(contents, ".R")
   expect_equal(sort(packages), sort(c("ggplot2", "dplyr", "digest")))
})

test_that("package dependencies are discovered in R Markdown documents", {
   contents <- paste(
      "# This is a markdown header",
      "",
      "And here's a paragraph. Now, how about a code chunk?",
      "",
      "```{r}",
      "require(caret)",
      "",
      "# Knit something",
      "knitr::knit(something)", 
      "```",
      "", sep = "\n")
   packages <- .rs.parsePackageDependencies(contents, ".Rmd")
   expect_equal(sort(packages), sort(c("caret", "knitr")))
})


test_that("package dependencies are discovered in R Markdown YAML headers", {

   # simple YAML header naming a package
   contents <- paste(
      "---",
      "title: Test Doc 1",
      "output: flexdashboard::flex_dashboard",
      "---",
      "", sep = "\n")
   packages <- .rs.parsePackageDependencies(contents, ".Rmd")
   expect_equal(packages, "flexdashboard")

   # more complicated YAML header in which package name is used as a key
   contents <- paste(
      "---",
      "title: Test Doc 2",
      "output:",
      "  flexdashboard::flex_dashboard:",
      "    orientation: columns",
      "---",
      "", sep = "\n")
   packages <- .rs.parsePackageDependencies(contents, ".Rmd")
   expect_equal(packages, "flexdashboard")

   # runtime: shiny requires the shiny package
   contents <- paste(
      "---",
      "title: Test Doc 3",
      "output: flexdashboard::flex_dashboard",
      "runtime: shiny",
      "---",
      "", sep = "\n")
   packages <- .rs.parsePackageDependencies(contents, ".Rmd")
   expect_equal(sort(packages), sort(c("flexdashboard", "shiny")))

   # parameterized R Markdown also requires the shiny package (for parameter prompts/deployment)
   contents <- paste(
      "---",
      "title: Test Doc 4",
      "output: html_document",
      "params:",
      "  data: \"hawaii\"",
      "---",
      "", sep = "\n")
   packages <- .rs.parsePackageDependencies(contents, ".Rmd")
   expect_equal(packages, "shiny")
})

test_that("R scripts do not get treated like R Markdown docs", {
   # ensures that the --- YAML delimiters don't cause us to try to parse YAML inside R scripts
   contents <- paste(
      "require(ggplot2)",
      "require(yaml)",
      "",
      "# ---",
      "# This is a comment header: a long one, too.",
      "x <- 1",
      "y <- 2",
      "# ---",
      "",
      "", sep = "\n")
   packages <- .rs.parsePackageDependencies(contents, ".R")
   expect_equal(sort(packages), sort(c("ggplot2", "yaml")))
})

test_that("HTML comments are not treated like YAML delimiters", {
   contents <- paste(
      "",
      "<!--- html comment --->",
      "",
      "```{r}",
      "require(callr)",
      "require(crayon)",
      "```",
      "", sep = "\n")
   packages <- .rs.parsePackageDependencies(contents, ".Rmd")
   expect_equal(sort(packages), sort(c("callr", "crayon")))
})

