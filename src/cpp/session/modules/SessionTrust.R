#
# SessionTrust.R
#
# Copyright (C) 2026 by Posit Software, PBC
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

.rs.addFunction("trust.grant", function(directory = getwd())
{
   directory <- normalizePath(directory, mustWork = TRUE)
   .Call("rs_trustGrant", directory, PACKAGE = "(embedding)")
   invisible(directory)
})

.rs.addFunction("trust.revoke", function(directory = getwd())
{
   directory <- normalizePath(directory, mustWork = TRUE)
   .Call("rs_trustRevoke", directory, PACKAGE = "(embedding)")
   invisible(directory)
})

.rs.addFunction("trust.reset", function(directory = getwd())
{
   directory <- normalizePath(directory, mustWork = TRUE)
   .Call("rs_trustReset", directory, PACKAGE = "(embedding)")
   invisible(directory)
})

.rs.addFunction("trust.status", function(directory = getwd())
{
   directory <- normalizePath(directory, mustWork = TRUE)
   .Call("rs_trustStatus", directory, PACKAGE = "(embedding)")
})
