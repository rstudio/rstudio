
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
