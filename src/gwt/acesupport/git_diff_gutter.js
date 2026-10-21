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

// Typing bursts are coalesced into a single diff.
var UPDATE_DELAY_MS = 150;

// Bail out on pathological inputs rather than hang the UI thread.
var MAX_COMPUTATION_MS = 250;

function splitLines(text) {
   return text === "" ? [] : text.split(/\r\n|\r|\n/);
}

// Only equality matters to the diff, so whitespace-insensitive comparison
// (git diff -w) is a matter of what the provider gets to compare.
function stripWhitespace(line) {
   return line.replace(/\s+/g, "");
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

function sameChunk(a, b) {
   return a.old.start.row === b.old.start.row &&
          a.old.end.row === b.old.end.row &&
          a.new.start.row === b.new.start.row &&
          a.new.end.row === b.new.end.row;
}

var GitDiffGutter = function(editor) {
   this.editor = editor;
   this.baseText = null;
   this.baseLines = null;
   this.chunks = [];
   this.rowClasses = {};
   this.updateTimer = null;
   this.ignoreWhitespace = false;
   this.peek = null;

   this.onAfterRender = this.onAfterRender.bind(this);
   this.scheduleUpdate = this.scheduleUpdate.bind(this);
   this.onGutterMouseDown = this.onGutterMouseDown.bind(this);
   this.onGutterClick = this.onGutterClick.bind(this);

   var gutter = editor.renderer.$gutterLayer;
   dom.addCssClass(gutter.element, GUTTER_CLASS);
   this.resetGutterPadding();
   gutter.on("afterRender", this.onAfterRender);
   editor.on("change", this.scheduleUpdate);

   // Capture phase, so these run ahead of Ace's own gutter handlers: the
   // breakpoint toggle hangs off mousedown, and the fold toggle off click and
   // dblclick, with a hit region that overlaps the marker.
   var gutterEl = editor.renderer.$gutter;
   gutterEl.addEventListener("mousedown", this.onGutterMouseDown, true);
   gutterEl.addEventListener("click", this.onGutterClick, true);
   gutterEl.addEventListener("dblclick", this.onGutterClick, true);
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

   this.scheduleUpdate = function() {
      if (this.updateTimer)
         return;

      var self = this;
      this.updateTimer = setTimeout(function() {
         self.updateTimer = null;
         self.update();
      }, UPDATE_DELAY_MS);
   };

   // Runs a pending update now, so that anything acting on the chunks sees
   // the current document rather than one up to UPDATE_DELAY_MS stale.
   this.flushUpdate = function() {
      if (!this.updateTimer)
         return;

      clearTimeout(this.updateTimer);
      this.updateTimer = null;
      this.update();
   };

   this.update = function() {
      var chunks = [];
      if (this.baseLines != null)
      {
         var base = this.baseLines;
         var lines = this.editor.session.doc.getAllLines();
         if (lines.length === 1 && lines[0] === "")
            lines = [];

         if (this.ignoreWhitespace)
         {
            base = base.map(stripWhitespace);
            lines = lines.map(stripWhitespace);
         }

         chunks = this.computeChunks(base, lines);
      }

      this.setChunks(chunks);
   };

   this.computeChunks = function(base, lines) {
      // The provider cannot take an empty side (it asserts in its refinement
      // pass), but those cases have only one possible answer anyway.
      if (base.length === 0 || lines.length === 0)
      {
         if (base.length === lines.length)
            return [];

         return [{
            old: new Range(0, 0, base.length, 0),
            new: new Range(0, 0, lines.length, 0)
         }];
      }

      try
      {
         var DiffProvider = require("ace/ext/diff/providers/default").DiffProvider;
         return new DiffProvider().compute(base, lines, {
            maxComputationTimeMs: MAX_COMPUTATION_MS
         });
      }
      catch (error)
      {
         // better no markers than an error surfacing on every keystroke
         console.warn("git diff gutter: diff failed", error);
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
   };

   // The gutter recycles its cell elements, so classes are reassigned from
   // scratch on every render.
   this.onAfterRender = function() {
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

   // Navigation wraps around, like Find.
   this.goToNextChange = function() {
      this.flushUpdate();
      if (this.chunks.length === 0)
         return false;

      var row = this.editor.getCursorPosition().row;
      var target = this.chunks[0];
      for (var i = 0; i < this.chunks.length; i++)
      {
         if (this.displayRow(this.chunks[i]) > row)
         {
            target = this.chunks[i];
            break;
         }
      }

      this.goToChunk(target);
      return true;
   };

   this.goToPreviousChange = function() {
      this.flushUpdate();
      if (this.chunks.length === 0)
         return false;

      var row = this.editor.getCursorPosition().row;
      var target = this.chunks[this.chunks.length - 1];
      for (var i = this.chunks.length - 1; i >= 0; i--)
      {
         if (this.displayRow(this.chunks[i]) < row)
         {
            target = this.chunks[i];
            break;
         }
      }

      this.goToChunk(target);
      return true;
   };

   this.goToChunk = function(chunk) {
      this.editor.gotoLine(this.displayRow(chunk) + 1, 0, true);
   };

   this.revertChangeAtCursor = function() {
      this.flushUpdate();
      var chunk = this.chunkAt(this.editor.getCursorPosition().row);
      if (!chunk)
         return false;

      return this.revertChunk(chunk);
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

      this.closePeek();
      session.replace(range, text);
      this.update();
      return true;
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

   // Shows the base version of a change in a line widget below it, with a
   // button to revert the change.
   this.showPeek = function(chunk) {
      this.closePeek();

      var self = this;
      var oldLines = this.baseLines.slice(chunk.old.start.row, chunk.old.end.row);
      var newCount = chunk.new.end.row - chunk.new.start.row;

      var label;
      if (isDeletion(chunk))
         label = "Removed " + pluralize(oldLines.length, "line");
      else if (isAddition(chunk))
         label = "Added " + pluralize(newCount, "line");
      else
         label = "Changed " + pluralize(newCount, "line") + "; previously:";

      var el = document.createElement("div");
      el.className = PEEK_CLASS;

      var header = document.createElement("div");
      header.className = PEEK_CLASS + "-header";
      el.appendChild(header);

      var labelEl = document.createElement("span");
      labelEl.className = PEEK_CLASS + "-label";
      labelEl.textContent = label;
      header.appendChild(labelEl);

      var revertButton = document.createElement("button");
      revertButton.type = "button";
      revertButton.className = PEEK_CLASS + "-button " + PEEK_CLASS + "-revert";
      revertButton.textContent = "Revert";
      revertButton.addEventListener("click", function() {
         self.revertChunk(chunk);
      });
      header.appendChild(revertButton);

      var closeButton = document.createElement("button");
      closeButton.type = "button";
      closeButton.className = PEEK_CLASS + "-button " + PEEK_CLASS + "-close";
      closeButton.textContent = "Close";
      closeButton.addEventListener("click", function() {
         self.closePeek();
      });
      header.appendChild(closeButton);

      if (oldLines.length > 0)
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

      // below the last changed line; for a deletion, where the lines were
      var row = isDeletion(chunk) ? Math.max(chunk.new.start.row - 1, 0) : chunk.new.end.row - 1;
      var widget = { row: row, el: el, fixedWidth: true, coverGutter: false };
      this.lineWidgetManager().addLineWidget(widget);
      this.peek = { chunk: chunk, widget: widget };
   };

   this.closePeek = function() {
      if (!this.peek)
         return;

      var peek = this.peek;
      this.peek = null;
      this.lineWidgetManager().removeLineWidget(peek.widget);
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

   this.onGutterMouseDown = function(e) {
      var chunk = this.markerChunkAt(e);
      if (!chunk)
         return;

      e.preventDefault();
      e.stopImmediatePropagation();

      if (this.peek && this.peek.chunk === chunk)
         this.closePeek();
      else
         this.showPeek(chunk);
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
      if (this.updateTimer)
      {
         clearTimeout(this.updateTimer);
         this.updateTimer = null;
      }

      this.closePeek();
      this.editor.off("change", this.scheduleUpdate);

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
   };

}).call(GitDiffGutter.prototype);

exports.GitDiffGutter = GitDiffGutter;

});
