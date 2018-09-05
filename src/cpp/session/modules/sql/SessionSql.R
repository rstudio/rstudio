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

.rs.addJsonRpcHandler("sql_get_completions", function(line)
{
   .rs.sql.getCompletions(line)
})

.rs.addFunction("sql.getCompletions", function(line)
{
   # TODO: harvest completion results from associated SQL connection
   # - handle for mixed-mode documents (request chunk options)
   # - handle for SQL scripts (look for preview line)
   # TODO: completion keywords on per-database case
   # TODO: options for casing
   # - smart (prefer upper)
   # - smart (prefer lower)
   # - upper
   # - lower
   
   # extract token
   parts <- .rs.strsplit(line, "\\s+")
   token <- tail(parts, n = 1L)
   
   # match against keywords
   keywords <- .rs.sql.keywords()
   if (identical(token, tolower(token)))
      keywords <- tolower(keywords)
   
   results <- .rs.selectFuzzyMatches(keywords, token)
   .rs.makeCompletions(token = token, results = results)
})

.rs.addFunction("sql.keywords", function()
{
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
