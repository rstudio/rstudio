#
# SessionCompilePdf.R
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

.rs.addGlobalFunction("compilePdf", function(file)
{
   invisible(.Call("rs_compilePdf", file, "view"))
})

.rs.addGlobalFunction("publishPdf", function(file)
{
   invisible(.Call("rs_compilePdf", file, "publish"))
})

.rs.addFunction( "getCompilationErrors", function(file)
{
   base <- basename(tools:::file_path_sans_ext(file))

   msg <- ""
   logfile <- paste(base, "log", sep = ".")
   if (utils::file_test("-f", logfile))
   {
      lines <- tools:::.get_LaTeX_errors_from_log_file(logfile)
      if (length(lines))
        msg <- paste(msg, "LaTeX errors:", paste(lines,
                     collapse = "\n"), sep = "\n")
   }


   logfile <- paste(base, "blg", sep = ".")
   if (utils::file_test("-f", logfile))
   {
      lines <- tools:::.get_BibTeX_errors_from_blg_file(logfile)
      if (length(lines))
        msg <- paste(msg, "BibTeX errors:", paste(lines,
                     collapse = "\n"), sep = "\n")
   }

   return (msg)
})

