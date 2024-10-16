/*
 * rmarkdown_folding.js
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * The Initial Developer of the Original Code is
 * Ajax.org B.V.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
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

define("rstudio/folding/rmarkdown", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var BaseFoldMode = require("ace/mode/folding/fold_mode").FoldMode;
var Range = require("ace/range").Range;
var Utils = require("mode/utils");

var FoldMode = exports.FoldMode = function() {};
oop.inherits(FoldMode, BaseFoldMode);

var RE_FOLD_BEGIN = /(?:^|[.])(?:codebegin|heading)(?:$|[.])/;
var RE_FOLD_END   = /(?:^|[.])(?:codeend)(?:$|[.])/;

var FOLD_STYLE_MARKBEGINEND = "markbeginend";

var FOLD_WIDGET_NONE  = "";
var FOLD_WIDGET_START = "start";
var FOLD_WIDGET_END   = "end";

(function() {

   var $findNextHeader = function(session, state, row, depth) {
      var n = session.getLength();
      for (var i = row + 1; i < n; i++) {
         // Check the state and guard against R comments
         var rowState = session.getState(i);
         if (rowState !== state)
            continue;

         var line = session.getLine(i);
         if (depth === 1 && /^[=]{3,}\s*/.test(line))
            return i - 2;
         
         if (depth === 2 && /^[-]{3,}\s*/.test(line))
            return i - 2;

         var match = /^(#+)(?:.*)$/.exec(line);
         if (match && match[1].length <= depth)
            return i - 1;
      }
      return n;
   };

   // NOTE: 'increment' is either 1 or -1, defining whether we are
   // looking forward or backwards. It's encoded this way both for
   // efficiency and to avoid duplicating this function for each
   // direction.
   this.$getBracedWidgetRange = function(session, foldStyle, row, pattern) {

      // Get the fold widget for this row.
      var widget = this.getFoldWidget(session, foldStyle, row);
      if (widget === FOLD_WIDGET_NONE) {
         return null;
      }

      // Figure out if we're looking forward for an end widget, or backwards
      // for a beginning widget.
      var increment, limit;
      if (widget === FOLD_WIDGET_START) {
         increment = 1;
         limit = session.getLength();
         if (row >= limit) {
            return null;
         }
      } else if (widget === FOLD_WIDGET_END) {
         increment = -1;
         limit = 0;
         if (row <= limit) {
            return null;
         }
      }
      
      // Find the end of the current fold range. Iterate through lines and apply
      // our fold pattern until we get a match.
      var startRow = row;
      var endRow = row + increment;
      while (endRow !== limit) {

         var line = session.getLine(endRow);
         if (pattern.test(line))
            break;

         endRow += increment;
      }
      
      // Build the fold range. Note that if we were folding backwards, then the
      // discovered 'endRow' would lie earlier in the document, on the row where
      // the fold region starts -- hence, the sort of 'mirroring' in the code below.
      if (widget === FOLD_WIDGET_START) {
         var startPos = { row: startRow, column: session.getLine(startRow).length };
         var endPos = { row: endRow, column: 0 };
         return Range.fromPoints(startPos, endPos);
      } else {
         var startPos = { row: endRow, column: session.getLine(endRow).length };
         var endPos = { row: startRow, column: 0 };
         return Range.fromPoints(startPos, endPos);
      }
      
   };


   this.getFoldWidget = function(session, foldStyle, row) {

      var tokens = session.getTokens(row);
      for (var token of tokens) {
         var type = token.type || "";
         if (RE_FOLD_BEGIN.test(type)) {
            return FOLD_WIDGET_START;
         } else if (RE_FOLD_END.test(type)) {
            return foldStyle === FOLD_STYLE_MARKBEGINEND ? FOLD_WIDGET_END : FOLD_WIDGET_NONE;
         }
      }

      return FOLD_WIDGET_NONE;
   };

   this.$getFoldWidgetRange = function(session, foldStyle, row) {

      var state = session.getState(row);
      var line = session.getLine(row);
      var trimmed = line.trim();

      // Handle chunk folds.
      var match = /^\s*(`{3,})/.exec(line);
      if (match !== null) {
         var pattern = new RegExp("^\\s*(`{" + match[1].length + "})(?!`)");
         return this.$getBracedWidgetRange(session, foldStyle, row, pattern);
      }

      // Handle YAML header.
      var prevState = row > 0 ? session.getState(row - 1) : "start";
      var isYamlStart = row === 0 && trimmed === "---";
      if (isYamlStart) {
         var pattern = /^\s*---\s*$/;
         return this.$getBracedWidgetRange(session, foldStyle, row, pattern);
      }

      var isYamlEnd = Utils.startsWith(prevState, "yaml");
      if (isYamlEnd) {
         var pattern = /^\s*(?:---|\.\.\.)\s*$/;
         return this.$getBracedWidgetRange(session, foldStyle, row, pattern);
      }
      
      // Handle Markdown header folds. They fold up until the next
      // header of the same depth.
      var depth;
      if (line[0] === '=')
         depth = 1;
      else if (line[0] === '-')
         depth = 2;
      else
      {
         var match = /^(#+)(?:.*)$/.exec(line);
         if (!match)
            return;
         
         depth = match[1].length;
      }

      if (depth === null)
         return;

      var endRow = $findNextHeader(session, state, row, depth);
      return new Range(row, line.length, endRow, session.getLine(endRow).length);

   };

   this.getFoldWidgetRange = function(session, foldStyle, row) {

      var range = this.$getFoldWidgetRange(session, foldStyle, row);

      // Protect against null ranges
      if (range == null)
         return;

      // Ace will throw an error if the range does not span at least
      // two characters.  Returning 'undefined' will instead cause the
      // widget to be colored red, to indicate that it was unable to
      // fold the region following.  This is (probably?) preferred,
      // although the red background treatment feels a bit too
      // negative.
      if (range.start.row === range.end.row)
         return;

      return range;
   };

}).call(FoldMode.prototype);

});
