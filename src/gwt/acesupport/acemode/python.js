/*
 * python.js
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * The Initial Developer of the Original Code is
 * Ajax.org B.V.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

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

define("mode/python", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var TextMode = require("ace/mode/text").Mode;
var PythonHighlightRules = require("mode/python_highlight_rules").PythonHighlightRules;
var PythonFoldMode = require("ace/mode/folding/pythonic").FoldMode;
var Range = require("ace/range").Range;

var Mode = function() {
    this.HighlightRules = PythonHighlightRules;
    this.foldingRules = new PythonFoldMode("\\:");
};
oop.inherits(Mode, TextMode);

(function() {

   this.lineCommentStart = "#";

   this.getLanguageMode = function(position)
   {
      return "Python";
   };

   this.getNextLineIndent = function(state, line, tab, row) {

      var indent = this.$getIndent(line);

      // if this line is a comment, use the same indent
      if (/^\s*[#]/.test(line))
         return indent;

      // detect lines ending with something that should increase indent
      // (nominally, these are open brackets and ':')
      if (/[{([:]\s*(?:$|[#])/.test(line))
         indent += tab;

      // decrease indent following things that 'end' a scope
      if (/^\s*(?:break|continue|pass|raise|return)\b/.test(line))
         indent = indent.substring(0, indent.length - tab.length);

      return indent;

   };

   // outdent the row at 'currentRow', setting its indentation to match
   // the indentation associated with 'requestRow'
   this.$performOutdent = function(session, currentRow, requestRow)
   {
      var currentLine = session.doc.$lines[currentRow];
      var requestLine = session.doc.$lines[requestRow];

      var currentIndent = this.$getIndent(currentLine);
      var requestIndent = this.$getIndent(requestLine);

      if (requestIndent.length < currentIndent.length)
      {
         var range = new Range(currentRow, 0, currentRow, currentIndent.length);
         session.replace(range, requestIndent);
      }

      return true;
   };

   this.$autoOutdentElse = function(state, session, row) 
   {
      // if we're inserting a colon following an 'else', then outdent
      var line = session.doc.$lines[row].substring(0, session.selection.cursor.column);
      var shouldOutdent = /^\s*(?:else|elif)(?:\s|[:])/.test(line);
      if (!shouldOutdent)
         return false;

      // 'else' can bind to 'if', 'elif', 'for', and 'try' blocks, so check
      // for each of these
      for (var i = row - 1; i >= 0; i--)
      {
         var prevLine = session.doc.$lines[i];
         var foundMatch = /^\s*(?:if|elif|for|try)(?:\s|[:])/.test(prevLine);
         if (foundMatch)
         {
            return this.$performOutdent(session, row, i);
         }
      }
   };

   this.$autoOutdentExceptFinally = function(state, session, row)
   {
      // check that this line matches an 'except' or 'finally'
      var line = session.doc.$lines[row].substring(0, session.selection.cursor.column);
      var shouldOutdent = /^\s*(?:except|finally)(?:\s|[:])/.test(line);
      if (!shouldOutdent)
         return false;

      // 'except' and 'finally' will bind to a paired 'try', so look for that
      for (var i = row - 1; i >= 0; i--)
      {
         var prevLine = session.doc.$lines[i];
         var foundMatch = /^\s*(?:try)(?:\s|[:])/.test(prevLine);
         if (foundMatch)
         {
            return this.$performOutdent(session, row, i);
         }
      }

   };

   this.checkOutdent = function(state, line, input)
   {
      this.$lastInput = input;
      return input === ":" || input === " ";
   };

   this.$canAutoOutdent = function(state, session, row)
   {
      // can't auto-outdent at start of line
      var cursor = session.selection.cursor;
      if (cursor.column === 0)
         return false;

      // if the user inserted a ':', then we can auto-outdent
      if (this.$lastInput === ":")
         return true;

      // if the user inserted a space, then attempt to auto-outdent only
      // if the space was inserted after a keyword
      if (this.$lastInput === " ")
      {
         var token = session.getTokenAt(cursor.row, cursor.column - 1) || {};
         var isKeyword = /\bkeyword\b/.test(token.type);
         return isKeyword;
      }

      // false if no cases above matched
      return false;
   };

   this.autoOutdent = function(state, session, row)
   {
      if (!this.$canAutoOutdent(state, session, row))
         return;

      if (this.$autoOutdentElse(state, session, row) ||
          this.$autoOutdentExceptFinally(state, session, row))
      {
         return;
      }

   };

   this.transformAction = function(state, action, editor, session, param) {
      return false;
   };

   this.$id = "mode/python";

}).call(Mode.prototype);

exports.Mode = Mode;

});
