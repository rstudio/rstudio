/*
 * snippets.js
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

define("rstudio/snippets", ["require", "exports", "module"], function(require, exports, module) {

exports.toSnippetText = function(snippets)
{
   var n = snippets.length;
   var snippetText = "";
   for (var i = 0; i < n; i++)
   {
      var snippet = snippets[i];
      snippetText +=
         "snippet " + snippet.name + "\n" +
         "\t" + snippet.content.replace(/\n/g, "\n\t") + "\n\n";
   }

   return snippetText;
};

exports.normalizeSnippets = function(snippets)
{
   var n = snippets.length;
   for (var i = 0; i < n; i++)
   {
      var snippet = snippets[i];
      if (snippet.tabTrigger == null)
         snippet.tabTrigger = snippet.name;
      snippet.content = snippet.content.replace("\n    ", "\n\t");
   }
};

});
