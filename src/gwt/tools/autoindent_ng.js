define(function(require, exports, module) {

var IndentManager = function(doc, tokenizer) {
   this.$doc = doc;
   this.$tokenizer = tokenizer;
   this.$tokens = [];
   this.$endStates = [];
   
   this.$doc.on('change', this.$onDocChange);
   
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
      var assumeGood = true;
      for ( ; row <= lastRow; row++)
      {
         // No need to tokenize rows until we hit one that has been explicitly
         // invalidated.
         if (assumeGood && !(this.$endStates[row] === null))
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
         //
         // It's possible that "row" actually points past the last row of the
         // document, but that's OK, $invalidateRow() will just no-op.
         this.$invalidateRow(row);
      }
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
   
   this.$removeRows = function(row, count)
   {
      this.$tokens.splice(row, count);
      this.$endStates.splice(row, count);
   };

}).call(IndentManager.prototype);

exports.IndentManager = IndentManager;

});