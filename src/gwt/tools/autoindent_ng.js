define(function(require, exports, module) {

var IndentManager = function(doc, tokenizer) {
   this.$doc = doc;
   this.$tokenizer = tokenizer;
   this.$tokens = [];
   this.$endStates = [];
   
   this.$doc.on('change', this.onChange);
   
};

(function () {

   this.getNextLineIndent = function(lastRow)
   {
      this.$tokenizeUpToRow(lastRow);
   };
   
   this.$tokenizeUpToRow = function(lastRow)
   {
      // Don't let lastRow be past the end of the document
      lastRow = Math.min(lastRow, this.$endStates.length - 1);
      
      var row = 0;
      while (true)
      {
         // Skip over rows that don't need to be updated (rows that have not been
         // invalidated, and are known not to be impacted by invalidated rows)
         for ( ; row <= lastRow && !(this.$endStates[row] === null); row++)
         {
         }

         // We may have exited the previous loop because we hit the end
         if (row > lastRow)
            return;
         
         // We're now at an invalidated row
         var state = (row === 0) ? 'start' : this.$endStates[row-1];
         for ( ; row <= lastRow; row++)
         {
            // Tokenize this line
            var lineTokens = this.$tokenizer.getLineTokens(this.$doc.getLine(row), state);
            this.$tokens[row] = lineTokens.tokens;
            state = lineTokens.state;
            
            // If this row ends with the same state we had cached, we know any following
            // rows will have up-to-date cached info, UNTIL we hit a row that has been
            // explicitly invalidated. We can break and go back to the top of the while-loop.
            if (state === this.$endStates[row] && state) // second clause is to prevent infinite loop
               break;
            
            
            this.$endStates[row] = state;
         }
         
         if (row > lastRow)
         {
            // If we got here, it means the last row we saw before we exited
            // was invalidated or impacted by an invalidated row. We need to
            // make sure the NEXT row doesn't get ignored next time the tokenizer
            // makes a pass.
            //
            // It's possible that "row" actually points past the last row of the
            // document, but that's OK, $invalidateRow() will just no-op.
            this.$invalidateRow(row);
            return;
         }
      }
   };

   this.onChange = function(evt)
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
      if (row < this.$tokens.length)
      {
         this.$tokens[row] = null;
         this.$endStates[row] = null;
      }
   };
   
   this.$insertNewRows = function(row, count)
   {
      var args = [row, 0];
      for (var i = 0; i < count; i++)
         args.push(null);
      this.$tokens.splice.apply(this.$tokens, args);
      this.$endStates.splice.apply(this.$endStates, args);
   };

}).call(IndentManager.prototype);

exports.IndentManager = IndentManager;

});