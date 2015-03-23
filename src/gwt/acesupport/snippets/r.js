/*
 * r.js
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

define("rstudio/snippets/r", function(require, exports, module) {

var SnippetManager = require("ace/snippets").snippetManager;

var snippets = [

   /* S4 snippets */
   {
      name: "setGeneric",
      content: [
         'setGeneric("${1:generic}", function(${2:x, ...}) {',
         '    standardGeneric("${1:generic}")',
         '})'
      ].join("\n")
   },
   {
      name: "setMethod",
      content: [
         'setGeneric("{$1:generic}", function(${2:x, ...}) {',
         '    ${0}',
         '})'
      ].join("\n")
   },
   {
      name: "setClass",
      content: [
         'setClass("${1:Class}", slots = c(${2:name = "type"}))'
      ].join("\n")
   },

   /* Control Flow and Keywords */
   {
      name: "if",
      content: [
         'if (${1:condition}) {',
         '    ${0}',
         '}'
      ].join("\n")
   },
   {
      name: "el",
      content: [
         'else {',
         '    ${0}',
         '}'
      ].join("\n")
   },
   {
      name: "ei",
      content: [
         'else if (${1:condition}) {',
         '    ${0}',
         '}'
      ].join("\n")
   },
   {
      name: "fun",
      content: [
         "${1:name} <- function(${2:variables}) {",
         "    {0}",
         "}"
      ].join("\n")
   },
   {
      name: "for",
      content: [
         'for (${1:variable} in ${2:vector}) {',
         '    ${0}',
         '}'
      ].join("\n")
   },
   {
      name: "while",
      content: [
         'while (${1:condition}) {',
         '    ${0}',
         '}'
      ].join("\n")
   },
   {
      name: "switch",
      content: [
         'switch (${1:object},',
         '    ${2:case} = ${3:action}',
         ')'
      ].join("\n")
   },
   {
      name: "tryCatch",
      content: [
         'tryCatch({',
         '    ${1:code}',
         '}, error = function(e) {',
         '    ${2:error}',
         '})'
      ].join("\n")
   },

   /* Avoid stringsAsFactors */
   {
      name: "df",
      content: 'data.frame(${1:...}, stringsAsFactors = FALSE)'
   },
   {
      name: "read.csv",
      content: 'read.csv(${1:...}, stringsAsFactors = FALSE)'
   },

   /* Apply */
   {
      name: "apply",
      content: 'apply(${1:array}, ${2:margin}, ${3:...})'
   },
   {
      name: "lapply",
      content: "lapply(${1:list}, ${2:function})"
   },
   {
      name: "sapply",
      content: "sapply(${1:list}, ${2:function})"
   },
   {
      name: "mapply",
      content: "mapply(${1:function}, ${2:...})"
   },
   {
      name: "tapply",
      content: "tapply(${1:vector}, ${2:index}, ${3:function})"
   },
   {
      name: "vapply",
      content: 'vapply(${1:list}, ${2:function}, FUN.VALUE = ${3:type}, ${4:...})'
   },
   {
      name: "rapply",
      content: "rapply(${1:list}, ${2:function})"
   },

   /* Regular Expression */
   {
      name: "grep",
      content: 'grep("${1:pattern}", "${2:text}", perl = TRUE)'
   },
   {
      name : "grepl",
      content: 'grepl("${1:pattern}", "${2:text}", perl = TRUE)'
   },
   {
      name : "sub",
      content: 'sub("${1:pattern}", "${2:replacement}", "${3:text}", perl = TRUE)'
   },
   {
      name : "gsub",
      content: 'gsub("${1:pattern}", "${2:replacement}", "${3:text}", perl = TRUE)'
   },
   {
      name : "regexpr",
      content: 'regexpr("${1:pattern}", "${2:text}", perl = TRUE)'
   },
   {
      name : "gregexpr",
      content: 'gregexpr("${1:pattern}", "${2:text}", perl = TRUE)'
   },
   {
      name : "regexec",
      content: 'regexec("${1:pattern}", "${2:text}", perl = TRUE)'
   },

   /* Miscellaneous */
   {
      name: "requireNamespace",
      content: 'requireNamespace("${1}", quietly = TRUE)'
   },
   {
      name: "now",
      content: "Sys.time()"
   },
   {
      name: "today",
      content: "Sys.Date()"
   }
];

SnippetManager.register(snippets, "r");

});
