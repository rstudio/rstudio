/*
 * python.js
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

/* ***** BEGIN LICENSE BLOCK *****
 * Distributed under the BSD license:
 *
 * Copyright (c) 2010, Ajax.org B.V.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Ajax.org B.V. nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL AJAX.ORG B.V. BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ***** END LICENSE BLOCK ***** */

define("mode/python", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var TextMode = require("ace/mode/text").Mode;
var PythonHighlightRules = require("mode/python_highlight_rules").PythonHighlightRules;
var PythonFoldMode = require("ace/mode/folding/pythonic").FoldMode;
var Range = require("ace/range").Range;

var Mode = function() {
    this.HighlightRules = PythonHighlightRules;
    this.foldingRules = new PythonFoldMode("\\:");
};
oop.inherits(Mode, TextMode);

(function() {

   this.lineCommentStart = "#";

   this.getLanguageMode = function(position)
   {
      return "Python";
   };

   this.getNextLineIndent = function(state, line, tab, row) {

      var indent = this.$getIndent(line);

      // if this line is a comment, use the same indent
      if (/^\s*[#]/.test(line))
         return indent;

      // detect lines ending with something that should increase indent
      // (nominally, these are open brackets and ':')
      if (/[{([:]\s*(?:$|[#])/.test(line))
         indent += tab;

      // decrease indent following things that 'end' a scope
      if (/^\s*(?:break|continue|pass|raise|return)\b/.test(line))
         indent = indent.substring(0, indent.length - tab.length);

      return indent;

   };

   this.checkOutdent = function(state, line, input) {
      return false;
   };

   this.autoOutdent = function(state, doc, row) {
      return;
   };

   this.transformAction = function(state, action, editor, session, param) {
      return false;
   };

   this.$id = "mode/python";

}).call(Mode.prototype);

exports.Mode = Mode;

});
