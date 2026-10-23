/*
 * git_diff_split.js
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

// Shows a document's changes side by side: a read-only editor holding the
// base version (the Git index) next to the live editor, with the lines of
// each change highlighted on both sides, padding inserted so that unchanged
// lines stay level, and scrolling kept in step. The diff itself comes from
// the GitDiffGutter on the live editor, so the two always agree.
//
// Built on Ace's SplitDiffView (ext/diff), which is written for a pair of
// editors it owns outright. The live editor here belongs to RStudio, so the
// parts that would take it over -- forcing editor options, replacing the
// scrollbar, and wiping its line widgets on every realignment -- are
// overridden below.

define("rstudio/git_diff_split", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var SplitDiffView = require("ace/ext/diff/split_diff_view").SplitDiffView;

var BASE_CLASS = "ace_git-diff-base";
var EMPTY_CLASS = "ace_git-diff-base-empty";

var EMPTY_MESSAGE = "This file has no staged or committed version to compare with.";

// Renderer options the base editor copies from the live one, so that the two
// sides look alike.
var RENDERER_OPTIONS = [
   "showGutter",
   "showLineNumbers",
   "showFoldWidgets",
   "fadeFoldWidgets",
   "fixedWidthGutter",
   "showInvisibles",
   "displayIndentGuides",
   "scrollPastEnd",
   "animatedScroll"
];

// Walks a session's folds, nested ones included.
function eachFold(folds, callback) {
   folds.forEach(function(fold) {
      callback(fold);
      if (fold.subFolds && fold.subFolds.length > 0)
         eachFold(fold.subFolds, callback);
   });
}

var GitDiffSplitView = function(gutter, container) {
   this.gutter = gutter;
   this.container = container;
   this.alignmentWidgets = [];
   this.focusHandlers = [];
   this.foldsStale = true;
   this.scrollSynced = false;

   this.onBaseFocus = this.onBaseFocus.bind(this);
   this.syncFont = this.syncFont.bind(this);
   this.syncMode = this.syncMode.bind(this);
   this.syncTabSize = this.syncTabSize.bind(this);
   this.syncWrap = this.syncWrap.bind(this);

   this.liveEditor = gutter.editor;
   this.baseEditor = this.createBaseEditor();

   // SplitDiffView's constructor takes the diff model and wires up the two
   // editors (scroll, wheel, selection, fold, and wrap handlers, plus the
   // change-highlight markers); everything it would do to the editors beyond
   // that is turned off by the overrides below
   SplitDiffView.call(this, { editorA: this.baseEditor, editorB: this.liveEditor });

   this.attachLiveHandlers();
   gutter.setSplitView(this);
};

oop.inherits(GitDiffSplitView, SplitDiffView);

(function() {

   this.createBaseEditor = function() {
      var liveEditor = this.liveEditor;

      var editorEl = document.createElement("div");
      editorEl.className = BASE_CLASS;
      this.container.appendChild(editorEl);

      var emptyEl = document.createElement("div");
      emptyEl.className = EMPTY_CLASS;
      emptyEl.textContent = EMPTY_MESSAGE;
      emptyEl.style.display = "none";
      this.container.appendChild(emptyEl);
      this.emptyEl = emptyEl;

      var loader = require("rstudio/loader");
      var editor = loader.loadEditor(editorEl);
      editor.setReadOnly(true);
      editor.renderer.theme = liveEditor.renderer.theme;
      editor.renderer.setHScrollBarAlwaysVisible(false);
      editor.setHighlightActiveLine(false);
      editor.setHighlightGutterLine(false);
      editor.setShowPrintMargin(false);
      editor.setOption("useResizeObserver", false);
      editor.on("focus", this.onBaseFocus);

      RENDERER_OPTIONS.forEach(function(option) {
         editor.renderer.setOption(option, liveEditor.renderer.getOption(option));
      });

      this.baseEditor = editor;
      this.syncFont();

      var session = editor.session;
      session.setUseSoftTabs(liveEditor.session.getUseSoftTabs());
      session.setUseWorker(false);
      this.syncTabSize();
      this.syncWrap();
      this.syncMode();

      return editor;
   };

   // ---- Kept in step with the live editor ---------------------------------

   // The base editor sits inside its pane rather than being the pane itself
   // (the notice shares the pane), so the editor font preferences, applied
   // to the live editor's own element, do not reach it, and Ace's stylesheet
   // would give it Ace's default font. It takes the live editor's size and
   // line height as they come out, and follows them: a change in either
   // shows up as a change in the live editor's character size.
   this.syncFont = function() {
      var live = window.getComputedStyle(this.liveEditor.container);
      var style = this.baseEditor.container.style;
      if (style.fontSize === live.fontSize && style.lineHeight === live.lineHeight)
         return;

      style.fontSize = live.fontSize;
      style.lineHeight = live.lineHeight;
      this.baseEditor.renderer.updateFontSize();
   };

   // Tokenization and folding come from a mode of the live session's kind.
   // RStudio modes bind to the session they are built for, so the base
   // session gets one of its own rather than sharing the live one.
   this.syncMode = function() {
      var session = this.baseEditor.session;
      var liveMode = this.liveEditor.session.getMode();
      if (liveMode && !(session.getMode() instanceof liveMode.constructor))
         session.setMode(new liveMode.constructor(false, session));
   };

   this.syncTabSize = function() {
      this.baseEditor.session.setTabSize(this.liveEditor.session.getTabSize());
   };

   // Both sides wrap at the live editor's effective wrap column, whether that
   // is the margin column or (with viewport-based wrapping) the live pane's
   // own width, so that an unchanged line takes the same number of screen
   // rows on each side however the splitter is placed. Left to its own
   // width, a narrower base would wrap unchanged lines onto more rows than
   // the document, and the two would fall out of step until the next change.
   this.syncWrap = function() {
      var live = this.liveEditor.session;
      var session = this.baseEditor.session;

      var wrap = live.getUseWrapMode();
      session.setUseWrapMode(wrap);
      if (!wrap)
         return;

      var limit = live.getWrapLimit();
      if (limit > 0)
         session.setWrapLimitRange(limit, limit);
   };

   this.attachLiveHandlers = function() {
      var session = this.liveEditor.session;
      session.on("changeMode", this.syncMode);
      session.on("changeTabSize", this.syncTabSize);
      session.on("changeWrapMode", this.syncWrap);
      session.on("changeWrapLimit", this.syncWrap);
      this.liveEditor.renderer.on("changeCharacterSize", this.syncFont);
   };

   this.detachLiveHandlers = function() {
      var session = this.liveEditor.session;
      session.off("changeMode", this.syncMode);
      session.off("changeTabSize", this.syncTabSize);
      session.off("changeWrapMode", this.syncWrap);
      session.off("changeWrapLimit", this.syncWrap);
      this.liveEditor.renderer.off("changeCharacterSize", this.syncFont);
   };

   // Ace keeps the two sides scrolling together from the moment the view is
   // created, but starts the base at the top; once the base has a size to
   // scroll within, it joins the document wherever that is.
   this.syncInitialScroll = function() {
      if (this.scrollSynced || this.baseEditor.renderer.$size.scrollerHeight <= 0)
         return;

      this.scrollSynced = true;
      this.baseEditor.session.setScrollTop(this.liveEditor.session.getScrollTop());
   };

   // ---- Folds --------------------------------------------------------------

   // Ace pairs a fold made on one side with one on the other (fold.other),
   // or, where the other side has nothing to fold, folds away the other
   // side's line widget (fold.lineWidget) -- and undoes both when the fold
   // goes. The base's folds are about to go without that (its text is being
   // replaced, or the view closed): unpair them, and hand back what they
   // hold, so that nothing on the live side follows them out.
   this.unlinkFolds = function() {
      var live = this.liveEditor.session;

      eachFold(this.baseEditor.session.getAllFolds(), function(fold) {
         if (fold.other) {
            fold.other.other = null;
            fold.other = null;
         }

         if (fold.lineWidget) {
            // alignment padding (no element) is laid out afresh anyway
            if (fold.lineWidget.el)
               live.widgetManager.addLineWidget(fold.lineWidget);
            fold.lineWidget = null;
         }
      });

      eachFold(live.getAllFolds(), function(fold) {
         fold.other = null;
      });
   };

   // A fresh base (the view just opened, or the index moved) has no folds,
   // while the document keeps its own; fold the base to match, so that
   // unchanged lines stay level, pairing the folds up as Ace does for a fold
   // made while the view is open. Nested folds come along inside their
   // parents. A fold whose lines the base does not have is left out.
   this.mirrorFolds = function() {
      var self = this;
      var session = this.baseEditor.session;

      this.$syncingFold = true;
      try {
         this.liveEditor.session.getAllFolds().forEach(function(fold) {
            if (fold.other)
               return;

            var range = self.transformRange(fold.range, false);
            if (range.isEmpty())
               return;

            var other;
            try {
               other = session.addFold(fold.placeholder, range);
            } catch (error) {
               return;
            }

            if (other) {
               fold.other = other;
               other.other = fold;
            }
         });
      } finally {
         this.$syncingFold = false;
      }
   };

   // ---- SplitDiffView overrides ------------------------------------------

   // Ace's version applies a fixed set of editor options to both sides (no
   // active-line highlight, its custom scrollbar, and so on); the live editor
   // is RStudio's to configure, and the base editor was set up to match it.
   this.$setupModels = function(diffModel) {
      this.setProvider(diffModel.diffProvider || { compute: function() { return []; } });
      this.showSideA = true;
      this.editorA = diffModel.editorA;
      this.editorB = diffModel.editorB;
      this.setDiffSession({
         sessionA: this.editorA.session,
         sessionB: this.editorB.session,
         chunks: []
      });
   };

   // Scrollbar change zones need Ace's custom scrollbar, which RStudio does
   // not use.
   this.setupScrollbars = function() {};
   this.updateScrollBarDecorators = function() {};

   // The live editor's gutter is decorated by the GitDiffGutter already, and
   // the base side's highlighted lines speak for themselves.
   this.addGutterDecorators = function() {};

   // The diff arrives from the gutter (see setChunks); Ace's recompute on
   // every keystroke is not wanted on top of it.
   this.onInput = function() {};
   this.scheduleOnInput = function() {};

   // Ace's search-highlight sync assumes the other session has been through
   // a selection change already, which the base editor may not have.
   this.searchHighlight = function(selection) {
      if (!selection.session.$searchHighlight)
         return;
      SplitDiffView.prototype.searchHighlight.call(this, selection);
   };

   // Alignment padding is applied as line widgets without content. Ace's
   // align() clears every line widget in both sessions before placing its
   // own; here only the previous padding goes, so that notebook chunk output
   // and other widgets on the live side survive. A row already holding one
   // of those gets no padding (the next change boundary realigns the sides),
   // since Ace shows a single widget per row.
   this.$initWidgets = function(editor) {
      var session = editor.session;
      if (!session.widgetManager) {
         var LineWidgets = require("ace/line_widgets").LineWidgets;
         session.widgetManager = new LineWidgets(session);
         session.widgetManager.attach(editor);
      }

      this.$removeLineWidgets(session);
      if (!session.lineWidgets)
         session.lineWidgets = [];
      session.$resetRowCache(0);
   };

   this.$addWidget = function(session, w) {
      var existing = session.lineWidgets[w.row];
      if (existing) {
         if (this.alignmentWidgets.indexOf(existing) === -1)
            return;

         // a change's start and end can pad the same row (an addition has
         // an empty range on the base side); the paddings add up
         w.rowsAbove += existing.rowsAbove > w.rowsAbove ? existing.rowsAbove : w.rowsAbove;
         w.rowCount += existing.rowCount;
         this.alignmentWidgets.splice(this.alignmentWidgets.indexOf(existing), 1);
      }

      w.session = session;
      session.lineWidgets[w.row] = w;
      this.alignmentWidgets.push(w);
      session.$resetRowCache(w.row);

      var fold = session.getFoldAt(w.row, 0);
      if (fold)
         session.widgetManager.updateOnFold({ data: fold, action: "add" }, session);
   };

   // Ace drops a padding widget itself when the rows around it are deleted
   // (clearing its session as it does), so only those still in a session
   // are kept track of.
   this.$removeLineWidgets = function(session) {
      var widgets = session.lineWidgets;
      var mine = this.alignmentWidgets.filter(function(w) { return w.session === session; });
      this.alignmentWidgets = this.alignmentWidgets.filter(function(w) { return w.session && w.session !== session; });

      if (!widgets || mine.length === 0)
         return;

      // A widget added on top of ours chains to it ($oldWidget), and a fold
      // ending on our row aliases it at the fold's start; unlink it wherever
      // it is found.
      for (var i = 0; i < widgets.length; i++) {
         var w = widgets[i];
         if (w && mine.indexOf(w) !== -1) {
            widgets[i] = w.$oldWidget;
            w = widgets[i];
         }

         while (w && w.$oldWidget) {
            if (mine.indexOf(w.$oldWidget) !== -1)
               w.$oldWidget = w.$oldWidget.$oldWidget;
            else
               w = w.$oldWidget;
         }
      }

      session.$resetRowCache(0);
      session._signal("changeFold", { data: { start: { row: 0 } } });
   };

   // Ace's detach also restores the options it had forced (none here) and
   // collapses the selection in both editors, which the live one should keep.
   this.detach = function() {
      this.editorA.renderer.off("beforeRender", this.realign);
      this.editorB.renderer.off("beforeRender", this.realign);
      this.$detachEventHandlers();
      this.$removeLineWidgets(this.sessionA);
      this.$removeLineWidgets(this.sessionB);
   };

   // ---- Driven by the gutter ---------------------------------------------

   // The base lines as the gutter split them (null when there is no base);
   // the base session shows them verbatim so that chunk rows line up.
   this.setBaseLines = function(lines) {
      var session = this.baseEditor.session;
      var text = lines ? lines.join("\n") : "";
      if (session.getValue() !== text) {
         // Replacing the text removes the base's folds, which Ace's fold
         // sync would carry over to their live counterparts; the document
         // keeps its folds, and the base gets them back once the diff
         // against the new base is in (see mirrorFolds).
         this.unlinkFolds();
         this.$syncingFold = true;
         try {
            session.setValue(text);
         } finally {
            this.$syncingFold = false;
         }
         this.foldsStale = true;
      }

      this.emptyEl.style.display = lines ? "none" : "";
      this.baseEditor.renderer.theme = this.liveEditor.renderer.theme;
   };

   // Whitespace-insensitive diffs were computed on lines with their
   // whitespace stripped, so their character ranges do not fit the text and
   // only the line ranges are kept.
   this.setChunks = function(chunks, ignoreWhitespace) {
      if (ignoreWhitespace) {
         chunks = chunks.map(function(chunk) {
            return { old: chunk.old, new: chunk.new };
         });
      }

      this.diffSession.chunks = this.chunks = chunks;

      if (this.foldsStale) {
         this.foldsStale = false;
         this.mirrorFolds();
      }

      this.align();
      this.editorA.renderer.updateBackMarkers();
      this.editorB.renderer.updateBackMarkers();
      this.syncInitialScroll();
   };

   this.resize = function() {
      this.baseEditor.resize();
      this.syncInitialScroll();
   };

   this.isFocused = function() {
      return this.baseEditor.isFocused();
   };

   this.focus = function() {
      this.baseEditor.focus();
   };

   // Runs the handler whenever the base editor takes focus, so that the
   // owning document can be made current and its base re-fetched, as when
   // the live editor is clicked into.
   this.onFocus = function(handler) {
      this.focusHandlers.push(handler);
   };

   this.onBaseFocus = function() {
      this.focusHandlers.forEach(function(handler) {
         handler();
      });
   };

   // The base cursor's row, for the gutter's cursor-driven commands when the
   // base pane holds the cursor.
   this.baseCursorRow = function() {
      return this.baseEditor.getCursorPosition().row;
   };

   this.goToBaseRow = function(row) {
      this.baseEditor.gotoLine(row + 1, 0, true);
   };

   this.dispose = function() {
      this.gutter.setSplitView(null);
      this.detachLiveHandlers();
      this.unlinkFolds();

      // unhooks both editors and restores the live one; the base editor is
      // ours to destroy (destroy() would take the live one with it)
      this.detach();
      this.baseEditor.destroy();

      var editorEl = this.baseEditor.container;
      if (editorEl.parentNode)
         editorEl.parentNode.removeChild(editorEl);
      if (this.emptyEl.parentNode)
         this.emptyEl.parentNode.removeChild(this.emptyEl);
   };

}).call(GitDiffSplitView.prototype);

exports.GitDiffSplitView = GitDiffSplitView;

});
