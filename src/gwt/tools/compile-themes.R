#!/usr/bin/env Rscript

if (!require("highlight")) {
   install.packages("highlight")
   if (!require("highlight")) {
      stop("This script requires 'highlight' in order to run!")
   }
}

## Default operator colors to use for light, dark themes. These should be
## a grey tone that remains distinctive on either dark or light backgrounds.
dark_theme_operator <- "#AAAAAA"
light_theme_operator <- "#888888"

## A set of operator colors to use, for each theme. Should match the name
## of the theme file in ace.
operator_theme_map <- list(
   "solarized_light" = "#93A1A1",
   "solarized_dark" = "#B58900",
   "twilight" = "#7587A6",
   "idle_fingers" = "#6892B2",
   "clouds" = light_theme_operator,
   "clouds_midnight" = "#A53553",
   "cobalt" = "#BED6FF",
   "dawn" = light_theme_operator,
   "eclipse" = light_theme_operator,
   "katzenmilch" = light_theme_operator,
   "kr_theme" = "#A56464",
   "merbivore" = dark_theme_operator,
   "merbivore_soft" = dark_theme_operator,
   "monokai" = dark_theme_operator,
   "pastel_on_dark" = dark_theme_operator,
   "vibrant_ink" = dark_theme_operator,
   "xcode" = light_theme_operator,
   NULL
)

## Similarly, colors for keywords that we might override.
keyword_theme_map <- list(
   "eclipse" = "#800080",
   "clouds" = "#800080",
   NULL
)

add_operator_color <- function(content, file) {
   add_content(
      content,
      ".ace_keyword.ace_operator {",
      "  color: %s !important;",
      "}",
      replace = operator_theme_map[[file]]
   )
}

add_keyword_color <- function(content, file) {
   add_content(
      content,
      ".ace_keyword {",
      "  color: %s !important;",
      "}",
      replace = keyword_theme_map[[file]]
   )
}

## Utility colors for parsing hex colors
parse_css_color <- function(value) {
   unlist(lapply(value, function(x) {
      if (grepl("\\srgb", x, perl = TRUE)) {
         stripped <- gsub("^.*\\((.*)\\)$", "\\1", x)
         splat <- strsplit(stripped, "\\s*,\\s*", perl = TRUE)[[1]]
         c(
            red = as.numeric(splat[[1]]),
            green = as.numeric(splat[[2]]),
            blue = as.numeric(splat[[3]])
         )
      } else {
         col2rgb(value)[, 1]
      }
   }))
}
   

format_css_color <- function(color) {
   sprintf("rgb(%s, %s, %s)",
           color[["red"]],
           color[["green"]],
           color[["blue"]])
}

color_as_hex <- function(color) {
   paste("#", paste(toupper(as.hexmode(as.integer(color))), collapse = ""), sep = "")
}

# Strip color from field
strip_color_from_field <- function(css)
   if (grepl("rgb", css)) {
      gsub(".*rgb", "rgb", css)
   } else {
      gsub(".*#", "#", css)
   }

mix_colors <- function(x, y, p) {
   setNames(as.integer(
      (p * x) +
      ((1 - p) * y)
   ), c("red", "green", "blue"))
}

add_content <- function(content, ..., replace)
   c(
      content,
      do.call(sprintf, list(paste(..., sep = "\n"), replace))
   )

create_line_marker_rule <- function(markerName, markerColor) {
   sprintf(paste(sep = "\n",
                 ".ace_marker-layer %s {",
                 "  position: absolute;",
                 "  z-index: -1;",
                 "  background-color: %s;",
                 "}"),
           markerName,
           markerColor)
}

chunk_bg_proportion_map <- list(
   "tomorrow_night_bright" = 0.85
)

get_chunk_bg_color <- function(themeName, isDark) {
   p <- chunk_bg_proportion_map[[themeName]]
   if (is.null(p))
      if (isDark) 0.9 else 0.95
   else
      p
}

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
   
   ## Guess the theme name -- all rules should start with it.
   stripped <- sub(" .*", "", content)
   stripped <- grep("^\\.", stripped, value = TRUE)
   
   ## Get the most common value; this is the theme name.
   ## We do this because some rules will have e.g.
   ## '.ace-<theme>.normal-mode', or other things we don't use.
   themeNameCssClass <- names(sort(c(table(stripped)), decreasing = TRUE))[[1]]
   
   ## There may (should) be a rule for just '.ace-<theme> { ... }'; we need
   ## to preserve this theme, but apply it to the '.ace_editor' directly.
   regex <- paste("^\\s*", themeNameCssClass, "\\s*\\{\\s*$", sep = "")
   content <- gsub(regex, ".ace_editor {", content)
   
   ## Strip the theme name rule from the CSS.
   regex <- paste("^\\", themeNameCssClass, "\\S*\\s+", sep = "")
   content <- gsub(regex, "", content)
   
   ## Modify the CSS so the parser can handle it.
   modified <- c()
   blockStarts <- grep("{", content, fixed = TRUE)
   blockEnds <- grep("}", content, fixed = TRUE)
   
   pairs <- Map(c, blockStarts, blockEnds)
   for (pair in pairs) {
      
      start <- pair[[1]]
      end <- pair[[2]]
      
      ## Figure out what classes are associated with this block.
      classesEnd <- start
      index <- start - 1
      if (index > 0 && grepl(",\\s*$", content[index])) {
         while (index > 0 && grepl(",\\s*$", content[index]))
            index <- index - 1
         classesStart <- index + 1
      } else {
         classesStart <- classesEnd
      }
      
      subContent <- content[classesStart:classesEnd]
      subContent[length(subContent)] <- gsub(
         "\\s*\\{.*", "",
         subContent[length(subContent)]
      )
      
      allClasses <- strsplit(paste(subContent, collapse = " "),
                             split = "\\s*,\\s*",
                             perl = TRUE)[[1]]
      
      thisBlock <- gsub(".*\\s*\\{.*", "{", content[start:end])
      
      # Ensure all CSS lines end with a semicolon.
      range <- 2:(length(thisBlock) - 1)
      thisBlock[range] <-
         paste(
            gsub(";\\s*", "", thisBlock[range]),
            ";",
            sep = ""
         )
      
      blockPasted <- paste(thisBlock, collapse = "\n")
      for (class in allClasses) {
         modified <- c(modified, paste(class, blockPasted))
      }
      
   }
   
   ## Parse the modified CSS.
   modified <- unlist(strsplit(modified, "\n", fixed = TRUE))
   parsed <- css.parser(lines = modified)
   
   if (!any(grepl("^ace_keyword", names(parsed)))) {
      warning("No field 'ace_keyword' in file '", basename(file), "'")
      next
   }
   
   name <- grep("^ace_keyword", names(parsed), value = TRUE)[[1]]
   keywordColor <- parsed[[name]]$color
   if (is.null(keywordColor)) {
      warning("No keyword color available for file '", basename(file), "'")
      next
   }
   
   content <- add_content(
      content,
      ".nocolor.ace_editor .ace_line span {",
      "  color: %s !important;",
      "}",
      replace = keywordColor
   )
   
   ## Coloring for brackets, discarding the ace bounding box shown
   ## on highlight.
   layerName <- "ace_marker-layer .ace_bracket"
   if (!(layerName %in% names(parsed))) {
      warning("Expected rule for '", layerName, "' in file '", basename(file), "'")
      next
   }
   
   borderField <- parsed[[layerName]]$border
   if (is.null(borderField)) {
      warning("No field for layer '", layerName, "' in file '", basename(file), "'")
      next
   }
   
   jsContents <- readLines(sub("css$", "js", file))
   isDark <- any(grepl("exports.isDark = true;", jsContents))
   
   operatorBgColor <- if (isDark)
      "rgba(128, 128, 128, 0.5)"
   else
      "rgba(192, 192, 192, 0.5)"
   
   content <- add_content(
      content,
      ".ace_bracket {",
      "  margin: 0 !important;",
      "  border: 0 !important;",
      "  background-color: %s;",
      "}",
      replace = operatorBgColor
   )
   
   ## Get the default background, foreground color for the theme.
   background <- parsed$ace_editor$`background-color`
   if (is.null(background))
      background <- if (isDark) "#000000" else "#FFFFFF"
   
   foreground <- parsed$ace_editor$color
   if (is.null(foreground))
      foreground <- if (isDark) "#FFFFFF" else "#000000"
   
   ## Tweak pastel on dark -- we want the foreground color to be white.
   if (identical(basename(file), "pastel_on_dark.css"))
   {
      foreground <- "#EAEAEA"
      content <- add_content(
         content,
         ".ace_editor {",
         "  color: %s !important;",
         "}",
         replace = foreground
      )
   }
      
   
   ## Generate a color used for chunks, e.g. in .Rmd documents.
   backgroundRgb <- parse_css_color(background)
   foregroundRgb <- parse_css_color(foreground)
   
   ## Determine an appropriate mixing proportion, and override for certain
   ## themes.
   mix <- get_chunk_bg_color(fileName, isDark)
   
   mergedColor <- mix_colors(
      backgroundRgb,
      foregroundRgb,
      mix
   )
   
   content <- c(
      content,
      create_line_marker_rule(".ace_foreign_line", color_as_hex(mergedColor))
   )
   
   ## Generate a color used for 'debugging' backgrounds.
   debugPrimary <- parse_css_color("#FFDE38")
   debugBg <- color_as_hex(mix_colors(backgroundRgb, debugPrimary, 0.5))
   
   content <- c(
      content,
      create_line_marker_rule(".ace_active_debug_line", debugBg)
   )
   
   ## Generate a background color used for console errors, as well as
   ## 'find_line' (used for highlighting e.g. 'sourceCpp' errors).
   
   ## Dark backgrounds need a bit more contrast than light ones for
   ## a nice visual display.
   mixingProportion <- if (isDark) 0.8 else 0.9
   errorBgColor <-
      color_as_hex(mix_colors(backgroundRgb, foregroundRgb, mixingProportion))
   
   content <- c(
      content,
      create_line_marker_rule(".ace_find_line", errorBgColor)
   )
   
   content <- add_content(
      content,
      ".ace_console_error {",
      "  background-color: %s;",
      "}",
      replace = errorBgColor
   )
   
   ## Add operator colors if necessary.
   if (fileName %in% names(operator_theme_map))
      content <- add_operator_color(content, fileName)
   
   ## Add keyword colors if necessary.
   if (fileName %in% names(keyword_theme_map))
      content <- add_keyword_color(content, fileName)
   
   ## Phew! Write it out.
   outputPath <- file.path(outDir, basename(file))
   cat(content, file = outputPath, sep = "\n")
   
}
