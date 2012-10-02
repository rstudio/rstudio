/*
 * r.js
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
   var Editor = require("ace/editor").Editor;
   var EditSession = require("ace/edit_session").EditSession;
   var Range = require("ace/range").Range;
   var oop = require("ace/lib/oop");
   var TextMode = require("ace/mode/text").Mode;
   var Tokenizer = require("ace/tokenizer").Tokenizer;
   var TextHighlightRules = require("ace/mode/text_highlight_rules")
         .TextHighlightRules;
   var RHighlightRules = require("mode/r_highlight_rules").RHighlightRules;
   var RCodeModel = require("mode/r_code_model").RCodeModel;
   var RMatchingBraceOutdent = require("mode/r_matching_brace_outdent").RMatchingBraceOutdent;
   var AutoBraceInsert = require("mode/auto_brace_insert").AutoBraceInsert;
   var unicode = require("ace/unicode");

   var Mode = function(suppressHighlighting, doc, session)
   {
      if (suppressHighlighting)
         this.$tokenizer = new Tokenizer(new TextHighlightRules().getRules());
      else
         this.$tokenizer = new Tokenizer(new RHighlightRules().getRules());

      this.codeModel = new RCodeModel(doc, this.$tokenizer, null);
      this.foldingRules = this.codeModel;
   };
   oop.inherits(Mode, TextMode);

   (function()
   {
      oop.implement(this, RMatchingBraceOutdent);

      this.tokenRe = new RegExp("^["
          + unicode.packages.L
          + unicode.packages.Mn + unicode.packages.Mc
          + unicode.packages.Nd
          + unicode.packages.Pc + "._]+", "g"
      );

      this.nonTokenRe = new RegExp("^(?:[^"
          + unicode.packages.L
          + unicode.packages.Mn + unicode.packages.Mc
          + unicode.packages.Nd
          + unicode.packages.Pc + "._]|\s])+", "g"
      );

      this.$complements = {
               "(": ")",
               "[": "]",
               '"': '"',
               "'": "'",
               "{": "}"
            };
      this.$reOpen = /^[(["'{]$/;
      this.$reClose = /^[)\]"'}]$/;

      this.getNextLineIndent = function(state, line, tab, tabSize, row)
      {
         return this.codeModel.getNextLineIndent(row, line, state, tab, tabSize);
      };

      this.allowAutoInsert = this.smartAllowAutoInsert;

      this.getIndentForOpenBrace = function(openBracePos)
      {
         return this.codeModel.getIndentForOpenBrace(openBracePos);
      };

      this.$getIndent = function(line) {
         var match = line.match(/^(\s+)/);
         if (match) {
            return match[1];
         }

         return "";
      };

      this.transformAction = function(state, action, editor, session, text) {
         if (action === 'insertion' && text === "\n") {

            // If newline in a doxygen comment, continue the comment
            var pos = editor.getSelectionRange().start;
            var line = session.doc.getLine(pos.row);

            var match = /^((\s*#+')\s*)/.exec(line);
            if (match && pos.start.column >= match[2].length) {
               return {text: "\n" + match[1]};
            }

            /* If we're putting a newline right before a closing brace, we don't
               want the closing newline to be incorrectly indented.

               There are two scenarios here (pipe character is the cursor pos):

               foo <- function() {|}

               foo <- function() {
               |}

               The difference between these two cases is whether there is
               non-whitespace content to the left of the cursor. If yes, then
               we assume the user wants to start typing a new line of content
               above the close brace. In this case, we want to put the cursor on
               its own line and the close brace on its own line, with the two
               indented independently indented.

               In the other case (whitespace or nothing to the left of the
               cursor) then we just assume the user wants to move the cursor
               down.
            */
/*
            if (/^\s*}/.test(line.substring(pos.column))) {
                var openBracePos = session.findMatchingBracket({row: pos.row, column: pos.column + 1});
                if (!openBracePos)
                     return null;

                var indent = this.getNextLineIndent(state, line.substring(0, line.length - 1), session.getTabString(), session.getTabSize(), pos.row);
                var next_indent = this.$getIndent(session.doc.getLine(openBracePos.row));

                if (/^\s*$/.test(line.substring(0, pos.column))) {
                    return {
                       text: '\n' + next_indent
                    };
                }

                return {
                    text: '\n' + indent + '\n' + next_indent,
                    selection: [1, indent.length, 1, indent.length]
                };
            }
*/
         }
         return false;
      };
   }).call(Mode.prototype);
   exports.Mode = Mode;
});
