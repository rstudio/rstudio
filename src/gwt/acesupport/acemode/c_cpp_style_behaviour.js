/*
 * c_cpp_style_behaviour.js
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


define('mode/behaviour/cstyle', ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var Behaviour = require("ace/mode/behaviour").Behaviour;
var CppCodeModel = require("mode/cpp_code_model").CppCodeModel;
var CppTokenCursor = require("mode/token_cursor").CppTokenCursor;
var TextMode = require("ace/mode/text").Mode;
var Utils = require("mode/utils");

var $fillinDoWhile = true;

var CStyleBehaviour = function(codeModel) {

   var codeModel = codeModel;
   var $complements = codeModel.$complements;

   var autoPairInsertion = function(text, input, editor, session) {

      var leftChar = text;
      var rightChar = $complements[leftChar];

      if (input == leftChar) {

         var selection = editor.getSelectionRange();
         var selected = session.doc.getTextRange(selection);
         if (selected !== "") {
            return {
               text: leftChar + selected + rightChar,
               selection: false
            };
         } else {
            return {
               text: leftChar + rightChar,
               selection: [1, 1]
            };
         }
      } else if (input == rightChar) {
         var cursor = editor.getCursorPosition();
         var line = session.doc.getLine(cursor.row);
         var cursorRightChar = line[cursor.column];
         if (cursorRightChar == rightChar) {

            // TODO: Workaround for 'findMatchingBracket' failing for '<>'
            if (rightChar === '>')
               return { text: '', selection: [1, 1] };

            var matchPos = session.findMatchingBracket({
               row: cursor.row,
               column: cursor.column + 1
            });
            
            if (matchPos !== null) {
               return {
                  text: '',
                  selection: [1, 1]
               };
            }
         }
      }
      
   };

   var autoPairDeletion = function(text, range, session) {

      var lChar = text;
      var rChar = $complements[text];
      
      var selected = session.doc.getTextRange(range);
      if (!range.isMultiLine() && selected == lChar) {
         var line = session.doc.getLine(range.start.row);
         var rightChar = line.substring(range.start.column + 1, range.start.column + 2);
         if (rightChar == rChar) {
            range.end.column++;
            return range;
         }
      }
   };
   

   this.add("R", "insertion", function(state, action, editor, session, text) {

      if (text === "R" || text === "r") {

         var cursor = editor.getCursorPosition();
         var line = new String(session.doc.getLine(cursor.row));
         var match = line.match(/^(\s*)\/\*{3,}\s*/);
         if (match) {
            return {
               text: "R\n" + match[1] + "\n" + match[1] + "*/",
               selection: [1, match[1].length, 1, match[1].length]
            };
         }
      }

   });

   this.add("newline", "insertion", function(state, action, editor, session, text) {

      if (text === "\n") {

         // Get some needed variables
         var row = editor.selection.getCursor().row;
         var col = editor.selection.getCursor().column;

         var tab = session.getTabString();

         var cursor = editor.getCursorPosition();
         var line = session.doc.getLine(cursor.row);

         if (this.codeModel.inMacro(session.getDocument().$lines, row - 1)) {
            return;
         }

         // If this line is an roxygen-style comment, continue that comment
         var match = /^(\s*\/\/'\s*)/.exec(line);
         if (match && col >= match[1].length)
         {
            return {
               text: "\n" + match[1]
            };
         }

         // If we're inserting a newline within a newly constructed comment
         // block, insert a '*'.
         if (/^\s*\/\*/.test(line))
         {
            // Double-check that we haven't already closed this comment,
            // or that this comment is continued on the next line.
            if (line.indexOf("*/") !== -1 || !/^\s*\*/.test(session.getLine(row + 1)))
            {
               var indent = this.$getIndent(line);
               var newIndent = indent + " * ";

               return {
                  text: "\n" + newIndent + "\n" + indent + " */",
                  selection: [1, newIndent.length, 1, newIndent.length]
               };
            }
         }

         // Comment indentation rules
         if (Utils.endsWith(state, "comment") ||
             Utils.endsWith(state, "doc-start"))
         {
            // Choose indentation for the current line based on the position
            // of the cursor -- but make sure we only apply this if the
            // cursor is on the same row as the line being indented
            if (cursor && cursor.row == row) {
               line = line.substring(0, cursor.column);
            }

            // If this is a comment start block, then insert appropriate indentation.
            var startMatch = /^(\s*)(\/\*)/.exec(line);
            if (startMatch)
            {
               return {
                  text: '\n' + startMatch[1] + " * "
               };
            }

            // We want to insert stars and spaces to match the indentation of the line.
            // Make sure we trim up to the cursor when necessary.
            var styleMatch = /^(\s*\*+\s*)/.exec(line);
            if (styleMatch) {
               return {
                  text: '\n' + styleMatch[1],
                  selection: [1, styleMatch[1].length, 1, styleMatch[1].length]
               };
            }
            
         }

         // Walk backwards over whitespace to find first non-whitespace char
         var i = col - 1;
         while (/\s/.test(line[i])) {
            --i;
         }
         var thisChar = line[i];
         var rightChar = line[col];

         // If we're creating a namespace, just use the line's indent itself
         var match = line.match(/\s*namespace\s*\w*\s*{/);
         if (match) {
            var indent = this.$getIndent(line);
            return {
               text: '\n' + indent,
               selection: [1, indent.length, 1, indent.length]
            };
         }

         // If we're handling the case where we want all function arguments
         // for a function call all on their own line, e.g.
         //
         // foo(
         //   |
         // )
         //
         // then indent appropriately, and put the closing paren on its
         // own line as well.
         if ((thisChar == "(" && rightChar == ")") ||
             (thisChar == "[" && rightChar == "]")) {

            var nextIndent = this.$getIndent(line);
            var indent = nextIndent + tab;
            
            return {
               text: "\n" + indent + "\n" + nextIndent,
               selection: [1, indent.length, 1, indent.length]
            };
         }

         // These insertion rules handle the case where we're inserting a newline
         // when within an auto-generated {} block; e.g. as class Foo {|};
         if (thisChar == '{' && rightChar == "}") {

            // If this line starts with an open brace, match that brace's indentation
            if (/^\s*{/.test(line)) {

               var nextIndent = this.$getIndent(line);
               var indent = nextIndent + session.getTabString();
               
               return {
                  text: "\n" + indent + "\n" + nextIndent,
                  selection: [1, indent.length, 1, indent.length]
               };
            }

            // Use heuristic indentation if possible
            var heuristicRow = codeModel.getRowForOpenBraceIndent(
               session, row
            );

            if (heuristicRow !== null && heuristicRow >= 0) {

               var nextIndent =
                      this.$getIndent(session.getDocument().getLine(heuristicRow));
               
               var indent = nextIndent + session.getTabString();
               
               return {
                  text: "\n" + indent + "\n" + nextIndent,
                  selection: [1, indent.length, 1, indent.length]
               };
               
            }

            // default behavior -- based on just the current row
            var nextIndent = this.$getIndent(line);
            var indent = nextIndent + tab;
            
            return {
               text: "\n" + indent + "\n" + nextIndent,
               selection: [1, indent.length, 1, indent.length]
            };
            
         }

      }
      
   });

   this.add("braces", "insertion", function (state, action, editor, session, text) {

      // Specialized insertion rules -- we infer whether a closing ';'
      // is appropriate, and we also provide comments for closing namespaces
      // (if desired)

      if (!this.insertMatching) return;
      
      if (text == '{') {

         // Ensure these rules are only run if there is no selection
         var selection = editor.getSelectionRange();
         var selected = session.doc.getTextRange(selection);
         if (selected === "") {

            // Get a token cursor, and place it at the cursor position.
            var cursor = this.codeModel.getTokenCursor();

            if (!cursor.moveToPosition(editor.getCursorPosition()))
               return autoPairInsertion("{", text, editor, session);

            do
            {
               // In case we're walking over a template class, e.g. for something like:
               //
               //    class Foo : public A<T>, public B<T>
               //
               // then we want to move over those matching arrows,
               // as their contents is non-informative for semi-colon insertion inference.
               if (cursor.bwdToMatchingArrow())
                  continue;

               var value = cursor.currentValue();
               if (!value || !value.length) break;

               // If we encounter a 'namespace' token, just insert a
               // single opening bracket. This is because we might be
               // enclosing some other namespaces following (and so the
               // automatic closing brace may be undesired)
               if (value === "namespace")
               {
                  return {
                     text: "{",
                     selection: [1, 1]
                  };
               }

               // If we encounter a 'class' or 'struct' token, this implies
               // we're defining a class -- add a semi-colon.
               //
               // We also do this for '=' operators, for C++11-style
               // braced initialization:
               //
               //    int foo = {1, 2, 3};
               //
               // TODO: Figure out if we can infer the same for braced initialization with
               // no equals; e.g.
               //
               //    MyClass object{1, 2, 3};
               //
               if (value === "class" ||
                   value === "struct" ||
                   value === "=")
               {
                  return {
                     text: "{};",
                     selection: [1, 1]
                  };
               }

               // Fill in the '{} while ()' bits for a do-while loop.
               if ($fillinDoWhile && value === "do")
               {
                  return {
                     text: "{} while ();",
                     selection: [1, 1]
                  };
               }

               // If, while walking backwards, we encounter certain tokens that
               // tell us we do not want semi-colon insertion, then stop there and return.
               if (value === ";" ||
                   value === "[" ||
                   value === "]" ||
                   value === "(" ||
                   value === ")" ||
                   value === "{" ||
                   value === "}" ||
                   value === "if" ||
                   value === "else" ||
                   value[0] === '#')
               {
                  return {
                     text: "{}",
                     selection: [1, 1]
                  };
               }
            } while (cursor.moveToPreviousToken());
         }

      }

      return autoPairInsertion("{", text, editor, session);

   });

   this.add("braces", "deletion", function (state, action, editor, session, range) {

      if (!this.insertMatching) return;
      
      var selected = session.doc.getTextRange(range);
      if (!range.isMultiLine() && selected == '{') {
         
         var line = session.doc.getLine(range.start.row);

         // Undo an auto-inserted do-while
         if (/^\s*do\s*\{\} while \(\);\s*$/.test(line)) {
            range.end.column = line.length;
            return range;
         }

         var rightChar = line.substring(range.end.column, range.end.column + 1);
         var rightRightChar =
                line.substring(range.end.column + 1, range.end.column + 2);
         if (rightChar == '}') {
            range.end.column++;
            if (rightRightChar == ';') {
               range.end.column++;
            }
            return range;
         }
      }
   });

   this.add("parens", "insertion", function (state, action, editor, session, text) {
      if (!this.insertMatching) return;
      return autoPairInsertion("(", text, editor, session);
   });

   this.add("parens", "deletion", function (state, action, editor, session, range) {
      if (!this.insertMatching) return;
      return autoPairDeletion("(", range, session);
   });
   
   this.add("brackets", "insertion", function (state, action, editor, session, text) {
      if (!this.insertMatching) return;
      return autoPairInsertion("[", text, editor, session);
   });

   this.add("brackets", "deletion", function (state, action, edditor, session, range) {
      if (!this.insertMatching) return;
      return autoPairDeletion("[", range, session);
   });

   this.add("arrows", "insertion", function (state, action, editor, session, text) {
      if (!this.insertMatching) return;
      var line = session.getLine(editor.getCursorPosition().row);
      if (!/^\s*#\s*include/.test(line)) return;
      return autoPairInsertion("<", text, editor, session);
   });

   this.add("arrows", "deletion", function (state, action, edditor, session, range) {
      if (!this.insertMatching) return;
      return autoPairDeletion("<", range, session);
   });

   this.add("string_dquotes", "insertion", function (state, action, editor, session, text) {
      if (!this.insertMatching) return;
      if (text == '"' || text == "'") {
         var quote = text;
         var selection = editor.getSelectionRange();
         var selected = session.doc.getTextRange(selection);
         if (selected !== "") {
            return {
               text: quote + selected + quote,
               selection: false
            };
         } else {
            var cursor = editor.getCursorPosition();
            var line = session.doc.getLine(cursor.row);
            var leftChar = line.substring(cursor.column-1, cursor.column);

            // We're escaped.
            if (leftChar == '\\') {
               return null;
            }

            // Find what token we're inside.
            var tokens = session.getTokens(selection.start.row);
            var col = 0, token;
            var quotepos = -1; // Track whether we're inside an open quote.

            for (var x = 0; x < tokens.length; x++) {
               token = tokens[x];
               if (token.type == "string") {
                  quotepos = -1;
               } else if (quotepos < 0) {
                  quotepos = token.value.indexOf(quote);
               }
               if ((token.value.length + col) > selection.start.column) {
                  break;
               }
               col += tokens[x].value.length;
            }

            // Try and be smart about when we auto insert.
            if (!token || (quotepos < 0 && token.type !== "comment" && (token.type !== "string" || ((selection.start.column !== token.value.length+col-1) && token.value.lastIndexOf(quote) === token.value.length-1)))) {
               return {
                  text: quote + quote,
                  selection: [1,1]
               };
            } else if (token && token.type === "string") {
               // Ignore input and move right one if we're typing over the closing quote.
               var rightChar = line.substring(cursor.column, cursor.column + 1);
               if (rightChar == quote) {
                  return {
                     text: '',
                     selection: [1, 1]
                  };
               }
            }
         }
      }
   });

   this.add("string_dquotes", "deletion", function (state, action, editor, session, range) {
      if (!this.insertMatching) return;
      var selected = session.doc.getTextRange(range);
      if (!range.isMultiLine() && (selected == '"' || selected == "'")) {
         var line = session.doc.getLine(range.start.row);
         var rightChar = line.substring(range.start.column + 1, range.start.column + 2);
         if (rightChar === '"' || rightChar === "'") {
            range.end.column++;
            return range;
         }
      }
   });

   this.add("punctuation.operator", "insertion", function(state, action, editor, session, text) {
      
      if (!this.insertMatching) return;
      // Step over ';'
      // TODO: only insert semi-colon if text following cursor is just
      // semi-colon + whitespace
      if (text === ";") {
         var cursor = editor.selection.getCursor();
         var line = session.getLine(cursor.row);
         if (line[cursor.column] == ";") {
            return {
               text: '',
               selection: [1, 1]
            };
         }

      }

   });

   // Provide an experimental 'macro mode' -- this allows for automatic indentation
   // and alignment of inserted '/' characters, and also provides the regular
   // indentation rules for expressions constructed within a macro.
   this.add("macro", "insertion", function(state, action, editor, session, text) {

      var margin = editor.getPrintMarginColumn();
      var backslashAlignColumn = Math.min(62, margin);

      // Get some useful quantities
      var lines = session.getDocument().$lines;
      var cursor = editor.getCursorPosition();
      var row = cursor.row;
      var line = lines[row];
      var lineSub = line.substring(0, cursor.column);

      // Enter macro mode: we enter macro mode if the user inserts a
      // '\' after a '#define' line.
      if (/^\s*#\s*define[^\\]*$/.test(line) && text == "\\") {

         var len = backslashAlignColumn - lineSub.length + 1;

         if (len >= 0) {
            return {
               text: new Array(len + 1).join(" ") + "\\\n" + this.$getIndent(line) + session.getTabString(),
               selection: false
            };
         } else {
            return {
               text: "\\\n" + session.getTabString(),
               selection: false
            };
         }
      }

      // Special rules for 'macro mode'.
      if (/^\s*#\s*define/.test(line) || this.codeModel.inMacro(lines, row - 1)) {

         // Handle insertion of a '\'.
         //
         // If there is only whitespace following the cursor, then
         // we try to nudge out the inserted '\'. Note that we
         // have some protection in this outdenting because of the
         // automatic matching done by '', "" insertion (which is the
         // only other context where we would expect a user to insert '\')
         if (text == "\\" &&
             (/^\s*$/.test(line.substring(lineSub.length, line.length)))) {
                
            var len = backslashAlignColumn - lineSub.length + 1;

            if (len >= 0) {
               return {
                  text: new Array(len + 1).join(" ") + "\\",
                  selection: false
               };
            } else {
               return {
                  text: "\\",
                  selection: false
               };
            }
         }

         // Newlines function slightly differently in 'macro mode'.
         // When a newline is inserted, we automatically add in an aligned
         // '\' for continuation if the line isn't blank.
         // If we try to insert a newline on a line that already has a
         // closing '\', then we just move the cursor down.
         if (text == "\n") {

            // Leave the macro if the line is blank. This provides an
            // escape hatch for '\n'.
            if (/^\s*$/.test(line)) {
               return {
                  text: "\n",
                  selection: false
               };
            }

            // Don't enter macro mode if the line is just a #define (with
            // no trailing \)
            if (/^\s*#\s*define/.test(line) && !/\\\s*$/.test(line)) {
               return {
                  text: '\n',
                  selection: false
               };
            }

            // Check if we already have a closing backslash to the right of the cursor.
            // This rule makes enter effectively function as a 'move down' action, e.g.
            // pressing the down arrow on the keyboard.
            if (/\\\s*$/.test(line) && !/\\\s*$/.test(lineSub)) {
               return {
                  text: '',
                  selection: [1, cursor.column, 1, cursor.column]
               };
            }

            // Otherwise, on enter, push a '\' out to an alignment column, so that
            // macros get formatted in a 'pretty' way.
            var nextIndent = session.getMode().getNextLineIndent(
               state,
               line + "\\", // added so the indentation mode believes we're still in a macro
               session.getTabString(),
               row,
               false
            );
            
            var len = backslashAlignColumn - lineSub.length + 1;
            var backSlash = /\\\s*$/.test(lineSub) ?
                   "" :
                   "\\";

            if (len >= 0) {
               return {
                  text: new Array(len + 1).join(" ") + backSlash + "\n" + nextIndent,
                  selection: false
               };
            } else {
               return {
                  text: backSlash + "\n" + nextIndent,
                  selection: false
               };
            }
         }
      }
      
   });

};

oop.inherits(CStyleBehaviour, Behaviour);

exports.CStyleBehaviour = CStyleBehaviour;

exports.setFillinDoWhile = function(x) {
   $fillinDoWhile = x;
};

exports.getFillinDoWhile = function() {
   return $fillinDoWhile;
}

});
