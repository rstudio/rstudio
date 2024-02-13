#
# SessionAuthoring.R
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
#


.rs.addFunction("knitrChunkOptions", function()
{
   if (!requireNamespace("knitr", quietly = TRUE))
      return(list())
   
   chunkOptions <- knitr:::opts_chunk_attr
   
   # paged.print is numeric, but it's normally used as logical
   # https://github.com/rstudio/rstudio/issues/9895
   chunkOptions$paged.print <- "logical"
   
   chunkOptions
})

.rs.addFunction( "sweaveChunkOptions", function()
{
   sweaveOptions <- list()
   
   sweaveOptions$label <- "character"
   sweaveOptions$engine <- list("R", "S")
   sweaveOptions$echo <- "logical"
   sweaveOptions$keep.source <- "logical"
   sweaveOptions$eval <- "logical"
   sweaveOptions$results <- list("verbatim", "tex", "hide")
   sweaveOptions$print <- "logical"
   sweaveOptions$term <- "logical"
   sweaveOptions$split <- "logical"
   sweaveOptions$strip.white <- list("true", "all", "false")
   sweaveOptions$prefix <- "logical"
   sweaveOptions$prefix.string <- "character"
   sweaveOptions$include <- "logical"
   sweaveOptions$fig <- "logical"
   sweaveOptions$eps <- "logical"
   sweaveOptions$pdf <- "logical"
   sweaveOptions$pdf.version <- "character"
   sweaveOptions$pdf.encoding <- "character"
   sweaveOptions$pdf.compress <-"logical"
   sweaveOptions$png <- "logical"
   sweaveOptions$jpeg <- "logical"
   sweaveOptions$grdevice <- "character"
   sweaveOptions$width <- "numeric"
   sweaveOptions$height <- "numeric"
   sweaveOptions$resolution <- "numeric"
   sweaveOptions$figs.only <- "logical"
   
   return (sweaveOptions)
})

