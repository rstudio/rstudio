/*
 * markdown.js
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

define("mode/rmarkdown", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var MarkdownMode = require("mode/markdown").Mode;

var Tokenizer = require("ace/tokenizer").Tokenizer;
var RMarkdownHighlightRules = require("mode/rmarkdown_highlight_rules").RMarkdownHighlightRules;

var MatchingBraceOutdent = require("ace/mode/matching_brace_outdent").MatchingBraceOutdent;
var RMatchingBraceOutdent = require("mode/r_matching_brace_outdent").RMatchingBraceOutdent;
var CppMatchingBraceOutdent = require("mode/c_cpp_matching_brace_outdent").CppMatchingBraceOutdent;

var RCodeModel = require("mode/r_code_model").RCodeModel;
var CppCodeModel = require("mode/cpp_code_model").CppCodeModel;
var PythonMode = require("mode/python").Mode;

var RMarkdownFoldMode = require("rstudio/folding/rmarkdown").FoldMode;
var CFoldMode = require("ace/mode/folding/cstyle").FoldMode;

var AutoBraceInsert = require("mode/auto_brace_insert").AutoBraceInsert;

var Utils = require("mode/utils");
var unicode = require("ace/unicode");

var Mode = function(suppressHighlighting, session) {
   var that = this;

   this.$session = session;
   this.$tokenizer = new Tokenizer(new RMarkdownHighlightRules().getRules());
   this.$outdent = new MatchingBraceOutdent();

   this.codeModel = new RCodeModel(
      session,
      this.$tokenizer,
      /^r-/,
      new RegExp(RMarkdownHighlightRules.prototype.$reChunkStartString),
      new RegExp(RMarkdownHighlightRules.prototype.$reChunkEndString)
   );
   this.$r_outdent = new RMatchingBraceOutdent(this.codeModel);

   this.cpp_codeModel = new CppCodeModel(
      session,
      this.$tokenizer,
      /^r-cpp-/,
      new RegExp(RMarkdownHighlightRules.prototype.$reCppChunkStartString),
      new RegExp(RMarkdownHighlightRules.prototype.$reChunkEndString)
   );
   this.$cpp_outdent = new CppMatchingBraceOutdent(this.cpp_codeModel);

   this.$python = new PythonMode();

   var rMarkdownFoldingRules = new RMarkdownFoldMode();
   var cFoldingRules = new CFoldMode();
   
   // Patch tokenizer to allow for YAML start at beginning of document
   this.$tokenizer.getLineTokens = function(line, state, row) {
      if (row === 0)
         state = "firstLine";
      return Tokenizer.prototype.getLineTokens.call(this, line, state, row);
   }

   // NOTE: R Markdown is in charge of generating all 'top-level' folds.
   // That is, for the YAML header, chunk boundaries, and Markdown headers.
   this.foldingRules = {

      getFoldWidget: function(session, foldStyle, row) {

         var position = {row: row, column: 0};
         var mode = that.getLanguageMode(position);
         var line = session.getLine(row);

         if (mode === "Markdown" || Utils.startsWith(line, "```") || row === 0)
            return rMarkdownFoldingRules.getFoldWidget(session, foldStyle, row);
         else if (mode === "C_CPP")
            return cFoldingRules.getFoldWidget(session, foldStyle, row);
         else
            return that.codeModel.getFoldWidget(session, foldStyle, row);
      },

      getFoldWidgetRange: function(session, foldStyle, row) {

         var position = {row: row, column: 0};
         var mode = that.getLanguageMode(position);
         var line = session.getLine(row);
         
         if (mode === "Markdown" || Utils.startsWith(line, "```") || row === 0)
            return rMarkdownFoldingRules.getFoldWidgetRange(session, foldStyle, row);
         else if (mode === "C_CPP")
            return cFoldingRules.getFoldWidgetRange(session, foldStyle, row);
         else
            return that.codeModel.getFoldWidgetRange(session, foldStyle, row);
      }

   };
};
oop.inherits(Mode, MarkdownMode);

(function() {

   function activeMode(state)
   {
      return Utils.activeMode(state, "markdown");
   }

   this.insertChunkInfo = {
      value: "```{r}\n\n```\n",
      position: {row: 0, column: 5},
      content_position: {row: 1, column: 0}
   };

   this.getLanguageMode = function(position)
   {
      var state = Utils.getPrimaryState(this.$session, position.row);
      var mode = activeMode(state);
      if (mode === "r")
         return "R";
      else if (mode === "r-cpp")
         return "C_CPP";
      else if (mode === "yaml")
         return "YAML";
      else if (mode === "python")
         return "Python";
      else if (mode == "sql")
         return "SQL";
      else if (mode === "stan")
         return "Stan";
      else
         return "Markdown";
   };

   this.$getNextLineIndent = this.getNextLineIndent;
   this.getNextLineIndent = function(state, line, tab, row, dontSubset)
   {
      var mode = activeMode(state);

      if (mode === "r")
         return this.codeModel.getNextLineIndent(state, line, tab, row);
      else if (mode === "r-cpp")
         return this.cpp_codeModel.getNextLineIndent(state, line, tab, row, dontSubset);
      else if (mode === "yaml")
         return this.$getIndent(this.$session.getLine(row + 1));
      else if (mode === "python")
         return this.$python.getNextLineIndent(state, line, tab, row);
      else
         return this.$getNextLineIndent(state, line, tab);
   };

   this.checkOutdent = function(state, line, input)
   {
      var mode = activeMode(state);
      if (mode === "r")
         return this.$r_outdent.checkOutdent(state, line, input);
      else if (mode === "r-cpp")
         return this.$cpp_outdent.checkOutdent(state, line, input);
      else if (mode === "yaml")
         return false;
      else
         return this.$outdent.checkOutdent(line, input);
   };

   this.autoOutdent = function(state, session, row)
   {
      var mode = activeMode(state);
      if (mode === "r")
         return this.$r_outdent.autoOutdent(state, session, row);
      else if (mode === "r-cpp")
         return this.$cpp_outdent.autoOutdent(state, session, row);
      else if (mode === "yaml")
         return;
      else if (mode === "python")
         return this.$python.autoOutdent(state, session, row);
      else
         return this.$outdent.autoOutdent(session, row);
   };

   this.transformAction = function(state, action, editor, session, text) {
      var mode = activeMode(state);
      if (mode === "r-cpp")
         return this.transformActionCpp(state, action, editor, session, text);
      else if (mode === "yaml")
         return this.transformActionYaml(state, action, editor, session, text);
      else if (mode === "python")
         return this.$python.transformAction(state, action, editor, session, text);
      else
         return false;
   };

   this.transformActionCpp = function(state, action, editor, session, text) {

      // from c_cpp.js
      if (action === 'insertion') {
         if (text === "\n") {
            // If newline in a doxygen comment, continue the comment
            var pos = editor.getSelectionRange().start;
            var match = /^((\s*\/\/+')\s*)/.exec(session.doc.getLine(pos.row));
            if (match && editor.getSelectionRange().start.column >= match[2].length) {
               return {text: "\n" + match[1]};
            }
         }

         else if (text === "R") {
            // If newline to start and embedded R chunk complete the chunk
            var pos = editor.getSelectionRange().start;
            var match = /^(\s*\/\*{3,}\s*)/.exec(session.doc.getLine(pos.row));
            if (match && editor.getSelectionRange().start.column >= match[1].length) {
               return {text: "R\n\n*/\n",
                       selection: [1,0,1,0]};
            }
         }
      }

      return false;
   };

   this.transformActionYaml = function(state, action, editor, session, text)
   {
      if (action === "insertion")
      {
         if (text === "\n")
         {
            // get current line + indent
            var pos = editor.getCursorPosition();
            var row = pos.row;
            var line = session.getLine(row);
            var indent = this.$getIndent(line);

            // if this line ends with a ':' or opening bracket,
            // then add some indent
            if (/[:({[]$/.test(line)) {
               var tab = this.$session.getTabString();
               return {
                  text: "\n" + indent + tab
               };
            }

            // otherwise, just preserve the current indent
            return {
               text: "\n" + indent
            };
         }
      }

      return false;
   };

   this.tokenRe = new RegExp("^[" + unicode.wordChars + "._]+", "g");
   this.nonTokenRe = new RegExp("^(?:[^" + unicode.wordChars + "._]|\\s)+", "g");

   this.allowAutoInsert = this.smartAllowAutoInsert;

   this.$id = "mode/rmarkdown";

}).call(Mode.prototype);

exports.Mode = Mode;
});
