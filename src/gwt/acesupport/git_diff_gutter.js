/*
 * git_diff_gutter.js
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

// Marks lines that differ from a base version of the document (the Git index)
// in the editor gutter, and lets the user step through, peek at, and revert
// those changes. The diff itself comes from Ace's ext/diff provider, which
// must be loaded before the first update.
//
// Line splitting matches Ace's Document, and an empty document counts as
// having no lines (Ace represents it as one empty line), so that typing into
// an empty file reads as an addition and emptying a file as a deletion.

define("rstudio/git_diff_gutter", ["require", "exports", "module"], function(require, exports, module) {

var dom = require("ace/lib/dom");
var Range = require("ace/range").Range;

var GUTTER_CLASS = "ace_git-gutter";

var ADDED_CLASS         = "ace_git-added";
var MODIFIED_CLASS      = "ace_git-modified";
var DELETED_CLASS       = "ace_git-deleted";
var DELETED_BELOW_CLASS = "ace_git-deleted-below";

var CELL_CLASSES = [ADDED_CLASS, MODIFIED_CLASS, DELETED_CLASS, DELETED_BELOW_CLASS];

var PEEK_CLASS = "ace_git-peek";

// The marker sits at the right edge of a gutter cell, in padding the gutter
// gains while attached (see themeStyles.css) so that it lies beyond Ace's
// fold arrow; a click there opens the peek instead of reaching Ace (and the
// breakpoint toggle behind it).
var MARKER_HIT_WIDTH = 6;

// An edit is diffed before the frame that paints it: the diff is queued as a
// microtask, so the markers move with the keystroke, while the many change
// events of one operation (replace all, a reformat) are diffed once, after
// it. Only the lines between the first and last difference are handed to the
// provider (see trimCommonLines), so a diff usually takes well under a
// millisecond; a document whose last diff took longer than SLOW_DIFF_MS is
// instead diffed once typing pauses.
var SLOW_DIFF_MS = 20;
var SLOW_UPDATE_DELAY_MS = 150;

// Bail out on pathological inputs rather than hang the UI thread. The
// provider does not say when it gave up; a timed-out line alignment comes
// back as one change spanning both documents, with the budget spent (see
// computeChunks for how that is told apart from a document that did change
// throughout).
var MAX_COMPUTATION_MS = 250;

// Peek text when none is supplied (the IDE passes translated strings in).
var DEFAULT_STRINGS = {
   removedLines: function(n) { return "Removed " + pluralize(n, "line"); },
   addedLines:   function(n) { return "Added " + pluralize(n, "line"); },
   changedLines: function(n) { return "Changed " + pluralize(n, "line") + "; previously:"; },
   changedLinesShort: function(n) { return "Changed " + pluralize(n, "line"); },
   revert: "Revert",
   close:  "Close"
};

// Mirrors how the editor loads a document (AceEditor.doSetCode strips
// escape characters) and how Ace's Document splits lines.
function splitLines(text) {
   text = text.replace(/\u001b/g, "");
   return text === "" ? [] : text.split(/\r\n|\r|\n/);
}

// Only equality matters to the diff, so whitespace-insensitive comparison
// (git diff -w) is a matter of what the provider gets to compare.
function stripWhitespace(line) {
   return line.replace(/\s+/g, "");
}

// The lines shared by both ends of the two documents, which the diff can
// skip. The provider aligns smaller inputs (under 1,700 lines in total) with
// a quadratic dynamic-programming pass, so a whole 300-800 line script costs
// 3-20ms per keystroke; the lines between the first and last difference are
// usually a handful. The matched suffix stops short of the prefix so that no
// line is claimed by both.
function trimCommonLines(base, lines) {
   var n = Math.min(base.length, lines.length);

   var prefix = 0;
   while (prefix < n && base[prefix] === lines[prefix])
      prefix++;

   var suffix = 0;
   while (suffix < n - prefix &&
          base[base.length - 1 - suffix] === lines[lines.length - 1 - suffix])
      suffix++;

   return { prefix: prefix, suffix: suffix };
}

// Stands a token in for each line, the same token for identical lines. The
// provider refines each change character by character, which is quadratic
// in the characters of the change and of no use here (only the line ranges
// are read), so a large replaced block spends the whole budget there; on
// one-character tokens that work is negligible. The tokens come from the CJK
// Unified Ideographs block, none of them whitespace: one code point per
// distinct line, two once there are more than 20,992. The indentation goes
// with the rest of the text, so the provider cannot use it to place a chunk
// that could sit at more than one row.
var TOKEN_FIRST = 0x4E00;
var TOKEN_COUNT = 0x9FFF - TOKEN_FIRST + 1;

function tokenFor(id) {
   var token = "";
   do
   {
      token += String.fromCharCode(TOKEN_FIRST + id % TOKEN_COUNT);
      id = Math.floor(id / TOKEN_COUNT);
   }
   while (id > 0);

   return token;
}

function tokenizeLines(base, lines) {
   var ids = new Map();
   var tokenize = function(line) {
      var id = ids.get(line);
      if (id == null)
      {
         id = ids.size;
         ids.set(line, id);
      }
      return tokenFor(id);
   };

   return { base: base.map(tokenize), lines: lines.map(tokenize) };
}

function offsetRange(range, rows) {
   return new Range(range.start.row + rows, range.start.column, range.end.row + rows, range.end.column);
}

// Moves a diff computed on a slice of the lines back to document rows. The
// character ranges within a change (shown by the side-by-side view) move
// with their lines.
function offsetChunks(chunks, rows) {
   if (rows === 0)
      return chunks;

   return chunks.map(function(chunk) {
      var result = {
         old: offsetRange(chunk.old, rows),
         new: offsetRange(chunk.new, rows)
      };

      if (chunk.charChanges)
      {
         result.charChanges = chunk.charChanges.map(function(change) {
            return { old: offsetRange(change.old, rows), new: offsetRange(change.new, rows) };
         });
      }

      return result;
   });
}

// The character ranges of a diff over tokens describe the tokens, not the
// text; only the line ranges carry over.
function dropCharChanges(chunks) {
   return chunks.map(function(chunk) {
      return { old: chunk.old, new: chunk.new };
   });
}

function pluralize(n, noun) {
   return n + " " + noun + (n === 1 ? "" : "s");
}

function isDeletion(chunk) {
   return chunk.new.start.row === chunk.new.end.row;
}

function isAddition(chunk) {
   return chunk.old.start.row === chunk.old.end.row;
}

function isWholeDocument(chunks, base, lines) {
   if (chunks.length !== 1)
      return false;

   var chunk = chunks[0];
   return chunk.old.start.row === 0 && chunk.old.end.row === base.length &&
          chunk.new.start.row === 0 && chunk.new.end.row === lines.length;
}

function sameChunk(a, b) {
   return a.old.start.row === b.old.start.row &&
          a.old.end.row === b.old.end.row &&
          a.new.start.row === b.new.start.row &&
          a.new.end.row === b.new.end.row;
}

var GitDiffGutter = function(editor, strings) {
   this.editor = editor;
   this.strings = strings || DEFAULT_STRINGS;
   this.baseText = null;
   this.baseLines = null;
   this.strippedBaseLines = null;
   this.chunks = [];
   this.rowClasses = {};
   this.updateTimer = null;
   this.updatePending = false;
   this.lastDiffMs = 0;
   this.diffFailed = false;
   this.ignoreWhitespace = false;
   this.peek = null;
   this.baseRefresher = null;
   this.revision = 0;
   this.splitView = null;
   this.statusListener = null;
   this.statusPending = false;

   this.onAfterRender = this.onAfterRender.bind(this);
   this.onChange = this.onChange.bind(this);
   this.onCursorActivity = this.onCursorActivity.bind(this);
   this.onGutterMouseDown = this.onGutterMouseDown.bind(this);
   this.onGutterClick = this.onGutterClick.bind(this);
   this.pinPeek = this.pinPeek.bind(this);

   // reachable from the editor for automation and debugging
   editor.gitDiffGutter = this;

   var gutter = editor.renderer.$gutterLayer;
   dom.addCssClass(gutter.element, GUTTER_CLASS);
   this.resetGutterPadding();
   gutter.on("afterRender", this.onAfterRender);
   editor.renderer.on("afterRender", this.pinPeek);
   editor.on("change", this.onChange);
   editor.on("changeSelection", this.onCursorActivity);
   editor.on("focus", this.onCursorActivity);

   // Capture phase, so these run ahead of Ace's own gutter handlers: the
   // breakpoint toggle hangs off mousedown, and the fold toggle off click and
   // dblclick, with a hit region that overlaps the marker.
   var gutterEl = editor.renderer.$gutter;
   gutterEl.addEventListener("mousedown", this.onGutterMouseDown, true);
   gutterEl.addEventListener("click", this.onGutterClick, true);
   gutterEl.addEventListener("dblclick", this.onGutterClick, true);

   // Escape closes an open peek. An Ace command rather than a keyboard
   // handler of our own: commands live in the default handler, the one
   // setKeyboardHandler keeps when RStudio rebuilds the editor's handlers
   // (file type or keybinding changes, re-attach), and the lowest in
   // precedence, so the completion popup and a Vim or Emacs mode, which own
   // Escape while active, still see the key first. Among commands on the
   // same key Ace tries the last one registered first, so screen reader
   // support's blurTextInput would take Escape whenever it was added after
   // this one (a file opened with ext-diff already loaded, or the pref turned
   // on later); the explicit position puts this command ahead regardless.
   var self = this;
   editor.commands.addCommand({
      name: "closeGitPeek",
      bindKey: { win: "Esc", mac: "Esc", position: 1 },
      readOnly: true,
      isAvailable: function() { return self.peek != null; },
      exec: function() { self.closePeek(); }
   });
};

(function() {

   // The gutter caches the cell padding it measured, and our class changes it.
   this.resetGutterPadding = function() {
      var renderer = this.editor.renderer;
      renderer.$gutterLayer.$padding = null;
      renderer.$loop.schedule(renderer.CHANGE_FULL);
   };

   // Sets the text to diff against; null clears all markers. An open peek
   // shows the old base, so it goes when the base really changes -- not on
   // the routine re-fetches (editor focus, Git refresh) that return the same
   // contents.
   this.setBase = function(text) {
      if (text === this.baseText)
         return;

      this.closePeek();
      this.baseText = text;
      this.baseLines = (text == null) ? null : splitLines(text);
      this.strippedBaseLines = null;
      if (this.splitView)
         this.splitView.setBaseLines(this.baseLines);
      this.update();
   };

   this.setIgnoreWhitespace = function(ignore) {
      ignore = !!ignore;
      if (ignore === this.ignoreWhitespace)
         return;

      this.closePeek();
      this.ignoreWhitespace = ignore;
      this.update();
   };

   this.getChunks = function() {
      return this.chunks;
   };

   // Installs the function that re-fetches the base. It is handed a callback
   // to run once the fetched base has been applied (through setBase), even
   // if that base turned out to be the one already in use.
   this.setBaseRefresher = function(refresher) {
      this.baseRefresher = refresher;
   };

   // Installs what is told when the change at the cursor, or the number of
   // changes, may have moved (see getChangeIndexAtCursor). Calls are batched
   // per task, and come after the side-by-side view has noted which pane
   // took focus.
   this.setStatusListener = function(listener) {
      this.statusListener = listener;
      this.notifyStatus();
   };

   this.notifyStatus = function() {
      if (!this.statusListener || this.statusPending)
         return;

      var self = this;
      this.statusPending = true;
      Promise.resolve().then(function() {
         self.statusPending = false;
         if (self.statusListener)
            self.statusListener();
      });
   };

   this.onCursorActivity = function() {
      this.notifyStatus();
   };

   // Whether the changes can be counted: there is a base to diff against
   // (none is fetched yet, or the file is untracked) and the diff came
   // through. One that gave up or failed marks nothing, which is not to say
   // there are no changes.
   this.canCountChanges = function() {
      return this.baseLines != null && !this.diffFailed;
   };

   this.getChangeCount = function() {
      return this.chunks.length;
   };

   // The position of the change at the cursor among the document's changes,
   // or -1 when the cursor is not on one.
   this.getChangeIndexAtCursor = function() {
      return this.chunks.indexOf(this.chunkAtCursor());
   };

   // A side-by-side view (see git_diff_split.js) shows the base beside the
   // document and follows this gutter's diff. While one is attached the peek
   // shows the actions alone (see showPeek), so an open one goes as the view
   // comes or goes. The base pane's cursor counts as the cursor while it
   // holds focus, so its moves are reported as the document's are.
   this.setSplitView = function(view) {
      this.closePeek();
      if (this.splitView)
      {
         this.splitView.baseEditor.off("changeSelection", this.onCursorActivity);
         this.splitView.baseEditor.off("focus", this.onCursorActivity);
      }

      this.splitView = view;
      this.notifyStatus();
      if (!view)
         return;

      view.baseEditor.on("changeSelection", this.onCursorActivity);
      view.baseEditor.on("focus", this.onCursorActivity);

      // (an update waiting out a slow diff's typing pause would otherwise
      // hand the view the previous chunks, and its first folds with them)
      this.flushUpdate();
      view.setBaseLines(this.baseLines);
      view.setChunks(this.chunks, this.ignoreWhitespace);
   };

   // Runs the callback once the base is current. The base is also re-fetched
   // on focus, but only after a delay and a round trip, and a revert must not
   // restore an index version that has since moved -- e.g. the file staged in
   // the Terminal while a peek was open, where the old base would undo the
   // very change just staged.
   this.withFreshBase = function(callback) {
      if (this.baseRefresher)
         this.baseRefresher(callback);
      else
         callback();
   };

   // Runs a revert once the base is current, unless the document was edited
   // in the meantime: an edit within the chunk's rows leaves the chunk's
   // coordinates intact, so revertChunk could not tell and would erase it.
   // The user still sees what they typed and can ask again.
   this.revertWhenFresh = function(revert) {
      var self = this;
      var revision = this.revision;
      this.withFreshBase(function() {
         if (self.revision === revision)
            revert();
      });
   };

   // Every edit bumps the revision, so that work deferred across a round
   // trip can tell whether the document it was meant for is still there.
   this.onChange = function() {
      this.revision++;
      this.scheduleUpdate();
   };

   this.scheduleUpdate = function() {
      var self = this;
      var run = function() {
         if (!self.updatePending)
            return;

         self.updatePending = false;
         self.updateTimer = null;
         self.update();
      };

      if (this.lastDiffMs > SLOW_DIFF_MS)
      {
         // slow: wait for a pause in the typing
         if (this.updateTimer)
            clearTimeout(this.updateTimer);
         this.updatePending = true;
         this.updateTimer = setTimeout(run, SLOW_UPDATE_DELAY_MS);
         return;
      }

      if (this.updatePending)
         return;

      this.updatePending = true;
      Promise.resolve().then(run);
   };

   // Runs a pending update now, so that anything acting on the chunks sees
   // the current document rather than one an edit behind.
   this.flushUpdate = function() {
      if (!this.updatePending)
         return;

      if (this.updateTimer)
      {
         clearTimeout(this.updateTimer);
         this.updateTimer = null;
      }

      this.updatePending = false;
      this.update();
   };

   this.update = function() {
      var chunks = [];
      this.diffFailed = false;
      if (this.baseLines != null)
      {
         var base = this.baseLines;
         var lines = this.editor.session.doc.getAllLines();
         if (lines.length === 1 && lines[0] === "")
            lines = [];

         if (this.ignoreWhitespace)
         {
            // the base does not change between updates, so strip it once
            if (!this.strippedBaseLines)
               this.strippedBaseLines = base.map(stripWhitespace);
            base = this.strippedBaseLines;
            lines = lines.map(stripWhitespace);
         }

         chunks = this.computeChunks(base, lines);
      }

      this.setChunks(chunks);
   };

   this.computeChunks = function(base, lines) {
      var common = trimCommonLines(base, lines);
      if (common.prefix > 0 || common.suffix > 0)
      {
         base = base.slice(common.prefix, base.length - common.suffix);
         lines = lines.slice(common.prefix, lines.length - common.suffix);
      }

      // The provider cannot take an empty side (it asserts in its refinement
      // pass), but those cases have only one possible answer anyway.
      if (base.length === 0 || lines.length === 0)
      {
         if (base.length === lines.length)
            return [];

         return offsetChunks([{
            old: new Range(0, 0, base.length, 0),
            new: new Range(0, 0, lines.length, 0)
         }], common.prefix);
      }

      try
      {
         var DiffProvider = require("ace/ext/diff/providers/default").DiffProvider;
         var options = { maxComputationTimeMs: MAX_COMPUTATION_MS };
         var started = Date.now();
         var chunks = new DiffProvider().compute(base, lines, options);
         this.lastDiffMs = Date.now() - started;

         // A timed-out line alignment is not a diff: it would mark every
         // line changed and hand Revert the whole document. But a large
         // replaced block looks the same, and spends the budget just as
         // surely, in the character-level refinement that follows a sound
         // alignment (quadratic in the block, and of no use here). The diff
         // is run again on tokens, which leaves the budget to the alignment;
         // only if that too gives up are there better no markers.
         if (this.lastDiffMs >= MAX_COMPUTATION_MS && isWholeDocument(chunks, base, lines))
         {
            var tokens = tokenizeLines(base, lines);
            started = Date.now();
            chunks = dropCharChanges(new DiffProvider().compute(tokens.base, tokens.lines, options));
            if (Date.now() - started >= MAX_COMPUTATION_MS && isWholeDocument(chunks, base, lines))
            {
               this.diffFailed = true;
               return [];
            }
         }

         return offsetChunks(chunks, common.prefix);
      }
      catch (error)
      {
         // better no markers than an error surfacing on every keystroke
         console.warn("git diff gutter: diff failed", error);
         this.diffFailed = true;
         return [];
      }
   };

   this.setChunks = function(chunks) {
      this.chunks = chunks;

      var rowClasses = {};
      var lastRow = this.editor.session.getLength() - 1;

      var add = function(row, cls) {
         rowClasses[row] = rowClasses[row] ? rowClasses[row] + " " + cls : cls;
      };

      chunks.forEach(function(chunk) {
         var start = chunk.new.start.row;
         var end = chunk.new.end.row;

         // Nothing of a pure deletion survives in the buffer, so mark the
         // boundary: the line that now follows the removed lines, or the
         // last line when the removal was at the end of the file.
         if (isDeletion(chunk))
         {
            if (start > lastRow)
               add(lastRow, DELETED_BELOW_CLASS);
            else
               add(start, DELETED_CLASS);
            return;
         }

         var cls = isAddition(chunk) ? ADDED_CLASS : MODIFIED_CLASS;
         for (var row = start; row < end; row++)
            add(row, cls);
      });

      this.rowClasses = rowClasses;

      if (this.splitView)
         this.splitView.setChunks(chunks, this.ignoreWhitespace);

      // An open peek survives only if its change is still there unchanged.
      if (this.peek)
      {
         var kept = null;
         for (var i = 0; i < chunks.length; i++)
         {
            if (sameChunk(chunks[i], this.peek.chunk))
               kept = chunks[i];
         }

         if (kept)
            this.peek.chunk = kept;
         else
            this.closePeek();
      }

      var renderer = this.editor.renderer;
      renderer.$loop.schedule(renderer.CHANGE_GUTTER);
      this.notifyStatus();
   };

   // The gutter recycles its cell elements, so classes are reassigned from
   // scratch on every render. This is also where a peek notices it has been
   // stacked under another feature's widget (Ace keeps the newest on top)
   // and gets out of the way.
   this.onAfterRender = function() {
      if (this.peek)
      {
         // Ace drops the whole collection once its last widget goes (e.g. an
         // edit spanning the peek's row), so the peek may be gone outright.
         var widgets = this.editor.session.lineWidgets;
         if (!widgets || widgets[this.peek.widget.row] !== this.peek.widget)
            this.closePeek();
      }

      var cells = this.editor.renderer.$gutterLayer.$lines.cells;
      for (var i = 0; i < cells.length; i++)
      {
         var cell = cells[i];
         var element = cell.element;
         for (var j = 0; j < CELL_CLASSES.length; j++)
            dom.removeCssClass(element, CELL_CLASSES[j]);

         var classes = this.rowClasses[cell.row];
         if (classes)
            element.className += " " + classes;
      }
   };

   // The row a change is shown on: its first line, or for a deletion the
   // line carrying the marker.
   this.displayRow = function(chunk) {
      var lastRow = this.editor.session.getLength() - 1;
      return isDeletion(chunk) ? Math.min(chunk.new.start.row, lastRow) : chunk.new.start.row;
   };

   // The current chunk equal to one captured earlier, or null if the diff
   // has moved on since.
   this.findChunk = function(chunk) {
      for (var i = 0; i < this.chunks.length; i++)
      {
         if (sameChunk(this.chunks[i], chunk))
            return this.chunks[i];
      }
      return null;
   };

   this.chunkAt = function(row) {
      for (var i = 0; i < this.chunks.length; i++)
      {
         var chunk = this.chunks[i];
         var hit = isDeletion(chunk)
            ? row === this.displayRow(chunk)
            : row >= chunk.new.start.row && row < chunk.new.end.row;
         if (hit)
            return chunk;
      }
      return null;
   };

   // The cursor-driven commands (Next/Previous Change, Show/Revert Change at
   // Cursor) start from the document's cursor -- or, when the base pane of a
   // side-by-side view holds the cursor, from the base cursor, which moving
   // around in the base does not carry over to the document. The base's rows
   // are those of the change's old side; an addition has none there, so it
   // is reached from the line that follows it, as a deletion is in the
   // document.
   this.baseFocused = function() {
      return this.splitView != null && this.splitView.heldFocusLast();
   };

   this.cursorRow = function() {
      if (this.baseFocused())
         return this.splitView.baseCursorRow();
      return this.editor.getCursorPosition().row;
   };

   this.baseDisplayRow = function(chunk) {
      // an empty base still has one (empty) row in Ace
      var lastRow = this.baseLines ? Math.max(this.baseLines.length - 1, 0) : 0;
      return isAddition(chunk) ? Math.min(chunk.old.start.row, lastRow) : chunk.old.start.row;
   };

   // The row a change is shown on in the pane holding the cursor.
   this.cursorPaneRow = function(chunk) {
      return this.baseFocused() ? this.baseDisplayRow(chunk) : this.displayRow(chunk);
   };

   this.chunkAtCursor = function() {
      var row = this.cursorRow();
      if (!this.baseFocused())
         return this.chunkAt(row);

      for (var i = 0; i < this.chunks.length; i++)
      {
         var chunk = this.chunks[i];
         var hit = isAddition(chunk)
            ? row === this.baseDisplayRow(chunk)
            : row >= chunk.old.start.row && row < chunk.old.end.row;
         if (hit)
            return chunk;
      }
      return null;
   };

   // Navigation wraps around, like Find. An open peek comes along to the
   // change navigated to, so that changes can be reviewed one after another.
   this.goToNextChange = function() {
      this.flushUpdate();
      if (this.chunks.length === 0)
         return false;

      var row = this.cursorRow();
      var target = this.chunks[0];
      for (var i = 0; i < this.chunks.length; i++)
      {
         if (this.cursorPaneRow(this.chunks[i]) > row)
         {
            target = this.chunks[i];
            break;
         }
      }

      this.goToChange(target);
      return true;
   };

   this.goToPreviousChange = function() {
      this.flushUpdate();
      if (this.chunks.length === 0)
         return false;

      var row = this.cursorRow();
      var target = this.chunks[this.chunks.length - 1];
      for (var i = this.chunks.length - 1; i >= 0; i--)
      {
         if (this.cursorPaneRow(this.chunks[i]) < row)
         {
            target = this.chunks[i];
            break;
         }
      }

      this.goToChange(target);
      return true;
   };

   // (the open peek goes first: the rows it takes up would otherwise come
   // out of the scroll position worked out for the change, and carry it
   // off screen when the peek sat above it)
   this.goToChange = function(chunk) {
      var peeking = this.peek != null && this.peek.chunk !== chunk;
      if (peeking)
         this.closePeek();

      this.goToChunk(chunk);
      if (peeking)
         this.showPeek(chunk);
   };

   this.goToChunk = function(chunk) {
      var row = this.displayRow(chunk);
      if (this.baseFocused())
      {
         // the base pane keeps the cursor (its scrolling brings the document
         // along); the document's cursor follows so that the two agree on
         // which change is at the cursor
         this.splitView.goToBaseRow(this.baseDisplayRow(chunk));
         this.editor.clearSelection();
         this.editor.moveCursorTo(row, 0);
         return;
      }

      this.editor.gotoLine(row + 1, 0, true);
   };

   // The change is the one under the cursor now; the revert itself waits
   // for the base (see revertWhenFresh).
   this.revertChangeAtCursor = function() {
      this.flushUpdate();
      var chunk = this.chunkAtCursor();
      if (!chunk)
         return false;

      var self = this;
      this.revertWhenFresh(function() {
         self.revertChunk(chunk);
      });
      return true;
   };

   // Replaces the chunk's lines in the buffer with the base version's. The
   // document's trailing line needs care: a chunk reaching the end of the
   // document owns the newline before it rather than one after it.
   this.revertChunk = function(chunk) {
      // the chunk may predate edits whose diff hasn't been computed yet
      this.flushUpdate();
      chunk = this.findChunk(chunk);
      if (!chunk)
      {
         this.closePeek();
         return false;
      }

      var session = this.editor.session;
      var doc = session.doc;
      var oldText = this.baseLines.slice(chunk.old.start.row, chunk.old.end.row).join("\n");
      var start = chunk.new.start.row;
      var end = chunk.new.end.row;
      var length = doc.getLength();

      // An empty document still has one (empty) line in Ace, which the diff
      // treated as no lines at all; the base replaces it outright.
      var docEmpty = length === 1 && doc.getLine(0) === "";

      var range, text;
      if (docEmpty)
      {
         range = new Range(0, 0, 0, 0);
         text = oldText;
      }
      else if (end < length)
      {
         range = new Range(start, 0, end, 0);
         text = isAddition(chunk) ? "" : oldText + "\n";
      }
      else if (start === 0)
      {
         range = new Range(0, 0, length - 1, doc.getLine(length - 1).length);
         text = oldText;
      }
      else
      {
         var anchorRow = Math.min(start, length) - 1;
         range = new Range(anchorRow, doc.getLine(anchorRow).length, length - 1, doc.getLine(length - 1).length);
         text = isAddition(chunk) ? "" : "\n" + oldText;
      }

      // the replace fires a change, which schedules an update; run that one
      // now rather than a second diff of the same document later
      this.closePeek();
      session.replace(range, text);
      this.flushUpdate();
      return true;
   };

   // Toggles the peek for the change under the cursor, so that what Revert
   // would restore can be seen without the mouse. Returns false when the
   // cursor is not on a change.
   this.showChangeAtCursor = function() {
      this.flushUpdate();
      var chunk = this.chunkAtCursor();
      if (!chunk)
         return false;

      this.togglePeek(chunk);
      return true;
   };

   // With the base on screen already (a side-by-side view), a marker click or
   // Show Change at Cursor also goes to the change, so that the two panes'
   // cursors agree on it.
   this.togglePeek = function(chunk) {
      if (this.peek && this.peek.chunk === chunk)
      {
         this.closePeek();
         return;
      }

      if (this.splitView)
         this.goToChunk(chunk);
      this.showPeek(chunk);
   };

   this.lineWidgetManager = function() {
      var session = this.editor.session;
      if (!session.widgetManager)
      {
         var LineWidgets = require("ace/line_widgets").LineWidgets;
         session.widgetManager = new LineWidgets(session);
         session.widgetManager.attach(this.editor);
      }
      return session.widgetManager;
   };

   // Where a peek for the chunk can go. Ace keeps one line widget per row and
   // RStudio enumerates them to persist notebook chunk output, so a peek must
   // not displace an existing widget: it goes below the last changed line (or
   // where removed lines were), else on the other side of the change, else
   // nowhere. A side-by-side view's alignment padding is no obstacle: the
   // view moves it off the peek's row as it realigns (or, above the first
   // line, onto the peek; see $addWidget in git_diff_split.js).
   this.peekRow = function(chunk) {
      var session = this.editor.session;
      var candidates = isDeletion(chunk)
         ? [chunk.new.start.row - 1, chunk.new.start.row]
         : [chunk.new.end.row - 1, chunk.new.start.row - 1];

      for (var i = 0; i < candidates.length; i++)
      {
         var row = candidates[i];
         if (row < 0 || row >= session.getLength())
            continue;

         var widget = session.lineWidgets && session.lineWidgets[row];
         if (widget && !widget.gitDiffPadding)
            continue;

         return row;
      }
      return null;
   };

   // Shows the base version of a change in a line widget below it, with a
   // button to revert the change. With a side-by-side view attached, whose
   // base pane shows the previous lines already, the widget is a compact bar
   // with the buttons alone; the view pads the base pane to match it. Returns
   // false if there is no room for it.
   this.showPeek = function(chunk) {
      this.closePeek();

      var row = this.peekRow(chunk);
      if (row == null)
         return false;

      var self = this;
      var compact = this.splitView != null;
      var oldLines = this.baseLines.slice(chunk.old.start.row, chunk.old.end.row);
      var newCount = chunk.new.end.row - chunk.new.start.row;

      var label;
      if (isDeletion(chunk))
         label = this.strings.removedLines(oldLines.length);
      else if (isAddition(chunk))
         label = this.strings.addedLines(newCount);
      else if (compact)
         label = this.strings.changedLinesShort(newCount);
      else
         label = this.strings.changedLines(newCount);

      // the kind picks the accent colour, matching the marker it came from
      var kind = isDeletion(chunk) ? "deleted" : isAddition(chunk) ? "added" : "modified";
      var el = document.createElement("div");
      el.className = PEEK_CLASS + " " + PEEK_CLASS + "-" + kind;

      var header = document.createElement("div");
      header.className = PEEK_CLASS + "-header";
      el.appendChild(header);

      // The compact bar's label is for assistive technology only; on screen
      // the bar sits against the change it is about.
      if (compact)
      {
         el.className += " " + PEEK_CLASS + "-compact";
         el.setAttribute("role", "group");
         el.setAttribute("aria-label", label);
      }
      else
      {
         var labelEl = document.createElement("span");
         labelEl.className = PEEK_CLASS + "-label";
         labelEl.textContent = label;
         header.appendChild(labelEl);
      }

      var revertButton = document.createElement("button");
      revertButton.type = "button";
      revertButton.className = PEEK_CLASS + "-button " + PEEK_CLASS + "-revert";
      revertButton.textContent = this.strings.revert;
      revertButton.addEventListener("click", function() {
         self.focusEditor();

         // Reverting restores what the peek shows, so it goes ahead only if
         // the peek is still open once the base is current: a changed base
         // closes it (see setBase), and reverting to lines the user never
         // saw would be wrong even where a chunk still sits on these rows.
         var peek = self.peek;
         self.revertWhenFresh(function() {
            if (self.peek === peek)
               self.revertChunk(peek.chunk);
         });
      });
      header.appendChild(revertButton);

      // Closing is the lesser action (the marker, Escape, and Show Change all
      // close the peek too), so it is a plain glyph at the end of the header.
      var closeButton = document.createElement("button");
      closeButton.type = "button";
      closeButton.className = PEEK_CLASS + "-close";
      closeButton.textContent = "\u00D7";
      closeButton.title = this.strings.close;
      closeButton.setAttribute("aria-label", this.strings.close);
      closeButton.addEventListener("click", function() {
         self.focusEditor();
         self.closePeek();
      });
      header.appendChild(closeButton);

      // With "Tab key always moves focus" on, Tab can land on the peek's
      // buttons, where a keypress never reaches the editor's textarea; let
      // Escape close the peek from there too.
      el.addEventListener("keydown", function(e) {
         if (e.key !== "Escape")
            return;

         e.preventDefault();
         e.stopPropagation();
         self.focusEditor();
         self.closePeek();
      });

      if (oldLines.length > 0 && !compact)
      {
         var lines = document.createElement("div");
         lines.className = PEEK_CLASS + "-lines";
         oldLines.forEach(function(line) {
            var lineEl = document.createElement("div");
            lineEl.className = PEEK_CLASS + "-line";
            lineEl.textContent = line.length ? line : " ";
            lines.appendChild(lineEl);
         });
         el.appendChild(lines);
      }

      var widget = { row: row, el: el, fixedWidth: true, coverGutter: false };
      this.lineWidgetManager().addLineWidget(widget);
      this.peek = { chunk: chunk, widget: widget };
      return true;
   };

   this.closePeek = function() {
      if (!this.peek)
         return;

      var peek = this.peek;
      this.peek = null;
      this.lineWidgetManager().removeLineWidget(peek.widget);

      // A fold ending on the peek's row aliases its widget at the fold's
      // start row, and removeLineWidget clears the widget's own row only;
      // sweep any slot (or stack) still pointing at it, or the next render
      // would bring it back with nothing left to close it.
      var widgets = this.editor.session.lineWidgets;
      if (!widgets)
         return;

      for (var i = 0; i < widgets.length; i++)
      {
         if (widgets[i] === peek.widget)
         {
            widgets[i] = peek.widget.$oldWidget;
            continue;
         }

         var above = widgets[i];
         while (above && above.$oldWidget)
         {
            if (above.$oldWidget === peek.widget)
               above.$oldWidget = peek.widget.$oldWidget;
            above = above.$oldWidget;
         }
      }
   };

   // A peek below a change taller than the editor would open out of view.
   // Like a sticky footer, its header is drawn at the bottom of the editor
   // instead, while the change's first line is still on screen above it; as
   // that line leaves through the bottom, it takes the header with it.
   //
   // The widget keeps its place (and the space reserved for it); only where
   // it is drawn moves. This runs in the render pass that positions it, and
   // the pinned and natural positions meet where pinning starts, so the
   // header never jumps as the editor scrolls between the two.
   this.pinPeek = function() {
      var peek = this.peek;
      if (!peek)
         return;

      // A peek above its change (where a notebook's output took the row
      // below) or at removed lines has no changed lines to cover.
      var widget = peek.widget;
      var chunk = peek.chunk;
      var el = widget.el;
      var lift = 0;
      var height = 0;
      if (!widget.hidden && !isDeletion(chunk) && widget.row >= chunk.new.start.row)
      {
         var header = el.firstChild;
         height = el.clientTop + header.offsetTop + header.offsetHeight;

         // (the floor is the bottom of the change's first screen line, not
         // of its first row, so that a line wrapped taller than the editor
         // brings the header up too)
         var top = this.rowScreenBottom(widget.row);
         var floor = this.rowScreenTop(chunk.new.start.row) + this.editor.renderer.layerConfig.lineHeight;
         var bottom = this.editor.renderer.$size.scrollerHeight;
         lift = top - Math.max(Math.min(top, bottom - height), floor);
      }

      var pinned = lift > 0;
      dom.setCssClass(el, PEEK_CLASS + "-pinned", pinned);
      el.style.transform = pinned ? "translateY(" + -lift + "px)" : "";

      // (the clip leaves room above for the pinned header's shadow)
      el.style.clipPath = pinned ? "inset(-8px 0 calc(100% - " + height + "px) 0)" : "";
   };

   // The top of a document row on screen, in the editor container's
   // coordinates.
   this.rowScreenTop = function(row) {
      var config = this.editor.renderer.layerConfig;
      var screenRow = this.editor.session.documentToScreenRow(row, 0);
      return (screenRow - config.firstRowScreen) * config.lineHeight - config.offset;
   };

   // The bottom of a document row on screen: where Ace's
   // LineWidgets.renderWidgets puts a widget below the row.
   this.rowScreenBottom = function(row) {
      var lineHeight = this.editor.renderer.layerConfig.lineHeight;
      return this.rowScreenTop(row) + lineHeight * this.editor.session.getRowLineCount(row);
   };

   this.rowForCell = function(element) {
      var cells = this.editor.renderer.$gutterLayer.$lines.cells;
      for (var i = 0; i < cells.length; i++)
      {
         if (cells[i].element === element)
            return cells[i].row;
      }
      return null;
   };

   // The chunk whose marker a gutter mouse event landed on, or null.
   this.markerChunkAt = function(e) {
      if (e.button !== 0 || !e.target.closest)
         return null;

      var cell = e.target.closest(".ace_gutter-cell");
      if (!cell)
         return null;

      if (e.clientX < cell.getBoundingClientRect().right - MARKER_HIT_WIDTH)
         return null;

      this.flushUpdate();
      var row = this.rowForCell(cell);
      return row == null ? null : this.chunkAt(row);
   };

   // Consumed clicks never reach Ace's own gutter handler, which is what
   // would focus the editor -- and the base is re-fetched on focus, so an
   // index moved from outside (e.g. staging in the Terminal) is picked up
   // when the user comes to the gutter just as when they come to the text.
   this.focusEditor = function() {
      if (!this.editor.isFocused())
         this.editor.focus();
   };

   this.onGutterMouseDown = function(e) {
      var chunk = this.markerChunkAt(e);
      if (!chunk)
         return;

      e.preventDefault();
      e.stopImmediatePropagation();
      this.focusEditor();
      this.togglePeek(chunk);
   };

   // The click and dblclick that follow a consumed mousedown would otherwise
   // reach Ace's fold handler, whose region covers the marker.
   this.onGutterClick = function(e) {
      if (!this.markerChunkAt(e))
         return;

      e.preventDefault();
      e.stopImmediatePropagation();
   };

   this.dispose = function() {
      this.updatePending = false;
      this.statusListener = null;
      if (this.updateTimer)
      {
         clearTimeout(this.updateTimer);
         this.updateTimer = null;
      }

      if (this.splitView)
         this.splitView.dispose();
      this.closePeek();
      this.editor.commands.removeCommand("closeGitPeek");
      this.editor.renderer.off("afterRender", this.pinPeek);
      this.editor.off("change", this.onChange);
      this.editor.off("changeSelection", this.onCursorActivity);
      this.editor.off("focus", this.onCursorActivity);

      var gutterEl = this.editor.renderer.$gutter;
      gutterEl.removeEventListener("mousedown", this.onGutterMouseDown, true);
      gutterEl.removeEventListener("click", this.onGutterClick, true);
      gutterEl.removeEventListener("dblclick", this.onGutterClick, true);

      // Strip our classes from the visible cells before letting go of the
      // render hook, since nothing else will.
      this.rowClasses = {};
      this.chunks = [];
      this.onAfterRender();

      var gutter = this.editor.renderer.$gutterLayer;
      gutter.off("afterRender", this.onAfterRender);
      dom.removeCssClass(gutter.element, GUTTER_CLASS);
      this.resetGutterPadding();

      if (this.editor.gitDiffGutter === this)
         delete this.editor.gitDiffGutter;
   };

}).call(GitDiffGutter.prototype);

exports.GitDiffGutter = GitDiffGutter;

});
