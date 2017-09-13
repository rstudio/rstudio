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

deleteTwoLineRule <- function(content, ruleName1, ruleName2, ruleName3, ruleName4) {
   match <- paste("^\\s*\\.", ruleName1, "\\s*\\.", ruleName2, ",\\s*$", sep = "")
   firstLineMatch = grep(match, content, perl = TRUE)
   if (length(firstLineMatch) != 1) {
      return(content)
   }
   match <- paste("^\\s*\\.", ruleName3, "\\s*\\.", ruleName4, "\\s*{", sep = "")
   secondLineMatches = grep(match, content, perl = TRUE)
   if (length(secondLineMatches) == 0) {
      return(content)
   }
   if (is.element(firstLineMatch + 1, secondLineMatches)) {
      nextBraceLoc <- findNext("}", content, start = firstLineMatch)
      if (length(nextBraceLoc) == 1) {
         return(content[-(firstLineMatch:nextBraceLoc)])
      }
   }
   content
}

deleteRule <- function(content, ruleName1, ruleName2 = NULL, ruleName3 = NULL) {
   if (!is.null(ruleName2) && !is.null(ruleName3)) {
      match <- paste("^\\s*\\.", ruleName1, "\\s*\\.", ruleName2, "\\s*", ruleName3, "\\s*{", sep = "")
   } else if (!is.null(ruleName2)) {
      match <- paste("^\\s*\\.", ruleName1, "\\s*\\.", ruleName2, "\\s*{", sep = "")
   } else {
      match <- paste("^\\s*\\.", ruleName1, "\\s*{", sep = "")
   }
   ruleLoc <- grep(match, content, perl = TRUE)
   nextBraceLoc <- findNext("}", content, ruleLoc)
   
   content[-(ruleLoc:nextBraceLoc)]
}

## Make the adjustments
content <- deleteRule(content, "terminal")
content <- deleteRule(content,
               "terminal\\.xterm-cursor-style-block\\.focus:not\\(\\.xterm-cursor-blink-on\\)",
               "terminal-cursor")
content <- deleteTwoLineRule(content,
               "terminal\\.focus\\.xterm-cursor-style-bar:not\\(\\.xterm-cursor-blink-on\\)", "terminal-cursor::before",
               "terminal\\.focus\\.xterm-cursor-style-underline:not\\(\\.xterm-cursor-blink-on\\)", "terminal-cursor::before")
content <- deleteRule(content, "terminal", "xterm-viewport")
content <- deleteRule(content, "terminal:not\\(\\.focus\\)", "terminal-cursor")
content <- deleteRule(content, "terminal", "xterm-selection", "div")

## Write it out.
outputPath <- file.path(outDir, baseName)
cat(content, file = outputPath, sep = "\n")
quit(status = 0)
