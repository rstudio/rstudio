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
   this.$getBracedWidgetRange = function(session, foldStyle, row, prefix, prefix2) {

      var widget = this.getFoldWidget(session, foldStyle, row);
      var increment = (widget === "start") ? 1 : -1;
      
      var startPos = {row: row, column: 0};
      if (increment === 1)
         startPos.column = session.getLine(row).length;
      
      var idx = row + increment;
      var limit = increment > 0 ? session.getLength() : 0;
      while (true)
      {
         if (idx === limit)
            break;

         var trimmed = session.getLine(idx).trim();
         if (Utils.startsWith(trimmed, prefix))
            break;

         if (prefix2 && Utils.startsWith(trimmed, prefix2))
            break;

         idx += increment;
      }
      
      var endPos = {row: idx, column: 0};
      if (increment === -1)
         endPos.column = session.getLine(idx).length;

      var range = increment === 1 ?
         Range.fromPoints(startPos, endPos) :
         Range.fromPoints(endPos, startPos);

      return range;
   };


   this.getFoldWidget = function(session, foldStyle, row) {

      var FOLD_NONE  = "";
      var FOLD_START = "start";
      var FOLD_END   = foldStyle === "markbeginend" ? "end" : "";

      var tokens = session.getTokens(row);
      for (var token of tokens) {
         var type = token.type || "";
         if (RE_FOLD_BEGIN.test(type)) {
            return FOLD_START;
         } else if (RE_FOLD_END.test(type)) {
            return FOLD_END;
         }
      }

      return FOLD_NONE;
   };

   this.$getFoldWidgetRange = function(session, foldStyle, row) {

      var state = session.getState(row);
      var line = session.getLine(row);
      var trimmed = line.trim();

      // Handle chunk folds.
      if (Utils.startsWith(line, "```"))
         return this.$getBracedWidgetRange(session, foldStyle, row, "```");

      // Handle YAML header.
      var prevState = row > 0 ? session.getState(row - 1) : "start";
      var isYamlStart = row === 0 && trimmed === "---";
      var isYamlEnd = Utils.startsWith(prevState, "yaml");
      
      if (isYamlStart || isYamlEnd)
         return this.$getBracedWidgetRange(session, foldStyle, row, "---", isYamlStart ? "..." : undefined);

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
