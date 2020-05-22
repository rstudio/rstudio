/*
 * r_matching_brace_outdent.js
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
define("mode/r_matching_brace_outdent", ["require", "exports", "module"], function(require, exports, module)
{
   var Range = require("ace/range").Range;

   var RMatchingBraceOutdent = function(codeModel) {
      this.codeModel = codeModel;
   };

   (function()
   {
      this.checkOutdent = function(state, line, input) {
         if (/^\s+$/.test(line) && /^\s*[\{\}\)\]]/.test(input))
            return true;

         // This is the case of a newline being inserted on a line that only
         // contains }
         if (/^\s*}\s*$/.test(line) && input == "\n")
            return true;

         // This is the case of a newline being inserted on a line that contains
         // a bunch of stuff including }, and the user hits Enter. The input
         // is not necessarily "\n" because we may auto-insert some padding
         // as well.
         //
         // We don't always want to autoindent in this case; ideally we would
         // only autoindent if Enter was being hit right before }. But at this
         // time we don't have that information. So we let the autoOutdent logic
         // run and trust it to only outdent if appropriate.
         if (/}\s*$/.test(line) && /\n/.test(input))
            return true;

         return false;
      };

      this.autoOutdent = function(state, session, row) {
         if (row === 0)
            return 0;

         var line = session.getLine(row);

         var match = line.match(/^(\s*[\}\)\]])/);
         if (match)
         {
            var column = match[1].length;
            var openBracePos = session.findMatchingBracket({row: row, column: column});

            if (!openBracePos || openBracePos.row == row) return 0;

            var indent = this.codeModel.getIndentForOpenBrace(openBracePos);
            session.replace(new Range(row, 0, row, column-1), indent);
         }

         match = line.match(/^(\s*\{)/);
         if (match)
         {
            var column = match[1].length;
            var indent = this.codeModel.getBraceIndent(row - 1);
            session.replace(new Range(row, 0, row, column - 1), indent);
         }
      };
   }).call(RMatchingBraceOutdent.prototype);
   exports.RMatchingBraceOutdent = RMatchingBraceOutdent;
});
