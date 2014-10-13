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

define("mode/c_cpp", function(require, exports, module) {

var oop = require("ace/lib/oop");
var TextMode = require("ace/mode/text").Mode;
var Tokenizer = require("ace/tokenizer").Tokenizer;
var Range = require("ace/range").Range;
var RHighlightRules = require("mode/r_highlight_rules").RHighlightRules;
var c_cppHighlightRules = require("mode/c_cpp_highlight_rules").c_cppHighlightRules;

var MatchingBraceOutdent = require("mode/c_cpp_matching_brace_outdent").MatchingBraceOutdent;
var CStyleBehaviour = require("mode/behaviour/cstyle").CStyleBehaviour;

var CppStyleFoldMode = null;
if (!window.NodeWebkit) {
   CppStyleFoldMode = require("mode/c_cpp_fold_mode").FoldMode;
}

var SweaveBackgroundHighlighter = require("mode/sweave_background_highlighter").SweaveBackgroundHighlighter;
var RCodeModel = require("mode/r_code_model").RCodeModel;
var RMatchingBraceOutdent = require("mode/r_matching_brace_outdent").RMatchingBraceOutdent;

var CppCodeModel = require("mode/cpp_code_model").CppCodeModel;

var TokenCursor = require("mode/token_cursor").TokenCursor;

var Mode = function(suppressHighlighting, doc, session) {

   // Keep references to current session, document
   this.$session = session;
   this.$doc = doc;

   // R-related tokenization
   this.$r_tokenizer = new Tokenizer(new RHighlightRules().getRules());
   this.$r_outdent = {};
   oop.implement(this.$r_outdent, RMatchingBraceOutdent);
   this.$r_codeModel = new RCodeModel(doc, this.$r_tokenizer, null, /^\s*\/\*{3,}\s+[Rr]\s*$/);

   // C/C++ related tokenization
   this.$tokenizer = new Tokenizer(new c_cppHighlightRules().getRules());
   this.$codeModel = new CppCodeModel(session, this.$tokenizer);
   
   this.$behaviour = new CStyleBehaviour(this.$codeModel);
   this.$outdent = new MatchingBraceOutdent(this.$codeModel);
   
   this.$sweaveBackgroundHighlighter = new SweaveBackgroundHighlighter(
      session,
         /^\s*\/\*{3,}\s+[Rr]\s*$/,
         /^\s*\*\/$/,
      true
   );

   if (!window.NodeWebkit)     
      this.foldingRules = new CppStyleFoldMode();

   this.$tokens = this.$codeModel.$tokens;
   this.getLineSansComments = this.$codeModel.getLineSansComments;

};
oop.inherits(Mode, TextMode);

(function() {

   var that = this;

   this.insertChunkInfo = {
      value: "/*** R\n\n*/\n",
      position: {row: 1, column: 0}
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
      return this.$session.getState(position.row).match(/^r-/) ? 'R' : 'C_CPP';
   };

   this.inRLanguageMode = function(state)
   {
      return state.match(/^r-/);
   };

   this.getNextLineIndent = function(state, line, tab, tabSize, row, dontSubset) {

      // Defer to the R language indentation rules when in R language mode
      if (this.inRLanguageMode(state))
         return this.$r_codeModel.getNextLineIndent(row, line, state, tab, tabSize);
      else
         return this.$codeModel.getNextLineIndent(row, line, state, tab, tabSize, dontSubset);

   };

   this.checkOutdent = function(state, line, input) {
      if (this.inRLanguageMode(state))
         return this.$r_outdent.checkOutdent(state, line, input);
      else
         return this.$outdent.checkOutdent(state, line, input);
   };

   this.autoOutdent = function(state, doc, row) {
      if (this.inRLanguageMode(state))
         return this.$r_outdent.autoOutdent(state, doc, row, this.$r_codeModel);
      else
         return this.$outdent.autoOutdent(state, doc, row);
   };

}).call(Mode.prototype);

exports.Mode = Mode;
});
