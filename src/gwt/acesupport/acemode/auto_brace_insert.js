/*
 * auto_brace_insert.js
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
define("mode/auto_brace_insert", ["require", "exports", "module"], function(require, exports, module)
{
   var Range = require("ace/range").Range;
   var TextMode = require("ace/mode/text").Mode;

   (function() {

      // modes can override these to provide for
      // auto-pairing of other kinds of tokens
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
            var currentChar = session.doc.getTextRange(Range.fromPoints(endPos, rangeEnd));
            if ((prevChar === "{" && currentChar === "}") ||
                (prevChar === "(" && currentChar === ")") ||
                (prevChar === "[" && currentChar === "]"))
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
         // Always allow auto-insert for other insertion types
         if (text !== "'" && text !== '"' && text !== '`')
            return true;

         // Only allow auto-insert of a '`' character if the number of
         // backticks on the line is not balanced. Note that this is an
         // R-centric view of backquoted strings, and assumes things within
         // can be escaped by \.
         if (text === '`')
         {
            // get line up to cursor position
            var start = {row: pos.row, column: 0};
            var line = session.doc.getTextRange({start: start, end: pos});

            // remove escaped characters from the line
            line = line.replace(/\\./g, '');

            // check count of '`' characters
            var match = line.match(/`/g);
            return (match.length % 2) != 0;
         }

         // Only allow auto-insertion of a quote char if the actual character
         // that was typed, was the start of a new string token
         if (pos.column == 0)
            return true;

         var token = this.codeModel.getTokenForPos(pos, false, true);
         return token &&
                token.type === 'string' &&
                token.column === pos.column - 1;
      };

      this.wrapRemove = function(editor, __remove, dir)
      {
         var cursor = editor.selection.getCursor();
         var doc = editor.session.getDocument();

         // Here are some easy-to-spot reasons why it might be impossible for us
         // to need our special deletion logic.
         if (!this.insertMatching ||
             dir != "left" ||
             !editor.selection.isEmpty() ||
             editor.$readOnly ||
             cursor.column == 0 ||     // hitting backspace at the start of line
             doc.getLine(cursor.row).length <= cursor.column) {

            return __remove.call(editor, dir);
         }

         var leftRange = Range.fromPoints(this.$moveLeft(doc, cursor), cursor);
         var rightRange = Range.fromPoints(cursor, this.$moveRight(doc, cursor));
         var leftText = doc.getTextRange(leftRange);

         var deleteRight = this.$reOpen.test(leftText) &&
                           this.$complements[leftText] == doc.getTextRange(rightRange);

         __remove.call(editor, dir);
         if (deleteRight)
            __remove.call(editor, 'right');
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
   }).call(TextMode.prototype);

   exports.setInsertMatching = function(insertMatching) {
      TextMode.prototype.insertMatching = insertMatching;
   };

});
