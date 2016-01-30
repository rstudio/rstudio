#
# SessionRmdNotebook.R
#
# Copyright (C) 2009-16 by RStudio, Inc.
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addFunction("executeSingleChunk", function(options, content, libDir, 
                                               outputFile) 
{
  # create a temporary file stub to send to R Markdown
  chunkFile <- tempfile(fileext = ".Rmd")
  chunk <- paste(paste("```{r", options, "echo=FALSE}"), 
                 content,
                 "```\n", 
                 sep = "\n");
  writeLines(chunk, con = chunkFile)
  on.exit(unlink(chunkFile), add = TRUE)
                 
  # render the stub to the given file
  capture.output(rmarkdown::render(input = chunkFile, 
                                   output_format = rmarkdown::html_document(
                                     theme = NULL,
                                     highlight = NULL,
                                     template = NULL, 
                                     self_contained = FALSE,
                                     lib_dir = libDir),
                                   output_file = outputFile,
                                   encoding = "UTF-8",
                                   quiet = TRUE))
  invisible(NULL)
})
