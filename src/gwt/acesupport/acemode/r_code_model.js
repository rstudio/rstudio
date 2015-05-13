/*
 * r_code_model.js
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

define("mode/r_code_model", function(require, exports, module) {

var Range = require("ace/range").Range;
var TokenIterator = require("ace/token_iterator").TokenIterator;
var RTokenCursor = require("mode/token_cursor").RTokenCursor;
var Utils = require("mode/utils");

var $verticallyAlignFunctionArgs = false;

function comparePoints(pos1, pos2)
{
   if (pos1.row != pos2.row)
      return pos1.row - pos2.row;
   return pos1.column - pos2.column;
}

function isOneOf(object, array)
{
   for (var i = 0; i < array.length; i++)
      if (object === array[i])
         return true;
   return false;
}

var ScopeManager = require("mode/r_scope_tree").ScopeManager;
var ScopeNode = require("mode/r_scope_tree").ScopeNode;

var RCodeModel = function(session, tokenizer,
                          statePattern, codeBeginPattern, codeEndPattern) {

   this.$session = session;
   this.$doc = session.getDocument();
   this.$tokenizer = tokenizer;
   this.$tokens = new Array(this.$doc.getLength());
   this.$endStates = new Array(this.$doc.getLength());
   this.$statePattern = statePattern;
   this.$codeBeginPattern = codeBeginPattern;
   this.$codeEndPattern = codeEndPattern;
   this.$scopes = new ScopeManager(ScopeNode);

   var that = this;
   this.$doc.on('change', function(evt) {
      that.$onDocChange.apply(that, [evt]);
   });
   
};

(function () {

   var contains = Utils.contains;

   this.getTokenCursor = function() {
      return new RTokenCursor(this.$tokens, 0, 0, this);
   };

   this.$complements = {
      '(': ')',
      ')': '(',
      '[': ']',
      ']': '[',
      '{': '}',
      '}': '{'
   };

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

   // Find the associated function token from an open brace, e.g.
   //
   //   foo <- function(a, b, c) {
   //          ^<<<<<<<<<<<<<<<<<^
   function findAssocFuncToken(tokenCursor)
   {
      var clonedCursor = tokenCursor.cloneCursor();
      if (clonedCursor.currentValue() !== "{")
         return false;
      if (!clonedCursor.moveBackwardOverMatchingParens())
         return false;
      if (!clonedCursor.moveToPreviousToken())
         return false;
      if (!pFunction(clonedCursor.currentToken()))
         return false;

      tokenCursor.$row = clonedCursor.$row;
      tokenCursor.$offset = clonedCursor.$offset;
      return true;

   }

   // Determine whether the token cursor lies within the
   // argument list for a control flow statement, e.g.
   //
   //    if (foo &&
   //        (bar
   //         ^
   function isWithinControlFlowArgList(tokenCursor)
   {
      while (tokenCursor.findOpeningBracket("(") &&
             tokenCursor.moveToPreviousToken())
         if (isOneOf(
            tokenCursor.currentValue(),
            ["if", "for", "while"]))
             return true;

      return false;
   }

   // Move from the function token to the end of a function name.
   // Note that it is legal to define functions in multi-line strings,
   // hence the somewhat awkward name / interface.
   //
   //     "some function" <- function(a, b, c) {
   //                   ^~~~~^
   function moveFromFunctionTokenToEndOfFunctionName(tokenCursor)
   {
      var clonedCursor = tokenCursor.cloneCursor();
      if (!pFunction(clonedCursor.currentToken()))
         return false;
      if (!clonedCursor.moveToPreviousToken())
         return false;
      if (!pAssign(clonedCursor.currentToken()))
         return false;
      if (!clonedCursor.moveToPreviousToken())
         return false;

      tokenCursor.$row = clonedCursor.$row;
      tokenCursor.$offset = clonedCursor.$offset;
      return true;
   }

   this.getFunctionsInScope = function(pos) {
      this.$buildScopeTreeUpToRow(pos.row);
      return this.$scopes.getFunctionsInScope(pos);
   };

   this.getAllFunctionScopes = function(row) {
      if (typeof row === "undefined")
         row = this.$doc.getLength();
      this.$buildScopeTreeUpToRow(row);
      return this.$scopes.getAllFunctionScopes();
   };

   function pInfix(token)
   {
      return /\binfix\b/.test(token.type);
   }

   this.getDplyrJoinContextFromInfixChain = function(cursor)
   {
      var clone = cursor.cloneCursor();
      
      var token = "";
      if (clone.currentType() === "identifier")
         token = clone.currentValue();
      
      // We're expecting to be within e.g.
      //
      //    mtcars %>% semi_join(foo, by = c("a" =
      //                                           ^
      // Here, we get:
      // 1. The name of the 'right table' (foo), and
      // 2. The cursor position (left or right of '=')
      var cursorPos = "left";
      if (clone.currentValue() === "=")
         cursorPos = "right";

      if (clone.currentType() === "identifier")
      {
         if (!clone.moveToPreviousToken())
            return null;
         
         if (clone.currentValue() === "=")
            cursorPos = "right";
      }

      // Move to the first opening paren
      if (!clone.findOpeningBracket("(", true))
         return null;

      if (!clone.moveToPreviousToken())
         return null;

      // Look back until we find a 'join' verb
      while (clone.findOpeningBracket("(", true))
      {
         if (!clone.moveToPreviousToken())
            return null;

         if (!/_join$/.test(clone.currentValue()))
            continue;

         // If we get here, it looks like a dplyr join in a magrittr chain
         // get the data name and the join verb
         var verb = clone.currentValue();

         if (!clone.moveToNextToken())
            return null;

         if (!clone.moveToNextToken())
            return null;

         var rightData = clone.currentValue();

         var leftData = "";
         var data = this.moveToDataObjectFromInfixChain(clone);
         if (data === false)
            return null;
         
         leftData = clone.currentValue();
         
         return {
            "token": token,
            "leftData": leftData,
            "rightData": rightData,
            "verb": verb,
            "cursorPos": cursorPos
         };
      }
      return null;
   };

   // If the token cursor lies within an infix chain, try to retrieve:
   // 1. The data object name, and
   // 2. Any custom variable names (e.g. set through 'mutate', 'summarise')
   this.getDataFromInfixChain = function(tokenCursor)
   {
      var data = this.moveToDataObjectFromInfixChain(tokenCursor);
      
      var additionalArgs = [];
      var excludeArgs = [];
      var name = "";
      var excludeArgsFromObject = false;
      if (data !== false)
      {
         if (data.excludeArgsFromObject)
            excludeArgsFromObject = data.excludeArgsFromObject;
         
         name = tokenCursor.currentValue();
         additionalArgs = data.additionalArgs;
         excludeArgs = data.excludeArgs;
      }

      return {
         "name": name,
         "additionalArgs": additionalArgs,
         "excludeArgs": excludeArgs,
         "excludeArgsFromObject": excludeArgsFromObject
      };
      
   };

   var $dplyrMutaterVerbs = [
      "mutate", "summarise", "summarize", "rename", "transmute",
      "select", "rename_vars",
      "inner_join", "left_join", "right_join", "semi_join", "anti_join",
      "outer_join", "full_join"
   ];

   // Add arguments from a function call in a chain.
   //
   //     select(x, y = 1)
   //     ^~~~~~~|~~|~~~~x
   var addDplyrArguments = function(cursor, data, limit, fnName)
   {
      if (!cursor.moveToNextToken())
         return false;

      if (cursor.currentValue() !== "(")
         return false;

      if (!cursor.moveToNextToken())
         return false;

      if (cursor.currentValue() === ")")
         return false;

      if (cursor.currentType() === "identifier")
         data.additionalArgs.push(cursor.currentValue());
      
      if (fnName === "rename")
      {
         if (!cursor.moveToNextToken())
            return false;
         data.excludeArgs.push(cursor.currentValue());
      }

      if (fnName === "select")
      {
         data.excludeArgsFromObject = true;
      }

      do
      {
         if (cursor.currentValue() === ")")
            break;
         
         if ((cursor.$row > limit.$row) ||
             (cursor.$row === limit.$row && cursor.$offset >= limit.$offset))
            break;
         
         if (cursor.fwdToMatchingToken())
         {
            if (!cursor.moveToNextToken())
               break;
            continue;
         }

         if (cursor.currentValue() === ",")
         {
            if (!cursor.moveToNextToken())
               return false;

            if (cursor.currentType() === "identifier")
               data.additionalArgs.push(cursor.currentValue());
            
            if (!cursor.moveToNextToken())
               return false;

            if (cursor.currentValue() === "=")
            {
               if (isOneOf(fnName, ["rename", "rename_vars"]))
               {
                  if (!cursor.moveToNextToken())
                     return false;
                  if (cursor.currentType() === "identifier")
                     data.excludeArgs.push(cursor.currentValue());
               }

            }
            

         }
         
      } while (cursor.moveToNextToken());

      return true;
      
   };

   var findChainScope = function(cursor)
   {
      var clone = cursor.cloneCursor();
      while (clone.findOpeningBracket("(", false))
      {
         // Move off of the opening paren
         if (!clone.moveToPreviousToken())
            return false;

         // Move off of identifier
         if (!clone.moveToPreviousToken())
            return false;

         // Move over '::' qualifiers
         if (clone.currentValue() === ":")
         {
            while (clone.currentValue() === ":")
               if (!clone.moveToPreviousToken())
                  return false;

            // Move off of identifier
            if (!clone.moveToPreviousToken())
               return false;
         }

         // If it's an infix operator, we use this scope
         // Ensure it's a '%%' operator (allow for other pipes)
         if (pInfix(clone.currentToken()))
         {
            cursor.$row = clone.$row;
            cursor.$offset = clone.$offset;
            return true;
         }

         // keep trying!
         
      }

      // give up
      return false;
      
   };

   // Attempt to move a token cursor from a function call within
   // a chain back to the starting data object.
   //
   //     df %.% foo %>>% bar() %>% baz(foo,
   //     ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~^
   this.moveToDataObjectFromInfixChain = function(tokenCursor)
   {
      // Find an opening paren associated with the nearest chain,
      // Find the outermost opening paren
      var clone = tokenCursor.cloneCursor();
      if (!findChainScope(clone))
         return false;

      // Fill custom args
      var data = {
         additionalArgs: [],
         excludeArgs: []
      };
      
      // Repeat the walk -- keep walking as we can find '%%'
      while (true)
      {
         if (clone.$row === 0 && clone.$offset === 0)
         {
            tokenCursor.$row = 0;
            tokenCursor.$offset = 0;
            return data;
         }

         // Move over parens to identifier if necessary
         //
         //    foo(bar, baz)
         //    ^~~~~~~~~~~~^
         clone.moveBackwardOverMatchingParens();

         // Move off of '%>%' (or '(') onto identifier
         if (!clone.moveToPreviousToken())
            return false;

         // If this identifier is a dplyr 'mutate'r, then parse
         // those variables.
         var value = clone.currentValue();
         if (contains($dplyrMutaterVerbs, value))
            addDplyrArguments(clone.cloneCursor(), data, tokenCursor, value);

         // Move off of identifier, on to new infix operator.
         // Note that we may already be at the start of the document,
         // so check for that.
         if (!clone.moveToPreviousToken())
         {
            if (clone.$row === 0 && clone.$offset === 0)
            {
               tokenCursor.$row = 0;
               tokenCursor.$offset = 0;
               return data;
            }
            return false;
         }

         // Move over '::' qualifiers
         if (clone.currentValue() === ":")
         {
            while (clone.currentValue() === ":")
               if (!clone.moveToPreviousToken())
                  return false;

            // Move off of identifier
            if (!clone.moveToPreviousToken())
               return false;
         }

         // We should be on an infix operator now. If we are, keep walking;
         // if not, then the identifier we care about is the next token.
         if (!pInfix(clone.currentToken()))
            break;
      }

      if (!clone.moveToNextToken())
         return false;

      tokenCursor.$row = clone.$row;
      tokenCursor.$offset = clone.$offset;
      return data;
   };

   function addForInToken(tokenCursor, scopedVariables)
   {
      var clone = tokenCursor.cloneCursor();
      if (clone.currentValue() !== "for")
         return false;

      if (!clone.moveToNextToken())
         return false;

      if (clone.currentValue() !== "(")
         return false;

      if (!clone.moveToNextToken())
         return false;

      var maybeForInVariable = clone.currentValue();
      if (!clone.moveToNextToken())
         return false;

      if (clone.currentValue() !== "in")
         return false;

      scopedVariables[maybeForInVariable] = "variable";
      return true;
   }

   // Moves out of an argument list for a function, e.g.
   //
   //     x <- function(a, b|
   //          ^~~~~~~~~~~~~^
   //
   // The cursor will be placed on the associated 'function' token
   // on success, and unmoved on failure.
   var moveOutOfArgList = function(tokenCursor)
   {
      var clone = tokenCursor.cloneCursor();
      if (!clone.findOpeningBracket("(", true))
         return false;

      if (!clone.moveToPreviousToken())
         return false;
      
      if (clone.currentValue() !== "function")
         return false;

      tokenCursor.$row = clone.$row;
      tokenCursor.$offset = clone.$offset;
      return true;
   };

   this.getVariablesInScope = function(pos) {

      var tokenCursor = this.getTokenCursor();
      if (!tokenCursor.moveToPosition(pos))
         return [];

      // If we're in a function call, avoid grabbing the parameters and
      // function name itself within the call. This is so that in e.g.
      //
      //     func <- foo(x = 1, y = 2, |
      //
      // we don't pick up 'func', 'x', and 'y' as potential completions
      // since they will not be valid in all contexts
      if (moveOutOfArgList(tokenCursor))
         if (moveFromFunctionTokenToEndOfFunctionName(tokenCursor))
            if (tokenCursor.findStartOfEvaluationContext())
               {} // previous statements will move the cursor as necessary

      var scopedVariables = {};
      do
      {
         if (tokenCursor.bwdToMatchingToken())
            continue;

         // Handle 'for (x in bar)'
         addForInToken(tokenCursor, scopedVariables);
         
         // Default -- assignment case
         if (pAssign(tokenCursor.currentToken()))
         {
            // Check to see if this is a function (simple check)
            var type = "variable";
            var functionCursor = tokenCursor.cloneCursor();
            if (functionCursor.moveToNextToken())
            {
               if (functionCursor.currentValue() === "function")
               {
                  type = "function";
               }
            }
            
            var clone = tokenCursor.cloneCursor();
            if (!clone.moveToPreviousToken()) continue;
            
            if (pIdentifier(clone.currentToken()))
            {
               var arg = clone.currentValue();
               scopedVariables[arg] = type;
               continue;
            }
            
         }
      } while (tokenCursor.moveToPreviousToken());

      var result = [];
      for (var key in scopedVariables)
         result.push({
            "token": key,
            "type": scopedVariables[key]
         });
      
      result.sort();
      return result;
      
   };

   // Get function arguments, starting at the start of a function definition, e.g.
   //
   // x <- function(a = 1, b = 2, c = list(a = 1, b = 2), ...)
   //      ?~~~~~~~?^~~~~~~^~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~^~~|
   function $getFunctionArgs(tokenCursor)
   {
      if (pFunction(tokenCursor.currentToken()))
         if (!tokenCursor.moveToNextToken())
            return [];

      if (tokenCursor.currentValue() === "(")
         if (!tokenCursor.moveToNextToken())
            return [];

      if (tokenCursor.currentValue() === ")")
         return [];

      var functionArgs = [];
      if (pIdentifier(tokenCursor.currentToken()))
         functionArgs.push(tokenCursor.currentValue());
      
      while (tokenCursor.moveToNextToken())
      {
         if (tokenCursor.fwdToMatchingToken())
            continue;

         if (tokenCursor.currentValue() === ")")
            break;

         // Yuck: '...' and ',' can get tokenized together as
         // text. All we can really do is ask if a particular token is
         // type 'text' and ends with a comma.
         // Once we encounter such a token, we look ahead to find an
         // identifier (it signifies an argument name)
         if (tokenCursor.currentValue() === ",")
         {
            while (tokenCursor.currentValue() === ",")
               if (!tokenCursor.moveToNextToken())
                  break;
            
            if (pIdentifier(tokenCursor.currentToken()))
               functionArgs.push(tokenCursor.currentValue());
         }
      }
      return functionArgs;
      
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

         if (typeof reOptions === "undefined")
            return "";
         
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
      var maxRow = Math.min(maxrow + 30, this.$doc.getLength() - 1);
      this.$tokenizeUpToRow(maxRow);

      var tokenCursor = this.getTokenCursor();
      if (!tokenCursor.seekToNearestToken(this.$scopes.parsePos, maxRow))
         return;

      do
      {
         this.$scopes.parsePos = tokenCursor.currentPosition();
         this.$scopes.parsePos.column += tokenCursor.currentValue().length;

         var tokenType = tokenCursor.currentToken().type;
         if (/\bsectionhead\b/.test(tokenType))
         {
            var sectionHeadMatch = /^#+'?[-=#\s]*(.*?)\s*[-=#]+\s*$/.exec(
                  tokenCursor.currentValue());

            var label = "" + sectionHeadMatch[1];
            if (label.length === 0)
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
            var bracePos = localCursor.currentPosition();
            
            var startPos;
            if (findAssocFuncToken(localCursor))
            {
               var argsCursor = localCursor.cloneCursor();
               argsCursor.moveToNextToken();
               var argsStartPos = argsCursor.currentPosition();

               var functionName = null;
               if (moveFromFunctionTokenToEndOfFunctionName(localCursor))
               {
                  var functionEndCursor = localCursor.cloneCursor();
                  if (localCursor.findStartOfEvaluationContext())
                  {
                     var functionStartPos = localCursor.currentPosition();
                     var functionEndPos = functionEndCursor.currentPosition();
                     functionName = this.$doc.getTextRange(new Range(
                        functionStartPos.row,
                        functionStartPos.column,
                        functionEndPos.row,
                        functionEndPos.column + functionEndCursor.currentValue().length
                     ));
                  }
               }
               
               startPos = localCursor.currentPosition();
               if (localCursor.isFirstSignificantTokenOnLine())
                  startPos.column = 0;

               var functionArgsString = this.$doc.getTextRange(new Range(
                  argsStartPos.row, argsStartPos.column,
                  bracePos.row, bracePos.column
               ));

               var functionLabel;
               if (functionName === null)
                  functionLabel = $normalizeWhitespace("<function>" + functionArgsString);
               else
                  functionLabel = $normalizeWhitespace(functionName + functionArgsString);

               // Obtain the function arguments by walking through the tokens
               var functionArgs = $getFunctionArgs(argsCursor);

               this.$scopes.onFunctionScopeStart(functionLabel,
                                                 startPos,
                                                 tokenCursor.currentPosition(),
                                                 functionName,
                                                 functionArgs);
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
         var tokenCursor = this.getTokenCursor();
         if (tokenCursor.seekToNearestToken(pos, pos.row)
                   && tokenCursor.currentValue() == "{"
               && tokenCursor.moveBackwardOverMatchingParens())
         {
            return this.$getIndent(this.$getLine(tokenCursor.currentPosition().row));
         }
      }

      return this.$getIndent(this.$getLine(pos.row));
   };

   this.getIndentForRow = function(row)
   {
      return this.getNextLineIndent(
         "start",
         this.$getLine(row),
         this.$session.getTabString(),
         row
      );
   };

   // NOTE: 'row' is an optional parameter, and is not used by default
   // on enter keypresses. When unset, we attempt to indent based on
   // the cursor position (which is what we want for 'enter'
   // keypresses).  However, for reindentation of particular lines (or
   // blocks), we need the row parameter in order to choose which row
   // we wish to reindent.
   this.getNextLineIndent = function(state, line, tab, row)
   {
      if (Utils.endsWith(state, "qstring"))
         return "";

      // NOTE: Pressing enter will already have moved the cursor to
      // the next row, so we need to push that back a single row.
      if (typeof row !== "number")
         row = this.$session.getSelection().getCursor().row - 1;

      var tabSize = this.$session.getTabSize();
      var tabAsSpaces = new Array(tabSize + 1).join(" ");

      // This lineOverrides nonsense is necessary because the line has not 
      // changed in the real document yet. We need to simulate it by replacing
      // the real line with the `line` param, and when we finish with this
      // method, undo the damage and invalidate the row.
      // To repro the problem without using lineOverrides, comment out this
      // block of code, and in the editor hit Enter in the middle of a line 
      // that contains a }.
      this.$lineOverrides = null;
      if (!(this.$doc.getLine(row) === line))
      {
         this.$lineOverrides = {};
         this.$lineOverrides[row] = line;
         this.$invalidateRow(row);
      }
      
      try
      {
         var defaultIndent = row < 0 ?
                "" : 
                this.$getIndent(this.$getLine(row));

         // jcheng 12/7/2013: It doesn't look to me like $tokenizeUpToRow can return
         // anything but true, at least not today.
         if (!this.$tokenizeUpToRow(row))
            return defaultIndent;

         // The significant token (no whitespace, comments) that most immediately
         // precedes this line. We don't look back further than 10 rows or so for
         // performance reasons.
         var startPos = {
            row: row,
            column: this.$getLine(row).length
         };

         var prevToken = this.$findPreviousSignificantToken(
            startPos,
            row - 10
         );

         // Used to add extra whitspace if the next line is a continuation of the
         // previous line (i.e. the last significant token is a binary operator).
         var continuationIndent = "";
         var startedOnOperator = false;

         if (prevToken &&
             /\boperator\b/.test(prevToken.token.type) &&
             !/\bparen\b/.test(prevToken.token.type))
         {
            // Fix issue 2579: If the previous significant token is an operator
            // (commonly, "+" when used with ggplot) then this line is a
            // continuation of an expression that was started on a previous
            // line. This line's indent should then be whatever would normally
            // be used for a complete statement starting here, plus a tab.
            continuationIndent = tab;
            startedOnOperator = true;
         }

         else if (prevToken
               && /\bparen\b/.test(prevToken.token.type)
               && /\)$/.test(prevToken.token.value))
         {
            // The previous token was a close-paren ")". Check if this is an
            // if/while/for/function without braces, in which case we need to
            // take the indentation of the keyword and indent by one level.
            //
            // Example:
            // if (identical(foo, 1) &&
            //     isTRUE(bar) &&
            //     (!is.null(baz) && !is.na(baz)))
            //   |
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
            // Check if this is a "repeat" or (more commonly) "else" without
            // braces, in which case we need to take the indent of the else/repeat
            // and increase by one level.
            return this.$getIndent(this.$getLine(prevToken.row)) + tab;
         }

         // Walk backwards looking for an open paren, square bracket, curly
         // brace, or assignment token. We use the first found token to provide
         // context for the indentation.
         var tokenCursor = this.getTokenCursor();

         // moveToPosition can fail if there are no tokens previous to
         // the cursor
         if (!tokenCursor.moveToPosition(startPos))
            return "";

         // The first loop looks for an open brace for indentation.
         do
         {
            var currentValue = tokenCursor.currentValue();

            if (tokenCursor.isAtStartOfNewExpression(false))
            {
               return this.$getIndent(
                  this.$doc.getLine(tokenCursor.$row)
               ) + continuationIndent;
            }
             
            // Walk over matching braces ('()', '{}', '[]')
            if (tokenCursor.bwdToMatchingToken())
               continue;

            // If we found a '{', we break out and loop back -- this is because
            // we may want to indent either on a '<-' token or on a '{'
            // token.
            if (currentValue === "{")
               break;

            // If we find an open parenthesis or bracket, we
            // can use this to provide the indentation context.
            if (contains(["[", "("], currentValue))
            {
               var openBracePos = tokenCursor.currentPosition();
               var nextTokenPos = null;

               if ($verticallyAlignFunctionArgs) {
                  // If the user has selected
                  // verticallyAlignFunctionArgs mode in the prefs,
                  // for example:
                  //
                  // soDomethingAwesome(a = 1,
                  //                    b = 2,
                  //                    c = 3)
                  //
                  // Then we simply follow the example of the next significant
                  // token. BTW implies that this mode also supports this:
                  //
                  // soDomethingAwesome(
                  //   a = 1,
                  //   b = 2,
                  //   c = 3)
                  //
                  // But not this:
                  //
                  // soDomethingAwesome(a = 1,
                  //   b = 2,
                  //   c = 3)
                  nextTokenPos = this.$findNextSignificantToken(
                     {
                        row: openBracePos.row,
                        column: openBracePos.column + 1
                     }, row);
               }

               if (!nextTokenPos)
               {
                  // Either there wasn't a significant token between the new
                  // line and the previous open brace, or, we're not in
                  // vertical argument alignment mode. Either way, we need
                  // to just indent one level from the open brace's level.
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
                  var result = leadingIndent + buffer;

                  // Compute the size of the indent in spaces (e.g. if a tab
                  // is 4 spaces, and result is "\t\t ", the size is 9)
                  var resultSize = result.replace("\t", tabAsSpaces).length;

                  // Sometimes even though verticallyAlignFunctionArgs is used,
                  // the user chooses to manually "break the rules" and use the
                  // non-aligned style, like so:
                  //
                  // plot(foo,
                  //   bar, baz,
                  //
                  // Without the below loop, hitting Enter after "baz," causes
                  // the cursor to end up aligned with foo. The loop simply
                  // replaces the indentation with the minimal indentation.
                  //
                  // TODO: Perhaps we can skip the above few lines of code if
                  // there are other lines present
                  var thisIndent;
                  for (var i = nextTokenPos.row + 1; i <= row; i++) {
                     // If a line contains only whitespace, it doesn't count
                     if (!/[^\s]/.test(this.$getLine(i)))
                        continue;
                     // If this line is is a continuation of a multi-line string, 
                     // ignore it.
                     var rowEndState = this.$endStates[i-1];
                     if (rowEndState === "qstring" || rowEndState === "qqstring") 
                        continue;
                     thisIndent = this.$getLine(i).replace(/[^\s].*$/, '');
                     var thisIndentSize = thisIndent.replace("\t", tabAsSpaces).length;
                     if (thisIndentSize < resultSize) {
                        result = thisIndent;
                        resultSize = thisIndentSize;
                     }
                  }

                  // We want to tweak vertical alignment; e.g. in this
                  // case:
                  //
                  //    if (foo &&
                  //        |
                  //
                  // vs.
                  //
                  //    plot(x +
                  //             |
                  //
                  // Ie, normally, we might want a continuation indent if
                  // the line ended with an operator; however, in some
                  // cases (notably with multi-line if statements) we
                  // would prefer not including that indentation.
                  if (isWithinControlFlowArgList(tokenCursor))
                     return result;
                  else
                     return result + continuationIndent;

               }

            }

         } while (tokenCursor.moveToPreviousToken());

         // If we got here, either the scope is provided by a '{'
         // or we failed otherwise. For '{' scopes, we may want to
         // short-circuit and indent based on a '<-', hence the second
         // pass through here.
         if (!tokenCursor.moveToPosition(startPos))
            return "";

         do
         {
            // Walk over matching parens.
            if (tokenCursor.bwdToMatchingToken())
               continue;
            
            // If we find an open brace, use its associated indentation
            // plus a tab.
            if (tokenCursor.currentValue() === "{")
            {
               return this.getIndentForOpenBrace(
                  tokenCursor.currentPosition()
               ) + tab + continuationIndent;
            }

            // If we found an assignment token, use that for indentation
            if (pAssign(tokenCursor.currentToken()))
            {
               while (pAssign(tokenCursor.currentToken()))
               {
                  // Move off of the assignment token
                  if (!tokenCursor.moveToPreviousToken())
                     break;

                  if (!tokenCursor.findStartOfEvaluationContext())
                     break;

                  // Make sure this isn't the only assignment within a 'naked'
                  // control flow section
                  //
                  //    if (foo)
                  //        x <- 1
                  //
                  // In such cases, we rely on the 'naked' control identifier
                  // to provide the appropriate indentation.
                  var clone = tokenCursor.cloneCursor();
                  if (clone.moveToPreviousToken())
                  {
                     if (clone.currentValue() === "else" ||
                         clone.currentValue() === "repeat")
                     {
                        return this.$getIndent(
                           this.$doc.getLine(clone.$row)
                        ) + continuationIndent + continuationIndent;
                     }

                     var tokenIsClosingParen = clone.currentValue() === ")";
                     if (tokenIsClosingParen &&
                         clone.bwdToMatchingToken() &&
                         clone.moveToPreviousToken())
                     {
                        var currentValue = clone.currentValue();
                        if (contains(
                           ["if", "for", "while", "repeat", "else"],
                           currentValue
                        ))
                        {
                           return this.$getIndent(
                              this.$doc.getLine(clone.$row)
                           ) + continuationIndent + continuationIndent;
                        }
                     }
                  }

                  // If the previous token is an assignment operator,
                  // move on to it
                  if (pAssign(tokenCursor.peekBwd().currentToken()))
                     tokenCursor.moveToPreviousToken();

               }

               // We broke out of the loop; we should be on the
               // appropriate line to provide for indentation now.
               return this.$getIndent(
                  this.$getLine(tokenCursor.$row)
               ) + continuationIndent;
            }

         } while (tokenCursor.moveToPreviousToken());

         // Fix some edge-case indentation issues, mainly for naked
         // 'if' and 'else' blocks.
         if (startedOnOperator)
         {
            var maxTokensToWalk = 20;
            var count = 0;
            
            tokenCursor = this.getTokenCursor();
            tokenCursor.moveToPosition(startPos);

            // Move off of the operator
            tokenCursor.moveToPreviousToken();
               
            do
            {
               // If we encounter an 'if' or 'else' statement, add to
               // the continuation indent
               if (isOneOf(tokenCursor.currentValue(), ["if", "else"]))
               {
                  continuationIndent += tab;
                  break;
               }
               
               // If we're on a constant, then we need to find an
               // operator beforehand, or give up.
               if (/\bconstant\b|\bidentifier\b/.test(tokenCursor.currentType()))
               {

                  if (!tokenCursor.moveToPreviousToken())
                     break;

                  // Check if we're already on an if / else
                  if (isOneOf(tokenCursor.currentValue(), ["if", "else"]))
                  {
                     continuationIndent += tab;
                     break;
                  }

                  // If we're on a ')', check if it's associated with an 'if'
                  if (tokenCursor.currentValue() === ")")
                  {
                     if (!tokenCursor.bwdToMatchingToken())
                        break;

                     if (!tokenCursor.moveToPreviousToken())
                        break;

                     if (isOneOf(tokenCursor.currentValue(), ["if", "else"]))
                     {
                        continuationIndent += tab;
                        break;
                     }

                  }

                  if (!/\boperator\b/.test(tokenCursor.currentType()))
                     break;

                  continue;
               }
               
               // Move over a generic 'evaluation', e.g.
               // foo::bar()[1]
               if (!tokenCursor.findStartOfEvaluationContext())
                  break;

            } while (tokenCursor.moveToPreviousToken() &&
                     count++ < maxTokensToWalk);
         }

         // All else fails -- just indent based on the first token.
         var firstToken = this.$findNextSignificantToken(
            {row: 0, column: 0},
            row
         );
         
         if (firstToken)
            return this.$getIndent(
               this.$getLine(firstToken.row)
            ) + continuationIndent;
         else
            return "" + continuationIndent;
      }
      finally
      {
         if (this.$lineOverrides)
         {
            this.$lineOverrides = null;
            this.$invalidateRow(row);
         }
      }
   };

   this.getBraceIndent = function(row)
   {
      var tokenCursor = this.getTokenCursor();
      var pos = {
         row: row,
         column: this.$getLine(row).length
      };

      if (!tokenCursor.moveToPosition(pos))
         return "";

      if (tokenCursor.currentValue() === ")")
      {
         if (tokenCursor.bwdToMatchingToken() &&
             tokenCursor.moveToPreviousToken())
         {
            var preParenValue = tokenCursor.currentValue();
            if (isOneOf(preParenValue, ["if", "while", "for", "function"]))
            {
               return this.$getIndent(this.$getLine(tokenCursor.$row));
            }
         }
      }
      else if (isOneOf(tokenCursor.currentValue(),
                       ["else", "repeat", "<-", "<<-", "="]) ||
        tokenCursor.currentType().indexOf("infix") !== -1 ||
        tokenCursor.currentType() === "keyword.operator")
      {
         return this.$getIndent(this.$getLine(tokenCursor.$row));
      }

      return this.getIndentForRow(row);
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

         var state = (row === 0) ? 'start' : this.$endStates[row - 1];
         var line = this.$getLine(row);
         var lineTokens = this.$tokenizer.getLineTokens(line, state);

         if (!this.$statePattern ||
             this.$statePattern.test(lineTokens.state) ||
             this.$statePattern.test(state))
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

   this.findNextSignificantToken = function(pos)
   {
	   return this.$findNextSignificantToken(pos, this.$tokens.length - 1);
   };
   
   this.$findPreviousSignificantToken = function(pos, firstRow)
   {
      if (this.$tokens.length === 0)
         return null;
      firstRow = Math.max(0, firstRow);
      
      var row = Math.min(pos.row, this.$tokens.length - 1);
      for ( ; row >= firstRow; row--)
      {
         var tokens = this.$tokens[row];
         if (tokens.length === 0)
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

exports.setVerticallyAlignFunctionArgs = function(verticallyAlign) {
   $verticallyAlignFunctionArgs = verticallyAlign;
};

exports.getVerticallyAlignFunctionArgs = function() {
   return $verticallyAlignFunctionArgs;
};

});
