/*
 * c_cpp_fold_mode.js
 *
 * Copyright (C) 2020 by RStudio, PBC
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


define("mode/c_cpp_fold_mode", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var Range = require("ace/range").Range;
var BaseFoldMode = require("ace/mode/folding/fold_mode").FoldMode;

var FoldMode = exports.FoldMode = function() {};
oop.inherits(FoldMode, BaseFoldMode);

(function() {

    var reBracketStart = /(\{|\[)[^\}\]]*$/;
    var reBracketEnd   = /^[^\[\{]*(\}|\])/;

    this.getFoldWidget = function(session, foldStyle, row) {

        var FOLD_NONE = "";
        var FOLD_START = "start";
        var FOLD_END = foldStyle === "markbeginend" ? "end" : "";

        var line = session.getLine(row);

        if (reBracketStart.test(line))
            return FOLD_START;

        if (reBracketEnd.test(line))
            return FOLD_END;

        var commentStartIdx = line.indexOf("/*");
        var commentEndIdx = line.indexOf("*/");

        if (commentStartIdx !== -1 && (commentEndIdx === -1 || commentStartIdx > commentEndIdx))
            return FOLD_START;

        if (commentEndIdx !== -1 && (commentStartIdx === -1 || commentEndIdx < commentStartIdx))
            return FOLD_END;

        return FOLD_NONE;

    };

    function getBlockCommentRange(session, startRow, startColumn, delta)
    {
        var lines = session.doc.$lines;
        var row = startRow + delta;
        var line = lines[row];
        var target = delta > 0 ? "*/" : "/*";

        var range;
        while (line != null)
        {
            var idx = line.indexOf(target);
            if (idx !== -1)
            {
                range = delta > 0 ?
                    new Range(startRow, startColumn, row, idx) :
                    new Range(row, line.length, startRow, startColumn);
                break;
            }

            row += delta;
            line = lines[row];
        }

        return range;
    }

    function findChunkRange(session, startRow, startColumn, targetType, delta)
    {
        var row = startRow + delta;
        var lines = session.doc.$lines;

        while (row >= 0 && row < session.getLength())
        {
            var tokens = session.getTokens(row);
            var line = lines[row];
            for (var i = 0; i < tokens.length; i++)
            {
                var token = tokens[i];
                if (token.type === targetType)
                {
                    return delta > 0 ?
                        new Range(startRow, startColumn, row, 0) :
                        new Range(row, line.length, startRow, startColumn);
                }
            }

            row += delta;
        }
    }

    this.getFoldWidgetRange = function(session, foldStyle, row) {

        var line = session.getLine(row);
        var match;

        // First, check for brackets for folding.
        match = line.match(reBracketStart);
        if (match)
            return this.openingBracketBlock(session, match[1], row, match.index);

        match = foldStyle === "markbeginend" && line.match(reBracketEnd);
        if (match)
            return this.closingBracketBlock(session, match[1], row, match.index + match[0].length);

        // Check for chunk headers / footers.
        var tokens = session.getTokens(row);
        for (var i = 0; i < tokens.length; i++)
        {
            var token = tokens[i];
            if (token.type === "support.function.codebegin")
                return findChunkRange(session, row, line.length, "support.function.codeend", 1);
            else if (token.type === "support.function.codeend")
                return findChunkRange(session, row, 0, "support.function.codebegin", -1);
        }

        // Next, check for block comment folds.
        var idx;

        idx = line.indexOf("/*");
        if (idx !== -1)
            return getBlockCommentRange(session, row, line.length, 1);

        idx = line.indexOf("*/");
        if (idx !== -1)
            return getBlockCommentRange(session, row, idx, -1);

        // No match -- just return undefined.
    };

}).call(FoldMode.prototype);

});
