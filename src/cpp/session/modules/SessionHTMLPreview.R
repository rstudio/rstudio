#
# SessionHTMLPreview.R
#
# Copyright (C) 2009-11 by RStudio, Inc.
#
# This program is licensed to you under the terms of version 3 of the
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
      knitrVersion <- packageVersion("knitr")
      caps$r_markdown_supported = .rs.scalar(knitrVersion >= markdownVersion)
      caps$stitch_supported = .rs.scalar(knitrVersion >= stitchVersion)
   }
   return (caps)
})

.rs.addFunction( "stitchScript", function(script, signature)
{
   # use the default knitr markdown template
   rmdTemplate <- system.file("misc",
                              "knitr-template.Rmd",
                              package = "knitr")

   # knitr attempts to run markdownToHTML and browseURL on the
   # output file if it knows it is markdown. Since we do this
   # internally in RStudio we need to suppress this behavior by
   # passing in an output file with an alternate extension
   scriptStem <- tools::file_path_sans_ext(script)
   rmd <- paste(scriptStem, ".Rmd", sep="")
   rmdTemp = paste(scriptStem, ".stitch-Rmd", sep="")

   # run the stitch
   knitr::stitch(script, rmdTemplate, output = rmdTemp)

   # copy the generated rmd to the target rmd path
   file.copy(rmdTemp, rmd, overwrite = TRUE)
   file.remove(rmdTemp)

   # append the signature (for overwrite protection)
   cat(signature, file = rmd, append = TRUE)
})

.rs.addFunction( "spinScript", function(script, signature)
{
   # do the spin
   rmd <- knitr::spin(script, knit = FALSE, format = "Rmd")

   # append the signature (for overwrite protection)
   cat(signature, file = rmd, append = TRUE)
})

