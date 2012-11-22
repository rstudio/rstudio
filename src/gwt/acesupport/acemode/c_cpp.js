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
 * This program is licensed to you under the terms of version 3 of the
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
var c_cppHighlightRules = require("mode/c_cpp_highlight_rules").c_cppHighlightRules;

var MatchingBraceOutdent = require("ace/mode/matching_brace_outdent").MatchingBraceOutdent;
var Range = require("ace/range").Range;
var CstyleBehaviour = require("ace/mode/behaviour/cstyle").CstyleBehaviour;

var CppStyleFoldMode = require("mode/c_cpp_fold_mode").FoldMode;

var SweaveBackgroundHighlighter = require("mode/sweave_background_highlighter").SweaveBackgroundHighlighter;
var RCodeModel = require("mode/r_code_model").RCodeModel;
var RMatchingBraceOutdent = require("mode/r_matching_brace_outdent").RMatchingBraceOutdent;


var Mode = function(suppressHighlighting, doc, session) {
    this.$session = session;
    this.$tokenizer = new Tokenizer(new c_cppHighlightRules().getRules());
    this.$outdent = new MatchingBraceOutdent();
    this.$r_outdent = {};
    oop.implement(this.$r_outdent, RMatchingBraceOutdent);
    this.$behaviour = new CstyleBehaviour();
    this.codeModel = new RCodeModel(doc, this.$tokenizer, /^r-/, /^\s*\/\*{3,}\s*[Rr]\s*$/);
    this.$sweaveBackgroundHighlighter = new SweaveBackgroundHighlighter(
        session,
        /^\s*\/\*{3,}\s*[Rr]\s*$/,
        /^\*\/$/,
        true);
    this.foldingRules = new CppStyleFoldMode();

};
oop.inherits(Mode, TextMode);

(function() {

    this.insertChunkInfo = {
        value: "/*** R\n\n*/\n",
        position: {row: 1, column: 0}
    };

    this.toggleCommentLines = function(state, doc, startRow, endRow) {
        var outdent = true;
        var re = /^(\s*)\/\//;

        for (var i=startRow; i<= endRow; i++) {
            if (!re.test(doc.getLine(i))) {
                outdent = false;
                break;
            }
        }

        if (outdent) {
            var deleteRange = new Range(0, 0, 0, 0);
            for (var i=startRow; i<= endRow; i++)
            {
                var line = doc.getLine(i);
                var m = line.match(re);
                deleteRange.start.row = i;
                deleteRange.end.row = i;
                deleteRange.end.column = m[0].length;
                doc.replace(deleteRange, m[1]);
            }
        }
        else {
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

    this.getNextLineIndent = function(state, line, tab, tabSize, row) {

        if (this.inRLanguageMode(state))
           return this.codeModel.getNextLineIndent(row, line, state, tab, tabSize);

        var indent = this.$getIndent(line);

        var tokenizedLine = this.$tokenizer.getLineTokens(line, state);
        var tokens = tokenizedLine.tokens;
        var endState = tokenizedLine.state;

        if (tokens.length && tokens[tokens.length-1].type == "comment") {
            return indent;
        }

        if (state == "start") {
            var match = line.match(/^.*[\{\(\[]\s*$/);
            if (match) {
                indent += tab;
            }
        } else if (state == "doc-start") {
            if (endState == "start") {
                return "";
            }
            var match = line.match(/^\s*(\/?)\*/);
            if (match) {
                if (match[1]) {
                    indent += " ";
                }
                indent += "* ";
            }
        }

        return indent;
    };

    this.checkOutdent = function(state, line, input) {
        if (this.inRLanguageMode(state))
            return this.$r_outdent.checkOutdent(line,input);
        else
            return this.$outdent.checkOutdent(line, input);
    };

    this.autoOutdent = function(state, doc, row) {
        if (this.inRLanguageMode(state))
            return this.$r_outdent.autoOutdent(state, doc, row);
        else
            return this.$outdent.autoOutdent(doc, row);
    };
    
    this.transformAction = function(state, action, editor, session, text) {
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

}).call(Mode.prototype);

exports.Mode = Mode;
});
