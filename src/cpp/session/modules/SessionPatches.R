#
# SessionPatches.R
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

.rs.registerPackageLoadHook("rstudioapi", function(...)
{
   # bail if we're not version 0.7
   if (packageVersion("rstudioapi") != "0.7")
      return()
   
   # re-assign the 'selectDirectory()' function
   rstudioapi <- asNamespace("rstudioapi")
   override <- function(caption = "Select Directory",
                        label = "Select",
                        path = rstudioapi::getActiveProject())
   {
      callFun("selectDirectory", caption, label, path)
   }
   environment(override) <- rstudioapi
   
   .rs.replaceBinding("selectDirectory", "rstudioapi", override)
})

.rs.registerPackageLoadHook("parallel", function(...)
{
   # enforce sequential setup of cluster
   # https://github.com/rstudio/rstudio/issues/6692
   parallel:::setDefaultClusterOptions(setup_strategy = "sequential")
})

# On Windows, because we now set the active code page to UTF-8,
# we need to be careful to ensure the outputs from list.files(), list.dirs()
# and dir() have their encoding properly marked. We do this here.
if (.rs.platform.isWindows)
{
   setHook("rstudio.sessionInit", function(...)
   {
      enabled <- getOption("rstudio.enableFileHooks", default = getRversion() < "4.2.0")
      if (identical(enabled, TRUE))
         .rs.files.replaceBindings()
   })
}
