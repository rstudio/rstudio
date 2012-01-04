/*
 * auto_brace_insert.js
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
define("mode/auto_brace_insert", function(require, exports, module)
{
   var Range = require("ace/range").Range;
   var TextMode = require("ace/mode/text").Mode;

   (function()
   {
      this.$complements = {
         "(": ")",
         "[": "]",
         '"': '"',
         "{": "}"
      };
      this.$reOpen = /^[(["{]$/;
      this.$reClose = /^[)\]"}]$/;

      // reStop is the set of characters before which we allow ourselves to
      // automatically insert a closing paren. If any other character
      // immediately follows the cursor we will NOT do the insert.
      this.$reStop = /^[;,\s)\]}]$/;

      this.wrapInsert = function(session, __insert, position, text)
      {
         if (!this.insertMatching)
            return __insert.call(session, position, text);

         var cursor = session.selection.getCursor();
         var typing = session.selection.isEmpty() &&
                      position.row == cursor.row &&
                      position.column == cursor.column;

         if (typing) {
            var postRng = Range.fromPoints(position, {
               row: position.row,
               column: position.column + 1});
            var postChar = session.doc.getTextRange(postRng);
            if (this.$reClose.test(postChar) && postChar == text) {
               session.selection.moveCursorTo(postRng.end.row,
                                              postRng.end.column,
                                              false);
               return;
            }
         }

         var prevChar = null;
         if (typing)
         {
            var rangeBegin = this.$moveLeft(session.doc, position);
            prevChar = session.doc.getTextRange(Range.fromPoints(rangeBegin,
                                                                 position));
         }

         var endPos = __insert.call(session, position, text);
         // Is this an open paren?
         if (typing && this.$reOpen.test(text)) {
            // Is the next char not a character or number?
            var nextCharRng = Range.fromPoints(endPos, {
               row: endPos.row,
               column: endPos.column + 1
            });
            var nextChar = session.doc.getTextRange(nextCharRng);
            if (this.$reStop.test(nextChar) || nextChar.length == 0) {
               if (this.allowAutoInsert(session, endPos, this.$complements[text])) {
                  session.doc.insert(endPos, this.$complements[text]);
                  session.selection.moveCursorTo(endPos.row, endPos.column, false);
               }
            }
         }
         else if (typing && text === "\n") {
            var rangeEnd = this.$moveRight(session.doc, endPos);
            if (prevChar == "{" && "}" == session.doc.getTextRange(Range.fromPoints(endPos, rangeEnd)))
            {
               var indent;
               if (this.getIndentForOpenBrace)
                  indent = this.getIndentForOpenBrace(this.$moveLeft(session.doc, position));
               else
                  indent = this.$getIndent(session.doc.getLine(endPos.row - 1));
               session.doc.insert(endPos, "\n" + indent);
               session.selection.moveCursorTo(endPos.row, endPos.column, false);
            }
         }
         return endPos;
      };

      this.allowAutoInsert = function(session, pos, text)
      {
         return true;
      };

      // To enable this, call "this.allowAutoInsert = this.smartAllowAutoInsert"
      // in the mode subclass
      this.smartAllowAutoInsert = function(session, pos, text)
      {
         if (text !== "'" && text !== '"')
            return true;

         // Only allow auto-insertion of a quote char if the actual character
         // that was typed, was the start of a new string token

         if (pos.column == 0)
            return true;

         var token = this.$rCodeModel.getTokenForPos(pos, false, true);
         return token &&
                token.type === 'string' &&
                token.column === pos.column-1;
      };

      this.$moveLeft = function(doc, pos)
      {
         if (pos.row == 0 && pos.column == 0)
            return pos;

         var row = pos.row;
         var col = pos.column;

         if (col)
            col--;
         else
         {
            row--;
            col = doc.getLine(row).length;
         }
         return {row: row, column: col};
      };

      this.$moveRight = function(doc, pos)
      {
         var row = pos.row;
         var col = pos.column;

         if (doc.getLine(row).length != col)
            col++;
         else
         {
            row++;
            col = 0;
         }

         if (row >= doc.getLength())
            return pos;
         else
            return {row: row, column: col};
      };

      this.$attachBehavioursIfNeeded = function() {
         if (this.$autoBraceInsertBehaviorsAttached)
            return;

         this.$autoBraceInsertBehaviorsAttached = true;

         this.$behaviour.add('auto_insert', 'deletion',
                             function(state, action, editor, session, range) {
            if (!this.insertMatching) {
               return false;
            }

            if (editor.getReadOnly())
               return false;

            var text = session.getDocument().getTextRange(range);
            if (this.$reOpen.test(text))
            {
               var nextCharRng = Range.fromPoints(range.end, {
                  row: editor.selection.getRange().end.row,
                  column: editor.selection.getRange().end.column + 1
               });
               var nextChar = session.getDocument().getTextRange(nextCharRng);
               if (nextChar == this.$complements[text])
               {
                  return Range.fromPoints(range.start, nextCharRng.end);
               }
            }

            return false;
         });
      };

      var __transformAction = this.transformAction;
      this.transformAction = function(state, action, editor, session, param) {
         this.$attachBehavioursIfNeeded();
         return __transformAction.apply(this, arguments);
      };

   }).call(TextMode.prototype);

   exports.setInsertMatching = function(insertMatching) {
      TextMode.prototype.insertMatching = insertMatching;
   };

});