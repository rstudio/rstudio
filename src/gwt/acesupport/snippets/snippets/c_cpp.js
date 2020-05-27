/*
 * c_cpp.js
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

define("rstudio/snippets/c_cpp", ["require", "exports", "module"], function(require, exports, module) {

var utils = require("rstudio/snippets");
var SnippetManager = require("ace/snippets").snippetManager;

var snippets = [
   {
      name: "once",
      content: [
         "#ifndef ${1:`HeaderGuardFileName`}",
         "#define ${1:`HeaderGuardFileName`}",
         "",
         "${0}",
         "",
         "#endif /* ${1:`HeaderGuardFileName`} */"
      ].join("\n")
   },
   {
      name: "ans",
      content: [
         "namespace {",
         "${0}",
         "} // anonymous namespace"
      ].join("\n")
   },
   {
      name: "ns",
      content: [
         "namespace ${1:ns} {",
         "${0}",
         "} // namespace ${1:ns}"
      ].join("\n")
   },
   {
      name: "cls",
      content: [
         "class ${1:ClassName} {",
         "public:",
         "    ${2}",
         "private:",
         "    ${3}",
         "};"
      ].join("\n")
   },
   {
      name: "str",
      content: [
         "struct ${1} {",
         "    ${0}",
         "};"
      ].join("\n")
   },
   {
      name: "ept",
      content: "// [[Rcpp::export]]\n"
   }
];

utils.normalizeSnippets(snippets);
exports.snippetText = utils.toSnippetText(snippets);

SnippetManager.register(snippets, "c_cpp");

});
