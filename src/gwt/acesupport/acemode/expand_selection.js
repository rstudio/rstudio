/*
 * expand_selection.js
 *
 * Copyright (C) 2020 by RStudio, PBC
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

define("util/expand_selection", ["require", "exports", "module"], function(require, exports, module) {

var Editor = require("ace/editor").Editor;
var Range = require("ace/range").Range;
var VimProvider = require("ace/keyboard/vim");
var TokenIterator = require("ace/token_iterator").TokenIterator;
var Utils = require("mode/utils");

(function() {

   var self = this;

   var $debuggingEnabled = false;
   function debuglog(/*...*/)
   {
      if ($debuggingEnabled)
         for (var i = 0; i < arguments.length; i++)
            console.log(arguments[i]);
   }

   function isCommentedRow(editor, row)
   {
      var tokens = editor.session.getTokens(row);
      for (var i = 0; i < tokens.length; i++)
      {
          var token = tokens[i];
          if (/^\s*$/.test(token.value))
             continue;

          return /\bcomment\b/.test(token.type);
      }

      return false;

   }

   function isExpansionOf(candidate, range)
   {
      var isLeftExpansion =
             candidate.start.row < range.start.row ||
             (candidate.start.row === range.start.row && candidate.start.column < range.start.column);

      var isLeftSame =
             candidate.start.row === range.start.row &&
             candidate.start.column === range.start.column;

      var isRightExpansion =
             candidate.end.row > range.end.row ||
             (candidate.end.row === range.end.row && candidate.end.column > range.end.column);

      var isRightSame =
             candidate.end.row === range.end.row &&
             candidate.end.column === range.end.column;

      if (isLeftExpansion)
         return isRightExpansion || isRightSame;
      else if (isRightExpansion)
         return isLeftExpansion || isLeftSame;
      else
         return false;
   }

   var reIdentifier = /['"\w.]/;

   function moveToStartOfStatement(iterator, delimiters)
   {
      var lookahead = iterator.getCurrentToken();
      var row = iterator.getCurrentTokenRow();
      while (iterator.moveToPreviousSignificantToken())
      {
         var token = iterator.getCurrentToken();
         if (row !== iterator.getCurrentTokenRow() &&
             token.type.indexOf("keyword.operator") !== 0)
         {
            if (!iterator.moveToNextSignificantToken())
               return false;

            return true;
         }

         if (reIdentifier.test(lookahead.value))
         {
            if (reIdentifier.test(token.value) ||
                Utils.isOpeningBracket(token.value) ||
                Utils.contains(delimiters, token.value))
            {
               if (!iterator.moveToNextSignificantToken())
                  return false;

               return true;
            }
         }

         iterator.bwdToMatchingToken();
         lookahead = iterator.getCurrentToken();
         row = iterator.getCurrentTokenRow();
      }

      return true;
   }

   function moveToEndOfStatement(iterator, delimiters)
   {
      var lookbehind = iterator.getCurrentToken();
      var row = iterator.getCurrentTokenRow();
      while (iterator.moveToNextSignificantToken())
      {
         var token = iterator.getCurrentToken();
         if (row !== iterator.getCurrentTokenRow() &&
             lookbehind.type.indexOf("keyword.operator") !== 0)
         {
            if (!iterator.moveToPreviousSignificantToken())
               return false;

            return true;
         }

         if (reIdentifier.test(lookbehind.value) ||
             Utils.isClosingBracket(lookbehind.value))
         {
            if (reIdentifier.test(token.value) ||
                Utils.isClosingBracket(token.value) ||
                Utils.contains(delimiters, token.value))
            {
               if (!iterator.moveToPreviousSignificantToken())
                  return false;

               return true;
            }
         }

         iterator.fwdToMatchingToken();
         lookbehind = iterator.getCurrentToken();
         row = iterator.getCurrentTokenRow();
      }

      return true;
   }


   var $handlersAttached = false;
   function ensureOnChangeHandlerAttached()
   {
      if (!$handlersAttached)
      {
         self.on("change", self.$onClearSelectionHistory);
         $handlersAttached = true;
      }
   }

   function ensureOnChangeHandlerDetached()
   {
      if ($handlersAttached)
      {
         self.off("change", self.$onClearSelectionHistory);
         $handlersAttached = false;
      }
   }

   this.$onClearSelectionHistory = function()
   {
      return self.$clearSelectionHistory();
   };

   this.$clearSelectionHistory = function()
   {
      this.$selectionRangeHistory = null;
      this.off("change", this.$clearSelectionHistory);
   };

   this.$acceptSelection = function(selection, newRange, oldRange)
   {
      debuglog("Accepting selection: ", oldRange, newRange);
      if (this.$selectionRangeHistory == null)
         this.$selectionRangeHistory = [];

      selection.setSelectionRange(newRange);

      var normalizedRange = selection.getRange();
      if (!normalizedRange.isEqual(oldRange))
         this.$selectionRangeHistory.push(oldRange);

      if (!(this.isRowFullyVisible(newRange.start.row) &&
            this.isRowFullyVisible(newRange.end.row)))
      {
         this.centerSelection(selection);
      }

      return newRange;
   };

   var $expansionFunctions = [];
   function addExpansionRule(name, immediate, method)
   {
      $expansionFunctions.push({
         name: name,
         immediate: immediate,
         execute: method
      });
   }

   addExpansionRule("string", true, function(editor, session, selection, range) {
      var token = session.getTokenAt(range.start.row, range.start.column + 1);
      if (token && /\bstring\b/.test(token.type)) {
         return new Range(
            range.start.row,
            token.column + 1,
            range.start.row,
            token.column + token.value.length - 1
         );
      }

      return null;
   });

   addExpansionRule("token", true, function(editor, session, selection, range) {

      var token = session.getTokenAt(range.start.row, range.start.column + 1);
      if (token && /[\d\w]/.test(token.value)) {
         return new Range(
            range.start.row, token.column,
            range.start.row, token.column + token.value.length
         );
      }

      return null;

   });

   addExpansionRule("comment", true, function(editor, session, selection, range) {

      // First, check that the whole selection is commented.
      var startRow = range.start.row;
      var endRow = range.end.row;

      for (var row = startRow; row <= endRow; row++)
      {
         if (!isCommentedRow(editor, row))
            return null;
      }

      // Now, expand the selection to include any other comments attached.
      while (isCommentedRow(editor, startRow))
         startRow--;

      while (isCommentedRow(editor, endRow))
         endRow++;

      var endColumn = editor.getSession().getLine(endRow - 1).length;
      return new Range(startRow + 1, 0, endRow - 1, endColumn);

   });

   addExpansionRule("includeBoundaries", true, function(editor, session, selection, range) {

      var lhsItr = new TokenIterator(session);
      var lhsToken = lhsItr.moveToPosition(range.start);
      if (range.start.column === 0)
         lhsToken = lhsItr.moveToPreviousToken();

      var rhsItr = new TokenIterator(session);
      var rhsToken = rhsItr.moveToPosition(range.end, true);

      if (lhsToken && rhsToken)
      {
         // Check for complementing types
         var isMatching =
                lhsToken.type === "support.function.codebegin" &&
                rhsToken.type === "support.function.codeend";

         if (!isMatching)
         {
            // Check for complementing brace types
            isMatching =
               Utils.isOpeningBracket(lhsToken.value) &&
               Utils.getComplement(lhsToken.value) === rhsToken.value;
         }

         if (isMatching)
         {
            debuglog("Expanding to match selection");
            var lhsPos = lhsItr.getCurrentTokenPosition();
            var rhsPos = rhsItr.getCurrentTokenPosition();
            rhsPos.column += rhsToken.value.length;
            return Range.fromPoints(lhsPos, rhsPos);
         }
      }

      return null;

   });

   addExpansionRule("matching", false, function(editor, session, selection, range) {

      // Look for matching bracket pairs. Note that this block does not
      // immediately return if a candidate range is found -- if the expansion
      // spans new rows, we may instead choose to just expand the current
      // selection to fill both the start and end rows.
      var iterator = new TokenIterator(session);
      var token = iterator.moveToPosition(range.start);
      if (token == null)
         return null;

      do
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
                   endPos.column = session.getLine(endPos.row).length;
               }
               return Range.fromPoints(startPos, endPos);
            }

         }
      }
      while ((token = iterator.stepBackward()))

      return null;

   });

   addExpansionRule("statement", false, function(editor, session, selection, range) {

      var bwdIt = new TokenIterator(session);
      if (!bwdIt.moveToPosition(range.start, true))
         return null;

      var fwdIt = new TokenIterator(session);
      if (!fwdIt.moveToPosition(range.end))
         return null;

      if (!moveToStartOfStatement(bwdIt, [";", ",", "=", "<-", "<<-"]))
         return null;

      if (!moveToEndOfStatement(fwdIt, [";", ","]))
         return null;

      var bwdPos = bwdIt.getCurrentTokenPosition();
      var fwdPos = fwdIt.getCurrentTokenPosition();

      if (bwdPos.row === range.start.row &&
          bwdPos.column === range.start.column &&
          fwdPos.row === range.end.row &&
          fwdPos.column <= range.end.column)
      {
         if (!moveToStartOfStatement(bwdIt, [";", ","]))
            return null;
      }

      var start = bwdIt.getCurrentTokenPosition();
      var end   = fwdIt.getCurrentTokenPosition();
      end.column += fwdIt.getCurrentTokenValue().length;

      return Range.fromPoints(start, end);
   });

   addExpansionRule("scope", false, function(editor, session, selection, range) {

      var mode = session.getMode();
      if (mode.codeModel == null || mode.codeModel.getCurrentScope == null)
         return null;

      var candidates = [];

      var scope = mode.codeModel.getCurrentScope(range.start);
      while (scope != null)
      {
         var startPos = scope.preamble;
         var endPos = scope.end;

         if (endPos == null && scope.parentScope)
         {
            var siblings = scope.parentScope.$children;
            for (var i = siblings.length - 2; i >= 0; i--)
            {
               if (siblings[i].equals(scope))
               {
                  endPos = siblings[i + 1].preamble;
                  break;
               }
            }
         }

         if (endPos == null)
            endPos = {row: session.getLength(), column: 0};

         candidates.push(Range.fromPoints(startPos, endPos));
         scope = scope.parentScope;
      }

      if (candidates.length === 0)
         return null;

      return candidates;

   });

   addExpansionRule("everything", false, function(editor, session, selection, range) {

      var n = session.getLength();
      if (n === 0)
         return new Range(0, 0, 0, 0);

      var lastLine = session.getLine(n - 1);
      return new Range(0, 0, n - 1, lastLine.length);

   });

   this.$expandSelection = function()
   {
      debuglog("Begin new expand selection session");
      debuglog("----------------------------------");

      ensureOnChangeHandlerAttached();

      // Extract some useful objects / variables.
      var session = this.getSession();
      var selection = this.getSelection();
      var initialRange = selection.getRange();

      // Loop through the registered expansion functions, and apply them.
      // Store the candidate ranges for later selection.
      var allCandidates = [];
      for (var i = 0; i < $expansionFunctions.length; i++)
      {
         var rule = $expansionFunctions[i];

         // Get the candidate range to use for expansion.
         var candidates = rule.execute(this, session, selection, initialRange);
         if (!Utils.isArray(candidates))
            candidates = [candidates];

         for (var j = 0; j < candidates.length; j++)
         {
            var candidate = candidates[j];

            // Check to see if we should apply it immediately.
            if (candidate && rule.immediate && isExpansionOf(candidate, initialRange))
            {
               debuglog("Accepting immediate expansion: '" + rule.name + "'");
               return this.$acceptSelection(selection, candidate, initialRange);
            }

            // Otherwise, add it to the list of candidates for later filtering.
            if (candidate && isExpansionOf(candidate, initialRange))
            {
               allCandidates.push({
                  name: rule.name,
                  range: candidate
               });
            }
         }

      }

      // Sort candidates by size of range. We want to choose the smallest range
      // that is still an expansion of the initial range.
      allCandidates.sort(function(lhs, rhs) {

         var lhs = lhs.range;
         var rhs = rhs.range;

         var lhsRowSpan = lhs.end.row - lhs.start.row;
         var rhsRowSpan = rhs.end.row - rhs.start.row;

         if (lhsRowSpan !== rhsRowSpan)
            return lhsRowSpan > rhsRowSpan;

         var lhsColSpan = lhs.end.column - lhs.start.column;
         var rhsColSpan = rhs.end.column - rhs.start.column;

         return lhsColSpan > rhsColSpan;

      });

      // Choose the smallest expansion.
      var bestFit = allCandidates[0].range;
      debuglog("Selected candidate '" + allCandidates[0].name + "'", bestFit);
      return this.$acceptSelection(selection, bestFit, initialRange);

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

      // No more history means we don't need to track
      // document changed any more.
      ensureOnChangeHandlerDetached();
      return this.getSelectionRange();

   };

}).call(Editor.prototype);

});
