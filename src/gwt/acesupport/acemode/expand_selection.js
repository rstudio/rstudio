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
var VimProvider = require("ace/keyboard/vim");
var TokenIterator = require("ace/token_iterator").TokenIterator;
var Utils = require("mode/utils");

(function() {

   var that = this;

   function isComment(editor, row)
   {
      var token = editor.getSession().getTokenAt(row, 0);
      if (token === null)
         return false;

      return /\bcomment\b/.test(token.type);
   }

   function commentRange(editor)
   {
      var startRow = editor.getCursorPosition().row;
      var endRow = startRow;

      while (isComment(editor, startRow))
         startRow--;

      while (isComment(editor, endRow))
         endRow++;

      return new Range(startRow + 1, 0, endRow, 0);
   }

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
   }

   var onDocumentChange = function(editor)
   {
      editor.$clearSelectionHistory();
   };

   this.$onClearSelectionHistory = function()
   {
      return that.$clearSelectionHistory();
   };

   this.$clearSelectionHistory = function()
   {
      this.$selectionRangeHistory = null;
      this.off("change", this.$clearSelectionHistory);
   };

   this.$acceptSelection = function(selection, newRange, oldRange)
   {
      if (this.$selectionRangeHistory == null)
         this.$selectionRangeHistory = [];
      this.$selectionRangeHistory.push(oldRange);

      selection.setSelectionRange(newRange);
      if (!(this.isRowFullyVisible(newRange.start.row) &&
            this.isRowFullyVisible(newRange.end.row)))
      {
         this.centerSelection(selection);
      }

      return newRange;
   };

   this.$expandSelection = function()
   {
      // Extract some useful objects / variables
      var session = this.getSession();
      var selection = this.getSelection();
      var range = selection.getRange();

      this.on("change", this.$onClearSelectionHistory);

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

      // Handle 'small' expansions of the current
      // selection.
      var prevToken = iterator.peekBwd(1);
      if (prevToken.type === "support.function.codebegin") {

         var startPos = {
            row: range.start.row - 1,
            column: 0
         };

         var endPos = {
            row: range.end.row + 1,
            column: iterator.$session.getLine(range.end.row + 1).length
         };

         candidate = Range.fromPoints(startPos, endPos);
         return this.$acceptSelection(selection, candidate, range);
      }

      if (Utils.isOpeningBracket(token.value)) {
         clone = iterator.clone();
         var startPos = iterator.getCurrentTokenPosition();
         if (clone.fwdToMatchingToken()) {
            candidate = Range.fromPoints(range.start, range.end);
            candidate.start.column--;
            candidate.end.column++;
            return this.$acceptSelection(selection, candidate, range);
         }
      }

      // If the current selection is in, or contains, a comment block,
      // expand selection to entire comment block.
      if (/\bcomment\b/.test(token.type))
      {
         var candidate = commentRange(this);
         selection.setSelectionRange(range);
         return this.$acceptSelection(selection, candidate, range);
      }

      // Look for matching bracket pairs.
      while ((token = iterator.stepBackward()))
      {
         if (token == null)
            break;

         if (iterator.bwdToMatchingToken())
            continue;

         if (Utils.isOpeningBracket(token.value) ||
             token.type === "support.function.codebegin")
         {
            var startPos = iterator.getCurrentTokenPosition();
            if (token.type === "support.function.codebegin") {
               startPos.row++;
               startPos.column = 0;
            } else {
               startPos.column += token.value.length;
            }

            var clone = iterator.clone();
            if (clone.fwdToMatchingToken()) {
               var endPos = clone.getCurrentTokenPosition();
               if (token.type === "support.function.codebegin") {
                   endPos.row--;
                   endPos.column = iterator.$session.getLine(startPos.row).length;
               }
               candidate = Range.fromPoints(startPos, endPos);
               if (!range.isEqual(candidate))
                  return this.$acceptSelection(selection, candidate, range);
            }

         }
      }

      // If we get here, just select everything.
      selection.selectAll();

      // If the new selection is not equal to the previous
      // selection, add it to the selection history.
      if (!this.getSelectionRange().isEqual(range))
         return this.$acceptSelection(selection, selection.getRange(), range);

      return true;
   };

   this.$shrinkSelection = function()
   {
      var history = this.$selectionRangeHistory;
      if (history && history.length) {
          var range = history.pop();
          this.getSelection().setSelectionRange(range);
          if (!(this.isRowFullyVisible(range.start.row) &&
                this.isRowFullyVisible(range.end.row)))
          {
             this.centerSelection(this.getSelection());
          }
          return range;
      }
      this.off("change", this.$onClearSelectionHistory);
   };

}).call(Editor.prototype);

});
