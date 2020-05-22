/*
 * markdown.js
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

define("rstudio/snippets/markdown", ["require", "exports", "module"], function(require, exports, module) {

var utils = require("rstudio/snippets");
var SnippetManager = require("ace/snippets").snippetManager;

var snippets = [
   {
      name: "[",
      content: '[${1:label}](${2:location})'
   },
   {
      name: "![",
      content: '![${1:label}](${2:location})'
   },
   {
      name: "r",
      content: "```{r ${1:label}, ${2:options}}\n${0}\n```"
   },
   {
      name: "rcpp",
      content: "```{r, engine='Rcpp'}\n#include <Rcpp.h>\nusing namespace Rcpp;\n\n${0}\n\n```"
   }
];

utils.normalizeSnippets(snippets);
exports.snippetText = utils.toSnippetText(snippets);

SnippetManager.register(snippets, "markdown");

});
