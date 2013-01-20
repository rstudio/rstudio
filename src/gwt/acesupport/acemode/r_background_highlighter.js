/*
 * r_background_highlighter.js
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
define("mode/r_background_highlighter", function(require, exports, module)
{
   var Range = require("ace/range").Range;

   var RBackgroundHighlighter = function(session) {
      this.$session = session;
      this.$doc = session.getDocument();

      var that = this;
      this.$doc.on('change', function(evt) {
         that.$onDocChange.apply(that, [evt]);
      });

      this.$rowState = new Array(this.$doc.getLength());
      this.$markers = new Array();

      for (var i = 0; i < this.$doc.getLength(); i++)
         this.$updateRow(i);
      this.$syncMarkers(0);
   };

   (function() {

      this.$updateRow = function(row) {
         if (this.$doc.getLine(row).match("^\\s*#+'.*$"))
            this.$rowState[row] = true;
         else {
            this.$rowState[row] = false;
         }
      };

      this.$syncMarkers = function(startRow, endRow) {
         if (typeof(endRow) == 'undefined')
            endRow = this.$doc.getLength() - 1;

         for (var row = startRow; row <= endRow; row++) {
            if (!!this.$rowState[row] != !!this.$markers[row]) {
               if (this.$rowState[row]) {
                  this.$markers[row] = this.$session.addMarker(new Range(row, 0, row, this.$session.getLine(row).length),
                                                               "ace_foreign_line",
                                                               "background",
                                                               false);
               }
               else {
                  this.$session.removeMarker(this.$markers[row]);
                  delete this.$markers[row];
               }
            }
         }
      };

      this.$insertNewRows = function(index, count) {
         var args = new Array(count + 2);
         args[0] = index;
         args[1] = 0;
         Array.prototype.splice.apply(this.$rowState, args);
      };

      this.$removeRows = function(index, count) {
         var markers = this.$rowState.splice(index, count);
      };

      this.$onDocChange = function(evt)
      {
         var delta = evt.data;

         if (delta.action === "insertLines")
         {
            var newLineCount = delta.range.end.row - delta.range.start.row;
            this.$insertNewRows(delta.range.start.row, newLineCount);
            for (var i = 0; i < newLineCount; i++)
               this.$updateRow(delta.range.start.row + i);
            this.$syncMarkers(delta.range.start.row);
         }
         else if (delta.action === "insertText")
         {
            if (this.$doc.isNewLine(delta.text))
            {
               this.$insertNewRows(delta.range.end.row, 1);
               this.$updateRow(delta.range.start.row);
               this.$updateRow(delta.range.start.row + 1);
               this.$syncMarkers(delta.range.start.row);
            }
            else
            {
               this.$updateRow(delta.range.start.row);
               this.$syncMarkers(delta.range.start.row, delta.range.start.row);
            }
         }
         else if (delta.action === "removeLines")
         {
            this.$removeRows(delta.range.start.row,
                             delta.range.end.row - delta.range.start.row);
            this.$updateRow(delta.range.start.row);
            this.$syncMarkers(delta.range.start.row);
         }
         else if (delta.action === "removeText")
         {
            if (this.$doc.isNewLine(delta.text))
            {
               this.$removeRows(delta.range.end.row, 1);
               this.$updateRow(delta.range.start.row);
               this.$syncMarkers(delta.range.start.row);
            }
            else
            {
               this.$updateRow(delta.range.start.row);
               this.$syncMarkers(delta.range.start.row, delta.range.start.row);
            }
         }
      };

   }).call(RBackgroundHighlighter.prototype);

   exports.RBackgroundHighlighter = RBackgroundHighlighter;
});
