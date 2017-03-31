/*
 * c_cpp.js
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * The Original Code is Ajax.org Code Editor (ACE).
 *
 * The Initial Developer of the Original Code is
 * Ajax.org B.V.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *      Fabian Jakobs <fabian AT ajax DOT org>
 *      Gastón Kleiman <gaston.kleiman AT gmail DOT com>
 *
 * Based on Bespin's C/C++ Syntax Plugin by Marc McIntyre.
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

define("mode/c_cpp", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var TextMode = require("ace/mode/text").Mode;
var Tokenizer = require("ace/tokenizer").Tokenizer;
var Range = require("ace/range").Range;
var RHighlightRules = require("mode/r_highlight_rules").RHighlightRules;
var c_cppHighlightRules = require("mode/c_cpp_highlight_rules").c_cppHighlightRules;

var CppMatchingBraceOutdent = require("mode/c_cpp_matching_brace_outdent").CppMatchingBraceOutdent;
var CStyleBehaviour = require("mode/behaviour/cstyle").CStyleBehaviour;

var CppStyleFoldMode = null;
if (!window.NodeWebkit) {
   CppStyleFoldMode = require("mode/c_cpp_fold_mode").FoldMode;
}

var RCodeModel = require("mode/r_code_model").RCodeModel;
var RMatchingBraceOutdent = require("mode/r_matching_brace_outdent").RMatchingBraceOutdent;

var CppCodeModel = require("mode/cpp_code_model").CppCodeModel;

var TokenCursor = require("mode/token_cursor").TokenCursor;

var Utils = require("mode/utils");

var Mode = function(suppressHighlighting, session) {

   // Keep references to current session, document
   this.$session = session;
   this.$doc = session.getDocument();

   // Only need one tokenizer for the document (we assume other rules
   // have been properly embedded)
   this.$tokenizer = new Tokenizer(new c_cppHighlightRules().getRules());

   // R-related tokenization
   this.r_codeModel = new RCodeModel(
      session,
      this.$tokenizer,
      /^r-/,
      /^\s*\/\*{3,}\s*([Rr])\s*$/,
      /^\s*\*+\//
   );
   this.$r_outdent = new RMatchingBraceOutdent(this.r_codeModel);

   // C/C++ related tokenization
   this.codeModel = new CppCodeModel(session, this.$tokenizer);
   
   this.$behaviour = new CStyleBehaviour(this.codeModel);
   this.$cpp_outdent = new CppMatchingBraceOutdent(this.codeModel);
   
   if (!window.NodeWebkit)     
      this.foldingRules = new CppStyleFoldMode();

   this.$tokens = this.codeModel.$tokens;
   this.getLineSansComments = this.codeModel.getLineSansComments;

};
oop.inherits(Mode, TextMode);

(function() {

   // We define our own 'wrapInsert', 'wrapRemove' functions that
   // delegate directly back to the editor / session -- this is
   // because we don't want to inherit the automatic matching brace
   // behaviour. Note that it is attached to the TextMode prototype by
   // 'matching_brace_outdent.js' and called through the wrappers set
   // in 'loader.js'.
   this.wrapInsert = function(session, __insert, position, text) {
      if (!this.cursorInRLanguageMode())
         return __insert.call(session, position, text);
      else
         return TextMode.prototype.wrapInsert(session, __insert, position, text);
   };

   this.wrapRemove = function(editor, __remove, dir) {
      if (!this.cursorInRLanguageMode())
         return __remove.call(editor, dir);
      else
         return TextMode.prototype.wrapRemove(editor, __remove, dir);
   };

   var that = this;

   this.insertChunkInfo = {
      value: "/*** R\n\n*/\n",
      position: {row: 1, column: 0},
      content_position: {row: 1, column: 0}
   };

   this.toggleCommentLines = function(state, doc, startRow, endRow) {
      var outdent = true;
      var re = /^(\s*)\/\//;

      for (var i = startRow; i <= endRow; i++) {
         if (!re.test(doc.getLine(i))) {
            outdent = false;
            break;
         }
      }

      if (outdent) {
         var deleteRange = new Range(0, 0, 0, 0);
         for (var i = startRow; i <= endRow; i++)
         {
            var line = doc.getLine(i);
            var m = line.match(re);
            deleteRange.start.row = i;
            deleteRange.end.row = i;
            deleteRange.end.column = m[0].length;
            doc.replace(deleteRange, m[1]);
         }
      } else {
         doc.indentRows(startRow, endRow, "//");
      }
   };

   this.getLanguageMode = function(position)
   {
      var state = Utils.getPrimaryState(this.$session, position.row);
      return state.match(/^r-/) ? 'R' : 'C_CPP';
   };

   this.cursorInRLanguageMode = function()
   {
      return this.getLanguageMode(this.$session.getSelection().getCursor()) === "R";
   };

   this.inRLanguageMode = function(state)
   {
      return state.match(/^r-/);
   };

   this.getNextLineIndent = function(state, line, tab, row, dontSubset)
   {
      state = Utils.primaryState(state);
      // Defer to the R language indentation rules when in R language mode
      if (this.inRLanguageMode(state))
         return this.r_codeModel.getNextLineIndent(state, line, tab, row);
      else
         return this.codeModel.getNextLineIndent(state, line, tab, row, dontSubset);
   };

   this.checkOutdent = function(state, line, input) {
      if (this.inRLanguageMode(state))
         return this.$r_outdent.checkOutdent(state, line, input);
      else
         return this.$cpp_outdent.checkOutdent(state, line, input);
   };

   this.autoOutdent = function(state, doc, row) {
      if (this.inRLanguageMode(state))
         return this.$r_outdent.autoOutdent(state, doc, row, this.r_codeModel);
      else
         return this.$cpp_outdent.autoOutdent(state, doc, row);
   };

   this.$transformAction = this.transformAction;
   this.transformAction = function(state, action, editor, session, param) {
      state = Utils.primaryState(state);
      if (this.inRLanguageMode(state)) {
         // intentionally left blank -- this behaviour is handled elsewhere
         // in the code base
      } else {
         return this.$transformAction(state, action, editor, session, param);
      }
   };

   this.$id = "mode/c_cpp";

}).call(Mode.prototype);

exports.Mode = Mode;
});
