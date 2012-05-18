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

.rs.addFunction( "getHTMLCapabilities", function(htmlVersion,
                                                 markdownVersion)
{
   caps <- list()
   caps$r_html_version = .rs.scalar(htmlVersion)
   caps$r_markdown_version = .rs.scalar(markdownVersion)
   caps$r_html_supported = .rs.scalar(FALSE)
   caps$r_markdown_supported = .rs.scalar(FALSE)
   if (.rs.isPackageInstalled("knitr"))
   {
      knitrVersion <- packageVersion("knitr")
      caps$r_html_supported = .rs.scalar(knitrVersion >= htmlVersion)
      caps$r_markdown_supported = .rs.scalar(knitrVersion >= markdownVersion)
   }
   return (caps)
})

.rs.addFunction( "rpubsEnable", function()
{
   invisible(.Call("rs_rpubsEnable"))
})
