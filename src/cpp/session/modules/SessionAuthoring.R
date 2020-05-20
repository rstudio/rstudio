#
# SessionAuthoring.R
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
#


.rs.addFunction( "knitrChunkOptions", function()
{
   # starting with v0.4.2 this is avavilable directly from knitr
   if (.rs.getPackageVersion("knitr") >= "0.4.2")
      return(knitr:::opts_chunk_attr)

   # still try to return correct results for previous versions
   knitrOptions <- knitr:::opts_chunk$get()
   knitrOptions <- as.list(sapply(knitrOptions, class))
   knitrOptions[knitrOptions == "NULL"] <- "character"
   knitrOptions$results <- list("markup", "asis", "hide")
   knitrOptions$fig.show <- list("asis", "hold", "animate")
   knitrOptions$fig.keep <- list("high", "none", "all", "first", "last")
   knitrOptions$fig.align <- list("left", "right", "center")
   if (.rs.getPackageVersion("knitr") >= "0.4")
      knitrOptions$dev <- as.list(names(knitr:::auto_exts))
   
   return (knitrOptions)
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

