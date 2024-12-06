#
# generate-commands-acessor.R
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

# Use stable project root for file paths
projectRoot <- here::here()
setwd(projectRoot)

# Find Commands.java
path <- "src/gwt/src/org/rstudio/studio/client/workbench/commands/Commands.java"
contents <- readLines(path, warn = FALSE)

# Pull out the AppCommand names
pattern <- "^\\s*public\\s+abstract\\s+AppCommand\\s+(\\w+).*$"
matchingLines <- grep(pattern, contents, perl = TRUE, value = TRUE)
commandNames <- gsub(pattern, "\\1", matchingLines, perl = TRUE)
commandInner <- paste(sprintf("   %1$s = \"%1$s\"", commandNames), collapse = ",\n")
commandCode <- paste("list(", commandInner, ")", sep = "\n")

# Generate code
generatedFileContents <- .rs.heredoc('
 #
 # AppCommands.R
 #
 # Copyright (C) %s by Posit Software, PBC
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
 # This file was automatically generated -- please do not modify it by hand.
 # Generator: src/cpp/generate-commands-accessor.R
 #

 .rs.setVar("appCommands", %s)
 
', format(Sys.Date(), "%Y"), commandCode)

writeLines(generatedFileContents, con = "src/cpp/r/R/AppCommands.R")
