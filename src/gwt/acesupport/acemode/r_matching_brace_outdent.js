/*
 * r_matching_brace_outdent.js
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
define("mode/r_matching_brace_outdent", function(require, exports, module)
{
   var Range = require("ace/range").Range;

   // This is a mixin that enables proper brace outdent behavior for R code.

   var RMatchingBraceOutdent = {};

   (function()
   {
      this.checkOutdent = function(state, line, input) {
         if (/^\s+$/.test(line) && /^\s*[\{\}\)]/.test(input))
            return true;

         if (/^\s*}\s*$/.test(line) && input == "\n")
            return true;

         return false;
      };

      this.autoOutdent = function(state, doc, row) {
         if (row == 0)
            return 0;

         var line = doc.getLine(row);

         var match = line.match(/^(\s*[\}\)])/);
         if (match)
         {
            var column = match[1].length;
            var openBracePos = doc.findMatchingBracket({row: row, column: column});

            if (!openBracePos || openBracePos.row == row) return 0;

            var indent = this.codeModel.getIndentForOpenBrace(openBracePos);
            doc.replace(new Range(row, 0, row, column-1), indent);
         }

         match = line.match(/^(\s*\{)/);
         if (match)
         {
            var column = match[1].length;
            var indent = this.codeModel.getBraceIndent(row-1);
            doc.replace(new Range(row, 0, row, column-1), indent);
         }
      };
   }).call(RMatchingBraceOutdent);
   exports.RMatchingBraceOutdent = RMatchingBraceOutdent;
});
