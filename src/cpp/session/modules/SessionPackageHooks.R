#
# SessionPackageHooks.R
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

.rs.addFunction("installPackages", function(pkgs, lib)
{
   ""
   "This is an RStudio hook."
   "Use `utils::install.packages()` to bypass this hook if necessary."
   ""
   
   call <- sys.call()
   call[[1L]] <- quote(.rs.installPackagesImpl)
   eval(call, envir = parent.frame())
})

.rs.addFunction("installPackagesImpl", function(pkgs, lib)
{
   # Check if package installation was disabled in this version of RStudio.
   canInstallPackages <- .Call("rs_canInstallPackages", PACKAGE = "(embedding)")
   if (!canInstallPackages)
   {
      msg <- "Package installation is disabled in this version of RStudio."
      stop(msg, call. = FALSE)
   }
   
   # Notify if we're about to update an already-loaded package.
   if (.rs.loadedPackageUpdates(pkgs))
   {
      call <- sys.call()
      call[[1L]] <- quote(install.packages)
      command <- .rs.deparseCall(call)
      
      .rs.enqueLoadedPackageUpdates(command)
      invokeRestart("abort")
   }
   
   # Make sure Rtools is on the PATH for Windows.
   .rs.addRToolsToPath()
   on.exit(.rs.restorePreviousPath(), add = TRUE)
   
   # Invoke the original function.
   # TODO: We could consider adding custom headers here.
   call <- sys.call()
   call[[1L]] <- quote(utils::install.packages)
   result <- eval(call, envir = parent.frame())
   
   # Notify the front-end that we've made some updates.
   .rs.updatePackageEvents()
   .Call("rs_packageLibraryMutated", PACKAGE = "(embedding)")
   
   # Return installation result, invisibly.
   invisible(result)
})

formals(.rs.installPackages)     <- formals(utils::install.packages)
formals(.rs.installPackagesImpl) <- formals(utils::install.packages)

.rs.replaceBindingImpl(
   envir   = as.environment("package:utils"),
   binding = "install.packages",
   value   = .rs.installPackages
)


.rs.addFunction("removePackages", function(pkgs, lib)
{
   ""
   "This is an RStudio hook."
   "Use `utils::remove.packages()` to bypass this hook if necessary."
   ""
   
   call <- sys.call()
   call[[1L]] <- quote(.rs.removePackagesImpl)
   eval(call, envir = parent.frame())
})

.rs.addFunction("removePackagesImpl", function(pkgs, lib)
{
   # Invoke original.
   result <- utils::remove.packages(pkgs, lib)
   
   # Notify front-end that the package library was mutated.
   .Call("rs_packageLibraryMutated", PACKAGE = "(embedding)")
   
   # Return original result.
   invisible(result)
})

formals(.rs.removePackages)     <- formals(utils::remove.packages)
formals(.rs.removePackagesImpl) <- formals(utils::remove.packages)

.rs.replaceBindingImpl(
   envir   = as.environment("package:utils"),
   binding = "remove.packages",
   value   = .rs.removePackages
)
