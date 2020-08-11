/*
 * utils.js
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

define("mode/utils", ["require", "exports", "module"], function(require, exports, module) {

var Range = require("ace/range").Range;
var TokenIterator = require("ace/token_iterator").TokenIterator;
var unicode = require("ace/unicode");

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

      // some highlight modes (notably, YAML) can have rules
      // that manipulate a stack of saved states -- in the
      // context of YAML, this is done to handle e.g. multiline
      // strings (tracking the indent used for those strings).
      // we need to clear that stack whenever we switch from one
      // mode to another.
      var onMatch = function(value, state, stack, line) {
         stack.splice(0);
         return this.token;
      }

      // define the highlight rules that allow us to transition
      // into the embedded mode
      for (var i = 0; i < startStates.length; i++) {
         rules[startStates[i]].unshift({
            token: "support.function.codebegin",
            regex: reStart,
            onMatch: onMatch,
            next: prefix + "-start"
         });
      }

      // call into Ace to embed rules with given 'prefix', and
      // define the rule that's used to return to parent mode
      HighlightRules.embedRules(EmbedRules, prefix + "-", [{
         token: "support.function.codeend",
         regex: reEnd,
         onMatch: onMatch,
         next: "start"
      }]);
      
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


}).call(exports);

});
