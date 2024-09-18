/*
 * utils.js
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

define("mode/utils", ["require", "exports", "module"], function(require, exports, module) {

var Range = require("ace/range").Range;
var TokenIterator = require("ace/token_iterator").TokenIterator;
var unicode = require("ace/unicode");
var YamlHighlightRules = require("mode/yaml_highlight_rules").YamlHighlightRules;

(function() {

   var that = this;
   var reWordCharacter = new RegExp("^[" + unicode.wordChars + "._]+", "");

   // Simulate 'new Foo([args])'; ie, construction of an
   // object from an array of arguments
   this.construct = function(constructor, args)
   {
      function F() {
         return constructor.apply(this, args);
      }

      F.prototype = constructor.prototype;
      return new F();
   };

   this.contains = function(array, object)
   {
      for (var i = 0; i < array.length; i++)
         if (array[i] === object)
            return true;

      return false;
   };

   this.isArray = function(object)
   {
      return Object.prototype.toString.call(object) === '[object Array]';
   };

   this.asArray = function(object)
   {
      return that.isArray(object) ? object : [object];
   };

   this.getPrimaryState = function(session, row)
   {
      return that.primaryState(session.getState(row));
   };

   this.primaryState = function(states)
   {
      if (that.isArray(states))
      {
         for (var i = 0; i < states.length; i++)
         {
            var state = states[i];
            if (state === "#tmp")
               continue;
            return state || "start";
         }
      }

      return states || "start";
   };

   this.activeMode = function(state, major)
   {
      var primary = that.primaryState(state);
      var modeIdx = primary.lastIndexOf("-");
      if (modeIdx === -1)
         return major;
      return primary.substring(0, modeIdx).toLowerCase();
   };

   this.endsWith = function(string, suffix)
   {
      return string.indexOf(suffix, string.length - suffix.length) !== -1;
   };

   this.escapeRegExp = function(string)
   {
      return string.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&");
   };

   this.embedRules = function(HighlightRules, EmbedRules,
                              prefix, reStart, reEnd,
                              startStates, endState)
   {
      if (typeof startStates === "undefined")
         startStates = ["start"];

      if (typeof endState === "undefined")
         endState = "start";

      startStates = that.asArray(startStates);
      var rules = HighlightRules.$rules;

      HighlightRules.embedRules(EmbedRules, prefix + "-", [{
         regex: reEnd,
         onMatch: function(value, state, stack, line, context) {

            // Check whether the width of this chunk tail matches
            // the width of the chunk header that started this chunk.
            var match = /^\s*((?:`|-|\.)+)/.exec(value);
            var width = match[1].length;
            if (context.chunk.width !== width) {
               this.next = state;
               return "text";
            }

            // Update the next state and return the matched token.
            this.next = context.chunk.state || "start";
            delete context.chunk;
            return "support.function.codeend";
         }
      }]);

      for (var i = 0; i < startStates.length; i++) {
         rules[startStates[i]].unshift({
            regex: reStart,
            onMatch: function(value, state, stack, line, context) {

               // Check whether we're already within a chunk. If so,
               // skip this chunk header -- assume that it's embedded
               // within another active chunk.
               context.chunk = context.chunk || {};
               if (context.chunk.state != null) {
                  this.next = state;
                  return "text";
               }

               // A chunk header was found; record the state we entered
               // from, and also the width of the chunk header.
               var match = /^\s*((?:`|-|\.)+)/.exec(value);
               context.chunk.width = match[1].length;
               context.chunk.state = state;

               // Update the next state and return the matched token.
               this.next = prefix + "-start";
               return "support.function.codebegin";
            }
         });
      }
   };

   this.isSingleLineString = function(string)
   {
      if (string.length < 2)
         return false;

      var firstChar = string[0];
      if (firstChar !== "'" && firstChar !== "\"")
         return false;

      var lastChar = string[string.length - 1];
      if (lastChar !== firstChar)
         return false;

      var isEscaped = string[string.length - 2] === "\\" &&
                      string[string.length - 3] !== "\\";

      if (isEscaped)
         return false;

      return true;
   };

   this.createTokenIterator = function(editor)
   {
      var position = editor.getSelectionRange().start;
      var session = editor.getSession();
      return new TokenIterator(session, position.row, position.column);
   };

   this.isWordCharacter = function(string)
   {
      return reWordCharacter.test(string);
   };

   // The default set of complements is R-centric.
   var $complements = {

      "'" : "'",
      '"' : '"',
      "`" : "`",

      "{" : "}",
      "(" : ")",
      "[" : "]",
      "<" : ">",

      "}" : "{",
      ")" : "(",
      "]" : "[",
      ">" : "<"
   };

   this.isBracket = function(string, allowArrow)
   {
      if (!!allowArrow && (string === "<" || string === ">"))
         return true;

      return string === "{" || string === "}" ||
             string === "(" || string === ")" ||
             string === "[" || string === "]";

   };

   this.isOpeningBracket = function(string, allowArrow)
   {
      return string === "{" ||
             string === "(" ||
             string === "[" ||
             (!!allowArrow && string === "<");
   };

   this.isClosingBracket = function(string, allowArrow)
   {
      return string === "}" ||
             string === ")" ||
             string === "]" ||
             (!!allowArrow && string === ">");
   };

   this.getComplement = function(string, complements)
   {
      if (typeof complements === "undefined")
         complements = $complements;

      var complement = complements[string];
      if (typeof complement === "undefined")
         return string;

      return complement;
   };

   this.stripEnclosingQuotes = function(string)
   {
      var n = string.length;
      if (n < 2)
         return string;

      var firstChar = string[0];
      var isQuote =
             firstChar === "'" ||
             firstChar === "\"" ||
             firstChar === "`";

      if (!isQuote)
         return string;

      var lastChar = string[n - 1];
      if (lastChar !== firstChar)
         return string;

      return string.substr(1, n - 2);
   };

   this.startsWith = function(string, prefix)
   {
      if (typeof string !== "string") return false;
      if (typeof prefix !== "string") return false;
      if (string.length < prefix.length) return false;

      for (var i = 0; i < prefix.length; i++)
         if (string[i] !== prefix[i])
            return false;

      return true;
   };

   this.getTokenTypeRegex = function(type)
   {
      return new RegExp("(?:^|[.])" + type + "(?:$|[.])", "");
   }

   this.embedQuartoHighlightRules = function(self)
   {
      // Embed YAML highlighting rules
      var prefix = "quarto-yaml-";
      self.embedRules(YamlHighlightRules, prefix);

      // allow Quarto YAML comments within each kind of chunk
      for (var state in self.$rules) {

         // add rules for highlighting YAML comments
         // TODO: Associate embedded rules with their comment tokens
         if (state === "start" || state.indexOf("-start") !== -1) {
            self.$rules[state].unshift({
               token: "comment.doc.tag",
               regex: "^\\s*#[|]",
               push: prefix + "start"
            });
         }

         // allow Quarto YAML highlight rules to consume leading comments
         if (state.indexOf(prefix) === 0) {

            // make sure YAML rules can consume a leading #|
            self.$rules[state].unshift({
               token: ["whitespace", "comment.doc.tag"],
               regex: "^(\\s*)(#[|])",
               next: state
            });

            // make sure YAML rules exit when there's no leading #|
            self.$rules[state].unshift({
               token: "whitespace",
               regex: "^\\s*(?!#)",
               next: "pop"
            });

         }

         self.$rules[prefix + "start"].unshift({
            token: "text",
            regex: "^\\s*(?!#)",
            next: "pop"
         });

         // allow for multi-line strings in YAML comments
         self.$rules[prefix + "multiline-string"].unshift({
            regex: /^(#[|])(\s*)/,
            onMatch: function(value, state, stack, line, context) {

               // apply token splitter regex
               var tokens = this.splitRegex.exec(value);

               // if we matched the whole line, continue in the multi-string state
               if (line === tokens[1] + tokens[2]) {
                  this.next = state;
               } else {
                  // if the indent has decreased relative to what
                  // was used to start the multiline string, then
                  // exit multiline string state
                  var indent = tokens[2].length;
                  if (context.yaml.indent >= indent) {
                     this.next = context.yaml.state;
                  } else {
                     this.next = state + "-rest";
                  }
               }

               // retrieve tokens for the matched value
               return [
                  { type: "comment.doc.tag", value: tokens[1] },
                  { type: "indent", value: tokens[2] }
               ];
            }
         });

      }
   }


}).call(exports);

});
