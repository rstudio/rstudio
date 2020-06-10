#
# SessionAskSecret.R
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

.rs.addFunction("getSecretService", function() {
   "RStudio Keyring Secrets"
})

.rs.addFunction("askForSecret", function(name, title = name, prompt = paste(name, ":", sep = ""))
{
   result <- .Call(
      "rs_askForSecret",
      name,
      title,
      prompt,
      .rs.isPackageInstalled("keyring"),
      .rs.hasSecret(name)
   )

   if (is.null(result)) stop("Ask for secret operation was cancelled.")
   
   result
})

.rs.addFunction("hasSecret", function(name)
{
   if (!.rs.isPackageInstalled("keyring"))
   {
      FALSE
   }
   else
   {
      keyring_list <- get("key_list", envir = asNamespace("keyring"))
      keys <- keyring_list(.rs.getSecretService())
      name %in% keys$username
   }
})

.rs.addFunction("retrieveSecret", function(name)
{
   if (!.rs.isPackageInstalled("keyring") || !.rs.hasSecret(name))
   {
      NULL
   }
   else 
   {
      keyring_get <- get("key_get", envir = asNamespace("keyring"))
      keyring_get(.rs.getSecretService(), username = name)
   }
})

.rs.addFunction("rememberSecret", function(name, secret)
{
   if (.rs.isPackageInstalled("keyring"))
   {
      keyring_set <- get("key_set_with_value", envir = asNamespace("keyring"))
      keyring_set(.rs.getSecretService(), username = name, password = secret)
   }
})

.rs.addFunction("forgetSecret", function(name)
{
   if (.rs.isPackageInstalled("keyring") && .rs.hasSecret(name))
   {
      keyring_remove <- get("key_delete", envir = asNamespace("keyring"))
      keyring_remove(.rs.getSecretService(), username = name)
   }
})
