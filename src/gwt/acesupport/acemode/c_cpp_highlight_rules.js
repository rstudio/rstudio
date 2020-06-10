/*
 * c_cpp_highlight_rules.js
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * The Original Code is Ajax.org Code Editor (ACE).
 *
 * The Initial Developer of the Original Code is
 * Ajax.org B.V.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *      Fabian Jakobs <fabian AT ajax DOT org>
 *      Gastón Kleiman <gaston.kleiman AT gmail DOT com>
 *
 * Based on Bespin's C/C++ Syntax Plugin by Marc McIntyre.
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

define("mode/c_cpp_highlight_rules", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var lang = require("ace/lib/lang");
var DocCommentHighlightRules = require("mode/doc_comment_highlight_rules").DocCommentHighlightRules;
var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;
var TexHighlightRules = require("mode/tex_highlight_rules").TexHighlightRules;
var RHighlightRules = require("mode/r_highlight_rules").RHighlightRules;
var RainbowParenHighlightRules = require("mode/rainbow_paren_highlight_rules").RainbowParenHighlightRules;

var c_cppHighlightRules = function() {

   function escapeRegExp(str) {
      return str.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&");
   }

   // See: http://en.cppreference.com/w/cpp/keyword. Note that
   // 'operatorYY' keywords are highlighted separately below.
   var keywords = lang.arrayToMap([
      "alignas", "alignof", "and", "and_eq", "asm", "auto", "bitand",
      "bitor", "bool", "break", "case", "catch", "char", "char16_t",
      "char32_t", "class", "compl", "const", "constexpr",
      "const_cast", "continue", "decltype", "default", "delete",
      "do", "double", "dynamic_cast", "else", "enum", "explicit",
      "export", "extern", "false", "float", "for", "friend", "goto",
      "if", "inline", "int", "in", "long", "mutable", "namespace",
      "new", "noexcept", "not", "not_eq", "nullptr", "or", "or_eq",
      "private", "protected", "public", "register", "reinterpret_cast",
      "return", "short", "signed", "sizeof", "sizeof...",
      "static", "static_assert", "static_cast", "struct", "switch",
      "template", "this", "thread_local", "throw", "true", "try",
      "typedef", "typeid", "typeof", "typename", "union", "unsigned",
      "using", "virtual", "void", "volatile", "wchar_t", "while",
      "xor", "xor_eq"
   ]);

   var preProcTokens = [
      "include", "pragma", "line", "define", "defined", "undef", "ifdef",
      "ifndef", "if", "else", "elif", "endif", "warning", "error"
   ];

   var buildinConstants = lang.arrayToMap(
      ("NULL").split("|")
   );

   var operatorTokens = [

      "new", "delete",

      ">>=", "<<=", "->*", "...",
      
      "<<", ">>", "&&", "||", "==", "!=", "<=", ">=", "::", "*=",
      "+=", "-=", "/=", "++", "--", "&=", "^=", "%=", "->", ".*",
      
      "!", "$", "&", "|", "+", "-", "*", "/", "^", "~", "=", "%"
      
   ];

   var reOperatorTokens = operatorTokens.map(function(x) {
      return escapeRegExp(x);
   }).join("|");

   var reOperator =
      ["->*"]
      .concat(operatorTokens)
      .concat(["<", ">", ",", "()", "[]", "->"])
      .map(function(x) {
         return escapeRegExp(x);
      });

   reOperator = ["new\\s*\\[\\]", "delete\\s*\\[\\]"].concat(reOperator);
   reOperator = "operator\\s*(?:" + reOperator.join("|") + ")|operator\\s+[\\w_]+(?:&&|&|\\*)?";

   // regexp must not have capturing parentheses. Use (?:) instead.
   // regexps are ordered -> the first match is used

   this.$rules = {
      "start" : [
         {
            // Attributes
            token: "comment.doc.tag",
            regex: "\\/\\/\\s*\\[\\[.*\\]\\].*$"
         }, {
            // Roxygen
            token : "comment",
            regex : "\\/\\/'",
            next : "rd-start"
         }, {
            // Standard comment
            token : "comment",
            regex : "\\/\\/.*$"
         },
         DocCommentHighlightRules.getStartRule("doc-start"),
         {
            token : "comment", // multi line comment
            merge : true,
            regex : "\\/\\*",
            next : "comment"
         }, {
            token : "string", // single line
            regex : '(?:R|L|u8|u|U)?["](?:(?:\\\\.)|(?:[^"\\\\]))*?["]'
         }, {
            token : "string", // multi line string start
            merge : true,
            regex : '(?:R|L|u8|u|U)?["].*\\\\$',
            next : "qqstring"
         }, {
            token : "string", // single line
            regex : "['](?:(?:\\\\.)|(?:[^'\\\\]))*?[']"
         }, {
            token : "string", // multi line string start
            merge : true,
            regex : "['].*\\\\$",
            next : "qstring"
         }, {
            token : "constant.numeric", // hex
            regex : "0[xX][0-9a-fA-F]+\\b"
         }, {
            token : "constant.numeric", // binary literal
            regex : "0[bB][01']+\\b"
         }, {
            token : "constant.numeric", // float
            regex : "[+-]?\\d+(?:(?:\\.\\d*)?(?:[eE][+-]?\\d+)?)?(?:(?:[fF])|(?:(?:[uU]?(?:(?:l?l?)|(?:L?L?))?)|(?:(?:(?:l?l?)|(?:L?L?))[uU]?))|(?:_\\w+))?\\b"
         }, {
            token : "keyword.preproc",
            regex : "#\\s*include\\b",
            next : "include"
         }, {
            token : "keyword.preproc", // pre-compiler directives
            regex : "(?:" + preProcTokens.map(function(x) { return "#\\s*" + x + "\\b"; }).join("|") + ")"
         }, {
            token : "variable.language", // compiler-specific constructs
            regex : "\\b__\\S+__\\b"
         }, {
            token: "keyword",
            regex: reOperator
         }, {
            token : function(value) {
               if (value == "this")
                  return "variable.language";
               else if (keywords.hasOwnProperty(value))
                  return "keyword";
               else if (buildinConstants.hasOwnProperty(value))
                  return "constant.language";
               else
                  return "identifier";
            },
            regex : "[a-zA-Z_$][a-zA-Z0-9_$]*\\b"
         }, {
            token : "keyword.operator",
            merge : false,
            regex : reOperatorTokens
         }, {
            token : "keyword.punctuation.operator",
            merge : false,
            regex : "\\?|\\:|\\,|\\;|\\.|\\\\"
         },
         RainbowParenHighlightRules.getParenRule(),
         {
             token : "paren.keyword.operator",
             merge : false,
             regex : "[<>]"
          }, {
             token : "text",
             regex : "\\s+"
          }
      ],
      "comment" : [
         {
            token : "comment", // closing comment
            regex : ".*?\\*\\/",
            next : "start"
         }, {
            token : "comment", // comment spanning whole line
            merge : true,
            regex : ".+"
         }
      ],
      "qqstring" : [
         {
            token : "string",
            regex : '(?:(?:\\\\.)|(?:[^"\\\\]))*?"',
            next : "start"
         }, {
            token : "string",
            merge : true,
            regex : '.+'
         }
      ],
      "qstring" : [
         {
            token : "string",
            regex : "(?:(?:\\\\.)|(?:[^'\\\\]))*?'",
            next : "start"
         }, {
            token : "string",
            merge : true,
            regex : '.+'
         }
      ],
      "include" : [
         {
            token : "string", // <CONSTANT>
            regex : /<.+>/,
            next : "start"
         },
         {
            token : "string",
            regex : /\".+\"/,
            next : "start"
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

   this.embedRules(DocCommentHighlightRules, "doc-",
                   [ DocCommentHighlightRules.getEndRule("start") ]);

   // Embed R syntax highlighting
   this.$rules["start"].unshift({
      token: "support.function.codebegin",
      regex: "^\\s*\\/\\*{3,}\\s*[Rr]\\s*$",
      next: "r-start"
   });

   var rRules = new RHighlightRules().getRules();
   this.addRules(rRules, "r-");
   this.$rules["r-start"].unshift({
      token: "support.function.codeend",
      regex: "\\*\\/",
      next: "start"
   });
};

oop.inherits(c_cppHighlightRules, TextHighlightRules);

exports.c_cppHighlightRules = c_cppHighlightRules;
});
