#
# SessionPackageProvidedExtension.R
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

.rs.addFunction("ppe.invokeHook", function(hook, data)
{
   # Treat single function as list of functions
   if (is.function(hook))
      hook <- list(hook)
   
   # Disable warnings in this scope
   op <- options(warn = -1L)
   on.exit(options(op), add = TRUE)
   
   # Invoke hooks
   lapply(hook, .rs.ppe.invokeHookImpl, data = data)
})

.rs.addFunction("ppe.invokeHookImpl", function(hook, data)
{
   status <- tryCatch(do.call(hook, data), error = identity)
   if (inherits(status, "error"))
      warning(conditionMessage(status))
})
