define("mode/r_autoindent", function(require, exports, module) {

var IndentManager = function(doc, tokenizer) {
   this.$doc = doc;
   this.$tokenizer = tokenizer;
   this.$tokens = new Array(doc.getLength());
   this.$endStates = new Array(doc.getLength());

   var that = this;
   this.$doc.on('change', function(evt) {
      that.$onDocChange.apply(that, [evt]);
   });
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

   this.getNextLineIndent = function(lastRow)
   {
      var indent = lastRow < 0 ? "" 
                               : this.$getIndent(this.$doc.getLine(lastRow));

      if (!this.$tokenizeUpToRow(lastRow))
         return indent;


      var prevToken = this.$findPreviousSignificantToken({row: lastRow, column: this.$doc.getLine(lastRow).length},
                                                         lastRow - 10);
      if (prevToken
            && /\bparen\b/.test(prevToken.token.type)
            && /\)$/.test(prevToken.token.value))
      {
         var parens = 0;
         var lastPos = null;
         
         var matched = !this.$walkParens(prevToken.row, prevToken.row - 10, function(paren, pos)
         {
            lastPos = pos;
            if (/[\[({]/.test(paren))
               parens++;
            else
               parens--;

            // We've either completed a set of balanced parens, or possibly
            // the parens are invalid. In either case, stop.
            if (parens >= 0)
               return false;

            return true;
         });
         
         if (matched)
         {
            var preParenToken = this.$findPreviousSignificantToken(lastPos, 0);
            if (preParenToken && preParenToken.token.type === "keyword"
                  && /^(if|while|for|function)$/.test(preParenToken.token.value))
            {
               return this.$getIndent(this.$doc.getLine(preParenToken.row)) + "  ";
            }
         }
      }
      else if (prevToken
                  && prevToken.token.type === "keyword"
                  && prevToken.token.value === "repeat")
      {
         return this.$getIndent(this.$doc.getLine(prevToken.row)) + "  ";
      }


      var parens = [];
      var that = this;
      var openBrace = null;
      var openBracePos = null;
      if (!this.$walkParens(lastRow, 0, function(paren, pos)
      {
         if (/[\[({]/.test(paren))
         {
            if (parens.length == 0
                  || parens[parens.length - 1] != that.$complements[paren])
            {
               openBracePos = pos;
               openBrace = paren;

               // stop walking
               return false;
            }
            else
               parens.pop();
         }
         else
         {
            parens.push(paren);
         }
         return true;
      }))
      {
         var nextTokenPos = this.$findNextSignificantToken({
               row: openBracePos.row,
               column: openBracePos.column + 1
            }, lastRow);

         if (!nextTokenPos)
         {
            // TODO: return line that contains the brace, plus 1 indent level
            // TODO: If the token is beyond lastRow, ignore it
            indent = this.$getIndent(this.$doc.getLine(openBracePos.row)) + "  ";
         }
         else
         {
            // return indent up to next token position
            indent = (new Array(nextTokenPos.column + 1)).join(" ");
         }
      }
      else
      {
         indent = "";
      }

      
      //return indent;
      return indent;
   };
   
   this.$tokenizeUpToRow = function(lastRow)
   {
      console.log("Tokenizing up to and including " + lastRow);
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
         var lineTokens = this.$tokenizer.getLineTokens(this.$doc.getLine(row), state);
         this.$tokens[row] = lineTokens.tokens;

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
                     if (value.length == 1)
                     {
                        if (!fun(value, {row: startRow, column: tokens[i].column}))
                           return false;
                     }
                     else
                     {
                        for (var j = 0; j < value.length; j++)
                        {
                           if (!fun(value.charAt(j), {row: startRow, column: tokens[i].column + j}))
                              return false;
                        }
                     }
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
                     if (value.length == 1)
                     {
                        if (!fun(value, {row: startRow, column: tokens[i].column}))
                           return false;
                     }
                     else
                     {
                        for (var j = value.length - 1; j >= 0; j--)
                        {
                           if (!fun(value.charAt(j), {row: startRow, column: tokens[i].column + j}))
                              return false;
                        }
                     }
                  }
               }
            }
            return true;
         }).call(this);
      }
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
         var tokens = this.$filterWhitespaceAndComments(this.$tokens[row]);

         for (var i = 0; i < tokens.length; i++)
         {
            if (tokens[i].column + tokens[i].value.length > col)
            {
               return {
                  token: tokens[i], 
                  row: row, 
                  column: Math.max(tokens[i].column, col)
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
         var tokens = this.$filterWhitespaceAndComments(this.$tokens[row]);
         if (tokens.length == 0)
            continue;
         
         if (row != pos.row)
            return {
               row: row,
               column: tokens[tokens.length - 1].column,
               token: tokens[tokens.length - 1]
            };
         
         for (var i = tokens.length - 1; i >= 0; i--)
         {
            if (tokens[i].column < pos.column)
            {
               return {
                  row: row,
                  column: tokens[i].column,
                  token: tokens[i]
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
      return tokens.filter(function (t) {
         return !isWhitespaceOrComment(t);
      });
   };

}).call(IndentManager.prototype);

exports.IndentManager = IndentManager;

});
