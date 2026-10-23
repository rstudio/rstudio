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
var Range = require("ace/range").Range;
var Fold = require("ace/edit_session/fold").Fold;
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
   "showLineNumbers",
   "enableKeyboardAccessibility"
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
// Whether a range runs forward, with something in it. A fold's range mapped
// to the other side can come out empty or reversed (both ends land in the
// same changed stretch, say an added block's), and there is nothing there
// to fold.
function isForward(range) {
   return range.start.row < range.end.row ||
          (range.start.row === range.end.row && range.start.column < range.end.column);
}

// Whether a row exists and can take padding: it holds no line widget, or
// padding already.
function padFreeRow(session, row) {
   if (row < 0 || row >= session.getLength())
      return false;
   var widgets = session.lineWidgets;
   return !widgets || !widgets[row] || isPadding(widgets[row]);
}

// Whether two fold lines hold a pair of mirrored folds (see mirrorFold).
function foldLinesPaired(a, b) {
   return a.folds.some(function(fold) {
      return fold.other && b.folds.indexOf(fold.other) !== -1;
   });
}

// Whether a fold is in a session, at the top level or nested in one that is.
function foldPresent(session, fold) {
   var present = false;
   eachFold(session.getAllFolds(), function(candidate) {
      if (candidate === fold)
         present = true;
   });
   return present;
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
   this.$trackFoldOperations();
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
   // shows up as a change in the live editor's character size. The view
   // can be created while the live editor is out of the document (the
   // layout switching from a regular split), with no computed style to
   // take; it is checked again as the pane is laid out and aligned.
   this.syncFont = function() {
      var live = window.getComputedStyle(this.liveEditor.container);
      var style = this.baseEditor.container.style;
      if (!live.fontSize || (style.fontSize === live.fontSize && style.lineHeight === live.lineHeight))
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

   // Whether a range on the given side lies within a block of lines that the
   // other side has none of (added lines, seen from the document; removed
   // ones, from the base). A fold of such lines has nothing to pair with.
   this.$withinChange = function(range, isOrig) {
      var side = isOrig ? "old" : "new";
      var otherSide = isOrig ? "new" : "old";
      return this.chunks.some(function(chunk) {
         return chunk[otherSide].start.row === chunk[otherSide].end.row &&
                chunk[side].start.row <= range.start.row &&
                range.end.row < chunk[side].end.row;
      });
   };

   // The range a counterpart fold takes: the other side's own fold range
   // for the mapped first row, when it spans the mapped rows (Ace maps the
   // columns of a changed line only roughly -- a space added inside the
   // line puts the mapped start past the end of the base's), else the
   // mapped range, as long as it runs forward and fits as it is (Ace would
   // clip it into something else, or nothing). Null when there is none.
   this.$counterpartRange = function(session, mapped) {
      // (a range mapped to before the first line -- a fold over an added
      // first line and the one after it -- or past the last has no place)
      if (mapped.start.row < 0 || mapped.end.row >= session.getLength())
         return null;

      // The mode's own fold range is asked for the first row only where the
      // mode folds at all (Ace installs getFoldWidgetRange with the folding
      // rules; plain text has none), and taken only over the very rows
      // mapped: a structural fold that merely ends on the same row (a
      // comment ending where a folded selection starts) is not the fold.
      var own = null;
      if (typeof session.getFoldWidgetRange === "function") {
         try {
            own = session.getFoldWidgetRange(mapped.start.row);
         } catch (error) {
            // the mode could not say
         }
      }
      if (own && own.start.row === mapped.start.row && own.end.row === mapped.end.row && isForward(own))
         return own;

      var clipped = session.$clipRangeToDocument(mapped.clone());
      return isForward(mapped) && clipped.isEqual(mapped) ? mapped : null;
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

      // A fold re-added by an expansion keeps its counterpart, which went
      // with the expanded fold's (see onChangeFold): that one comes back,
      // nested folds and all, rather than a new one.
      var counterpart = fold.other;
      if (counterpart && !foldPresent(other, counterpart)) {
         try {
            other.addFold(counterpart);
         } catch (error) {
            fold.other = null;
            counterpart.other = null;
            counterpart = null;
         }
      }

      if (!counterpart && !this.$withinChange(range, isOrig)) {
         var otherRange = this.$counterpartRange(other, this.transformRange(range, isOrig));
         if (otherRange) {
            // Fold All folds the nested levels as they are expanded, and
            // drops the folds within as it is added; Ace reads that off the
            // range, so the counterpart gets it the same way
            otherRange.collapseChildren = fold.collapseChildren;
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
         if (!counterpart) {
            // the padding, if any, comes back with the next alignment
            fold.lineWidget = null;
         } else if (this.$addingFold > 0 && !this.$addingCollapses) {
            // Taken into a fold being added around it: Ace puts it back
            // inside the new fold, nested folds and all. The counterpart
            // stays, paired; the new fold's counterpart takes it in the same
            // way (see mirrorFold). Not so for a fold that collapses its
            // levels afresh, which drops it: so does its counterpart, below.
         } else {
            // The counterpart goes too, with its nested folds; only the pair
            // itself is undone, and the nested folds' ranges are made
            // absolute, as Ace does for the folds it re-adds on expansion
            // (each of which brings its own counterpart back; see
            // mirrorFold). An expansion restores the rest of the
            // counterpart's; a removal outright (Ctrl-click, Unfold All)
            // leaves them removed.
            fold.other = null;
            counterpart.other = null;
            if (foldPresent(other, counterpart))
               other.removeFold(counterpart);
            counterpart.subFolds.forEach(function(sub) {
               counterpart.restoreRange(sub);
            });

            if (this.$expandingFold === fold)
               this.$expandCounterpart(counterpart, other);
         }
      } else if (ev.action === "add") {
         // A fold can follow an edit before the gutter has re-run the diff
         // (undo restores removed lines, folds and all, in one go); the
         // mapping below is only as good as the chunks it uses
         this.gutter.flushUpdate();
         if (!this.hasBase)
            return;

         var range = this.transformRange(fold.range, isOrig);
         if (!isForward(range)) {
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

   // A fold's removal is reported the same way whether the fold is being
   // expanded, taken into a fold being added around it, or removed outright,
   // and its counterpart is treated differently in each case (see
   // onChangeFold): the sessions' expandFold and addFold are wrapped to
   // tell which is under way.
   this.$trackFoldOperations = function() {
      var self = this;
      this.$expandingFold = null;
      this.$addingFold = 0;
      this.$addingCollapses = false;
      this.$untrackFoldOperations = [this.sessionA, this.sessionB].map(function(session) {
         var expandFold = session.expandFold;
         var addFold = session.addFold;

         session.expandFold = function(fold) {
            var previous = self.$expandingFold;
            self.$expandingFold = fold;
            try {
               return expandFold.apply(this, arguments);
            } finally {
               self.$expandingFold = previous;
            }
         };

         // (a fold added with collapseChildren set -- Fold All, a recursive
         // collapse -- discards the folds within its range instead of
         // nesting them, to fold them afresh as it is expanded)
         session.addFold = function(placeholder, range) {
            var collapses = placeholder instanceof Fold
               ? placeholder.collapseChildren
               : range && range.collapseChildren;
            var previous = self.$addingCollapses;
            self.$addingFold++;
            self.$addingCollapses = !!collapses;
            try {
               return addFold.apply(this, arguments);
            } finally {
               self.$addingFold--;
               self.$addingCollapses = previous;
            }
         };

         return function() {
            session.expandFold = expandFold;
            session.addFold = addFold;
         };
      });
   };

   // The counterpart of a fold being expanded has been removed (see
   // onChangeFold). Ace is about to add the expanded fold's nested folds
   // back, which brings back the counterparts of those; the rest of the
   // counterpart's nested folds -- those with no twin on the expanded
   // side, which has none of their lines -- come back here. So do the
   // nested levels that Fold All left to be folded on expansion
   // (collapseChildren), within the counterpart's own range: they are
   // mirrored, and the expanded side's own lazy folding then finds them in
   // place.
   this.$expandCounterpart = function(counterpart, session) {
      this.$syncingFold = true;
      try {
         counterpart.subFolds.forEach(function(sub) {
            if (sub.other || foldPresent(session, sub))
               return;
            try {
               session.addFold(sub);
            } catch (error) {
               // its lines are folded another way by now
            }
         });
      } finally {
         this.$syncingFold = false;
      }

      if (counterpart.collapseChildren > 0)
         session.foldAll(counterpart.start.row + 1, counterpart.end.row, counterpart.collapseChildren - 1);
   };

   // ---- SplitDiffView overrides ------------------------------------------

   // Ace's screen row of a change boundary. At the end of the document the
   // position is past the last line, which Ace measures by clipping to the
   // last line and adding one; a line widget below that line (chunk output
   // on the last line of a file without a trailing newline) lies between
   // the two and is counted here.
   this.$screenRow = function(pos, session) {
      var row = SplitDiffView.prototype.$screenRow.call(this, pos, session);
      if (pos.row >= session.getLength()) {
         var widgets = session.lineWidgets;
         var last = widgets && widgets[session.getLength() - 1];
         if (last && !last.hidden && last.rowCount)
            row += last.rowCount - (last.rowsAbove || 0);
      }
      return row;
   };

   // Ace's realign runs on the next render. A render asked for from within
   // a render -- as a line widget's height change is reported, say -- is
   // dropped by Ace's render loop after a few in a row (its recursion
   // limit), and the realign with it, until something else renders; so a
   // frame of its own is asked for from outside the loop.
   this.scheduleRealign = function() {
      SplitDiffView.prototype.scheduleRealign.call(this);
      if (this.$realignFrame)
         return;

      var self = this;
      this.$realignFrame = setTimeout(function() {
         self.$realignFrame = null;
         if (self.realignPending && !self.disposed)
            self.liveEditor.renderer.updateFull();
      }, 0);
   };

   // Ace's align, with two differences. The sides are marked as being
   // aligned while it runs, so that the changeFold events it raises are
   // told from a line widget changing on its own (see onChangeFold). And
   // before the changes are padded for, the document's own line widgets
   // are: chunk output takes rows the base does not have, whether or not a
   // change lies below it to pad at (Ace pads at change boundaries only).
   // The change padding that follows measures screen rows, so it takes
   // this padding into account rather than counting the widget twice.
   this.align = function() {
      this.$aligning = true;
      try {
         this.$initWidgets(this.editorA);
         this.$initWidgets(this.editorB);
         if (this.hasBase) {
            this.$padForeignWidgets();
            this.$padWrapDifferences();
         }
         this.$padChanges();
         this.sessionA._emit("changeFold", { data: { start: { row: 0 } } });
         this.sessionB._emit("changeFold", { data: { start: { row: 0 } } });
      } finally {
         this.$aligning = false;
      }
   };

   // Pads the base below the row that corresponds to each of the document's
   // line widgets, by the widget's height.
   this.$padForeignWidgets = function() {
      var widgets = this.sessionB.lineWidgets;
      if (!widgets)
         return;

      var lastRow = this.sessionA.getLength() - 1;
      for (var row = 0; row < widgets.length; row++) {
         var w = widgets[row];
         if (!w || w.hidden || isPadding(w) || !w.rowCount)
            continue;

         // a widget on an added line has no base row of its own: the
         // padding at the end of the addition takes it in (its rows count
         // toward the addition's screen rows), whereas a row of padding
         // here would sit outside that measure and be added again
         if (this.$withinChange(new Range(row, 0, row, 0), false))
            continue;

         var baseRow = this.transformRange(new Range(row, 0, row, 0), false).start.row;
         this.$addWidget(this.sessionA, {
            rowCount: w.rowCount,
            rowsAbove: 0,
            row: Math.min(baseRow, lastRow)
         });
      }
   };

   // With a whitespace-insensitive diff and soft wrap on, a line whose only
   // difference is whitespace is no change to pad at, yet it can wrap onto
   // a different number of rows on each side; the matched lines are
   // compared row by row and the shorter side padded below the line.
   this.$padWrapDifferences = function() {
      if (!this.ignoreWhitespace || !this.sessionB.getUseWrapMode())
         return;

      var self = this;
      var baseRow = 0;
      var liveRow = 0;

      // pads the side whose line (measured at the first rows) shows fewer
      // rows, below the given rows
      var pad = function(baseLine, liveLine, baseBelow, liveBelow) {
         var difference = self.sessionB.getRowLineCount(liveLine) - self.sessionA.getRowLineCount(baseLine);
         if (difference > 0)
            self.$addWidget(self.sessionA, { rowCount: difference, rowsAbove: 0, row: baseBelow });
         else if (difference < 0)
            self.$addWidget(self.sessionB, { rowCount: -difference, rowsAbove: 0, row: liveBelow });
      };
      var compareUpTo = function(baseEnd, liveEnd) {
         while (baseRow < baseEnd && liveRow < liveEnd) {
            var baseFold = self.sessionA.getFoldLine(baseRow);
            var liveFold = self.sessionB.getFoldLine(liveRow);
            if (!baseFold && !liveFold) {
               pad(baseRow, liveRow, baseRow, liveRow);
               baseRow++;
               liveRow++;
               continue;
            }

            // A fold line shows as one line, wrapped as a whole (its first
            // row carries the wrap count; the rows folded away show none).
            // It is compared when the two sides' fold lines hold a pair of
            // mirrored folds (which can span different rows, with a change
            // inside), with any padding below its last row, where Ace shows
            // a folded row's widget; folded on one side only, the rows are
            // passed over.
            var paired = baseFold && liveFold &&
                         baseFold.start.row === baseRow && liveFold.start.row === liveRow &&
                         foldLinesPaired(baseFold, liveFold);
            if (paired) {
               pad(baseRow, liveRow, baseFold.end.row, liveFold.end.row);
               baseRow = baseFold.end.row + 1;
               liveRow = liveFold.end.row + 1;
            } else {
               baseRow++;
               liveRow++;
            }
         }
      };

      this.chunks.forEach(function(chunk) {
         compareUpTo(chunk.old.start.row, chunk.new.start.row);
         // (a fold line can run on past a change within it)
         baseRow = Math.max(baseRow, chunk.old.end.row);
         liveRow = Math.max(liveRow, chunk.new.end.row);
      });
      compareUpTo(this.sessionA.getLength(), this.sessionB.getLength());
   };

   // Ace's alignment of the changes: where a change starts or ends lower
   // on one side than the other, the other is padded up to it.
   this.$padChanges = function() {
      var self = this;
      this.chunks.forEach(function(ch) {
         self.$padBoundary(ch.old.start, ch.new.start);
         self.$padBoundary(ch.old.end, ch.new.end);
      });
   };

   this.$padBoundary = function(oldPos, newPos) {
      var diff1 = this.$screenRow(oldPos, this.sessionA);
      var diff2 = this.$screenRow(newPos, this.sessionB);
      if (diff1 < diff2) {
         this.$addWidget(this.sessionA, {
            rowCount: diff2 - diff1,
            rowsAbove: oldPos.row === 0 ? diff2 - diff1 : 0,
            row: oldPos.row === 0 ? 0 : oldPos.row - 1
         });
      } else if (diff1 > diff2) {
         this.$addWidget(this.sessionB, {
            rowCount: diff1 - diff2,
            rowsAbove: newPos.row === 0 ? diff1 - diff2 : 0,
            row: newPos.row === 0 ? 0 : newPos.row - 1
         });
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
         if (!isPadding(existing)) {
            // The row holds another widget (chunk output), and Ace shows
            // one per row. The part of the padding meant below the line
            // goes above the next line instead, right below that widget;
            // the part meant above the line goes below the line before,
            // the same place. Neither moves onto another widget's row, and
            // above the first line there is nowhere else to go.
            var above = w.rowsAbove || 0;
            var below = w.rowCount - above;
            if (below > 0 && padFreeRow(session, w.row + 1))
               this.$addWidget(session, { rowCount: below, rowsAbove: below, row: w.row + 1 });
            if (above > 0 && padFreeRow(session, w.row - 1))
               this.$addWidget(session, { rowCount: above, rowsAbove: 0, row: w.row - 1 });
            return;
         }

         // two paddings can land on one row (a change's start and end, as
         // an addition has an empty range on the base side; a widget's and
         // a change's); they add up, above the line and below it alike
         w.rowsAbove += existing.rowsAbove;
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

      // Without a base (none yet, none at all, or one that could not be
      // re-fetched) the base pane has nothing to scroll: Ace clamps its
      // offset to the top and its scroll sync would take the document
      // there too, so the sync is off until a base is back, when the base
      // joins the document again (see syncInitialScroll).
      this.hasBase = lines != null;
      if (!this.hasBase) {
         this.$syncScroll = false;
         this.scrollSynced = false;
      }

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

      if (this.hasBase)
         this.$syncScroll = true;
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
      this.ignoreWhitespace = !!ignoreWhitespace;

      // (an empty base has nothing to fold; the folds wait for one)
      if (this.foldsStale && this.hasBase) {
         this.foldsStale = false;
         this.mirrorFolds();
      }

      this.align();
      this.editorA.renderer.updateBackMarkers();
      this.editorB.renderer.updateBackMarkers();
      this.syncFont();
      this.syncInitialScroll();
   };

   this.resize = function() {
      this.baseEditor.resize();
      this.syncFont();
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
      this.disposed = true;
      this.gutter.setSplitView(null);
      this.detachLiveHandlers();
      this.$untrackFoldOperations.forEach(function(restore) {
         restore();
      });
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
