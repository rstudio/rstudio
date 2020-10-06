#
#  compile-themes.R
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

.rs.addFunction("parseCss", function(lines)
{
   css <- list()
   
   # Split any lines with "\n" for proper parsing.
   cssLines <- unlist(strsplit(gsub("\\}", "\\}\n", lines), c("\n"), perl = TRUE))
   
   currKeys <-list()
   isLastDescForKey <- FALSE
   inCommentBlock <- FALSE
   candidateKey <- NULL
   for (currLine in cssLines)
   {
      orgLine <- currLine
      
      # We use this to still parse the code before the start of the comment, if any.
      startCommentBlock <- FALSE
      
      # Remove all in-line comments.
      currLine <- gsub("/\\*.*?\\*/", "", currLine)
      
      # If we're not in a comment block and the current line has a comment opener, start the comment
      # block and update the line by removing the commented section. This allows for CSS before the
      # start of a comment. Note that this comment can't be contained wholly on a single line
      # because of the gsub above this.
      if (!inCommentBlock && grepl("/\\*", currLine))
      {
         startCommentBlock <- TRUE
         currLine <- sub("/\\*.*$", "", currLine)
      }
      
      # If we're in a comment block and the current line has a comment closer, end the comment block
      # and update the line by removing the commented section. This allows for CSS after the end of
      # a comment.
      if (inCommentBlock && grepl("\\*/", currLine))
      {
         inCommentBlock <- FALSE
         currLine <- sub("^.*?\\*/", "", currLine)
      }
      
      if (!inCommentBlock)
      {
         # Check for a change of key. A "key" in this context is the name of a CSS rule block. For example, in
         # .ace_editor {
         #     font-weight: bold;
         #     color: #AAAAAA
         # }
         # ".ace_editor" is the key and "font-weight: bold" and "color: #AAAAAA" are descriptions for that key.
         if (grepl("^\\s*\\.[^\\{]+\\{", currLine))
         {
            candidateKey <- paste(
               candidateKey, 
               regmatches(
                  currLine,
                  regexec("^\\s*([^\\{]*?)\\s*\\{", currLine))[[1]][2],
               sep = " ")
            candidateKey <- gsub("^\\s*|\\s*$", "", candidateKey, perl = TRUE)
            
            if (!grepl("^\\s*$", candidateKey))
            {
               if (length(currKeys) > 0)
               {
                  warning("Malformed CSS: ", orgLine, ". No closing bracket for last block.")
               }
               currKeys <- unlist(strsplit(candidateKey, "\\s*,\\s*", perl = TRUE))
               candidateKey <- NULL
               
               for (currKey in currKeys)
               {
                  css[[currKey]] <- list()
               }
               currLine <- sub("^\\s*[^\\{]*?\\s*\\{", "", currLine)
            }
         }
         
         # If there is currently a key, look for it's descriptions.
         if (length(currKeys) > 0)
         {
            # Determine if we're at the end of the block and remove the closing bracket to be able
            # to parse anything that comes before it on the same line.
            if (grepl("\\}", currLine))
            {
               isLastDescForKey <- TRUE
               currLine <- sub("^([^\\}]*)\\}\\s*$", "\\1", currLine)
               if (grepl("\\}", currLine))
               {
                  warning("Maformed CSS: ", orgLine, ". Extra closing brackets.")
               }
            }
            
            # Handle any URL fields, because they're too complex to be parsed as below.
            urlPattern <-"\\s*(background(?:-image)?)\\s*:\\s*(url\\((?:\"[^\"]*\"|[^)]*)\\)[^();]*?)\\s*(?:;|$)"
            
            while (grepl(urlPattern, currLine, ignore.case = TRUE, perl = TRUE))
            {
               ruleName = sub(urlPattern, "\\1", currLine, ignore.case = TRUE, perl = TRUE)
               ruleValue = sub(urlPattern, "\\2", currLine, ignore.case = TRUE, perl = TRUE)
               currLine <- sub(urlPattern, "", currLine, ignore.case = TRUE, perl = TRUE)
               
               for (currKey in currKeys) css[[currKey]][[ruleName]] <- ruleValue
            }
            
            # Look for a : on the line. CSS rules always have the format "name: style", so this
            # indicates there is a description on this line..
            if (grepl(":", currLine))
            {
               # Split on the rule separator. There may be multiple descriptions on one line.
               descValues <- strsplit(currLine, "\\s*;\\s*")[[1]]
               for (value in descValues)
               {
                  # If there is a non-empty value, parse the description.
                  if (value != "")
                  {
                     # Strip whitespace and ;, if any, and then split on :.
                     desc <- strsplit(sub("^\\s*([^;]+);?\\s*$", "\\1", value), "\\s*:\\s*")[[1]]
                     if (length(desc) != 2)
                     {
                        warning("Malformed CSS: ", orgLine, ". Invalid element within block.")
                     }
                     else
                     {
                        for (currKey in currKeys) css[[currKey]][[ desc[1] ]] <- tolower(desc[2])
                     }
                  }
               }
            }
            # If the line doesn't contain a description and is non-empty, it is malformed.
            else if (!grepl("^\\s*$", currLine))
            {
               warning("Malformd CSS: ", orgLine, ". Unexpected non-css line.")
            }
         }
         else if (!grepl("^\\s*$", currLine))
         {
            if (is.null(candidateKey))
            {
               candidateKey <- currLine
            }
            else
            {
               # selectors for CSS rule blocks may be split over multiple lines.
               candidateKey <- paste(candidateKey, currLine)
            }
         }
         
         # If we've finished a block, reset the key and the block-end indicator.
         if (isLastDescForKey)
         {
            currKeys <- list()
            isLastDescForKey <- FALSE
         }
         
         # If we started a comment block this line, update the inCommentBlock status so we ignore
         # all lines until the end of the comment block is found.
         if (startCommentBlock)
         {
            inCommentBlock <- TRUE
         }
      }
   }
   
   css
})

.rs.addFunction("add_operator_color", function(content, name, isDark = FALSE, overrideMap = list()) {
   color <- overrideMap[[name]]
   
   if (is.null(color))
      color <- if (isDark) "#AAAAAA" else "#888888"
   
   .rs.add_content(
      content,
      ".ace_keyword.ace_operator {",
      "  color: %s !important;",
      "}",
      replace = color
   )
})

.rs.addFunction("add_keyword_color", function(content, name, overrideMap) {
   .rs.add_content(
      content,
      ".ace_keyword {",
      "  color: %s !important;",
      "}",
      replace = overrideMap[[name]]
   )
})

# Converts a color to an array of the RGB values of the color.
#
# @param color    The color to convert.
#
# Returns the RGB color.
.rs.addFunction("getRgbColor", function(color) {

   # Named CSS colors we can decode. Source:
   # https://developer.mozilla.org/en-US/docs/Web/CSS/color_value
   namedCssColors <- list(
      black                  = c(0L, 0L, 0L),
      silver                 = c(192L, 192L, 192L),
      gray                   = c(128L, 128L, 128L),
      white                  = c(255L, 255L, 255L),
      maroon                 = c(128L, 0L, 0L),
      red                    = c(255L, 0L, 0L),
      purple                 = c(128L, 0L, 128L),
      fuchsia                = c(255L, 0L, 255L),
      green                  = c(0L, 128L, 0L),
      lime                   = c(0L, 255L, 0L),
      olive                  = c(128L, 128L, 0L),
      yellow                 = c(255L, 255L, 0L),
      navy                   = c(0L, 0L, 128L),
      blue                   = c(0L, 0L, 255L),
      teal                   = c(0L, 128L, 128L),
      aqua                   = c(0L, 255L, 255L),
      orange                 = c(255L, 165L, 0L),
      aliceblue              = c(240L, 248L, 255L),
      antiquewhite           = c(250L, 235L, 215L),
      aquamarine             = c(127L, 255L, 212L),
      azure                  = c(240L, 255L, 255L),
      beige                  = c(245L, 245L, 220L),
      bisque                 = c(255L, 228L, 196L),
      blanchedalmond         = c(255L, 235L, 205L),
      blueviolet             = c(138L, 43L, 226L),
      brown                  = c(165L, 42L, 42L),
      burlywood              = c(222L, 184L, 135L),
      cadetblue              = c(95L, 158L, 160L),
      chartreuse             = c(127L, 255L, 0L),
      chocolate              = c(210L, 105L, 30L),
      coral                  = c(255L, 127L, 80L),
      cornflowerblue         = c(100L, 149L, 237L),
      cornsilk               = c(255L, 248L, 220L),
      crimson                = c(220L, 20L, 60L),
      cyan                   = c(0L, 255L, 255L),
      darkblue               = c(0L, 0L, 139L),
      darkcyan               = c(0L, 139L, 139L),
      darkgoldenrod          = c(184L, 134L, 11L),
      darkgray               = c(169L, 169L, 169L),
      darkgreen              = c(0L, 100L, 0L),
      darkgrey               = c(169L, 169L, 169L),
      darkkhaki              = c(189L, 183L, 107L),
      darkmagenta            = c(139L, 0L, 139L),
      darkolivegreen         = c(85L, 107L, 47L),
      darkorange             = c(255L, 140L, 0L),
      darkorchid             = c(153L, 50L, 204L),
      darkred                = c(139L, 0L, 0L),
      darksalmon             = c(233L, 150L, 122L),
      darkseagreen           = c(143L, 188L, 143L),
      darkslateblue          = c(72L, 61L, 139L),
      darkslategray          = c(47L, 79L, 79L),
      darkslategrey          = c(47L, 79L, 79L),
      darkturquoise          = c(0L, 206L, 209L),
      darkviolet             = c(148L, 0L, 211L),
      deeppink               = c(255L, 20L, 147L),
      deepskyblue            = c(0L, 191L, 255L),
      dimgray                = c(105L, 105L, 105L),
      dimgrey                = c(105L, 105L, 105L),
      dodgerblue             = c(30L, 144L, 255L),
      firebrick              = c(178L, 34L, 34L),
      floralwhite            = c(255L, 250L, 240L),
      forestgreen            = c(34L, 139L, 34L),
      gainsboro              = c(220L, 220L, 220L),
      ghostwhite             = c(248L, 248L, 255L),
      gold                   = c(255L, 215L, 0L),
      goldenrod              = c(218L, 165L, 32L),
      greenyellow            = c(173L, 255L, 47L),
      grey                   = c(128L, 128L, 128L),
      honeydew               = c(240L, 255L, 240L),
      hotpink                = c(255L, 105L, 180L),
      indianred              = c(205L, 92L, 92L),
      indigo                 = c(75L, 0L, 130L),
      ivory                  = c(255L, 255L, 240L),
      khaki                  = c(240L, 230L, 140L),
      lavender               = c(230L, 230L, 250L),
      lavenderblush          = c(255L, 240L, 245L),
      lawngreen              = c(124L, 252L, 0L),
      lemonchiffon           = c(255L, 250L, 205L),
      lightblue              = c(173L, 216L, 230L),
      lightcoral             = c(240L, 128L, 128L),
      lightcyan              = c(224L, 255L, 255L),
      lightgoldenrodyellow   = c(250L, 250L, 210L),
      lightgray              = c(211L, 211L, 211L),
      lightgreen             = c(144L, 238L, 144L),
      lightgrey              = c(211L, 211L, 211L),
      lightpink              = c(255L, 182L, 193L),
      lightsalmon            = c(255L, 160L, 122L),
      lightseagreen          = c(32L, 178L, 170L),
      lightskyblue           = c(135L, 206L, 250L),
      lightslategray         = c(119L, 136L, 153L),
      lightslategrey         = c(119L, 136L, 153L),
      lightsteelblue         = c(176L, 196L, 222L),
      lightyellow            = c(255L, 255L, 224L),
      limegreen              = c(50L, 205L, 50L),
      linen                  = c(250L, 240L, 230L),
      magenta                = c(255L, 0L, 255L),
      mediumaquamarine       = c(102L, 205L, 170L),
      mediumblue             = c(0L, 0L, 205L),
      mediumorchid           = c(186L, 85L, 211L),
      mediumpurple           = c(147L, 112L, 219L),
      mediumseagreen         = c(60L, 179L, 113L),
      mediumslateblue        = c(123L, 104L, 238L),
      mediumspringgreen      = c(0L, 250L, 154L),
      mediumturquoise        = c(72L, 209L, 204L),
      mediumvioletred        = c(199L, 21L, 133L),
      midnightblue           = c(25L, 25L, 112L),
      mintcream              = c(245L, 255L, 250L),
      mistyrose              = c(255L, 228L, 225L),
      moccasin               = c(255L, 228L, 181L),
      navajowhite            = c(255L, 222L, 173L),
      oldlace                = c(253L, 245L, 230L),
      olivedrab              = c(107L, 142L, 35L),
      orangered              = c(255L, 69L, 0L),
      orchid                 = c(218L, 112L, 214L),
      palegoldenrod          = c(238L, 232L, 170L),
      palegreen              = c(152L, 251L, 152L),
      paleturquoise          = c(175L, 238L, 238L),
      palevioletred          = c(219L, 112L, 147L),
      papayawhip             = c(255L, 239L, 213L),
      peachpuff              = c(255L, 218L, 185L),
      peru                   = c(205L, 133L, 63L),
      pink                   = c(255L, 192L, 203L),
      plum                   = c(221L, 160L, 221L),
      powderblue             = c(176L, 224L, 230L),
      rosybrown              = c(188L, 143L, 143L),
      royalblue              = c(65L, 105L, 225L),
      saddlebrown            = c(139L, 69L, 19L),
      salmon                 = c(250L, 128L, 114L),
      sandybrown             = c(244L, 164L, 96L),
      seagreen               = c(46L, 139L, 87L),
      seashell               = c(255L, 245L, 238L),
      sienna                 = c(160L, 82L, 45L),
      skyblue                = c(135L, 206L, 235L),
      slateblue              = c(106L, 90L, 205L),
      slategray              = c(112L, 128L, 144L),
      slategrey              = c(112L, 128L, 144L),
      snow                   = c(255L, 250L, 250L),
      springgreen            = c(0L, 255L, 127L),
      steelblue              = c(70L, 130L, 180L),
      tan                    = c(210L, 180L, 140L),
      thistle                = c(216L, 191L, 216L),
      tomato                 = c(255L, 99L, 71L),
      turquoise              = c(64L, 224L, 208L),
      violet                 = c(238L, 130L, 238L),
      wheat                  = c(245L, 222L, 179L),
      whitesmoke             = c(245L, 245L, 245L),
      yellowgreen            = c(154L, 205L, 50L),
      rebeccapurple          = c(102L, 51L, 153L)
   )

   if (is.vector(color) && any(is.integer(color)))
   {
      if (length(color) != 3)
      {
         stop(
            "expected 3 values for RGB color, not ",
            length(color),
            call. = FALSE)
      }
      colorVec <- color
   }
   else if (substr(color, 0, 1) == "#") 
   {
      if (nchar(color) != 7)
      {
         stop(
            "hex representation of RGB values should have the format \"#RRGGBB\", where `RR`, `GG` and `BB` are in [0x00, 0xFF]. Found: ",
            color,
            call. = FALSE)
      }
      else
      {
         colorVec <- sapply(
            c(substr(color, 2, 3), substr(color, 4, 5), substr(color, 6, 7)),
            function(str) { strtoi(str, 16L) },
            USE.NAMES = FALSE)
      }
   }
   else if (grepl("^rgba?", color))
   {
      matches = regmatches(color, regexec("\\(([^,\\)]+),([^,\\)]+),([^,\\)]+)", color))[[1]]
      if (length(matches) != 4)
      {
         stop(
            "non-hex representation of RGB values should have the format \"rgb(R, G, B)\" or \"rgba(R, G, B, A)\" where `R`, `G`, and `B` are integer values in [0, 255] and `A` is decimal value in [0, 1.0]. Found: ",
            color,
            call. = FALSE)
      }
      colorVec <- strtoi(matches[2:4])
   }
   else if (color %in% names(namedCssColors))
   {
      colorVec <- namedCssColors[[color]]
   }
   else
   {
      stop(
         "supplied color has an invalid format: ",
         color,
         ". Expected \"#RRGGBB\", \"rgb(R, G, B) or \"rgba(R, G, B, A)\", where `RR`, `GG` and `BB` are in [0x00, 0xFF], `R`, `G`, and `B` are integer values in [0, 255], and `A` is decimal value in [0, 1.0]",
         call. = FALSE)
   }
   
   # Check for inconsistencies.
   invalidMsg <- paste0("invalid color supplied: ", color, ". ")
   if (any(is.na(colorVec)) || any(!is.integer(colorVec)))
   {
      stop(
         invalidMsg,
         "One or more RGB values could not be converted to an integer",
         call. = FALSE)
   }
   if (any(colorVec < 0))
   {
      stop(invalidMsg, "RGB value cannot be negative", call. = FALSE)
   }
   if (any(colorVec > 255))
   {
      stop(invalidMsg, "RGB value cannot be greater than 255", call. = FALSE)
   }
   
   colorVec
})

.rs.addFunction("format_css_color", function(color) {
   sprintf("rgb(%s, %s, %s)",
           color[1],
           color[2],
           color[3])
})

.rs.addFunction("color_as_hex", function(color) {
   paste("#", paste(format(as.hexmode(as.integer(color[1:3])), upper.case = TRUE, width = 2), collapse = ""), sep = "")
})

# Strip color from field
.rs.addFunction("strip_color_from_field", function(css) {
   if (grepl("rgb", css)) {
      gsub(".*rgb", "rgb", css)
   } else {
      gsub(".*#", "#", css)
   }
})

# Mixes two colors together.
#
# @param color1   The first color.
# @param color2   The second color.
# @param alpha1   The alpha of the first color.
# @param alpha2   The alpha of the second color.
# 
# Returns the mixed color in string format.
.rs.addFunction("mixColors", function(color1, color2, alpha1, alpha2 = NULL) {
   c1rgb <- .rs.getRgbColor(color1)
   c2rgb <- .rs.getRgbColor(color2)
   
   if (is.null(alpha2))
   {
      alpha2 = 1 - alpha1
   }
   
   .rs.color_as_hex(
      c(
         ceiling(alpha1 * c1rgb[[1]] + alpha2 * c2rgb[[1]]),
         ceiling(alpha1 * c1rgb[[2]] + alpha2 * c2rgb[[2]]),
         ceiling(alpha1 * c1rgb[[3]] + alpha2 * c2rgb[[3]]),
         sep = ","))
})

.rs.addFunction("add_content", function(content, ..., replace)
   c(
      content,
      do.call(sprintf, list(paste(..., sep = "\n"), replace))
   ))

.rs.addFunction("create_line_marker_rule", function(markerName, markerColor) {
   sprintf(paste(sep = "\n",
                 ".ace_marker-layer %s {",
                 "  position: absolute;",
                 "  z-index: -1;",
                 "  background-color: %s;",
                 "}"),
           markerName,
           markerColor)
})

.rs.addFunction("createNodeSelectorRule", function(themeName, isDark, overrides = list()) {
   nodeSelectorColor <- overrides[[themeName]]
   if (is.null(nodeSelectorColor))
      nodeSelectorColor <- if (isDark) "#fb6f1c" else "rgb(102, 155, 243)"
   
   sprintf(paste(sep = "\n",
                 ".ace_node-selector {",
                 "  background-color: %s",
                 "}"),
           nodeSelectorColor)
})

.rs.addFunction("createCommentBgRule", function(themeName, isDark, overrides = list()) {
   commentFgColor <- if (isDark) "#4D4333" else "#3C4C72"
   commentBgColor <- if (isDark) "#D1B78A" else "#FFECCB"
   
   override <- overrides[[themeName]]
   if (!is.null(override) && !is.null(override$fg))
      commentFgColor <- override$fg
   
   if (!is.null(override) && !is.null(override$bg))
      commentBgColor <- override$bg
   
   sprintf(paste(sep = "\n",
                 ".ace_comment-highlight {",
                 "  color: %s;",
                 "  background-color: %s;",
                 "}"),
           commentFgColor,
           commentBgColor)
})

.rs.addFunction("get_chunk_bg_color", function(themeName, isDark, overrides = list()) {
   p <- overrides[[themeName]]
   if (is.null(p))
      if (isDark) 0.9 else 0.95
   else
      p
})

.rs.addFunction("findNext", function(regex, content, start = 1, end = length(content)) {
   matches <- grep(regex, content, perl = TRUE)
   matches[(matches > start) & (matches < end)][1]
})

.rs.addFunction("updateSetting", function(content, newValue, cssClass, settingName) {
   classPat <- paste0("\\.", cssClass, "( *){")
   if (any(grepl(classPat, content, perl = TRUE)))
   {
      settingName <- paste0(settingName, ":")
      blockLoc <- grep(classPat, content, perl = TRUE)
      
      newLine <-  paste0("  ", settingName, " ", newValue, ";")
      settingPat <- paste0("(^| )",settingName)
      if (!any(grepl(settingPat, content, perl = TRUE)))
      {
         settingLoc <- grep("}",  content, perl = TRUE)
         
         loc <- settingLoc[settingLoc > blockLoc][1]
         content <- c(content[1:loc-1], newLine, content[loc:length(content)])
      }
      else
      {
         settingLoc <- grep(settingPat,  content, perl = TRUE)
      
         loc <- settingLoc[settingLoc > blockLoc][1]
         content[loc] <- newLine
      }
   }
   content
})

.rs.addFunction("setPrintMarginColor", function(content, color) {
   .rs.updateSetting(content, color, "ace_print-margin", "background")
})

.rs.addFunction("setActiveDebugLineColor", function(content, color) {
   .rs.updateSetting(content, color, "ace_active_debug_line", "background-color")
})

.rs.addFunction("setSelectionStartBorderRadius", function(content) {
   .rs.updateSetting(content, "2px", "ace_selection.ace_start", "border-radius")
})

.rs.addFunction("create_terminal_cursor_rules", function(isDark) {
   termCursorBgColorFirst <-  "#333"
   termCursorBgColorSecond <- "#CCC"
   if (isDark) {
      termCursorBgColorFirst <-  "#CCC"
      termCursorBgColorSecond <- "#1e1e1e"
   }
   
   sprintf(paste(sep = "\n",
                 ".terminal.xterm-cursor-style-block.focus:not(.xterm-cursor-blink-on) .terminal-cursor {",
                 "  background-color: %s;",
                 "  color: %s;",
                 "}",
                 ".terminal.focus.xterm-cursor-style-bar:not(.xterm-cursor-blink-on) .terminal-cursor::before,",
                 ".terminal.focus.xterm-cursor-style-underline:not(.xterm-cursor-blink-on) .terminal-cursor::before {",
                 "  content: \'\';",
                 "  position: absolute;",
                 "  background-color: %s;",
                 "}",
                 ".terminal:not(.focus) .terminal-cursor {",
                 "  outline: 1px solid %s;",
                 "  outline-offset: -1px;",
                 "}",
                 ".terminal .xterm-selection div {",
                 "   position: absolute;",
                 "   background-color: %s;",
                 "}"),
           
           # .terminal.xterm-cursor-style-block.focus:not(.xterm-cursor-blink-on) .terminal-cursor
           termCursorBgColorFirst,
           termCursorBgColorSecond,
           
           # .terminal.focus.xterm-cursor-style-bar:not(.xterm-cursor-blink-on) .terminal-cursor::before,
           # .terminal.focus.xterm-cursor-style-underline:not(.xterm-cursor-blink-on) .terminal-cursor::before
           termCursorBgColorFirst,
           
           # .terminal:not(.focus) .terminal-cursor
           termCursorBgColorFirst,
           
           # .terminal .xterm-selection div
           termCursorBgColorFirst)
})

.rs.addFunction("create_terminal_rule", function(background, foreground) {
   sprintf(paste(sep = "\n",
                 ".terminal {",
                 "  background-color: %s;",
                 "  color: %s;",
                 "  font-feature-settings: \"liga\" 0;",
                 "  position: relative;",
                 "  user-select: none;",
                 "  -ms-user-select: none;",
                 "  -webkit-user-select: none;",
                 "}"),
           background, foreground)
})

.rs.addFunction("create_terminal_viewport_rule", function(background) {
   sprintf(paste(sep = "\n",
                 ".terminal .xterm-viewport {",
                 "  background-color: %s;",
                 "  overflow-y: scroll;",
                 "}"),
           background)
})

.rs.addFunction("create_xterm_color_rules", function(background, foreground, isDark) {
   paste(sep = "\n",
         sprintf(".xtermInvertColor { color: %s; }", background),
         sprintf(".xtermInvertBgColor { background-color: %s; }", foreground),
         ".xtermBold { font-weight: bold; }",
         ".xtermUnderline { text-decoration: underline; }",
         ".xtermBlink { text-decoration: blink; }",
         ".xtermHidden { visibility: hidden; }",
         ".xtermItalic { font-style: italic; }",
         ".xtermStrike { text-decoration: line-through; }",
         ".xtermColor0 { color: #2e3436 !important; }",
         ".xtermBgColor0 { background-color: #2e3436; }",
         ".xtermColor1 { color: #cc0000 !important; }",
         ".xtermBgColor1 { background-color: #cc0000; }",
         ".xtermColor2 { color: #4e9a06 !important; }",
         ".xtermBgColor2 { background-color: #4e9a06; }",
         ".xtermColor3 { color: #c4a000 !important; }",
         ".xtermBgColor3 { background-color: #c4a000; }",
         ".xtermColor4 { color: #3465a4 !important; }",
         ".xtermBgColor4 { background-color: #3465a4; }",
         ".xtermColor5 { color: #75507b !important; }",
         ".xtermBgColor5 { background-color: #75507b; }",
         ".xtermColor6 { color: #06989a !important; }",
         ".xtermBgColor6 { background-color: #06989a; }",
         ".xtermColor7 { color: #d3d7cf !important; }",
         ".xtermBgColor7 { background-color: #d3d7cf; }",
         ".xtermColor8 { color: #555753 !important; }",
         ".xtermBgColor8 { background-color: #555753; }",
         ".xtermColor9 { color: #ef2929 !important; }",
         ".xtermBgColor9 { background-color: #ef2929; }",
         ".xtermColor10 { color: #8ae234 !important; }",
         ".xtermBgColor10 { background-color: #8ae234; }",
         ".xtermColor11 { color: #fce94f !important; }",
         ".xtermBgColor11 { background-color: #fce94f; }",
         ".xtermColor12 { color: #729fcf !important; }",
         ".xtermBgColor12 { background-color: #729fcf; }",
         ".xtermColor13 { color: #ad7fa8 !important; }",
         ".xtermBgColor13 { background-color: #ad7fa8; }",
         ".xtermColor14 { color: #34e2e2 !important; }",
         ".xtermBgColor14 { background-color: #34e2e2; }",
         ".xtermColor15 { color: #eeeeec !important; }",
         ".xtermBgColor15 { background-color: #eeeeec; }",
         ".xtermColor16 { color: #000000 !important; }",
         ".xtermBgColor16 { background-color: #000000; }",
         ".xtermColor17 { color: #00005f !important; }",
         ".xtermBgColor17 { background-color: #00005f; }",
         ".xtermColor18 { color: #000087 !important; }",
         ".xtermBgColor18 { background-color: #000087; }",
         ".xtermColor19 { color: #0000af !important; }",
         ".xtermBgColor19 { background-color: #0000af; }",
         ".xtermColor20 { color: #0000d7 !important; }",
         ".xtermBgColor20 { background-color: #0000d7; }",
         ".xtermColor21 { color: #0000ff !important; }",
         ".xtermBgColor21 { background-color: #0000ff; }",
         ".xtermColor22 { color: #005f00 !important; }",
         ".xtermBgColor22 { background-color: #005f00; }",
         ".xtermColor23 { color: #005f5f !important; }",
         ".xtermBgColor23 { background-color: #005f5f; }",
         ".xtermColor24 { color: #005f87 !important; }",
         ".xtermBgColor24 { background-color: #005f87; }",
         ".xtermColor25 { color: #005faf !important; }",
         ".xtermBgColor25 { background-color: #005faf; }",
         ".xtermColor26 { color: #005fd7 !important; }",
         ".xtermBgColor26 { background-color: #005fd7; }",
         ".xtermColor27 { color: #005fff !important; }",
         ".xtermBgColor27 { background-color: #005fff; }",
         ".xtermColor28 { color: #008700 !important; }",
         ".xtermBgColor28 { background-color: #008700; }",
         ".xtermColor29 { color: #00875f !important; }",
         ".xtermBgColor29 { background-color: #00875f; }",
         ".xtermColor30 { color: #008787 !important; }",
         ".xtermBgColor30 { background-color: #008787; }",
         ".xtermColor31 { color: #0087af !important; }",
         ".xtermBgColor31 { background-color: #0087af; }",
         ".xtermColor32 { color: #0087d7 !important; }",
         ".xtermBgColor32 { background-color: #0087d7; }",
         ".xtermColor33 { color: #0087ff !important; }",
         ".xtermBgColor33 { background-color: #0087ff; }",
         ".xtermColor34 { color: #00af00 !important; }",
         ".xtermBgColor34 { background-color: #00af00; }",
         ".xtermColor35 { color: #00af5f !important; }",
         ".xtermBgColor35 { background-color: #00af5f; }",
         ".xtermColor36 { color: #00af87 !important; }",
         ".xtermBgColor36 { background-color: #00af87; }",
         ".xtermColor37 { color: #00afaf !important; }",
         ".xtermBgColor37 { background-color: #00afaf; }",
         ".xtermColor38 { color: #00afd7 !important; }",
         ".xtermBgColor38 { background-color: #00afd7; }",
         ".xtermColor39 { color: #00afff !important; }",
         ".xtermBgColor39 { background-color: #00afff; }",
         ".xtermColor40 { color: #00d700 !important; }",
         ".xtermBgColor40 { background-color: #00d700; }",
         ".xtermColor41 { color: #00d75f !important; }",
         ".xtermBgColor41 { background-color: #00d75f; }",
         ".xtermColor42 { color: #00d787 !important; }",
         ".xtermBgColor42 { background-color: #00d787; }",
         ".xtermColor43 { color: #00d7af !important; }",
         ".xtermBgColor43 { background-color: #00d7af; }",
         ".xtermColor44 { color: #00d7d7 !important; }",
         ".xtermBgColor44 { background-color: #00d7d7; }",
         ".xtermColor45 { color: #00d7ff !important; }",
         ".xtermBgColor45 { background-color: #00d7ff; }",
         ".xtermColor46 { color: #00ff00 !important; }",
         ".xtermBgColor46 { background-color: #00ff00; }",
         ".xtermColor47 { color: #00ff5f !important; }",
         ".xtermBgColor47 { background-color: #00ff5f; }",
         ".xtermColor48 { color: #00ff87 !important; }",
         ".xtermBgColor48 { background-color: #00ff87; }",
         ".xtermColor49 { color: #00ffaf !important; }",
         ".xtermBgColor49 { background-color: #00ffaf; }",
         ".xtermColor50 { color: #00ffd7 !important; }",
         ".xtermBgColor50 { background-color: #00ffd7; }",
         ".xtermColor51 { color: #00ffff !important; }",
         ".xtermBgColor51 { background-color: #00ffff; }",
         ".xtermColor52 { color: #5f0000 !important; }",
         ".xtermBgColor52 { background-color: #5f0000; }",
         ".xtermColor53 { color: #5f005f !important; }",
         ".xtermBgColor53 { background-color: #5f005f; }",
         ".xtermColor54 { color: #5f0087 !important; }",
         ".xtermBgColor54 { background-color: #5f0087; }",
         ".xtermColor55 { color: #5f00af !important; }",
         ".xtermBgColor55 { background-color: #5f00af; }",
         ".xtermColor56 { color: #5f00d7 !important; }",
         ".xtermBgColor56 { background-color: #5f00d7; }",
         ".xtermColor57 { color: #5f00ff !important; }",
         ".xtermBgColor57 { background-color: #5f00ff; }",
         ".xtermColor58 { color: #5f5f00 !important; }",
         ".xtermBgColor58 { background-color: #5f5f00; }",
         ".xtermColor59 { color: #5f5f5f !important; }",
         ".xtermBgColor59 { background-color: #5f5f5f; }",
         ".xtermColor60 { color: #5f5f87 !important; }",
         ".xtermBgColor60 { background-color: #5f5f87; }",
         ".xtermColor61 { color: #5f5faf !important; }",
         ".xtermBgColor61 { background-color: #5f5faf; }",
         ".xtermColor62 { color: #5f5fd7 !important; }",
         ".xtermBgColor62 { background-color: #5f5fd7; }",
         ".xtermColor63 { color: #5f5fff !important; }",
         ".xtermBgColor63 { background-color: #5f5fff; }",
         ".xtermColor64 { color: #5f8700 !important; }",
         ".xtermBgColor64 { background-color: #5f8700; }",
         ".xtermColor65 { color: #5f875f !important; }",
         ".xtermBgColor65 { background-color: #5f875f; }",
         ".xtermColor66 { color: #5f8787 !important; }",
         ".xtermBgColor66 { background-color: #5f8787; }",
         ".xtermColor67 { color: #5f87af !important; }",
         ".xtermBgColor67 { background-color: #5f87af; }",
         ".xtermColor68 { color: #5f87d7 !important; }",
         ".xtermBgColor68 { background-color: #5f87d7; }",
         ".xtermColor69 { color: #5f87ff !important; }",
         ".xtermBgColor69 { background-color: #5f87ff; }",
         ".xtermColor70 { color: #5faf00 !important; }",
         ".xtermBgColor70 { background-color: #5faf00; }",
         ".xtermColor71 { color: #5faf5f !important; }",
         ".xtermBgColor71 { background-color: #5faf5f; }",
         ".xtermColor72 { color: #5faf87 !important; }",
         ".xtermBgColor72 { background-color: #5faf87; }",
         ".xtermColor73 { color: #5fafaf !important; }",
         ".xtermBgColor73 { background-color: #5fafaf; }",
         ".xtermColor74 { color: #5fafd7 !important; }",
         ".xtermBgColor74 { background-color: #5fafd7; }",
         ".xtermColor75 { color: #5fafff !important; }",
         ".xtermBgColor75 { background-color: #5fafff; }",
         ".xtermColor76 { color: #5fd700 !important; }",
         ".xtermBgColor76 { background-color: #5fd700; }",
         ".xtermColor77 { color: #5fd75f !important; }",
         ".xtermBgColor77 { background-color: #5fd75f; }",
         ".xtermColor78 { color: #5fd787 !important; }",
         ".xtermBgColor78 { background-color: #5fd787; }",
         ".xtermColor79 { color: #5fd7af !important; }",
         ".xtermBgColor79 { background-color: #5fd7af; }",
         ".xtermColor80 { color: #5fd7d7 !important; }",
         ".xtermBgColor80 { background-color: #5fd7d7; }",
         ".xtermColor81 { color: #5fd7ff !important; }",
         ".xtermBgColor81 { background-color: #5fd7ff; }",
         ".xtermColor82 { color: #5fff00 !important; }",
         ".xtermBgColor82 { background-color: #5fff00; }",
         ".xtermColor83 { color: #5fff5f !important; }",
         ".xtermBgColor83 { background-color: #5fff5f; }",
         ".xtermColor84 { color: #5fff87 !important; }",
         ".xtermBgColor84 { background-color: #5fff87; }",
         ".xtermColor85 { color: #5fffaf !important; }",
         ".xtermBgColor85 { background-color: #5fffaf; }",
         ".xtermColor86 { color: #5fffd7 !important; }",
         ".xtermBgColor86 { background-color: #5fffd7; }",
         ".xtermColor87 { color: #5fffff !important; }",
         ".xtermBgColor87 { background-color: #5fffff; }",
         ".xtermColor88 { color: #870000 !important; }",
         ".xtermBgColor88 { background-color: #870000; }",
         ".xtermColor89 { color: #87005f !important; }",
         ".xtermBgColor89 { background-color: #87005f; }",
         ".xtermColor90 { color: #870087 !important; }",
         ".xtermBgColor90 { background-color: #870087; }",
         ".xtermColor91 { color: #8700af !important; }",
         ".xtermBgColor91 { background-color: #8700af; }",
         ".xtermColor92 { color: #8700d7 !important; }",
         ".xtermBgColor92 { background-color: #8700d7; }",
         ".xtermColor93 { color: #8700ff !important; }",
         ".xtermBgColor93 { background-color: #8700ff; }",
         ".xtermColor94 { color: #875f00 !important; }",
         ".xtermBgColor94 { background-color: #875f00; }",
         ".xtermColor95 { color: #875f5f !important; }",
         ".xtermBgColor95 { background-color: #875f5f; }",
         ".xtermColor96 { color: #875f87 !important; }",
         ".xtermBgColor96 { background-color: #875f87; }",
         ".xtermColor97 { color: #875faf !important; }",
         ".xtermBgColor97 { background-color: #875faf; }",
         ".xtermColor98 { color: #875fd7 !important; }",
         ".xtermBgColor98 { background-color: #875fd7; }",
         ".xtermColor99 { color: #875fff !important; }",
         ".xtermBgColor99 { background-color: #875fff; }",
         ".xtermColor100 { color: #878700 !important; }",
         ".xtermBgColor100 { background-color: #878700; }",
         ".xtermColor101 { color: #87875f !important; }",
         ".xtermBgColor101 { background-color: #87875f; }",
         ".xtermColor102 { color: #878787 !important; }",
         ".xtermBgColor102 { background-color: #878787; }",
         ".xtermColor103 { color: #8787af !important; }",
         ".xtermBgColor103 { background-color: #8787af; }",
         ".xtermColor104 { color: #8787d7 !important; }",
         ".xtermBgColor104 { background-color: #8787d7; }",
         ".xtermColor105 { color: #8787ff !important; }",
         ".xtermBgColor105 { background-color: #8787ff; }",
         ".xtermColor106 { color: #87af00 !important; }",
         ".xtermBgColor106 { background-color: #87af00; }",
         ".xtermColor107 { color: #87af5f !important; }",
         ".xtermBgColor107 { background-color: #87af5f; }",
         ".xtermColor108 { color: #87af87 !important; }",
         ".xtermBgColor108 { background-color: #87af87; }",
         ".xtermColor109 { color: #87afaf !important; }",
         ".xtermBgColor109 { background-color: #87afaf; }",
         ".xtermColor110 { color: #87afd7 !important; }",
         ".xtermBgColor110 { background-color: #87afd7; }",
         ".xtermColor111 { color: #87afff !important; }",
         ".xtermBgColor111 { background-color: #87afff; }",
         ".xtermColor112 { color: #87d700 !important; }",
         ".xtermBgColor112 { background-color: #87d700; }",
         ".xtermColor113 { color: #87d75f !important; }",
         ".xtermBgColor113 { background-color: #87d75f; }",
         ".xtermColor114 { color: #87d787 !important; }",
         ".xtermBgColor114 { background-color: #87d787; }",
         ".xtermColor115 { color: #87d7af !important; }",
         ".xtermBgColor115 { background-color: #87d7af; }",
         ".xtermColor116 { color: #87d7d7 !important; }",
         ".xtermBgColor116 { background-color: #87d7d7; }",
         ".xtermColor117 { color: #87d7ff !important; }",
         ".xtermBgColor117 { background-color: #87d7ff; }",
         ".xtermColor118 { color: #87ff00 !important; }",
         ".xtermBgColor118 { background-color: #87ff00; }",
         ".xtermColor119 { color: #87ff5f !important; }",
         ".xtermBgColor119 { background-color: #87ff5f; }",
         ".xtermColor120 { color: #87ff87 !important; }",
         ".xtermBgColor120 { background-color: #87ff87; }",
         ".xtermColor121 { color: #87ffaf !important; }",
         ".xtermBgColor121 { background-color: #87ffaf; }",
         ".xtermColor122 { color: #87ffd7 !important; }",
         ".xtermBgColor122 { background-color: #87ffd7; }",
         ".xtermColor123 { color: #87ffff !important; }",
         ".xtermBgColor123 { background-color: #87ffff; }",
         ".xtermColor124 { color: #af0000 !important; }",
         ".xtermBgColor124 { background-color: #af0000; }",
         ".xtermColor125 { color: #af005f !important; }",
         ".xtermBgColor125 { background-color: #af005f; }",
         ".xtermColor126 { color: #af0087 !important; }",
         ".xtermBgColor126 { background-color: #af0087; }",
         ".xtermColor127 { color: #af00af !important; }",
         ".xtermBgColor127 { background-color: #af00af; }",
         ".xtermColor128 { color: #af00d7 !important; }",
         ".xtermBgColor128 { background-color: #af00d7; }",
         ".xtermColor129 { color: #af00ff !important; }",
         ".xtermBgColor129 { background-color: #af00ff; }",
         ".xtermColor130 { color: #af5f00 !important; }",
         ".xtermBgColor130 { background-color: #af5f00; }",
         ".xtermColor131 { color: #af5f5f !important; }",
         ".xtermBgColor131 { background-color: #af5f5f; }",
         ".xtermColor132 { color: #af5f87 !important; }",
         ".xtermBgColor132 { background-color: #af5f87; }",
         ".xtermColor133 { color: #af5faf !important; }",
         ".xtermBgColor133 { background-color: #af5faf; }",
         ".xtermColor134 { color: #af5fd7 !important; }",
         ".xtermBgColor134 { background-color: #af5fd7; }",
         ".xtermColor135 { color: #af5fff !important; }",
         ".xtermBgColor135 { background-color: #af5fff; }",
         ".xtermColor136 { color: #af8700 !important; }",
         ".xtermBgColor136 { background-color: #af8700; }",
         ".xtermColor137 { color: #af875f !important; }",
         ".xtermBgColor137 { background-color: #af875f; }",
         ".xtermColor138 { color: #af8787 !important; }",
         ".xtermBgColor138 { background-color: #af8787; }",
         ".xtermColor139 { color: #af87af !important; }",
         ".xtermBgColor139 { background-color: #af87af; }",
         ".xtermColor140 { color: #af87d7 !important; }",
         ".xtermBgColor140 { background-color: #af87d7; }",
         ".xtermColor141 { color: #af87ff !important; }",
         ".xtermBgColor141 { background-color: #af87ff; }",
         ".xtermColor142 { color: #afaf00 !important; }",
         ".xtermBgColor142 { background-color: #afaf00; }",
         ".xtermColor143 { color: #afaf5f !important; }",
         ".xtermBgColor143 { background-color: #afaf5f; }",
         ".xtermColor144 { color: #afaf87 !important; }",
         ".xtermBgColor144 { background-color: #afaf87; }",
         ".xtermColor145 { color: #afafaf !important; }",
         ".xtermBgColor145 { background-color: #afafaf; }",
         ".xtermColor146 { color: #afafd7 !important; }",
         ".xtermBgColor146 { background-color: #afafd7; }",
         ".xtermColor147 { color: #afafff !important; }",
         ".xtermBgColor147 { background-color: #afafff; }",
         ".xtermColor148 { color: #afd700 !important; }",
         ".xtermBgColor148 { background-color: #afd700; }",
         ".xtermColor149 { color: #afd75f !important; }",
         ".xtermBgColor149 { background-color: #afd75f; }",
         ".xtermColor150 { color: #afd787 !important; }",
         ".xtermBgColor150 { background-color: #afd787; }",
         ".xtermColor151 { color: #afd7af !important; }",
         ".xtermBgColor151 { background-color: #afd7af; }",
         ".xtermColor152 { color: #afd7d7 !important; }",
         ".xtermBgColor152 { background-color: #afd7d7; }",
         ".xtermColor153 { color: #afd7ff !important; }",
         ".xtermBgColor153 { background-color: #afd7ff; }",
         ".xtermColor154 { color: #afff00 !important; }",
         ".xtermBgColor154 { background-color: #afff00; }",
         ".xtermColor155 { color: #afff5f !important; }",
         ".xtermBgColor155 { background-color: #afff5f; }",
         ".xtermColor156 { color: #afff87 !important; }",
         ".xtermBgColor156 { background-color: #afff87; }",
         ".xtermColor157 { color: #afffaf !important; }",
         ".xtermBgColor157 { background-color: #afffaf; }",
         ".xtermColor158 { color: #afffd7 !important; }",
         ".xtermBgColor158 { background-color: #afffd7; }",
         ".xtermColor159 { color: #afffff !important; }",
         ".xtermBgColor159 { background-color: #afffff; }",
         ".xtermColor160 { color: #d70000 !important; }",
         ".xtermBgColor160 { background-color: #d70000; }",
         ".xtermColor161 { color: #d7005f !important; }",
         ".xtermBgColor161 { background-color: #d7005f; }",
         ".xtermColor162 { color: #d70087 !important; }",
         ".xtermBgColor162 { background-color: #d70087; }",
         ".xtermColor163 { color: #d700af !important; }",
         ".xtermBgColor163 { background-color: #d700af; }",
         ".xtermColor164 { color: #d700d7 !important; }",
         ".xtermBgColor164 { background-color: #d700d7; }",
         ".xtermColor165 { color: #d700ff !important; }",
         ".xtermBgColor165 { background-color: #d700ff; }",
         ".xtermColor166 { color: #d75f00 !important; }",
         ".xtermBgColor166 { background-color: #d75f00; }",
         ".xtermColor167 { color: #d75f5f !important; }",
         ".xtermBgColor167 { background-color: #d75f5f; }",
         ".xtermColor168 { color: #d75f87 !important; }",
         ".xtermBgColor168 { background-color: #d75f87; }",
         ".xtermColor169 { color: #d75faf !important; }",
         ".xtermBgColor169 { background-color: #d75faf; }",
         ".xtermColor170 { color: #d75fd7 !important; }",
         ".xtermBgColor170 { background-color: #d75fd7; }",
         ".xtermColor171 { color: #d75fff !important; }",
         ".xtermBgColor171 { background-color: #d75fff; }",
         ".xtermColor172 { color: #d78700 !important; }",
         ".xtermBgColor172 { background-color: #d78700; }",
         ".xtermColor173 { color: #d7875f !important; }",
         ".xtermBgColor173 { background-color: #d7875f; }",
         ".xtermColor174 { color: #d78787 !important; }",
         ".xtermBgColor174 { background-color: #d78787; }",
         ".xtermColor175 { color: #d787af !important; }",
         ".xtermBgColor175 { background-color: #d787af; }",
         ".xtermColor176 { color: #d787d7 !important; }",
         ".xtermBgColor176 { background-color: #d787d7; }",
         ".xtermColor177 { color: #d787ff !important; }",
         ".xtermBgColor177 { background-color: #d787ff; }",
         ".xtermColor178 { color: #d7af00 !important; }",
         ".xtermBgColor178 { background-color: #d7af00; }",
         ".xtermColor179 { color: #d7af5f !important; }",
         ".xtermBgColor179 { background-color: #d7af5f; }",
         ".xtermColor180 { color: #d7af87 !important; }",
         ".xtermBgColor180 { background-color: #d7af87; }",
         ".xtermColor181 { color: #d7afaf !important; }",
         ".xtermBgColor181 { background-color: #d7afaf; }",
         ".xtermColor182 { color: #d7afd7 !important; }",
         ".xtermBgColor182 { background-color: #d7afd7; }",
         ".xtermColor183 { color: #d7afff !important; }",
         ".xtermBgColor183 { background-color: #d7afff; }",
         ".xtermColor184 { color: #d7d700 !important; }",
         ".xtermBgColor184 { background-color: #d7d700; }",
         ".xtermColor185 { color: #d7d75f !important; }",
         ".xtermBgColor185 { background-color: #d7d75f; }",
         ".xtermColor186 { color: #d7d787 !important; }",
         ".xtermBgColor186 { background-color: #d7d787; }",
         ".xtermColor187 { color: #d7d7af !important; }",
         ".xtermBgColor187 { background-color: #d7d7af; }",
         ".xtermColor188 { color: #d7d7d7 !important; }",
         ".xtermBgColor188 { background-color: #d7d7d7; }",
         ".xtermColor189 { color: #d7d7ff !important; }",
         ".xtermBgColor189 { background-color: #d7d7ff; }",
         ".xtermColor190 { color: #d7ff00 !important; }",
         ".xtermBgColor190 { background-color: #d7ff00; }",
         ".xtermColor191 { color: #d7ff5f !important; }",
         ".xtermBgColor191 { background-color: #d7ff5f; }",
         ".xtermColor192 { color: #d7ff87 !important; }",
         ".xtermBgColor192 { background-color: #d7ff87; }",
         ".xtermColor193 { color: #d7ffaf !important; }",
         ".xtermBgColor193 { background-color: #d7ffaf; }",
         ".xtermColor194 { color: #d7ffd7 !important; }",
         ".xtermBgColor194 { background-color: #d7ffd7; }",
         ".xtermColor195 { color: #d7ffff !important; }",
         ".xtermBgColor195 { background-color: #d7ffff; }",
         ".xtermColor196 { color: #ff0000 !important; }",
         ".xtermBgColor196 { background-color: #ff0000; }",
         ".xtermColor197 { color: #ff005f !important; }",
         ".xtermBgColor197 { background-color: #ff005f; }",
         ".xtermColor198 { color: #ff0087 !important; }",
         ".xtermBgColor198 { background-color: #ff0087; }",
         ".xtermColor199 { color: #ff00af !important; }",
         ".xtermBgColor199 { background-color: #ff00af; }",
         ".xtermColor200 { color: #ff00d7 !important; }",
         ".xtermBgColor200 { background-color: #ff00d7; }",
         ".xtermColor201 { color: #ff00ff !important; }",
         ".xtermBgColor201 { background-color: #ff00ff; }",
         ".xtermColor202 { color: #ff5f00 !important; }",
         ".xtermBgColor202 { background-color: #ff5f00; }",
         ".xtermColor203 { color: #ff5f5f !important; }",
         ".xtermBgColor203 { background-color: #ff5f5f; }",
         ".xtermColor204 { color: #ff5f87 !important; }",
         ".xtermBgColor204 { background-color: #ff5f87; }",
         ".xtermColor205 { color: #ff5faf !important; }",
         ".xtermBgColor205 { background-color: #ff5faf; }",
         ".xtermColor206 { color: #ff5fd7 !important; }",
         ".xtermBgColor206 { background-color: #ff5fd7; }",
         ".xtermColor207 { color: #ff5fff !important; }",
         ".xtermBgColor207 { background-color: #ff5fff; }",
         ".xtermColor208 { color: #ff8700 !important; }",
         ".xtermBgColor208 { background-color: #ff8700; }",
         ".xtermColor209 { color: #ff875f !important; }",
         ".xtermBgColor209 { background-color: #ff875f; }",
         ".xtermColor210 { color: #ff8787 !important; }",
         ".xtermBgColor210 { background-color: #ff8787; }",
         ".xtermColor211 { color: #ff87af !important; }",
         ".xtermBgColor211 { background-color: #ff87af; }",
         ".xtermColor212 { color: #ff87d7 !important; }",
         ".xtermBgColor212 { background-color: #ff87d7; }",
         ".xtermColor213 { color: #ff87ff !important; }",
         ".xtermBgColor213 { background-color: #ff87ff; }",
         ".xtermColor214 { color: #ffaf00 !important; }",
         ".xtermBgColor214 { background-color: #ffaf00; }",
         ".xtermColor215 { color: #ffaf5f !important; }",
         ".xtermBgColor215 { background-color: #ffaf5f; }",
         ".xtermColor216 { color: #ffaf87 !important; }",
         ".xtermBgColor216 { background-color: #ffaf87; }",
         ".xtermColor217 { color: #ffafaf !important; }",
         ".xtermBgColor217 { background-color: #ffafaf; }",
         ".xtermColor218 { color: #ffafd7 !important; }",
         ".xtermBgColor218 { background-color: #ffafd7; }",
         ".xtermColor219 { color: #ffafff !important; }",
         ".xtermBgColor219 { background-color: #ffafff; }",
         ".xtermColor220 { color: #ffd700 !important; }",
         ".xtermBgColor220 { background-color: #ffd700; }",
         ".xtermColor221 { color: #ffd75f !important; }",
         ".xtermBgColor221 { background-color: #ffd75f; }",
         ".xtermColor222 { color: #ffd787 !important; }",
         ".xtermBgColor222 { background-color: #ffd787; }",
         ".xtermColor223 { color: #ffd7af !important; }",
         ".xtermBgColor223 { background-color: #ffd7af; }",
         ".xtermColor224 { color: #ffd7d7 !important; }",
         ".xtermBgColor224 { background-color: #ffd7d7; }",
         ".xtermColor225 { color: #ffd7ff !important; }",
         ".xtermBgColor225 { background-color: #ffd7ff; }",
         ".xtermColor226 { color: #ffff00 !important; }",
         ".xtermBgColor226 { background-color: #ffff00; }",
         ".xtermColor227 { color: #ffff5f !important; }",
         ".xtermBgColor227 { background-color: #ffff5f; }",
         ".xtermColor228 { color: #ffff87 !important; }",
         ".xtermBgColor228 { background-color: #ffff87; }",
         ".xtermColor229 { color: #ffffaf !important; }",
         ".xtermBgColor229 { background-color: #ffffaf; }",
         ".xtermColor230 { color: #ffffd7 !important; }",
         ".xtermBgColor230 { background-color: #ffffd7; }",
         ".xtermColor231 { color: #ffffff !important; }",
         ".xtermBgColor231 { background-color: #ffffff; }",
         ".xtermColor232 { color: #080808 !important; }",
         ".xtermBgColor232 { background-color: #080808; }",
         ".xtermColor233 { color: #121212 !important; }",
         ".xtermBgColor233 { background-color: #121212; }",
         ".xtermColor234 { color: #1c1c1c !important; }",
         ".xtermBgColor234 { background-color: #1c1c1c; }",
         ".xtermColor235 { color: #262626 !important; }",
         ".xtermBgColor235 { background-color: #262626; }",
         ".xtermColor236 { color: #303030 !important; }",
         ".xtermBgColor236 { background-color: #303030; }",
         ".xtermColor237 { color: #3a3a3a !important; }",
         ".xtermBgColor237 { background-color: #3a3a3a; }",
         ".xtermColor238 { color: #444444 !important; }",
         ".xtermBgColor238 { background-color: #444444; }",
         ".xtermColor239 { color: #4e4e4e !important; }",
         ".xtermBgColor239 { background-color: #4e4e4e; }",
         ".xtermColor240 { color: #585858 !important; }",
         ".xtermBgColor240 { background-color: #585858; }",
         ".xtermColor241 { color: #626262 !important; }",
         ".xtermBgColor241 { background-color: #626262; }",
         ".xtermColor242 { color: #6c6c6c !important; }",
         ".xtermBgColor242 { background-color: #6c6c6c; }",
         ".xtermColor243 { color: #767676 !important; }",
         ".xtermBgColor243 { background-color: #767676; }",
         ".xtermColor244 { color: #808080 !important; }",
         ".xtermBgColor244 { background-color: #808080; }",
         ".xtermColor245 { color: #8a8a8a !important; }",
         ".xtermBgColor245 { background-color: #8a8a8a; }",
         ".xtermColor246 { color: #949494 !important; }",
         ".xtermBgColor246 { background-color: #949494; }",
         ".xtermColor247 { color: #9e9e9e !important; }",
         ".xtermBgColor247 { background-color: #9e9e9e; }",
         ".xtermColor248 { color: #a8a8a8 !important; }",
         ".xtermBgColor248 { background-color: #a8a8a8; }",
         ".xtermColor249 { color: #b2b2b2 !important; }",
         ".xtermBgColor249 { background-color: #b2b2b2; }",
         ".xtermColor250 { color: #bcbcbc !important; }",
         ".xtermBgColor250 { background-color: #bcbcbc; }",
         ".xtermColor251 { color: #c6c6c6 !important; }",
         ".xtermBgColor251 { background-color: #c6c6c6; }",
         ".xtermColor252 { color: #d0d0d0 !important; }",
         ".xtermBgColor252 { background-color: #d0d0d0; }",
         ".xtermColor253 { color: #dadada !important; }",
         ".xtermBgColor253 { background-color: #dadada; }",
         ".xtermColor254 { color: #e4e4e4 !important; }",
         ".xtermBgColor254 { background-color: #e4e4e4; }",
         ".xtermColor255 { color: #eeeeee !important; }",
         ".xtermBgColor255 { background-color: #eeeeee; }")
})

.rs.addFunction("themes_static_rules", function(isDark) {
   content <- paste(".rstudio-themes-flat.editor_dark.ace_editor_theme a {",
                    "   color: #FFF !important;",
                    "}",
                    "",
                    ".ace_layer {",
                    "   z-index: 3;",
                    "}",
                    "",
                    # support for showing margin line over nodebook code chunks 
                    ".ace_layer.ace_print-margin-layer {",
                    "   z-index: 2;",
                    "}",
                    "",
                    ".ace_layer.ace_marker-layer {",
                    "   z-index: 1;",
                    "}",
                    sep = "\n")
   
   if (isDark) {
      content <- c(
         content,
         paste(
            ".rstudio-themes-flat.rstudio-themes-dark-menus .ace_editor.ace_autocomplete {",
            "   background: #2f3941;",                    # darkGreyMenuBackground
            "   border: solid 1px #4e5c68 !important;",   # darkGreyMenuBorder
            "   color: #f0f0f0;",
            "}",
            "",
            ".rstudio-themes-flat.rstudio-themes-dark-menus .ace_editor.ace_autocomplete .ace_marker-layer .ace_active-line,",
            ".rstudio-themes-flat.rstudio-themes-dark-menus .ace_editor.ace_autocomplete .ace_marker-layer .ace_line-hover {",
            "   background: rgba(255, 255, 255, 0.15);",  # darkGreyMenuSelected 
            "   border: none",
            "}",
            sep = "\n"
         )
      )
   }
   
   content
})

.rs.addFunction("compile_theme", function(
   lines,
   isDark,
   name = "",
   chunkBgPropOverrideMap = list(),
   operatorOverrideMap = list(),
   keywordOverrideMap = list(),
   nodeSelectorOverrideMap = list(),
   commentBgOverrideMap = list())
{
   ## Guess the theme name -- all rules should start with it.
   stripped <- sub(" .*", "", lines)
   stripped <- grep("^\\.", stripped, value = TRUE)
   
   ## Get the most common value; this is the theme name.
   ## We do this because some rules will have e.g.
   ## '.ace-<theme>.normal-mode', or other things we don't use.
   themeNameCssClass <- names(sort(c(table(stripped)), decreasing = TRUE))[[1]]
   
   ## There may (should) be a rule for just '.ace-<theme> { ... }'; we need
   ## to preserve this theme, but apply it to the '.ace_editor' directly.
   regex <- paste("^\\s*", themeNameCssClass, "\\s*\\{\\s*$", sep = "")
   content <- gsub(regex, paste(
      ".ace_editor",
      ".rstudio-themes-flat.ace_editor_theme .profvis-flamegraph",
      ".rstudio-themes-flat.ace_editor_theme", 
      ".rstudio-themes-flat .ace_editor_theme {",
      sep = ", "), lines)
   
   ## Strip the theme name rule from the CSS.
   regex <- paste("^\\", themeNameCssClass, "\\S*\\s+", sep = "")
   content <- gsub(regex, "", content)
   
   ## Add border radius
   content <- .rs.setSelectionStartBorderRadius(content)
   
   ## Parse the css
   parsed <- .rs.parseCss(lines = content)
   
   names(parsed)[grep("^\\.ace_editor(,.*|)$", names(parsed), perl = TRUE)] <- "ace_editor"
   
   if (!any(grepl("^\\.ace_keyword", names(parsed)))) {
      warning("No field 'ace_keyword' in file '", paste0(name,".css"), "'; skipping", call. = FALSE)
      return(c())
   }
   
   key <- grep("^\\.ace_keyword\\s*$", names(parsed), value = TRUE)
   keywordColor <- NULL
   if (length(key) > 0) {
      keywordColor <- parsed[[key[[1]]]]$color
   }
   if (is.null(keywordColor)) {
      warning("No field 'color' for rule 'ace_keyword' in file '", paste0(name,".css"), "'", call. = FALSE)
      return(c())
   }
   
   content <- .rs.add_content(
      content,
      ".nocolor.ace_editor .ace_line span {",
      "  color: %s !important;",
      "}",
      replace = keywordColor
   )
   
   ## Coloring for brackets, discarding the ace bounding box shown
   ## on highlight.
   layerName <- ".ace_marker-layer .ace_bracket"
   if (!(layerName %in% names(parsed))) {
      warning("Expected rule for '", layerName, "' in file '", paste0(name,".css"), "'; skipping", call. = FALSE)
      return(c())
   }
   
   borderField <- parsed[[layerName]]$border
   if (is.null(borderField)) {
      warning("No field 'border' for rule '", layerName, "' in file '", paste0(name,".css"), "'; skipping", call. = FALSE)
      return(c())
   }
   
   operatorBgColor <- if (isDark)
      "rgba(128, 128, 128, 0.5)"
   else
      "rgba(192, 192, 192, 0.5)"
   
   content <- .rs.add_content(
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
   
   
   ## Generate a color used for chunks, e.g. in .Rmd documents.
   ## Determine an appropriate mixing proportion, and override for certain
   ## themes.
   mix <- .rs.get_chunk_bg_color(name, isDark, chunkBgPropOverrideMap)
   
   mergedColor <- .rs.mixColors(
      background,
      foreground,
      mix
   )
   
   content <- c(
      content,
      .rs.create_line_marker_rule(".ace_foreign_line", mergedColor),
      .rs.createNodeSelectorRule(name, isDark, nodeSelectorOverrideMap),
      .rs.createCommentBgRule(name, isDark, commentBgOverrideMap)
   )
   
   ## Generate a color used for 'debugging' backgrounds.
   if (!any(grepl(".ace_active_debug_line", content, fixed = TRUE)))
   {
      debugPrimary <- "#FFDE38"
      debugBg <- .rs.mixColors(background, debugPrimary, 0.5)
   
      content <- c(
         content,
         .rs.create_line_marker_rule(".ace_active_debug_line", debugBg)
      )
   }
   
   ## Generate a background color used for console errors, as well as
   ## 'find_line' (used for highlighting e.g. 'sourceCpp' errors).
   
   ## Dark backgrounds need a bit more contrast than light ones for
   ## a nice visual display.
   mixingProportion <- if (isDark) 0.8 else 0.9
   errorBgColor <- .rs.mixColors(background, foreground, mixingProportion)
   
   content <- c(
      content,
      .rs.create_line_marker_rule(".ace_find_line", errorBgColor)
   )
   
   content <- .rs.add_content(
      content,
      ".ace_console_error {",
      "  background-color: %s;",
      "}",
      replace = errorBgColor
   )
   
   ## Add operator colors if necessary.
   if (name %in% names(operatorOverrideMap))
      content <- .rs.add_operator_color(content, name, isDark, operatorOverrideMap)
   
   ## Add keyword colors if necessary.
   if (name %in% names(keywordOverrideMap))
      content <- .rs.add_keyword_color(content, name, keywordOverrideMap)
   
   # Apply terminal-specifics
   content <- c(content,
                .rs.create_terminal_rule(background, foreground)) 
   content <- c(content,
                .rs.create_terminal_cursor_rules(isDark))
   content <- c(content,
                .rs.create_terminal_viewport_rule(background))
   
   # Add xterm-256 colors for colorized console output
   content <- c(content,
                .rs.create_xterm_color_rules(background, foreground, isDark))
   
   # Theme rules
   content <- c(content,
                .rs.themes_static_rules(isDark)) 
   
   # All done, return the lines.
   content
})
