#
# SessionHTMLPreview.R
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

.rs.addFunction( "getHTMLCapabilities", function(markdownVersion,
                                                 stitchVersion)
{
   caps <- list()
   caps$r_markdown_supported = .rs.scalar(FALSE)
   caps$stitch_supported = .rs.scalar(FALSE)
   if (.rs.isPackageInstalled("knitr"))
   {
      knitrVersion <- .rs.getPackageVersion("knitr")
      caps$r_markdown_supported = .rs.scalar(knitrVersion >= markdownVersion)
      caps$stitch_supported = .rs.scalar(knitrVersion >= stitchVersion)
   }
   return (caps)
})


.rs.addFunction( "spinScript", function(script, signature)
{
   # do the spin
   rmd <- knitr::spin(script, knit = FALSE, format = "Rmd")

   # append the signature (for overwrite protection)
   cat(signature, file = rmd, append = TRUE)
})

