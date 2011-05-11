/*
 * r_code_model.js
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

define("mode/r_code_model", function(require, exports, module) {

function comparePoints(pos1, pos2)
{
   if (pos1.row != pos2.row)
      return pos1.row - pos2.row;
   return pos1.column - pos2.column;
}

var Anchor = require("ace/anchor").Anchor;

var RCodeModel = function(doc, tokenizer, statePattern) {
   this.$doc = doc;
   this.$tokenizer = tokenizer;
   this.$tokens = new Array(doc.getLength());
   this.$endStates = new Array(doc.getLength());
   this.$statePattern = statePattern;

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

      this.seekToNearestToken = function(position)
      {
         this.$row = position.row;
         var rowTokens = that.$tokens[this.$row] || [];
         for (this.$offset = 0; this.$offset < rowTokens.length; this.$offset++)
         {
            var token = rowTokens[this.$offset];
            if (token.column + token.value.length >= position.column)
            {
               break;
            }
         }
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

   this.$ScopeTree = function(label, start)
   {
      this.label = label;
      this.start = start;
      this.end = null;
      this.$children = [];
   };

   (function() {
      this.getLabel = function()
      {
         return this.label;
      };

      this.getStart = function()
      {
         return this.start;
      };

      this.setEnd = function(pos)
      {
         this.end = pos;
      };

      this.getEnd = function()
      {
         return this.end;
      };

      this.exportFunctions = function(list)
      {
         if (this.label)
         {
            var here = {
               label: this.label,
               start: this.start,
               children: []
            };
            list.push(here);
            list = here.children;
         }

         for (var i = 0; i < this.$children.length; i++)
            this.$children[i].exportFunctions(list);
      };

      this.comparePosition = function(pos)
      {
         if (comparePoints(pos, this.start) < 0)
            return -1;
         if (this.end != null && comparePoints(pos, this.end) >= 0)
            return 1;
         return 0;
      };

      this.addChild = function(label, start)
      {
         var child = new that.$ScopeTree(label, start);
         var index = this.$binarySearch(start);
         if (index >= 0)
         {
            return this.$children.addChild(label, start);
         }
         else
         {
            index = -(index+1);
            this.$children.splice(index, 0, child);
            return child;
         }
      };

      this.findFunction = function(pos)
      {
         var index = this.$binarySearch(pos);
         if (index >= 0)
         {
            var child = this.$children[index].findFunction(pos);
            if (child)
               return child;
            if (this.label)
               return this;
            return null;
         }
         else
         {
            return this.label ? this : null;
         }
      };

      // Invalidates everything after pos, and possibly some stuff before.
      // Returns the position from which parsing should resume.
      this.invalidateFrom = function(pos)
      {
         var index = this.$binarySearch(pos);
         var resumePos;
         if (index >= 0)
         {
            resumePos = this.$children[index].getStart();
         }
         else
         {
            index = -(index+1);
            resumePos = pos;
         }

         if (index < this.$children.length)
         {
            this.$children.splice(index, this.$children.length - index);
         }

         this.end = null;

         return resumePos;
      };

      this.pushOpenScopes = function(stack)
      {
         if (this.end === null)
         {
            stack.push(this);
            if (this.$children.length > 0)
               this.$children[this.$children.length - 1].pushOpenScopes(stack);
         }
      };

      // start is inclusive, end is exclusive
      // Positive result is match, negative result is -([closest index] + 1)
      this.$binarySearch = function(pos, start /*optional*/, end /*optional*/)
      {
         if (typeof(start) === 'undefined')
            start = 0;
         if (typeof(end) === 'undefined')
            end = this.$children.length;

         // No elements left to test
         if (start === end)
            return -(start + 1);

         var mid = Math.floor((start + end)/2);
         var comp = this.$children[mid].comparePosition(pos);
         if (comp === 0)
            return mid;
         else if (comp < 0)
            return this.$binarySearch(pos, start, mid);
         else // comp > 0
            return this.$binarySearch(pos, mid + 1, end);
      };
   }).call(this.$ScopeTree.prototype);

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
      if (!tokenCursor.moveToPreviousToken())
         return false;
      if (tokenCursor.currentValue() !== ")")
         return false;

      var success = false;
      var parenCount = 0;
      while (tokenCursor.moveToPreviousToken())
      {
         if (tokenCursor.currentValue() === "(")
         {
            if (parenCount == 0)
            {
               success = true;
               break;
            }
            parenCount--;
         }
         else if (tokenCursor.currentValue() === ")")
         {
            parenCount++;
         }
      }
      if (!success)
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

   this.$invalidateScopeTreeFrom = function(position)
   {
      if (this.$scopeTree && comparePoints(this.$scopeTreeParsePos, position) > 0)
      {
         this.$scopeTreeParsePos = this.$scopeTree.invalidateFrom(position);
      }
   };

   this.$buildScopeTreeUpToRow = function(maxrow)
   {
      // It's possible that determining the scope at 'position' may require
      // parsing beyond the position itself--for example if the position is
      // on the identifier of a function whose open brace is a few tokens later.
      // Seems like it would be rare indeed for this distance to be more than 30
      // rows.
      maxRow = Math.min(maxrow + 30, this.$doc.getLength() - 1);
      this.$tokenizeUpToRow(maxRow);

      // If this is the first time we're parsing, initialize the scope tree
      if (!this.$scopeTree)
      {
         this.$scopeTreeParsePos = {row: 0, column: 0};
         this.$scopeTree = new this.$ScopeTree("(Top Level)", this.$scopeTreeParsePos);
      }

      var nodes = [];
      this.$scopeTree.pushOpenScopes(nodes);
      var tokenCursor = new this.$TokenCursor();
      tokenCursor.seekToNearestToken(this.$scopeTreeParsePos);

      while (tokenCursor.moveToNextToken(maxRow))
      {
         this.$scopeTreeParsePos = tokenCursor.currentPosition();
         this.$scopeTreeParsePos.column += tokenCursor.currentValue().length;

         if (tokenCursor.currentValue() === "{")
         {
            var localCursor = tokenCursor.cloneCursor();
            var label, startPos;
            if (findAssocFuncToken(localCursor))
            {
               label = localCursor.currentValue();
               startPos = localCursor.currentPosition();
               if (localCursor.isFirstSignificantTokenOnLine())
                  startPos.column = 0;
            }
            else
            {
               label = null;
               startPos = tokenCursor.currentPosition();
               if (tokenCursor.isFirstSignificantTokenOnLine())
                  startPos.column = 0;
            }

            var child = nodes[nodes.length - 1].addChild(label, startPos);
            nodes.push(child);
         }
         else if (tokenCursor.currentValue() === "}")
         {
            if (nodes.length > 1)
            {
               var node = nodes.pop();
               var pos = tokenCursor.currentPosition();
               if (tokenCursor.isLastSignificantTokenOnLine())
               {
                  pos.row++;
                  pos.column = 0;
               }
               else
               {
                  pos.column++;
               }
               node.setEnd(pos);
            }
         }
      }
   };

   this.getCurrentFunction = function(position)
   {
      if (!position)
         return "";
      this.$buildScopeTreeUpToRow(position.row);
      return this.$scopeTree.findFunction(position);
   };

   this.getFunctionTree = function()
   {
      this.$buildScopeTreeUpToRow(this.$doc.getLength() - 1);
      var list = [];
      this.$scopeTree.exportFunctions(list);
      return list[0].children;
   };

   this.getNextLineIndent = function(lastRow, line, endState, tab, tabSize)
   {
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
            // Is the previous line also a continuation line?
            var prevContToken = this.$findPreviousSignificantToken({row: prevToken.row, column: 0}, 0);
            if (!prevContToken || !/\boperator\b/.test(prevContToken.token.type) || /\bparen\b/.test(prevContToken.token.type))
               return this.$getIndent(this.$getLine(prevToken.row)) + tab;
            else
               return this.$getIndent(this.$getLine(prevToken.row));
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
               // return line that contains the brace, plus 1 indent level
               return this.$getIndent(this.$getLine(openBracePos.row)) + tab;
            }
            else
            {
               // return indent up to next token position
               var indentWidth = nextTokenPos.column;
               var tabsToUse = Math.floor(indentWidth / tabSize);
               var spacesToAdd = indentWidth - (tabSize * tabsToUse);
               var buffer = "";
               for (var i = 0; i < tabsToUse; i++)
                  buffer += tab;
               for (var j = 0; j < spacesToAdd; j++)
                  buffer += " ";
               return buffer;
            }
         }

         var firstToken = this.$findNextSignificantToken({row: 0, column: 0}, lastRow);
         if (firstToken)
            return this.$getIndent(this.$getLine(firstToken.row));
         else
            return "";
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

      return this.$getIndent(lastRow);
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
         if (!this.$statePattern || this.$statePattern.test(lineTokens.state))
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
         this.$invalidateRow(delta.range.start.row);
      }

      this.$invalidateScopeTreeFrom(delta.range.start);
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
      return /^\s*$/.test(token.value) || token.type === "comment";
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
