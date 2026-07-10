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

# tools::parse_Rd() loads user-macro definitions from R's system.Rd by
# default, but minimal R installations can ship without that file, turning
# parsing into an error. The Rd fixtures in these tests use only base Rd
# tags, so when system.Rd is missing, fall back to a structurally-valid but
# empty user-macro set loaded from an empty file; this parses identically.
parseRd <- function(file) {
    macros <- file.path(R.home("share"), "Rd", "macros", "system.Rd")
    if (!file.exists(macros)) {
        macros <- tempfile(fileext = ".Rd")
        file.create(macros)
        on.exit(unlink(macros), add = TRUE)
    }
    tools::parse_Rd(file, macros = macros)
}

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
    expect_true(.rs.exampleCodeLaunchesApp("learnr::run_tutorial(\"hello\")"))

    # every launcher in the detection list, bare and namespace-qualified
    launchers <- c(
        "runApp", "runExample", "runGadget",
        "runUrl", "runGist", "runGitHub",
        "shinyApp", "shinyAppDir", "shinyAppFile",
        "run_tutorial"
    )

    for (launcher in launchers) {
        expect_true(.rs.exampleCodeLaunchesApp(sprintf("%s(x)", launcher)), info = launcher)
        expect_true(.rs.exampleCodeLaunchesApp(sprintf("pkg::%s(x)", launcher)), info = launcher)
    }
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

test_that(".rs.helpExampleDivertCommand() handles examples", {
    # build a fake installed package with a help database, in a temporary
    # library; this mirrors what R CMD INSTALL produces (an Rd lazy-load db
    # plus an aliases index), which is what example(give.lines = TRUE) reads
    lib <- tempfile("rstudio-test-lib-")
    pkg <- file.path(lib, "fakehelppkg")
    dir.create(file.path(pkg, "help"), recursive = TRUE)
    on.exit(unlink(lib, recursive = TRUE), add = TRUE)

    writeLines(
        c("Package: fakehelppkg", "Version: 0.1.0"),
        file.path(pkg, "DESCRIPTION")
    )

    blockingRd <- tempfile("blocking-", fileext = ".Rd")
    writeLines(
        c(
            "\\name{blocking}",
            "\\alias{blocking}",
            "\\alias{it's \"blocking\"}",
            "\\title{Blocking}",
            "\\description{Launches a Shiny app.}",
            "\\examples{",
            "ui <- shiny::fluidPage()",
            "server <- function(input, output, session) {}",
            "shiny::runApp(shiny::shinyApp(ui, server))",
            "}"
        ),
        blockingRd
    )

    benignRd <- tempfile("benign-", fileext = ".Rd")
    writeLines(
        c(
            "\\name{benign}",
            "\\alias{benign}",
            "\\title{Benign}",
            "\\description{Just plots.}",
            "\\examples{",
            "plot(1:10)",
            "}"
        ),
        benignRd
    )

    db <- list(
        blocking = parseRd(blockingRd),
        benign = parseRd(benignRd)
    )
    tools:::makeLazyLoadDB(db, file.path(pkg, "help", "fakehelppkg"))

    aliases <- c(
        "blocking" = "blocking",
        "it's \"blocking\"" = "blocking",
        "benign" = "benign"
    )
    saveRDS(aliases, file.path(pkg, "help", "aliases.rds"))

    libPaths <- .libPaths()
    .libPaths(c(lib, libPaths))
    on.exit(.libPaths(libPaths), add = TRUE)

    # an example that launches an app is diverted to the console
    expect_identical(
        .rs.helpExampleDivertCommand("Example", "fakehelppkg", "blocking"),
        "example(\"blocking\", package = \"fakehelppkg\")"
    )

    # topics needing escaping survive the round-trip through deparse()
    expect_identical(
        .rs.helpExampleDivertCommand("Example", "fakehelppkg", "it's \"blocking\""),
        "example(\"it's \\\"blocking\\\"\", package = \"fakehelppkg\")"
    )

    # a benign example, and a missing one, are not diverted
    expect_null(.rs.helpExampleDivertCommand("Example", "fakehelppkg", "benign"))
    expect_null(.rs.helpExampleDivertCommand("Example", "fakehelppkg", "nosuchtopic"))
})

test_that(".rs.helpExampleDivertCommand() ignores launchers inside dontrun blocks", {
    # \dontrun{} code is commented out by example(give.lines = TRUE), exactly
    # as it would be in the diverted example() call, so it must not divert
    lib <- tempfile("rstudio-test-lib-")
    pkg <- file.path(lib, "fakedontrunpkg")
    dir.create(file.path(pkg, "help"), recursive = TRUE)
    on.exit(unlink(lib, recursive = TRUE), add = TRUE)

    writeLines(
        c("Package: fakedontrunpkg", "Version: 0.1.0"),
        file.path(pkg, "DESCRIPTION")
    )

    dontrunRd <- tempfile("dontrun-", fileext = ".Rd")
    writeLines(
        c(
            "\\name{dontrun}",
            "\\alias{dontrun}",
            "\\title{Dontrun}",
            "\\description{Launches an app, but only via dontrun.}",
            "\\examples{",
            "x <- 1:10",
            "\\dontrun{",
            "shiny::runApp(app)",
            "}",
            "}"
        ),
        dontrunRd
    )

    db <- list(dontrun = parseRd(dontrunRd))
    tools:::makeLazyLoadDB(db, file.path(pkg, "help", "fakedontrunpkg"))
    saveRDS(c("dontrun" = "dontrun"), file.path(pkg, "help", "aliases.rds"))

    libPaths <- .libPaths()
    .libPaths(c(lib, libPaths))
    on.exit(.libPaths(libPaths), add = TRUE)

    expect_null(.rs.helpExampleDivertCommand("Example", "fakedontrunpkg", "dontrun"))
})
