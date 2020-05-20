/*
 * r.js
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
define("mode/r", ["require", "exports", "module"], function(require, exports, module)
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

   var Mode = function(suppressHighlighting, session)
   {
      if (suppressHighlighting)
         this.$tokenizer = new Tokenizer(new TextHighlightRules().getRules());
      else
         this.$tokenizer = new Tokenizer(new RHighlightRules().getRules());

      this.codeModel = new RCodeModel(session, this.$tokenizer);
      this.foldingRules = this.codeModel;
      this.$outdent = new RMatchingBraceOutdent(this.codeModel);
   };
   oop.inherits(Mode, TextMode);

   (function()
   {
      this.getLanguageMode = function(position) {
         return "R";
      };

      this.checkOutdent = function(state, line, input) {
         return this.$outdent.checkOutdent(state, line, input);
      };

      this.autoOutdent = function(state, session, row) {
         return this.$outdent.autoOutdent(state, session, row);
      };
      
      this.tokenRe = new RegExp("^[" + unicode.wordChars + "._]+", "g");
      this.nonTokenRe = new RegExp("^(?:[^" + unicode.wordChars + "._]|\\s)+", "g");

      // NOTE: these override fields used for 'auto_brace_insert'
      this.$complements = {
               "(": ")",
               "[": "]",
               '"': '"',
               "'": "'",
               "{": "}",
               "`": "`"
            };
      this.$reOpen  = /^[{(\["'`]$/;
      this.$reClose = /^[})\]"'`]$/;

      this.getNextLineIndent = function(state, line, tab, row)
      {
         return this.codeModel.getNextLineIndent(state, line, tab, row);
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
            var docLine = session.doc.getLine(pos.row);
            var match = /^((\s*#+')\s*)/.exec(docLine);
            if (match && editor.getSelectionRange().start.column >= match[2].length) {
               return {text: "\n" + match[1]};
            }

            // If newline in a plumber comment, continue the comment
            match = /^((\s*#+\*)\s*)/.exec(docLine);
            if (match && editor.getSelectionRange().start.column >= match[2].length) {
               return {text: "\n" + match[1]};
            }
         }
         return false;
      };

      this.$id = "mode/r";
   }).call(Mode.prototype);
   exports.Mode = Mode;
});
