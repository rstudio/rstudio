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

var $colorFunctionCalls = false;

define("mode/r_highlight_rules", ["require", "exports", "module"], function(require, exports, module)
{
   function include(/*...*/) {
      var result = new Array(arguments.length);
      for (var i = 0; i < arguments.length; i++) {
         result[i] = {include: arguments[i]};
      }
      return result;
   }

   var oop = require("ace/lib/oop");
   var lang = require("ace/lib/lang");
   var TextHighlightRules = require("ace/mode/text_highlight_rules")
         .TextHighlightRules;
   var TexHighlightRules = require("mode/tex_highlight_rules").TexHighlightRules;

   var RHighlightRules = function()
   {
      var keywords = lang.arrayToMap([
         "function", "if", "else", "in", "break", "next", "repeat", "for", "while"
      ]);

      var specialFunctions = lang.arrayToMap([
         "return", "switch", "try", "tryCatch", "stop",
         "warning", "require", "library", "attach", "detach",
         "source", "setMethod", "setGeneric", "setGroupGeneric",
         "setClass", "setRefClass", "R6Class", "UseMethod", "NextMethod"
      ]);

      var builtinConstants = lang.arrayToMap([
         "NULL", "NA", "TRUE", "FALSE", "T", "F", "Inf",
         "NaN", "NA_integer_", "NA_real_", "NA_character_",
         "NA_complex_"
      ]);

      var reIdentifier = "[a-zA-Z.][a-zA-Z0-9._]*";

      var rules = {};

      // Define rule sub-blocks that can be included to create
      // full rule states.
      rules["#comment"] = [
         {
            token : "comment.sectionhead",
            regex : "#+(?!').*(?:----|====|####)\\s*$",
            next  : "start"
         },
         {
            // Roxygen
            token : "comment",
            regex : "#+'",
            next  : "rd-start"
         },
         {
            token : "comment",
            regex : "#.*$",
            next  : "start"
         }
      ];

      rules["#string"] = [
         {
            token : "string", // single line
            regex : '["](?:(?:\\\\.)|(?:[^"\\\\]))*?["]',
            next  : "start"
         },
         {
            token : "string", // single line
            regex : "['](?:(?:\\\\.)|(?:[^'\\\\]))*?[']",
            next  : "start"
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
         }
      ];

      rules["#number"] = [
         {
            token : "constant.numeric", // hex
            regex : "0[xX][0-9a-fA-F]+[Li]?",
            merge : false,
            next  : "start"
         },
         {
            token : "constant.numeric", // number + integer
            regex : "(?:(?:\\d+(?:\\.\\d*)?)|(?:\\.\\d+))(?:[eE][+\\-]?\\d*)?[iL]?",
            merge : false,
            next  : "start"
         }
      ];

      rules["#quoted-identifier"] = [
         {
            token : "identifier",
            regex : "`.*?`",
            merge : false,
            next  : "start"
         }
      ];

      rules["#identifier"] = [
         {
            token : function(value)
            {
               if (builtinConstants.hasOwnProperty(value))
                  return "constant.language";
               else if (value.match(/^\.\.\d+$/))
                  return "variable.language";
               else
                  return "identifier";
            },
            regex : reIdentifier,
            next  : "start"
         }
      ];

      rules["#keyword-or-identifier"] = [
         {
            token : function(value)
            {
               if (builtinConstants.hasOwnProperty(value))
                  return "constant.language";
               else if (keywords.hasOwnProperty(value))
                  return "keyword";
               else if (value.match(/^\.\.\d+$/))
                  return "variable.language";
               else
                  return "identifier";
            },
            regex : reIdentifier,
            next  : "start"
         }
      ];

      rules["#package-access"] = [
         {
            token : function(value) {
               if ($colorFunctionCalls)
                  return "identifier.support.class";
               else
                  return "identifier";
            },
            regex : reIdentifier + "(?=\\s*::)",
            next  : "start"
         }
      ];

      rules["#function-call"] = [
         {
            token : function(value) {
               if ($colorFunctionCalls)
                  return "identifier.support.function";
               else
                  return "identifier";
            },
            regex : reIdentifier + "(?=\\s*\\()",
            next  : "start"
         }
      ];

      rules["#function-call-or-keyword"] = [
         {
            token : function(value) {
               if (specialFunctions.hasOwnProperty(value) || keywords.hasOwnProperty(value))
                  return "keyword";
               else if ($colorFunctionCalls)
                  return "identifier.support.function";
               else
                  return "identifier";
            },
            regex : reIdentifier + "(?=\\s*\\()",
            next  : "start"
         }
      ];

      rules["#operator"] = [
         {
            token : "keyword.operator",
            regex : "\\$|@",
            merge : false,
            next  : "afterDollar"
         },
         {
            token : "keyword.operator",
            regex : ":::|::|:=|%%|>=|<=|==|!=|\\->|<\\-|<<\\-|\\|\\||&&|=|\\+|\\-|\\*\\*?|/|\\^|>|<|!|&|\\||~|\\$|:|@|\\?",
            merge : false,
            next  : "start"
         },
         {
            token : "keyword.operator.infix", // infix operators
            regex : "%.*?%",
            merge : false,
            next  : "start"
         },
         {
            // Obviously these are neither keywords nor operators, but
            // labelling them as such was the easiest way to get them
            // to be colored distinctly from regular text
            token : "paren.keyword.operator",
            merge : false,
            regex : "[[({]",
            next  : "start"
         },
         {
            // Obviously these are neither keywords nor operators, but
            // labelling them as such was the easiest way to get them
            // to be colored distinctly from regular text
            token : "paren.keyword.operator",
            merge : false,
            regex : "[\\])}]",
            next  : "start"
         },
         {
            token : function(value) {
               return $colorFunctionCalls ?
                  "punctuation.keyword.operator" :
                  "punctuation";
            },
            regex : "[;]",
            merge : false,
            next  : "start"
         },
         {
            token : function(value) {
               return $colorFunctionCalls ?
                  "punctuation.keyword.operator" :
                  "punctuation";
            },
            regex : "[,]",
            merge : false,
            next  : "start"
         }
      ];

      rules["#text"] = [
         {
            token : "text",
            regex : "\\s+"
         }
      ];

      // Construct rules from previously defined blocks.
      rules["start"] = include(
         "#comment", "#string", "#number",
         "#package-access", "#quoted-identifier",
         "#function-call-or-keyword", "#keyword-or-identifier",
         "#operator", "#text"
      );

      rules["afterDollar"] = include(
         "#comment", "#string", "#number",
         "#quoted-identifier",
         "#function-call", "#keyword-or-identifier",
         "#operator", "#text"
      );

      rules["qqstring"] = [
         {
            token : "string",
            regex : '(?:(?:\\\\.)|(?:[^"\\\\]))*?"',
            next  : "start"
         },
         {
            token : "string",
            regex : '.+',
            merge : true
         }
      ];

      rules["qstring"] = [
         {
            token : "string",
            regex : "(?:(?:\\\\.)|(?:[^'\\\\]))*?'",
            next  : "start"
         },
         {
            token : "string",
            regex : '.+',
            merge : true
         }
      ];

      this.$rules = rules;

      // Embed 'Rd' comment rules, for Roxygen.
      var rdRules = new TexHighlightRules("comment").getRules();

      // Make all embedded TeX virtual-comment so they don't interfere with
      // auto-indent.
      for (var i = 0; i < rdRules["start"].length; i++) {
         rdRules["start"][i].token += ".virtual-comment";
      }

      this.addRules(rdRules, "rd-");

      // embed our own roxygen highlight rules
      var rdStartRulesBefore = [
         {
            token : "comment",
            regex : "@@"
         },
         {
            token : ["keyword", "comment"],
            regex : "(@(?:export|inheritParams|name|param|rdname|slot|template|useDynLib))(\\s+)(?=[a-zA-Z0-9._-])",
            next  : "rd-highlight"
         },
         {
            token : "keyword",
            regex : "@(?!@)[^ ]*"
         },
         {
            token : "comment",
            regex : "^",
            next  : "start"
         }
      ];

      var rdStartRulesAfter = [
         {
            token : "comment",
            regex : "[^%\\\\[({\\])}]+"
         }
      ];

      this.$rules["rd-start"] =
         rdStartRulesBefore.concat(this.$rules["rd-start"]).concat(rdStartRulesAfter);

      // highlight terms after certain roxygen tags
      this.$rules["rd-highlight"] = [
         {
            token : "identifier.support.function",
            regex : "[a-zA-Z0-9._-]+"
         },
         {
            token : "comment",
            regex : ","
         },
         {
            token : "comment",
            regex : "\\s*",
            next  : "rd-start"
         }
      ];

      this.normalizeRules();

   };

   oop.inherits(RHighlightRules, TextHighlightRules);

   exports.RHighlightRules = RHighlightRules;
   exports.setHighlightRFunctionCalls = function(value) {
      $colorFunctionCalls = value;
   };

});
