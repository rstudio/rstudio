/*
 * expand_selection.js
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

define("util/expand_selection", function(require, exports, module) {

var Editor = require("ace/editor").Editor;
var Range = require("ace/range").Range;
var TokenIterator = require("ace/token_iterator").TokenIterator;

(function() {

   function isSingleLineString(string)
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

   function getCurrentTokenPosition(tokenIterator)
   {
      return {
         row: tokenIterator.getCurrentTokenRow(),
         column: tokenIterator.getCurrentTokenColumn()
      };
   }

   function getCurrentTokenRange(tokenIterator)
   {
      var start = getCurrentTokenPosition(tokenIterator);
      var end = {
         row: start.row,
         column: start.column + this.getCurrentToken().value.length
      };
      return Range.fromPoints(start, end);
   }

   this.$clearSelectionHistory = function()
   {
      this.$selectionRangeHistory = null;
   };

   this.$acceptSelection = function(selection, newRange, oldRange)
   {
      if (this.$selectionRangeHistory == null)
         this.$selectionRangeHistory = [];
      this.$selectionRangeHistory.push(oldRange);
      selection.setSelectionRange(newRange);
      return true;
   };

   this.$expandSelection = function()
   {
      // Extract some useful objects / variables
      var session = this.getSession();
      var selection = this.getSelection();
      var range = selection.getRange();

      // Place a token iterator at the cursor position
      var position = range.start;
      var iterator = new TokenIterator(session, position.row, position.column);
      var token = iterator.getCurrentToken();

      // A null token implies the document is empty.
      if (token == null)
         return false;

      // If we currently have no selection, select the current word.
      if (selection.isEmpty())
      {
         var prevChar = session.getLine(range.start.row)[range.start.column - 1];
         if (prevChar !== ' ' &&
             prevChar !== '\n' &&
             prevChar !== '\t')
         {
            this.navigateWordLeft();
         }
         selection.selectWordRight();
         return this.$acceptSelection(selection, selection.getRange(), range);
      }

      // If the current token is a string and the current selection lies within
      // the string, then expand to select the string.
      var candidate;
      if (token.type === "string" && isSingleLineString(token.value))
      {
         candidate = iterator.getCurrentTokenRange();
         if (candidate.containsRange(range) && !range.isEqual(candidate))
            return this.$acceptSelection(selection, candidate, range);
      }

      // Look for matching bracket pairs.
      while ((token = iterator.stepBackward()))
      {
         if (token == null)
            break;

         var value = token.value;
         if (value === "(" || value === "{" || value === "[")
         {
            var startPos = getCurrentTokenPosition(iterator);
            startPos.column += 1; // place cursor ahead of opening bracket
            var matchPos = session.$findClosingBracket(value, startPos);
            if (matchPos != null)
            {
               candidate = Range.fromPoints(startPos, matchPos);
               if (!range.isEqual(candidate))
                  return this.$acceptSelection(selection, candidate, range);
            }
            
         }
      }

      // If we get here, just select everything.
      selection.selectAll();

      if (this.getSelectionRange().isEqual(range))
         return this.$addRangeToSelectionHistory(range);

      return true;
   };

   this.$shrinkSelection = function()
   {
      var history = this.$selectionRangeHistory;
      if (history && history.length)
         this.getSelection().setSelectionRange(history.pop());
   };

}).call(Editor.prototype);

});
