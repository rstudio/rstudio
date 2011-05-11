/*
 * r.js
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
define("mode/r", function(require, exports, module)
{

   var Range = require("ace/range").Range;
   var oop = require("pilot/oop");
   var TextMode = require("ace/mode/text").Mode;
   var Tokenizer = require("ace/tokenizer").Tokenizer;
   var TextHighlightRules = require("ace/mode/text_highlight_rules")
         .TextHighlightRules;
   var RHighlightRules = require("mode/r_highlight_rules").RHighlightRules;
   var IndentManager = require("mode/r_autoindent").IndentManager;
   var MatchingBraceOutdent = require("ace/mode/matching_brace_outdent").MatchingBraceOutdent;

   var Mode = function(suppressHighlighting, doc)
   {
      if (suppressHighlighting)
         this.$tokenizer = new Tokenizer(new TextHighlightRules().getRules());
      else
         this.$tokenizer = new Tokenizer(new RHighlightRules().getRules());
      this.$outdent = new MatchingBraceOutdent();

      this.$indentManager = new IndentManager(doc, this.$tokenizer, null);
   };
   oop.inherits(Mode, TextMode);

   (function()
   {
      this.wrapInsert = function(session, __insert, position, text)
      {
         var cursor = session.selection.getCursor();
         var typing = session.selection.isEmpty() &&
                      position.row == cursor.row &&
                      position.column == cursor.column;

         var endPos = __insert.call(session, position, text);
         // Is this an open paren?
         if (typing && /^\(+$/.test(text)) {
            // Is the next char not a character or number?
            var nextCharRng = Range.fromPoints(endPos, {
               row: endPos.row,
               column: endPos.column + 1
            });
            var nextChar = session.doc.getTextRange(nextCharRng);
            if (/^[;,\s)]$/.test(nextChar) || nextChar.length == 0) {
               session.doc.insert(endPos, ")");
               session.selection.moveCursorTo(endPos.row, endPos.column, false);
            }
         }
         return endPos;
      };

      this.wrapRemoveLeft = function(editor, __removeLeft)
      {
         if (editor.$readOnly)
            return;

         var secondaryDeletion = null;
         if (editor.selection.isEmpty()) {
            editor.selection.selectLeft();
            if (editor.session.getDocument().getTextRange(editor.selection.getRange()) == "(")
            {
               var nextCharRng = Range.fromPoints(editor.selection.getRange().end, {
                  row: editor.selection.getRange().end.row,
                  column: editor.selection.getRange().end.column + 1
               });
               var nextChar = editor.session.getDocument().getTextRange(nextCharRng);
               if (nextChar == ")")
               {
                  secondaryDeletion = editor.getSelectionRange();
               }
            }
         }

         editor.session.remove(editor.getSelectionRange());
         if (secondaryDeletion)
            editor.session.remove(secondaryDeletion);
         editor.clearSelection();
      };

      this.getNextLineIndent = function(state, line, tab, tabSize, row)
      {
         return this.$indentManager.getNextLineIndent(row, line, state, tab, tabSize);
      };

      this.getCurrentFunction = function(position)
      {
         return this.$indentManager.getCurrentFunction(position);
      };

      this.getFunctionTree = function()
      {
         return this.$indentManager.getFunctionTree();
      };

      this.checkOutdent = function(state, line, input) {
         if (! /^\s+$/.test(line))
            return false;

         return /^\s*[\{\}]/.test(input);
      };

      this.autoOutdent = function(state, doc, row) {
         if (row == 0)
            return 0;

         var line = doc.getLine(row);

         var match = line.match(/^(\s*\})/);
         if (match)
         {
            var column = match[1].length;
            var openBracePos = doc.findMatchingBracket({row: row, column: column});

            if (!openBracePos || openBracePos.row == row) return 0;

            var indent = this.$getIndent(doc.getLine(openBracePos.row));
            doc.replace(new Range(row, 0, row, column-1), indent);
         }

         match = line.match(/^(\s*\{)/);
         if (match)
         {
            var column = match[1].length;
            var indent = this.$indentManager.getBraceIndent(row-1);
            doc.replace(new Range(row, 0, row, column-1), indent);
         }
      };

      this.$getIndent = function(line) {
         var match = line.match(/^(\s+)/);
         if (match) {
            return match[1];
         }

         return "";
      };
   }).call(Mode.prototype);
   exports.Mode = Mode;

});
