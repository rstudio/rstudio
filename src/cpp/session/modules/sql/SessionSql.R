#
# SessionSql.R
#
# Copyright (C) 2009-18 by RStudio, Inc.
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

.rs.setVar("sql.connectionBlacklist", new.env(parent = emptyenv()))

.rs.addJsonRpcHandler("sql_get_completions", function(...)
{
   .rs.sql.getCompletions(...)
})

.rs.addFunction("sql.getCompletions", function(line, conn, ctx)
{
   # extract token
   parts <- .rs.strsplit(line, "\\s+")
   token <- tail(parts, n = 1L)
   
   # if we have a '.' in the token, assume that we're
   # attempting to complete field names from a table
   if (grepl(".", token, fixed = TRUE))
      return(.rs.sql.getCompletionsFields(token, conn, ctx))
   
   # check if we're explicitly requesting table name completions
   if (length(parts) > 1)
   {
      previous <- parts[[length(parts) - 1L]]
      if (tolower(previous) %in% c("from", "into", "join", "update"))
         return(.rs.sql.getCompletionsTables(token, conn, ctx))
   }
   
   # otherwise, gather completions from other sources
   Reduce(.rs.appendCompletions, list(
      .rs.sql.getCompletionsKeywords(token, conn, ctx),
      .rs.sql.getCompletionsTables(token, conn, ctx),
      .rs.sql.getCompletionsFields(token, conn, ctx),
      .rs.sql.getCompletionsIdentifiers(token, conn, ctx)
   ))
})

.rs.addFunction("sql.getCompletionsKeywords", function(token, conn, ctx)
{
   if (!nzchar(token))
      return(.rs.emptyCompletions(language = "SQL"))
   
   # collect keywords (convert to lowercase to match token when appropriate)
   lowercase <- if (nzchar(token))
      identical(token, tolower(token))
   else
      ctx$preferLowercaseKeywords
    
   keywords <- if (lowercase)
      tolower(.rs.sql.keywords(conn))
   else
      toupper(.rs.sql.keywords(conn))
   
   results <- .rs.selectFuzzyMatches(keywords, token)
   .rs.makeCompletions(
      token = token,
      results = results,
      packages = "keyword",
      type = .rs.acCompletionTypes$KEYWORD,
      language = "SQL"
   )
})

.rs.addFunction("sql.getCompletionsTables", function(token, conn, ctx)
{
   # use context-completed tables by default
   tables <- ctx$tables
   
   # attempt to derive table names from connection
   conn <- .rs.sql.asDBIConnection(conn)
   if (!is.null(conn))
   {
      listedTables <- .rs.sql.listTables(conn)
      if (length(listedTables))
         tables <- listedTables
   }
   
   # if we didn't get any tables, user context completions
   if (!length(tables))
      tables <- ctx$tables
   
   # add in alias names since the user might want to use those in lieu
   # of the 'real' table name in certain contexts
   tables <- c(tables, names(ctx$aliases))
   
   results <- .rs.selectFuzzyMatches(tables, token)
   .rs.makeCompletions(
      token = token,
      results = results,
      packages = "table",
      type = .rs.acCompletionTypes$DATAFRAME
   )
})

.rs.addFunction("sql.getCompletionsFields", function(token, conn, ctx)
{
   conn <- .rs.sql.asDBIConnection(conn)
   if (is.null(conn))
      return(.rs.emptyCompletions(language = "SQL"))
   
   # if we have a '.' in the token, then only retrieve
   # completions from the requested table
   tables <- ctx$tables
   if (grepl(".", token, fixed = TRUE))
   {
      parts <- .rs.strsplit(token, ".", fixed = TRUE)
      tables <- parts[[length(parts) - 1]]
      token  <- parts[[length(parts) - 0]]
   }
   
   Reduce(.rs.appendCompletions, lapply(tables, function(table) {
      
      # if this table is an alias, find the 'true' table name
      if (table %in% names(ctx$aliases))
         table <- ctx$aliases[[table]]
      
      fields <- .rs.tryCatch(DBI::dbListFields(conn, table))
      if (inherits(fields, "error"))
         return(.rs.emptyCompletions(language = "SQL"))
      
      .rs.makeCompletions(
         token = token,
         results = .rs.selectFuzzyMatches(fields, token),
         packages = table,
         type = .rs.acCompletionTypes$UNKNOWN
      )
      
   }))
   
})

.rs.addFunction("sql.getCompletionsIdentifiers", function(token, conn, ctx)
{
   identifiers <- setdiff(as.character(ctx$identifiers), token)
   results <- .rs.selectFuzzyMatches(identifiers, token)
   .rs.makeCompletions(
      token = token,
      results = results,
      type = .rs.acCompletionTypes$CONTEXT,
      language = "SQL"
   )
})

.rs.addFunction("sql.listTables", function(conn)
{
   conn <- .rs.sql.asDBIConnection(conn)
   if (is.null(conn))
      return(character())
   
   # purge any disconnected connections (avoid keeping a live reference)
   keys <- ls(envir = .rs.sql.connectionBlacklist)
   for (key in keys)
      if (!DBI::dbIsValid(.rs.sql.connectionBlacklist[[key]]))
         rm(key, envir = .rs.sql.connectionBlacklist)
   
   # if the user has opted out of completions altogether
   # then just use context completions
   timeout <- as.numeric(getOption("sql.completion.timeout", 2))
   if (timeout == 0)
      return(character())
   
   # check and see whether we've blacklisted this connection
   id <- digest::digest(conn)
   if (!is.null(.rs.sql.connectionBlacklist[[id]]))
      return(character())
   
   # request completions; if we take too long then blacklist further
   # attempts to get completions
   time <- system.time(tables <- .rs.tryCatch(DBI::dbListTables(conn)))
   if (time[["user.self"]] > timeout)
   {
      .rs.sql.connectionBlacklist[[id]] <- conn
      return(character())
   }
   
   # we got completions in time; use them
   tables
})

.rs.addFunction("sql.keywords", function(conn)
{
   # allow users to define their own set of keywords if preferred
   keywords <- getOption("sql.keywords")
   if (is.function(keywords))
   {
      if (length(formals(keywords)))
         return(keywords(conn))
      else
         return(keywords())
   }
   else if (is.character(keywords))
   {
      return(keywords)
   }
   
   # try to see if we can figure out the set of keywords from
   # the conn itself
   if (is.character(conn))
      conn <- .rs.tryCatch(eval(parse(text = conn), envir = globalenv()))
   
   # NOTE: these are the keywords understood by SQLite, as per
   # https://www.sqlite.org/lang_keywords.html
   c(
      "ABORT", "ACTION", "ADD", "AFTER", "ALL", "ALTER", "ANALYZE", "AND",
      "AS", "ASC", "ATTACH", "AUTOINCREMENT", "BEFORE", "BEGIN", "BETWEEN",
      "BY", "CASCADE", "CASE", "CAST", "CHECK", "COLLATE", "COLUMN", "COMMIT",
      "CONFLICT", "CONSTRAINT", "CREATE", "CROSS", "CURRENT_DATE", "CURRENT_TIME",
      "CURRENT_TIMESTAMP", "DATABASE", "DEFAULT", "DEFERRABLE", "DEFERRED",
      "DELETE", "DESC", "DETACH", "DISTINCT", "DROP", "EACH", "ELSE", "END",
      "ESCAPE", "EXCEPT", "EXCLUSIVE", "EXISTS", "EXPLAIN", "FAIL", "FOR",
      "FOREIGN", "FROM", "FULL", "GLOB", "GROUP", "HAVING", "IF", "IGNORE",
      "IMMEDIATE", "IN", "INDEX", "INDEXED", "INITIALLY", "INNER", "INSERT",
      "INSTEAD", "INTERSECT", "INTO", "IS", "ISNULL", "JOIN", "KEY", "LEFT",
      "LIKE", "LIMIT", "MATCH", "NATURAL", "NO", "NOT", "NOTNULL", "NULL", "OF",
      "OFFSET", "ON", "OR", "ORDER", "OUTER", "PLAN", "PRAGMA", "PRIMARY", "QUERY",
      "RAISE", "RECURSIVE", "REFERENCES", "REGEXP", "REINDEX", "RELEASE", "RENAME",
      "REPLACE", "RESTRICT", "RIGHT", "ROLLBACK", "ROW", "SAVEPOINT", "SELECT",
      "SET", "TABLE", "TEMP", "TEMPORARY", "THEN", "TO", "TRANSACTION", "TRIGGER",
      "UNION", "UNIQUE", "UPDATE", "USING", "VACUUM", "VALUES", "VIEW", "VIRTUAL",
      "WHEN", "WHERE", "WITH", "WITHOUT"
   )
})

.rs.addFunction("sql.asDBIConnection", function(conn)
{
   if (inherits(conn, "DBIConnection"))
      return(conn)
   
   if (is.character(conn))
   {
      if (!requireNamespace("DBI", quietly = TRUE))
         return(NULL)
      
      conn <- .rs.tryCatch(eval(parse(text = conn), envir = globalenv()))
      if (inherits(conn, "error"))
         return(NULL)
   }
   
   conn
})
