/*
 * r_code_model.js
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

define("mode/r_code_model", function(require, exports, module) {

var Range = require("ace/range").Range;
var TokenIterator = require("ace/token_iterator").TokenIterator;

function comparePoints(pos1, pos2)
{
   if (pos1.row != pos2.row)
      return pos1.row - pos2.row;
   return pos1.column - pos2.column;
}

var ScopeManager = require("mode/r_scope_tree").ScopeManager;

var RCodeModel = function(doc, tokenizer, statePattern, codeBeginPattern) {
   this.$doc = doc;
   this.$tokenizer = tokenizer;
   this.$tokens = new Array(doc.getLength());
   this.$endStates = new Array(doc.getLength());
   this.$statePattern = statePattern;
   this.$codeBeginPattern = codeBeginPattern;
   this.$scopes = new ScopeManager();

   var that = this;
   this.$doc.on('change', function(evt) {
      that.$onDocChange.apply(that, [evt]);
   });

   this.$TokenCursor = function(row, offset)
   {
      this.$row = row || 0;
      this.$offset = offset || 0;
   };
   (function() {
      this.moveToStartOfRow = function(row)
      {
         this.$row = row;
         this.$offset = 0;
      };

      this.moveToPreviousToken = function()
      {
         while (this.$offset == 0 && this.$row > 0)
         {
            this.$row--;
            this.$offset = that.$tokens[this.$row].length;
         }

         if (this.$offset == 0)
            return false;

         this.$offset--;

         return true;
      };

      this.moveToNextToken = function(maxRow)
      {
         if (this.$row > maxRow)
            return;

         this.$offset++;

         while (this.$offset >= that.$tokens[this.$row].length && this.$row < maxRow)
         {
            this.$row++;
            this.$offset = 0;
         }

         if (this.$offset >= that.$tokens[this.$row].length)
            return false;

         return true;
      };

      this.seekToNearestToken = function(position, maxRow)
      {
         if (position.row > maxRow)
            return false;
         this.$row = position.row;
         var rowTokens = that.$tokens[this.$row] || [];
         for (this.$offset = 0; this.$offset < rowTokens.length; this.$offset++)
         {
            var token = rowTokens[this.$offset];
            if (token.column >= position.column)
            {
               return true;
            }
         }
         return this.moveToNextToken(maxRow);
      };

      this.moveBackwardOverMatchingParens = function()
      {
         if (!this.moveToPreviousToken())
            return false;
         if (this.currentValue() !== ")")
            return false;

         var success = false;
         var parenCount = 0;
         while (this.moveToPreviousToken())
         {
            if (this.currentValue() === "(")
            {
               if (parenCount == 0)
               {
                  success = true;
                  break;
               }
               parenCount--;
            }
            else if (this.currentValue() === ")")
            {
               parenCount++;
            }
         }
         return success;
      };

      this.findToken = function(predicate, maxRow)
      {
         do
         {
            var t = this.currentToken();
            if (t && predicate(t))
               return t;
         }
         while (this.moveToNextToken(maxRow));
         return null;
      };

      this.currentToken = function()
      {
         return (that.$tokens[this.$row] || [])[this.$offset];
      };

      this.currentValue = function()
      {
         return this.currentToken().value;
      };

      this.currentPosition = function()
      {
         var token = this.currentToken();
         if (token === null)
            return null;
         else
            return {row: this.$row, column: token.column};
      };

      this.cloneCursor = function()
      {
         var clone = new that.$TokenCursor();
         clone.$row = this.$row;
         clone.$offset = this.$offset;
         return clone;
      };

      this.isFirstSignificantTokenOnLine = function()
      {
         return this.$offset == 0;
      };

      this.isLastSignificantTokenOnLine = function()
      {
         return this.$offset == (that.$tokens[this.$row] || []).length - 1;
      };

   }).call(this.$TokenCursor.prototype);

};

(function () {

   this.$complements = {
      '(': ')',
      ')': '(',
      '[': ']',
      ']': '[',
      '{': '}',
      '}': '{'
   };

   function pFunction(t)
   {
      return t.type == 'keyword' && t.value == 'function';
   }

   function pAssign(t)
   {
      return /\boperator\b/.test(t.type) && /^(=|<-|<<-)$/.test(t.value);
   }

   function pIdentifier(t)
   {
      return /\bidentifier\b/.test(t.type);
   }

   function findAssocFuncToken(tokenCursor)
   {
      if (tokenCursor.currentValue() !== "{")
         return false;
      if (!tokenCursor.moveBackwardOverMatchingParens())
         return false;
      if (!tokenCursor.moveToPreviousToken())
         return false;
      if (!pFunction(tokenCursor.currentToken()))
         return false;
      if (!tokenCursor.moveToPreviousToken())
         return false;
      if (!pAssign(tokenCursor.currentToken()))
         return false;
      if (!tokenCursor.moveToPreviousToken())
         return false;
      if (!pIdentifier(tokenCursor.currentToken()))
         return false;

      return true;
   }

   this.$buildScopeTreeUpToRow = function(maxrow)
   {
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

      function getChunkLabel(reOptions, comment) {
         var match = reOptions.exec(comment);
         if (!match)
            return null;
         var value = match[1];
         var values = value.split(',');
         if (values.length == 0)
            return null;

         // If first arg has no =, it's a label
         if (!/=/.test(values[0])) {
            return values[0].replace(/(^\s+)|(\s+$)/g, '');
         }

         for (var i = 0; i < values.length; i++) {
            match = /^\s*label\s*=\s*(.*)$/.exec(values[i]);
            if (match) {
               return maybeEvaluateLiteralString(
                                        match[1].replace(/(^\s+)|(\s+$)/g, ''));
            }
         }

         return null;
      }

      // It's possible that determining the scope at 'position' may require
      // parsing beyond the position itself--for example if the position is
      // on the identifier of a function whose open brace is a few tokens later.
      // Seems like it would be rare indeed for this distance to be more than 30
      // rows.
      maxRow = Math.min(maxrow + 30, this.$doc.getLength() - 1);
      this.$tokenizeUpToRow(maxRow);

      //console.log("Seeking to " + this.$scopes.parsePos.row + "x"+ this.$scopes.parsePos.column);
      var tokenCursor = new this.$TokenCursor();
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
            var sectionHeadMatch = /^#+'?[-=#\s]*(.*?)\s*[-=#]+\s*$/.exec(
                  tokenCursor.currentValue());

            var label = "" + sectionHeadMatch[1];
            if (label.length == 0)
               label = "(Untitled)";
            if (label.length > 50)
               label = label.substring(0, 50) + "...";

            this.$scopes.onSectionHead(label, tokenCursor.currentPosition());
         }
         else if (/\bcodebegin\b/.test(tokenType))
         {
            var chunkStartPos = tokenCursor.currentPosition();
            var chunkPos = {row: chunkStartPos.row + 1, column: 0};
            var chunkNum = this.$scopes.getTopLevelScopeCount()+1;
            var chunkLabel = getChunkLabel(this.$codeBeginPattern,
                                           tokenCursor.currentValue());
            var scopeName = "Chunk " + chunkNum;
            if (chunkLabel)
               scopeName += ": " + chunkLabel;
            this.$scopes.onChunkStart(chunkLabel,
                                      scopeName,
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
            var localCursor = tokenCursor.cloneCursor();
            var startPos;
            if (findAssocFuncToken(localCursor))
            {
               startPos = localCursor.currentPosition();
               if (localCursor.isFirstSignificantTokenOnLine())
                  startPos.column = 0;
               this.$scopes.onFunctionScopeStart(localCursor.currentValue(),
                                                 startPos,
                                                 tokenCursor.currentPosition());
            }
            else
            {
               startPos = tokenCursor.currentPosition();
               if (tokenCursor.isFirstSignificantTokenOnLine())
                  startPos.column = 0;
               this.$scopes.onScopeStart(startPos);
            }
         }
         else if (tokenCursor.currentValue() === "}")
         {
            var pos = tokenCursor.currentPosition();
            if (tokenCursor.isLastSignificantTokenOnLine())
            {
               pos.column = this.$getLine(pos.row).length + 1;
            }
            else
            {
               pos.column++;
            }
            this.$scopes.onScopeEnd(pos);
         }
      } while (tokenCursor.moveToNextToken(maxRow));
   };

   this.$getFoldToken = function(session, foldStyle, row) {
      this.$tokenizeUpToRow(row);

      if (this.$statePattern && !this.$statePattern.test(this.$endStates[row]))
         return "";

      var rowTokens = this.$tokens[row];

      if (rowTokens.length == 1 && /\bsectionhead\b/.test(rowTokens[0].type))
         return rowTokens[0];

      var depth = 0;
      var unmatchedOpen = null;
      var unmatchedClose = null;

      for (var i = 0; i < rowTokens.length; i++) {
         var token = rowTokens[i];
         if (/\bparen\b/.test(token.type)) {
            switch (token.value) {
               case '{':
                  depth++;
                  if (depth == 1) {
                     unmatchedOpen = token;
                  }
                  break;
               case '}':
                  depth--;
                  if (depth == 0) {
                     unmatchedOpen = null;
                  }
                  if (depth < 0) {
                     unmatchedClose = token;
                     depth = 0;
                  }
                  break;
            }
         }
      }

      if (unmatchedOpen)
         return unmatchedOpen;

      if (foldStyle == "markbeginend" && unmatchedClose)
         return unmatchedClose;

      if (rowTokens.length >= 1) {
         if (/\bcodebegin\b/.test(rowTokens[0].type))
            return rowTokens[0];
         else if (/\bcodeend\b/.test(rowTokens[0].type))
            return rowTokens[0];
      }

      return null;
   };

   this.getFoldWidget = function(session, foldStyle, row) {
      var foldToken = this.$getFoldToken(session, foldStyle, row);
      if (foldToken == null)
         return "";
      if (foldToken.value == '{')
         return "start";
      else if (foldToken.value == '}')
         return "end";
      else if (/\bcodebegin\b/.test(foldToken.type))
         return "start";
      else if (/\bcodeend\b/.test(foldToken.type))
         return "end";
      else if (/\bsectionhead\b/.test(foldToken.type))
         return "start";

      return "";
   };

   this.getFoldWidgetRange = function(session, foldStyle, row) {
      var foldToken = this.$getFoldToken(session, foldStyle, row);
      if (!foldToken)
         return;

      var pos = {row: row, column: foldToken.column + 1};

      if (foldToken.value == '{') {
         var end = session.$findClosingBracket(foldToken.value, pos);
         if (!end)
            return;
         return Range.fromPoints(pos, end);
      }
      else if (foldToken.value == '}') {
         var start = session.$findOpeningBracket(foldToken.value, pos);
         if (!start)
            return;
         return Range.fromPoints({row: start.row, column: start.column+1},
                                 {row: pos.row, column: pos.column-1});
      }
      else if (/\bcodebegin\b/.test(foldToken.type)) {
         // Find next codebegin or codeend
         var tokenIterator = new TokenIterator(session, row, 0);
         for (var tok; tok = tokenIterator.stepForward(); ) {
            if (/\bcode(begin|end)\b/.test(tok.type)) {
               var begin = /\bcodebegin\b/.test(tok.type);
               var tokRow = tokenIterator.getCurrentTokenRow();
               var endPos = begin
                     ? {row: tokRow-1, column: session.getLine(tokRow-1).length}
                     : {row: tokRow, column: session.getLine(tokRow).length};
               return Range.fromPoints(
                     {row: row, column: foldToken.column + foldToken.value.length},
                     endPos);
            }
         }
         return;
      }
      else if (/\bcodeend\b/.test(foldToken.type)) {
         var tokenIterator2 = new TokenIterator(session, row, 0);
         for (var tok2; tok2 = tokenIterator2.stepBackward(); ) {
            if (/\bcodebegin\b/.test(tok2.type)) {
               var tokRow2 = tokenIterator2.getCurrentTokenRow();
               return Range.fromPoints(
                     {row: tokRow2, column: session.getLine(tokRow2).length},
                     {row: row, column: session.getLine(row).length});
            }
         }
         return;
      }
      else if (/\bsectionhead\b/.test(foldToken.type)) {
         var match = /([-=#])\1+\s*$/.exec(foldToken.value);
         if (!match)
            return;  // this would be surprising

         pos.column += match.index - 1; // Not actually sure why -1 is needed
         var tokenIterator3 = new TokenIterator(session, row, 0);
         var lastRow = row;
         for (var tok3; tok3 = tokenIterator3.stepForward(); ) {
            if (/\bsectionhead\b/.test(tok3.type)) {
               break;
            }
            lastRow = tokenIterator3.getCurrentTokenRow();
         }

         return Range.fromPoints(
               pos,
               {row: lastRow, column: session.getLine(lastRow).length});
      }

      return;
   };

   this.getCurrentScope = function(position, filter)
   {
      if (!filter)
         filter = function(scope) { return true; }

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

   this.findFunctionDefinitionFromUsage = function(usagePos, functionName)
   {
      this.$buildScopeTreeUpToRow(this.$doc.getLength() - 1);
      return this.$scopes.findFunctionDefinitionFromUsage(usagePos,
                                                          functionName);
   };

   this.getIndentForOpenBrace = function(pos)
   {
      if (this.$tokenizeUpToRow(pos.row))
      {
         var tokenCursor = new this.$TokenCursor();
         if (tokenCursor.seekToNearestToken(pos, pos.row)
                   && tokenCursor.currentValue() == "{"
               && tokenCursor.moveBackwardOverMatchingParens())
         {
            return this.$getIndent(this.$getLine(tokenCursor.currentPosition().row));
         }
      }

      return this.$getIndent(this.$getLine(pos.row));
   };

   this.getNextLineIndent = function(lastRow, line, endState, tab, tabSize)
   {
      if (endState == "qstring" || endState == "qqstring")
         return "";

      // This lineOverrides nonsense is necessary because the line has not 
      // changed in the real document yet. We need to simulate it by replacing
      // the real line with the `line` param, and when we finish with this
      // method, undo the damage and invalidate the row.
      // To repro the problem without using lineOverrides, comment out this
      // block of code, and in the editor hit Enter in the middle of a line 
      // that contains a }.
      this.$lineOverrides = null;
      if (!(this.$doc.getLine(lastRow) === line))
      {
         this.$lineOverrides = {};
         this.$lineOverrides[lastRow] = line;
         this.$invalidateRow(lastRow);
      }
      
      try
      {
         var defaultIndent = lastRow < 0 ? "" 
                                         : this.$getIndent(this.$getLine(lastRow));

         if (!this.$tokenizeUpToRow(lastRow))
            return defaultIndent;

         if (this.$statePattern && !this.$statePattern.test(endState))
            return defaultIndent;

         var continuationIndent = "";

         var prevToken = this.$findPreviousSignificantToken({row: lastRow, column: this.$getLine(lastRow).length},
                                                            lastRow - 10);
         if (prevToken
               && /\bparen\b/.test(prevToken.token.type)
               && /\)$/.test(prevToken.token.value))
         {
            var openParenPos = this.$walkParensBalanced(
                  prevToken.row,
                  prevToken.row - 10,
                  null,
                  function(parens, paren, pos)
                  {
                     return parens.length === 0;
                  });

            if (openParenPos != null)
            {
               var preParenToken = this.$findPreviousSignificantToken(openParenPos, 0);
               if (preParenToken && preParenToken.token.type === "keyword"
                     && /^(if|while|for|function)$/.test(preParenToken.token.value))
               {
                  return this.$getIndent(this.$getLine(preParenToken.row)) + tab;
               }
            }
         }
         else if (prevToken
                     && prevToken.token.type === "keyword"
                     && (prevToken.token.value === "repeat" || prevToken.token.value === "else"))
         {
            return this.$getIndent(this.$getLine(prevToken.row)) + tab;
         }
         else if (prevToken && /\boperator\b/.test(prevToken.token.type) && !/\bparen\b/.test(prevToken.token.type))
         {
            // Fix issue 2579: If the previous significant token is an operator
            // (commonly, "+" when used with ggplot) then this line is a
            // continuation of an expression that was started on a previous
            // line. This line's indent should then be whatever would normally
            // be used for a complete statement starting here, plus a tab.
            continuationIndent = tab;
         }

         var openBracePos = this.$walkParensBalanced(
               lastRow,
               0,
               function(parens, paren, pos)
               {
                  return /[\[({]/.test(paren) && parens.length === 0;
               },
               null);

         if (openBracePos != null)
         {
            var nextTokenPos = this.$findNextSignificantToken({
                  row: openBracePos.row,
                  column: openBracePos.column + 1
               }, lastRow);

            if (!nextTokenPos)
            {
               return this.getIndentForOpenBrace(openBracePos) +
                      tab + continuationIndent;
            }
            else
            {
               // Return indent up to next token position.
               // Note that in hard tab mode, the tab character only counts 
               // as a single character unfortunately. What we really want
               // is the screen column, but what we have is the document
               // column, which we can't convert to screen column without
               // copy-and-pasting a bunch of code from layer/text.js.
               // As a shortcut, we just pull off the leading whitespace
               // from the line and include it verbatim in the new indent.
               // This strategy works fine unless there is a tab in the
               // line that comes after a non-whitespace character, which
               // seems like it should be rare.
               var line = this.$getLine(nextTokenPos.row);
               var leadingIndent = line.replace(/[^\s].*$/, '');

               var indentWidth = nextTokenPos.column - leadingIndent.length;
               var tabsToUse = Math.floor(indentWidth / tabSize);
               var spacesToAdd = indentWidth - (tabSize * tabsToUse);
               var buffer = "";
               for (var i = 0; i < tabsToUse; i++)
                  buffer += tab;
               for (var j = 0; j < spacesToAdd; j++)
                  buffer += " ";
               return leadingIndent + buffer + continuationIndent;
            }
         }

         var firstToken = this.$findNextSignificantToken({row: 0, column: 0}, lastRow);
         if (firstToken)
            return this.$getIndent(this.$getLine(firstToken.row)) + continuationIndent;
         else
            return "" + continuationIndent;
      }
      finally
      {
         if (this.$lineOverrides)
         {
            this.$lineOverrides = null;
            this.$invalidateRow(lastRow);
         }
      }
   };

   this.getBraceIndent = function(lastRow)
   {
      this.$tokenizeUpToRow(lastRow);

      var prevToken = this.$findPreviousSignificantToken({row: lastRow, column: this.$getLine(lastRow).length},
                                                         lastRow - 10);
      if (prevToken
            && /\bparen\b/.test(prevToken.token.type)
            && /\)$/.test(prevToken.token.value))
      {
         var lastPos = this.$walkParensBalanced(
               prevToken.row,
               prevToken.row - 10,
               null,
               function(parens, paren, pos)
               {
                  return parens.length == 0;
               });

         if (lastPos != null)
         {
            var preParenToken = this.$findPreviousSignificantToken(lastPos, 0);
            if (preParenToken && preParenToken.token.type === "keyword"
                  && /^(if|while|for|function)$/.test(preParenToken.token.value))
            {
               return this.$getIndent(this.$getLine(preParenToken.row));
            }
         }
      }
      else if (prevToken
                  && prevToken.token.type === "keyword"
                  && (prevToken.token.value === "repeat" || prevToken.token.value === "else"))
      {
         return this.$getIndent(this.$getLine(prevToken.row));
      }

      return this.$getIndent(lastRow);
   };

   /**
    * If headInclusive, then a token will match if it starts at pos.
    * If tailInclusive, then a token will match if it ends at pos (meaning
    *    token.column + token.length == pos.column, and token.row == pos.row
    * In all cases, a token will match if pos is after the head and before the
    *    tail.
    *
    * If no token is found, null is returned.
    *
    * Note that whitespace and comment tokens will never be returned.
    */
   this.getTokenForPos = function(pos, headInclusive, tailInclusive)
   {
      this.$tokenizeUpToRow(pos.row);

      if (this.$tokens.length <= pos.row)
         return null;
      var tokens = this.$tokens[pos.row];
      for (var i = 0; i < tokens.length; i++)
      {
         var token = tokens[i];

         if (headInclusive && pos.column == token.column)
            return token;
         if (pos.column <= token.column)
            return null;

         if (tailInclusive && pos.column == token.column + token.value.length)
            return token;
         if (pos.column < token.column + token.value.length)
            return token;
      }
      return null;
   };

   this.$tokenizeUpToRow = function(lastRow)
   {
      // Don't let lastRow be past the end of the document
      lastRow = Math.min(lastRow, this.$endStates.length - 1);

      var row = 0;
      var assumeGood = true;
      for ( ; row <= lastRow; row++)
      {
         // No need to tokenize rows until we hit one that has been explicitly
         // invalidated.
         if (assumeGood && this.$endStates[row])
            continue;
         
         assumeGood = false;

         var state = (row === 0) ? 'start' : this.$endStates[row-1];
         var lineTokens = this.$tokenizer.getLineTokens(this.$getLine(row), state);
         if (!this.$statePattern || this.$statePattern.test(lineTokens.state) || this.$statePattern.test(state))
            this.$tokens[row] = this.$filterWhitespaceAndComments(lineTokens.tokens);
         else
            this.$tokens[row] = [];

         // If we ended in the same state that the cache says, then we know that
         // the cache is up-to-date for the subsequent lines--UNTIL we hit a row
         // that has been explicitly invalidated.
         if (lineTokens.state === this.$endStates[row])
            assumeGood = true;
         else
            this.$endStates[row] = lineTokens.state;
      }
      
      if (!assumeGood)
      {
         // If we get here, it means the last row we saw before we exited
         // was invalidated or impacted by an invalidated row. We need to
         // make sure the NEXT row doesn't get ignored next time the tokenizer
         // makes a pass.
         if (row < this.$tokens.length)
            this.$invalidateRow(row);
      }
      
      return true;
   };

   this.$onDocChange = function(evt)
   {
      var delta = evt.data;

      if (delta.action === "insertLines")
      {
         this.$insertNewRows(delta.range.start.row,
                             delta.range.end.row - delta.range.start.row);
      }
      else if (delta.action === "insertText")
      {
         if (this.$doc.isNewLine(delta.text))
         {
            this.$invalidateRow(delta.range.start.row);
            this.$insertNewRows(delta.range.end.row, 1);
         }
         else
         {
            this.$invalidateRow(delta.range.start.row);
         }
      }
      else if (delta.action === "removeLines")
      {
         this.$removeRows(delta.range.start.row,
                          delta.range.end.row - delta.range.start.row);
         this.$invalidateRow(delta.range.start.row);
      }
      else if (delta.action === "removeText")
      {
         if (this.$doc.isNewLine(delta.text))
         {
            this.$removeRows(delta.range.end.row, 1);
            this.$invalidateRow(delta.range.start.row);
         }
         else
         {
            this.$invalidateRow(delta.range.start.row);
         }
      }

      this.$scopes.invalidateFrom(delta.range.start);
   };
   
   this.$invalidateRow = function(row)
   {
      this.$tokens[row] = null;
      this.$endStates[row] = null;
   };
   
   this.$insertNewRows = function(row, count)
   {
      var args = [row, 0];
      for (var i = 0; i < count; i++)
         args.push(null);
      this.$tokens.splice.apply(this.$tokens, args);
      this.$endStates.splice.apply(this.$endStates, args);
   };
   
   this.$removeRows = function(row, count)
   {
      this.$tokens.splice(row, count);
      this.$endStates.splice(row, count);
   };
   
   this.$getIndent = function(line)
   {
      var match = /^([ \t]*)/.exec(line);
      if (!match)
         return ""; // should never happen, but whatever
      else
         return match[1];
   };

   this.$getLine = function(row)
   {
      if (this.$lineOverrides && typeof(this.$lineOverrides[row]) != 'undefined')
         return this.$lineOverrides[row];
      return this.$doc.getLine(row);
   };

   this.$walkParens = function(startRow, endRow, fun)
   {
      var parenRe = /\bparen\b/;

      if (startRow < endRow)  // forward
      {
         return (function() {
            for ( ; startRow <= endRow; startRow++)
            {
               var tokens = this.$tokens[startRow];
               for (var i = 0; i < tokens.length; i++)
               {
                  if (parenRe.test(tokens[i].type))
                  {
                     var value = tokens[i].value;
                     if (!fun(value, {row: startRow, column: tokens[i].column}))
                        return false;
                  }
               }
            }
            return true;
         }).call(this);
      }
      else // backward
      {
         return (function() {
            startRow = Math.max(0, startRow);
            endRow = Math.max(0, endRow);

            for ( ; startRow >= endRow; startRow--)
            {
               var tokens = this.$tokens[startRow];
               for (var i = tokens.length - 1; i >= 0; i--)
               {
                  if (parenRe.test(tokens[i].type))
                  {
                     var value = tokens[i].value;
                     if (!fun(value, {row: startRow, column: tokens[i].column}))
                        return false;
                  }
               }
            }
            return true;
         }).call(this);
      }
   };

   // Walks BACKWARD over matched pairs of parens. Stop and return result
   // when optional function params preMatch or postMatch return true.
   // preMatch is called when a paren is encountered and BEFORE the parens
   // stack is modified. postMatch is called after the parens stack is modified.
   this.$walkParensBalanced = function(startRow, endRow, preMatch, postMatch)
   {
      // The current stack of parens that are in effect.
      var parens = [];
      var result = null;
      var that = this;
      this.$walkParens(startRow, endRow, function(paren, pos)
      {
         if (preMatch && preMatch(parens, paren, pos))
         {
            result = pos;
            return false;
         }

         if (/[\[({]/.test(paren))
         {
            if (parens[parens.length - 1] === that.$complements[paren])
               parens.pop();
            else
               return true;
         }
         else
         {
            parens.push(paren);
         }

         if (postMatch && postMatch(parens, paren, pos))
         {
            result = pos;
            return false;
         }

         return true;
      });

      return result;
   };

   this.$findNextSignificantToken = function(pos, lastRow)
   {
      if (this.$tokens.length == 0)
         return null;
      lastRow = Math.min(lastRow, this.$tokens.length - 1);
      
      var row = pos.row;
      var col = pos.column;
      for ( ; row <= lastRow; row++)
      {
         var tokens = this.$tokens[row];

         for (var i = 0; i < tokens.length; i++)
         {
            if (tokens[i].column + tokens[i].value.length > col)
            {
               return {
                  token: tokens[i], 
                  row: row, 
                  column: Math.max(tokens[i].column, col),
                  offset: i
               };
            }
         }

         col = 0; // After the first row, we'll settle for a token anywhere
      }
      return null;
   };
   
   this.$findPreviousSignificantToken = function(pos, firstRow)
   {
      if (this.$tokens.length == 0)
         return null;
      firstRow = Math.max(0, firstRow);
      
      var row = Math.min(pos.row, this.$tokens.length - 1);
      for ( ; row >= firstRow; row--)
      {
         var tokens = this.$tokens[row];
         if (tokens.length == 0)
            continue;
         
         if (row != pos.row)
            return {
               row: row,
               column: tokens[tokens.length - 1].column,
               token: tokens[tokens.length - 1],
               offset: tokens.length - 1
            };
         
         for (var i = tokens.length - 1; i >= 0; i--)
         {
            if (tokens[i].column < pos.column)
            {
               return {
                  row: row,
                  column: tokens[i].column,
                  token: tokens[i],
                  offset: i
               };
            }
         }
      }
   };
   
   function isWhitespaceOrComment(token)
   {
      // virtual-comment is for roxygen content that needs to be highlighted
      // as TeX, but for the purposes of the code model should be invisible.

      if (/\bcode(?:begin|end)\b/.test(token.type))
         return false;

      if (/\bsectionhead\b/.test(token.type))
         return false;

      return /^\s*$/.test(token.value) ||
             token.type.match(/\b(?:ace_virtual-)?comment\b/);
   }

   this.$filterWhitespaceAndComments = function(tokens)
   {
      tokens = tokens.filter(function (t) {
         return !isWhitespaceOrComment(t);
      });

      for (var i = tokens.length - 1; i >= 0; i--)
      {
         if (tokens[i].value.length > 1 && /\bparen\b/.test(tokens[i].type))
         {
            var token = tokens[i];
            tokens.splice(i, 1);
            for (var j = token.value.length - 1; j >= 0; j--)
            {
               var newToken = {
                  type: token.type,
                  value: token.value.charAt(j),
                  column: token.column + j
               };
               tokens.splice(i, 0, newToken);
            }
         }
      }
      return tokens;
   };

}).call(RCodeModel.prototype);

exports.RCodeModel = RCodeModel;

});
