#
# SessionDataPreview.R
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

# Expressions which the user has explicitly permitted to run during a preview
# this session. Membership is checked with identical() against the
# (srcref-stripped) parsed expression; we deliberately do not persist this set
# to disk, so an approval never silently re-arms across sessions or projects.
.rs.setVar("preview.permittedExprs", new.env(parent = emptyenv()))

# Cache for the administrator-configured allowlist extension (read lazily from
# the 'preview-allowed-functions' session option); NULL until first read.
.rs.setVar("preview.allowedFunctions", NULL)

# The set of function calls we consider safe to evaluate without prompting when
# resolving a preview expression (a SQL '!preview conn=' connection, or an
# argument to an r2d3 preview). These are database connection constructors plus
# a handful of pure helpers commonly used to build arguments. The list is
# intentionally small; any call whose callee is not named here is treated as
# unsafe and requires explicit consent.
.rs.addFunction("preview.safeCallAllowlist", function()
{
   builtin <- c(
      # database connection constructors (sorted alphabetically)
      "bigquery", "bigrquery::bigquery",
      "dbConnect", "DBI::dbConnect",
      "dbDriver", "DBI::dbDriver",
      "dbPool", "pool::dbPool",
      "duckdb", "duckdb::duckdb",
      "MariaDB", "RMariaDB::MariaDB",
      "MySQL", "RMySQL::MySQL",
      "odbc", "odbc::odbc",
      "Postgres", "RPostgres::Postgres",
      "PostgreSQL", "RPostgreSQL::PostgreSQL",
      "Redshift", "RPostgres::Redshift",
      "Spark", "sparklyr::spark_connect",
      "SQLite", "RSQLite::SQLite",

      # pure helpers for constructing arguments (sorted alphabetically)
      "c", "file.path", "getOption", "list",
      "paste", "paste0", "sprintf", "Sys.getenv"
   )

   # Sites can extend the allowlist with their own connection helpers (for
   # example, an internally-built package) via the 'preview-allowed-functions'
   # session option (set in rsession.conf or by the session launcher). This
   # only widens which *callees* are permitted; their arguments are still
   # validated recursively, so it cannot be used to wave through arbitrary
   # code. It is safe to honor here because untrusted file content can never
   # reach this configuration: a preview expression's code does not run until
   # the classifier has already approved it.
   c(builtin, .rs.preview.configuredAllowlist())
})

# Additional safe callees configured by an administrator via the
# 'preview-allowed-functions' session option. The option is a single string of
# function names ('pkg::fn' or bare 'fn') separated by commas or whitespace.
# The value is fixed for the lifetime of the session, so cache it after the
# first read.
.rs.addFunction("preview.configuredAllowlist", function()
{
   if (is.null(.rs.preview.allowedFunctions))
   {
      value <- .rs.tryCatch(.Call("rs_previewAllowedFunctions", PACKAGE = "(embedding)"))
      parts <- if (is.character(value) && length(value))
         unlist(strsplit(value, "[,[:space:]]+"))
      else
         character()

      .rs.setVar("preview.allowedFunctions", parts[nzchar(parts)])
   }

   .rs.preview.allowedFunctions
})

# Resolve the name of a call's function position to a character string, for
# matching against the allowlist. Handles bare symbols ('dbConnect') and
# namespace-qualified access ('DBI::dbConnect'). Returns NULL for anything
# else (e.g. a call that itself produces a function).
.rs.addFunction("preview.callName", function(callee)
{
   if (is.symbol(callee))
      return(as.character(callee))

   if (is.call(callee) &&
       (identical(callee[[1L]], as.symbol("::")) ||
        identical(callee[[1L]], as.symbol(":::"))))
   {
      return(paste0(as.character(callee[[2L]]), "::", as.character(callee[[3L]])))
   }

   NULL
})

# Determine, via static analysis, whether a parsed preview expression is safe
# to evaluate without prompting the user. The expression comes from a file's
# '!preview' header, so it is fully attacker-controlled when the file comes
# from an untrusted source.
#
# An expression is safe when it is:
#   - a literal constant (no evaluation runs code), or
#   - a bare symbol that is not an active binding (a plain variable lookup), or
#   - namespace access ('pkg::name', 'pkg:::name'), or
#   - a parenthesized safe expression, or
#   - a call to an allow-listed function in which every argument is itself safe.
#
# Everything else (notably arbitrary function calls such as system(...)) is
# unsafe. Note that namespace qualification does not make a call safe:
# 'base::system(...)' is just as dangerous as 'system(...)', so the allowlist
# is consulted by name and the arguments are validated recursively.
#
# 'envir' is the environment in which the expression would ultimately be
# evaluated (the preview code path uses globalenv()). It is required to decide
# whether a bare symbol would trigger an active binding on lookup, so it is a
# parameter rather than being hard-coded -- both to keep the contract honest
# and so the classifier can be tested against a constructed environment.
.rs.addFunction("preview.isSafeExpr", function(expr, envir = globalenv())
{
   # literal constants (strings, numbers, logicals, NULL, ...) are safe
   if (!is.language(expr))
      return(TRUE)

   # a bare symbol is a plain variable lookup; safe unless it is an active
   # binding, in which case the lookup itself would execute a function
   if (is.symbol(expr))
   {
      name <- as.character(expr)
      if (!nzchar(name))
         return(FALSE)

      # eval() resolves symbols starting in 'envir' and walking its enclosing
      # environments (for globalenv() that is the attached search path), so an
      # active binding anywhere along that chain -- not just in 'envir' itself
      # -- would execute code on lookup. Walk from 'envir' up its parents (the
      # same order eval uses) to the environment that actually holds the name,
      # and reject it if that binding is active.
      env <- envir
      while (!identical(env, emptyenv()))
      {
         if (exists(name, envir = env, inherits = FALSE))
         {
            if (bindingIsActive(name, env))
               return(FALSE)
            break
         }
         env <- parent.env(env)
      }

      return(TRUE)
   }

   # anything that is not a literal or symbol must be a call to be safe
   if (!is.call(expr))
      return(FALSE)

   callee <- expr[[1L]]

   # namespace access ('pkg::name') only resolves a binding; it does not call
   if (identical(callee, as.symbol("::")) || identical(callee, as.symbol(":::")))
      return(TRUE)

   # a parenthesized expression is safe iff its body is safe
   if (identical(callee, as.symbol("(")))
      return(.rs.preview.isSafeExpr(expr[[2L]], envir = envir))

   # a regular call is safe only if the callee is allow-listed by name and
   # every argument is itself safe (checked recursively)
   name <- .rs.preview.callName(callee)
   if (is.null(name) || !(name %in% .rs.preview.safeCallAllowlist()))
      return(FALSE)

   args <- as.list(expr)[-1L]
   for (arg in args)
      if (!.rs.preview.isSafeExpr(arg, envir = envir))
         return(FALSE)

   TRUE
})

.rs.addFunction("preview.isPermittedExpr", function(expr)
{
   permitted <- .rs.preview.permittedExprs$items
   for (item in permitted)
      if (identical(item, expr))
         return(TRUE)
   FALSE
})

.rs.addFunction("preview.permitExpr", function(expr)
{
   if (!.rs.preview.isPermittedExpr(expr))
      .rs.preview.permittedExprs$items <-
         c(.rs.preview.permittedExprs$items, list(expr))

   invisible(NULL)
})

# Classify a parsed preview expression as "safe" (statically safe to
# evaluate), "permitted" (the user has explicitly allowed it this session), or
# "unsafe" (requires explicit consent before evaluation).
.rs.addFunction("preview.exprStatus", function(expr, envir = globalenv())
{
   if (.rs.preview.isSafeExpr(expr, envir = envir))
      return("safe")

   if (.rs.preview.isPermittedExpr(expr))
      return("permitted")

   "unsafe"
})

.rs.addJsonRpcHandler("preview_sql", function(code, allow = FALSE)
{
   # result helpers; the front-end dispatches on 'action'
   onError <- function(reason) {
      list(action = .rs.scalar("error"),
           message = .rs.scalar(.rs.truncate(reason)))
   }

   onConfirm <- function(expr) {
      list(action = .rs.scalar("confirm"),
           expression = .rs.scalar(.rs.truncate(.rs.deparse(expr))))
   }

   onSuccess <- function() {
      list(action = .rs.scalar("ok"))
   }

   # parse the user-provided code (strip srcref so connection expressions
   # compare equal against the permitted set maintained in SessionSql.R)
   parsed <- .rs.tryCatch(parse(text = code, keep.source = FALSE)[[1]])
   if (inherits(parsed, "error"))
      return(onError("Failed to parse SQL preview comment."))

   # require the canonical preview call shape. the '!preview' header lets the
   # user override the function name (e.g. '-- !preview system(...)'), so we
   # must confirm the callee is our own preview helper before evaluating --
   # otherwise the function position is itself a code-injection vector.
   if (!is.call(parsed) || !identical(parsed[[1L]], quote(.rs.previewSql)))
      return(onError("Unsupported SQL preview comment."))

   # substitute missing arguments for NULL, so that match.call
   # does what we expect
   for (i in seq_along(parsed))
      if (identical(parsed[[i]], quote(expr = )))
         parsed[i] <- list(NULL)

   # attempt to match the call
   matched <- .rs.tryCatch(match.call(.rs.previewSql, parsed))
   if (inherits(matched, "error"))
      return(onError("Unexpected SQL preview comment."))

   # validate that the user provided a connection
   if (is.null(matched$conn))
      return(onError("No connection was specified in SQL preview comment."))

   # every argument other than the connection (the statement path, plus any
   # extra arguments forwarded to DBI::dbSendQuery) must be statically safe,
   # so they cannot smuggle in arbitrary code alongside a benign connection
   others <- as.list(matched)[-1L]
   others$conn <- NULL
   for (arg in others)
      if (!.rs.preview.isSafeExpr(arg))
         return(onError("Unsupported SQL preview comment."))

   # classify the connection expression itself; arbitrary expressions require
   # the user's explicit consent before we evaluate them
   connExpr <- matched$conn
   if (identical(.rs.preview.exprStatus(connExpr), "unsafe"))
   {
      if (!isTRUE(allow))
         return(onConfirm(connExpr))

      # the user approved this expression; remember it for the rest of the
      # session so future previews and completions resolve it without prompting
      .rs.preview.permitExpr(connExpr)
   }

   # okay, try to evaluate it
   status <- .rs.tryCatch(eval(parsed, envir = globalenv()))
   if (inherits(status, "error"))
      return(onError(conditionMessage(status)))

   # .rs.previewSql reports connection/query failures by returning a
   # (non-empty) message string rather than by signaling an error; surface
   # those to the user just as the previous string-returning contract did
   if (is.character(status) && length(status) && nzchar(status))
      return(onError(status))

   onSuccess()
})

# Consent check for the r2d3 ('// !preview r2d3 ...') file preview. Unlike SQL
# preview, the built 'r2d3::r2d3(...)' call is run in the console (so r2d3 can
# render the widget), so this handler only classifies the command and reports
# whether it is safe to run -- the front-end performs the console execution.
.rs.addJsonRpcHandler("preview_r2d3", function(code, allow = FALSE)
{
   onError <- function(reason) {
      list(action = .rs.scalar("error"),
           message = .rs.scalar(.rs.truncate(reason)))
   }

   onConfirm <- function(expr) {
      list(action = .rs.scalar("confirm"),
           expression = .rs.scalar(.rs.truncate(.rs.deparse(expr))))
   }

   onSuccess <- function() {
      list(action = .rs.scalar("ok"))
   }

   parsed <- .rs.tryCatch(parse(text = code, keep.source = FALSE)[[1]])
   if (inherits(parsed, "error"))
      return(onError("Failed to parse preview comment."))

   # the front-end forces the call to r2d3::r2d3; confirm that here so the
   # function position cannot itself be used to inject code
   if (!is.call(parsed) || !identical(parsed[[1L]], quote(r2d3::r2d3)))
      return(onError("Unsupported preview comment."))

   # every argument (the source path plus header arguments such as 'data=')
   # must be statically safe or already permitted; otherwise ask for consent
   args <- as.list(parsed)[-1L]
   unsafe <- Filter(function(arg) identical(.rs.preview.exprStatus(arg), "unsafe"), args)
   if (length(unsafe))
   {
      if (!isTRUE(allow))
         return(onConfirm(unsafe[[1L]]))

      for (arg in unsafe)
         .rs.preview.permitExpr(arg)
   }

   onSuccess()
})

.rs.addFunction("previewDataFrame", function(data, script)
{
   preparedData <- .rs.prepareViewerData(
      data,
      maxFactors = 100,
      maxCols = 100,
      maxRows = 1000
   )

   preview <- list(
      data = preparedData$data,
      columns = preparedData$columns,
      title = if (is.character(script)) .rs.scalar(script) else NULL
   )

   .rs.enqueClientEvent("data_output_completed", preview)
   
   invisible(NULL)

})

.rs.addFunction("previewSql", function(conn, statement, ...)
{
   script <- NULL
   if (file.exists(statement)) {
      script <- statement
      statement <- paste(readLines(script), collapse = "\n")
   }

   # remove comments since some drivers might not support them
   statement <- gsub("--[^\n]*\n+", "", statement)

   # force the connection to let DBI and others initialize S3
   conn <- .rs.tryCatch(force(conn))
   if (inherits(conn, "error")) {
      msg <- paste("Failed to retrieve connection:", conditionMessage(conn))
      return(.rs.scalar(msg))
   }

   # fetch at most 100 records as a preview
   status <- .rs.tryCatch({
      rs <- DBI::dbSendQuery(conn, statement = statement, ...)
      data <- DBI::dbFetch(rs, n = 1000)
      DBI::dbClearResult(rs)
   })
   
   if (inherits(status, "error")) {
      msg <- paste("Failed to query database:", conditionMessage(status))
      return(.rs.scalar(msg))
   }

   .rs.previewDataFrame(data, script)
})

