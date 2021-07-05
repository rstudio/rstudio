#
# SessionQuarto.R
#
# Copyright (C) 2021 by RStudio, PBC
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

.rs.addFunction("quarto.servePort", function() {
   if (requireNamespace("quarto", quietly = TRUE)) {
      if (!is.null(quarto:::quarto$ps) && quarto:::quarto$ps$is_alive()) {
         if (is.numeric(quarto:::quarto$port)) {
            quarto:::quarto$port
         } else {
            0
         }
      } else {
         0
      }
   } else {
      0
   }
})

.rs.addFunction("quarto.defaultOutputFormat", function(path, encoding) {
   
   # set some defaults
   format <- "html"
   self_contained <- FALSE
   
   # read yaml
   yaml <- rmarkdown::yaml_front_matter(path, encoding)
   self_contained <- yaml[["self-contained"]]
   if (!is.null(yaml$format)) {
      if (is.character(yaml$format)) {
         format <- yaml$format
      } else if (is.list(yaml$format) && !is.null(names(yaml$format))) {
         # set the format name
         format <- names(yaml$format)[[1]]
         
         # check for self-contained
         if (is.list(yaml$format[[format]]) &&
             !is.null(yaml$format[[format]][["self-contained"]])) {
            self_contained <- yaml$format[[format]][["self-contained"]]
         }
      }
   }  
   
   # Adjust format name to align with IDE detection schema defined here:
   # https://github.com/rstudio/rstudio/blob/main/src/gwt/src/org/rstudio/studio/client/rmarkdown/model/RmdOutputFormat.java
   # The main point of this function is to have the IDE classify Quarto 
   # documents correctly, which is in turn mostly about treating
   # presentations specially (although there is some special handling for
   # MS Word document preview on windows)
   if (startsWith(format, "docx")) {
      format = "word_document"
   } else if (startsWith(format, "pdf")) {
      format = "pdf_document"
   } else if (startsWith(format, "pptx")) {
      format = "powerpoint_presentation"
   } else if (startsWith(format, "beamer")) {
      format = "beamer_presentation"
   } else if (startsWith(format, "revealjs")) {
      format = "revealjs_presentation"
   } else if (startsWith(format, "slidy")) {
      format = "slidy_presentation"
   }
   
   # if no explicit self_contained then default to FALSE
   if (is.null(self_contained)) {
      self_contained <- FALSE
   }
   
   list(
      name = format,
      options = list(
         self_contained = self_contained
      )
   )
})

