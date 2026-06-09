#
# test-help.R
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

library(testthat)

context("help")

test_that(".rs.getHelp() handles empty and NULL package", {
    help_rnorm1 <- .rs.getHelp("rnorm")
    help_rnorm2 <- .rs.getHelp("rnorm", package = "")
    help_rnorm3 <- .rs.getHelp("rnorm", package = NULL)
    help_rnorm4 <- .rs.getHelp("rnorm", package = "stats")
    help_rnorm5 <- .rs.getHelp("stats::rnorm")
    help_rnorm6 <- .rs.getHelp("rnorm", "package:stats")

    expect_identical(help_rnorm1, help_rnorm2)
    expect_identical(help_rnorm1, help_rnorm3)
    expect_identical(help_rnorm1, help_rnorm4)
    expect_identical(help_rnorm1, help_rnorm5)
    expect_identical(help_rnorm1, help_rnorm6)
})

test_that(".rs.exampleCodeLaunchesApp() detects blocking application launchers", {
    shiny_code <- c(
        "captureVals <- function() {",
        "  ui <- shiny::fluidPage(shiny::textInput(\"the_text\", \"Enter text\"))",
        "  server <- function(input, output, session) {}",
        "  shiny::runApp(shiny::shinyApp(ui, server))",
        "}",
        "captureVals()"
    )

    expect_true(.rs.exampleCodeLaunchesApp(shiny_code))
    expect_true(.rs.exampleCodeLaunchesApp("shiny::runApp(app)"))
    expect_true(.rs.exampleCodeLaunchesApp("runGadget(myGadget())"))
    expect_true(.rs.exampleCodeLaunchesApp("learnr::run_tutorial(\"hello\")"))
})

test_that(".rs.exampleCodeLaunchesApp() ignores launcher names in comments and strings", {
    # mentions runApp, but never actually calls it
    benign_code <- c(
        "# call shiny::runApp() to launch the app yourself",
        "x <- 1:10",
        "msg <- \"use runApp() to start\"",
        "mean(x)"
    )

    expect_false(.rs.exampleCodeLaunchesApp(benign_code))
    expect_false(.rs.exampleCodeLaunchesApp("plot(1:10)"))

    # unparseable input should never error -- it just isn't diverted
    expect_false(.rs.exampleCodeLaunchesApp("this is not valid R code ("))
})

test_that(".rs.helpExampleDivertCommand() handles demos", {
    # build a fake installed package with demos, in a temporary library
    lib <- tempfile("rstudio-test-lib-")
    pkg <- file.path(lib, "fakepkg")
    dir.create(file.path(pkg, "demo"), recursive = TRUE)
    on.exit(unlink(lib, recursive = TRUE), add = TRUE)

    writeLines(
        c("Package: fakepkg", "Version: 0.1.0"),
        file.path(pkg, "DESCRIPTION")
    )

    writeLines(
        c(
            "ui <- shiny::fluidPage()",
            "server <- function(input, output, session) {}",
            "shiny::runApp(shiny::shinyApp(ui, server))"
        ),
        file.path(pkg, "demo", "blocking.R")
    )

    writeLines("plot(1:10)", file.path(pkg, "demo", "benign.R"))

    libPaths <- .libPaths()
    .libPaths(c(lib, libPaths))
    on.exit(.libPaths(libPaths), add = TRUE)

    # a demo that launches an app is diverted to the console
    expect_identical(
        .rs.helpExampleDivertCommand("Demo", "fakepkg", "blocking"),
        "demo(\"blocking\", package = \"fakepkg\")"
    )

    # a benign demo, and a missing one, are not diverted
    expect_null(.rs.helpExampleDivertCommand("Demo", "fakepkg", "benign"))
    expect_null(.rs.helpExampleDivertCommand("Demo", "fakepkg", "nosuchdemo"))
})
