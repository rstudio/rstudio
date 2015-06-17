/*
 * rmarkdown_folding.js
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

define("rstudio/folding/rmarkdown", function(require, exports, module) {

var oop = require("ace/lib/oop");
var BaseFoldMode = require("ace/mode/folding/fold_mode").FoldMode;
var Range = require("ace/range").Range;

var FoldMode = exports.FoldMode = function() {};
var Utils = require("mode/utils");

oop.inherits(FoldMode, BaseFoldMode);

(function() {

   // Return:
   // 'start' to begin a new code fold region,
   // 'end' to end a new code fold region,
   // '' to skip.
   this.getFoldWidget = function(session, foldStyle, row) {

      var line = session.getLine(row);
      var state = session.getState(row);

      if (state === "$start")
      {
         if (line[0] === "#")
            return "start";
         else if (/^[=-]{3,}\s*$/.test(line))
            return "start";
      }

      var trimmed = line.trim();

      // YAML
      if (trimmed === "---")
      {
         if (row === 0)
            return "start";
         else if (state === "start")
            return "end";
      }

      // Chunk
      if (Utils.startsWith(trimmed, "```"))
      {
         // I know this looks weird. However, the state associated
         // with blocks that begin 'chunks', or GitHub-style fenced
         // blocks, will be special (to indicate a transition into a
         // separate mode); when ending that chunk and returning back
         // to the 'start' state the session will instead report that
         // state.
         //
         // The '$start' bit is an artefact of how we sneak YAML highlighting
         // into the start of R Markdown documents.
         if (state === "$start")
            return "end";
         else
            return "start";
      }
      
      // No match (bail)
      return "";
   };

   // NOTE: 'increment' is either 1 or -1, defining whether we are
   // looking forward or backwards. It's encoded this way both for
   // efficiency and to avoid duplicating this function for each
   // direction.
   var $getBracedWidgetRange = function(session, state, row, prefix)
   {
      var increment;
      if (state === "start" || state === "$start")
         increment = -1;
      else
         increment = 1;
      
      var startPos = {row: row, column: 0};
      if (increment === 1)
         startPos.column = session.getLine(row).length;
      
      var idx = row + increment;
      var limit = increment > 0 ? session.getLength() : 0;
      while (true)
      {
         idx += increment;
         if (idx === limit)
            break;

         var trimmed = session.getLine(idx).trim();
         if (Utils.startsWith(trimmed, prefix))
            break;
      }
      
      var endPos = {row: idx, column: 0};
      if (increment === -1)
         endPos.column = session.getLine(idx).length;

      var range = increment === 1 ?
         Range.fromPoints(startPos, endPos) :
         Range.fromPoints(endPos, startPos);

      return range;
   };

   var $findNextHeader = function(session, row, depth)
   {
      var n = session.getLength();
      for (var i = row + 1; i < n; i++)
      {
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

   this.$getFoldWidgetRange = function(session, foldStyle, row) {

      var state = session.getState(row);
      var line = session.getLine(row);
      var trimmed = line.trim();

      // Handle chunk folds.
      if (Utils.startsWith(line, "```"))
         return $getBracedWidgetRange(session, state, row, "```");

      // Handle YAML header.
      var prevState = row > 0 ? session.getState(row - 1) : "start";
      var isYamlStart = row === 0 && trimmed === "---";
      var isYamlEnd = Utils.startsWith(prevState, "yaml");
      
      if (isYamlStart || isYamlEnd)
         return $getBracedWidgetRange(session, state, row, "---");

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

      if (depth == null)
         return;

      var endRow = $findNextHeader(session, row, depth);
      return new Range(row, line.length, endRow, session.getLine(endRow).length);

   };

   this.getFoldWidgetRange = function(session, foldStyle, row)
   {
      var range = this.$getFoldWidgetRange(session, foldStyle, row);

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
