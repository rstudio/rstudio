/*
 * mermaid_highlight_rules.js
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
define("mode/mermaid_highlight_rules", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;

var MermaidHighlightRules = function() {

   // regexp must not have capturing parentheses
   // regexps are ordered -> the first match is used
   var keywords =
      "sequenceDiagram|participant|graph|subgraph|" +
      "loop|alt|is|opt|else|end|style|linkStyle|classDef|class";

   var keywordMapper = this.createKeywordMapper({
      "keyword": keywords
   }, "identifier", false);

   this.$rules = {

      "start" : [
      {
         token: "keyword",
         merge: false,
         regex: "^\\s*graph\\s+(?:TB|BT|RL|LR|TD)\\s*$"
      },
      {
         token: "keyword",
         merge: false,
         regex: "^\\s*(?:sequenceDiagram|participant|subgraph|loop|alt(?:\\s+is)?|opt|else(?:\\s+is)?|end|style|linkStyle|classDef|class)"
      },
      {
         token : "keyword.operator",
         merge: false,
         regex : ">|\\->|\\-\\->|\\-\\-\\-|\\-\\-|\\-\\.\\->|\\-\\.|\\.\\->|==>|==|\\->>|\\-\\->>|\\-x|\\-\\-x"
      },
      {
         token : "paren.keyword.operator",
         merge : false,
         regex : "[[({\\|]"
      },
      {
         token : "paren.keyword.operator",
         merge : false,
         regex : "[\\])}]"
      },
      {
         token: "markup.list",
         merge: false,
         regex: "Note\\s+(?:left|right)\\s+of"
      },
      {
         token: "markup.list",
         merge: false,
         regex: "<br/>"
      },
      {
         token : "text",
         regex : "\\s+",
         merge : true
      }
      ]
   };
};
oop.inherits(MermaidHighlightRules, TextHighlightRules);

exports.MermaidHighlightRules = MermaidHighlightRules;
});
