#
# SessionRHooks.R
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

.rs.addFunction("invokeHook", function(hookName, ...) {
  # ensure that the list of hooks can be iterated over as a list
  hooks <- getHook(hookName)
  if (!is.list(hooks))
    hooks <- list(hooks)

  # execute each attached hook with the given parameters
  for (fun in hooks) {
    if (is.character(fun))
      fun <- get(fun)
    tryCatch(fun(...),
             error = function(e) { 
                # ignore errors occurring during hook execution
             })
  }
})

