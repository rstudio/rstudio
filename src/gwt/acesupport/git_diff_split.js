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

// Text shown to the user; the IDE passes its translations in (see
// GitDiffSplitNative), these stand in without them.
var DEFAULT_STRINGS = {
   baseEditor: "Staged or committed version, read-only",
   noBase: "This file has no staged or committed version to compare with."
};

// Renderer options the base editor copies from the live one, and keeps
// following (see syncRendererOptions), so that the two sides look alike.
var RENDERER_OPTIONS = [
   "showGutter",
   "showFoldWidgets",
   "fadeFoldWidgets",
   "fixedWidthGutter",
   "showInvisibles",
   "displayIndentGuides",
   "scrollPastEnd",
   "animatedScroll"
];

// The same, for the options Ace keeps on the editor rather than its
// renderer (asking the renderer for one of these gets a warning).
var EDITOR_OPTIONS = [
   "showLineNumbers"
];

// A nested fold's range in document coordinates, from the range Ace keeps
// relative to the enclosing fold's start (see Fold.restoreRange).
function absoluteRange(range, start) {
   var absolute = range.clone();
   [absolute.start, absolute.end].forEach(function(point) {
      point.row += start.row;
      if (point.row === start.row)
         point.column += start.column;
   });
   return absolute;
}

// Copies the options named that differ between two Ace option holders.
function syncOptions(from, to, options) {
   options.forEach(function(option) {
      var value = from.getOption(option);
      if (to.getOption(option) !== value)
         to.setOption(option, value);
   });
}

// The alignment padding is told from other line widgets (notebook chunk
// output, equation previews) by a mark of its own.
function isPadding(widget) {
   return !!(widget && widget.gitDiffPadding);
}

// A copy of a session's line widgets with those on the given rows that are
// not padding replaced by hidden stand-ins of the same height, or null when
// there are none to replace; see $restrictStripes.
function foreignWidgetsHidden(widgets, firstRow, lastRow) {
   var copy = null;
   var last = Math.min(lastRow, widgets.length - 1);
   for (var row = firstRow; row <= last; row++) {
      var w = widgets[row];
      if (!w || w.hidden || isPadding(w))
         continue;

      if (!copy)
         copy = widgets.slice();
      copy[row] = { rowCount: w.rowCount, rowsAbove: w.rowsAbove, hidden: true };
   }
   return copy;
}

// Walks a session's folds, nested ones included.
function eachFold(folds, callback) {
   folds.forEach(function(fold) {
      callback(fold);
      if (fold.subFolds && fold.subFolds.length > 0)
         eachFold(fold.subFolds, callback);
   });
}

// Unpairs folds (nested ones included) from their counterparts on the
// other side.
function unpairFolds(folds) {
   eachFold(folds, function(fold) {
      if (fold.other) {
         fold.other.other = null;
         fold.other = null;
      }
   });
}

var GitDiffSplitView = function(gutter, container, strings) {
   this.gutter = gutter;
   this.container = container;
   this.strings = strings || DEFAULT_STRINGS;
   this.$aligning = false;
   this.focusHandlers = [];
   this.foldsStale = true;
   this.hasBase = false;
   this.scrollSynced = false;

   this.onBaseFocus = this.onBaseFocus.bind(this);
   this.syncFont = this.syncFont.bind(this);
   this.syncMode = this.syncMode.bind(this);
   this.syncRendererOptions = this.syncRendererOptions.bind(this);
   this.syncTabSize = this.syncTabSize.bind(this);
   this.syncWrap = this.syncWrap.bind(this);

   this.liveEditor = gutter.editor;
   this.baseEditor = this.createBaseEditor();

   // SplitDiffView's constructor takes the diff model and wires up the two
   // editors (scroll, wheel, selection, fold, and wrap handlers, plus the
   // change-highlight markers); everything it would do to the editors beyond
   // that is turned off by the overrides below
   SplitDiffView.call(this, { editorA: this.baseEditor, editorB: this.liveEditor });

   this.$restrictStripes(this.markerA);
   this.$restrictStripes(this.markerB);
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

      // a live region, so that the notice is announced when it appears
      var emptyEl = document.createElement("div");
      emptyEl.className = EMPTY_CLASS;
      emptyEl.setAttribute("role", "status");
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

      // The label of the read-only base, for assistive technology: on the
      // input itself, as the IDE labels its editors, and as the prefix Ace
      // puts ahead of "Cursor at row N" when its keyboard accessibility
      // mode relabels the input on focus.
      editor.setOption("textInputAriaLabel", this.strings.baseEditor);
      editor.textInput.getElement().setAttribute("aria-label", this.strings.baseEditor);

      this.baseEditor = editor;
      this.syncRendererOptions();
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

   // The display options have no change event of their own; they are
   // checked as the live editor renders, which a change to any of them
   // brings about.
   this.syncRendererOptions = function() {
      syncOptions(this.liveEditor.renderer, this.baseEditor.renderer, RENDERER_OPTIONS);
      syncOptions(this.liveEditor, this.baseEditor, EDITOR_OPTIONS);
   };

   // Tokenization and folding come from a mode of the live session's kind.
   // RStudio modes bind to the session they are built for, so the base
   // session gets one of its own rather than sharing the live one.
   this.syncMode = function() {
      var session = this.baseEditor.session;
      var liveMode = this.liveEditor.session.getMode();
      if (!liveMode)
         return;

      // the modes derive from one another (R from Text, say), so only the
      // very same kind will do
      var mode = session.getMode();
      if (!mode || mode.constructor !== liveMode.constructor)
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

      // RStudio turns Ace's indented soft wrap off for its editors (see
      // AceEditorWidget); left on in the base, an indented line can wrap
      // onto more rows there than in the document
      var indented = live.getOption("indentedSoftWrap");
      if (session.getOption("indentedSoftWrap") !== indented)
         session.setOption("indentedSoftWrap", indented);

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
      this.liveEditor.renderer.on("afterRender", this.syncRendererOptions);
   };

   this.detachLiveHandlers = function() {
      var session = this.liveEditor.session;
      session.off("changeMode", this.syncMode);
      session.off("changeTabSize", this.syncTabSize);
      session.off("changeWrapMode", this.syncWrap);
      session.off("changeWrapLimit", this.syncWrap);
      this.liveEditor.renderer.off("changeCharacterSize", this.syncFont);
      this.liveEditor.renderer.off("afterRender", this.syncRendererOptions);
   };

   // Ace keeps the two sides scrolling together from the moment the view is
   // created, but starts the base at the top; once the base has a size to
   // scroll within, and lines to scroll, it joins the document wherever
   // that is. Without a base (none yet, or none at all) the offset would
   // be clamped to the top, and Ace's scroll sync would take the document
   // there too.
   this.syncInitialScroll = function() {
      if (this.scrollSynced || !this.hasBase || this.baseEditor.renderer.$size.scrollerHeight <= 0)
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

   // Pairs a fold (given with its range in document coordinates, and its
   // side: isOrig for the base) with a new fold of the same lines on the
   // other side, as Ace does for a fold made while the view is open -- or
   // with nothing, if the other side has none of those lines. Then the
   // folds nested in it, each as a fold of its own: Ace nests a fold added
   // within another, and keeps a nested fold's range relative to its
   // parent's start. Expanding the fold on either side then restores them
   // on both (see onChangeFold).
   this.mirrorFold = function(fold, range, isOrig) {
      var other = isOrig ? this.sessionB : this.sessionA;

      if (!fold.other) {
         var otherRange = this.transformRange(range, isOrig);
         if (!otherRange.isEmpty()) {
            var counterpart = null;
            try {
               counterpart = other.addFold(fold.placeholder, otherRange);
            } catch (error) {
               // no room for it there: the lines are changed, or already
               // folded another way
            }

            if (counterpart) {
               fold.other = counterpart;
               counterpart.other = fold;
            }
         }
      }

      var self = this;
      fold.subFolds.forEach(function(subFold) {
         self.mirrorFold(subFold, absoluteRange(subFold.range, range.start), isOrig);
      });
   };

   // A fresh base (the view just opened, or the index moved) has no folds,
   // while the document keeps its own; fold the base to match, so that
   // unchanged lines stay level.
   this.mirrorFolds = function() {
      var self = this;

      this.$syncingFold = true;
      try {
         this.liveEditor.session.getAllFolds().forEach(function(fold) {
            self.mirrorFold(fold, fold.range, false);
         });
      } finally {
         this.$syncingFold = false;
      }
   };

   // Ace's handler, with two differences. A fold added over folds already
   // there takes them in as nested folds (Ace removes them, which took their
   // counterparts with them, and re-adds them under it); the counterpart
   // made here gets nested folds of its own, so that expanding the fold on
   // either side -- a plain removal of the counterpart, nested folds and
   // all, followed by the re-adding of this side's nested folds -- brings
   // them back on both. And a fold whose counterpart would be empty folds
   // away the line widget on the row that follows on the other side only
   // if that widget is this view's alignment padding, never a notebook
   // chunk's output or an equation preview.
   this.onChangeFold = function(ev, session) {
      var fold = ev.data;
      if (this.$syncingFold || !fold)
         return;

      // A line widget being added, removed or resized (chunk output run,
      // collapsed or expanded) is reported as a changeFold without an
      // action, which Ace's handler ignores; it moves the rows below it,
      // so the sides are realigned -- unless it is this view's own
      // alignment at work (see align).
      if (!ev.action) {
         if (!this.$aligning)
            this.scheduleRealign();
         return;
      }

      this.scheduleRealign();
      var isOrig = session === this.sessionA;
      var other = isOrig ? this.sessionB : this.sessionA;

      if (ev.action === "remove") {
         var counterpart = fold.other;
         if (counterpart) {
            // The counterpart goes with its nested folds. Those paired with
            // nested folds of this one come back if this is an expansion
            // (Ace re-adds this side's, and they are mirrored); those with
            // no twin here -- folds of lines only the other side has --
            // would be lost, so they are put back by hand.
            var orphans = counterpart.subFolds.filter(function(sub) {
               return !sub.other;
            });
            unpairFolds([fold]);
            unpairFolds([counterpart]);
            if (counterpart.foldLine && counterpart.foldLine.folds.indexOf(counterpart) !== -1)
               other.removeFold(counterpart);

            this.$syncingFold = true;
            try {
               orphans.forEach(function(sub) {
                  counterpart.restoreRange(sub);
                  try {
                     other.addFold(sub);
                  } catch (error) {
                     // its lines are folded another way by now
                  }
               });
            } finally {
               this.$syncingFold = false;
            }
         } else if (fold.lineWidget) {
            // the padding comes back with the next alignment
            fold.lineWidget = null;
         }
      } else if (ev.action === "add") {
         var range = this.transformRange(fold.range, isOrig);
         if (range.isEmpty()) {
            var widget = other.lineWidgets && other.lineWidgets[range.start.row + 1];
            if (isPadding(widget)) {
               fold.lineWidget = widget;
               other.widgetManager.removeLineWidget(widget);
               (isOrig ? this.editorB : this.editorA).renderer.updateBackMarkers();
            }
         } else {
            this.$syncingFold = true;
            try {
               this.mirrorFold(fold, fold.range, isOrig);
            } finally {
               this.$syncingFold = false;
            }
         }
      }
   };

   // ---- SplitDiffView overrides ------------------------------------------

   // Ace's align, marked while it runs: the changeFold events it raises
   // as it lays the padding out are told from a line widget changing on
   // its own (see onChangeFold).
   this.align = function() {
      this.$aligning = true;
      try {
         SplitDiffView.prototype.align.call(this);
      } finally {
         this.$aligning = false;
      }
   };

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
         if (!isPadding(existing))
            return;

         // a change's start and end can pad the same row (an addition has
         // an empty range on the base side); the paddings add up
         w.rowsAbove += existing.rowsAbove > w.rowsAbove ? existing.rowsAbove : w.rowsAbove;
         w.rowCount += existing.rowCount;
      }

      w.gitDiffPadding = true;
      w.session = session;
      session.lineWidgets[w.row] = w;
      session.$resetRowCache(w.row);

      var fold = session.getFoldAt(w.row, 0);
      if (fold)
         session.widgetManager.updateOnFold({ data: fold, action: "add" }, session);
   };

   // Takes the padding out of a session's line widgets. A widget added on
   // top of a padding chains to it ($oldWidget), and a fold ending on its
   // row aliases it at the fold's start; it is unlinked wherever found.
   this.$removeLineWidgets = function(session) {
      var widgets = session.lineWidgets;
      if (!widgets)
         return;

      var removed = false;
      for (var i = 0; i < widgets.length; i++) {
         var w = widgets[i];
         if (isPadding(w)) {
            widgets[i] = w.$oldWidget;
            w = widgets[i];
            removed = true;
         }

         while (w && w.$oldWidget) {
            if (isPadding(w.$oldWidget)) {
               w.$oldWidget = w.$oldWidget.$oldWidget;
               removed = true;
            } else {
               w = w.$oldWidget;
            }
         }
      }

      if (!removed)
         return;

      session.$resetRowCache(0);
      var aligning = this.$aligning;
      this.$aligning = true;
      try {
         session._signal("changeFold", { data: { start: { row: 0 } } });
      } finally {
         this.$aligning = aligning;
      }
   };

   // Ace's change highlighter stripes the rows of every line widget in a
   // session as alignment padding, not only ours: chunk output hides the
   // stripes under its own background, but a multi-line Assistant suggestion
   // has none. For the duration of a draw, a foreign widget on a row being
   // drawn (the only rows the highlighter looks at) is stood in for by a
   // hidden copy of the same height, which keeps its rows where they are
   // and has the highlighter pass it over.
   this.$restrictStripes = function(marker) {
      var update = marker.update;
      marker.update = function(html, markerLayer, session, config) {
         var widgets = session.lineWidgets;
         var swapped = widgets && foreignWidgetsHidden(widgets, config.firstRow, config.lastRow);
         if (swapped)
            session.lineWidgets = swapped;

         try {
            update.call(marker, html, markerLayer, session, config);
         } finally {
            session.lineWidgets = widgets;
         }
      };
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

   // The base lines as the gutter split them (null while there is no base:
   // not fetched yet, not fetchable, or none to fetch); the base session
   // shows them verbatim so that chunk rows line up.
   this.setBaseLines = function(lines) {
      var session = this.baseEditor.session;
      var text = lines ? lines.join("\n") : "";
      this.hasBase = lines != null;
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

      this.baseEditor.renderer.theme = this.liveEditor.renderer.theme;
   };

   // Whether the document is known to have no staged or committed version,
   // as opposed to a base that is not (yet) at hand: only then does the
   // pane say so, rather than sit empty.
   this.setUntracked = function(untracked) {
      this.emptyEl.textContent = untracked ? this.strings.noBase : "";
      this.emptyEl.style.display = untracked ? "" : "none";
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
