/*
 * cpp_code_model.js
 *
 * Copyright (C) 2020 by RStudio, PBC
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

define("mode/cpp_code_model", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var Range = require("ace/range").Range;

var TokenUtils = require("mode/token_utils").TokenUtils;
var TokenIterator = require("ace/token_iterator").TokenIterator;
var CppTokenCursor = require("mode/token_cursor").CppTokenCursor;

var CppScopeNode = require("mode/cpp_scope_tree").CppScopeNode;
var CppScopeManager = require("mode/cpp_scope_tree").CppScopeManager;

var getVerticallyAlignFunctionArgs = require("mode/r_code_model").getVerticallyAlignFunctionArgs;
var Utils = require("mode/utils");

var CppCodeModel = function(session, tokenizer,
                            statePattern, codeBeginPattern, codeEndPattern) {

   this.$session = session;
   this.$doc = session.getDocument();
   this.$tokenizer = tokenizer;

   this.$tokens = new Array(this.$doc.getLength());
   this.$statePattern = statePattern;
   this.$codeBeginPattern = codeBeginPattern;
   this.$codeEndPattern = codeEndPattern;

   this.$tokenUtils = new TokenUtils(
      this.$doc,
      this.$tokenizer,
      this.$tokens,
      this.$statePattern,
      this.$codeBeginPattern
   );

   this.$scopes = new CppScopeManager(CppScopeNode);

   var $firstChange = true;
   var onChangeMode = function(data, session)
   {
      if ($firstChange)
      {
         $firstChange = false;
         return;
      }

      this.$doc.off('change', onDocChange);
      this.$session.off('changeMode', onChangeMode);
   }.bind(this);

   var onDocChange = function(evt)
   {
      this.$onDocChange(evt);
   }.bind(this);

   this.$session.on('changeMode', onChangeMode);
   this.$doc.on('change', onDocChange);

   var that = this;
   
};

(function() {

   var contains = Utils.contains;

   this.getTokenCursor = function() {
      return new CppTokenCursor(this.$tokens, 0, 0, this);
   };

   this.$tokenizeUpToRow = function(row) {
      this.$tokenUtils.$tokenizeUpToRow(row);
   };

   var $walkBackForScope = function(cursor, that) {
      
      while (true) {
         
         var value = cursor.currentValue();
         var line = that.$doc.getLine(cursor.$row);

         // Bail on some specific tokens not found in
         // function type specifiers
         if (contains(["{", "}", ";"], value))
            break;

         // Bail on 'public:' etc.
         if (value === ":") {
            var prevValue = cursor.peekBwd().currentValue();
            if (contains(["public", "private", "protected"], prevValue))
               break;
         }

         // Bail on lines intended for the preprocessor
         if (/^\s*#/.test(line))
            break;

         if (!cursor.moveToPreviousToken())
            break;
         
      }
      
   };

   var debugCursor = function(message, cursor) {
      // console.log(message);
      // console.log(cursor);
      // console.log(cursor.currentToken());
   };

   var controlFlowKeywords = [
      "if", "else", "for", "do", "while", "struct", "class", "try",
      "catch", "switch"
   ];

   var $normalizeWhitespace = function(text) {
      text = text.trim();
      text = text.replace(/[\n\s]+/g, " ");
      return text;
   };

   var $truncate = function(text, width) {
      
      if (typeof width === "undefined")
         width = 80;
      
      if (text.length > width)
         text = text.substring(0, width) + "...";
      
      return text;
   };

   var $normalizeAndTruncate = function(text, width) {
      return $truncate($normalizeWhitespace(text), width);
   };

   this.$complements = {
      "<" : ">",
      ">" : "<",
      "{" : "}",
      "}" : "{",
      "[" : "]",
      "]" : "[",
      "(" : ")",
      ")" : "(",
      "'" : "'",
      '"' : '"'
   };

   // Align continuation slashses (for e.g. macros)
   this.alignContinuationSlashes = function(doc, range) {

      if (typeof range === "undefined") {
         range = {
            start: 0,
            end: doc.getLength()
         };
      }

      var lines = doc.$lines;
      if (!(lines instanceof Array)) {
         return false;
      }

      var n = lines.length;
      for (var i = range.start; i < range.end; i++) {
         if (reEndsWithBackslash.test(lines[i])) {
            var start = i;
            var j = i + 1;
            while (reEndsWithBackslash.test(lines[j]) && j <= range.end) {
               j++;
            }
            var end = j;

            var indices = lines.slice(start, end).map(function(x) {
               return x.lastIndexOf("\\");
            });

            var maxIndex = Math.max.apply(null, indices);

            for (var idx = 0; idx < end - start; idx++) {

               var pos = {
                  row: start + idx,
                  column: indices[idx]
               };

               var whitespace = new Array(maxIndex - indices[idx] + 1).join(" ");
               doc.insert(pos, whitespace);
            }
            
            i = j;
         }
      }

      return true;
      
   };

   this.allIndicesOf = function(string, character) {
      var result = [];
      for (var i = 0; i < string.length; i++) {
         if (string[i] == character) {
            result.push(i);
         }
      }
      return result;
   };

   // Heuristic for finding a matching '>'.
   //
   // We attempt to find matches for '<' and '>' where:
   //
   // 1. '>' occurs at the beginning of the line, and
   // 2. '<' occurs at the end of the line.
   //
   // Primarily intended for template contexts, e.g.
   //
   // template <                     <-- returns this row
   //     int RTYPE
   // >
   //
   // ^                              <-- want to align with that line
   this.getRowForMatchingEOLArrows = function(session, doc, row) {
      var maxLookback = 100;
      var balance = 0;
      var thisLine = "";
      for (var i = 1; i < maxLookback; i++) {
         thisLine = this.getLineSansComments(doc, row - i);

         // Small escape hatch -- break if we encounter a line ending with
         // a semi-colon since that should never happen in template contexts
         if (/;\s*$/.test(thisLine))
            break;
         
         if (/<\s*$/.test(thisLine) && !/<<\s*$/.test(thisLine)) {
            if (balance === 0) {
               return row - i;
            } else {
               balance--;
            }
         } else if (/^\s*>/.test(thisLine) && !/^\s*>>/.test(thisLine)) {
            balance++;
         }
      }
      
      return -1;
      
   };

   var reStartsWithDefine = /^\s*#\s*define/;
   var reEndsWithBackslash = /\\\s*$/;

   // NOTE: We need to be careful of comment block starts and ends. (/*, */)
   var reStartsWithContinuationToken = /^\s*[+\-/&^%$!<\>.?|=~]|^\s*\*[^/]|^\s*\/[^\*]/;
   var reEndsWithContinuationToken =       /[+\-*&^%$!<\>.?|=~]\s*$|\*[^/]\s*$|\/[^\*]\s*$/;

   var reContinuation = function(x) {
      return reStartsWithContinuationToken.test(x) ||
         reEndsWithContinuationToken.test(x);
   };

   var endsWithCommaOrOpenParen = function(x) {
      return /[,(]\s*$/.test(x);
   };

   var charCount = function(string, character) {
      return string.split(character).length - 1;
   };
   
   // Identify whether we're currently writing a macro -- either the current
   // line starts with a '#define' statement, or a chain of lines ending with
   // '\' leads back to a line starting with a '#define' statement.
   this.inMacro = function(lines, row) {

      var line = lines[row];

      if (row < 0) {
         return false;
      } else if (reEndsWithBackslash.test(line)) {
         if (reStartsWithDefine.test(line)) {
            return true;
         } else {
            return this.inMacro(lines, row - 1);
         }
      } else {
         return false;
      }
   };

   this.$buildScopeTreeUpToRow = function(maxrow) {

      function maybeEvaluateLiteralString(value) {
         // NOTE: We could evaluate escape sequences and whatnot here as well.
         //       Hard to imagine who would abuse Rnw by putting escape
         //       sequences in chunk labels, though.
         var match = /^(['"])(.*)\1$/.exec(value);
         if (!match)
            return value;
         else
            return match[2];
      }

      var maxRow = Math.min(maxrow + 30, this.$doc.getLength() - 1);
      this.$tokenUtils.$tokenizeUpToRow(maxRow);

      var tokenCursor = this.getTokenCursor();
      if (!tokenCursor.seekToNearestToken(this.$scopes.parsePos, maxRow))
         return;

      do
      {
         this.$scopes.parsePos = tokenCursor.currentPosition();
         this.$scopes.parsePos.column += tokenCursor.currentValue().length;

         //console.log("                                 Token: " + tokenCursor.currentValue() + " [" + tokenCursor.currentPosition().row + "x" + tokenCursor.currentPosition().column + "]");

         var tokenType = tokenCursor.currentToken().type;
         if (/\bsectionhead\b/.test(tokenType))
         {
            var sectionHeadMatch = /^\/\/'?[-=#\s]*(.*?)\s*[-=#]+\s*$/.exec(
                  tokenCursor.currentValue());

            if (!sectionHeadMatch)
               continue;

            var label = "" + sectionHeadMatch[1];
            if (label.length == 0)
               label = "(Untitled)";
            if (label.length > 50)
               label = label.substring(0, 50) + "...";

            this.$scopes.onSectionStart(label, tokenCursor.currentPosition());
         }
         
         else if (/\bcodebegin\b/.test(tokenType))
         {
            var chunkStartPos = tokenCursor.currentPosition();
            var chunkPos = {row: chunkStartPos.row + 1, column: 0};
            var chunkNum = this.$scopes.getTopLevelScopeCount()+1;
            var chunkLabel = "(R Code Chunk)";
            this.$scopes.onChunkStart(chunkLabel,
                                      chunkLabel,
                                      chunkStartPos,
                                      chunkPos);
         }
         else if (/\bcodeend\b/.test(tokenType))
         {
            var pos = tokenCursor.currentPosition();
            // Close any open functions
            while (this.$scopes.onScopeEnd(pos))
            {
            }

            pos.column += tokenCursor.currentValue().length;
            this.$scopes.onChunkEnd(pos);
         }
         else if (tokenCursor.currentValue() === "{")
         {
            // We need to determine if this open brace is associated with an
            // 1. namespace,
            // 2. class (struct),
            // 3. function,
            // 4. lambda,
            // 5. anonymous / other
            var localCursor = tokenCursor.cloneCursor();
            var startPos = localCursor.currentPosition();
            if (localCursor.isFirstSignificantTokenOnLine())
               startPos.column = 0;

            // namespace
            if (localCursor.peekBwd(2).currentValue() === "namespace") {

               // named namespace
               localCursor.moveToPreviousToken();
               var namespaceName = localCursor.currentValue();
               this.$scopes.onNamespaceScopeStart("namespace " + namespaceName,
                                                  localCursor.currentPosition(),
                                                  tokenCursor.currentPosition(),
                                                  namespaceName);
               
            }

            // anonymous namespace
            else if (localCursor.peekBwd().currentValue() === "namespace") {
               this.$scopes.onNamespaceScopeStart("anonymous namespace",
                                                  startPos,
                                                  tokenCursor.currentPosition(),
                                                  "<anonymous>");
            }

            // class (struct)
            else if (localCursor.peekBwd(2).currentValue() === "class" ||
                     localCursor.peekBwd(2).currentValue() === "struct" ||
                     localCursor.bwdOverClassInheritance()) {

               localCursor.moveToPreviousToken();
               
               // Clone the cursor and look back to get
               // the return type. Do this by walking
               // backwards until we hit a ';', '{',
               // '}'.
               var classCursor = localCursor.cloneCursor();
               $walkBackForScope(classCursor, this);

               var classStartPos = classCursor.peekFwd().currentPosition();
               var classText = this.$session.getTextRange(new Range(
                  classStartPos.row, classStartPos.column,
                  startPos.row, startPos.column
               ));
               
               classText = $normalizeWhitespace(classText);
               
               this.$scopes.onClassScopeStart(classText,
                                              localCursor.currentPosition(),
                                              tokenCursor.currentPosition(),
                                              classText);
            }

            // function and lambdas
            else if (
               localCursor.bwdOverConstNoexceptDecltype() &&
               (localCursor.bwdOverInitializationList() &&
                localCursor.moveBackwardOverMatchingParens()) ||
                  localCursor.moveBackwardOverMatchingParens()) {

               if (localCursor.peekBwd().currentType() === "identifier" ||
                   localCursor.peekBwd().currentValue() === "]" ||
                   /^operator/.test(localCursor.peekBwd().currentValue())) {
                  
                  var valueBeforeParen = localCursor.peekBwd().currentValue();
                  if (valueBeforeParen === "]") {

                     var lambdaStartPos = localCursor.currentPosition();
                     var lambdaText = this.$session.getTextRange(new Range(
                        lambdaStartPos.row, lambdaStartPos.column,
                        startPos.row, startPos.column - 1
                     ));

                     lambdaText = $normalizeWhitespace("lambda " + lambdaText);

                     // TODO: Extract lambda arguments.
                     this.$scopes.onLambdaScopeStart(lambdaText,
                                                     startPos,
                                                     tokenCursor.currentPosition());
                     
                  } else {

                     if (localCursor.moveToPreviousToken()) {

                        var fnType = "";
                        var fnName = localCursor.currentValue();
                        var fnArgs = "";

                        var enclosingScopes = this.$scopes.getActiveScopes(
                           localCursor.currentPosition());
                        
                        if (enclosingScopes != null) {
                           var parentScope = enclosingScopes[enclosingScopes.length - 1];
                           if (parentScope.isClass() &&
                               parentScope.label === "class " + fnName) {
                              if (localCursor.peekBwd().currentValue() === "~") {
                                 fnName = "~" + fnName;
                              }
                           } else {
                              // Clone the cursor and look back to get
                              // the return type. Do this by walking
                              // backwards until we hit a ';', '{',
                              // '}'.
                              var fnTypeCursor = localCursor.cloneCursor();
                              $walkBackForScope(fnTypeCursor, this);
                              
                              // Move back up one token
                              fnTypeCursor.moveToNextToken();

                              // Get the type from the text range
                              var fnTypeStartPos = fnTypeCursor.currentPosition();
                              var fnTypeEndPos = localCursor.currentPosition();
                              fnType = this.$session.getTextRange(new Range(
                                 fnTypeStartPos.row, fnTypeStartPos.column,
                                 fnTypeEndPos.row, fnTypeEndPos.column
                              ));
                           }
                           
                        }

                        // Get the position of the opening paren
                        var fnArgsCursor = localCursor.peekFwd();
                        var fnArgsStartPos = fnArgsCursor.currentPosition();

                        if (fnArgsCursor.fwdToMatchingToken()) {

                           // Move over 'const'
                           if (fnArgsCursor.peekFwd().currentValue() === "const")
                              fnArgsCursor.moveToNextToken();

                           // Move over 'noexcept'
                           if (fnArgsCursor.peekFwd().currentValue() === "noexcept")
                              fnArgsCursor.moveToNextToken();

                           // Move over parens
                           if (fnArgsCursor.currentValue() === "noexcept" &&
                               fnArgsCursor.peekFwd().currentValue() === "(") {
                              fnArgsCursor.moveToNextToken();
                              fnArgsCursor.fwdToMatchingToken();
                           }

                           var fnArgsEndPos = fnArgsCursor.peekFwd().currentPosition();
                           if (fnArgsEndPos)
                              fnArgs = this.$session.getTextRange(new Range(
                                 fnArgsStartPos.row, fnArgsStartPos.column,
                                 fnArgsEndPos.row, fnArgsEndPos.column
                              ));
                           
                        }
                        
                        var fullFnName;
                        if (fnType.length > 0)
                           fullFnName = $normalizeAndTruncate(
                              fnName.trim() + fnArgs.trim() + ": " + fnType.trim());
                        else
                           fullFnName = $normalizeAndTruncate(
                              fnName.trim() + fnArgs.trim());
                        
                        this.$scopes.onFunctionScopeStart(
                           fullFnName,
                           localCursor.currentPosition(),
                           tokenCursor.currentPosition(),
                           fnName.trim(),
                           fnArgs.split(",")
                        );
                     }
                  }
               }

               // It's possible that we were on something that 'looked' like a function call,
               // but wasn't actually (e.g. `while () { ... }`) -- handle these cases
               else {
                  this.$scopes.onScopeStart(startPos);
               }
            }
            // other (unknown)
            else {
               this.$scopes.onScopeStart(startPos);
            }
            
         }
         else if (tokenCursor.currentValue() === "}")
         {
            var pos = tokenCursor.currentPosition();
            if (tokenCursor.isLastSignificantTokenOnLine())
            {
               pos.column = this.$doc.getLine(pos.row).length + 1;
            }
            else
            {
               pos.column++;
            }
            this.$scopes.onScopeEnd(pos);
         }
      } while (tokenCursor.moveToNextToken(maxRow));
      
   };

   this.getCurrentScope = function(position, filter)
   {
      if (!filter)
         filter = function(scope) { return true; };

      if (!position)
         return "";
      this.$buildScopeTreeUpToRow(position.row);

      var scopePath = this.$scopes.getActiveScopes(position);
      if (scopePath)
      {
         for (var i = scopePath.length-1; i >= 0; i--) {
            if (filter(scopePath[i]))
               return scopePath[i];
         }
      }

      return null;
   };

   this.getScopeTree = function()
   {
      this.$buildScopeTreeUpToRow(this.$doc.getLength() - 1);
      return this.$scopes.getScopeList();
   };
   

   // Given a row with a '{', we look back for the row that provides
   // the start of the scope, for purposes of indentation. We look back
   // for:
   //
   // 1. A class token, or
   // 2. A constructor with an initializer list.
   //
   // Return 'null' if no row could be found, and the corresponding row
   // otherwise.
   this.getRowForOpenBraceIndent = function(session, row, useCursor) {

      var doc = session.getDocument();
      var lines = doc.$lines;
      if (lines.length <= 1) return -1;

      var line = lines[row];

      // Walk tokens backwards until we find something that provides
      // the appropriate indentation.
      if (this.$tokenUtils.$tokenizeUpToRow(row)) {

         try {

            // Remove any trailing '\' tokens, then reapply them. This way, indentation
            // will work even in 'macro mode'.
            var tokens = new Array(this.$tokens.length);

            for (var i = 0; i < this.$tokens.length; i++) {
               if (this.$tokens[i] != null &&
                   this.$tokens[i].length > 0) {
                  var rowTokens = this.$tokens[i];
                  if (rowTokens[rowTokens.length - 1].value === "\\") {
                     tokens[i] = this.$tokens[i].splice(rowTokens.length - 1, 1)[0];
                  }
               } 
            }

            var tokenCursor = this.getTokenCursor();
            if (useCursor) {
               var cursor = session.getSelection().getCursor();
               if (!tokenCursor.moveToPosition(cursor))
                  return 0;
            } else {
               tokenCursor.$row = row;

               var i = tokenCursor.$tokens[row].length - 1;
               for (var i = tokenCursor.$tokens[row].length - 1;
                    i >= 0;
                    i--)
               {
                  tokenCursor.$offset = i;
                  if (tokenCursor.currentValue() === "{") {
                     break;
                  }
               }
            }

            if (tokenCursor.peekBwd().currentValue() === "{" ||
                tokenCursor.currentValue() === ";") {
               return -1;
            }
            
            // Move backwards over matching parens. Note that we may need to walk up
            // e.g. a constructor's initialization list, so we need to check for
            //
            //     , a_(a)
            //
            // so we need to look two tokens backwards to see if it's a
            // comma or a colon.
            debugCursor("Before moving over initialization list", tokenCursor);
            tokenCursor.bwdOverInitializationList();

            debugCursor("Before moving over class inheritance", tokenCursor);
            tokenCursor.bwdOverClassInheritance();

            // If we didn't walk over anything previously, the cursor
            // will still be on the same '{'.  Walk backwards one token.
            if (tokenCursor.currentValue() === "{") {
               if (!tokenCursor.moveToPreviousToken()) {
                  return -1;
               }
            }

            // Bail if we encountered a '{'
            if (tokenCursor.currentValue() === "{") {
               return -1;
            }

            // Move backwards over any keywords.
            debugCursor("Before walking over keywords", tokenCursor);
            while (tokenCursor.currentType() === "keyword") {

               // Return on 'control flow' keywords.
               var value = tokenCursor.currentValue();
               
               if (contains(controlFlowKeywords, value))
                  return tokenCursor.$row;

               if (tokenCursor.$row === 0 && tokenCursor.$offset === 0)
                  return tokenCursor.$row;
               
               if (!tokenCursor.moveToPreviousToken())
                  return -1;
            }

            // Move backwards over matching parens.
            debugCursor("Before walking over matching parens", tokenCursor);

            // If we landed on a ':' token and the previous token is
            // e.g. public, then we went too far -- go back up one token.
            if (tokenCursor.currentValue() === ":") {

               var prevValue = tokenCursor.peekBwd().currentValue();
               if (contains(["public", "private", "protected"], prevValue))
               {
                  tokenCursor.moveToNextToken();
                  return tokenCursor.$row;
               }
            }

            if (tokenCursor.currentValue() === ":") {

               // We want to walk over specifiers preceeding the ':' which may
               // specify an initializer list. We need to walk e.g.
               //
               //    const foo) const noexcept(bar) :
               //
               // so we do this by jumping parens and keywords, stopping once
               // we hit an actual identifier.
               while (tokenCursor.moveToPreviousToken()) {

                  if (tokenCursor.bwdToMatchingToken()) {

                     if (tokenCursor.peekBwd().currentType() === "keyword") {
                        continue;
                     } else {
                        break;
                     }
                  }

                  if (tokenCursor.currentType() === "identifier")
                     break;
               }

            }

            if (tokenCursor.currentValue() === ")") {
               if (!tokenCursor.bwdToMatchingToken()) {
                  return -1;
               }
            }

            if (tokenCursor.currentValue() === "(") {
               if (!tokenCursor.moveToPreviousToken()) {
                  return -1;
               }
            }

            // Use this row for indentation.
            debugCursor("Ended at", tokenCursor);
            if (tokenCursor.currentValue() === "=") {
               if (tokenCursor.moveToPreviousToken()) {
                  return tokenCursor.$row;
               }
            }

            return tokenCursor.$row;
            
         } finally {

            for (var i = 0; i < tokens.length; i++) {
               if (typeof tokens[i] !== "undefined") {
                  this.$tokens[i].push(tokens[i]);
               }
            }
            
         }

      }

      // Give up
      return -1;
      
   };

   var getRegexIndices = function(regex, line) {

      var match = null;
      var indices = [];
      while ((match = regex.exec(line))) {
         indices.push(match.index);
      }
      return indices;
   };

   // Get a line, with comments (following '//') stripped. Also strip
   // a trailing '\' anticipating e.g. macros.
   this.getLineSansComments = function(doc, row, stripConstAndNoexcept) {

      if (row < 0) {
         return "";
      }
      
      var line = doc.getLine(row);

      // Strip quotes before stripping comments -- this is to avoid
      // problems with e.g.
      //
      //   int foo("// comment");
      //
      // Note that we preserve the quotes themselves, e.g. post strip
      // the line would appear as:
      //
      //   int foo("");
      //
      // as this allows other heuristics to still work fine.
      var indices = getRegexIndices(/(?!\\)\"/g, line);

      if (indices.length > 0 && indices.length % 2 === 0) {

         for (var i = 0; i < indices.length / 2; i = i + 2) {

            var start = indices[i];
            var end = indices[i + 1];

            line = line.substring(0, start + 1) +
                   line.substring(end, line.length);
         }
      }

      // Strip out a trailing line comment
      var index = line.indexOf("//");
      if (index != -1) {
         line = line.substring(0, index);
      }

      // Strip off a trailing '\' -- this is mainly done
      // for macro mode (so we get regular indentation rules)
      if (reEndsWithBackslash.test(line)) {
         line = line.substring(0, line.lastIndexOf("\\"));
      }

      if (stripConstAndNoexcept) {
         line = line
            .replace(/\bconst\b/, "")
            .replace(/\bnoexcept\b/, "");
      }

      return line;
      
   };

   this.findStartOfCommentBlock = function(lines, row, maxLookback) {
      var count = 0;
      var reCommentBlockStart = /^\s*\/+\*/;
      while (row >= 0 && count < maxLookback) {
         var line = lines[row];
         if (reCommentBlockStart.test(line)) {
            return row;
         }
         --row;
         ++count;
      }
      return -1;
   };

   this.getNextLineIndent = function(state, line, tab, row, dontSubset) {

      // Ask the R code model if we want to use vertical alignment
      var $verticallyAlignFunctionArgs = getVerticallyAlignFunctionArgs();

      var session = this.$session;
      var tabSize = session.getTabSize();
      var doc = session.getDocument();

      if (typeof row !== "number")
         row = session.getSelection().getCursor().row - 1;

      // If we went back too far, use the first row for indentation.
      if (row === -1) {
         var lineZero = doc.getLine(0);
         if (lineZero.length > 0) {
            return this.$getIndent(lineZero);
         } else {
            return "";
         }
      }

      // If this line is intended for the preprocessor, it should be aligned
      // at the start. Use the previous line for indentation.
      if (line.length === 0 || /^\s*#/.test(line))
         return this.getNextLineIndent(
            Utils.getPrimaryState(session, row - 1),
            doc.getLine(row - 1),
            tab,
            row - 1,
            dontSubset
         );

      var indent = this.$getIndent(line);
      var unindent = this.$getUnindent(line, tabSize);
      
      var lines = doc.$lines;

      var prevLine;
      if (row > 0) {
         prevLine = lines[row - 1];
      } else {
         prevLine = "";
      }

      // Indentation rules for comments
      if (Utils.endsWith(state, "comment") ||
          Utils.endsWith(state, "doc-start"))
      {

         // Choose indentation for the current line based on the position
         // of the cursor -- but make sure we only apply this if the
         // cursor is on the same row as the line being indented
         if (cursor && cursor.row == row) {
            line = line.substring(0, cursor.column);
         }

         // Bail if line is just whitespace. This is necessary for when the
         // cursor is to the left of a comment block.
         if (/^\s*$/.test(line)) {
            return this.$getIndent(lines[row]);
         }
         
         // NOTE: It is the responsibility of c_style_behaviour to insert
         // a '*' and leading spaces on newline insertion! We just look
         // for the opening block and use indentation based on that. Otherwise,
         // reindent will replicate the leading comment stars.
         var commentStartRow = this.findStartOfCommentBlock(lines, row, 200);
         if (commentStartRow !== null) {
            return this.$getIndent(lines[commentStartRow]) + " ";
         }
         
      }

      // Rules for the 'general' state
      if (Utils.endsWith(state, "start")) {

         var match = null;

         /**
          * We start by checking some special-cases for indentation --
          * ie, simple cases wherein we can resolve the correct form of
          * indentation from just the first, or previous, line.
          */

         // Indent after a #define with continuation; but don't indent
         // without continutation
         if (reStartsWithDefine.test(line)) {
            if (/\\\s*$/.test(line)) {
               return indent + tab;
            }
            return indent;
         }

         // Don't indent after a preprocessor line
         if (/^\s*#\s*\S/.test(line)) {
            return indent;
         }

         // Unindent after leaving a #define with continuation
         if (this.inMacro(lines, row - 1) &&
             !reEndsWithBackslash.test(line)) {
            return unindent;
         }

         // Decisions made should not depend on trailing comments in the line
         // So, we strip those out for the purposes of indentation.
         //
         // Note that we strip _after_ the define steps so that we can
         // effectively leverage the indentation rules within macro settings.
         line = this.getLineSansComments(doc, row);

         var cursor = session.getSelection().getCursor();

         // Choose indentation for the current line based on the position
         // of the cursor -- but make sure we only apply this if the
         // cursor is on the same row as the line being indented.
         //
         // Note that callers can set 'dontSubset' to avoid this behaviour;
         // this is desired for e.g. the 'reindent' function (which should
         // not take the position of the cursor into account)
         if (cursor && cursor.row == row && !dontSubset) {
            line = line.substring(0, cursor.column);
         }
         
         prevLine = this.getLineSansComments(doc, row - 1);

         // If this line is just whitespace, match that line's indent. This
         // ensures multiple enter keypresses can blast the cursor off into
         // space.
         if (typeof line !== "string") {
            return "";
         } else if (line.length === 0 ||
                    /^\s*$/.test(line))
         {
            return this.$getIndent(lines[row]);
         }

         // Unindent after leaving a block comment.
         //
         // /**
         //  *
         //  */
         // ^
         if (/\*\/\s*$/.test(line)) {

            // Find the start of the comment block
            var blockStartRow = this.findStartOfCommentBlock(
               lines,
               row,
               200
            );
            
            if (blockStartRow >= 0) {
               return this.$getIndent(lines[blockStartRow]);
            }
         }

         // Special-case indentation for aligned streaming.
         // This handles indentation for the case of e.g.
         //
         //   std::cout << foo
         //             << bar
         //             << baz;
         //   ^
         //
         if (/^\s*<</.test(line)) {
            var currentRow = row - 1;
            while (/^\s*<</.test(lines[currentRow])) {
               currentRow--;
            }
            return this.$getIndent(lines[currentRow]);
         }

         // Do something similar for '.' alignment, for chained
         // function calls:
         //
         //   foo.bar()
         //      .baz()
         //      .bam();
         //
         if (/^\s*\./.test(line)) {
            var currentRow = row - 1;
            while (/^\s*\./.test(lines[currentRow])) {
               currentRow--;
            }
            return this.$getIndent(lines[currentRow]);
         }

         // Don't indent for namespaces, switch statements.
         if (/\bnamespace\b.*\{\s*$/.test(line) ||
             /\bswitch\b.*\{\s*$/.test(line)) {
            return indent;
         }

         // Indent following an opening paren.
         // We prefer inserting two tabs here, reflecting the rules of
         // the Google C++ style guide:
         // http://google-styleguide.googlecode.com/svn/trunk/cppguide.html#Function_Declarations_and_Definitions
         //
         // We take a slightly different approach -- indentation for
         // function declarations gets two indents, while indentation
         // for function calls gets a single indent.
         if (line.match(/\(\s*$/)) {

            // Check for a function call.
            if (line.indexOf("=") !== -1 || /^\s*(return\s+)?[a-zA-Z0-9_.->:]+\(\s*$/.test(line))
               return indent + tab;
            return indent + tab + tab;
         }

         // If we have a class with an open brace on the same line, indent
         //
         //   class Foo {
         //       ^
         //
         if (/^\s*(class|struct).*\{\s*$/.test(line)) {
            return indent + tab;
         }

         // If we have a line beginning a class definition ending with a colon, indent
         //
         //   class Foo :
         //       |
         //       ^
         //
         if (/^\s*(class|struct)\s+.+:\s*$/.test(line)) {
            return indent + tab;
         }

         // Match the indentation of the ':' in a statement e.g.
         //
         //   class Foo : public A
         //             ^
         //
         // Note the absence of a closing comma. This is for users
         // who would prefer to align commas with colons, when
         // doing multi-line inheritance.
         //
         // Need some special handling for e.g.
         //
         //   class Foo::Bar : public A
         //                  ^
         //
         var match = line.match(/(^\s*(?:class|struct)\s+.*\w[^:]):[^:]\s*.+/);
         if (match && !/,\s*/.test(line)) {
            return $verticallyAlignFunctionArgs ?
               new Array(match[1].length + 1).join(" ") :
               indent + tab;
         }

         // If we're looking at a class with the first inheritted member
         // on the same line, e.g.
         //
         //   class Foo : public A,
         //               ^
         //
         match = line.match(/^(\s*(class|struct).*:\s*).*,\s*$/);
         if (match) {
            return $verticallyAlignFunctionArgs ?
               new Array(match[1].length + 1).join(" ") :
               indent + tab;
         }

         // If we're looking at something like inheritance for a class, e.g.
         //
         // class Foo
         // : public Bar,
         //   ^
         //
         // then indent according to the first word following the ':'.
         match = line.match(/^(\s*:\s*)(\w+).*,\s*$/);
         if (match) {
            return $verticallyAlignFunctionArgs ?
               new Array(match[1].length + 1).join(" ") :
               indent + tab;
         }

         // Similar to the above, but we have a leading colon with some
         // following text, and no closing comma; ie
         //
         //   class Foo
         //       : public A
         //       ^
         match = line.match(/^(\s*)[:,]\s*[\w\s]*$/);
         if (match) {
            return $verticallyAlignFunctionArgs ?
               new Array(match[1].length + 1).join(" ") :
               indent + tab;
         }

         // Indent for lines ending with a '<'.
         if (/<\s*$/.test(line)) {
            return indent + tab;
         }

         // If the line is entirely a string, then match that line's indent.
         if (/^\s*\".*\"\s*$/.test(line)) {
            return indent;
         }

         // Don't indent for templates e.g.
         //
         //     template < ... >
         if (/^\s*template\s*<.*>\s*$/.test(line) &&
             line.split(">").length == line.split("<").length) {
            return indent;
         }
         
         // Vertical alignment
         // We need to handle vertical alignment for two scenarios:
         // One, for multi-line function declarations, so that e.g.
         //
         //   void foo(int a, int b, 
         //            ^
         //
         // and two, for cases where we have multiple objects. Maybe
         // this can just be specialized for {.
         //
         //   static object foo {
         //        {foo, bar},
         //        ^
         //
         // Only do this if there are more opening parens than closing parens
         // on the line, so that indentation for e.g. initialization lists
         // work as expected:
         //
         //   Foo(Foo const& other)
         //       : a_(a),
         //         b_(b),
         //         ^
         var bracePos = /([\[\{\(<]).+,\s*$/.exec(line);
         if (bracePos) {

            // Loop through the openers until we find an unmatched brace on
            // the line
            var openers = ["(", "{", "[", "<"];
            for (var i = 0; i < openers.length; i++) {

               // Get the character alongside its complement
               var lChar = openers[i];
               var rChar = this.$complements[lChar];

               // Get the indices for matches of the character and its complement
               var lIndices = this.allIndicesOf(line, lChar);
               if (!lIndices.length) continue;
               
               var rIndices = this.allIndicesOf(line, rChar);

               // Get the index -- we use the first unmatched index
               var indexToUse = lIndices.length - rIndices.length - 1;
               if (indexToUse < 0) continue;

               var index = lIndices[indexToUse];

               if ($verticallyAlignFunctionArgs) {

                  // Find the first character following the open token --
                  // this is where we want to set the indentation
                  var firstCharAfter = line.substr(index + 1).match(/([^\s])/);
                  return new Array(index + firstCharAfter.index + 2).join(" ");
                  
               } else {
                  return indent + tab;
               }
               
            }
         }

         // If this line begins, or ends, with an operator token alongside the previous,
         // then just use this line's indentation. This ensures that we match the indentation
         // for continued lines, e.g.
         //
         //     a +
         //         b +
         //         ^
         //
         var i = row - 1;
         var prevLineNotWhitespace = prevLine;
         while (i >= 0 && /^\s*$|^\s*#/.test(prevLineNotWhitespace)) {
            prevLineNotWhitespace = this.getLineSansComments(doc, i);
            i--;
         }

         if (reContinuation(line) && reContinuation(prevLineNotWhitespace))
            return this.$getIndent(line);

         // Try token walking
         if (this.$tokenUtils.$tokenizeUpToRow(row + 2)) {

            var tokens = new Array(this.$tokens.length);

            try {
               
               // Remove any trailing '\' tokens, then reapply them. This way, indentation
               // will work even in 'macro mode'.

               for (var i = 0; i < this.$tokens.length; i++) {
                  if (this.$tokens[i] != null &&
                      this.$tokens[i].length > 0) {
                     var rowTokens = this.$tokens[i];
                     if (rowTokens[rowTokens.length - 1].value === "\\") {
                        tokens[i] = this.$tokens[i].splice(rowTokens.length - 1, 1)[0];
                     }
                  } 
               }

               var tokenCursor = this.getTokenCursor();
               
               // If 'dontSubset' is false, then we want to plonk the token cursor
               // on the first token before the cursor. Otherwise, we place it at
               // the end of the current line
               if (!dontSubset)
               {
                  tokenCursor.moveToPosition(cursor);
               }
               else
               {
                  tokenCursor.$row = row;
                  tokenCursor.$offset = this.$tokens[row].length - 1;
               }

               // If there is no token on this current line (this can occur when this code
               // is accessed by e.g. the matching brace offset code) then move back
               // to the previous row
               while (tokenCursor.$offset < 0 && tokenCursor.$row > 0) {
                  tokenCursor.$row--;
                  tokenCursor.$offset = tokenCursor.$tokens[tokenCursor.$row].length - 1;
               }

               // If we're on a preprocessor line, keep moving back
               while (tokenCursor.$row > 0 &&
                      /^\s*#/.test(doc.getLine(tokenCursor.$row)))
               {
                  tokenCursor.$row--;
                  tokenCursor.$offset = tokenCursor.$tokens[tokenCursor.$row].length - 1;
               }

               // Set additional indent based on the first character
               var additionalIndent = "";

               // Keep track of where we started

               var startCursor = tokenCursor.cloneCursor();
               var startValue = startCursor.currentValue();
               var startType = startCursor.currentType();

               if (startType === "constant" ||
                   startType === "keyword" ||
                   startType === "identifier" ||
                   contains(["{", ")", ">", ":"], startValue))
               {
                  additionalIndent = tab;
               }

               // Move over any initial semicolons
               while (tokenCursor.currentValue() === ";") {
                  if (!tokenCursor.moveToPreviousToken()) {
                     break;
                  }
               }

               var lastCursor = tokenCursor.cloneCursor();

               if ($verticallyAlignFunctionArgs)
               {
                  // If the token cursor is on an operator at the end of the
                  // line...
                  if (tokenCursor.isLastSignificantTokenOnLine() &&
                      (tokenCursor.currentType() === "keyword.operator" ||
                       tokenCursor.currentType() === "punctuation.operator"))
                  {
                     // ... and the line starts with a keyword...
                     var lineStartCursor = tokenCursor.cloneCursor();
                     lineStartCursor.$offset = 0;
                     
                     if (lineStartCursor.currentType() === "keyword")
                     {
                        // ... and there are more opening parens than closing on the line,
                        // then vertically align
                        var balance = line.split("(").length - line.split(")").length;
                        if (balance > 0) {
                           var parenMatch = line.match(/.*?\(\s*(\S)/);
                           if (parenMatch) {
                              return new Array(parenMatch[0].length).join(" ");
                           }
                        }
                     }
                  }
               }

               // If the token cursor is on a comma...
               if (tokenCursor.currentValue() === ",") {

                  // ... and the previous character is a ']', find its match for indentation.
                  if ($verticallyAlignFunctionArgs)
                  {
                     var peekOne = tokenCursor.peekBwd();
                     if (peekOne.currentValue() === "]") {
                        if (peekOne.bwdToMatchingToken()) {
                           return new Array(peekOne.currentPosition().column + 1).join(" ");
                        }
                     }
                     
                     // ... and there are more opening parens than closing on the line,
                     // then vertically align
                     var balance = line.split("(").length - line.split(")").length;
                     if (balance > 0) {
                        var parenMatch = line.match(/.*?\(\s*(\S)/);
                        if (parenMatch) {
                           return new Array(parenMatch[0].length).join(" ");
                        }
                     }
                  }

                  // ... and this is a continuation of multiple commas, e.g.
                  //
                  //     int x = foo,
                  //       y = bar,
                  //       z = baz;
                  //
                  // then return that indent
                  if (endsWithCommaOrOpenParen(line) &&
                      endsWithCommaOrOpenParen(prevLineNotWhitespace))
                     return this.$getIndent(line);

                  // ... and it's an entry in an enum, then indent
                  var clone = tokenCursor.cloneCursor();
                  if (clone.findOpeningBracket("{", false) &&
                      clone.bwdOverClassySpecifiers() &&
                      clone.currentValue() === "enum")
                  {
                     return this.$getIndent(lines[clone.$row]) + tab;
                  }

                  // ... and there is an '=' on the line, then indent
                  if (line.indexOf("=") !== -1)
                     return this.$getIndent(line) + tab;

                  // ... just return the indent of the current line
                  return this.$getIndent(line);
               }

               // If the token cursor is on an operator, ident if the previous
               // token is not a class modifier token.
               if (startType === "keyword.operator" &&
                   startValue !== ":") {
                  return this.$getIndent(lines[row]) + tab;
               }

               while (true)
               {

                  // The token cursor is undefined (we moved past the start of the
                  // document)
                  if (typeof tokenCursor.currentValue() === "undefined") {
                     if (typeof lastCursor.currentValue() !== "undefined") {
                        return this.$getIndent(lines[lastCursor.$row]) + additionalIndent;
                     }
                     return additionalIndent;
                  }

                  lastCursor = tokenCursor.cloneCursor();

                  // We hit a semi-colon -- use the first token after that semi-colon.
                  if (tokenCursor.currentValue() === ";") {
                     if (tokenCursor.moveToNextToken()) {
                        
                        var row = tokenCursor.$row;
                        // Move up over preproc lines
                        while (lines[row] != null && /^\s*#/.test(lines[row]))
                           ++row;
                        
                        return this.$getIndent(lines[row]) + additionalIndent;
                     }
                  }

                  // We hit a 'control flow' keyword ...
                  if (contains(
                        ["for", "while", "do", "try"],
                        tokenCursor.currentValue()))
                  {
                     // ... and the first token wasn't a semi-colon, then indent
                     if (startValue !== ";") {
                        return this.$getIndent(lines[tokenCursor.$row]) + additionalIndent;
                     }
                     
                  }

                  // We hit a colon ':'...
                  var peekOne = tokenCursor.peekBwd();
                  if (tokenCursor.currentValue() === ":") {

                     // ... preceeded by a class access modifier
                     if (contains(["public", "private", "protected"],
                                  peekOne.currentValue()))
                     {
                        // Indent once relative to the 'public:'s indentation.
                        return this.$getIndent(lines[peekOne.$row]) + tab;
                     }

                     // ... with a line starting with 'case'
                     var maybeCaseLine = lines[tokenCursor.$row];
                     if (/^\s*case/.test(maybeCaseLine)) {
                        return this.$getIndent(maybeCaseLine) + tab;
                     }

                     // ... opening an initialization list
                     if (peekOne.currentValue() === ")") {
                        var clone = peekOne.cloneCursor();
                        if (clone.bwdToMatchingToken()) {

                           var peek1 = clone.peekBwd(1);
                           var peek2 = clone.peekBwd(2);

                           if (
                              (peek1 !== null && peek1.currentType() === "identifier") &&
                                 (peek2 !== null && !/\boperator\b/.test(peek2.currentType()))
                           )
                           {
                              
                              return this.$getIndent(lines[clone.peekBwd().$row]) + additionalIndent;
                           }
                        }
                     }
                  }

                  // We hit a '[]()' lambda expression.
                  if (tokenCursor.currentValue() === "]" &&
                      tokenCursor.peekFwd().currentValue() === "(") {
                     var clone = tokenCursor.cloneCursor();
                     if (clone.bwdToMatchingToken()) {
                        return this.$getIndent(lines[clone.$row]) + additionalIndent;
                     }
                  }

                  // Vertical alignment for e.g. 'for ( ... ;'.
                  //
                  // NOTE: Any ')' token found with a match _will have been jumped over_,
                  // so we can assume that any opening token found does not have a match.
                  if (tokenCursor.currentValue() === "(" &&
                      peekOne.currentValue() === "for" &&
                      startValue === ";")
                  {
                     
                     // Find the matching paren for the '(' after the cursor
                     var lookaheadCursor = tokenCursor.peekFwd().cloneCursor();

                     return $verticallyAlignFunctionArgs ?
                        new Array(tokenCursor.peekFwd().currentPosition().column + 1).join(" ") :
                        this.$getIndent(lines[tokenCursor.peekFwd().$row]) + tab;
                     
                  }

                  // Alignment for e.g.
                  // int foo(int
                  //
                  //             ^
                  if ($verticallyAlignFunctionArgs) {
                     if (tokenCursor.currentValue() === "(" &&
                         !tokenCursor.isLastSignificantTokenOnLine())
                     {
                        tokenCursor.moveToNextToken();
                        return new Array(tokenCursor.currentPosition().column + 1 + tabSize).join(" ");
                        
                     }
                  }

                  // We hit an 'if' or an 'else'
                  if (tokenCursor.currentValue() === "if" ||
                      tokenCursor.currentValue() === "else") {
                     return this.$getIndent(lines[tokenCursor.$row]) + additionalIndent;
                  }

                  // We hit 'template <'
                  if (tokenCursor.currentValue() === "template" &&
                      tokenCursor.peekFwd().currentValue() === "<")
                  {
                     return this.$getIndent(lines[tokenCursor.$row]) + additionalIndent;
                  }

                  // We hit an '{'
                  if (tokenCursor.currentValue() === "{") {

                     var openBraceIndentRow = this.getRowForOpenBraceIndent(session, tokenCursor.$row);
                     if (openBraceIndentRow >= 0) {
                        
                        // Don't indent if the brace is on the same line as a 'namespace' token
                        var line = this.getLineSansComments(doc, openBraceIndentRow);
                        var indent = this.$getIndent(line);
                        
                        return /\bnamespace\b/.test(line) ?
                           indent :
                           indent + tab;
                        
                     } else {
                        return this.$getIndent(lines[tokenCursor.$row]) + tab;
                     }
                  }

                  // We're at the start of the document
                  if (tokenCursor.$row === 0 && tokenCursor.$offset === 0) {
                     return this.$getIndent(lines[0]) + additionalIndent;
                  }

                  // Walking:

                  // Step over parens. Walk over '>' only if we can
                  // find its match to be associated with a 'template'.
                  if (tokenCursor.currentValue() === ">")
                  {
                     var clone = tokenCursor.cloneCursor();
                     if (clone.bwdToMatchingArrow()) {
                        if (clone.peekBwd().currentValue() === "template") {
                           if (startValue === ">") additionalIndent = "";
                           return this.$getIndent(lines[clone.$row]) + additionalIndent;
                        }
                     }
                  }

                  tokenCursor.bwdToMatchingToken();

                  // If we cannot move to a previous token, bail
                  if (!tokenCursor.moveToPreviousToken())
                     break;

                  // If the token cursor is on a preproc line, skip it
                  while (tokenCursor.$row > 0 &&
                         /^\s*#/.test(lines[tokenCursor.$row]))
                  {
                     tokenCursor.$row--;
                     tokenCursor.$offset = this.$tokens[tokenCursor.$row].length - 1;
                  }
               }

            } finally {

               for (var i = 0; i < tokens.length; i++) {
                  if (typeof tokens[i] !== "undefined") {
                     this.$tokens[i].push(tokens[i]);
                  }
               }

            }
            
         }

      } // start state rules

      return indent;
   };

   this.$onDocChange = function(evt)
   {
      if (evt.action === "insert")
         this.$tokenUtils.$insertNewRows(evt.start.row, evt.end.row - evt.start.row);
      else
         this.$tokenUtils.$removeRows(evt.start.row, evt.end.row - evt.start.row);

      this.$tokenUtils.$invalidateRow(evt.start.row);
      this.$scopes.invalidateFrom(evt.start);
   };

   this.$getIndent = function(line)
   {
      var match = /^([ \t]*)/.exec(line);
      if (!match)
         return ""; // should never happen, but whatever
      else
         return match[1];
   };

   // Pad an indentation up to some size by adding leading spaces.
   // This preserves tabs in the indent. Returns indentation as-is
   // if it's already that size or greater.
   this.$padIndent = function(indent, tabSize, newIndentSize) {

      var tabsAsSpaces = new Array(tabSize + 1).join(" ");
      var indentLength = indent.replace("\t", tabsAsSpaces);

      if (indentLength >= newIndentSize) {
         return indent;
      } else {
         return indent +
            new Array(newIndentSize - indentLength + 1).join(" ");
      }
   };

   this.$getUnindent = function(line, tabSize) {

      // Get the current line indent
      var indent = this.$getIndent(line);
      if (indent === null || indent.length === 0) {
         return "";
      }

      // Try cutting off a tab.
      var tabIndex = indent.indexOf("\t");
      if (tabIndex != -1) {
         return indent.substring(0, tabIndex) +
            indent.substring(tabIndex + 1, indent.length);
      }

      // Otherwise, try to remove up to 'tabSize' number of spaces
      var numLeadingSpaces = 0;
      for (var i = 0; i < tabSize && i < indent.length; i++) {
         if (indent[i] === " ") {
            numLeadingSpaces++;
         }
      }
      
      return indent.substring(numLeadingSpaces, indent.length);
      
   };

   
}).call(CppCodeModel.prototype);

exports.CppCodeModel = CppCodeModel;

});

