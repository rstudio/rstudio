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
   function include(rules) {
      var result = new Array(rules.length);
      for (var i = 0; i < rules.length; i++) {
         result[i] = {include: rules[i]};
      }
      return result;
   }

   var oop = require("ace/lib/oop");
   var lang = require("ace/lib/lang");
   var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;
   var Utils = require("mode/utils");

   var reLhsBracket = "[[({]";
   var reRhsBracket = "[\\])}]";

   var RoxygenHighlightRules = function()
   {
      var rules = {};


      rules["start"] = [
         {
            // escaped '@' sign
            token : "comment",
            regex : "@@"
         },
         {
            // latex-style keyword
            token : "keyword",
            regex : "\\\\[a-zA-Z0-9]+"
         },
         {
            // roxygen tag accepting a parameter
            token : ["keyword", "comment"],
            regex : "(@(?:export|inheritParams|name|param|rdname|slot|template|useDynLib))(\\s+)(?=[a-zA-Z0-9._-])",
            next  : "rd-highlight"
         },
         {
            // generic roxygen tag
            token : "keyword",
            regex : "@(?!@)[^ ]*"
         },
         {
            // markdown link with =
            token : ["paren.keyword.operator", "comment"],
            regex : "(\\[)(=)",
            next  : "markdown-link"
         },
         {
            // markdown link
            token : "paren.keyword.operator",
            regex : "\\[",
            next  : "markdown-link"
         },
         {
            // markdown: `code`
            token : ["support.function", "support.function", "support.function"],
            regex : "(`+)(.*?[^`])(\\1)"
         },
         {
            // markdown: __strong__
            token: ["comment", "constant.language.boolean"],
            regex: "(\\s+|^)(__.+?__)\\b"
         },
         {
            // markdown: _emphasis_
            token: ["comment", "constant.language.boolean"],
            regex: "(\\s+|^)(_(?=[^_])(?:(?:\\\\.)|(?:[^_\\\\]))*?_)\\b"
         },
         {
            // markdown: **strong**
            token: ["constant.numeric"],
            regex: "([*][*].+?[*][*])"
         },
         {
            // markdown: *emphasis*
            token: ["constant.numeric"],
            regex: "([*](?=[^*])(?:(?:\\\\.)|(?:[^*\\\\]))*?[*])"
         },
         {
            // highlight brackets
            token : "paren.keyword.operator",
            regex : "(?:" + reLhsBracket + "|" + reRhsBracket + ")"
         },
         {
            defaultToken: "comment"
         }
      ];

      rules["highlight"] = [
         {
            // highlight non-comma tokens
            token : "identifier.support.function",
            regex : "[^ ,]+"
         },
         {
            // don't highlight commas (e.g. @param a,b,c)
            token : "comment",
            regex : ","
         },
         {
            // escape this state and eat whitespace
            token : "comment",
            regex : "\\s*",
            next  : "start"
         }
      ];

      rules["markdown-link"] = [
         {
            // escape when we find a ']'
            token : "paren.keyword.operator",
            regex : "\\]",
            next  : "start"
         },
         {
            // package qualifier: 'pkg::'
            token : ["identifier.support.class", "comment"],
            regex : "([a-zA-Z0-9_.]+)(:{1,3})"
         },
         {
            // quoted function or object
            token : "support.function",
            regex : "`.*?`"
         },
         {
            // non-parens
            token : "support.function",
            regex : "[^{}()[\\]]+"
         },
         {
            // brackets
            token : "paren.keyword.operator",
            regex : "(?:" + reLhsBracket + "|" + reRhsBracket + ")"
         },
         {
            defaultToken: "comment"
         }
      ];


      this.$rules = rules;
      this.normalizeRules();
   };

   oop.inherits(RoxygenHighlightRules, TextHighlightRules);

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
            // Begin Roxygen with todo
            token : ["comment", "comment.keyword.operator"],
            regex : "(#+'\\s*)(TODO|FIXME)\\b",
            next  : "rd-start"
         },
         {
            // Roxygen
            token : "comment",
            regex : "#+'",
            next  : "rd-start"
         },
         {
            // todo in plain comment
            token : ["comment", "comment.keyword.operator", "comment"],
            regex : "(#+\\s*)(TODO|FIXME)\\b(.*)$",
            next  : "start"
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
            regex : reLhsBracket,
            next  : "start"
         },
         {
            // Obviously these are neither keywords nor operators, but
            // labelling them as such was the easiest way to get them
            // to be colored distinctly from regular text
            token : "paren.keyword.operator",
            merge : false,
            regex : reRhsBracket,
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
      rules["start"] = include([
         "#comment", "#string", "#number",
         "#package-access", "#quoted-identifier",
         "#function-call-or-keyword", "#keyword-or-identifier",
         "#operator", "#text"
      ]);

      rules["afterDollar"] = include([
         "#comment", "#string", "#number",
         "#quoted-identifier",
         "#function-call", "#keyword-or-identifier",
         "#operator", "#text"
      ]);

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

      // Embed Roxygen highlight Roxygen highlight rules
      var rdRules = new RoxygenHighlightRules().getRules();

      // Add 'virtual-comment' to embedded rules
      for (var state in rdRules) {
         var rules = rdRules[state];
         for (var i = 0; i < rules.length; i++) {
            if (Utils.isArray(rules[i].token)) {
               for (var j = 0; j < rules[i].token.length; j++)
                  rules[i].token[j] += ".virtual-comment";
            } else {
               rules[i].token += ".virtual-comment";
            }
         }
      }

      this.embedRules(rdRules, "rd-", [{
         token : "text",
         regex : "^",
         next  : "start"
      }]);

      this.normalizeRules();

   };

   oop.inherits(RHighlightRules, TextHighlightRules);

   exports.RHighlightRules = RHighlightRules;
   exports.setHighlightRFunctionCalls = function(value) {
      $colorFunctionCalls = value;
   };

});
