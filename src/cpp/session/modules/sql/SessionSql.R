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

.rs.addJsonRpcHandler("sql_get_completions", function(
   line,
   conn,
   preferLowerCaseKeywords)
{
   .rs.sql.getCompletions(line, conn, preferLowerCaseKeywords)
})

.rs.addFunction("sql.getCompletions", function(
   line,
   conn,
   preferLowerCaseKeywords)
{
   # extract token
   parts <- .rs.strsplit(line, "\\s+")
   token <- tail(parts, n = 1L)
   
   # collect keywords (convert to lowercase to match token when appropriate)
   useLowerCaseKeywords <-
      (nzchar(token) && identical(token, tolower(token))) ||
      preferLowerCaseKeywords
    
   keywords <- .rs.sql.keywords(conn)
   keywords <- if (useLowerCaseKeywords)
      tolower(keywords)
   else
      toupper(keywords)
   
   # collect table fields
   fields <- .rs.sql.listTableFields(conn)
   
   # now start building completions
   completions <- Reduce(.rs.appendCompletions, list(
      
      .rs.makeCompletions(
         token = token,
         results = .rs.selectFuzzyMatches(keywords, if (nzchar(token)) token else "!"),
         packages = "keyword",
         quote = FALSE,
         type = .rs.acCompletionTypes$KEYWORD,
         language = "SQL"
      ),
      
      .rs.makeCompletions(
         token = token,
         results = .rs.selectFuzzyMatches(names(fields), token),
         packages = "table",
         type = .rs.acCompletionTypes$DATASET,
         language = "SQL"
      ),
      
      
      .rs.makeCompletions(
         token = token,
         results = .rs.selectFuzzyMatches(unlist(fields, use.names = FALSE), token),
         packages = "field",
         type = .rs.acCompletionTypes$VECTOR,
         language = "SQL"
      )

   ))
   
   completions
})

.rs.addFunction("sql.listTableFields", function(conn)
{
   if (!requireNamespace("DBI", quietly = TRUE))
      return(character())
   
   if (is.character(conn))
   {
      conn <- .rs.tryCatch(eval(parse(text = conn), envir = globalenv()))
      if (inherits(conn, "error"))
         return(character())
   }
   
   # TODO: it will likely be expensive to query all table names + associated fields
   # for large databases -- can we do something smarter here? learn more about the
   # context from the user's SQL script?
   .rs.withTimeLimit(1L, {
      tables <- DBI::dbListTables(conn)
      fields <- lapply(tables, function(table) DBI::dbListFields(conn, table))
      names(fields) <- tables
      fields
   })
   
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
