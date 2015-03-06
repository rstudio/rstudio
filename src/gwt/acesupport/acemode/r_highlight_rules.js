/*
 * r_highlight_rules.js
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * The Initial Developer of the Original Code is
 * Ajax.org B.V.
 * Portions created by the Initial Developer are Copyright (C) 2010
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
define("mode/r_highlight_rules", function(require, exports, module)
{

   var oop = require("ace/lib/oop");
   var lang = require("ace/lib/lang");
   var TextHighlightRules = require("ace/mode/text_highlight_rules")
         .TextHighlightRules;
   var TexHighlightRules = require("mode/tex_highlight_rules").TexHighlightRules;

   var RHighlightRules = function()
   {

      var keywords = lang.arrayToMap(
            ("function|if|in|break|next|repeat|else|for|return|switch|while|try|tryCatch|stop|warning|require|library|attach|detach|source|setMethod|setGeneric|setGroupGeneric|setClass|setRefClass")
                  .split("|")
            );

      var buildinConstants = lang.arrayToMap(
            ("NULL|NA|TRUE|FALSE|T|F|Inf|NaN|NA_integer_|NA_real_|NA_character_|" +
             "NA_complex_").split("|")
            );

      // regexp must not have capturing parentheses. Use (?:) instead.
      // regexps are ordered -> the first match is used

      this.$rules = {
         "start" : [
            {
               // Roxygen
               token : "comment.sectionhead",
               regex : "#+(?!').*(?:----|====|####)\\s*$"
            },
            {
               // Roxygen
               token : "comment",
               regex : "#+'",
               next : "rd-start"
            },
            {
               token : "comment",
               regex : "#.*$"
            },
            {
               token : "string", // single line
               regex : '["](?:(?:\\\\.)|(?:[^"\\\\]))*?["]'
            },
            {
               token : "string", // single line
               regex : "['](?:(?:\\\\.)|(?:[^'\\\\]))*?[']"
            },
            {
               token : "string", // multi line string start
               merge : true,
               regex : '["]',
               next : "qqstring"
            },
            {
               token : "string", // multi line string start
               merge : true,
               regex : "[']",
               next : "qstring"
            },
            {
               token : "constant.numeric", // hex
               regex : "0[xX][0-9a-fA-F]+[Li]?\\b",
               merge : false
            },
            {
               token : "constant.numeric", // number + integer
               regex : "\\d+(?:\\.\\d*)?(?:[eE][+\\-]?\\d*)?[iL]?\\b",
               merge : false
            },
            {
               token : "constant.numeric", // number + integer with leading decimal
               regex : "\\.\\d+(?:[eE][+\\-]?\\d*)?[iL]?\\b",
               merge : false
            },
            {
               token : "constant.language.boolean",
               regex : "(?:TRUE|FALSE|T|F)\\b",
               merge : false
            },
            {
               token : "identifier",
               regex : "`.*?`",
               merge : false
            },
            {
               token : function(value)
               {
                  if (keywords[value])
                     return "keyword";
                  else if (buildinConstants[value])
                     return "constant.language";
                  else if (value.match(/^\.\.\d+$/))
                     return "variable.language";
                  else
                     return "identifier";
               },
               regex : "[a-zA-Z.][a-zA-Z0-9._]*"
            },
            {
               token : "keyword.operator",
               regex : ":::|::|%%|>=|<=|==|!=|\\->|<\\-|<<\\-|\\|\\||&&|=|\\+|\\-|\\*|/|\\^|>|<|!|&|\\||~|\\$|:|@",
               merge : false
            },
            {
               token : "keyword.operator.infix", // infix operators
               regex : "%.*?%",
               merge : false
            },
            {
               // Obviously these are neither keywords nor operators, but
               // labelling them as such was the easiest way to get them
               // to be colored distinctly from regular text
               token : "paren.keyword.operator",
               merge : false,
               regex : "[[({]"
            },
            {
               // Obviously these are neither keywords nor operators, but
               // labelling them as such was the easiest way to get them
               // to be colored distinctly from regular text
               token : "paren.keyword.operator",
               merge : false,
               regex : "[\\])}]"
            },
            {
               token : "punctuation",
               regex : "[;,]",
               merge : false
            },
            {
               token : "text",
               regex : "\\s+"
            }
         ],
         "qqstring" : [
            {
               token : "string",
               regex : '(?:(?:\\\\.)|(?:[^"\\\\]))*?"',
               next : "start"
            },
            {
               token : "string",
               regex : '.+',
               merge : true
            }
         ],
         "qstring" : [
            {
               token : "string",
               regex : "(?:(?:\\\\.)|(?:[^'\\\\]))*?'",
               next : "start"
            },
            {
               token : "string",
               regex : '.+',
               merge : true
            }
         ]
      };

      var rdRules = new TexHighlightRules("comment").getRules();

      // Make all embedded TeX virtual-comment so they don't interfere with
      // auto-indent.
      for (var i = 0; i < rdRules["start"].length; i++) {
         rdRules["start"][i].token += ".virtual-comment";
      }

      this.addRules(rdRules, "rd-");
      this.$rules["rd-start"].unshift({
          token: "text",
          regex: "^",
          next: "start"
      });
      this.$rules["rd-start"].unshift({
         token : "keyword",
         regex : "@(?!@)[^ ]*"
      });
      this.$rules["rd-start"].unshift({
         token : "comment",
         regex : "@@"
      });
      this.$rules["rd-start"].push({
         token : "comment",
         regex : "[^%\\\\[({\\])}]+"
      });
   };

   oop.inherits(RHighlightRules, TextHighlightRules);

   exports.RHighlightRules = RHighlightRules;
});
