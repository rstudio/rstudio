/* ***** BEGIN LICENSE BLOCK *****
 * Distributed under the BSD license:
 *
 * Copyright (c) 2010, Ajax.org B.V.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Ajax.org B.V. nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL AJAX.ORG B.V. BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ***** END LICENSE BLOCK ***** */

define("mode/yaml_highlight_rules", ["require", "exports", "module"], function (require, exports, module) {

   var oop = require("ace/lib/oop");
   var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;

   var makeNumberRule = function(suffix) {
      return {
         token: ["constant.numeric", "text"],
         regex: `([-+]?(?:(?:\\d+(?:\\.\\d*)?)|(?:\\.\\d+))(?:[eE][+-]?\\d*)?)(\\s*)${suffix}`
      };
   };

   // NOTE: These highlight rules are embedded in e.g. R, for highlighting of
   // Quarto YAML. Because the "start" of a YAML string might include the '#|'
   // comment prefix, we avoid using the '^' anchor in regular expressions below,
   // and instead just use '\b' to require a word boundary.
   var YamlHighlightRules = function () {

      var rules = {};

      var makeKeywordRule = function(suffix) {
        return {
            token: ["constant.language.boolean", "text"],
            regex: `\\b(true|false|TRUE|FALSE|True|False|yes|no|~)(\\s*)${suffix}`
        }
      }

      rules["#string"] = [
         {
            token: "string",
            regex: "'",
            push: "qstring"
         },
         {
            token: "string",
            regex: "\"",
            push: "qqstring"
         }
      ];

      rules["start"] = [
         {
            token: "comment",
            regex: "#.*"
         },
         {
            token: "whitespace",
            regex: "\\s+"
         },
         {
            token: "list.markup",
            regex: /\b(?:-{3}|\.{3})\s*(?=#|$)/
         },
         {
            token: "list.markup.keyword.operator",
            regex: /[-?](?=$|\s)/
         },
         {
            token: "constant",
            regex: "!![\\w//]+"
         },
         {
            token: "constant.language",
            regex: "[&\\*][a-zA-Z0-9-_]+"
         },
         {
            // (package) (::) (function) (:)
            token: ["meta.tag", "keyword", "meta.tag", "keyword.operator"],
            regex: /\b(\s*[\w\-.]*?)(:{2,3})(\s*[\w\-.]*?)(:(?:\s+|$))/
         },
         {
            // (dictionary-key) (:)
            token: ["meta.tag", "keyword.operator"],
            regex: /\b(\s*[\w\-.]*?)(:(?:\s+|$))/
         },
         {
            token: "keyword.operator",
            regex: "<<\\w*:\\w*"
         },
         {
            token: "keyword.operator",
            regex: "-\\s*(?=[{])"
         },
         {
            include: "#string"
         },
         {
            token: "string", // multi line string start
            regex: /[|>][-+\d\s]*$/,
            onMatch: function (val, state, stack, line, context) {

               // compute indent (allow for comment prefix for comment-embedded YAML)
               var match = /^(?:#[|])?(\s*)/.exec(line);
               var indent = match[1];

               // save prior state + indent length
               context.yaml = context.yaml || {};
               context.yaml.state = state;
               context.yaml.indent = indent.length;

               // return token
               this.next = state.replace(/start$/, "multiline-string");
               return this.token;
            }
         },
         makeNumberRule ("(?=(?:$|#))"),
         makeKeywordRule("(?=(?:$|#))"),
         {
            token: "paren.lparen.keyword.operator",
            regex: "\\[",
            push: "list"
         },
         {
            token: "paren.lparen.keyword.operator",
            regex: "\\{",
            push: "dictionary"
         },
         {
            token: "paren.lparen",
            regex: "[[({]"
         },
         {
            token: "paren.rparen",
            regex: "[\\])}]"
         },
         {
            token: ["text", "whitespace", "comment"],
            regex: "(.+?)(?:$|(\\s+)(#.*))",
         }
      ];

      rules["list"] = [
         {
            token: "paren.rparen.keyword.operator",
            regex: "\\]",
            next: "pop"
         },
         {
            token: "whitespace",
            regex: "\\s+"
         },
         {
            token: "punctuation.keyword.operator",
            regex: ","
         },
         {
            token: "paren.lparen.keyword.operator",
            regex: "\\[",
            push: "list"
         },
         {
            token: "paren.lparen.keyword.operator",
            regex: "\\{",
            push: "dictionary"
         },
         {
            include: "#string"
         },
         makeNumberRule ("(?=(?:$|[,\\]]))"),
         makeKeywordRule("(?=(?:$|[,\\]]))"),
         {
            token: "text",
            regex: "[^,\\]]+",
         }
      ];

      rules["dictionary"] = [
         {
            token: "paren.rparen.keyword.operator",
            regex: "\\}",
            next: "pop"
         },
         {
            token: "whitespace",
            regex: "\\s+"
         },
         {
            token: "punctuation.keyword.operator",
            regex: "[:,]"
         },
         {
            token: "paren.lparen.keyword.operator",
            regex: "\\[",
            push: "list"
         },
         {
            token: "paren.lparen.keyword.operator",
            regex: "\\{",
            push: "dictionary"
         },
         {
            include: "#string"
         },
         makeNumberRule ("(?=(?:$|[:,}]))"),
         makeKeywordRule("(?=(?:$|[:,}]))"),
         {
            token: "text",
            regex: "[^}:,]+",
         }
      ];

      rules["qstring"] = [
         {
            token: "constant.language.escape",
            regex: "''"
         },
         {
            token: "string",
            regex: "'",
            next: "pop"
         },
         {
            token: "string",
            regex: "[^']+"
         }
      ];

      rules["qqstring"] = [
         {
            token: "constant.language.escape",
            regex: "\\\\."
         },
         {
            token: "string",
            regex: "\"",
            next: "pop"
         },
         {
            token: "string",
            regex: "[^\\\\\"]+"
         }
      ];

      rules["multiline-string"] = [
         {
            token: "string",
            regex: /\s*/,
            onMatch: function (value, state, stack, line, context) {

               // skip blank lines (include Quarto comment prefixes)
               if (/^\s*(?:#[|])?\s*$/.test(line)) {
                  this.next = state;
                  return this.token;
               }

               // if the indent has decreased relative to what
               // was used to start the multiline string, then
               // exit multiline string state
               if (context.yaml.indent >= value.length) {
                  this.next = context.yaml.state;
               } else {
                  this.next = state + "-rest";
               }

               return this.token;
            }
         }
      ];

      rules["multiline-string-rest"] = [
         {
            token: "string",
            regex: ".+",
            next: "multiline-string"
         }
      ];

      this.$rules = rules;
      this.normalizeRules();

   };

   oop.inherits(YamlHighlightRules, TextHighlightRules);

   exports.YamlHighlightRules = YamlHighlightRules;
});
