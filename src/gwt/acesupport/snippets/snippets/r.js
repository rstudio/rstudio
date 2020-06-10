/*
 * r.js
 *
 * Copyright (C) 2020 by RStudio, PBC
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

define("rstudio/snippets/r", ["require", "exports", "module"], function(require, exports, module) {

var utils = require("rstudio/snippets");
var SnippetManager = require("ace/snippets").snippetManager;

var snippets = [

   /* Import */
   {
      name: "lib",
      content: "library(${1:package})"
   },
   {
      name: "req",
      content: 'require(${1:package})'
   },
   {
      name: "src",
      content: 'source("${1:file.R}")'
   },
   {
      name: "ret",
      content: 'return(${1:code})'
   },
   {
      name: "mat",
      content: 'matrix(${1:data}, nrow = ${2:rows}, ncol = ${3:cols})'
   },

   /* S4 snippets */
   {
      name: "sg",
      content: [
         'setGeneric("${1:generic}", function(${2:x, ...}) {',
         '    standardGeneric("${1:generic}")',
         '})'
      ].join("\n")
   },
   {
      name: "sm",
      content: [
         'setMethod("${1:generic}", ${2:class}, function(${2:x, ...}) {',
         '    ${0}',
         '})'
      ].join("\n")
   },
   {
      name: "sc",
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
         "    ${0}",
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

   /* Utilities */
   {
      name: "ts",
      content: '`r paste("#", date(), "------------------------------\\n")`'
   },

   /* Shiny */
   {
      name: "shinyapp",
      content: [
         'library(shiny)',
         '',
         'ui <- fluidPage(',
         '  ${0}',
         ')',
         '',
         'server <- function(input, output, session) {',
         '  ',
         '}',
         '',
         'shinyApp(ui, server)'
      ].join("\n")
   },
   {
      name: "shinymod",
      content: [
         '${1:name}_UI <- function(id) {',
         '  ns <- NS(id)',
         '  tagList(',
         '    ${0}',
         '  )',
         '}',
         '',
         '${1:name} <- function(input, output, session) {',
         '  ',
         '}'
      ].join("\n")
   }
];

utils.normalizeSnippets(snippets);
exports.snippetText = utils.toSnippetText(snippets);

SnippetManager.register(snippets, "r");

});
