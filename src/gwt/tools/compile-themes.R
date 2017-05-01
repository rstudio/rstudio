#!/usr/bin/env Rscript

if (!require("highlight", quietly = TRUE)) {
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

applyFixups <- function(content, fileName, parsed) {
   
   methodName <- paste("applyFixups", fileName, sep = ".")
   method <- try(get(methodName), silent = TRUE)
   if (inherits(method, "try-error"))
      return(content)
   
   method(content, parsed)
}

findNext <- function(regex, content, start = 1, end = length(content)) {
   matches <- grep(regex, content, perl = TRUE)
   matches[(matches > start) & (matches < end)][1]
}

setPrintMarginColor <- function(content, color) {
   printMarginLoc <- grep("print-margin", content, perl = TRUE)
   bgLoc <- grep("background:", content, perl = TRUE)
   loc <- bgLoc[bgLoc > printMarginLoc][1]
   content[loc] <- paste("  background:", color, ";")
   content
}

applyFixups.ambiance <- function(content, parsed) {
   
   aceCursorLayerLoc <- grep("^\\s*\\.ace_cursor-layer\\s*{", content, perl = TRUE)
   nextBraceLoc <- findNext("}", content, aceCursorLayerLoc)
   
   content[aceCursorLayerLoc:nextBraceLoc] <- ""
   
   content
}

applyFixups.cobalt <- function(content, parsed) {
   content <- setPrintMarginColor(content, "#246")
   content
}

applyFixups.clouds_midnight <- function(content, parsed) {
   content <- setPrintMarginColor(content, "#333")
   content
}

applyFixups.idle_fingers <- function(content, parsed) {
   content <- setPrintMarginColor(content, "#555")
   content
}

applyFixups.kr_theme <- function(content, parsed) {
   content <- setPrintMarginColor(content, "#333")
   content
}

applyFixups.merbivore_soft <- applyFixups.kr_theme
applyFixups.pastel_on_dark <- applyFixups.kr_theme

applyFixups.tomorrow_night_blue <- applyFixups.kr_theme
applyFixups.tomorrow_night_bright <- applyFixups.kr_theme

applyFixups.tomorrow_night_eighties <- function(content, parsed) {
   content <- setPrintMarginColor(content, "#444")
   content
}
applyFixups.tomorrow_night <- applyFixups.tomorrow_night_eighties

applyFixups.twilight <- function(content, parsed) {
   content <- setPrintMarginColor(content, "#333")
   content
}

applyFixups.vibrant_ink <- applyFixups.tomorrow_night_eighties

create_terminal_cursor_rules <- function(isDark) {
   termCursorBgColorFirst <-  "#333"
   termCursorBgColorSecond <- "#CCC"
   if (isDark) {
      termCursorBgColorFirst <-  "#CCC"
      termCursorBgColorSecond <- "#1e1e1e"
   }
   
   sprintf(paste(sep = "\n",
                 ".terminal.focus:not(.xterm-cursor-style-underline):not(.xterm-cursor-style-bar) .terminal-cursor {",
                 "   background-color: %s;",
                 "   color: %s;",
                 "}",
                 ".terminal:not(.focus) .terminal-cursor {",
                 "  outline: 1px solid %s;",
                 "  outline-offset: -1px;",
                 "  background-color: transparent;",
                 "}",
                 ".terminal.xterm-cursor-style-bar .terminal-cursor::before,",
                 ".terminal.xterm-cursor-style-underline .terminal-cursor::before {",
                 "   content: \"\";",
                 "   display: block;",
                 "   position: absolute;",
                 "   background-color: %s;",
                 "}",
                 ".terminal.xterm-cursor-style-bar.focus.xterm-cursor-blink .terminal-cursor::before,",
                 ".terminal.xterm-cursor-style-underline.focus.xterm-cursor-blink .terminal-cursor::before {",
                 "   background-color: %s;",
                 "}"),
           
           # .terminal:not(.xterm-cursor-style-underline):not(.xterm-cursor-style-bar) .terminal-cursor
           termCursorBgColorFirst,
           termCursorBgColorSecond,
           
           # .terminal:not(.focus) .terminal-cursor
           termCursorBgColorFirst,
           
           # .terminal.xterm-cursor-style-bar .terminal-cursor::before
           # .terminal.xterm-cursor-style-underline .terminal-cursor::before
           termCursorBgColorFirst,

           # .terminal.xterm-cursor-style-bar.focus.xterm-cursor-blink .terminal-cursor::before,
           # .terminal.xterm-cursor-style-underline.focus.xterm-cursor-blink .terminal-cursor::before
           termCursorBgColorFirst)
}

create_terminal_rule <- function(background, foreground) {
   sprintf(paste(sep = "\n",
                 ".terminal {",
                 "  background-color: %s;",
                 "  color: %s;",
                 "  font-feature-settings: \"liga\" 0;",
                 "  position: relative;",
                 "}"),
           background, foreground)
}

create_terminal_viewport_rule <- function(background) {
   sprintf(paste(sep = "\n",
                 ".terminal .xterm-viewport {",
                 "  /* On OS X this is required in order for the scroll bar to appear fully opaque */",
                 "  background-color: %s;",
                 "  overflow-y: scroll;",
                 "}"),
           background)
}

create_xterm_color_rules <- function(background, foreground, isDark) {
   paste(sep = "\n",
      sprintf(".xtermInvertColor { color: %s }", background),
      sprintf(".xtermInvertBgColor { background-color: %s }", foreground),
      ".xtermBold { font-weight: bold; }",
      ".xtermUnderline { text-decoration: underline; }",
      ".xtermBlink { text-decoration: blink; }",
      ".xtermHidden { visibility: hidden; }",
      ".xtermColor0 { color: #2e3436; }",
      ".xtermBgColor0 { background-color: #2e3436; }",
      ".xtermColor1 { color: #cc0000; }",
      ".xtermBgColor1 { background-color: #cc0000; }",
      ".xtermColor2 { color: #4e9a06; }",
      ".xtermBgColor2 { background-color: #4e9a06; }",
      ".xtermColor3 { color: #c4a000; }",
      ".xtermBgColor3 { background-color: #c4a000; }",
      ".xtermColor4 { color: #3465a4; }",
      ".xtermBgColor4 { background-color: #3465a4; }",
      ".xtermColor5 { color: #75507b; }",
      ".xtermBgColor5 { background-color: #75507b; }",
      ".xtermColor6 { color: #06989a; }",
      ".xtermBgColor6 { background-color: #06989a; }",
      ".xtermColor7 { color: #d3d7cf; }",
      ".xtermBgColor7 { background-color: #d3d7cf; }",
      ".xtermColor8 { color: #555753; }",
      ".xtermBgColor8 { background-color: #555753; }",
      ".xtermColor9 { color: #ef2929; }",
      ".xtermBgColor9 { background-color: #ef2929; }",
      ".xtermColor10 { color: #8ae234; }",
      ".xtermBgColor10 { background-color: #8ae234; }",
      ".xtermColor11 { color: #fce94f; }",
      ".xtermBgColor11 { background-color: #fce94f; }",
      ".xtermColor12 { color: #729fcf; }",
      ".xtermBgColor12 { background-color: #729fcf; }",
      ".xtermColor13 { color: #ad7fa8; }",
      ".xtermBgColor13 { background-color: #ad7fa8; }",
      ".xtermColor14 { color: #34e2e2; }",
      ".xtermBgColor14 { background-color: #34e2e2; }",
      ".xtermColor15 { color: #eeeeec; }",
      ".xtermBgColor15 { background-color: #eeeeec; }",
      ".xtermColor16 { color: #000000; }",
      ".xtermBgColor16 { background-color: #000000; }",
      ".xtermColor17 { color: #00005f; }",
      ".xtermBgColor17 { background-color: #00005f; }",
      ".xtermColor18 { color: #000087; }",
      ".xtermBgColor18 { background-color: #000087; }",
      ".xtermColor19 { color: #0000af; }",
      ".xtermBgColor19 { background-color: #0000af; }",
      ".xtermColor20 { color: #0000d7; }",
      ".xtermBgColor20 { background-color: #0000d7; }",
      ".xtermColor21 { color: #0000ff; }",
      ".xtermBgColor21 { background-color: #0000ff; }",
      ".xtermColor22 { color: #005f00; }",
      ".xtermBgColor22 { background-color: #005f00; }",
      ".xtermColor23 { color: #005f5f; }",
      ".xtermBgColor23 { background-color: #005f5f; }",
      ".xtermColor24 { color: #005f87; }",
      ".xtermBgColor24 { background-color: #005f87; }",
      ".xtermColor25 { color: #005faf; }",
      ".xtermBgColor25 { background-color: #005faf; }",
      ".xtermColor26 { color: #005fd7; }",
      ".xtermBgColor26 { background-color: #005fd7; }",
      ".xtermColor27 { color: #005fff; }",
      ".xtermBgColor27 { background-color: #005fff; }",
      ".xtermColor28 { color: #008700; }",
      ".xtermBgColor28 { background-color: #008700; }",
      ".xtermColor29 { color: #00875f; }",
      ".xtermBgColor29 { background-color: #00875f; }",
      ".xtermColor30 { color: #008787; }",
      ".xtermBgColor30 { background-color: #008787; }",
      ".xtermColor31 { color: #0087af; }",
      ".xtermBgColor31 { background-color: #0087af; }",
      ".xtermColor32 { color: #0087d7; }",
      ".xtermBgColor32 { background-color: #0087d7; }",
      ".xtermColor33 { color: #0087ff; }",
      ".xtermBgColor33 { background-color: #0087ff; }",
      ".xtermColor34 { color: #00af00; }",
      ".xtermBgColor34 { background-color: #00af00; }",
      ".xtermColor35 { color: #00af5f; }",
      ".xtermBgColor35 { background-color: #00af5f; }",
      ".xtermColor36 { color: #00af87; }",
      ".xtermBgColor36 { background-color: #00af87; }",
      ".xtermColor37 { color: #00afaf; }",
      ".xtermBgColor37 { background-color: #00afaf; }",
      ".xtermColor38 { color: #00afd7; }",
      ".xtermBgColor38 { background-color: #00afd7; }",
      ".xtermColor39 { color: #00afff; }",
      ".xtermBgColor39 { background-color: #00afff; }",
      ".xtermColor40 { color: #00d700; }",
      ".xtermBgColor40 { background-color: #00d700; }",
      ".xtermColor41 { color: #00d75f; }",
      ".xtermBgColor41 { background-color: #00d75f; }",
      ".xtermColor42 { color: #00d787; }",
      ".xtermBgColor42 { background-color: #00d787; }",
      ".xtermColor43 { color: #00d7af; }",
      ".xtermBgColor43 { background-color: #00d7af; }",
      ".xtermColor44 { color: #00d7d7; }",
      ".xtermBgColor44 { background-color: #00d7d7; }",
      ".xtermColor45 { color: #00d7ff; }",
      ".xtermBgColor45 { background-color: #00d7ff; }",
      ".xtermColor46 { color: #00ff00; }",
      ".xtermBgColor46 { background-color: #00ff00; }",
      ".xtermColor47 { color: #00ff5f; }",
      ".xtermBgColor47 { background-color: #00ff5f; }",
      ".xtermColor48 { color: #00ff87; }",
      ".xtermBgColor48 { background-color: #00ff87; }",
      ".xtermColor49 { color: #00ffaf; }",
      ".xtermBgColor49 { background-color: #00ffaf; }",
      ".xtermColor50 { color: #00ffd7; }",
      ".xtermBgColor50 { background-color: #00ffd7; }",
      ".xtermColor51 { color: #00ffff; }",
      ".xtermBgColor51 { background-color: #00ffff; }",
      ".xtermColor52 { color: #5f0000; }",
      ".xtermBgColor52 { background-color: #5f0000; }",
      ".xtermColor53 { color: #5f005f; }",
      ".xtermBgColor53 { background-color: #5f005f; }",
      ".xtermColor54 { color: #5f0087; }",
      ".xtermBgColor54 { background-color: #5f0087; }",
      ".xtermColor55 { color: #5f00af; }",
      ".xtermBgColor55 { background-color: #5f00af; }",
      ".xtermColor56 { color: #5f00d7; }",
      ".xtermBgColor56 { background-color: #5f00d7; }",
      ".xtermColor57 { color: #5f00ff; }",
      ".xtermBgColor57 { background-color: #5f00ff; }",
      ".xtermColor58 { color: #5f5f00; }",
      ".xtermBgColor58 { background-color: #5f5f00; }",
      ".xtermColor59 { color: #5f5f5f; }",
      ".xtermBgColor59 { background-color: #5f5f5f; }",
      ".xtermColor60 { color: #5f5f87; }",
      ".xtermBgColor60 { background-color: #5f5f87; }",
      ".xtermColor61 { color: #5f5faf; }",
      ".xtermBgColor61 { background-color: #5f5faf; }",
      ".xtermColor62 { color: #5f5fd7; }",
      ".xtermBgColor62 { background-color: #5f5fd7; }",
      ".xtermColor63 { color: #5f5fff; }",
      ".xtermBgColor63 { background-color: #5f5fff; }",
      ".xtermColor64 { color: #5f8700; }",
      ".xtermBgColor64 { background-color: #5f8700; }",
      ".xtermColor65 { color: #5f875f; }",
      ".xtermBgColor65 { background-color: #5f875f; }",
      ".xtermColor66 { color: #5f8787; }",
      ".xtermBgColor66 { background-color: #5f8787; }",
      ".xtermColor67 { color: #5f87af; }",
      ".xtermBgColor67 { background-color: #5f87af; }",
      ".xtermColor68 { color: #5f87d7; }",
      ".xtermBgColor68 { background-color: #5f87d7; }",
      ".xtermColor69 { color: #5f87ff; }",
      ".xtermBgColor69 { background-color: #5f87ff; }",
      ".xtermColor70 { color: #5faf00; }",
      ".xtermBgColor70 { background-color: #5faf00; }",
      ".xtermColor71 { color: #5faf5f; }",
      ".xtermBgColor71 { background-color: #5faf5f; }",
      ".xtermColor72 { color: #5faf87; }",
      ".xtermBgColor72 { background-color: #5faf87; }",
      ".xtermColor73 { color: #5fafaf; }",
      ".xtermBgColor73 { background-color: #5fafaf; }",
      ".xtermColor74 { color: #5fafd7; }",
      ".xtermBgColor74 { background-color: #5fafd7; }",
      ".xtermColor75 { color: #5fafff; }",
      ".xtermBgColor75 { background-color: #5fafff; }",
      ".xtermColor76 { color: #5fd700; }",
      ".xtermBgColor76 { background-color: #5fd700; }",
      ".xtermColor77 { color: #5fd75f; }",
      ".xtermBgColor77 { background-color: #5fd75f; }",
      ".xtermColor78 { color: #5fd787; }",
      ".xtermBgColor78 { background-color: #5fd787; }",
      ".xtermColor79 { color: #5fd7af; }",
      ".xtermBgColor79 { background-color: #5fd7af; }",
      ".xtermColor80 { color: #5fd7d7; }",
      ".xtermBgColor80 { background-color: #5fd7d7; }",
      ".xtermColor81 { color: #5fd7ff; }",
      ".xtermBgColor81 { background-color: #5fd7ff; }",
      ".xtermColor82 { color: #5fff00; }",
      ".xtermBgColor82 { background-color: #5fff00; }",
      ".xtermColor83 { color: #5fff5f; }",
      ".xtermBgColor83 { background-color: #5fff5f; }",
      ".xtermColor84 { color: #5fff87; }",
      ".xtermBgColor84 { background-color: #5fff87; }",
      ".xtermColor85 { color: #5fffaf; }",
      ".xtermBgColor85 { background-color: #5fffaf; }",
      ".xtermColor86 { color: #5fffd7; }",
      ".xtermBgColor86 { background-color: #5fffd7; }",
      ".xtermColor87 { color: #5fffff; }",
      ".xtermBgColor87 { background-color: #5fffff; }",
      ".xtermColor88 { color: #870000; }",
      ".xtermBgColor88 { background-color: #870000; }",
      ".xtermColor89 { color: #87005f; }",
      ".xtermBgColor89 { background-color: #87005f; }",
      ".xtermColor90 { color: #870087; }",
      ".xtermBgColor90 { background-color: #870087; }",
      ".xtermColor91 { color: #8700af; }",
      ".xtermBgColor91 { background-color: #8700af; }",
      ".xtermColor92 { color: #8700d7; }",
      ".xtermBgColor92 { background-color: #8700d7; }",
      ".xtermColor93 { color: #8700ff; }",
      ".xtermBgColor93 { background-color: #8700ff; }",
      ".xtermColor94 { color: #875f00; }",
      ".xtermBgColor94 { background-color: #875f00; }",
      ".xtermColor95 { color: #875f5f; }",
      ".xtermBgColor95 { background-color: #875f5f; }",
      ".xtermColor96 { color: #875f87; }",
      ".xtermBgColor96 { background-color: #875f87; }",
      ".xtermColor97 { color: #875faf; }",
      ".xtermBgColor97 { background-color: #875faf; }",
      ".xtermColor98 { color: #875fd7; }",
      ".xtermBgColor98 { background-color: #875fd7; }",
      ".xtermColor99 { color: #875fff; }",
      ".xtermBgColor99 { background-color: #875fff; }",
      ".xtermColor100 { color: #878700; }",
      ".xtermBgColor100 { background-color: #878700; }",
      ".xtermColor101 { color: #87875f; }",
      ".xtermBgColor101 { background-color: #87875f; }",
      ".xtermColor102 { color: #878787; }",
      ".xtermBgColor102 { background-color: #878787; }",
      ".xtermColor103 { color: #8787af; }",
      ".xtermBgColor103 { background-color: #8787af; }",
      ".xtermColor104 { color: #8787d7; }",
      ".xtermBgColor104 { background-color: #8787d7; }",
      ".xtermColor105 { color: #8787ff; }",
      ".xtermBgColor105 { background-color: #8787ff; }",
      ".xtermColor106 { color: #87af00; }",
      ".xtermBgColor106 { background-color: #87af00; }",
      ".xtermColor107 { color: #87af5f; }",
      ".xtermBgColor107 { background-color: #87af5f; }",
      ".xtermColor108 { color: #87af87; }",
      ".xtermBgColor108 { background-color: #87af87; }",
      ".xtermColor109 { color: #87afaf; }",
      ".xtermBgColor109 { background-color: #87afaf; }",
      ".xtermColor110 { color: #87afd7; }",
      ".xtermBgColor110 { background-color: #87afd7; }",
      ".xtermColor111 { color: #87afff; }",
      ".xtermBgColor111 { background-color: #87afff; }",
      ".xtermColor112 { color: #87d700; }",
      ".xtermBgColor112 { background-color: #87d700; }",
      ".xtermColor113 { color: #87d75f; }",
      ".xtermBgColor113 { background-color: #87d75f; }",
      ".xtermColor114 { color: #87d787; }",
      ".xtermBgColor114 { background-color: #87d787; }",
      ".xtermColor115 { color: #87d7af; }",
      ".xtermBgColor115 { background-color: #87d7af; }",
      ".xtermColor116 { color: #87d7d7; }",
      ".xtermBgColor116 { background-color: #87d7d7; }",
      ".xtermColor117 { color: #87d7ff; }",
      ".xtermBgColor117 { background-color: #87d7ff; }",
      ".xtermColor118 { color: #87ff00; }",
      ".xtermBgColor118 { background-color: #87ff00; }",
      ".xtermColor119 { color: #87ff5f; }",
      ".xtermBgColor119 { background-color: #87ff5f; }",
      ".xtermColor120 { color: #87ff87; }",
      ".xtermBgColor120 { background-color: #87ff87; }",
      ".xtermColor121 { color: #87ffaf; }",
      ".xtermBgColor121 { background-color: #87ffaf; }",
      ".xtermColor122 { color: #87ffd7; }",
      ".xtermBgColor122 { background-color: #87ffd7; }",
      ".xtermColor123 { color: #87ffff; }",
      ".xtermBgColor123 { background-color: #87ffff; }",
      ".xtermColor124 { color: #af0000; }",
      ".xtermBgColor124 { background-color: #af0000; }",
      ".xtermColor125 { color: #af005f; }",
      ".xtermBgColor125 { background-color: #af005f; }",
      ".xtermColor126 { color: #af0087; }",
      ".xtermBgColor126 { background-color: #af0087; }",
      ".xtermColor127 { color: #af00af; }",
      ".xtermBgColor127 { background-color: #af00af; }",
      ".xtermColor128 { color: #af00d7; }",
      ".xtermBgColor128 { background-color: #af00d7; }",
      ".xtermColor129 { color: #af00ff; }",
      ".xtermBgColor129 { background-color: #af00ff; }",
      ".xtermColor130 { color: #af5f00; }",
      ".xtermBgColor130 { background-color: #af5f00; }",
      ".xtermColor131 { color: #af5f5f; }",
      ".xtermBgColor131 { background-color: #af5f5f; }",
      ".xtermColor132 { color: #af5f87; }",
      ".xtermBgColor132 { background-color: #af5f87; }",
      ".xtermColor133 { color: #af5faf; }",
      ".xtermBgColor133 { background-color: #af5faf; }",
      ".xtermColor134 { color: #af5fd7; }",
      ".xtermBgColor134 { background-color: #af5fd7; }",
      ".xtermColor135 { color: #af5fff; }",
      ".xtermBgColor135 { background-color: #af5fff; }",
      ".xtermColor136 { color: #af8700; }",
      ".xtermBgColor136 { background-color: #af8700; }",
      ".xtermColor137 { color: #af875f; }",
      ".xtermBgColor137 { background-color: #af875f; }",
      ".xtermColor138 { color: #af8787; }",
      ".xtermBgColor138 { background-color: #af8787; }",
      ".xtermColor139 { color: #af87af; }",
      ".xtermBgColor139 { background-color: #af87af; }",
      ".xtermColor140 { color: #af87d7; }",
      ".xtermBgColor140 { background-color: #af87d7; }",
      ".xtermColor141 { color: #af87ff; }",
      ".xtermBgColor141 { background-color: #af87ff; }",
      ".xtermColor142 { color: #afaf00; }",
      ".xtermBgColor142 { background-color: #afaf00; }",
      ".xtermColor143 { color: #afaf5f; }",
      ".xtermBgColor143 { background-color: #afaf5f; }",
      ".xtermColor144 { color: #afaf87; }",
      ".xtermBgColor144 { background-color: #afaf87; }",
      ".xtermColor145 { color: #afafaf; }",
      ".xtermBgColor145 { background-color: #afafaf; }",
      ".xtermColor146 { color: #afafd7; }",
      ".xtermBgColor146 { background-color: #afafd7; }",
      ".xtermColor147 { color: #afafff; }",
      ".xtermBgColor147 { background-color: #afafff; }",
      ".xtermColor148 { color: #afd700; }",
      ".xtermBgColor148 { background-color: #afd700; }",
      ".xtermColor149 { color: #afd75f; }",
      ".xtermBgColor149 { background-color: #afd75f; }",
      ".xtermColor150 { color: #afd787; }",
      ".xtermBgColor150 { background-color: #afd787; }",
      ".xtermColor151 { color: #afd7af; }",
      ".xtermBgColor151 { background-color: #afd7af; }",
      ".xtermColor152 { color: #afd7d7; }",
      ".xtermBgColor152 { background-color: #afd7d7; }",
      ".xtermColor153 { color: #afd7ff; }",
      ".xtermBgColor153 { background-color: #afd7ff; }",
      ".xtermColor154 { color: #afff00; }",
      ".xtermBgColor154 { background-color: #afff00; }",
      ".xtermColor155 { color: #afff5f; }",
      ".xtermBgColor155 { background-color: #afff5f; }",
      ".xtermColor156 { color: #afff87; }",
      ".xtermBgColor156 { background-color: #afff87; }",
      ".xtermColor157 { color: #afffaf; }",
      ".xtermBgColor157 { background-color: #afffaf; }",
      ".xtermColor158 { color: #afffd7; }",
      ".xtermBgColor158 { background-color: #afffd7; }",
      ".xtermColor159 { color: #afffff; }",
      ".xtermBgColor159 { background-color: #afffff; }",
      ".xtermColor160 { color: #d70000; }",
      ".xtermBgColor160 { background-color: #d70000; }",
      ".xtermColor161 { color: #d7005f; }",
      ".xtermBgColor161 { background-color: #d7005f; }",
      ".xtermColor162 { color: #d70087; }",
      ".xtermBgColor162 { background-color: #d70087; }",
      ".xtermColor163 { color: #d700af; }",
      ".xtermBgColor163 { background-color: #d700af; }",
      ".xtermColor164 { color: #d700d7; }",
      ".xtermBgColor164 { background-color: #d700d7; }",
      ".xtermColor165 { color: #d700ff; }",
      ".xtermBgColor165 { background-color: #d700ff; }",
      ".xtermColor166 { color: #d75f00; }",
      ".xtermBgColor166 { background-color: #d75f00; }",
      ".xtermColor167 { color: #d75f5f; }",
      ".xtermBgColor167 { background-color: #d75f5f; }",
      ".xtermColor168 { color: #d75f87; }",
      ".xtermBgColor168 { background-color: #d75f87; }",
      ".xtermColor169 { color: #d75faf; }",
      ".xtermBgColor169 { background-color: #d75faf; }",
      ".xtermColor170 { color: #d75fd7; }",
      ".xtermBgColor170 { background-color: #d75fd7; }",
      ".xtermColor171 { color: #d75fff; }",
      ".xtermBgColor171 { background-color: #d75fff; }",
      ".xtermColor172 { color: #d78700; }",
      ".xtermBgColor172 { background-color: #d78700; }",
      ".xtermColor173 { color: #d7875f; }",
      ".xtermBgColor173 { background-color: #d7875f; }",
      ".xtermColor174 { color: #d78787; }",
      ".xtermBgColor174 { background-color: #d78787; }",
      ".xtermColor175 { color: #d787af; }",
      ".xtermBgColor175 { background-color: #d787af; }",
      ".xtermColor176 { color: #d787d7; }",
      ".xtermBgColor176 { background-color: #d787d7; }",
      ".xtermColor177 { color: #d787ff; }",
      ".xtermBgColor177 { background-color: #d787ff; }",
      ".xtermColor178 { color: #d7af00; }",
      ".xtermBgColor178 { background-color: #d7af00; }",
      ".xtermColor179 { color: #d7af5f; }",
      ".xtermBgColor179 { background-color: #d7af5f; }",
      ".xtermColor180 { color: #d7af87; }",
      ".xtermBgColor180 { background-color: #d7af87; }",
      ".xtermColor181 { color: #d7afaf; }",
      ".xtermBgColor181 { background-color: #d7afaf; }",
      ".xtermColor182 { color: #d7afd7; }",
      ".xtermBgColor182 { background-color: #d7afd7; }",
      ".xtermColor183 { color: #d7afff; }",
      ".xtermBgColor183 { background-color: #d7afff; }",
      ".xtermColor184 { color: #d7d700; }",
      ".xtermBgColor184 { background-color: #d7d700; }",
      ".xtermColor185 { color: #d7d75f; }",
      ".xtermBgColor185 { background-color: #d7d75f; }",
      ".xtermColor186 { color: #d7d787; }",
      ".xtermBgColor186 { background-color: #d7d787; }",
      ".xtermColor187 { color: #d7d7af; }",
      ".xtermBgColor187 { background-color: #d7d7af; }",
      ".xtermColor188 { color: #d7d7d7; }",
      ".xtermBgColor188 { background-color: #d7d7d7; }",
      ".xtermColor189 { color: #d7d7ff; }",
      ".xtermBgColor189 { background-color: #d7d7ff; }",
      ".xtermColor190 { color: #d7ff00; }",
      ".xtermBgColor190 { background-color: #d7ff00; }",
      ".xtermColor191 { color: #d7ff5f; }",
      ".xtermBgColor191 { background-color: #d7ff5f; }",
      ".xtermColor192 { color: #d7ff87; }",
      ".xtermBgColor192 { background-color: #d7ff87; }",
      ".xtermColor193 { color: #d7ffaf; }",
      ".xtermBgColor193 { background-color: #d7ffaf; }",
      ".xtermColor194 { color: #d7ffd7; }",
      ".xtermBgColor194 { background-color: #d7ffd7; }",
      ".xtermColor195 { color: #d7ffff; }",
      ".xtermBgColor195 { background-color: #d7ffff; }",
      ".xtermColor196 { color: #ff0000; }",
      ".xtermBgColor196 { background-color: #ff0000; }",
      ".xtermColor197 { color: #ff005f; }",
      ".xtermBgColor197 { background-color: #ff005f; }",
      ".xtermColor198 { color: #ff0087; }",
      ".xtermBgColor198 { background-color: #ff0087; }",
      ".xtermColor199 { color: #ff00af; }",
      ".xtermBgColor199 { background-color: #ff00af; }",
      ".xtermColor200 { color: #ff00d7; }",
      ".xtermBgColor200 { background-color: #ff00d7; }",
      ".xtermColor201 { color: #ff00ff; }",
      ".xtermBgColor201 { background-color: #ff00ff; }",
      ".xtermColor202 { color: #ff5f00; }",
      ".xtermBgColor202 { background-color: #ff5f00; }",
      ".xtermColor203 { color: #ff5f5f; }",
      ".xtermBgColor203 { background-color: #ff5f5f; }",
      ".xtermColor204 { color: #ff5f87; }",
      ".xtermBgColor204 { background-color: #ff5f87; }",
      ".xtermColor205 { color: #ff5faf; }",
      ".xtermBgColor205 { background-color: #ff5faf; }",
      ".xtermColor206 { color: #ff5fd7; }",
      ".xtermBgColor206 { background-color: #ff5fd7; }",
      ".xtermColor207 { color: #ff5fff; }",
      ".xtermBgColor207 { background-color: #ff5fff; }",
      ".xtermColor208 { color: #ff8700; }",
      ".xtermBgColor208 { background-color: #ff8700; }",
      ".xtermColor209 { color: #ff875f; }",
      ".xtermBgColor209 { background-color: #ff875f; }",
      ".xtermColor210 { color: #ff8787; }",
      ".xtermBgColor210 { background-color: #ff8787; }",
      ".xtermColor211 { color: #ff87af; }",
      ".xtermBgColor211 { background-color: #ff87af; }",
      ".xtermColor212 { color: #ff87d7; }",
      ".xtermBgColor212 { background-color: #ff87d7; }",
      ".xtermColor213 { color: #ff87ff; }",
      ".xtermBgColor213 { background-color: #ff87ff; }",
      ".xtermColor214 { color: #ffaf00; }",
      ".xtermBgColor214 { background-color: #ffaf00; }",
      ".xtermColor215 { color: #ffaf5f; }",
      ".xtermBgColor215 { background-color: #ffaf5f; }",
      ".xtermColor216 { color: #ffaf87; }",
      ".xtermBgColor216 { background-color: #ffaf87; }",
      ".xtermColor217 { color: #ffafaf; }",
      ".xtermBgColor217 { background-color: #ffafaf; }",
      ".xtermColor218 { color: #ffafd7; }",
      ".xtermBgColor218 { background-color: #ffafd7; }",
      ".xtermColor219 { color: #ffafff; }",
      ".xtermBgColor219 { background-color: #ffafff; }",
      ".xtermColor220 { color: #ffd700; }",
      ".xtermBgColor220 { background-color: #ffd700; }",
      ".xtermColor221 { color: #ffd75f; }",
      ".xtermBgColor221 { background-color: #ffd75f; }",
      ".xtermColor222 { color: #ffd787; }",
      ".xtermBgColor222 { background-color: #ffd787; }",
      ".xtermColor223 { color: #ffd7af; }",
      ".xtermBgColor223 { background-color: #ffd7af; }",
      ".xtermColor224 { color: #ffd7d7; }",
      ".xtermBgColor224 { background-color: #ffd7d7; }",
      ".xtermColor225 { color: #ffd7ff; }",
      ".xtermBgColor225 { background-color: #ffd7ff; }",
      ".xtermColor226 { color: #ffff00; }",
      ".xtermBgColor226 { background-color: #ffff00; }",
      ".xtermColor227 { color: #ffff5f; }",
      ".xtermBgColor227 { background-color: #ffff5f; }",
      ".xtermColor228 { color: #ffff87; }",
      ".xtermBgColor228 { background-color: #ffff87; }",
      ".xtermColor229 { color: #ffffaf; }",
      ".xtermBgColor229 { background-color: #ffffaf; }",
      ".xtermColor230 { color: #ffffd7; }",
      ".xtermBgColor230 { background-color: #ffffd7; }",
      ".xtermColor231 { color: #ffffff; }",
      ".xtermBgColor231 { background-color: #ffffff; }",
      ".xtermColor232 { color: #080808; }",
      ".xtermBgColor232 { background-color: #080808; }",
      ".xtermColor233 { color: #121212; }",
      ".xtermBgColor233 { background-color: #121212; }",
      ".xtermColor234 { color: #1c1c1c; }",
      ".xtermBgColor234 { background-color: #1c1c1c; }",
      ".xtermColor235 { color: #262626; }",
      ".xtermBgColor235 { background-color: #262626; }",
      ".xtermColor236 { color: #303030; }",
      ".xtermBgColor236 { background-color: #303030; }",
      ".xtermColor237 { color: #3a3a3a; }",
      ".xtermBgColor237 { background-color: #3a3a3a; }",
      ".xtermColor238 { color: #444444; }",
      ".xtermBgColor238 { background-color: #444444; }",
      ".xtermColor239 { color: #4e4e4e; }",
      ".xtermBgColor239 { background-color: #4e4e4e; }",
      ".xtermColor240 { color: #585858; }",
      ".xtermBgColor240 { background-color: #585858; }",
      ".xtermColor241 { color: #626262; }",
      ".xtermBgColor241 { background-color: #626262; }",
      ".xtermColor242 { color: #6c6c6c; }",
      ".xtermBgColor242 { background-color: #6c6c6c; }",
      ".xtermColor243 { color: #767676; }",
      ".xtermBgColor243 { background-color: #767676; }",
      ".xtermColor244 { color: #808080; }",
      ".xtermBgColor244 { background-color: #808080; }",
      ".xtermColor245 { color: #8a8a8a; }",
      ".xtermBgColor245 { background-color: #8a8a8a; }",
      ".xtermColor246 { color: #949494; }",
      ".xtermBgColor246 { background-color: #949494; }",
      ".xtermColor247 { color: #9e9e9e; }",
      ".xtermBgColor247 { background-color: #9e9e9e; }",
      ".xtermColor248 { color: #a8a8a8; }",
      ".xtermBgColor248 { background-color: #a8a8a8; }",
      ".xtermColor249 { color: #b2b2b2; }",
      ".xtermBgColor249 { background-color: #b2b2b2; }",
      ".xtermColor250 { color: #bcbcbc; }",
      ".xtermBgColor250 { background-color: #bcbcbc; }",
      ".xtermColor251 { color: #c6c6c6; }",
      ".xtermBgColor251 { background-color: #c6c6c6; }",
      ".xtermColor252 { color: #d0d0d0; }",
      ".xtermBgColor252 { background-color: #d0d0d0; }",
      ".xtermColor253 { color: #dadada; }",
      ".xtermBgColor253 { background-color: #dadada; }",
      ".xtermColor254 { color: #e4e4e4; }",
      ".xtermBgColor254 { background-color: #e4e4e4; }",
      ".xtermColor255 { color: #eeeeee; }",
      ".xtermBgColor255 { background-color: #eeeeee; }")
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

   ## Copy ace_editor as ace_editor_theme
   regex <- paste("^\\.ace_editor \\{$", sep = "")
   content <- gsub(regex, ".ace_editor, .ace_editor_theme {", content)
   
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
   parsed <- suppressWarnings(css.parser(lines = modified))
   
   if (!any(grepl("^ace_keyword", names(parsed)))) {
      warning("No field 'ace_keyword' in file '", basename(file), "'; skipping", call. = FALSE)
      next
   }
   
   name <- grep("^ace_keyword", names(parsed), value = TRUE)[[1]]
   keywordColor <- parsed[[name]]$color
   if (is.null(keywordColor)) {
      warning("No keyword color available for file '", basename(file), "'; skipping", call. = FALSE)
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
      warning("Expected rule for '", layerName, "' in file '", basename(file), "'; skipping", call. = FALSE)
      next
   }
   
   borderField <- parsed[[layerName]]$border
   if (is.null(borderField)) {
      warning("No field for layer '", layerName, "' in file '", basename(file), "'; skipping", call. = FALSE)
      next
   }
   
   jsContents <- readLines(sub("css$", "js", file), warn = FALSE)
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
   
   # Apply other custom fixups
   content <- applyFixups(content, fileName, parsed)
   
   # Apply terminal-specifics
   content <- c(content,
                create_terminal_rule(background, foreground)) 
   content <- c(content,
                create_terminal_cursor_rules(isDark))
   content <- c(content,
                create_terminal_viewport_rule(background))
   
   # Add xterm-256 colors for colorized console output
   content <- c(content,
                create_xterm_color_rules(background, foreground, isDark)) 
   
   ## Phew! Write it out.
   outputPath <- file.path(outDir, basename(file))
   cat(content, file = outputPath, sep = "\n")
}
