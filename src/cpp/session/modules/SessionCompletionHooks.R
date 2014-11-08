#
# SessionCompletionHooks.R
#
# Copyright (C) 2014 by RStudio, Inc.
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
.rs.addFunction("getCompletionHooks", function()
{
   if (exists(".rs.RCompletionHooksEnv"))
      mget(objects(.rs.RCompletionHooksEnv), envir = .rs.RCompletionHooksEnv)
})

.rs.addFunction("addCompletionHook", function(pkgName, hookName, hook)
{
   if (exists(".rs.RCompletionHooksEnv"))
   {
      fullName <- paste(pkgName, hookName, sep = ":")
      assign(fullName, hook, envir = .rs.RCompletionsHookEnv)
   }
})

.rs.addJsonRpcHandler("get_user_completions", function(content,
                                                       cursorPos)
{
   completions <- .rs.emptyCompletions()
   if (exists(".rs.RCompletionHooksEnv"))
   {
      hooks <- mget(objects(.rs.RCompletionHooksEnv), envir = .rs.RCompletionHooksEnv)
      for (hook in hooks)
      {
         completions <- .rs.appendCompletions(
            completions,
            hook(content, cursorPos)
         )
      }
   }
   completions
})
