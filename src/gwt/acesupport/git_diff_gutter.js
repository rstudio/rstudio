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

// The marker sits at the right edge of a gutter cell; a click there opens
// the peek instead of reaching Ace (and the breakpoint toggle behind it).
var MARKER_HIT_WIDTH = 8;

// Typing bursts are coalesced into a single diff.
var UPDATE_DELAY_MS = 150;

// Bail out on pathological inputs rather than hang the UI thread.
var MAX_COMPUTATION_MS = 250;

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
   this.baseLines = null;
   this.chunks = [];
   this.rowClasses = {};
   this.updateTimer = null;
   this.ignoreWhitespace = false;
   this.peek = null;

   this.onAfterRender = this.onAfterRender.bind(this);
   this.scheduleUpdate = this.scheduleUpdate.bind(this);
   this.onGutterMouseDown = this.onGutterMouseDown.bind(this);

   var gutter = editor.renderer.$gutterLayer;
   dom.addCssClass(gutter.element, GUTTER_CLASS);
   gutter.on("afterRender", this.onAfterRender);
   editor.on("change", this.scheduleUpdate);

   // Capture phase, so this runs ahead of Ace's own gutter handler (and
   // RStudio's breakpoint toggle, which hangs off it).
   editor.renderer.$gutter.addEventListener("mousedown", this.onGutterMouseDown, true);
};

(function() {

   // Sets the text to diff against; null clears all markers.
   this.setBase = function(text) {
      this.baseLines = (text == null) ? null : text.split(/\r?\n/);
      this.update();
   };

   this.setIgnoreWhitespace = function(ignore) {
      this.ignoreWhitespace = !!ignore;
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

   this.update = function() {
      var chunks = [];
      if (this.baseLines != null)
      {
         var DiffProvider = require("ace/ext/diff/providers/default").DiffProvider;
         var lines = this.editor.session.doc.getAllLines();
         chunks = new DiffProvider().compute(this.baseLines, lines, {
            maxComputationTimeMs: MAX_COMPUTATION_MS,
            ignoreTrimWhitespace: this.ignoreWhitespace
         });
      }

      this.setChunks(chunks);
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
      var chunk = this.chunkAt(this.editor.getCursorPosition().row);
      if (!chunk)
         return false;

      this.revertChunk(chunk);
      return true;
   };

   // Replaces the chunk's lines in the buffer with the base version's. The
   // document's trailing line needs care: a chunk reaching the end of the
   // document owns the newline before it rather than one after it.
   this.revertChunk = function(chunk) {
      var session = this.editor.session;
      var doc = session.doc;
      var oldText = this.baseLines.slice(chunk.old.start.row, chunk.old.end.row).join("\n");
      var start = chunk.new.start.row;
      var end = chunk.new.end.row;
      var length = doc.getLength();

      var range, text;
      if (end < length)
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

   this.onGutterMouseDown = function(e) {
      if (e.button !== 0 || !e.target.closest)
         return;

      var cell = e.target.closest(".ace_gutter-cell");
      if (!cell)
         return;

      if (e.clientX < cell.getBoundingClientRect().right - MARKER_HIT_WIDTH)
         return;

      var row = this.rowForCell(cell);
      var chunk = row == null ? null : this.chunkAt(row);
      if (!chunk)
         return;

      e.preventDefault();
      e.stopImmediatePropagation();

      if (this.peek && this.peek.chunk === chunk)
         this.closePeek();
      else
         this.showPeek(chunk);
   };

   this.dispose = function() {
      if (this.updateTimer)
      {
         clearTimeout(this.updateTimer);
         this.updateTimer = null;
      }

      this.closePeek();
      this.editor.off("change", this.scheduleUpdate);
      this.editor.renderer.$gutter.removeEventListener("mousedown", this.onGutterMouseDown, true);

      // Strip our classes from the visible cells before letting go of the
      // render hook, since nothing else will.
      this.rowClasses = {};
      this.chunks = [];
      this.onAfterRender();

      var gutter = this.editor.renderer.$gutterLayer;
      gutter.off("afterRender", this.onAfterRender);
      dom.removeCssClass(gutter.element, GUTTER_CLASS);
   };

}).call(GitDiffGutter.prototype);

exports.GitDiffGutter = GitDiffGutter;

});
