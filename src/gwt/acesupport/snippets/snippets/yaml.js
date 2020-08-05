/*
 * yaml.js
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

define("rstudio/snippets/yaml", ["require", "exports", "module"], function(require, exports, module) {

var utils = require("rstudio/snippets");
var SnippetManager = require("ace/snippets").snippetManager;

var snippets = [
   {
      name: "key",
      content: '${1:key}: ${2:value}'
   },
   {
      name: "list",
      content: '${1:key}:\n  - ${2:value1}\n  - ${3:value2}'
   }
];

utils.normalizeSnippets(snippets);
exports.snippetText = utils.toSnippetText(snippets);

SnippetManager.register(snippets, "yaml");

});
