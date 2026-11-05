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
try {
   view.align();
} catch (error) {
   check("failed planning leaves the applied layout intact", view.sessionA.lineWidgets === widgetsBefore &&
      view.sessionA.$docRowCache === cacheBefore && peek.rowCount === 3 && peek.gitDiffLentRows === 2);
}
check("failed planning clears the alignment guard", !view.$aligning);

document.getElementById("passed").textContent = passed;
document.getElementById("failed").textContent = failed;
