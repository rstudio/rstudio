/*
 * julia_highlight_rules.js
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

// Highlighting for Julia (1.x), in place of Ace's, which predates Julia 1.0.
define("mode/julia_highlight_rules", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;
var RainbowParenHighlightRules = require("mode/rainbow_paren_highlight_rules").RainbowParenHighlightRules;
var Utils = require("mode/utils");

var JuliaHighlightRules = function() {

   // the reserved words, and the contextual ones rarely used as names
   // ('abstract type' and the like are matched as a whole, below)
   var keywords = [
      "baremodule", "begin", "break", "catch", "const", "continue", "do",
      "else", "elseif", "end", "export", "finally", "for", "function",
      "global", "if", "import", "let", "local", "macro", "module", "public",
      "quote", "return", "struct", "try", "using", "while",
      "as", "in", "isa", "outer", "where"
   ].join("|");

   var constants = [
      "true", "false", "nothing", "missing", "undef", "C_NULL",
      "Inf", "Inf16", "Inf32", "Inf64", "NaN", "NaN16", "NaN32", "NaN64"
   ].join("|");

   var keywordMapper = this.createKeywordMapper({
      "constant.language": constants,
      "keyword": keywords
   }, "identifier");

   // Names may use most of Unicode; Julia's operators come from the blocks
   // left out here (arrows, mathematical operators and other symbols), and
   // from the multiplication and division signs.
   var letter = "A-Za-z_\\u00A1-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u218F\\u2C00-\\uFFFF";
   var identifier = "[" + letter + "](?:[" + letter + "0-9]|!(?!=))*";
   var qualified = identifier + "(?:\\." + identifier + ")*";

   var number =
      "(?:0x[0-9a-fA-F_]+(?:\\.[0-9a-fA-F_]*)?(?:p[+-]?[0-9]+)?" +
      "|0b[01_]+" +
      "|0o[0-7_]+" +
      "|(?:[0-9][0-9_]*(?:\\.(?!\\.)[0-9_]*)?|\\.[0-9][0-9_]*)(?:[eEf][+-]?[0-9]+)?" +
      ")(?:im\\b)?";

   var escape = "\\\\(?:[\\\\\"'$abefnrtv0]|x[0-9a-fA-F]{1,2}|u\\{?[0-9a-fA-F]{1,6}\\}?|U[0-9a-fA-F]{1,8}|[0-7]{1,3}|.)";

   var character = "'(?:[^\\\\']|" + escape + ")'";

   var operators = [
      "::", "<:", ">:", "->", "=>", "\\|>", "<\\|", "\\.\\.\\.", "===", "!==",
      "==", "!=", "<=", ">=", "&&", "\\|\\|",
      "\\.?(?:>>>|>>|<<|//|[-+*/\\\\^%<>&|~!=\\u00D7\\u00F7\\u2190-\\u2BFF])=?",
      "[?:;,.'$]"
   ].join("|");

   var parenRule = RainbowParenHighlightRules.getParenRule();

   // '$name' and '$(...)' in an interpolating string or command
   var interpolation = [{
      token : ["keyword.operator", "identifier"],
      regex : "(\\$)(" + identifier + ")"
   }, {
      token : "keyword.operator",
      regex : "\\$\\(",
      push  : "interpolation"
   }];

   this.$rules = {

      "start" : [{
         token : "comment",
         regex : "#=",
         push  : "blockcomment"
      }, {
         token : "comment",
         regex : "#.*$"
      }, {
         // docstrings, among others
         token : "string",
         regex : '"""',
         push  : "tqstring"
      }, {
         token : "string",
         regex : '"',
         push  : "qstring"
      }, {
         // non-standard string literals (r"...", raw"...", b"...") aren't
         // interpolated, and may take flags after (r"..."i)
         token : ["support.function", "string"],
         regex : "(" + identifier + ')(""")',
         push  : "rawtqstring"
      }, {
         token : ["support.function", "string"],
         regex : "(" + identifier + ')(")',
         push  : "rawqstring"
      }, {
         token : "string",
         regex : "```",
         push  : "tqcommand"
      }, {
         token : "string",
         regex : "`",
         push  : "command"
      }, {
         // a quote after a name is the adjoint operator, as in A'
         token : ["identifier", "keyword.operator"],
         regex : "(" + identifier + ")('+)"
      }, {
         token : "string",
         regex : character
      }, {
         token : "keyword",
         regex : "\\b(?:abstract|primitive)\\s+type\\b|\\bmutable\\s+struct\\b"
      }, {
         token : ["keyword", "text", "entity.name.function"],
         regex : "\\b(function|macro)(\\s+)(" + qualified + ")"
      }, {
         // macro calls: @time, @. and @Base.kwdef
         token : "support.function",
         regex : "@(?:\\.|" + qualified + ")"
      }, {
         // in a range like 1:n, the right side is no symbol
         token : ["constant.numeric", "keyword.operator"],
         regex : "(" + number + ")(:)(?=[" + letter + "0-9(])"
      }, {
         token : ["identifier", "keyword.operator"],
         regex : "(" + identifier + ")(:)(?=[" + letter + "0-9(])"
      }, {
         // nor after a closing bracket, as in (a + b):c; the bracket goes
         // through the paren rule, which tracks the rainbow nesting
         regex : "[)\\]]:(?=[" + letter + "0-9(])",
         onMatch : function(value, state, stack, line, context) {
            var bracket = value.charAt(0);
            return [
               { type : parenRule.onMatch(bracket, state, stack, line, context), value : bracket },
               { type : "keyword.operator", value : ":" }
            ];
         }
      }, {
         token : "constant.numeric",
         regex : number
      }, {
         token : keywordMapper,
         regex : identifier
      }, {
         token : "keyword.operator",
         regex : "::"
      }, {
         // Most themes color constant.language, but many leave constant.other
         // at (or near) the text color. Adding 'other' would let a theme's
         // .ace_constant.ace_other rule tie with, and win over, its
         // .ace_constant.ace_language rule.
         token : "constant.language.symbol",
         regex : ":" + identifier
      }, {
         token : "keyword.operator",
         regex : operators,
         merge : false
      },
      parenRule,
      {
         token : "text",
         regex : "\\s+"
      }],

      // block comments nest
      "blockcomment" : [{
         token : "comment",
         regex : "#=",
         push  : "blockcomment"
      }, {
         token : "comment",
         regex : "=#",
         next  : "pop"
      }, {
         defaultToken : "comment"
      }],

      "qstring" : [{
         token : "constant.language.escape",
         regex : escape
      }].concat(interpolation, [{
         token : "string",
         regex : '"',
         next  : "pop"
      }, {
         defaultToken : "string"
      }]),

      "tqstring" : [{
         token : "constant.language.escape",
         regex : escape
      }].concat(interpolation, [{
         token : "string",
         regex : '"""',
         next  : "pop"
      }, {
         defaultToken : "string"
      }]),

      "rawqstring" : [{
         token : "constant.language.escape",
         regex : '\\\\"'
      }, {
         token : "string",
         regex : '"[' + letter + '0-9]*',
         next  : "pop"
      }, {
         defaultToken : "string"
      }],

      "rawtqstring" : [{
         token : "string",
         regex : '"""[' + letter + '0-9]*',
         next  : "pop"
      }, {
         defaultToken : "string"
      }],

      "command" : [{
         token : "constant.language.escape",
         regex : escape
      }].concat(interpolation, [{
         token : "string",
         regex : "`",
         next  : "pop"
      }, {
         defaultToken : "string"
      }]),

      "tqcommand" : [{
         token : "constant.language.escape",
         regex : escape
      }].concat(interpolation, [{
         token : "string",
         regex : "```",
         next  : "pop"
      }, {
         defaultToken : "string"
      }]),

      // code in '$(...)', up to the parenthesis that closes it
      "interpolation" : [{
         token : "keyword.operator",
         regex : "\\)",
         next  : "pop"
      }, {
         token : "keyword.operator",
         regex : "\\(",
         push  : "interpolation"
      }, {
         include : "start"
      }]
   };

   Utils.embedQuartoHighlightRules(this);
   this.normalizeRules();
};

oop.inherits(JuliaHighlightRules, TextHighlightRules);

exports.JuliaHighlightRules = JuliaHighlightRules;
});
