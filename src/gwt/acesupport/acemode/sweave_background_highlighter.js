/*
 * sweave_background_highlighter.js
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * The Initial Developer of the Original Code is
 * Ajax.org B.V.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
define("mode/sweave_background_highlighter", function(require, exports, module)
{
   var Range = require("ace/range").Range;

   var SweaveBackgroundHighlighter = function(session) {
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

      var TYPE_TEX = 'tex';
      var TYPE_BEGIN = 'begin';
      var TYPE_END = 'end';
      var TYPE_RCODE = 'r';

      this.$updateRow = function(row) {
         // classify this row
         var line = this.$doc.getLine(row);

         var type = TYPE_TEX;
         var nextType = TYPE_TEX;
         if (line.match("^\\s*\\<\\<.*\\>\\>=.*$")) {
            type = TYPE_BEGIN;
            nextType = TYPE_RCODE;
         }
         else if (line.match("^\\s*@(?:\\s.*)?$")) {
            type = TYPE_END;
            nextType = TYPE_TEX;
         }
         else if (row > 0) {
            var prevRowState = this.$rowState[row-1];
            if (prevRowState === TYPE_BEGIN || prevRowState === TYPE_RCODE) {
               type = TYPE_RCODE;
               nextType = TYPE_RCODE;
            }
         }

         this.$rowState[row] = type;
         for (var i = row+1; i < this.$rowState.length; i++) {
            var thisType = this.$rowState[i];
            if (thisType === TYPE_BEGIN || thisType === TYPE_END)
               break;
            if (this.$rowState[i] === nextType)
               break;
            this.$rowState[i] = nextType;
         }
      };

      this.$syncMarkers = function(startRow, rowsChanged) {
         var dontStopBeforeRow =
               (typeof(rowsChanged) == 'undefined' ? this.$doc.getLength()
                                                   : startRow + rowsChanged);

         var endRow = this.$doc.getLength() - 1;
         for (var row = startRow; row <= endRow; row++) {
            var foreign = this.$rowState[row] != TYPE_TEX;
            if (!!foreign != !!this.$markers[row]) {
               if (foreign) {
                  this.$markers[row] = this.$session.addMarker(new Range(row, 0, row + 1, 0),
                                                               "ace_foreign_line",
                                                               "background",
                                                               false);
               }
               else {
                  this.$session.removeMarker(this.$markers[row]);
                  delete this.$markers[row];
               }
            }
            else if (row > dontStopBeforeRow)
               break;
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
               this.$syncMarkers(delta.range.start.row, 1);
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
               this.$syncMarkers(delta.range.start.row, 1);
            }
         }
      };

   }).call(SweaveBackgroundHighlighter.prototype);

   exports.SweaveBackgroundHighlighter = SweaveBackgroundHighlighter;
});
