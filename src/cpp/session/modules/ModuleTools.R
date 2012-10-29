#
# ModuleTools.R
#
# Copyright (C) 2009-12 by RStudio, Inc.
#
# This program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addFunction("enqueClientEvent", function(type, data = NULL)
{
   .Call("rs_enqueClientEvent", type, data)
})

.rs.addFunction("showErrorMessage", function(title, message)
{
   .Call("rs_showErrorMessage", title, message)
})

.rs.addFunction("logErrorMessage", function(message)
{
   .Call("rs_logErrorMessage", message)
})

.rs.addFunction("logWarningMessage", function(message)
{
   .Call("rs_logWarningMessage", message)
})

.rs.addFunction("getSignature", function(obj)
{
   sig = capture.output(print(args(obj)))
   sig = sig[1:length(sig)-1]
   sig = gsub('^\\s+', '', sig)
   paste(sig, collapse='')
})

# Wrap a return value in this to give a hint to the
# JSON serializer that one-element vectors should be
# marshalled as scalar types instead of arrays
.rs.addFunction("scalar", function(obj)
{
   class(obj) <- 'rs.scalar'
   return(obj)
})

.rs.addFunction("validateAndNormalizeEncoding", function(encoding)
{
   iconvList <- toupper(iconvlist())
   encodingUpper <- toupper(encoding)
   if (encodingUpper %in% iconvList)
   {
      return (encodingUpper)
   }
   else
   {
      encodingUpper <- gsub("[_]", "-", encodingUpper)
      if (encodingUpper %in% iconvList)
         return (encodingUpper)
      else
         return ("")
   }
})

.rs.addFunction("usingUtf8Charset", function()
{
   l10n_info()$`UTF-8` || identical(utils::localeToCharset(), "UTF-8")
})


.rs.addFunction("isRtoolsOnPath", function()
{
   return (nzchar(Sys.which("ls.exe")) && nzchar(Sys.which("gcc.exe")))
})

.rs.addFunction("getPackageFunction", function(name, packageName)
{
   tryCatch(eval(parse(text=paste(packageName, ":::", name, sep=""))),
            error = function(e) NULL)
})
