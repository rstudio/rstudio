/*
 * background_highlighter.js
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
define("mode/background_highlighter", ["require", "exports", "module"], function(require, exports, module)
{
   var Range = require("ace/range").Range;
   var markerClass = "ace_foreign_line background_highlight";
   var markerType = "fullLine";

   var debuglog = function(/*...*/)
   {
      for (var i = 0; i < arguments.length; i++)
         console.log(arguments[i]);
   };

   var BackgroundHighlighter = function(session, highlighters)
   {
      this.$session = session;
      this.$doc = session.getDocument();
      this.$highlighters = highlighters;

      // Listen for document change events from Ace.
      var onDocChange = function(evt)
      {
         this.$onDocChange(evt);
      }.bind(this);
      this.$doc.on('change', onDocChange);

      var onChangeFold = function(evt)
      {
         this.$onChangeFold(evt);
      }.bind(this);
      this.$session.on('changeFold', onChangeFold);

      // When the session's mode is changed, a new background
      // highlighter will get attached. In that case, we need
      // to detach this (now the old) highlighter.
      var $attached = false;
      var onChangeMode = function()
      {
         if ($attached)
         {
            this.$clearMarkers();
            this.$doc.off('change', onDocChange);
            this.$session.off('changeMode', onChangeMode);
            this.$session.off('changeFold', onChangeFold);
         }
         $attached = true;
      }.bind(this);
      this.$session.on('changeMode', onChangeMode);


      // Initialize other state in the highlighter.
      this.$rowState = new Array(this.$doc.getLength());
      this.$markers = [];

      // The Sweave background highlighter is destroyed and recreated with the
      // document mode. Look through the markers and see if any were created by
      // a previous instance of this highlighter; if so, take ownership of them
      // so we don't create duplicates.
      var markers = session.getMarkers(false);
      for (var markerId in markers) {
          var marker = markers[markerId];
          if (marker.range &&
              marker.clazz.indexOf(markerClass) !== -1 &&
              marker.type == markerType) {
              this.$markers[marker.range.start.row] = markerId;
    	  }
      }

      this.$synchronize(0);
   };

   (function() {

      // The states that the background highlighter needs to understand
      // and differentiate; e.g.
      //
      //    This is an R Markdown document. | TEXT
      //                                    | TEXT
      //    ```{r}                          | CHUNK_START
      //    print(mtcars)                   | CHUNK_BODY
      //    ```                             | CHUNK_END
      //                                    | TEXT
      //    This is some more text.         | TEXT
      //
      var STATE_TEXT        = 1;
      var STATE_CHUNK_START = 2;
      var STATE_CHUNK_BODY  = 3;
      var STATE_CHUNK_END   = 4;

      this.$synchronize = function(fromRow)
      {
         this.$update(fromRow);
         this.$syncMarkers(fromRow);
      };

      this.$update = function(fromRow)
      {
         // If this row has no state, then we need to look back until
         // we find a row with cached state.
         while (fromRow > 0 && this.$rowState[fromRow - 1] == null)
            fromRow--;

         this.$activeHighlighter = this.$findActiveHighlighter(fromRow);

         var n = this.$doc.getLength();
         // debuglog("There are " + n + " rows to be updated.");
         for (var row = fromRow; row < n; row++)
         {
            var oldState = this.$rowState[row];
            var newState = this.$getState(row);

            if (oldState === newState)
               return;

            // debuglog("Updating row: " + row + " [" + oldState + " -> " + newState + "]");
            this.$rowState[row] = newState;
         }
      };

      this.$getState = function(row)
      {
         var line = this.$doc.getLine(row);
         var prevRowState = this.$rowState[row - 1] || STATE_TEXT;

         if (prevRowState === STATE_TEXT ||
             prevRowState === STATE_CHUNK_END)
         {
            if (this.$beginChunkHighlight(line))
               return STATE_CHUNK_START;
            else
               return STATE_TEXT;
         }
         else if (prevRowState === STATE_CHUNK_START ||
                  prevRowState === STATE_CHUNK_BODY)
         {
            if (this.$endChunkHighlight(line))
               return STATE_CHUNK_END;
            else
               return STATE_CHUNK_BODY;
         }

         // shouldn't be reached
         return STATE_TEXT;
      };

      this.$findActiveHighlighter = function(fromRow)
      {
         for (var row = fromRow; row >= 0; row--)
         {
            var state = this.$rowState[row];
            if (state === STATE_TEXT)
               return null;

            var line = this.$doc.getLine(row);
            if (state == STATE_CHUNK_START)
            {
               var highlighter = this.$beginChunkHighlight(line);
               if (highlighter)
                  return highlighter;
            }
         }

         return null;
      };

      this.$beginChunkHighlight = function(line)
      {
         for (var i = 0; i < this.$highlighters.length; i++)
         {
            var highlighter = this.$highlighters[i];
            if (highlighter.begin.test(line))
            {
               this.$activeHighlighter = highlighter;
               return highlighter;
            }
         }

         return null;
      };

      this.$endChunkHighlight = function(line)
      {
         if (this.$activeHighlighter == null)
            return null;

         if (!this.$activeHighlighter.end.test(line))
            return null;

         var highlighter = this.$activeHighlighter;
         this.$activeHighlighter = null;
         return highlighter;
      };

      this.$insertNewRows = function(index, count)
      {
         var args = new Array(count + 2);
         args[0] = index;
         args[1] = 0;
         Array.prototype.splice.apply(this.$rowState, args);
      };

      this.$removeRows = function(index, count)
      {
         this.$rowState.splice(index, count);
      };

      // Marker-related methods ----

      this.$clearMarkers = function()
      {
         for (var i = 0; i < this.$markers.length; i++)
            this.$session.removeMarker(this.$markers[i]);
         this.$markers = [];
      };

      this.$syncMarkers = function(startRow)
      {
         var endRow = this.$doc.getLength() - 1;
         for (var row = startRow; row <= endRow; row++) {

            var state = this.$rowState[row];
            var isForeign = state !== STATE_TEXT;

            if (this.$markers[row]) {
               this.$session.removeMarker(this.$markers[row]);
               this.$markers[row] = null;
            }

            // Don't show background highlighting if this chunk lies within a fold.
            var foldLine = this.$session.getFoldLine(row);
            if (foldLine !== null)
            {
               var startRow = foldLine.start.row;
               if (startRow < row)
                  continue;
            }

            if (isForeign) {

               var isChunkStart = state === STATE_CHUNK_START;

               var clazz = isChunkStart ?
                  markerClass + " rstudio_chunk_start" :
                  markerClass;

               this.$markers[row] = this.$session.addMarker(
                  new Range(row, 0, row, Infinity),
                  clazz,
                  markerType,
                  false
               );
            }

         }
      };

      this.$onDocChange = function(evt)
      {
         var delta = evt.data;
         var action = delta.action;
         var range = delta.range;

         // debuglog("Document change [" + action + "]");

         // First, synchronize the internal $rowState array with the
         // document change delta. We want to splice in / out rows
         // based on the number of newlines inserted / removed in the delta.
         //
         // Note that on such newline insertions, we want to also invalidate
         // the start row.
         if (action === "insertLines")
         {
            this.$rowState[range.start.row] = undefined;
            var newLineCount = range.end.row - range.start.row;
            this.$insertNewRows(range.start.row, newLineCount);
         }
         else if (action === "insertText" && this.$doc.isNewLine(delta.text))
         {
            this.$rowState[range.start.row] = undefined;
            this.$insertNewRows(range.end.row, 1);
         }
         else if (action === "removeLines")
         {
            this.$rowState[range.start.row] = undefined;
            this.$removeRows(
               range.start.row,
               range.end.row - range.start.row
            );
         }
         else if (action === "removeText" && this.$doc.isNewLine(delta.text))
         {
            this.$rowState[range.start.row] = undefined;
            this.$removeRows(range.end.row, 1);
         }

         this.$synchronize(range.start.row);
      };

      this.$onChangeFold = function(evt)
      {
         var row = evt.data.start.row;
         this.$synchronize(row);
      }

   }).call(BackgroundHighlighter.prototype);

   exports.BackgroundHighlighter = BackgroundHighlighter;
});
