#!/usr/bin/env Rscript

## This should be run via the build-xterm script, not directly.
##
## Tweaks the css that comes with xterm.js so it can be overriden by Ace editor
## themes, copying the modified file to the RStudio source directory.

outDir <- "../../../src/org/rstudio/studio/client/workbench/views/terminal/xterm"
baseName <- "xterm.css"

content <- suppressWarnings(readLines(baseName))

findNext <- function(regex, content, start = 1, end = length(content)) {
   matches <- grep(regex, content, perl = TRUE)
   matches[(matches > start) & (matches < end)][1]
}

## Delete the @keyframes rule. Contains nested braces.
deleteKeyFrame <- function(content) {
   keyFrameStarts <- grep("@keyframes", content, fixed = TRUE)
   if (length(keyFrameStarts) != 1) {
      warning("No '@keyframe' in file '", baseName, "'; stopping", call. = FALSE)
      quit(status = 1)
   }
   keyFrameEnds <- keyFrameStarts
   openCount <- 1
   while (openCount > 0) {
      keyFrameEnds = keyFrameEnds + 1
      if (grepl("}", content[keyFrameEnds], fixed = TRUE) == TRUE) {
         openCount = openCount - 1
      } else if (grepl("{", content[keyFrameEnds], fixed = TRUE) == TRUE) {
         openCount = openCount + 1
      }
   }
   content[-(keyFrameStarts:keyFrameEnds)]
}

deleteRule <- function(content, ruleName1, ruleName2 = NULL) {
   if (!is.null(ruleName2)) {
      match <- paste("^\\s*\\.", ruleName1, "\\s*\\.", ruleName2, "\\s*{", sep = "")
   } else {
      match <- paste("^\\s*\\.", ruleName1, "\\s*{", sep = "")
   }
   ruleLoc <- grep(match, content, perl = TRUE)
   nextBraceLoc <- findNext("}", content, ruleLoc)
   
   content[-(ruleLoc:nextBraceLoc)]
}

## Make the adjustments
content <- deleteKeyFrame(content)
content <- deleteRule(content, "terminal")
content <- deleteRule(content, "terminal", "xterm-viewport")
content <- deleteRule(content, "terminal", "terminal-cursor")
content <- deleteRule(content, "terminal:not\\(\\.focus\\)", "terminal-cursor")

## Write it out.
outputPath <- file.path(outDir, baseName)
cat(content, file = outputPath, sep = "\n")
quit(status = 0)
