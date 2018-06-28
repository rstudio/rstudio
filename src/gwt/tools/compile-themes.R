#!/usr/bin/env Rscript

if (!require("highlight", quietly = TRUE)) {
   install.packages("highlight")
   if (!require("highlight")) {
      stop("This script requires 'highlight' in order to run!")
   }
}

if (!"tools:rstudio" %in% search())
{
   # Needed to source the file below.
   .rs.Env <- attach(NULL, name="tools:rstudio")
   # add a function to the tools:rstudio environment
   assign( envir = .rs.Env, ".rs.addFunction", function(
      name, FN, attrs = list())
   { 
      fullName = paste(".rs.", name, sep="")
      for (attrib in names(attrs))
         attr(FN, attrib) <- attrs[[attrib]]
      assign(fullName, FN, .rs.Env)
      environment(.rs.Env[[fullName]]) <- .rs.Env
   })
}

source(file.path("..", "..", "cpp", "session", "resources", "themes", "compile-themes.R"))

## A set of operator colors to use, for each theme. Should match the name
## of the theme file in ace.
operator_theme_map <- list(
   "solarized_light" = "#93A1A1",
   "solarized_dark" = "#B58900",
   "twilight" = "#7587A6",
   "idle_fingers" = "#6892B2",
   "clouds_midnight" = "#A53553",
   "cobalt" = "#BED6FF",
   "kr_theme" = "#A56464",
   NULL
)

## Similarly, colors for keywords that we might override.
keyword_theme_map <- list(
   "eclipse" = "#800080",
   "clouds" = "#800080",
   NULL
)

chunk_bg_proportion_map <- list(
   "tomorrow_night_bright" = 0.85
)

applyFixups <- function(content, fileName, parsed) {
   
   methodName <- paste("applyFixups", fileName, sep = ".")
   method <- try(get(methodName), silent = TRUE)
   if (inherits(method, "try-error"))
      return(content)
   
   method(content, parsed)
}

applyFixups.ambiance <- function(content, parsed) {
   
   aceCursorLayerLoc <- grep("^\\s*\\.ace_cursor-layer\\s*{", content, perl = TRUE)
   nextBraceLoc <- findNext("}", content, aceCursorLayerLoc)
   
   content[aceCursorLayerLoc:nextBraceLoc] <- ""
   
   content
}

applyFixups.cobalt <- function(content, parsed) {
   content <- tools::setPrintMarginColor(content, "#246")
   content
}

applyFixups.clouds_midnight <- function(content, parsed) {
   content <- tools::setPrintMarginColor(content, "#333")
   content
}

applyFixups.idle_fingers <- function(content, parsed) {
   content <- tools::setPrintMarginColor(content, "#555")
   content
}

applyFixups.kr_theme <- function(content, parsed) {
   content <- tools::setPrintMarginColor(content, "#333")
   content
}

applyFixups.merbivore_soft <- applyFixups.kr_theme
applyFixups.pastel_on_dark <- applyFixups.kr_theme

applyFixups.tomorrow_night_blue <- applyFixups.kr_theme
applyFixups.tomorrow_night_bright <- applyFixups.kr_theme

applyFixups.tomorrow_night_eighties <- function(content, parsed) {
   content <- tools::setPrintMarginColor(content, "#444")
   content
}
applyFixups.tomorrow_night <- applyFixups.tomorrow_night_eighties

applyFixups.twilight <- function(content, parsed) {
   content <- tools::setPrintMarginColor(content, "#333")
   content
}

applyFixups.vibrant_ink <- applyFixups.tomorrow_night_eighties

## Get the set of all theme .css files
outDir <- "../src/org/rstudio/studio/client/workbench/views/source/editors/text/themes"
themeDir <- "ace/lib/ace/theme"
themeFiles <- list.files(
   path = themeDir,
   full.names = TRUE,
   pattern = "\\.css$"
)

## Process the theme files -- we strip out the name preceeding the theme,
## and then add some custom rules.
for (file in themeFiles) {
   
   content <- suppressWarnings(readLines(file))
   fileName <- gsub("\\.css$", "", basename(file))
   
   # Get whether it's dark or not
   jsContents <- readLines(sub("css$", "js", file), warn = FALSE)
   isDark <- any(grepl("exports.isDark = true;", jsContents))
   
   content <- .rs.compile_theme(
      content,
      isDark,
      fileName,
      chunkBgPropOverrideMap = chunk_bg_proportion_map,
      operatorOverrideMap = operator_theme_map,
      keywordOverrideMap = keyword_theme_map)
   
   ## Tweak pastel on dark -- we want the foreground color to be white.
   if (identical(basename(file), "pastel_on_dark.css"))
   {
      foreground <- "#EAEAEA"
      content <- .rs.add_content(
         content,
         paste(
            ".ace_editor, ",
            ".rstudio-themes-flat.ace_editor_theme .profvis-flamegraph, ",
            ".rstudio-themes-flat.ace_editor_theme, ", 
            ".rstudio-themes-flat .ace_editor_theme {",
            sep = ""
         ),
         "  color: %s !important;",
         "}",
         replace = foreground
      )
   }
   
   ## Tweak chaos on dark -- we want the margin column to not overlap the chunk
   if (identical(basename(file), "chaos.css"))
   {
      content <- add_content(
         content,
         ".ace_print-margin {",
         "  width: 0px;",
         "}",
         replace = ""
      )
   }
   
   # Apply final custom fixups
   content <- applyFixups(content, fileName, parsed)
   
   ## Phew! Write it out.
   outputPath <- file.path(outDir, basename(file))
   cat(content, file = outputPath, sep = "\n")
}
