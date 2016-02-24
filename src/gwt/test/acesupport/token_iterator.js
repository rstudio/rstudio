var Range = require("ace/range").Range;

// Ace components
var Editor = require("ace/editor").Editor;
var EditSession = require("ace/edit_session").EditSession;
var Document = require("ace/document").Document;
require("mode/auto_brace_insert").setInsertMatching(true);

var TokenIterator = require("ace/token_iterator").TokenIterator;
require("mixins/token_iterator");

// Modes
var RMode = require("mode/r").Mode;

// Utility functions
Editor.prototype.clear = function() {

   var start = {
      row: 0,
      column: 0
   };

   var end = {
      row: this.getSession().getLength(),
      column: 0
   };

   this.getSession().remove(Range.fromPoints(start, end));
}

Editor.prototype.getContents = function() {
   return this.getSession().getDocument().getAllLines().join("\n");
}

Editor.prototype.setContents = function(text) {
    this.clear();
    this.insert(text);
};

// Initialize editor + provide aliases
var editor = ace.edit("editor");

function Module(editor, name) {

   QUnit.module(name, {
      beforeEach: function() {
         editor.clear();
      },

      afterEach: function() {
         editor.clear();
      }
   });
}

Module(editor, "Text");

QUnit.test("Insert text into the editor", function(assert) {
   editor.insert("abcdef");
   assert.equal(editor.getContents(), "abcdef");
});


Module(editor, "R");
editor.getSession().setMode(new RMode(false, editor.getSession()));

QUnit.test("TokenIterator works as expected", function(assert) {
    editor.insert("foo <- function(x) {\n  print(x)\n}");
    var iterator = new TokenIterator(editor.getSession());

    iterator.moveToPosition({row: 0, column: 0});
    assert.equal(iterator.getCurrentTokenRow(), 0, "First token lies at row 0");
    assert.equal(iterator.getCurrentTokenColumn(), 0, "First token lies on column 0");

    iterator.moveToNextToken();
    assert.equal(iterator.getCurrentTokenRow(), 0, "Second token lies on row 0");
    assert.equal(iterator.getCurrentTokenColumn(), 3, "Second token lies on column 3");

    var token = iterator.getCurrentToken();
    var prevToken = iterator.peekBwd(1);
    var failedToAdvance = false;
    do
    {
       if (token.row === prevToken.row &&
           token.column === prevToken.column)
       {
          failedToAdvance = true;
          break;
       }

       prevToken = token;

    } while ((token = iterator.moveToNextToken()));

    assert.ok(prevToken.value === "}", "The final token is a closing bracket (was '" + prevToken.value + "')");

});

QUnit.test("TokenIterator moves to position as expected", function(assert) {

    // NOTE: The column indices are written above the text just
    // as a means of double-checking the test.
    //
    //             012345678
    editor.insert("(((abc)))");

    var iterator = new TokenIterator(editor.getSession());

    iterator.moveToPosition({row: 0, column: 0});
    assert.equal(iterator.getCurrentToken().value, "(");

    iterator.moveToPosition({row: 0, column: 3});
    assert.equal(iterator.getCurrentToken().value, "(");

    iterator.moveToPosition({row: 0, column: 3}, true);
    assert.equal(iterator.getCurrentToken().value, "abc");

    iterator.moveToPosition({row: 0, column: 4});
    assert.equal(iterator.getCurrentToken().value, "abc");

    iterator.moveToPosition({row: 0, column: 4}, true);
    assert.equal(iterator.getCurrentToken().value, "abc");

    iterator.moveToPosition({row: 0, column: 5});
    assert.equal(iterator.getCurrentToken().value, "abc");

    iterator.moveToPosition({row: 0, column: 5}, true);
    assert.equal(iterator.getCurrentToken().value, "abc");

    iterator.moveToPosition({row: 0, column: 6});
    assert.equal(iterator.getCurrentToken().value, "abc");

    iterator.moveToPosition({row: 0, column: 6}, true);
    assert.equal(iterator.getCurrentToken().value, ")");

});

