// Exercise alignment with the bundled Ace sessions, folds, wrapping and line
// widget manager. The view's editor UI is not needed for layout calculations.
var EditSession = require("ace/edit_session").EditSession;
var LineWidgets = require("ace/line_widgets").LineWidgets;
var Range = require("ace/range").Range;
var GitDiffSplitView = require("rstudio/git_diff_split").GitDiffSplitView;
var passed = 0;
var failed = 0;

function check(label, ok) {
   document.getElementById("log").textContent += (ok ? "PASS " : "FAIL ") + label + "\n";
   if (ok)
      passed++;
   else
      failed++;
}

function chunk(oldStart, oldEnd, newStart, newEnd) {
   return {
      old: new Range(oldStart, 0, oldEnd, 0),
      new: new Range(newStart, 0, newEnd, 0)
   };
}

function makeView(base, live, chunks) {
   var view = Object.create(GitDiffSplitView.prototype);
   view.sessionA = new EditSession(base);
   view.sessionB = new EditSession(live);
   view.editorA = { session: view.sessionA };
   view.editorB = { session: view.sessionB };
   view.chunks = chunks;
   view.gutter = {};
   view.hasBase = true;
   // (never rendered, so with no place in its view to keep; see closingView)
   view.liveEditor = {
      session: view.sessionB,
      renderer: { layerConfig: { lineHeight: 0 }, $size: { scrollerHeight: 0 } },
      getCursorPosition: function() { return { row: 0, column: 0 }; }
   };
   [view.sessionA, view.sessionB].forEach(function(session) {
      session.setUseWorker(false);
      new LineWidgets(session);
      session.lineWidgets = [];
      session.notifications = 0;
      session.resets = 0;
      session.on("changeFold", function() { session.notifications++; });
      var reset = session.$resetRowCache;
      session.$resetRowCache = function(row) {
         if (this === session)
            session.resets++;
         return reset.call(this, row);
      };
   });
   return view;
}

function unchanged(label, view) {
   var sessions = [view.sessionA, view.sessionB];
   var before = sessions.map(function(session) {
      session.documentToScreenPosition(session.getLength() - 1, 0);
      return {
         widgets: session.lineWidgets,
         entries: session.lineWidgets.slice(),
         docRows: session.$docRowCache,
         screenRows: session.$screenRowCache,
         notifications: session.notifications,
         resets: session.resets
      };
   });
   view.align();
   sessions.forEach(function(session, i) {
      var saved = before[i];
      check(label + ": pane " + i + " keeps widgets", session.lineWidgets === saved.widgets &&
         saved.entries.every(function(widget, row) { return widget === session.lineWidgets[row]; }));
      check(label + ": pane " + i + " keeps caches", session.$docRowCache === saved.docRows &&
         session.$screenRowCache === saved.screenRows && session.resets === saved.resets);
      check(label + ": pane " + i + " emits no fold event", session.notifications === saved.notifications);
   });
}

function level(label, view, baseRow, liveRow) {
   check(label, view.sessionA.documentToScreenRow(baseRow, 0) ===
      view.sessionB.documentToScreenRow(liveRow, 0));
}

function wrap(view, limit) {
   [view.sessionA, view.sessionB].forEach(function(session) {
      session.setUseWrapMode(true);
      session.adjustWrapLimit(limit);
   });
}

var view = makeView("a\nb\nc", "a\nx\ny\nc", [chunk(1, 2, 1, 3)]);
view.align();
level("initial padding aligns the following line", view, 2, 3);
view.sessionB.insert({ row: 1, column: 1 }, "z");
unchanged("typing within a modified line", view);
view.chunks = [];
view.align();
check("obsolete padding is removed", !view.sessionA.lineWidgets.some(Boolean));
unchanged("empty padding set", view);

view = makeView("a\nb", "inserted\na\nb", [chunk(0, 0, 0, 1)]);
view.align();
check("an addition at the start pads above row zero", view.sessionA.lineWidgets[0].rowsAbove === 1);
level("first unchanged line aligns", view, 0, 1);
unchanged("padding above row zero", view);

view = makeView("a\nb\nc", "a\n123456789\nc", [chunk(1, 2, 1, 2)]);
wrap(view, 10);
view.align();
view.sessionB.insert({ row: 1, column: 9 }, "xyz");
var liveNotifications = view.sessionB.notifications;
view.align();
check("wrapping inside the same chunk adds padding", view.sessionA.lineWidgets[1].rowCount === 1);
check("only the changed pane is signalled", view.sessionB.notifications === liveNotifications);
level("wrap growth aligns the next boundary", view, 2, 2);
unchanged("unchanged wrapped padding", view);
view.sessionB.remove(new Range(1, 9, 1, 12));
view.align();
check("unwrapping removes padding", !view.sessionA.lineWidgets.some(Boolean));
level("unwrapped boundary aligns", view, 2, 2);

view = makeView("a\nb\nc", "a\n          b\nc", []);
view.ignoreWhitespace = true;
wrap(view, 10);
view.align();
level("whitespace-only wrapping aligns without chunks", view, 2, 2);
unchanged("whitespace-only padding", view);

view = makeView("a\nb\nc", "a\nb\nc", []);
var output = { row: 1, rowCount: 2.5, rowsAbove: 0, fixedWidth: true };
view.sessionB.widgetManager.addLineWidget(output);
view.align();
level("fractional notebook output height aligns", view, 2, 2);
unchanged("notebook output padding", view);
output.rowCount = 4;
view.sessionB.$resetRowCache(1);
view.align();
check("resized output changes base padding", view.sessionA.lineWidgets[1].rowCount === 4);
check("notebook output keeps its identity and height", view.sessionB.lineWidgets[1] === output && output.rowCount === 4);
level("resized notebook output aligns", view, 2, 2);
view.sessionB.widgetManager.removeLineWidget(output);
view.align();
check("removing output removes its padding", !view.sessionA.lineWidgets.some(Boolean));

view = makeView("a\nb\nc", "a\nx\ny\nc", [chunk(1, 2, 1, 3)]);
view.align();
var oldPadding = view.sessionA.lineWidgets[1];
var overlay = { row: 1, rowCount: 2, rowsAbove: 0, fixedWidth: true };
view.sessionA.widgetManager.addLineWidget(overlay);
check("Ace chains a widget placed over padding", overlay.$oldWidget === oldPadding);
view.align();
check("planning removes padding under a foreign widget", !overlay.$oldWidget);
check("foreign overlay survives", view.sessionA.lineWidgets[1] === overlay);
unchanged("padding replaced by an overlay", view);

view = makeView("a\nb\nc\nd", "a\nb\nc\nd", []);
output = { row: 2, rowCount: 3, rowsAbove: 0, fixedWidth: true };
view.sessionB.widgetManager.addLineWidget(output);
view.sessionA.addFold("...", new Range(0, 1, 2, 1));
view.sessionB.addFold("...", new Range(0, 1, 2, 1));
view.align();
check("padding at a fold's end is aliased at its start", view.sessionA.lineWidgets[0] === view.sessionA.lineWidgets[2]);
level("folded output aligns", view, 3, 3);
unchanged("folded widget aliases", view);
view.sessionA.expandFold(view.sessionA.getAllFolds()[0]);
view.sessionB.expandFold(view.sessionB.getAllFolds()[0]);
view.align();
level("expanding folds preserves alignment", view, 3, 3);
unchanged("expanded output", view);

// A deletion at EOF has nowhere to move padding below the peek, so the
// action bar borrows those rows. Re-measuring it supplies its own height.
view = makeView("a\nb\nc", "a", [chunk(1, 3, 1, 1)]);
var peek = { row: 0, rowCount: 1, rowsAbove: 0, fixedWidth: true };
view.gutter.peek = { widget: peek };
view.sessionB.widgetManager.addLineWidget(peek);
view.align();
check("peek borrows rows at EOF", peek.gitDiffLentRows === 2 && peek.rowCount === 3);
unchanged("borrowed peek rows", view);
peek.rowCount = 2;
view.sessionB.$resetRowCache(0);
view.align();
check("remeasured peek gets its loan back once", peek.gitDiffOwnRowCount === 2 && peek.rowCount === 4);
unchanged("remeasured peek", view);
view.$removeLineWidgets(view.sessionA);
view.$removeLineWidgets(view.sessionB);
check("closing returns borrowed rows", peek.rowCount === 2 && peek.gitDiffLentRows === 0);

view = makeView("a\nb\nc", "a\nx\ny\nc", [chunk(1, 2, 1, 3)]);
view.align();
view.sessionB.insert({ row: 0, column: 0 }, "new\n");
view.chunks = [chunk(0, 0, 0, 1), chunk(1, 2, 2, 4)];
view.align();
level("an inserted line changes padding at the start", view, 0, 1);
level("a shifted chunk aligns below the edit", view, 2, 4);
unchanged("shifted chunk padding", view);

view = makeView("a\nb\nc", "a\nb\nc\nadded", [chunk(3, 3, 3, 4)]);
view.align();
var padding = view.sessionA.lineWidgets[2];
view.sessionA.remove(new Range(1, 1, 2, 1));
view.chunks = [chunk(2, 2, 2, 4)];
view.align();
check("a removed row does not preserve stale padding", view.sessionA.lineWidgets[1] !== padding &&
   view.sessionA.lineWidgets[1].rowCount === 2);
unchanged("padding after removing its row", view);

// Planning must not return a real peek's borrowed rows or change its cache,
// even if calculating the other pane fails before the plan can be applied.
view = makeView("a\nb\nc", "a", [chunk(1, 3, 1, 1)]);
peek = { row: 0, rowCount: 1, rowsAbove: 0, fixedWidth: true };
view.gutter.peek = { widget: peek };
view.sessionB.widgetManager.addLineWidget(peek);
view.align();
var widgetsBefore = view.sessionA.lineWidgets;
var cacheBefore = view.sessionA.$docRowCache;
view.$padChanges = function() { throw new Error("test planning failure"); };
var threw = false;
try {
   view.align();
} catch (error) {
   threw = true;
}
check("failed planning throws", threw);
check("failed planning leaves the applied layout intact", view.sessionA.lineWidgets === widgetsBefore &&
   view.sessionA.$docRowCache === cacheBefore && peek.rowCount === 3 && peek.gitDiffLentRows === 2);
check("failed planning clears the alignment guard", !view.$aligning);

// Closing the view keeps a line of the live editor where it was in the view
// (see $liveViewAnchor). A 100-row view of 10px lines over a document with
// lines 50 to 149 (rows 49 to 148) removed, padded for them above row 49.
function lines(from, to) {
   var result = [];
   for (var i = from; i <= to; i++)
      result.push("m" + i);
   return result.join("\n");
}

function closingView(base, live, chunks, cursorRow, height) {
   var view = makeView(base, live, chunks);
   view.liveEditor = {
      session: view.sessionB,
      renderer: { layerConfig: { lineHeight: 10 }, $size: { scrollerHeight: height } },
      getCursorPosition: function() { return { row: cursorRow, column: 0 }; }
   };
   view.align();
   return view;
}

function liveTop(view, row) {
   return view.sessionB.documentToScreenRow(row, 0) * 10;
}

function close(view) {
   var anchor = view.$liveViewAnchor();
   view.$removeLineWidgets(view.sessionA);
   view.$removeLineWidgets(view.sessionB);
   view.$restoreLiveView(anchor);
   return anchor;
}

var middle = [chunk(49, 149, 49, 49)];
var middleLive = lines(1, 49) + "\n" + lines(150, 200);

view = closingView(lines(1, 200), middleLive, middle, 49, 100);
view.sessionB.setScrollTop(liveTop(view, 49) - 50);
var anchor = close(view);
check("the cursor's line keeps its place", anchor.row === 49 && anchor.offset === 50 &&
   view.sessionB.getScrollTop() === 49 * 10 - 50);

view = closingView(lines(1, 200), middleLive, middle, 0, 100);
view.sessionB.setScrollTop(liveTop(view, 49) - 50);
anchor = close(view);
check("without the cursor, the first line shown keeps its place", anchor.row === 49 &&
   anchor.offset === 50 && view.sessionB.getScrollTop() === 49 * 10 - 50);

view = closingView(lines(1, 200), middleLive, middle, 0, 100);
view.sessionB.setScrollTop(liveTop(view, 49) - 500);
anchor = close(view);
check("a view of padding keeps the line below it at its bottom", anchor.row === 49 &&
   anchor.offset === 90 && view.sessionB.getScrollTop() === 49 * 10 - 90);

view = closingView(lines(1, 200), lines(1, 100), [chunk(100, 200, 100, 100)], 0, 100);
view.sessionB.setScrollTop(liveTop(view, 99) + 200);
anchor = close(view);
check("a view of padding at the end keeps the last line at its top",
   anchor.row === 99 && anchor.offset === 0 && view.sessionB.getScrollTop() === 99 * 10);

// Lines 152 to 171 (rows 151 to 170) removed, padded below row 150, the end
// of a fold from row 10. The scan meets the fold above the view and steps
// past it to the line after the padding.
view = closingView(lines(1, 200), lines(1, 151) + "\n" + lines(172, 200),
   [chunk(151, 171, 151, 151)], 0, 100);
[view.sessionA, view.sessionB].forEach(function(session) {
   session.addFold("...", new Range(10, 3, 150, 3));
});
view.align();
view.sessionB.setScrollTop(liveTop(view, 151) - 60);
anchor = close(view);
check("the scan steps past a fold to the line after it", anchor.row === 151 &&
   anchor.offset === 60 && view.sessionB.getScrollTop() === liveTop(view, 151) - 60);

// A view inside chunk output, which stays after the close, is not padding:
// the scroll is left where it was. 50 rows of output below row 49 fill
// screen rows 50 to 99, and below row 99 fill rows 100 to 149.
function outputView(row) {
   view = closingView(lines(1, 100), lines(1, 100), [], 0, 100);
   view.sessionB.widgetManager.addLineWidget({ row: row, rowCount: 50, rowsAbove: 0 });
   view.align();
   return view;
}

view = outputView(49);
view.sessionB.setScrollTop(600);
anchor = close(view);
check("a view inside output keeps its place", anchor.row === 49 &&
   anchor.offset === -110 && view.sessionB.getScrollTop() === 600);

view = outputView(99);
view.sessionB.setScrollTop(1200);
anchor = close(view);
check("a view inside output at the end keeps its place", anchor.row === 99 &&
   anchor.offset === -210 && view.sessionB.getScrollTop() === 1200);

// The action bar goes as the view closes, so a view inside the padding it
// borrows (lines 51 to 150 removed at the end) is a view of padding.
view = closingView(lines(1, 150), lines(1, 50), [chunk(50, 150, 50, 50)], 0, 100);
peek = { row: 49, rowCount: 1, rowsAbove: 0, fixedWidth: true };
view.gutter.peek = { widget: peek };
view.sessionB.widgetManager.addLineWidget(peek);
view.align();
view.sessionB.setScrollTop(liveTop(view, 49) + 300);
anchor = close(view);
check("a view inside the action bar's borrowed rows is padding", peek.gitDiffLentRows === 0 &&
   anchor.row === 49 && anchor.offset === 0 && view.sessionB.getScrollTop() === 49 * 10);

view = closingView("a\n" + new Array(101).join("x"), "a\n" + new Array(101).join("x"), [], 0, 100);
wrap(view, 10);
view.sessionB.setScrollTop(50);
anchor = close(view);
check("a wrapped line that starts above the view keeps its place", anchor.row === 1 &&
   anchor.offset === -40 && view.sessionB.getScrollTop() === 50);

view = closingView(lines(1, 200), middleLive, middle, 49, 100);
view.liveEditor.renderer.layerConfig.lineHeight = 0;
view.sessionB.setScrollTop(1234);
anchor = close(view);
check("a view never rendered is left alone", anchor === null &&
   view.sessionB.getScrollTop() === 1234);

// Realigning keeps a line of the live editor where it is in its view, and
// the base pane at the same offset, as padding comes and goes above it: here
// with the removal of lines 50 to 149 staged, then unstaged again.
function scrollBoth(view, top) {
   view.sessionA.setScrollTop(top);
   view.sessionB.setScrollTop(top);
}

function restage(view, base, chunks) {
   view.sessionA.setValue(base);
   view.baseChanged = true;
   view.chunks = chunks;
   view.align();
}

function scrolledTo(view, top) {
   return view.sessionA.getScrollTop() === top && view.sessionB.getScrollTop() === top;
}

view = closingView(lines(1, 200), middleLive, middle, 0, 100);
scrollBoth(view, liveTop(view, 80) - 30);
restage(view, middleLive, []);
check("a line keeps its place as padding above it goes", scrolledTo(view, 80 * 10 - 30));
restage(view, lines(1, 200), middle);
check("a line keeps its place as padding above it comes", scrolledTo(view, 180 * 10 - 30));

// The view opening: no layout yet, and the base pane still at its top.
view = closingView(lines(1, 200), middleLive, [], 80, 100);
view.alignedLive = null;
view.baseChanged = true;
view.sessionB.setScrollTop(80 * 10 - 30);
view.chunks = middle;
view.align();
check("a line keeps its place as the view opens", scrolledTo(view, 180 * 10 - 30));

// The cursor's line is the one kept, with the padding between it and the
// top of the view: lines 200 to 249 removed, the view from row 180.
var lowLive = lines(1, 199) + "\n" + lines(250, 300);
var low = [chunk(199, 249, 199, 199)];
view = closingView(lines(1, 300), lowLive, low, 199, 1000);
scrollBoth(view, 1800);
restage(view, lowLive, []);
check("the cursor's line keeps its place as padding in view goes", scrolledTo(view, 1800 - 50 * 10));

// Only the base is padded for an addition, and the scroll is left alone.
view = closingView("a\nb\nc", "a\nx\ny\nc", [], 0, 100);
view.sessionB.setScrollTop(5);
view.chunks = [chunk(1, 2, 1, 3)];
view.align();
check("padding in the base alone leaves the scroll alone", view.sessionA.lineWidgets[1].rowCount === 1 &&
   view.sessionA.getScrollTop() === 0 && view.sessionB.getScrollTop() === 5);

// An edit since the last layout moves the rows it was planned for: the
// scroll is left alone, and the next layout goes by this one.
view = closingView(lines(1, 200), middleLive, middle, 0, 100);
scrollBoth(view, liveTop(view, 80) - 30);
view.liveEdited = true;
restage(view, middleLive, []);
check("an edited document's scroll is left alone", scrolledTo(view, 180 * 10 - 30) && !view.liveEdited);
restage(view, lines(1, 200), middle);
check("the layout after an edit is the one gone by next", scrolledTo(view, 280 * 10 - 30));

// A view of padding keeps its place in it. Lines 10 to 19 are removed as
// well (padded above row 9), and that removal staged, with the view in the
// middle of the padding for lines 50 to 149 (above row 39).
var twoLive = lines(1, 9) + "\n" + lines(20, 49) + "\n" + lines(150, 200);
view = closingView(lines(1, 200), twoLive, [chunk(9, 19, 9, 9), chunk(49, 149, 39, 39)], 0, 100);
scrollBoth(view, liveTop(view, 39) - 500);
restage(view, lines(1, 9) + "\n" + lines(20, 200), [chunk(39, 139, 39, 39)]);
check("a view of padding keeps its place in it as padding above goes",
   liveTop(view, 39) === 1390 && scrolledTo(view, 1390 - 500));

// With the padding it showed staged away, the view moves up by its rows.
view = closingView(lines(1, 300), lowLive, low, 0, 100);
scrollBoth(view, 2200);
restage(view, lowLive, []);
check("a view of padding that goes moves up by its rows", scrolledTo(view, 2200 - 50 * 10));

// Padding that changes without a new base (a fold, a widget, the wrapping)
// leaves the scroll alone.
view = closingView(lines(1, 200), middleLive, middle, 0, 100);
scrollBoth(view, liveTop(view, 80) - 30);
view.chunks = [];
view.align();
check("padding that changes with the same base leaves the scroll alone",
   !view.sessionB.lineWidgets.some(Boolean) && scrolledTo(view, 180 * 10 - 30));

// The action bar takes the padding above the first line on, and leaves none
// behind as it closes (it does as the base changes) until the next layout
// puts it back: that is not padding gone and come again. Lines 1 to 100 are
// removed; in the new base, the last line differs as well.
var topLive = lines(101, 200);
view = closingView(lines(1, 200), topLive, [chunk(0, 100, 0, 0)], 0, 100);
view.baseChanged = true;
view.alignedLive = null;
view.$removeLineWidgets(view.sessionB);
view.align();
check("the first line keeps its place as padding comes above it", scrolledTo(view, 100 * 10));
scrollBoth(view, 0);
peek = { row: 0, rowCount: 1, rowsAbove: 0, fixedWidth: true };
view.gutter.peek = { widget: peek };
view.sessionB.widgetManager.addLineWidget(peek);
view.align();
check("the action bar takes the padding on", peek.gitDiffLentAbove === 100 && scrolledTo(view, 0));
view.gutter.peek = null;
view.sessionB.widgetManager.removeLineWidget(peek);
restage(view, lines(1, 199) + "\nother", [chunk(0, 100, 0, 0), chunk(199, 200, 99, 100)]);
check("the action bar closing as the base changes leaves the scroll alone",
   view.sessionB.lineWidgets[0].rowsAbove === 100 && scrolledTo(view, 0));

// The same with the removal staged whole, from lower down: the action bar
// closes and takes the padding with it, so the document has none left to
// lose, yet it goes from above the line in view all the same. (The bar's
// own row goes too, as any widget's would.)
view = closingView(lines(1, 200), topLive, [chunk(0, 100, 0, 0)], 0, 100);
peek = { row: 0, rowCount: 1, rowsAbove: 0, fixedWidth: true };
view.gutter.peek = { widget: peek };
view.sessionB.widgetManager.addLineWidget(peek);
view.align();
scrollBoth(view, liveTop(view, 80) - 30);
view.gutter.peek = null;
view.sessionB.widgetManager.removeLineWidget(peek);
restage(view, topLive, []);
check("padding the action bar had taken on counts as gone when its change is staged",
   !view.sessionB.lineWidgets.some(Boolean) && scrolledTo(view, (80 + 100 + 1) * 10 - 30 - 100 * 10));

// The line kept is one that was in view with that padding in place, not one
// that shows there once the bar has taken it away. Lines 201 to 250 are
// removed as well (padded above row 100), with rows 60 to 69 in view, above
// that padding: staging the second removal alone leaves them where they
// are, and staging both moves the view up by the rows of the first.
var bothLive = lines(101, 200) + "\n" + lines(251, 300);
var both = [chunk(0, 100, 0, 0), chunk(200, 250, 100, 100)];
[
   { label: "below the view", base: lines(1, 200) + "\n" + lines(251, 300), chunks: [chunk(0, 100, 0, 0)], rows: 0 },
   { label: "above and below it", base: bothLive, chunks: [], rows: 100 }
].forEach(function(staged) {
   view = closingView(lines(1, 300), bothLive, both, 0, 100);
   peek = { row: 0, rowCount: 1, rowsAbove: 0, fixedWidth: true };
   view.gutter.peek = { widget: peek };
   view.sessionB.widgetManager.addLineWidget(peek);
   view.align();
   var top = liveTop(view, 60);
   scrollBoth(view, top);
   view.gutter.peek = null;
   view.sessionB.widgetManager.removeLineWidget(peek);
   restage(view, staged.base, staged.chunks);
   check("with the action bar closing, padding staged away " + staged.label + " counts from the lines in view",
      top === (60 + 100 + 1) * 10 && scrolledTo(view, top - staged.rows * 10));
});

document.getElementById("passed").textContent = passed;
document.getElementById("failed").textContent = failed;
