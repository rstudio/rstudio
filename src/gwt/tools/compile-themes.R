#!/usr/bin/env Rscript

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

theme_details_map <- list(
   "ambiance" = list(name = "Ambiance", isDark = TRUE),
   "chaos" = list(name = "Chaos", isDark = TRUE),
   "chrome" = list(name = "Chrome", isDark = FALSE),
   "clouds" = list(name = "Clouds", isDark = FALSE),
   "clouds_midnight" = list(name = "Clouds Midnight", isDark = TRUE),
   "cobalt" = list(name = "Cobalt", isDark = TRUE),
   "crimson_editor" = list(name = "Crimson Editor", isDark = FALSE),
   "dawn" = list(name = "Dawn", isDark = FALSE),
   "dracula" = list(name = "Dracula", isDark = TRUE),
   "dreamweaver" = list(name = "Dreamweaver", isDark = FALSE),
   "eclipse" = list(name = "Eclipse", isDark = FALSE),
   "gob" = list(name = "Gob", isDark = TRUE),
   "idle_fingers" = list(name = "Idle Fingers", isDark = TRUE),
   "iplastic" = list(name = "iPlastic", isDark = FALSE),
   "katzenmilch" = list(name = "Katzenmilch", isDark = FALSE),
   "kr_theme" = list(name = "Kr Theme", isDark = TRUE),
   "merbivore" = list(name = "Merbivore", isDark = TRUE),
   "merbivore_soft" = list(name = "Merbivore Soft", isDark = TRUE),
   "mono_industrial" = list(name = "Mono Industrial", isDark = TRUE),
   "monokai" = list(name = "Monokai", isDark = TRUE),
   "pastel_on_dark" = list(name = "Pastel On Dark", isDark = TRUE),
   "solarized_dark" = list(name = "Solarized Dark", isDark = TRUE),
   "solarized_light" = list(name = "Solarized Light", isDark = FALSE),
   "sqlserver" = list(name = "SQL Server", isDark = FALSE),
   "textmate" = list(name = "Textmate (default)", isDark = FALSE),
   "tomorrow" = list(name = "Tomorrow", isDark = FALSE),
   "tomorrow_night" = list(name = "Tomorrow Night", isDark = TRUE),
   "tomorrow_night_blue" = list(name = "Tomorrow Night Blue", isDark = TRUE),
   "tomorrow_night_bright" = list(name = "Tomorrow Night Bright", isDark = TRUE),
   "tomorrow_night_eighties" = list(name = "Tomorrow Night 80s", isDark = TRUE),
   "twilight" = list(name = "Twilight", isDark = TRUE),
   "vibrant_ink" = list(name = "Vibrant Ink", isDark = TRUE),
   "xcode" = list(name = "Xcode", isDark = FALSE)
)

## A set of operator colors to use, for each theme. Should match the name
## of the theme file in ace.
## We need to explicitly set themes that should be overridden with the default 
## value to NULL
operator_theme_map <- list(
   "solarized_light" = "#93A1A1",
   "solarized_dark" = "#B58900",
   "twilight" = "#7587A6",
   "idle_fingers" = "#6892B2",
   "clouds_midnight" = "#A53553",
   "cobalt" = "#BED6FF",
   "kr_theme" = "#A56464",
   "clouds" = NULL,
   "dawn" = NULL,
   "eclipse" = NULL,
   "katzenmilch" = NULL,
   "merbivore" = NULL,
   "merbivore_soft" = NULL,
   "monokai" = NULL,
   "pastel_on_dark" = NULL,
   "vibrant_ink" = NULL,
   "xcode" = NULL,
   NULL
)

node_selector_map <- list(
   "solarized_light" = NULL,
   "solarized_dark" = NULL,
   "twilight" = NULL,
   "idle_fingers" = NULL,
   "clouds_midnight" = NULL,
   "cobalt" = NULL,
   "kr_theme" = NULL,
   "clouds" = NULL,
   "dawn" = NULL,
   "eclipse" = NULL,
   "katzenmilch" = NULL,
   "merbivore" = NULL,
   "merbivore_soft" = NULL,
   "monokai" = NULL,
   "pastel_on_dark" = NULL,
   "vibrant_ink" = NULL,
   "xcode" = NULL,
   NULL
)

comment_bg_map <- list(
   "solarized_light" = list( "fg" = "#5A776B", "bg" = "#FAE5B7" ),
   "solarized_dark" = NULL,
   "twilight" = NULL,
   "idle_fingers" = NULL,
   "clouds_midnight" = NULL,
   "cobalt" = NULL,
   "kr_theme" = NULL,
   "clouds" = NULL,
   "dawn" = NULL,
   "eclipse" = NULL,
   "katzenmilch" = NULL,
   "merbivore" = NULL,
   "merbivore_soft" = NULL,
   "monokai" = NULL,
   "pastel_on_dark" = NULL,
   "vibrant_ink" = NULL,
   "xcode" = NULL,
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

active_dgb_line_map <- list(
   "solarized_dark" = "#585B2C"
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
   nextBraceLoc <- .rs.findNext("}", content, aceCursorLayerLoc)
   
   content[aceCursorLayerLoc:nextBraceLoc] <- ""
   
   content
}

applyFixups.cobalt <- function(content, parsed) {
   content <- .rs.setPrintMarginColor(content, "#246")
   content
}

applyFixups.clouds_midnight <- function(content, parsed) {
   content <- .rs.setPrintMarginColor(content, "#333")
   content
}

applyFixups.idle_fingers <- function(content, parsed) {
   content <- .rs.setPrintMarginColor(content, "#555")
   content
}

applyFixups.kr_theme <- function(content, parsed) {
   content <- .rs.setPrintMarginColor(content, "#333")
   content
}

applyFixups.merbivore_soft <- applyFixups.kr_theme
applyFixups.pastel_on_dark <- function(content, parsed)
{
   content <- .rs.setPrintMarginColor(content, "#333")
   content <- .rs.updateSetting(content, "#3F3B3B", "ace_foreign_line", "background-color")
   content <- .rs.updateSetting(content, "#524E4E", "ace_find_line", "background-color")
   content <- .rs.updateSetting(content, "#524E4E", "ace_console_error", "background-color")
   content <- .rs.updateSetting(content, "#EAEAEA", "terminal", "color")
   
   termBgLine <- grep(".xtermInvertBgColor( *){", content, perl = TRUE)
   content[termBgLine] <- paste0(".xtermInvertBgColor { background-color: #EAEAEA; }")
   
   content
}

applyFixups.solarized_dark <- function(content, parsed) {
   content <- .rs.setActiveDebugLineColor(content, "#585b2c")
}

applyFixups.tomorrow_night_blue <- applyFixups.kr_theme
applyFixups.tomorrow_night_bright <- applyFixups.kr_theme

applyFixups.tomorrow_night_eighties <- function(content, parsed) {
   content <- .rs.setPrintMarginColor(content, "#444")
   content
}
applyFixups.tomorrow_night <- applyFixups.tomorrow_night_eighties

applyFixups.twilight <- function(content, parsed) {
   content <- .rs.setPrintMarginColor(content, "#333")
   content
}

applyFixups.vibrant_ink <- applyFixups.tomorrow_night_eighties

## Get the set of all theme .css files
aceRoot <- "./rstudio-ace"
args <- commandArgs(trailingOnly=TRUE)
if (length(args) >= 1)
   aceRoot <- args[1]

outDir <- "../../cpp/session/resources/themes"
themeDir <- file.path(aceRoot, "/lib/ace/theme")

if (!file.exists(themeDir))
   stop("Ace directory not found. Usage: Rscript compile-themes.R <path-to-ace-reposiotry>")

themeFiles <- list.files(
   path = themeDir,
   full.names = TRUE,
   pattern = "\\.css$"
)

## Process the theme files -- we strip out the name preceding the theme,
## and then add some custom rules.
for (themeFile in themeFiles) {
   content <- suppressWarnings(readLines(themeFile))
   fileName <- gsub("\\.css$", "", basename(themeFile))
   
   # Get whether it's dark or not
   jsContents <- readLines(sub("css$", "js", themeFile), warn = FALSE)
   isDark <- any(grepl("exports.isDark = true;", jsContents))
   
   content <- .rs.compile_theme(
      content,
      isDark,
      fileName,
      chunkBgPropOverrideMap = chunk_bg_proportion_map,
      operatorOverrideMap = operator_theme_map,
      keywordOverrideMap = keyword_theme_map,
      nodeSelectorOverrideMap = node_selector_map,
      commentBgOverrideMap = comment_bg_map)

   if (length(content) > 0)
   {
      content <- unlist(strsplit(content, "\n"))
      ## Tweak pastel on dark -- we want the foreground color to be white.
      if (identical(basename(themeFile), "pastel_on_dark.css"))
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
      if (identical(basename(themeFile), "chaos.css"))
      {
         content <- .rs.add_content(
            content,
            ".ace_print-margin {",
            "  width: 0px;",
            "}",
            replace = ""
         )
      }
      # Apply final custom fixups
      content <- applyFixups(content, fileName, parsed)
      
      # Add final details
      themeDetails <- if (is.null(theme_details_map[[fileName]])) list(name = fileName, isDark = FALSE)
                      else theme_details_map[[fileName]]
      content <- c(
         paste("/* rs-theme-name:", themeDetails$name, "*/"),
         paste("/* rs-theme-is-dark:", themeDetails$isDark, "*/"),
         content)
      
      ## Phew! Write it out.
      outputPath <- file.path(outDir, paste0(fileName, ".rstheme"))
      cat(content, file = outputPath, sep = "\n")
   }
}
