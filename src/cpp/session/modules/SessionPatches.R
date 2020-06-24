#
# SessionPatches.R
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

# NOTE: registered hooks will be run immediately if the
# package has already been loaded.
.rs.addFunction("registerPackageLoadHook", function(package, hook)
{
   # if the package is already loaded, run the hook;
   # otherwise, register a load hook
   if (package %in% loadedNamespaces())
      hook()
   else
      setHook(packageEvent(package, "onLoad"), hook)
})

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
   # enforce sequential setup of cluster on macOS
   # https://github.com/rstudio/rstudio/issues/6692
   if (Sys.info()[["sysname"]] == "Darwin")
      parallel:::setDefaultClusterOptions(setup_strategy = "sequential")
})

