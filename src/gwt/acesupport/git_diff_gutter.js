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
// in the editor gutter. The diff itself comes from Ace's ext/diff provider,
// which must be loaded before the first update.

define("rstudio/git_diff_gutter", ["require", "exports", "module"], function(require, exports, module) {

var dom = require("ace/lib/dom");

var GUTTER_CLASS = "ace_git-gutter";

var ADDED_CLASS         = "ace_git-added";
var MODIFIED_CLASS      = "ace_git-modified";
var DELETED_CLASS       = "ace_git-deleted";
var DELETED_BELOW_CLASS = "ace_git-deleted-below";

var CELL_CLASSES = [ADDED_CLASS, MODIFIED_CLASS, DELETED_CLASS, DELETED_BELOW_CLASS];

// Typing bursts are coalesced into a single diff.
var UPDATE_DELAY_MS = 150;

// Bail out on pathological inputs rather than hang the UI thread.
var MAX_COMPUTATION_MS = 250;

var GitDiffGutter = function(editor) {
   this.editor = editor;
   this.baseLines = null;
   this.chunks = [];
   this.rowClasses = {};
   this.updateTimer = null;

   this.onAfterRender = this.onAfterRender.bind(this);
   this.scheduleUpdate = this.scheduleUpdate.bind(this);

   var gutter = editor.renderer.$gutterLayer;
   dom.addCssClass(gutter.element, GUTTER_CLASS);
   gutter.on("afterRender", this.onAfterRender);
   editor.on("change", this.scheduleUpdate);
};

(function() {

   // Sets the text to diff against; null clears all markers.
   this.setBase = function(text) {
      this.baseLines = (text == null) ? null : text.split(/\r?\n/);
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
            maxComputationTimeMs: MAX_COMPUTATION_MS
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
         if (start === end)
         {
            if (start > lastRow)
               add(lastRow, DELETED_BELOW_CLASS);
            else
               add(start, DELETED_CLASS);
            return;
         }

         var oldEmpty = chunk.old.start.row === chunk.old.end.row;
         var cls = oldEmpty ? ADDED_CLASS : MODIFIED_CLASS;
         for (var row = start; row < end; row++)
            add(row, cls);
      });

      this.rowClasses = rowClasses;

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

   this.dispose = function() {
      if (this.updateTimer)
      {
         clearTimeout(this.updateTimer);
         this.updateTimer = null;
      }

      this.editor.off("change", this.scheduleUpdate);

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
