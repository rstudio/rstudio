/*
 * stan.js
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * The Initial Developer of the Original Code is Jeffrey Arnold
 * Portions created by the Initial Developer are Copyright (C) 2015
 * the Initial Developer. All Rights Reserved.
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

define("rstudio/snippets/stan", ["require", "exports", "module"], function(require, exports, module) {

var utils = require("rstudio/snippets");
var SnippetManager = require("ace/snippets").snippetManager;

var snippets = [
  {
      name: "for",
      content: [
         'for (${1:var} in ${2:start}:${3:end}) {',
         '  ${0}',
         '}'
      ].join("\n")
   },
   {
      name: "if",
      content: [
         'if (${1:condition}) {',
         '  ${0}',
         '}'
      ].join("\n")
   },
   {
      name: "el",
      content: [
         'else (${1:condition}) {',
         '  ${0}',
         '}'
      ].join("\n")
   },
   {
      name: "ei",
      content: [
         'else if (${1:condition}) {',
         '  ${0}',
         '}'
      ].join("\n")
   },
   {
      name: "<l",
      content: "<lower = ${1:expression}>${0}"
   },
   {
      name: "<u",
      content: "<upper = ${1:expression}>${0}"
   },
   {
      name: "<lu",
      content: "<lower = ${1:expression}, upper = ${2:expression}>${0}"
   },
   {
      name: "while",
      content: [
         'while (${1:condition}) {',
         '  ${0}',
         '}'
      ].join("\n")
   },
   {
      name: "gen",
      content: [
         'generated quantities {',
         '  ${0}',
         '}'
      ].join("\n")
   },
   {
      name: "mdl",
      content: [
         'model {',
         '  ${0}',
         '}'
      ].join("\n")
   },
   {
      name: "par",
      content: [
         'parameters {',
         '  ${0}',
         '}'
      ].join("\n")
   },
   {
      name: "tpar",
      content: [
         'transformed parameters {',
         '  ${0}',
         '}'
      ].join("\n")
   },
   {
      name: "data",
      content: [
         'data {',
         '  ${0}',
         '}'
      ].join("\n")
   },
   {
      name: "tdata",
      content: [
         'transformed data {',
         '  ${0}',
         '}'
      ].join("\n")
   },
   {
      name: "ode",
      content: "integrate_ode(${1:function}, ${2:y0}, ${3:t0}, ${4:t}, ${5:theta}, ${6:x_r}, ${7:x_i});"
   },
   {
      name: "funs",
      content: [
         'functions {',
         '  ${0}',
         '}'
      ].join("\n")
   },
   {
      name: "fun",
      content: [
         '${1:return} ${2:name} (${3:args}) {',
         '  ${0}',
         '}'
      ].join("\n")
   }
];

utils.normalizeSnippets(snippets);
exports.snippetText = utils.toSnippetText(snippets);

SnippetManager.register(snippets, "stan");

});
