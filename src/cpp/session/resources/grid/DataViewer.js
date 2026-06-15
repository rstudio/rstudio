/*
 * gridviewer.js
 *
 * Copyright (C) 2025 by Posit Software, PBC
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
(function() {

// ==========================================================================
// Constants
// ==========================================================================

// Visual row height in pixels (kept in sync with the grid CSS). All
// virtual-scroll math and spacer-row sizing assumes a uniform row height.
var ROW_HEIGHT = 23;

// Number of off-screen rows rendered above and below the viewport so the
// recycler has a buffer to draw from before the next fetch lands. Kept modest:
// every buffered row now also pays for the rendered column window, and 200
// (the previous value) multiplied the rendered-cell count enough to be a
// meaningful share of the wide-frame render cost (#17806). Prefetch latency is
// covered separately by FETCH_SIZE, so this only needs to cover fast scroll.
var BUFFER_ROWS = 25;

// Default fetch chunk for row requests, sized to comfortably cover the
// visible window plus the buffer above.
var FETCH_SIZE = 500;

// Row prefetch is currently one FETCH_SIZE block ahead of the visible window
// (see renderVisibleRows). If that proves too coarse for fast scrolling, a
// pixel-distance-from-edge threshold could be reintroduced here to trigger the
// next fetch earlier.

// rowCache eviction. Holding ~20k rows in memory comfortably covers any
// reasonable visible-window-plus-prefetch; once we exceed the limit we
// trim back to the threshold, keeping the rows nearest the visible window.
var ROW_CACHE_LIMIT = 20000;
var ROW_CACHE_TARGET = 15000;

// Column width bounds, in pixels. MIN/MAX bound auto-sizing; DEFAULT is
// the fallback when no width can be inferred; MAX_COL_WIDTH_CHAR caps
// widths derived from the col_max_chars hint so a 1000-char column can't
// blow out the grid.
var MIN_COL_WIDTH = 50;
var MAX_COL_WIDTH = 300;
var MAX_COL_WIDTH_CHAR = 500;
var DEFAULT_COL_WIDTH = 120;

// Reference string for the canvas-based "average character width" probe.
// A mix of digits, upper- and lowercase letters, and common punctuation
// produces a stable per-font heuristic that the col_max_chars-to-pixel
// conversion uses.
var AVG_CHAR_REF_STRING =
   "0123456789 ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz .,-_";

// Persisted UI state version + localStorage key prefix. Bump
// STATE_VERSION when the stored state shape changes incompatibly so old
// payloads are dropped on next read instead of being applied with
// mismatched indices.
var STATE_VERSION = 3;
var STATE_KEY_PREFIX = "rstudio.dataViewer:";

// Per-load sentinel used in place of a real column fingerprint when the
// server response omits one. Comparing two sentinels from different page
// loads always produces a mismatch, so missing fingerprint data triggers
// state invalidation rather than silently passing the equality check.
var MISSING_FINGERPRINT_SENTINEL =
   "__missing__:" + Math.random().toString(36).slice(2) + ":" + Date.now();

// User-facing timings, all in milliseconds. Tweak together when tuning feel.
var TIMING = {
   debounceDefault: 200,        // debounce() wait when none given
   filterDebounce: 200,         // numeric/text filter input -> applyFilters
   searchDebounce: 100,         // global search input -> applyFilters
   infoBarDebounce: 150,        // "Showing X to Y" text update during scroll
   resizeDebounce: 75,          // window resize -> relayout
   sidebarTransition: 200,      // CSS transition duration for sidebar expand/collapse
   columnFlash: 1000,           // duration of the column highlight-flash on a go-to-column jump
   scrollbarHide: 1200          // delay before custom scrollbars fade out
};

// ==========================================================================
// State
// ==========================================================================

// Column definitions from the server
var cols = null;

// Total number of columns in the data frame
var totalCols = 0;

// Offset from which to start rendering columns
var columnOffset = 0;

// Maximum columns to display at once
var maxDisplayColumns = -1;

// Bounds of the currently FETCHED column window (absolute, 1-based,
// inclusive), captured when `cols` is established. Layout math (the
// unfetched-span widths, content-x mapping) keys off these rather than
// columnOffset so it stays self-consistent while a slide is in flight
// (columnOffset already points at the next window then).
// fetchedWindowEnd === 0 means "unknown" (no grid yet).
var fetchedWindowStart = 1;
var fetchedWindowEnd = 0;

// Non-zero while a column-window slide's metadata fetch is in flight
// (holds that slide's generation token). Gates scroll-driven slides so
// fast horizontal scrubbing issues at most one metadata fetch at a time.
var slideInFlightGen = 0;

// Maximum rows to display (-1 = all)
var maxRows = -1;

// Whether to display null values as NA
var displayNullsAsNAs = false;

// Whether ordering (sorting) is enabled
var ordering = true;

// Whether to show row numbers in the index column
var rowNumbers = true;

// Status text override (replaces "Showing x of y...")
var statusTextOverride = null;

// Current sort state. sortColumn is the absolute (1-based) column index in
// the full frame, or -1 when unsorted -- not a position within the currently
// fetched window, so it stays correct as the user pages through columns.
var sortColumn = -1;
var sortDirection = ""; // "", "asc", "desc"

// Cached search/filter values, keyed by absolute (1-based) column index so a
// filter tracks its column across pagination rather than its window position.
var cachedSearch = "";
var cachedFilterValues = {};

// Scroll position preservation
var lastScrollTop = 0;
var lastScrollLeft = 0;

// Scroll position captured just before a refresh tears the grid down, so it
// can be restored once the rebuilt grid has re-fetched its first row batch.
// Holds { top, left, rows } where rows is the pre-refresh unfiltered row count;
// the restore only fires when the new row count matches (see
// restoreScrollAfterRefresh).
//
// pendingScrollRestore is the handoff slot, set by captureScrollForRefresh()
// just before it calls bootstrap(). bootstrap() immediately moves it into
// activeScrollRestore (the value owned by the in-flight rebuild) and clears the
// handoff slot, so it is null except in that brief window. This split keeps a
// capture bound to a single rebuild: if a refresh aborts before the restore
// callback runs, the stranded value lives in activeScrollRestore, which the
// next bootstrap unconditionally overwrites -- so it can never leak onto an
// unrelated later rebuild (e.g. column pagination, which carries no scroll
// intent).
var pendingScrollRestore = null;
var activeScrollRestore = null;

// Row data cache. Each entry is an array aligned 1:1 with the current `cols`
// (rowname at position 0, then the fetched columns in fetch order).
var rowCache = new Map();
var totalRows = 0;
var filteredRows = 0;
var drawCounter = 0;

// Column signature of the current `cols` (comma-joined absolute indices) and,
// per FETCH_SIZE-aligned block, the signature its cached rows were fetched
// with. A block whose stored signature differs from colsSig is "incomplete":
// its rows were remapped from a previous column window (see remapRowCache)
// and may carry undefined cells, so it is refetched when it next becomes
// visible. Cleared with rowCache in invalidateCache.
var colsSig = "";
var blockColsSig = new Map();

// In-flight fetch requests
var pendingFetches = new Map(); // key: "start-length" -> AbortController

// Current render window
var renderStart = 0;
var renderEnd = 0;

// Incremental row recycling state
var renderedRowElements = new Map(); // rowIndex -> <tr> element
var topSpacerRow = null;
var bottomSpacerRow = null;

// Column resize state
var didResize = false;
var resizingColIdx = null;
var initResizeX = null;
var initResizingWidth = null;
var origTableWidth = null;
var resizingBoundsExceeded = 0;
var origColWidths = [];
// User-driven resize widths, keyed by absolute (col_index) column identity --
// like pins/sort/filters -- so a width follows its column across column
// pagination instead of landing on whatever column occupies the same position
// in the next fetched window.
var manualWidths = {};

// Filter popup state
var dismissActivePopup = null;

// Column type popup state
var columnsPopup = null;
var activeColumnInfo = {};
var onColumnOpen = null;
var onColumnDismiss = null;

// Bootstrapping flag, plus a generation token for the column fetch: each
// bootstrap bumps the generation, and the fetchColumns callback only runs
// initGrid when its captured generation is still current. Without this,
// overlapping bootstraps (e.g. two rapid data refreshes) could apply stale
// column metadata out of order, or run initGrid twice without an intervening
// destroyGrid (orphaning the previous custom-scrollbar DOM).
var bootstrapping = false;
var bootstrapGeneration = 0;

// Post-init deferred actions
var postInitActions = {};

// Sidebar state
var sidebarVisible = true;

// True once the first bootstrap has resolved sidebarVisible from the URL
// default / saved state. Later re-bootstraps (refresh after a data change,
// column pagination) must not re-apply the URL default: the live in-memory
// value reflects the user's most recent choice, even when a column-structure
// change invalidated the saved state that recorded it.
var sidebarDefaultResolved = false;

// Marker class for the injected per-column filter widgets. Shared by
// setFilterUIVisible (which injects them) and isFilterUIVisible (which reads
// the recorded postInitActions entry keyed by this marker).
var FILTER_UI_MARKER = "filter-injected-ui";

// Active cell coordinates. activeRow is the 0-based index into the
// currently displayed (filtered/sorted) rows; activeCol is the 0-based
// position in columnOrder (display order, accounting for pinning).
// Both -1 when no cell is selected. Reset by anything that changes row
// identity (sort, filter, search, column pagination).
var activeRow = -1;
var activeCol = -1;

// Active column header (display index in columnOrder), or -1 when no
// header is active. setActiveCell and setActiveHeader enforce mutual
// exclusion -- calling one clears the other -- but clearActiveCell on its
// own does not touch activeHeaderCol, so callers that change the column
// window (e.g. setMaxColumns) must also call clearActiveHeader. Reachable
// from the body via ArrowUp at row 0.
var activeHeaderCol = -1;

// Pinned columns: a set of absolute (1-based) column indices in the full
// frame, so a pin tracks its column across pagination rather than its position
// within the currently fetched window. The rownames column is always
// implicitly pinned and is not represented here.
var pinnedColumns = new Set();

// Saved per-object state loaded at the start of bootstrap (before the column
// fetch) so the request can include pinned columns that fall outside the
// visible window. Validated against the column fingerprint once `cols`
// arrives; see primeSelectionState / applySavedState.
var pendingSavedState = null;

// Number of pinned columns primed from saved state for the in-flight column
// request. If the fingerprint turns out not to match (object reassigned to a
// different frame), those columns were requested against the wrong frame, so
// initGrid re-bootstraps once after clearing the stale selection.
var primedPinnedCount = 0;

// Pinned-column layout cache. cachedPinnedOffsets[colIdx] -> px offset
// from the viewport's left edge; the more elaborate pinnedOffsetsCache
// also tracks total pinned width. pinnedOffsetsCache is dropped by
// invalidatePinnedOffsets() on width/pinning changes; cachedPinnedOffsets
// is refreshed in renderVisibleRows whenever the row window is (re)rendered,
// and reset in resetGridState.
var cachedPinnedOffsets = {};
var pinnedOffsetsCache = null;

// Current column display order (pinned first, then unpinned), recomputed
// by rebuildHeaders.
var columnOrder = [];

// Rendered unpinned column window (columnOrder positions, inclusive). Set by
// computeColumnWindow from the horizontal scroll position; pinned columns are
// always rendered and sit outside this range. See "Column virtualization".
var colWinStart = -1;
var colWinEnd = -1;

// Header-attached UIs (filter inputs, column-type editors) that must be
// re-applied whenever a header is (re)created as the column window slides.
// Keyed by markerClass -> initialize(th, col, colIdx). Populated by
// setHeaderUIVisible; consumed by reinjectHeaderUI.
var activeHeaderUIs = {};

// Per-column auto-sized widths in pixels, indexed by column position.
// Populated by autoSizeColumns; user-driven resizes go to manualWidths
// (in the Column resize state group above).
var measuredWidths = [];

// Cached cumulative column offsets (see columnOffsets). Derived purely from
// columnOrder + measuredWidths, so it shares their invalidation point
// (invalidatePinnedOffsets). Memoized because columnOffsets is called once per
// rendered row in appendWindowedCells -- recomputing an O(columns) prefix sum
// for every row of a wide frame was a measurable share of render cost (#17806).
var columnOffsetsCache = null;

// Cached widths of the unfetched column spans flanking the fetched window
// (see leftSpanWidth / rightSpanWidth). -1 = needs recompute; shares the
// invalidation point of the other layout caches (invalidatePinnedOffsets).
var leftSpanWidthCache = -1;
var rightSpanWidthCache = -1;

// Authoritative table content width; mirrors the sum of measuredWidths after
// autoSizeColumns. Prefer this over deriving content width from
// table.offsetWidth - paddingRight, which the browser may reconcile
// inconsistently when box-sizing: border-box, table-layout: fixed, and a
// dynamic paddingRight all interact.
var totalTableWidth = 0;

// Canvas-based text measurer. Lazily initialized so a non-DOM context
// (tests) doesn't pay the canvas allocation up front. measureCtxFont tracks
// the font currently assigned to the context: reassigning canvas .font reparses
// the shorthand and dominates the autoSizeColumns measure loop, so we set it
// only when it actually changes (the loop alternates between just two fonts).
var measureCanvas = null;
var measureCtx = null;
var measureCtxFont = "";

// The two fonts measureTextWidth uses: regular for data cells, bold for
// headers. Same size/family; only the weight differs.
var MEASURE_FONT =
   "11px 'DejaVu Sans', 'Lucida Grande', 'Segoe UI', Verdana, Helvetica, sans-serif";
var MEASURE_FONT_BOLD = "bold " + MEASURE_FONT;

// Cached "1 character width" derived from AVG_CHAR_REF_STRING. Invalid
// until a measureTextWidth call has populated it; reset to 0 to force
// a re-measure (e.g. after a font change).
var avgCharWidthCache = 0;

// Set when initial autoSize ran with a hidden viewport (offsetHeight === 0);
// the next onActivate re-measures.
var needsAutoSize = false;

// Set when autoSizeColumns was asked to run while a header editor (a filter
// popup or a focused inline text-filter input) was open. Rebuilding the header
// row destroys that editor mid-edit, so the rebuild is deferred and flushed
// once the editor closes (flushDeferredHeaderRebuild).
var deferredHeaderRebuild = false;

// Custom scrollbar handles (vertical/horizontal grid + sidebar). Null
// before createCustomScrollbars and again after destroyCustomScrollbars.
var gridScrollbarV_ = null;
var gridScrollbarH_ = null;
var sidebarScrollbar_ = null;

// Sparklines whose container has been created but whose (relatively
// expensive) canvas has not been drawn yet. Populated by initSidebar and
// flushed by renderPendingSparklines once the summary panel is actually
// visible -- so a hidden panel (e.g. data_viewer_show_summary off) never
// pays the rendering cost for dozens of numeric columns (#17806).
var pendingSparklines_ = [];

// requestAnimationFrame tokens used to coalesce scroll/scrollbar updates
// to once per frame. Non-zero means a frame is already scheduled.
var pendingScrollbarRaf = 0;
var pendingScrollRaf = 0;

// Latched so a localStorage write failure is reported once per session
// rather than spamming the console on every state-change debounce tick.
var persistWarned = false;

// ==========================================================================
// Utilities
// ==========================================================================

// debounce(wait?, watch?, func) -- the debounced function comes last so call
// sites can end with the closure body. wait defaults to
// TIMING.debounceDefault when omitted. watch (optional) is a getter sampled
// when a call is scheduled and again when the timer fires; the pending call
// is dropped if the watched value changed in between (see watchColumnSearch
// for the rationale).
var debounce = function(wait, watch, func) {
   if (func === undefined) {
      if (watch === undefined) {
         func = wait;
         wait = TIMING.debounceDefault;
      } else {
         func = watch;
      }
      watch = null;
   }

   var timeout = null;
   var pendingContext = null, pendingArgs = null, pendingToken;

   var fire = function() {
      timeout = null;
      var context = pendingContext, args = pendingArgs;
      pendingContext = null;
      pendingArgs = null;
      if (watch && watch() !== pendingToken)
         return;
      func.apply(context, args);
   };

   var debounced = function() {
      pendingContext = this;
      pendingArgs = arguments;
      pendingToken = watch ? watch() : undefined;
      clearTimeout(timeout);
      timeout = setTimeout(fire, wait);
   };
   debounced.cancel = function() {
      clearTimeout(timeout);
      timeout = null;
      pendingContext = null;
      pendingArgs = null;
   };
   // Run a pending call now instead of waiting out the timer (no-op when
   // nothing is pending). The staleness watch still applies, exactly as if
   // the timer had fired at this moment.
   debounced.flush = function() {
      if (timeout === null) return;
      clearTimeout(timeout);
      fire();
   };
   return debounced;
};

var escapeHtml = function(html) {
   if (html === null || html === undefined) return "";
   var s = (typeof html === "string") ? html : String(html);
   // Escape ' as well as the four canonical chars: no current call site
   // emits user data into a single-quoted attribute, but covering ' here
   // means a future copy-paste like attr='${escapeHtml(x)}' is safe by
   // construction rather than relying on every caller to remember.
   var replacements = {
      "<": "&lt;",
      ">": "&gt;",
      "&": "&amp;",
      '"': "&quot;",
      "'": "&#39;"
   };
   return s.replace(/[&<>"']/g, function(ch) { return replacements[ch]; });
};

var highlightSearchMatch = function(data, search, pos) {
   return escapeHtml(data.substring(0, pos)) +
      '<span class="searchMatch">' +
      escapeHtml(data.substring(pos, pos + search.length)) +
      '</span>' +
      escapeHtml(data.substring(pos + search.length, data.length));
};

var showError = function(msg) {
   document.getElementById("errorWrapper").style.display = "block";
   document.getElementById("errorMask").style.display = "block";
   document.getElementById("error").textContent = msg;
   var grid = document.getElementById("rsGridData");
   if (grid) grid.style.display = "none";
};

// Reverse of showError, run when a bootstrap succeeds: without this, a grid
// that recovers from a transient failure (e.g. session busy during a refresh)
// rebuilds underneath a stuck error mask and stays hidden until a full page
// reload.
var hideError = function() {
   document.getElementById("errorWrapper").style.display = "none";
   document.getElementById("errorMask").style.display = "none";
   var grid = document.getElementById("rsGridData");
   if (grid) grid.style.display = "";
};

var parseLocationUrl = function() {
   var result = {
      env: "", obj: "", cacheKey: "", id: "", dataSource: "",
      maxDisplayColumns: -1, maxCols: 0, maxRows: 0,
      // Default true so the sidebar shows when the param is absent (e.g.
      // older callers that don't pass it).
      showSummary: true
   };
   var query = window.location.search.substring(1);
   var vars = query.split("&");
   for (var i = 0; i < vars.length; i++) {
      var pair = vars[i].split("=");
      var key = pair[0], val = pair[1];
      if (key === "env") result.env = decodeURIComponent(val);
      else if (key === "obj") result.obj = decodeURIComponent(val);
      else if (key === "cache_key") result.cacheKey = decodeURIComponent(val);
      else if (key === "data_source") result.dataSource = decodeURIComponent(val);
      else if (key === "id") result.id = decodeURIComponent(val);
      else if (key === "max_display_columns") result.maxDisplayColumns = parseInt(val, 10);
      else if (key === "max_cols") result.maxCols = parseInt(val, 10);
      else if (key === "max_rows") result.maxRows = parseInt(val, 10);
      else if (key === "show_summary") result.showSummary = (val === "1" || val === "true");
   }
   return result;
};

var parseSearchString = function(val) {
   var pipe = val.indexOf("|");
   if (pipe > 0) return val.substr(pipe + 1);
   return val;
};

// ==========================================================================
// Data Layer
// ==========================================================================

var buildFormData = function(params) {
   var parts = [];
   for (var key in params) {
      if (params[key] !== undefined && params[key] !== null) {
         parts.push(encodeURIComponent(key) + "=" + encodeURIComponent(params[key]));
      }
   }
   return parts.join("&");
};

// Shared POST to ../grid_data, returns a Promise resolving to parsed JSON.
// On non-2xx, throws an Error whose message is the server-formatted error
// (when the body is a JSON {error: ...} payload), or a clean status-code
// message otherwise (so a proxy 502 HTML page or gateway timeout doesn't
// render raw markup into the error chrome). The HTTP status is also
// attached as `err.status` for callers that want to branch on it.
var gridDataFetch = function(body, signal) {
   var init = {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: body
   };
   if (signal) init.signal = signal;
   return fetch("../grid_data", init).then(function(response) {
      if (!response.ok) {
         return response.text().then(function(t) {
            var msg;
            try {
               var parsed = JSON.parse(t);
               if (parsed && parsed.error) msg = parsed.error;
            } catch (e) { /* not JSON */ }
            if (!msg) {
               msg = "Server returned " + response.status +
                     (response.statusText ? " " + response.statusText : "");
            }
            var err = new Error(msg);
            err.status = response.status;
            throw err;
         });
      }
      return response.json();
   });
};

// Whether any column filter or global search is active. Sort is excluded: it
// reorders rows but doesn't change the set, so it leaves summaries unchanged.
// This is the gate for showing filtered (vs. whole-frame) summaries.
var hasActiveRowFilter = function() {
   if (cachedSearch && cachedSearch.length > 0)
      return true;
   for (var k in cachedFilterValues) {
      if (cachedFilterValues.hasOwnProperty(k) && cachedFilterValues[k])
         return true;
   }
   return false;
};

// Add the active filter/search/sort to a request params object, in the
// DataTables wire form the backend parses. Shared by the row, summary, and
// detail fetches so they all resolve to the same filtered/sorted frame.
var appendTransformParams = function(params) {
   params["search[value]"] = cachedSearch;

   if (sortColumn >= 0 && sortDirection) {
      params["order[0][column]"] = sortColumn;
      params["order[0][dir]"] = sortDirection;
   }

   for (var absIdx in cachedFilterValues) {
      if (!cachedFilterValues.hasOwnProperty(absIdx))
         continue;
      var filterVal = cachedFilterValues[absIdx];
      if (filterVal)
         params["columns[" + absIdx + "][search][value]"] = filterVal;
   }
   return params;
};

var fetchColumnSummary = function(columnIndex, callback) {
   var loc = parseLocationUrl();
   var params = {
      env: loc.env,
      obj: loc.obj,
      cache_key: loc.cacheKey,
      show: "column_summary",
      column: columnIndex,
      max_rows: maxRows
   };
   // Detail stats describe the same rows the grid shows: send the active
   // filter/search/sort so the backend computes them over the filtered frame.
   appendTransformParams(params);

   gridDataFetch(buildFormData(params))
      .then(function(result) { if (callback) callback(result); })
      .catch(function(err) {
         if (err && err.name === "AbortError") {
            // Still invoke the callback (with null) so the caller can balance
            // any pending-fetch bookkeeping (e.g. the sidebar spinner refcount).
            if (callback) callback(null);
            return;
         }
         console.warn("fetchColumnSummary failed:", err);
         // Render an error state instead of staying on a "Loading..." spinner
         // forever. Pass through the server-formatted message when available.
         if (callback) callback({ error: (err && err.message) || "Failed to load summary." });
      });
};

// Per-column summary stats for the sidebar, computed over the filtered rows
// (show=cols&filtered=1). Keyed by absolute column index; null when no filter
// is active (the sidebar then renders from the full-frame `cols` metadata).
// Separate from `cols` on purpose: `cols` keeps describing the full frame so
// the filter popups' brush histogram and factor levels stay full-range.
var filteredSummaries = null;

// Filtered row count reported alongside filteredSummaries (the rownames entry's
// total_rows), used as the percentage denominator when rendering them. Avoids
// racing the row fetch that updates filteredRows.
var filteredSummariesRowCount = 0;

// Complete per-column identity (col_name/col_type/col_class/col_index) for
// EVERY column, backing the sidebar's full column list (show=column_index).
// null until loaded, when the sidebar falls back to the fetched grid window.
// Cleared on bootstrap (a refresh can rename/retype columns).
var sidebarColumns = null;
var sidebarColumnsFetching = false;

// Lazily-fetched summary descriptors (breaks/counts/range/NA/...) for columns
// OUTSIDE the fetched grid window, keyed by absolute index. Window columns get
// their summaries from `cols` (or filteredSummaries when filtered); this
// covers the rest, populated by the sidebar's IntersectionObserver as off-
// window entries scroll into view. Cleared when the filter state changes
// (refreshSidebarSummaries) and on bootstrap.
var sidebarLazySummaries = {};

// Full-frame column descriptors (abs index -> describe result) for the sidebar
// filter popups, fetched on demand when the icon is clicked. Kept separate from
// sidebarLazySummaries because those are computed over the FILTERED rows when a
// filter is active, whereas a filter brush must span the whole column's range
// (matching the header popups, which read full-frame `cols`). Cleared when the
// column set / fingerprint changes (alongside sidebarLazySummaries).
var filterDescriptors = {};

// Abs indices queued for the next (debounced) lazy summary fetch, populated as
// entries are built into the virtual window. Reset per sidebar rebuild.
var sidebarPendingFetch = {};

// Summary-sidebar virtualization. Like the grid rows, only the entries whose
// vertical band intersects the panel viewport are built; everything else is
// stood in for by two spacer divs sized from the constant entry height. This
// keeps initSidebar O(visible) rather than O(all columns). See initSidebar and
// renderSidebarWindow.
var SIDEBAR_BUFFER_ENTRIES = 5;  // extra entries kept built on each side
var sidebarListCols = [];        // listed column descriptors (rowname excluded)
var sidebarRenderTop = null;     // top spacer div
var sidebarRenderMid = null;     // container holding the currently built entries
var sidebarRenderBottom = null;  // bottom spacer div (includes overscroll tail)
var sidebarWinStart = -1;        // first built virtual index (inclusive)
var sidebarWinEnd = -1;          // last built virtual index (inclusive)
var sidebarIndexByAbs = {};      // abs column index -> virtual index in sidebarListCols
var sidebarScrollRaf = 0;        // rAF handle coalescing scroll-driven re-renders

var fetchFilteredSummaries = function(callback) {
   var loc = parseLocationUrl();
   var params = {
      env: loc.env,
      obj: loc.obj,
      cache_key: loc.cacheKey,
      show: "cols",
      filtered: 1,
      max_rows: maxRows
   };

   // Describe the same columns the sidebar lists (the fetched window).
   var requestedColumns = colsRequestList();
   if (requestedColumns.length > 0)
      params["columns_requested"] = requestedColumns.join(",");

   appendTransformParams(params);

   gridDataFetch(buildFormData(params))
      .then(function(result) {
         if (!result || result.error || !result.length) {
            callback(null, 0);
            return;
         }
         prepareColumnResponse(result);
         var map = {};
         var rowCount = 0;
         for (var i = 0; i < result.length; i++) {
            var entry = result[i];
            if (typeof entry.total_rows === "number" && entry.total_rows >= 0)
               rowCount = entry.total_rows;
            if (typeof entry.col_index === "number" && entry.col_index >= 1)
               map[entry.col_index] = entry;
         }
         callback(map, rowCount);
      })
      .catch(function(err) {
         console.warn("fetchFilteredSummaries failed:", err);
         callback(null, 0);
      });
};

// Rebuild the sidebar, preserving its scroll position across the teardown.
var rebuildSidebarPreservingScroll = function() {
   var sidebarContent = document.getElementById("sidebarContent");
   var scrollTop = sidebarContent ? sidebarContent.scrollTop : 0;
   initSidebar();
   sidebarContent = document.getElementById("sidebarContent");
   if (sidebarContent) {
      sidebarContent.scrollTop = scrollTop;
      // initSidebar built the window for scrollTop 0; rebuild it for the
      // restored position so the visible entries match where we scrolled back to.
      renderSidebarWindow(true);
   }
};

// Rebuild the sidebar against the current filter state: fetch filtered
// summaries first when a filter/search is active, otherwise clear them and
// render the full-frame metadata. Debounced calls during rapid filter typing
// coalesce via the drawCounter staleness check below.
var refreshSidebarSummaries = function() {
   // Lazily-fetched off-window summaries reflect the previous filter state;
   // drop them so they're refetched for the new one.
   sidebarLazySummaries = {};

   if (!hasActiveRowFilter()) {
      filteredSummaries = null;
      filteredSummariesRowCount = 0;
      rebuildSidebarPreservingScroll();
      return;
   }

   var startToken = drawCounter;
   fetchFilteredSummaries(function(map, rowCount) {
      // Drop a response superseded by a newer filter/search/refresh.
      if (startToken !== drawCounter)
         return;
      filteredSummaries = map;
      filteredSummariesRowCount = rowCount;
      rebuildSidebarPreservingScroll();
   });
};

// Column names for the WHOLE frame (the fetched window only covers a slice),
// backing the go-to-column popup. Fetched lazily on first use and cached;
// invalidated on bootstrap (a data refresh can rename columns).
var columnNamesCache = null;

var fetchColumnNames = function(callback) {
   if (columnNamesCache) {
      callback(columnNamesCache);
      return;
   }

   var params = "show=colnames&" + window.location.search.substring(1);
   gridDataFetch(params)
      .then(function(result) {
         if (result && !result.error && result.names) {
            columnNamesCache = result.names;
            callback(columnNamesCache);
         } else {
            callback(null);
         }
      })
      .catch(function(err) {
         console.warn("fetchColumnNames failed:", err);
         callback(null);
      });
};

var fetchColumns = function(callback) {
   var params = "show=cols&" + window.location.search.substring(1);

   // Request exactly the columns we display (pinned first, then the window),
   // by absolute index. An empty list means the whole frame.
   var requestedColumns = buildRequestedColumns();
   if (requestedColumns.length > 0) {
      params += "&columns_requested=" + requestedColumns.join(",");
   }

   // Errors are passed through to the callback rather than handled here so
   // the caller can unwind its bootstrap state: initGrid surfaces
   // result.error via showError and clears the bootstrapping flag, which
   // would otherwise be stranded true and permanently disable column
   // pagination after a transient failure (e.g. session busy).
   gridDataFetch(params)
      .then(function(result) {
         callback(result);
      })
      .catch(function(err) {
         callback({ error: (err && err.message) || "The object could not be displayed." });
      });
};

var fetchRows = function(start, length, callback) {
   var key = start + "-" + length;
   if (pendingFetches.has(key)) return;

   var startToken = drawCounter;
   var loc = parseLocationUrl();
   var params = {
      env: loc.env,
      obj: loc.obj,
      cache_key: loc.cacheKey,
      show: "data",
      start: start,
      length: length,
      draw: startToken,
      max_rows: maxRows
   };

   // Request exactly the columns of the current `cols` so the returned rows
   // are always aligned 1:1 with it (an empty list means the whole frame).
   var requestedColumns = colsRequestList();
   var requestSig = requestedColumns.join(",");
   if (requestedColumns.length > 0) {
      params["columns_requested"] = requestSig;
   }

   // Filter/search/sort parameters (all keyed by absolute column index).
   appendTransformParams(params);

   var controller = new AbortController();
   pendingFetches.set(key, controller);

   gridDataFetch(buildFormData(params), controller.signal)
      .then(function(result) {
         // Only clear our own bookkeeping entry: when invalidateCache aborts
         // this request and a new fetch re-registers the same key before this
         // (async) handler runs, deleting unconditionally would remove the
         // new request's controller, breaking its abort and dedup.
         if (pendingFetches.get(key) === controller)
            pendingFetches.delete(key);
         // Discard responses for requests that started before the most recent
         // invalidate (sort, filter, search, column-frame change).
         // AbortController covers most cases, but races are still possible.
         if (startToken !== drawCounter) return;
         if (result.error) { showError(result.error); return; }
         totalRows = result.recordsTotal;
         filteredRows = result.recordsFiltered;
         for (var i = 0; i < result.data.length; i++) {
            rowCache.set(start + i, result.data[i]);
         }
         // Record the column signature these rows carry. `start` is always
         // FETCH_SIZE-aligned (every caller fetches whole blocks), so this
         // marks exactly the block the rows landed in.
         blockColsSig.set(start, requestSig);
         trimRowCache();
         if (callback) callback();
         renderVisibleRows(true);
         updateInfoBar();
      })
      .catch(function(err) {
         if (pendingFetches.get(key) === controller)
            pendingFetches.delete(key);
         if (err.name === "AbortError") return;
         // Surface every other failure (network errors, 500s, CORS
         // rejections, JSON-shaped {error: ...} payloads). Silent failures
         // here leave the table frozen on "Loading" with no signal to the
         // user. gridDataFetch normalizes err.message to a clean string.
         showError(err.message || "Failed to load data.");
      });
};

var invalidateCache = function() {
   rowCache.clear();
   blockColsSig.clear();
   // Abort all pending fetches
   pendingFetches.forEach(function(controller) { controller.abort(); });
   pendingFetches.clear();
   totalRows = 0;
   filteredRows = 0;
   // Bump the staleness token so any in-flight responses that slip past
   // AbortController are discarded by fetchRows' response handler.
   drawCounter++;
};

// Whether a block's cached rows are present AND complete for the current
// column set. A block remapped from a previous column window keeps its old
// signature (its rows have undefined cells for newly entered columns), so it
// reads as needing a refetch while still rendering its overlap data.
var blockIsCurrent = function(blockStart) {
   return rowCache.has(blockStart) && blockColsSig.get(blockStart) === colsSig;
};

// Cap rowCache size so a long scroll through a multi-million-row dataset
// can't accumulate the entire row set in memory. Trims by distance from the
// current visible window: blocks near the user's view are kept, blocks far
// away are evicted. Cheap O(n log n) when triggered, and the soft hysteresis
// (LIMIT vs TARGET) keeps the trigger rate low during steady scroll.
//
// Eviction MUST happen at whole-FETCH_SIZE-block granularity:
// renderVisibleRows decides whether a block needs refetching by probing only
// its first row (rowCache.has(blockStart)), so evicting part of a block would
// leave rows that are gone from the cache but never refetched -- rendering as
// a permanently blank band when the user scrolls back into them.
var trimRowCache = function() {
   if (rowCache.size <= ROW_CACHE_LIMIT) return;

   // Group cached row keys by the fetch block that loaded them.
   var blocks = new Map();
   rowCache.forEach(function(value, key) {
      var blockStart = Math.floor(key / FETCH_SIZE) * FETCH_SIZE;
      var rows = blocks.get(blockStart);
      if (!rows) {
         rows = [];
         blocks.set(blockStart, rows);
      }
      rows.push(key);
   });

   // Keep the blocks nearest the visible window until the row budget is
   // spent; evict the remaining blocks in their entirety.
   var center = (renderStart + renderEnd) / 2;
   var blockStarts = Array.from(blocks.keys());
   blockStarts.sort(function(a, b) {
      return Math.abs(a + FETCH_SIZE / 2 - center) -
             Math.abs(b + FETCH_SIZE / 2 - center);
   });

   var kept = 0;
   for (var i = 0; i < blockStarts.length; i++) {
      var rows = blocks.get(blockStarts[i]);
      if (kept + rows.length <= ROW_CACHE_TARGET) {
         kept += rows.length;
      } else {
         for (var j = 0; j < rows.length; j++) {
            rowCache.delete(rows[j]);
         }
         blockColsSig.delete(blockStarts[i]);
      }
   }
};

// Remap every cached row from the old `cols` alignment to the new one,
// keyed by absolute column identity (col_index). Cells for columns present
// in both windows (rownames, pinned columns, any overlap) carry over; cells
// for newly entered columns become undefined and render blank until the
// block's refetch lands (blocks keep their old signature, so blockIsCurrent
// reports them as needing that refetch). This is what lets a column-window
// slide show row skeletons immediately instead of a blank grid.
var remapRowCache = function(oldCols, newCols) {
   if (rowCache.size === 0 || !oldCols)
      return;

   var oldPosByAbs = {};
   for (var i = 0; i < oldCols.length; i++) {
      oldPosByAbs[oldCols[i].col_index] = i;
   }

   var srcFor = new Array(newCols.length);
   for (var j = 0; j < newCols.length; j++) {
      var src = oldPosByAbs[newCols[j].col_index];
      srcFor[j] = (typeof src === "number") ? src : -1;
   }

   rowCache.forEach(function(row, key) {
      var newRow = new Array(newCols.length);
      for (var k = 0; k < newCols.length; k++) {
         if (srcFor[k] >= 0)
            newRow[k] = row[srcFor[k]];
      }
      rowCache.set(key, newRow);
   });
};

// ==========================================================================
// Cell Rendering
// ==========================================================================

// Render cell contents directly into the given td. Uses textContent for the
// common plain-text case (fastest, no HTML parsing) and innerHTML only when a
// cell needs markup (NA pill, search highlight, data/list link).
var renderCellContents = function(td, data, colIdx, rowData, clazz) {
   // NA handling: 0 means NA, or null when displayNullsAsNAs is set
   if (data === 0 || (displayNullsAsNAs && data === null)) {
      td.innerHTML = '<span class="naCell">NA</span>';
      return;
   }

   // Coerce to string up front so the rest of the function can use String
   // methods (substring, toLowerCase, indexOf) without crashing on null,
   // undefined, or numeric cell values (e.g. the preview-mode row index).
   if (typeof data !== "string") {
      data = (data === null || data === undefined) ? "" : String(data);
   }

   // Row name column: parse JSON to unwrap quoted character row names; numeric
   // automatic row names parse to a number whose toString matches the wire form.
   if (rowNumbers && colIdx === 0) {
      try { data = String(JSON.parse(data)); } catch(e) { /* leave as-is */ }
   }

   if (clazz === "dataCell") {
      if (data.substring(0, 5) === "list(" && data.indexOf("=") > 0) {
         var varCount = data.split("=").length - 1;
         var varLabel = varCount > 1 ? "variables" : "variable";
         data = varCount + " " + varLabel;
      }
   }

   // Search highlighting (produces HTML)
   var didHighlight = false;
   if (clazz !== "dataCell" && cachedSearch.length > 0) {
      var idx = data.toLowerCase().indexOf(cachedSearch.toLowerCase());
      if (idx >= 0) {
         data = highlightSearchMatch(data, cachedSearch, idx);
         didHighlight = true;
      }
   }

   // Column-specific search highlighting (skip if global search already highlighted)
   if (!didHighlight) {
      var colSearch = getColumnSearch(absColIndex(colIdx));
      if (colSearch && colSearch.indexOf("character|") === 0) {
         var term = decodeURIComponent(parseSearchString(colSearch));
         var colIdx2 = data.toLowerCase().indexOf(term.toLowerCase());
         if (colIdx2 >= 0) {
            data = highlightSearchMatch(data, term, colIdx2);
            didHighlight = true;
         }
      }
   }

   // Data/list cell links: build the link with a real click handler so
   // user-controlled row names never get concatenated into a javascript: URL.
   if (clazz === "dataCell" || clazz === "listCell") {
      var escaped = didHighlight ? data : escapeHtml(data);
      td.innerHTML = "<i>" + escaped + "</i> ";

      var cbName = clazz === "dataCell" ? "dataViewerCallback" : "listViewerCallback";
      var cbCol = absColIndex(colIdx);
      // rowData[0] arrives JSON-encoded for character row names and as a plain
      // numeric string (or a number, in preview mode) for automatic ones;
      // JSON.parse handles both.
      var cbRow;
      try { cbRow = JSON.parse(rowData[0]); } catch(e) { cbRow = rowData[0]; }

      var linkEl = document.createElement("a");
      linkEl.className = "viewerLink";
      linkEl.href = "#";
      var openLabel = clazz === "dataCell"
         ? "Open data viewer for this cell"
         : "Open object viewer for this cell";
      linkEl.setAttribute("aria-label", openLabel);
      linkEl.addEventListener("click", function(evt) {
         evt.preventDefault();
         var fn = window[cbName];
         if (typeof fn === "function") fn(cbRow, cbCol);
      });
      var imgEl = document.createElement("img");
      imgEl.className = "viewerImage";
      imgEl.src = clazz === "dataCell" ? "data-viewer.png" : "object-viewer.png";
      // Image is decorative -- the surrounding link carries the accessible name.
      imgEl.setAttribute("alt", "");
      linkEl.appendChild(imgEl);
      td.appendChild(linkEl);
      return;
   }

   if (didHighlight) {
      td.innerHTML = data;
   } else {
      td.textContent = data;
   }
};

var createCell = function(data, colIdx, rowData, clazz) {
   var td = document.createElement("td");

   var classes = [clazz];
   if (isColumnPinned(colIdx)) {
      classes.push("pinned");
      td.style.left = (cachedPinnedOffsets[colIdx] || 0) + "px";
   }
   if (colIdx === 0 && rowNumbers) classes.push("first-child");
   // Long cells get a max-width + ellipsis treatment via .largeCell so they
   // don't wrap and break row height.
   if (typeof data === "string" && data.length >= 10) classes.push("largeCell");
   td.className = classes.join(" ");

   renderCellContents(td, data, colIdx, rowData, clazz);

   // Tooltip: for the row-names column, unwrap the JSON-encoded form so the
   // tooltip shows `foo` rather than `"foo"`. Other columns pass through;
   // non-strings (numbers, NA sentinels) intentionally get no tooltip.
   if (rowNumbers && colIdx === 0 && typeof data === "string") {
      try { td.title = String(JSON.parse(data)); } catch(e) { td.title = data; }
   } else if (typeof data === "string") {
      td.title = data;
   }

   return td;
};

// ==========================================================================
// Header Construction
// ==========================================================================

// Column-type predicates. col_type is the column's R typeof() ("double",
// "integer", "logical", "character", "list", ...; "rownames" is a synthetic
// sentinel for the row-name column) and col_class is its class() reported as
// an array. Note typeof() of a data.frame column is "list", so a single
// col_type === "list" test covers both list and data.frame columns.
var colHasClass = function(col, name) {
   return col.col_class && col.col_class.indexOf(name) >= 0;
};

var isRownameColumn = function(col) {
   return col.col_type === "rownames";
};

var isDataFrameColumn = function(col) {
   return colHasClass(col, "data.frame");
};

var isListColumn = function(col) {
   return col.col_type === "list" && !isDataFrameColumn(col);
};

var isFactorColumn = function(col) {
   return colHasClass(col, "factor");
};

// Base numeric columns (right-aligned, histogram-summarized). Factors have
// typeof "integer" but class "factor", so keying on class excludes them;
// Date / POSIXct are likewise excluded, matching prior behavior.
var isNumericColumn = function(col) {
   return colHasClass(col, "numeric") ||
          colHasClass(col, "integer") ||
          colHasClass(col, "double") ||
          colHasClass(col, "integer64");
};

// Date / datetime columns. They get a brushable histogram + range filter like
// numerics, but their col_breaks are epoch values (days for Date, seconds for
// POSIXct) paired with formatted col_break_labels for display, and they carry
// an optional col_tz. Kept distinct from isNumericColumn so dates aren't
// right-aligned or fed to the numeric stats/footer paths. Only Date and
// POSIXct are matched -- the backend emits the date metadata for exactly those
// (a data.frame coerces POSIXlt to POSIXct), so matching POSIXlt here too would
// claim columns the server never marked up.
var isDateColumn = function(col) {
   return colHasClass(col, "Date") ||
          colHasClass(col, "POSIXct");
};

// Whether the backend computed histogram data for this column. col_breaks /
// col_counts are populated only for base-numeric columns; other data columns
// carry empty arrays and the rownames column omits the fields entirely. Both
// the empty-array and undefined cases are handled by the explicit checks (an
// empty array is truthy in JS, so length must be tested too).
var hasHistogram = function(col) {
   return col.col_breaks && col.col_breaks.length > 0 &&
          col.col_counts && col.col_counts.length > 0;
};

// Whether a (factor / character) column ships per-category counts for the
// sidebar's frequency bars. The server only emits these below its bar
// cutoff, so presence of the fields is the entire decision.
var hasCategoryCounts = function(col) {
   if (!col.col_cat_vals || col.col_cat_vals.length === 0)
      return false;
   if (!col.col_cat_counts ||
       col.col_cat_counts.length !== col.col_cat_vals.length) {
      // The fields only ship as a pair, so a mismatch means a server-side
      // bug; leave a trace rather than silently dropping the sparkline.
      console.warn("category values/counts mismatch for column '" +
         col.col_name + "': " + col.col_cat_vals.length + " values, " +
         (col.col_cat_counts ? col.col_cat_counts.length : 0) + " counts");
      return false;
   }
   return true;
};

// Coarse category used to choose which summary stats to render in the sidebar.
// Anything not matched here (Date, POSIXct, ...) falls through to the generic
// min/max stats in renderColumnStats.
var statsCategory = function(col) {
   if (isFactorColumn(col)) return "factor";
   if (isNumericColumn(col)) return "numeric";
   if (col.col_type === "character") return "character";
   if (col.col_type === "logical") return "boolean";
   return "other";
};

var getColClass = function(col) {
   if (isNumericColumn(col)) return "numberCell";
   if (isDataFrameColumn(col)) return "dataCell";
   if (isListColumn(col)) return "listCell";
   return "textCell";
};

var createHeader = function(idx, col) {
   var th = document.createElement("th");
   th.className = "sorting";
   th.id = "rsGridHeader_" + idx;
   th.setAttribute("data-col-idx", idx);
   th.setAttribute("scope", "col");

   if (idx === 0 && rowNumbers) {
      th.classList.add("pinned");
   }

   var interior = document.createElement("div");
   interior.className = "headerCell";

   // Pin icon to the left of the label (not shown for rownames -- always pinned)
   if (!(idx === 0 && rowNumbers)) {
      // The pin icon is a click affordance only -- it isn't a tabstop and
      // is not announced as a separate button. Keyboard users pin/unpin
      // by activating the column header (P key in header mode), which
      // keeps the grid a single tabstop per the WAI-ARIA grid pattern.
      var pinIcon = document.createElement("span");
      pinIcon.className = "pin-icon";
      var pinned = pinnedColumns.has(absColIndex(idx));
      if (pinned) pinIcon.classList.add("pinned");
      pinIcon.setAttribute("aria-hidden", "true");
      pinIcon.title = pinned ? "Unpin column" : "Pin column";
      pinIcon.addEventListener("click", function(evt) {
         evt.stopPropagation();
         evt.preventDefault();
         togglePinColumn(absColIndex(idx));
         // Focus the viewport and mark the column the user just pinned
         // as the active header so subsequent keyboard nav routes to
         // the grid and starts from this column. displayIdx can be -1
         // when the column lives past maxDisplayColumns and the unpin
         // pushed it out of the visible window -- in that case we just
         // focus the grid without setting an active header.
         var displayIdx = columnOrder.indexOf(idx);
         if (displayIdx >= 0) setActiveHeader(displayIdx);
         focusGridViewport();
      });
      interior.appendChild(pinIcon);
   }

   var title = document.createElement("span");
   title.textContent = col.col_name;
   interior.appendChild(title);

   // Tooltip
   if (isRownameColumn(col)) {
      th.title = "row names";
   } else {
      th.title = "column " + absColIndex(idx) + ": " + col.col_type;
      if (col.col_tz)
         th.title += "\ntimezone: " + col.col_tz;
      if (isDateColumn(col) && col.col_min_label && col.col_max_label) {
         // col_breaks are raw epoch values for dates; show the formatted range.
         th.title += "\nrange: " + col.col_min_label + " to " + col.col_max_label;
      } else if (hasHistogram(col)) {
         th.title += " with range " + col.col_breaks[0] +
            " - " + col.col_breaks[col.col_breaks.length - 1];
      } else if (isFactorColumn(col) && col.col_vals) {
         th.title += " with " + col.col_vals.length + " levels";
      }
   }

   // Column label
   if (col.col_label && col.col_label.length > 0) {
      var label = document.createElement("div");
      label.className = "colLabel";
      label.textContent = col.col_label;
      label.title = col.col_label;
      interior.appendChild(label);
   }

   th.appendChild(interior);

   // Resize handle -- appended to <th> directly (not inside headerCell)
   // so it isn't clipped by overflow:hidden on the interior div
   var resizer = document.createElement("div");
   resizer.className = "resizer";
   resizer.setAttribute("data-col", idx);
   th.appendChild(resizer);

   // Header click handler. Headers are not individually focusable -- the
   // grid is a single tabstop. Keyboard sort is handled by the grid-level
   // keydown handler when the header is the active descendant
   // (Enter/Space in header mode). Every header (including rownames and
   // when ordering is disabled) routes the click into the grid so
   // subsequent keystrokes operate on the column the user just clicked;
   // sort cycling additionally requires ordering && !rownames.
   var sortable = ordering && isColumnSortable(col);
   if (sortable) {
      th.style.cursor = "pointer";
      th.setAttribute("role", "columnheader");
      th.setAttribute("aria-sort",
         absColIndex(idx) === sortColumn ? sortAriaValue(sortDirection) : "none");
   }
   th.addEventListener("click", function(evt) {
      if (evt.target.className === "resizer") return;
      if (evt.target.classList && evt.target.classList.contains("pin-icon")) return;
      if (didResize) { didResize = false; return; }
      if (sortable) handleSortClick(absColIndex(idx));
      var displayIdx = columnOrder.indexOf(idx);
      if (displayIdx >= 0) setActiveHeader(displayIdx);
      focusGridViewport();
   });

   return th;
};

// Whether a column can be sorted. Rownames are an identity column with no
// natural ordering, and list / data.frame columns are non-atomic -- R's
// order() and xtfrm() error out on them ("unimplemented type 'list'"), which
// would otherwise put the whole grid into a "Failed to fetch" error state.
// Both list and data.frame columns report col_type === "list" (their typeof).
var isColumnSortable = function(col) {
   return col && !isRownameColumn(col) && col.col_type !== "list";
};

var sortAriaValue = function(dir) {
   if (dir === "asc") return "ascending";
   if (dir === "desc") return "descending";
   return "none";
};

// absIdx is the absolute (1-based) column index, which is how sortColumn is
// stored -- so this works for any column, including one not in the fetched
// window (e.g. sorted from its sidebar entry). Callers holding a window
// position translate via absColIndex first.
var handleSortClick = function(absIdx) {
   // Cycle: unsorted -> asc -> desc -> unsorted
   var newDir = "";
   if (sortColumn !== absIdx) {
      newDir = "asc";
   } else if (sortDirection === "asc") {
      newDir = "desc";
   } else if (sortDirection === "desc") {
      newDir = "";
   } else {
      newDir = "asc";
   }

   // Update sort state
   sortColumn = newDir ? absIdx : -1;
   sortDirection = newDir;

   // Sort changes row identity at every index; clear the active cell so
   // we don't re-highlight a different row at the old coordinate.
   clearActiveCell();

   // Refresh the arrow classes and aria-sort on every rendered header, and
   // mirror the new state into the sidebar's sort icons. AT users hear
   // "ascending" / "descending" / "none" in place of the visual arrow cue.
   applySortIndicators();
   updateSidebarColumnIndicators();

   // Re-fetch data
   invalidateCache();
   fetchRows(0, FETCH_SIZE, function() {
      scrollToTop();
   });
   saveState();
};

// Clear the active sort, restoring the frame's natural row order. Wired to
// the info bar's clear-sort button at startup.
var clearSort = function() {
   if (sortColumn < 0 && !sortDirection) return;

   sortColumn = -1;
   sortDirection = "";

   // Sort changes row identity at every index; clear the active cell so
   // we don't re-highlight a different row at the old coordinate.
   clearActiveCell();

   // Reset the header arrows + aria-sort and the sidebar's sort icons.
   applySortIndicators();
   updateSidebarColumnIndicators();

   // Re-fetch data
   invalidateCache();
   fetchRows(0, FETCH_SIZE, function() {
      scrollToTop();
   });
   saveState();
};

// ==========================================================================
// Column Pinning
// ==========================================================================

// Translate a position in the currently fetched `cols` array to the absolute
// (1-based) column index in the full frame. Falls back to the position itself
// when col_index is unavailable (older server or the rownames column, which is
// index 0). This is the single bridge between the window-relative positions
// used throughout the render path and the absolute identities used for
// pinning, sorting, and filtering.
var absColIndex = function(pos) {
   if (cols && cols[pos] && typeof cols[pos].col_index === "number")
      return cols[pos].col_index;
   return pos;
};

// Reverse of absColIndex: find the position in `cols` of the column with the
// given absolute index, or -1 if it isn't in the current window/fetch.
var posForAbsColIndex = function(absIdx) {
   if (!cols)
      return -1;
   for (var i = 0; i < cols.length; i++) {
      if (cols[i] && cols[i].col_index === absIdx)
         return i;
   }
   return -1;
};

// Build the ordered list of absolute (1-based) column indices to request from
// the server: pinned columns first (ascending), then the visible window,
// prepending only pinned columns that fall OUTSIDE that window (so their data
// is available while the window is scrolled away). Pins that are already inside
// the window keep their natural position -- the pinned-first display order is a
// render-time concern handled by getColumnOrder, not a fetch-order one, so the
// position of an in-window column stays stable across a re-fetch. Returns an
// empty array to mean "the whole frame" -- used when no column windowing is
// configured, in which case every column is present.
var buildRequestedColumns = function() {
   var maxCols = effectiveMaxDisplayColumns();
   if (maxCols <= 0)
      return [];

   // Window range is 1-based absolute: columnOffset is a 0-based data-column
   // offset, so the first window column is columnOffset + 1. The server clamps
   // indices past the end of the frame, so an unknown totalCols is harmless.
   var windowStart = columnOffset + 1;
   var windowEnd = columnOffset + maxCols;
   if (totalCols > 0)
      windowEnd = Math.min(windowEnd, totalCols);

   var requested = [];
   var seen = {};

   // Pinned columns that lie outside the window, in ascending order, come
   // first so their data accompanies the window we're about to display.
   Array.from(pinnedColumns)
      .filter(function(a) {
         return typeof a === "number" && a >= 1 &&
            (a < windowStart || a > windowEnd);
      })
      .sort(function(a, b) { return a - b; })
      .forEach(function(a) {
         if (!seen[a]) { seen[a] = true; requested.push(a); }
      });

   for (var c = windowStart; c <= windowEnd; c++) {
      if (!seen[c]) { seen[c] = true; requested.push(c); }
   }
   return requested;
};

// The ordered list of absolute column indices to request for ROW data. Once
// `cols` exists this derives from it -- not from buildRequestedColumns -- so
// returned rows are always aligned 1:1 with `cols`, even when the desired
// window has drifted from the fetched one (e.g. unpinning an out-of-window
// column changes buildRequestedColumns before the metadata is re-fetched).
// An empty list means "the whole frame" (no column windowing configured).
var colsRequestList = function() {
   if (effectiveMaxDisplayColumns() <= 0)
      return [];
   if (cols && cols.length > 1) {
      var list = [];
      for (var i = 1; i < cols.length; i++) {
         list.push(typeof cols[i].col_index === "number" ? cols[i].col_index : i);
      }
      return list;
   }
   return buildRequestedColumns();
};

// The number of display columns in the current window, resolved from the
// module state if known or from the URL parameters otherwise (the module
// value isn't set until initGrid, but the column fetch runs before that).
var effectiveMaxDisplayColumns = function() {
   if (maxDisplayColumns > 0)
      return maxDisplayColumns;
   var loc = parseLocationUrl();
   if (loc.maxDisplayColumns > 0)
      return loc.maxDisplayColumns;
   if (loc.maxCols > 0)
      return loc.maxCols;
   return -1;
};

// Returns the column render order: pinned columns first (in original order),
// then unpinned columns (in original order). Column 0 (rownames) is always first.
// The server returns exactly the columns we asked for (pinned + window), so we
// order the whole fetched set rather than capping at maxDisplayColumns.
var getColumnOrder = function() {
   var colCount = cols ? cols.length : 0;
   var pinned = [];
   var unpinned = [];
   for (var i = 0; i < colCount; i++) {
      if (isColumnPinned(i)) {
         pinned.push(i);
      } else {
         unpinned.push(i);
      }
   }
   return pinned.concat(unpinned);
};

var isColumnPinned = function(colIdx) {
   return (colIdx === 0 && rowNumbers) || pinnedColumns.has(absColIndex(colIdx));
};

// ----------------------------------------------------------------------------
// Column virtualization (client-side only -- the server still sends the full
// page of columns; this just limits how many cells reach the DOM).
//
// Rendering ~200 columns x ~100 visible rows materializes ~20k cells, and the
// compositor commit for that many cells stalls for seconds (#17806). Mirror
// the row virtualization horizontally: render the pinned columns (always) plus
// only the unpinned columns whose x-range intersects the viewport, with a
// left/right spacer cell standing in for the off-window columns so the table's
// total width -- and thus the custom horizontal scrollbar -- is unchanged.
// ----------------------------------------------------------------------------

// Number of extra off-screen columns to keep rendered on each side, so a small
// horizontal scroll doesn't require an immediate re-render.
var BUFFER_COLS = 3;

// First unpinned position in columnOrder (pinned columns are all at the front).
var firstUnpinnedPos = function() {
   for (var i = 0; i < columnOrder.length; i++) {
      if (!isColumnPinned(columnOrder[i])) return i;
   }
   return columnOrder.length;
};

// Cumulative left offset (in table content px) of each columnOrder position,
// from measuredWidths. Returns an array of length columnOrder.length + 1 where
// entry i is the sum of widths of positions [0, i); the last entry is the
// total content width. Falls back to an empty result until widths exist.
//
// Memoized in columnOffsetsCache; invalidated via invalidatePinnedOffsets when
// columnOrder or measuredWidths change. Callers treat the result as read-only.
var columnOffsets = function() {
   if (columnOffsetsCache !== null) return columnOffsetsCache;

   var offs = [0];
   for (var i = 0; i < columnOrder.length; i++) {
      offs.push(offs[i] + (measuredWidths[i] || 0));
   }
   columnOffsetsCache = offs;
   return offs;
};

// ----------------------------------------------------------------------------
// Unfetched column spans. The table's layout covers the WHOLE frame: pinned
// columns, then a left span standing in for the unfetched columns before the
// fetched window, the fetched window itself, and a right span for the
// unfetched columns after it. Span columns are billed at DEFAULT_COL_WIDTH (no
// metadata exists for them yet); the discrepancy against their real widths is
// absorbed when a slide lands (anchor-based scroll compensation in
// applyColumnWindowUpdate). This is what gives the horizontal scrollbar the
// full frame's range, so the user scrolls -- rather than paginates -- through
// columns.
// ----------------------------------------------------------------------------

// Number of pinned columns whose absolute index falls in [lo, hi].
var countPinnedInRange = function(lo, hi) {
   if (lo > hi) return 0;
   var n = 0;
   pinnedColumns.forEach(function(abs) {
      if (abs >= lo && abs <= hi) n++;
   });
   return n;
};

// Unpinned column counts in the spans. Pinned columns are excluded: they are
// always fetched and rendered sticky at the front, so they occupy no span
// space. fetchedWindowEnd === 0 (no grid yet) yields empty spans.
var leftSpanCols = function() {
   var hi = fetchedWindowStart - 1;
   if (fetchedWindowEnd <= 0 || hi < 1) return 0;
   return hi - countPinnedInRange(1, hi);
};

var rightSpanCols = function() {
   if (fetchedWindowEnd <= 0 || fetchedWindowEnd >= totalCols) return 0;
   var lo = fetchedWindowEnd + 1;
   return (totalCols - lo + 1) - countPinnedInRange(lo, totalCols);
};

var leftSpanWidth = function() {
   if (leftSpanWidthCache < 0)
      leftSpanWidthCache = leftSpanCols() * DEFAULT_COL_WIDTH;
   return leftSpanWidthCache;
};

var rightSpanWidth = function() {
   if (rightSpanWidthCache < 0)
      rightSpanWidthCache = rightSpanCols() * DEFAULT_COL_WIDTH;
   return rightSpanWidthCache;
};

// The k-th (0-based) unpinned absolute column index at or after startAbs.
// Walks the (small, sorted) pinned set to skip over pinned indices.
var nthUnpinnedAbs = function(startAbs, k) {
   var sorted = Array.from(pinnedColumns).sort(function(a, b) { return a - b; });
   var abs = startAbs + k;
   for (var i = 0; i < sorted.length; i++) {
      if (sorted[i] >= startAbs && sorted[i] <= abs) abs++;
   }
   return Math.max(1, Math.min(abs, totalCols));
};

// Map a content x-coordinate (px from the table's left edge) to the absolute
// index of the unpinned column at that position, across all three layout
// segments (left span / fetched window / right span).
var absColAtContentX = function(x) {
   var offs = columnOffsets();
   var firstUnpinned = firstUnpinnedPos();
   var lastPos = columnOrder.length - 1;
   if (lastPos < 0 || offs.length === 0) return 1;

   var spanBase = offs[firstUnpinned]; // pinned block width
   var leftW = leftSpanWidth();

   if (x < spanBase + leftW) {
      var k = Math.max(0, Math.floor((x - spanBase) / DEFAULT_COL_WIDTH));
      return nthUnpinnedAbs(1, k);
   }

   var fx = x - leftW;
   for (var i = firstUnpinned; i <= lastPos; i++) {
      if (fx < offs[i + 1]) return absColIndex(columnOrder[i]);
   }

   var rx = x - (offs[lastPos + 1] + leftW);
   var k2 = Math.max(0, Math.floor(rx / DEFAULT_COL_WIDTH));
   return nthUnpinnedAbs(fetchedWindowEnd + 1, k2);
};

// Inverse of absColAtContentX: the layout x of a column's left edge, by
// absolute index. Fetched columns resolve through the measured prefix sums;
// span columns through the DEFAULT_COL_WIDTH estimate.
var layoutXOfAbs = function(absIdx) {
   var offs = columnOffsets();
   var firstUnpinned = firstUnpinnedPos();
   var lastPos = columnOrder.length - 1;
   if (lastPos < 0 || offs.length === 0) return 0;

   var leftW = leftSpanWidth();

   var pos = posForAbsColIndex(absIdx);
   if (pos >= 0) {
      var orderPos = columnOrder.indexOf(pos);
      if (orderPos >= 0) {
         return orderPos < firstUnpinned
            ? offs[orderPos]               // pinned: sticky at the front
            : offs[orderPos] + leftW;      // fetched window
      }
   }

   if (absIdx < fetchedWindowStart) {
      var k = (absIdx - 1) - countPinnedInRange(1, absIdx - 1);
      return offs[firstUnpinned] + Math.max(0, k) * DEFAULT_COL_WIDTH;
   }

   var lo = fetchedWindowEnd + 1;
   var k2 = (absIdx - lo) - countPinnedInRange(lo, absIdx - 1);
   return offs[lastPos + 1] + leftW + Math.max(0, k2) * DEFAULT_COL_WIDTH;
};

// Compute the visible unpinned column window for a given horizontal scroll
// position. Returns columnOrder positions { start, end } (inclusive) covering
// the unpinned columns whose x-range intersects the viewport plus BUFFER_COLS
// on each side. Pinned columns are handled separately (always rendered), so
// start is clamped to the first unpinned position. When widths aren't measured
// yet, returns the full unpinned range so the grid still renders.
var getColumnWindow = function(scrollLeft, clientWidth) {
   var firstUnpinned = firstUnpinnedPos();
   var lastPos = columnOrder.length - 1;
   if (lastPos < firstUnpinned || measuredWidths.length === 0) {
      return { start: firstUnpinned, end: lastPos };
   }

   var offs = columnOffsets();
   var visLeft = scrollLeft;
   var visRight = scrollLeft + clientWidth;

   // Fetched columns sit to the right of the left unfetched span; shift
   // their layout positions accordingly.
   var shift = leftSpanWidth();

   var start = -1, end = -1;
   for (var i = firstUnpinned; i <= lastPos; i++) {
      var colLeft = offs[i] + shift;
      var colRight = offs[i + 1] + shift;
      if (colRight > visLeft && colLeft < visRight) {
         if (start === -1) start = i;
         end = i;
      }
   }

   // Nothing intersected: the viewport is over an unfetched span (or past the
   // end during a transient layout). Keep the nearest edge column rendered so
   // we never produce an empty body.
   if (start === -1) {
      var nearest = (visRight <= offs[firstUnpinned] + shift)
         ? firstUnpinned : lastPos;
      start = nearest;
      end = nearest;
   }

   start = Math.max(firstUnpinned, start - BUFFER_COLS);
   end = Math.min(lastPos, end + BUFFER_COLS);
   return { start: start, end: end };
};

// Recompute the rendered column window from the current horizontal scroll
// position. Returns true if the window changed (so callers can rebuild).
var computeColumnWindow = function() {
   var viewport = document.getElementById("gridViewport");
   var sl = viewport ? viewport.scrollLeft : 0;
   var cw = viewport ? viewport.clientWidth : 0;
   var win = getColumnWindow(sl, cw);
   var changed = (win.start !== colWinStart || win.end !== colWinEnd);
   colWinStart = win.start;
   colWinEnd = win.end;
   return changed;
};

// A filler cell standing in for a run of off-window columns. Carries no
// borders/padding and an explicit width so table-layout: fixed reserves the
// off-window columns' horizontal space (keeping the table's total width, and
// thus the custom horizontal scrollbar, unchanged).
var colSpacerCell = function(tag, widthPx) {
   var c = document.createElement(tag);
   c.className = "col-spacer";
   c.setAttribute("aria-hidden", "true");
   c.style.width = widthPx + "px";
   c.style.minWidth = widthPx + "px";
   c.style.padding = "0";
   c.style.border = "none";
   return c;
};

// Append the windowed cell sequence to `parent`: all pinned cells, a left
// spacer, the visible column window, and a right spacer. `makeCell(pos)`
// builds the cell for columnOrder position `pos`. Shared by the header and
// body so their column structure stays identical (required for table-layout:
// fixed to keep columns aligned).
var appendWindowedCells = function(parent, makeCell, spacerTag) {
   var firstUnpinned = firstUnpinnedPos();
   var lastPos = columnOrder.length - 1;

   // Pinned columns are always rendered (they're sticky).
   for (var p = 0; p < firstUnpinned; p++) {
      parent.appendChild(makeCell(p));
   }
   if (firstUnpinned > lastPos) return;

   // Clamp the window to the unpinned range; fall back to the full range if it
   // hasn't been computed yet so we never render an empty body.
   var winStart = colWinStart, winEnd = colWinEnd;
   if (winEnd < winStart || winStart < firstUnpinned || winEnd > lastPos) {
      winStart = firstUnpinned;
      winEnd = lastPos;
   }

   // The spacers absorb both the off-window fetched columns AND the unfetched
   // spans flanking the fetched window, so the table's total width spans the
   // whole frame and the horizontal scrollbar covers every column.
   var offs = columnOffsets();
   var leftW = (offs[winStart] - offs[firstUnpinned]) + leftSpanWidth();
   if (leftW > 0) parent.appendChild(colSpacerCell(spacerTag, leftW));

   for (var p = winStart; p <= winEnd; p++) {
      parent.appendChild(makeCell(p));
   }

   var rightW = (offs[lastPos + 1] - offs[winEnd + 1]) + rightSpanWidth();
   if (rightW > 0) parent.appendChild(colSpacerCell(spacerTag, rightW));
};

// Number of cells appendWindowedCells emits per row (pinned + optional left
// spacer + window + optional right spacer). The vertical spacer row's td must
// use exactly this as its colSpan: an oversized colspan would force the table
// to that many columns and break table-layout: fixed width distribution.
var renderedColumnCount = function() {
   var firstUnpinned = firstUnpinnedPos();
   var lastPos = columnOrder.length - 1;
   var count = firstUnpinned; // pinned cells (always rendered)
   if (firstUnpinned > lastPos) return count || 1;

   var winStart = colWinStart, winEnd = colWinEnd;
   if (winEnd < winStart || winStart < firstUnpinned || winEnd > lastPos) {
      winStart = firstUnpinned;
      winEnd = lastPos;
   }
   var offs = columnOffsets();
   if (offs[winStart] - offs[firstUnpinned] + leftSpanWidth() > 0)
      count++;                                                  // left spacer
   count += (winEnd - winStart + 1);                            // window cells
   if (offs[lastPos + 1] - offs[winEnd + 1] + rightSpanWidth() > 0)
      count++;                                                  // right spacer
   return count || 1;
};

// Re-apply any active header-attached UIs (filter inputs, column-type editors)
// to a freshly created header. Needed because the column window destroys and
// recreates headers as it slides, and that UI lives inside the <th>.
var reinjectHeaderUI = function(th, colIdx, col) {
   for (var marker in activeHeaderUIs) {
      if (!activeHeaderUIs.hasOwnProperty(marker)) continue;
      var existing = th.querySelectorAll("." + marker);
      for (var k = 0; k < existing.length; k++) {
         th.removeChild(existing[k]);
      }
      var el = activeHeaderUIs[marker](th, col, colIdx);
      if (el) {
         el.classList.add(marker);
         th.appendChild(el);
      }
   }
};

// Rebuild the header row for the current column window: pinned headers, a left
// spacer, the windowed headers (with widths + any active header UI), and a
// right spacer. Cheap to call on every horizontal-window change.
var rebuildHeaderWindow = function() {
   var thead = document.getElementById("data_cols");
   if (!thead || !cols || !columnOrder.length) return;

   thead.innerHTML = "";
   appendWindowedCells(
      thead,
      function(pos) {
         var colIdx = columnOrder[pos];
         var th = createHeader(colIdx, cols[colIdx]);
         if (typeof measuredWidths[pos] === "number") {
            th.style.width = measuredWidths[pos] + "px";
         }
         reinjectHeaderUI(th, colIdx, cols[colIdx]);
         return th;
      },
      "th"
   );
   applySortIndicators();

   // Headers are recreated as the window slides, so re-apply the active-header
   // highlight if its column landed in the new window (the active cell is
   // re-applied by buildRow).
   if (activeHeaderCol >= 0 && activeHeaderCol < columnOrder.length) {
      var activeTh = getHeaderCell(columnOrder[activeHeaderCol]);
      if (activeTh) activeTh.classList.add("activeHeader");
   }
};

// Recompute the rendered column window from the current scroll position; if it
// changed, rebuild the windowed header and re-apply pinned styling. Returns
// whether the window changed, so callers can decide how to re-render rows (a
// changed window forces a full row rebuild -- existing rows carry the old
// window's cells). Shared by the scroll handlers, the keyboard / go-to scroll-
// into-view paths, and the column-window slide.
var syncColumnWindow = function() {
   if (!computeColumnWindow())
      return false;
   rebuildHeaderWindow();
   applyPinnedColumns();
   return true;
};

// absIdx is the absolute (1-based) column index; pinnedColumns tracks absolute
// identities, so this works for any column, including one not in the fetched
// window (e.g. pinned from its sidebar entry). Callers holding a window
// position translate via absColIndex first.
var togglePinColumn = function(absIdx) {
   if (pinnedColumns.has(absIdx)) {
      pinnedColumns.delete(absIdx);
   } else {
      pinnedColumns.add(absIdx);
   }
   // Pinning reorders columns, so the active cell's display index would
   // now refer to a different column; clear it. The active header, if
   // any, follows the pinned column to its new display index so that
   // keyboard users keep their place after pressing P.
   clearActiveCell();
   var headerOrigCol = -1;
   if (activeHeaderCol >= 0) {
      headerOrigCol = columnOrder[activeHeaderCol];
      clearActiveHeader();
   }
   invalidatePinnedOffsets();
   rebuildHeaders();
   renderVisibleRows(true);
   // Pinning changes the horizontal pinned-overscroll padding (see
   // applyPinnedColumns), which shifts the scrollable content width;
   // refresh the custom scrollbars so the thumb size matches before the
   // user scrolls.
   updateCustomScrollbars();
   updateSidebarColumnIndicators();
   saveState();
   if (headerOrigCol >= 0) {
      var newDisplayIdx = columnOrder.indexOf(headerOrigCol);
      if (newDisplayIdx >= 0) setActiveHeader(newDisplayIdx);
   }

   // Unpinning a column that lies outside the current window removes it from
   // the desired fetch set, leaving `cols` (and the rows aligned to it) with
   // a column the window no longer wants. Re-sync the fetched window in
   // place; in-window pin toggles don't change the requested set, so this is
   // a no-op for them.
   if (buildRequestedColumns().join(",") !== colsRequestList().join(","))
      slideColumnWindow();
};

// Rebuild headers in the current column order (pinned first, then unpinned).
// Reorder existing <th> elements rather than recreating them so any attached
// filter UI / popup state is preserved across pin toggles.
var rebuildHeaders = function() {
   var thead = document.getElementById("data_cols");
   if (!thead || !cols) return;

   // Recompute the pinned/unpinned order, then let autoSizeColumns rebuild the
   // windowed header row. Header-attached UI (filters, column types) is
   // restored by reinjectHeaderUI from activeHeaderUIs, so it survives the
   // rebuild without the previous reuse-existing-<th> dance.
   columnOrder = getColumnOrder();
   autoSizeColumns();
   applyPinnedColumns();
};

// Compute the cumulative left offset for each pinned column based on render order.
// Returns { offsets: { colIdx: leftPx, ... }, totalWidth: number }.
//
// Result is cached in pinnedOffsetsCache (declared with the rest of the
// state). Each entry costs an offsetWidth read which forces layout, and
// this function is called from renderVisibleRows on every scroll.
// Callers that change column widths or pinning must invalidate via
// invalidatePinnedOffsets(). This also drops columnOffsetsCache, which is
// derived from the same columnOrder + measuredWidths state.
var invalidatePinnedOffsets = function() {
   pinnedOffsetsCache = null;
   columnOffsetsCache = null;
   leftSpanWidthCache = -1;
   rightSpanWidthCache = -1;
};

var getPinnedOffsets = function() {
   if (pinnedOffsetsCache !== null) return pinnedOffsetsCache;

   var offsets = {};
   var cumulative = 0;

   // Derive pinned offsets from measuredWidths rather than reading rendered
   // <th> offsetWidths: with column virtualization the pinned headers are
   // always present, but keying off measuredWidths avoids a per-call layout
   // flush and stays correct regardless of what else is in the DOM.
   for (var i = 0; i < columnOrder.length; i++) {
      var colIdx = columnOrder[i];
      if (!isColumnPinned(colIdx)) break; // pinned columns are all at the front
      offsets[colIdx] = cumulative;
      cumulative += measuredWidths[i] || 0;
   }
   pinnedOffsetsCache = { offsets: offsets, totalWidth: cumulative };
   return pinnedOffsetsCache;
};

// Apply pinned styling to header cells
var applyPinnedColumns = function() {
   var thead = document.getElementById("data_cols");
   if (!thead) return;

   // No-op if column widths haven't been measured yet (e.g. autoSizeColumns
   // bailed because the viewport was hidden). Without a real totalTableWidth
   // the overscroll calculation below would silently produce paddingRight=0;
   // wait for the next autoSizeColumns + applyPinnedColumns pair instead.
   if (totalTableWidth === 0) return;

   var pinned = getPinnedOffsets();

   for (var i = 0; i < thead.children.length; i++) {
      var th = thead.children[i];
      var colIdx = parseInt(th.getAttribute("data-col-idx"), 10);
      // Skip spacer cells (col-spacer): they carry no data-col-idx and are
      // never pinned. With column virtualization thead.children no longer maps
      // 1:1 to columnOrder, so read the column index off the header itself.
      if (isNaN(colIdx)) continue;
      var pinIcon = th.querySelector(".pin-icon");
      var nowPinned = isColumnPinned(colIdx);

      if (nowPinned) {
         th.classList.add("pinned");
         th.style.left = (pinned.offsets[colIdx] || 0) + "px";
      } else {
         th.classList.remove("pinned");
         th.style.left = "";
      }
      if (pinIcon) {
         if (nowPinned) pinIcon.classList.add("pinned");
         else pinIcon.classList.remove("pinned");
         pinIcon.title = nowPinned ? "Unpin column" : "Pin column";
      }
   }

   // Horizontal overscroll: lets the user scroll the rightmost column to sit
   // just past the pinned columns for side-by-side context. Applied even when
   // all columns fit in the viewport so horizontal scrolling is consistently
   // available. We reserve room for the rightmost column so it stays visible
   // at maximum scroll; without this reservation the user can scroll every
   // unpinned column off-screen (issue #17612). If every column is pinned
   // (or none exist) there is nothing to scroll past, so we skip the padding
   // to avoid a phantom scrollbar over empty space.
   var viewport = document.getElementById("gridViewport");
   var table = document.getElementById("rsGridData");
   if (viewport && table) {
      var overscroll = 0;
      var lastIdx = columnOrder.length - 1;
      // columnOrder places pinned columns before unpinned, so the last entry
      // being unpinned is sufficient to confirm an unpinned column exists.
      if (lastIdx >= 0 && !isColumnPinned(columnOrder[lastIdx])) {
         // The last LAYOUT column may be an unfetched span column (billed at
         // the default estimate) or a fetched column outside the rendered
         // window; use the appropriate width rather than a rendered <th>.
         var lastUnpinnedWidth = rightSpanWidth() > 0
            ? DEFAULT_COL_WIDTH
            : (measuredWidths[lastIdx] || 0);
         overscroll = Math.max(0,
            viewport.clientWidth - pinned.totalWidth - lastUnpinnedWidth);
      }
      table.style.paddingRight = overscroll + "px";
   }
};

// ==========================================================================
// Column Auto-Sizing
// ==========================================================================

// Measure the natural text width of a string using a 2d canvas. Avoids the
// layout flush that an offsetWidth read on a DOM element would force, which
// matters when measuring 100+ rows x N columns at startup.
var measureTextWidth = function(text, bold) {
   if (!measureCtx) {
      measureCanvas = document.createElement("canvas");
      measureCtx = measureCanvas.getContext("2d");
      measureCtxFont = "";
   }
   var font = bold ? MEASURE_FONT_BOLD : MEASURE_FONT;
   if (font !== measureCtxFont) {
      measureCtx.font = font;
      measureCtxFont = font;
   }
   return Math.ceil(measureCtx.measureText(text).width);
};

// Average character width, derived from AVG_CHAR_REF_STRING measured at
// the same font/size as the data cells. Used to translate the server-
// provided col_max_chars hint into a pixel width without measuring every
// cell.
var avgCharWidth = function() {
   if (!avgCharWidthCache) {
      avgCharWidthCache =
         measureTextWidth(AVG_CHAR_REF_STRING, false) / AVG_CHAR_REF_STRING.length;
   }
   return avgCharWidthCache;
};

// Width of the widest digit at the cell font. Numeric and ISO date/time cells
// render only digits and separators ('-', ':', '.', ' '), and every separator
// is narrower than a digit, so col_max_chars * this is a safe upper bound on
// such a column's rendered width -- letting autoSizeColumns size those columns
// from the (whole-column-accurate) col_max_chars hint without sampling cells.
// Cached; the font never changes.
var digitWidthCache = 0;
var digitWidth = function() {
   if (!digitWidthCache) {
      var w = 0;
      for (var d = 0; d <= 9; d++) {
         var dw = measureTextWidth(String(d), false);
         if (dw > w) w = dw;
      }
      digitWidthCache = w;
   }
   return digitWidthCache;
};

// Width of the widest logical literal at the cell font. Logical cells render
// only TRUE / FALSE / NA, so this sizes a logical column exactly without
// sampling. Cached; the font never changes.
var logicalCellWidthCache = 0;
var logicalCellWidth = function() {
   if (!logicalCellWidthCache) {
      logicalCellWidthCache = Math.max(
         measureTextWidth("TRUE", false),
         measureTextWidth("FALSE", false),
         measureTextWidth("NA", false));
   }
   return logicalCellWidthCache;
};

// Compute column widths by measuring header text and a sample of cached cell
// values, then apply them with table-layout:fixed.
// True while a header editor is open: a filter popup (numeric/factor/boolean,
// tracked by dismissActivePopup) or a focused inline text-filter input (which
// lives in the <th> and is not popup-tracked). Rebuilding the header row while
// one is open would tear it down mid-edit, dropping the user's keystrokes.
var isHeaderEditorOpen = function() {
   if (dismissActivePopup)
      return true;
   var active = document.activeElement;
   return !!(active && active.classList &&
             active.classList.contains("textFilterBox"));
};

// Run a header rebuild that was deferred because an editor was open. Scheduled
// asynchronously so it doesn't reenter DOM teardown from within the editor's
// own blur/dismiss handler.
var flushDeferredHeaderRebuild = function() {
   if (!deferredHeaderRebuild)
      return;
   setTimeout(function() {
      if (!deferredHeaderRebuild || isHeaderEditorOpen())
         return;
      deferredHeaderRebuild = false;
      autoSizeColumns();
      applyPinnedColumns();
   }, 0);
};

var autoSizeColumns = function() {
   var thead = document.getElementById("data_cols");
   // Widths are computed from columnOrder + data (below), so headers need not
   // exist yet -- this is also the path that first builds the windowed header.
   if (!thead || !cols || !columnOrder.length) return;

   // If the viewport isn't visible (e.g. background tab), measurements
   // will be wrong. Flag and bail; onActivate will re-run sizing once the
   // tab has real layout.
   var viewport = document.getElementById("gridViewport");
   if (viewport && viewport.offsetHeight === 0) {
      needsAutoSize = true;
      return;
   }

   // Don't rebuild the header row out from under an open filter editor (the
   // post-load width refine, fired from the initial row fetch, otherwise
   // destroys a filter the user just opened). Defer until the editor closes.
   if (isHeaderEditorOpen()) {
      deferredHeaderRebuild = true;
      return;
   }

   measuredWidths = [];
   var totalWidth = 0;

   // Memoize cell-text measurements for this pass. Sampled columns are often
   // low-cardinality (logical, factor, repeated integers), so the same string
   // is measured many times across the up-to-100-row sample; caching collapses
   // that to one measureText per distinct value. Lives for one pass only, so it
   // can't go stale and is bounded by the sample size.
   var cellWidthMemo = new Map();

   // CSS-derived chrome added beyond the measured text width:
   //   td/th: padding 5px each side + 1px border-right = 11px
   //   .dataCell/.listCell: padding-right overridden to 16px (= 22px chrome)
   //   .headerCell: padding-right 18px (sort indicator area)
   //   .pin-icon: 12px width + 3px margin-right = 15px
   //   .first-child (rownames): +4px padding each side beyond the base 5px
   var TD_EXTRA      = 11 + 2;            // chrome + sub-pixel safety
   var TD_DATA_EXTRA = 22 + 2;
   var TH_EXTRA      = 11 + 18 + 2;
   var PIN_ICON_W    = 15;
   var ROWNAMES_PAD  = 8;

   // Measure from data (col metadata + cached cell values), not from rendered
   // <th>/<td>. With column virtualization only a window of headers is in the
   // DOM at a time, so widths must be computable for every column regardless
   // of what's currently rendered. measuredWidths is indexed by columnOrder
   // position (same as columnOrder), so the offset/window math and the apply
   // loop below can both key off it.
   for (var i = 0; i < columnOrder.length; i++) {
      var colIdx = columnOrder[i];
      var col = cols[colIdx];
      var isRowNames = (colIdx === 0 && rowNumbers);
      var rowNamesPad = isRowNames ? ROWNAMES_PAD : 0;

      // Measure header text width (bold)
      var pinChrome = isRowNames ? 0 : PIN_ICON_W;
      var maxW = measureTextWidth(col.col_name || "", true) +
                 TH_EXTRA + pinChrome + rowNamesPad;

      // Cell chrome depends on the cell class
      var colClass = getColClass(col);
      var cellExtra = (colClass === "dataCell" || colClass === "listCell")
         ? TD_DATA_EXTRA : TD_EXTRA;
      cellExtra += rowNamesPad;

      // Fixed-glyph columns (numeric, Date, POSIXct) render only digits and
      // separators ('-', ':', '.', ' '), none wider than a digit; logical
      // columns render only TRUE / FALSE / NA. Neither needs text measured per
      // cell. Row names are excluded: their values are JSON-encoded and
      // proportional, so they take the generic measureText sampling path.
      var isFixedGlyph = !isRowNames && (isNumericColumn(col) || isDateColumn(col));

      // Fast paths that size a column with no per-cell sampling at all.
      var sized = false;
      if (col.col_type === "logical") {
         // Only TRUE / FALSE / NA: the widest literal is the column's cell width.
         var lw = logicalCellWidth() + cellExtra;
         if (lw > maxW) maxW = lw;
         sized = true;
      } else if (isFixedGlyph &&
                 typeof col.col_max_chars === "number" && col.col_max_chars > 0) {
         // The server's col_max_chars hint is computed over ALL rows, so
         // col_max_chars x widest-digit width is an exact upper bound -- cheaper
         // AND more accurate than sampling (which could miss the widest value).
         var nw = Math.ceil(col.col_max_chars * digitWidth()) + cellExtra;
         if (nw > maxW) maxW = nw;
         sized = true;
      }

      if (!sized) {
         // No usable metadata hint. For character/factor the rendered width is
         // proportional and must be measured; for fixed-glyph columns without a
         // hint (non-integral doubles, which the server declines to bound) the
         // cells are still digit-only, so the longest sampled string x digit
         // width bounds them -- a cheap string-length scan rather than a
         // measureText per cell.
         if (!isFixedGlyph &&
             typeof col.col_max_chars === "number" && col.col_max_chars > 0) {
            var hintW = Math.ceil(col.col_max_chars * avgCharWidth()) + cellExtra;
            if (hintW > maxW) maxW = hintW;
         }

         // Sample from the current render window rather than row 0: after a
         // column-window slide the cache holds the rows around the viewport,
         // which may be nowhere near the top.
         var sampleStart = Math.max(0, renderStart);
         var sampleEnd = sampleStart + Math.min(rowCache.size, 100);
         for (var r = sampleStart; r < sampleEnd; r++) {
            var row = rowCache.get(r);
            if (!row) continue;
            var cellVal = row[colIdx];
            if (cellVal === 0 || cellVal === null || cellVal === undefined) {
               // NA -- short
               cellVal = "NA";
            }
            var cellText = String(cellVal);

            var cellW;
            if (isFixedGlyph) {
               // Digit-bounded: char count x widest digit, no text measurement.
               // The non-finite literals Inf/-Inf/NaN contain letters that can
               // be slightly wider than a digit, so an all-non-finite column
               // (no col_max_chars) could be under-sized by a few px -- harmless
               // here since those tokens are short and MIN_COL_WIDTH (plus the
               // header text width) dominates.
               cellW = Math.ceil(cellText.length * digitWidth()) + cellExtra;
            } else {
               // For row names (col 0), the value is JSON-encoded.
               if (isRowNames) {
                  try { cellText = JSON.parse(cellText).toString(); } catch(e) { /* leave as-is */ }
               }
               var textW = cellWidthMemo.get(cellText);
               if (textW === undefined) {
                  textW = measureTextWidth(cellText, false);
                  cellWidthMemo.set(cellText, textW);
               }
               cellW = textW + cellExtra;
            }
            if (cellW > maxW) maxW = cellW;
         }
      }

      // Manual widths (user resize) take precedence over computed widths.
      // Otherwise clamp to min/max -- allow wider max for character columns.
      var manualW = manualWidths[absColIndex(colIdx)];
      var w;
      if (typeof manualW === "number" && manualW > 0) {
         w = manualW;
      } else {
         var upperBound = (col.col_type === "character") ? MAX_COL_WIDTH_CHAR : MAX_COL_WIDTH;
         w = Math.max(MIN_COL_WIDTH, Math.min(upperBound, maxW));
      }
      measuredWidths.push(w);
      totalWidth += w;
   }

   // The table spans the WHOLE frame: measured widths for the fetched
   // columns plus the estimated unfetched spans on either side, so the
   // horizontal scrollbar's range covers every column. Invalidate the layout
   // caches first -- the span widths depend on the (possibly just-changed)
   // fetched window bounds.
   invalidatePinnedOffsets();
   totalWidth += leftSpanWidth() + rightSpanWidth();

   var table = document.getElementById("rsGridData");
   if (table) {
      table.style.width = totalWidth + "px";
      table.style.tableLayout = "fixed";
   }

   totalTableWidth = totalWidth;

   // Recompute the visible column window against the new widths and (re)build
   // the windowed header row, which applies per-column widths to the rendered
   // headers. Widths now exist for every column, so the window is accurate.
   computeColumnWindow();
   rebuildHeaderWindow();
};

// ==========================================================================
// Column Resize
// ==========================================================================

var initResizeHandlers = function() {
   document.addEventListener("mousedown", function(evt) {
      if (evt.target.className !== "resizer") return;

      resizingColIdx = parseInt(evt.target.getAttribute("data-col"));
      didResize = false;
      initResizeX = evt.clientX;
      resizingBoundsExceeded = 0;

      var th = getHeaderCell(resizingColIdx);
      if (th) {
         initResizingWidth = th.offsetWidth;
         origTableWidth = totalTableWidth;
         if (typeof origColWidths[resizingColIdx] === "undefined") {
            origColWidths[resizingColIdx] = initResizingWidth;
         }
      }

      // Lock the cursor and suppress pointer events on grid content so the
      // drag isn't interrupted by hover effects, native tooltips, or
      // cursor:pointer elements (e.g. the pin icon) the cursor passes over.
      document.body.classList.add("col-resizing");

      evt.preventDefault();
   });

   document.addEventListener("mousemove", function(evt) {
      if (resizingColIdx === null) return;
      // Detect a stale drag -- the user may have released the mouse outside
      // the iframe, so we never saw the mouseup. evt.buttons === 0 means no
      // mouse buttons are pressed; clean up and bail out.
      if (evt.buttons === 0) { endResize(); return; }
      var delta = evt.clientX - initResizeX;
      if (delta !== 0) didResize = true;
      applyResizeDelta(delta);
      evt.preventDefault();
   });

   var endResize = function() {
      // Always remove the body class, even if resizingColIdx was already
      // cleared elsewhere (e.g. resetGridState during teardown). Otherwise
      // body.col-resizing's pointer-events: none would freeze the UI.
      document.body.classList.remove("col-resizing");
      if (resizingColIdx === null) return;
      resizingColIdx = null;
      saveState();
      // applyResizeDelta updates totalTableWidth on every mousemove but
      // skips applyPinnedColumns/updateCustomScrollbars to avoid jank
      // (200+ DOM writes plus layout reads per frame). Sync everything once at
      // end of drag. autoSizeColumns recomputes measuredWidths (honoring the
      // new manualWidths), which the column-window/offset math depends on, and
      // rebuilds the windowed header; then refresh rows, pinned offsets, and
      // the scrollbar thumb to match the new widths.
      if (didResize) {
         autoSizeColumns();
         renderVisibleRows(true);
         applyPinnedColumns();
         updateCustomScrollbars();
      }
      // Reset didResize after the click event that follows mouseup has been
      // dispatched, so exactly one click is suppressed (the one synthesized
      // from this drag) and subsequent clicks sort normally.
      if (didResize) {
         setTimeout(function() { didResize = false; }, 0);
      }
   };

   document.addEventListener("mouseup", endResize);
   // Fallback: if focus leaves the iframe entirely, cancel any in-flight drag
   // so col-resizing doesn't get stuck after a mouseup-outside scenario.
   window.addEventListener("blur", endResize);
};

var getHeaderCell = function(colIdx) {
   var thead = document.getElementById("data_cols");
   if (!thead) return null;
   return thead.querySelector('th[data-col-idx="' + colIdx + '"]');
};

var applyResizeDelta = function(delta) {
   if (resizingColIdx === null) return;

   var colWidth = initResizingWidth + delta;
   var minColWidth = origColWidths[resizingColIdx] || 50;
   if (minColWidth > 100) minColWidth = 100;

   if (delta < 0 && colWidth < minColWidth) {
      resizingBoundsExceeded += delta;
      return;
   }

   if (delta > 0 && resizingBoundsExceeded < 0) {
      resizingBoundsExceeded += delta;
      if (resizingBoundsExceeded < 0) return;
      delta = resizingBoundsExceeded;
      colWidth = initResizingWidth + delta;
   }

   // Apply width to header
   var th = getHeaderCell(resizingColIdx);
   if (th) {
      th.style.width = colWidth + "px";
      manualWidths[absColIndex(resizingColIdx)] = colWidth;
   }

   // Apply width to table
   var table = document.getElementById("rsGridData");
   if (table) {
      table.style.width = (origTableWidth + delta) + "px";
      totalTableWidth = origTableWidth + delta;
   }

   invalidatePinnedOffsets();
   // Note: saveState fires once at end of drag (in endResize), not on every
   // mousemove -- repeated localStorage writes during a drag would cause jank.
};

// ==========================================================================
// Filter UI
// ==========================================================================

// Column search types that have a typed filter widget. The rownames column and
// unsupported types (list, data.frame) have none.
var isFilterableSearchType = function(searchType) {
   return searchType === "numeric" || searchType === "date" ||
          searchType === "character" || searchType === "factor" ||
          searchType === "boolean";
};

// Build the typed filter widget for a column, dispatching on its search type.
// `idx` is the ABSOLUTE column index. `anchor`, when given, is the element the
// popup attaches to (and whose click toggles it) instead of the widget's own
// display element -- used by the sidebar to anchor the popup under its filter
// icon. Returns the widget's display element (with a `dvFilterController`
// property exposing { open, dismiss } for popup-backed types), or null.
var buildTypedFilterUI = function(idx, col, onDismiss, anchor) {
   if (col.col_search_type === "numeric") return createNumericFilterUI(idx, col, onDismiss, anchor);
   if (col.col_search_type === "date") return createDateFilterUI(idx, col, onDismiss, anchor);
   if (col.col_search_type === "factor") return createFactorFilterUI(idx, col, onDismiss, anchor);
   if (col.col_search_type === "character") return createTextFilterUI(idx, col, onDismiss, anchor);
   if (col.col_search_type === "boolean") return createBooleanFilterUI(idx, col, onDismiss, anchor);
   return null;
};

var createFilterUI = function(idx, col) {
   if (idx < 1) return null;

   var host = document.createElement("div");
   var val = null, ui = null;
   // Use classList rather than `className =` here and below so we don't
   // clobber the marker class that setHeaderUIVisible adds to the host
   // -- otherwise toggling filter visibility off after a filter has been
   // applied leaves stale UI in the DOM (the querySelectorAll for the
   // marker can no longer find it).
   host.classList.add("colFilter", "unfiltered");

   var setUnfiltered = function() {
      if (ui !== null) {
         if (ui.parentNode === host) host.replaceChild(val, ui);
         ui = null;
      }
      host.classList.remove("filtered");
      host.classList.add("unfiltered");
      clear.style.display = "none";

      // The header now shows "All"; drop any debounced apply still pending
      // from input typed just before the dismissal. The value-based watch
      // alone can't catch this when no filter was ever committed (the slot
      // is "" both when the apply was scheduled and now).
      bumpColumnFilterEpoch(idx);
   };

   var onDismiss = function() {
      var colSearch = getColumnSearch(idx);
      if (colSearch.length === 0) setUnfiltered();
   };

   var clear = document.createElement("div");
   clear.className = "clearFilter";
   clear.style.display = "none";
   clear.addEventListener("click", function(evt) {
      if (dismissActivePopup) dismissActivePopup(true);
      setColumnSearch(idx, "");
      applyFilters();
      setUnfiltered();
      evt.preventDefault();
      evt.stopPropagation();
   });
   host.appendChild(clear);

   val = document.createElement("div");
   val.textContent = "All";

   var buildTypedUI = function() {
      return buildTypedFilterUI(idx, col, onDismiss);
   };

   // Swap the "All" placeholder for the typed filter widget and mark the host
   // filtered. The typed widgets self-render from the cached search value
   // (createNumericFilterUI calls renderActiveFilter; the text/factor boxes
   // restore input.value), so this also correctly shows an already-applied
   // filter -- not just a freshly clicked one.
   var showFilteredUI = function() {
      ui = buildTypedUI();
      if (!ui) return false;
      ui.classList.add("filterValue");
      host.replaceChild(ui, val);
      host.classList.remove("unfiltered");
      host.classList.add("filtered");
      clear.style.display = "block";
      return true;
   };

   val.addEventListener("click", function(evt) {
      if (showFilteredUI()) {
         ui.dispatchEvent(new MouseEvent("click", { bubbles: true }));
         evt.preventDefault();
         evt.stopPropagation();
      }
   });

   host.appendChild(val);

   // Restore the filtered display when the column already has an active
   // filter. With column virtualization a header is destroyed and recreated
   // as it scrolls out of and back into the window, so without this the widget
   // would reset to "All" even though the filter is still applied server-side
   // (#17806).
   if (getColumnSearch(idx).length > 0) {
      showFilteredUI();
   }

   return host;
};

// Keyed by ABSOLUTE column index (cachedFilterValues' key), so a filter follows
// its column across pagination and can be driven from either the header (which
// passes col_index) or the sidebar (which works in absolute indices). Callers
// holding a window position must convert via absColIndex() first.
var getColumnSearch = function(absIdx) {
   return cachedFilterValues[absIdx] || "";
};

var setColumnSearch = function(absIdx, val) {
   if (val) {
      cachedFilterValues[absIdx] = val;
   } else {
      delete cachedFilterValues[absIdx];
   }
};

// Human-readable summary of a stored filter value ("type|value"), for the
// sidebar filter indicator's tooltip. Range filters (numeric / date) read as
// "lo to hi". Factor values are a stored level INDEX, not the label, so they'd
// read as a meaningless number ("Filtered: 3") -- return "" for them so the
// caller shows a generic "Filtered" instead. Everything else (character,
// boolean) is already human-readable.
var describeFilterValue = function(raw) {
   if (!raw) return "";
   var pipe = raw.indexOf("|");
   var type = pipe > 0 ? raw.substring(0, pipe) : "";
   var value = parseSearchString(raw);
   if ((type === "numeric" || type === "date") && value.indexOf("_") > 0) {
      var parts = value.split("_");
      return parts[0] + " to " + parts[1];
   }
   if (type === "factor")
      return "";
   return value;
};

// Per-column dismissal epochs, keyed by absolute column index. Bumped when a
// column's filter UI reverts to the unfiltered display (Escape, blur, the
// clear X) so debounced applies scheduled before the dismissal are dropped
// even when the stored search slot is unchanged -- which is exactly the case
// when the user was typing a *first* filter into an unfiltered column (the
// slot is "" at schedule time and still "" after the dismissal).
var columnFilterEpochs = {};

var bumpColumnFilterEpoch = function(absIdx) {
   columnFilterEpochs[absIdx] = (columnFilterEpochs[absIdx] || 0) + 1;
};

// Watch a column's stored search value (debounce's watch argument): a
// pending debounced filter apply is dropped if the filter was cleared or
// rewritten underneath it -- e.g. the header X was clicked while the
// clear's own blur fired a native "change" commit on the dirty input,
// scheduling one last apply that would silently re-apply the old filter
// with the header showing "All". The dismissal epoch is folded into the
// token so a dismissal also drops pending applies when the stored value
// didn't change (see columnFilterEpochs). Watching per-column state (rather
// than a global invalidation counter) means applies racing unrelated
// invalidations -- a sort, a search, another column's filter -- still run.
var watchColumnSearch = function(absIdx) {
   return function() {
      return (columnFilterEpochs[absIdx] || 0) + "#" + getColumnSearch(absIdx);
   };
};

var applyFilters = function() {
   clearActiveCell();
   invalidateCache();
   fetchRows(0, FETCH_SIZE, function() {
      scrollToTop();
   });
   // The sidebar summaries describe the filtered rows; recompute them (or
   // restore the whole-frame ones when the last filter was cleared).
   refreshSidebarSummaries();
   // Light the sidebar filter indicators immediately (the summary refresh is
   // async; this reflects the new filter state without waiting for it).
   updateSidebarColumnIndicators();
   saveState();
};

// Append apply (checkmark) and clear (x) buttons to the top-right of a
// numeric/date filter popup. onApply commits the current value -- an unchanged
// / full-range selection yields no filter (applyNumericFilter/applyDateFilter
// only store a filter when the value narrows the range), so applying a freshly
// opened popup does nothing. onClear drops the filter. Both then dismiss. The
// header chip has its own clear X, but a sidebar-opened popup has neither, so
// these are its only apply/clear affordances.
var appendPopupActions = function(popup, onApply, onClear) {
   var addButton = function(cls, label, handler) {
      var btn = document.createElement("span");
      btn.className = cls;
      btn.title = label;
      btn.setAttribute("role", "button");
      btn.setAttribute("tabindex", "0");
      btn.setAttribute("aria-label", label);
      var run = function(evt) {
         evt.stopPropagation();
         evt.preventDefault();
         handler();
         if (dismissActivePopup) dismissActivePopup(true);
      };
      btn.addEventListener("click", run);
      btn.addEventListener("keydown", function(evt) {
         if (evt.key === "Enter" || evt.key === " ") run(evt);
      });
      popup.appendChild(btn);
   };
   addButton("filterPopupApply", "Apply filter", onApply);
   addButton("filterPopupClear", "Clear filter", onClear);
};

// Shared brushable range-filter widget for numeric and date/datetime columns.
// Both render a histogram (hist.js) with a draggable brush plus a text box, and
// share all the popup wiring -- debounced apply, Enter/Escape commit/cancel, the
// change-replay suppression, and the apply/clear buttons. The `spec` supplies
// the type-specific pieces:
//   initialRange()        -> { lo, hi } seed values for the box and brush
//   filterFromRange(s, e) -> the box's display string for a [s, e] selection
//   textFromBrush(s, e)   -> the box value for a brush move (raw break values)
//   binsFor(lo, hi)       -> { start, end } brushed histogram bins
//   applyFilter(boxValue) -> parse the box, store the search, applyFilters()
// The header label rendering ("[lo, hi]" / "[v]" / "[...]") is identical for
// both types, so it lives here.
var createRangeFilterUI = function(idx, col, onDismiss, anchor, spec) {
   var ele = document.createElement("div");

   // Set by buildPopup each time the popup opens; lets the dismiss wrapper
   // below commit a brush/typed value still inside the debounce window.
   var flushPendingApply = null;

   // Render the active filter into the header label: "[15, 30]" for a range,
   // "[15]" for a single value, "[...]" while the popup is open with no value
   // yet. Keeps the user oriented on what's filtered without reopening it.
   var renderActiveFilter = function() {
      var raw = parseSearchString(getColumnSearch(idx));
      if (raw.length === 0) {
         ele.textContent = "[...]";
         return;
      }
      var sep = raw.indexOf("_");
      if (sep > 0) {
         ele.textContent = "[" + raw.substring(0, sep) +
                           ", " + raw.substring(sep + 1) + "]";
      } else {
         ele.textContent = "[" + raw + "]";
      }
   };

   ele.dvFilterController = invokeFilterPopup(anchor || ele, function(popup) {
      popup.classList.add("numericFilterPopup");

      var init = spec.initialRange();

      var valBox = document.createElement("input");
      valBox.type = "text";
      valBox.className = "numValueBox";
      valBox.style.textAlign = "center";
      valBox.value = spec.filterFromRange(init.lo, init.hi);

      // Commit the current box value: spec.applyFilter parses it and stores the
      // search (or clears it for a full-range selection), then refresh the label.
      var applyValue = function() {
         spec.applyFilter(valBox.value);
         renderActiveFilter();
      };

      var updateView = debounce(
         TIMING.filterDebounce, watchColumnSearch(idx), applyValue);
      flushPendingApply = function() { updateView.flush(); };

      // Dismissing the popup removes a focused, dirty input from the DOM,
      // which replays a native "change". Suppressed once the keydown handler
      // below has already committed (Enter) or cancelled (Escape) the value;
      // otherwise that replay would schedule one more apply that lands after
      // the popup is gone.
      var suppressChange = false;
      valBox.addEventListener("change", function() {
         if (suppressChange) return;
         updateView();
      });
      valBox.addEventListener("click", function(evt) { evt.stopPropagation(); });
      valBox.addEventListener("keydown", function(evt) {
         if (!dismissActivePopup) return;
         if (evt.keyCode === 27) {
            // Escape cancels: drop any pending debounced apply rather than
            // letting it land after the popup closes, and revert the header
            // to "All" when no value was ever committed (matching the text
            // filter's Escape behavior).
            suppressChange = true;
            updateView.cancel();
            dismissActivePopup(false);
            onDismiss();
         } else if (evt.keyCode === 13) {
            // Enter commits: apply synchronously so the value is stored
            // before the dismissal's onDismiss inspects the search slot --
            // a debounced apply would still be pending at that point and
            // the column would wrongly revert to the unfiltered display.
            suppressChange = true;
            updateView.cancel();
            applyValue();
            dismissActivePopup(true);
         }
      });

      var histBrush = document.createElement("div");
      histBrush.className = "numHist";
      // A click inside the histogram adjusts the brush; stop it from bubbling
      // to the body-level light-dismiss handler, which would close the popup
      // (the value box already stops its own clicks for the same reason).
      histBrush.addEventListener("click", function(evt) { evt.stopPropagation(); });

      var bins = spec.binsFor(init.lo, init.hi);

      // Use the existing hist.js for interactive histogram. A brush move sets
      // the box value from the (raw) break values and schedules an apply.
      hist(histBrush, col.col_breaks, col.col_counts, bins.start, bins.end,
         function(start, end) {
            valBox.value = spec.textFromBrush(start, end);
            updateView();
         });

      popup.appendChild(histBrush);
      popup.appendChild(valBox);
      appendPopupActions(popup, function() {
         // Apply: commit the current box value (no-op filter if it's the full
         // range). suppressChange stops the input's removal-triggered change
         // from scheduling a second, late apply.
         suppressChange = true;
         updateView.cancel();
         applyValue();
      }, function() {
         suppressChange = true;
         updateView.cancel();
         setColumnSearch(idx, "");
         applyFilters();
         renderActiveFilter();
      });
   }, function() {
      // Light dismiss (click-away / Enter) commits a brush or typed value
      // still inside the debounce window before onDismiss inspects the
      // search slot -- it's user intent, not noise. Escape cancels the
      // pending apply explicitly before this runs, so its flush is a no-op.
      if (flushPendingApply) flushPendingApply();
      onDismiss();
   });

   renderActiveFilter();
   return ele;
};

var createNumericFilterUI = function(idx, col, onDismiss, anchor) {
   var fullMin = col.col_breaks[0];
   var fullMax = col.col_breaks[col.col_breaks.length - 1];

   var filterFromRange = function(s, e) {
      if (Math.abs(s - e) === 0) return "" + s;
      return s + " - " + e;
   };

   // Numeric tokens accept optional scientific-notation suffix (1e10,
   // 2.5e-5). The range separator is `-` surrounded by required
   // whitespace, which disambiguates from a leading negative sign.
   var NUM = "-?\\d+\\.?\\d*(?:[eE][+-]?\\d+)?";
   var SINGLE_RE = new RegExp("^\\s*" + NUM + "\\s*$");
   var RANGE_RE = new RegExp(
      "^\\s*(" + NUM + ")\\s*-\\s*(" + NUM + ")\\s*");

   return createRangeFilterUI(idx, col, onDismiss, anchor, {
      initialRange: function() {
         var val = parseSearchString(getColumnSearch(idx));
         if (val.indexOf("_") > 0) {
            var range = val.split("_");
            return { lo: range[0], hi: range[1] };
         }
         if (!isNaN(parseFloat(val)) && val.length > 0) {
            var n = parseFloat(val);
            return { lo: n, hi: n };
         }
         return { lo: fullMin.toString(), hi: fullMax.toString() };
      },
      filterFromRange: filterFromRange,
      textFromBrush: filterFromRange,
      binsFor: function(lo, hi) {
         var binStart = 0;
         var binEnd = col.col_breaks.length - 2;
         for (var i = 0; i < col.col_breaks.length; i++) {
            if (Math.abs(col.col_breaks[i] - lo) < Math.abs(col.col_breaks[binStart] - lo))
               binStart = i;
            if (i === 0) continue;
            if (Math.abs(col.col_breaks[i] - hi) < Math.abs(col.col_breaks[binEnd] - hi))
               binEnd = i - 1;
         }
         if (binEnd < binStart) binStart = binEnd;
         return { start: binStart, end: binEnd };
      },
      applyFilter: function(v) {
         var searchText = "";
         v = v.replace(/[^-+0-9 .eE]/g, "");
         var digit = v.match(SINGLE_RE);
         if (digit !== null) {
            searchText = digit[0].trim();
         } else {
            var matches = v.match(RANGE_RE);
            if (matches !== null && matches.length > 2) {
               if (Math.abs(parseFloat(matches[1]) - col.col_breaks[0]) !== 0 ||
                   Math.abs(parseFloat(matches[2]) - col.col_breaks[col.col_breaks.length - 1]) !== 0) {
                  searchText = matches[1] + "_" + matches[2];
               }
            }
         }
         if (searchText.length > 0) searchText = "numeric|" + searchText;
         setColumnSearch(idx, searchText);
         applyFilters();
      }
   });
};

// Date / datetime range filter. Reuses the numeric filter's brushable
// histogram (and its CSS), but the histogram breaks are epoch values paired
// with formatted col_break_labels, so the brush and the text box operate in
// formatted dates. The serialized filter value is "date|<lo>_<hi>" -- two ISO
// strings the backend parses on the native Date/POSIXct scale. The brush always
// produces a range; a single typed value is accepted as a one-instant range
// (lo === hi), which for a Date is that whole day.
var createDateFilterUI = function(idx, col, onDismiss, anchor) {
   var labels = col.col_break_labels || [];
   var fullMin = labels.length > 0 ? labels[0] : "";
   var fullMax = labels.length > 0 ? labels[labels.length - 1] : "";

   // Map an epoch break value (what hist.js hands back) to its formatted
   // label by locating it in col_breaks; nearest wins if precision drifts.
   var labelForBreak = function(v) {
      var best = 0;
      for (var i = 0; i < col.col_breaks.length; i++) {
         if (Math.abs(col.col_breaks[i] - v) < Math.abs(col.col_breaks[best] - v))
            best = i;
      }
      return labels[best] !== undefined ? labels[best] : String(v);
   };

   var filterFromRange = function(s, e) {
      if (s === e) return "" + s;
      return s + " - " + e;
   };

   return createRangeFilterUI(idx, col, onDismiss, anchor, {
      initialRange: function() {
         var val = parseSearchString(getColumnSearch(idx));
         if (val.indexOf("_") > 0) {
            var range = val.split("_");
            return { lo: range[0], hi: range[1] };
         }
         return { lo: fullMin, hi: fullMax };
      },
      filterFromRange: filterFromRange,
      textFromBrush: function(start, end) {
         return filterFromRange(labelForBreak(start), labelForBreak(end));
      },
      binsFor: function(lo, hi) {
         // Seed the brushed bins from the active filter's labels (if any).
         var binStart = 0;
         var binEnd = col.col_breaks.length - 2;
         var loIdx = labels.indexOf(lo);
         var hiIdx = labels.indexOf(hi);
         if (loIdx >= 0) binStart = loIdx;
         if (hiIdx >= 1) binEnd = hiIdx - 1;
         if (binEnd < binStart) binStart = binEnd;
         return { start: binStart, end: binEnd };
      },
      applyFilter: function(v) {
         var searchText = "";
         var parts = v.split(" - ");
         var a = parts[0] ? parts[0].trim() : "";
         var b = (parts.length > 1 && parts[1]) ? parts[1].trim() : a;
         // Only treat it as a filter when it narrows the full range; an
         // untouched [min, max] selection clears the filter.
         if (a.length > 0 && b.length > 0 && (a !== fullMin || b !== fullMax))
            searchText = "date|" + a + "_" + b;
         setColumnSearch(idx, searchText);
         applyFilters();
      }
   });
};

var createTextFilterBox = function(ele, idx, col, onDismiss) {
   var input = document.createElement("input");
   input.type = "text";
   input.className = "textFilterBox";

   var search = getColumnSearch(idx).split("|");
   if (search.length > 1 && search[0] === "character")
      input.value = decodeURIComponent(search[1]);

   var updateView = debounce(TIMING.filterDebounce, watchColumnSearch(idx), function() {
      setColumnSearch(idx, "character|" + encodeURIComponent(input.value));
      applyFilters();
   });

   input.addEventListener("keyup", function(evt) {
      // Escape dismisses (handled on keydown below); its keyup must not
      // schedule one more apply of the abandoned input, which would land
      // after the dismissal with the header showing "All".
      if (evt.keyCode === 27) return;
      updateView();
   });
   input.addEventListener("keydown", function(evt) {
      if (evt.keyCode === 27) onDismiss();
   });
   ele.addEventListener("click", function(evt) {
      input.focus();
      evt.preventDefault();
      evt.stopPropagation();
   });
   ele.appendChild(input);
   return input;
};

var createFactorFilterUI = function(idx, col, onDismiss, anchor) {
   var ele = document.createElement("div");
   var input = createTextFilterBox(ele, idx, col, onDismiss);

   // createTextFilterBox only restores "character|" values, so a recreated
   // header (column scrolled back into view, or saved state restored) would
   // show a blank box while a level filter is still applied server-side.
   // Map the stored 1-based level index back to its level name.
   var search = getColumnSearch(idx).split("|");
   if (search.length > 1 && search[0] === "factor") {
      var level = parseInt(search[1], 10);
      if (level >= 1 && level <= col.col_vals.length)
         input.value = col.col_vals[level - 1];
   }

   input.addEventListener("keyup", function() {
      if (dismissActivePopup) dismissActivePopup(false);
   });
   input.addEventListener("blur", function() {
      if (!dismissActivePopup) onDismiss();
   });
   input.addEventListener("focus", function() {
      if (dismissActivePopup) dismissActivePopup(false);
   });

   ele.dvFilterController = invokeFilterPopup(anchor || ele, function(popup) {
      var list = document.createElement("div");
      list.className = "choiceList";

      for (var i = 0; i < col.col_vals.length; i++) {
         var opt = document.createElement("div");
         opt.textContent = col.col_vals[i];
         opt.className = "choiceListItem";
         (function(factor, text) {
            opt.addEventListener("click", function() {
               setColumnSearch(idx, "factor|" + factor);
               applyFilters();
               input.value = text;
            });
         })(i + 1, col.col_vals[i]);
         list.appendChild(opt);
      }
      popup.appendChild(list);
   }, onDismiss);

   return ele;
};

var createTextFilterUI = function(idx, col, onDismiss, anchor) {
   var ele = document.createElement("div");

   // Alternate location (sidebar): the character filter is normally a bare
   // inline header input, but with an anchor we present a padded text box with
   // placeholder guidance inside a popup attached to the trigger (the sidebar
   // filter icon) -- the divergence from the inline header widget is intended.
   if (anchor) {
      ele.dvFilterController = invokeFilterPopup(anchor, function(popup) {
         popup.classList.add("textFilterPopup");
         var input = createTextFilterBox(popup, idx, col, onDismiss);
         input.placeholder = "Filter " + col.col_name;
         setTimeout(function() { input.focus(); }, 0);
      }, onDismiss);
      return ele;
   }

   var input = createTextFilterBox(ele, idx, col, onDismiss);
   input.addEventListener("blur", function() {
      onDismiss();
      flushDeferredHeaderRebuild();
   });
   input.addEventListener("focus", function() {
      if (dismissActivePopup) dismissActivePopup(true);
   });
   return ele;
};

var createBooleanFilterUI = function(idx, col, onDismiss, anchor) {
   var ele = document.createElement("div");
   var display = document.createElement("span");
   display.innerHTML = "&nbsp;";
   ele.appendChild(display);

   // Restore the active selection so a recreated header (column scrolled back
   // into view) shows the applied value rather than a blank, matching the
   // self-rendering numeric/text/factor widgets.
   var boolSearch = getColumnSearch(idx).split("|");
   if (boolSearch.length > 1 && boolSearch[0] === "boolean") {
      display.textContent = boolSearch[1];
   }

   ele.dvFilterController = invokeFilterPopup(anchor || ele, function(popup) {
      var list = document.createElement("div");
      list.className = "choiceList";
      var values = ["TRUE", "FALSE"];
      for (var i = 0; i < values.length; i++) {
         var opt = document.createElement("div");
         opt.textContent = values[i];
         opt.className = "choiceListItem";
         (function(text) {
            opt.addEventListener("click", function() {
               setColumnSearch(idx, "boolean|" + text);
               applyFilters();
               display.textContent = text;
            });
         })(values[i]);
         list.appendChild(opt);
      }
      popup.appendChild(list);
   }, onDismiss);

   return ele;
};

// Light-dismiss popups: any body click outside the popup dismisses, plus any
// unstopped click inside the popup (e.g., a factor list item that wants
// "apply and close" semantics). Inner controls that should NOT dismiss the
// popup must stopPropagation on their click events.
var invokeFilterPopup = function(ele, buildPopup, onDismiss) {
   var popup = null;

   var dismissPopup = function(actionComplete) {
      if (popup) {
         document.body.removeChild(popup);
         document.body.removeEventListener("click", checkLightDismiss);
         document.body.removeEventListener("keydown", checkEscDismiss);
         dismissActivePopup = null;
         popup = null;
         if (actionComplete) onDismiss();
         flushDeferredHeaderRebuild();
         return true;
      }
      return false;
   };

   var checkLightDismiss = function() {
      if (popup) dismissPopup(true);
   };

   var checkEscDismiss = function(evt) {
      if (popup && evt.keyCode === 27) dismissPopup(true);
   };

   // Open the popup (or toggle it closed if already open), anchored under
   // `ele`. Dismisses any other open filter popup first. Exposed on the
   // returned controller so callers (e.g. the sidebar filter icon) can open it
   // programmatically rather than only via the bound click.
   var openPopup = function() {
      if (dismissActivePopup && dismissActivePopup !== dismissPopup) {
         dismissActivePopup(true);
      }
      if (popup) {
         dismissPopup(true);
         return;
      }
      popup = document.createElement("div");
      popup.className = "filterPopup";
      var popupInfo = buildPopup(popup);
      document.body.appendChild(popup);

      var rect = ele.getBoundingClientRect();
      // +2px gap so the popup sits just below the trigger rather than flush.
      var top = rect.bottom + 2 + (!popupInfo ? 0 : (popupInfo.top || 0));
      var left = rect.left + (!popupInfo ? -4 : (popupInfo.left || -4));

      // When opened from the summary sidebar, center the popup within the
      // sidebar column rather than anchoring to the narrow, right-aligned icon
      // (which pushed it against the panel's right edge).
      var sidebarPanel = ele.closest ? ele.closest("#sidebarPanel") : null;
      if (sidebarPanel) {
         var sb = sidebarPanel.getBoundingClientRect();
         left = sb.left + (sb.width - popup.offsetWidth) / 2;
      }

      // Keep the popup fully on-screen on both edges.
      if (popup.offsetWidth + left > document.body.offsetWidth)
         left = document.body.offsetWidth - popup.offsetWidth;
      if (left < 0)
         left = 0;

      popup.style.top = top + "px";
      popup.style.left = left + "px";

      document.body.addEventListener("click", checkLightDismiss);
      document.body.addEventListener("keydown", checkEscDismiss);
      dismissActivePopup = dismissPopup;
   };

   ele.addEventListener("click", function(evt) {
      openPopup();
      evt.preventDefault();
      evt.stopPropagation();
   });

   return { open: openPopup, dismiss: dismissPopup };
};

// ==========================================================================
// Column Type UI (data import mode)
// ==========================================================================

var createColumnTypesUI = function(th, idx, col) {
   var host = document.createElement("div");
   host.className = "columnTypeWrapper";

   var val = document.createElement("div");
   // show the user's assigned type if they picked one in the import dialog,
   // otherwise fall back to the detected R class
   val.textContent = "(" + (col.col_type_assigned || colClassLabel(col)) + ")";
   val.className = "columnTypeHeader";

   th.classList.add("columnClickable");
   th.addEventListener("click", function(evt) {
      if (columnsPopup !== th) {
         columnsPopup = th;
         var rect = host.getBoundingClientRect();
         var parentRect = th.getBoundingClientRect();
         activeColumnInfo = {
            left: rect.left - 5,
            top: parentRect.height + 11,
            width: parentRect.width - 1,
            index: idx,
            name: col.col_name
         };
         if (onColumnOpen) onColumnOpen();
         evt.preventDefault();
         evt.stopPropagation();
      } else {
         columnsPopup = null;
         if (onColumnDismiss) onColumnDismiss();
      }
   });

   host.appendChild(val);
   return host;
};

// ==========================================================================
// Virtual Scroll Engine
// ==========================================================================

var scrollToTop = function() {
   var viewport = document.getElementById("gridViewport");
   if (viewport) setViewportScrollTop(viewport, 0);
};

// Single funnel for programmatic horizontal scroll changes (reveals, anchor /
// saved-state restores, column-window slides). Sets viewport.scrollLeft and
// keeps lastScrollLeft -- the module's mirror of the live scroll position, read
// by saveState / onActivate / the info bar's column range -- in sync
// synchronously, rather than relying on the async scroll event to heal it.
// Clamps negatives, and records the browser's (possibly clamped) resulting
// scrollLeft so the mirror reflects reality even when the request overshoots.
// Returns true when the request differed from the current position. Live-gesture
// scrolling (native wheel, custom scrollbar drag) is mirrored by onScroll
// instead; those are not routed through here.
var setViewportScrollLeft = function(viewport, left) {
   left = Math.max(0, left);
   if (viewport.scrollLeft === left)
      return false;
   viewport.scrollLeft = left;
   lastScrollLeft = viewport.scrollLeft;
   return true;
};

// Vertical counterpart to setViewportScrollLeft: applies a programmatic
// scrollTop and keeps lastScrollTop -- the mirror read by saveState / onActivate
// / the info bar's "Showing X to Y" range -- in sync synchronously. The scroll
// math that decides the target row lives in the callers (e.g.
// ensureActiveCellVisible); this only applies it and records the (possibly
// clamped) result. Live-gesture scrolling is mirrored by onScroll instead.
var setViewportScrollTop = function(viewport, top) {
   top = Math.max(0, top);
   if (viewport.scrollTop === top)
      return false;
   viewport.scrollTop = top;
   lastScrollTop = viewport.scrollTop;
   return true;
};

// Snapshot the live scroll position (and the current unfiltered row count) so a
// refresh triggered by an in-place data change can restore the user's position
// after the grid is torn down and rebuilt. Must run before bootstrap() (whose
// destroyGrid zeroes the viewport and lastScroll* state).
var captureScrollForRefresh = function() {
   var viewport = document.getElementById("gridViewport");
   var top = viewport ? viewport.scrollTop : 0;
   var left = viewport ? viewport.scrollLeft : 0;
   // If the refresh fired while the tab was inactive, the live scrollTop may
   // already read 0 (onDeactivate folded it into lastScrollTop); fall back to
   // the saved value so the position isn't lost.
   if (top === 0 && lastScrollTop > 0) top = lastScrollTop;
   if (left === 0 && lastScrollLeft > 0) left = lastScrollLeft;
   pendingScrollRestore = { top: top, left: left, rows: totalRows };
};

// After a refresh re-fetches its first row batch, restore the pre-refresh
// scroll position -- but only when the underlying (unfiltered) row count is
// unchanged. A changed row count means the data was materially replaced (rows
// subset, reassigned, appended, or removed), so the row the user was looking at
// no longer maps to the same thing; leave the grid at the top instead. The
// transformations we do want to preserve -- adding a column, removing a column,
// editing values in an existing column -- all leave recordsTotal unchanged,
// which is exactly what this row-count compare detects.
//
// The compare is against the unfiltered total, not the filtered view. Per-column
// filters are persisted across the refresh, so the filtered position is also
// preserved. An active *global* search is not persisted (cachedSearch is cleared
// on refresh); when one was active, filteredRows snaps back to totalRows and the
// restored offset lands on a different (clamped, never out-of-range) row. That
// edge is benign and rare enough not to special-case here.
var restoreScrollAfterRefresh = function() {
   var restore = activeScrollRestore;
   activeScrollRestore = null;
   if (!restore || restore.rows !== totalRows)
      return;

   // Guard against a rapid second refresh landing in the gap before this frame
   // paints: capture the current draw and bail if another fetch has superseded
   // us, so a stale offset isn't applied to a newer render.
   var restoreToken = drawCounter;

   // The viewport's scrollable height only exists once renderVisibleRows has
   // sized the spacer rows, which fetchRows does right after this callback.
   // Defer to the next frame so the scrollTop assignment isn't clamped to 0
   // against a body that hasn't grown yet.
   requestAnimationFrame(function() {
      if (restoreToken !== drawCounter)
         return;
      var viewport = document.getElementById("gridViewport");
      if (!viewport)
         return;
      setViewportScrollTop(viewport, restore.top);
      setViewportScrollLeft(viewport, restore.left);
      renderVisibleRows(true);
      updateInfoBar();
      updateCustomScrollbars();
   });
};

var updateAriaRowCount = function() {
   // Expose the navigable row count to assistive tech. Use filteredRows so
   // a screen reader announces the rows the user can actually move through,
   // not the unfiltered backing total.
   var table = document.getElementById("rsGridData");
   if (!table) return;
   if (totalRows > 0) {
      table.setAttribute("aria-rowcount", String(filteredRows));
   } else {
      table.removeAttribute("aria-rowcount");
   }
};

// Height of the viewport area in which data rows are actually visible. The
// viewport's full clientHeight overstates this: the sticky <thead> overlays
// the top (while still contributing to scroll content height), and when
// horizontal scroll is active the custom horizontal scrollbar overlays the
// bottom 10px (see .custom-scrollbar.horizontal in DataViewer.css).
var visibleBodyHeight = function(viewport) {
   var headerEl = document.getElementById("data_cols");
   var headerH = (headerEl && headerEl.parentElement)
      ? headerEl.parentElement.offsetHeight : 0;
   var hasHScroll = viewport.scrollWidth > viewport.clientWidth + 1;
   return Math.max(0, viewport.clientHeight - headerH - (hasHScroll ? 10 : 0));
};

// Update the "Sorted by" portion of the info bar. The text span and the
// clear-sort button are static elements from gridviewer.html; toggle the
// button's visibility rather than rebuilding it so the click listener
// wired at startup survives updates.
var setSortStatus = function(text) {
   var textEl = document.getElementById("rsGridData_info_sort_text");
   if (textEl) textEl.textContent = text;
   var clearEl = document.getElementById("rsGridData_info_sort_clear");
   if (clearEl) clearEl.style.display = text ? "inline-block" : "none";
};

// ", columns X to Y of N" for the (unpinned) columns currently in view, or
// "" when the frame doesn't column-slide (it fits in one fetch window) or
// the layout isn't measured yet -- callers fall back to the total-only
// form. The range exists to keep the user oriented during long-range
// horizontal scrolling; for a frame that's merely clipped by the viewport,
// the plain total reads better and stays stable.
var visibleColumnRangeText = function() {
   if (!cols || measuredWidths.length === 0 || totalCols <= 0)
      return "";
   if (maxDisplayColumns <= 0 || totalCols <= maxDisplayColumns)
      return "";
   var viewport = document.getElementById("gridViewport");
   if (!viewport)
      return "";

   var pinnedW = getPinnedOffsets().totalWidth;
   var vLo = absColAtContentX(lastScrollLeft + pinnedW);
   var vHi = absColAtContentX(lastScrollLeft + viewport.clientWidth);
   if (vLo <= 1 && vHi >= totalCols)
      return "";

   return ", columns " + vLo.toLocaleString() + " to " + vHi.toLocaleString() +
          " of " + totalCols.toLocaleString();
};

var updateInfoBar = function() {
   updateAriaRowCount();

   // Suppress info bar updates during custom scrollbar drags -- reading
   // scrollTop forces style+layout recalc which causes jank. The info
   // bar is updated once when the drag ends.
   if (anyScrollbarDragging()) return;

   var info = document.getElementById("rsGridData_info");
   if (!info) return;
   var textEl = document.getElementById("rsGridData_info_text");

   if (statusTextOverride) {
      if (textEl) textEl.textContent = statusTextOverride;
      setSortStatus("");
      return;
   }

   var activeRows = filteredRows;
   if (totalRows === 0) {
      if (textEl) textEl.textContent = "";
      setSortStatus("");
      return;
   }

   // Count a row as "in view" when at least 50% of it is visible, using the
   // effective body-visible height (visibleBodyHeight) for the bottom edge.
   //
   // Top edge: ceil((scrollTop + H/2) / H) is round-half-down of
   // scrollTop/H + 1, which keeps a row scrolled exactly halfway out in
   // the range (Math.round would round half away from zero and exclude
   // it).
   //
   // Bottom edge: Math.round is half-up, which correctly includes a row
   // that's exactly 50% visible at the bottom of the data area.
   var first, last;
   if (activeRows === 0) {
      // Filter matched no rows. Use a 0/0 range so we don't claim a
      // bogus "Showing 1 to 1 of 0" -- the (filtered from N total) suffix
      // below explains why.
      first = 0;
      last = 0;
   } else {
      var viewport = document.getElementById("gridViewport");
      var bodyH = viewport ? visibleBodyHeight(viewport) : 0;
      first = Math.ceil((lastScrollTop + ROW_HEIGHT / 2) / ROW_HEIGHT);
      last = Math.round((lastScrollTop + bodyH) / ROW_HEIGHT);
      // Clamp both bounds against the data extent. At max scroll on a
      // viewport shorter than a row, first can otherwise overshoot
      // activeRows and produce an out-of-bounds range like "11 to 11 of 10".
      first = Math.max(1, Math.min(first, activeRows));
      last = Math.max(first, Math.min(last, activeRows));
   }

   var text = "Showing " + first.toLocaleString() + " to " + last.toLocaleString() +
      " of " + activeRows.toLocaleString() + " entries";
   if (filteredRows < totalRows) {
      text += " (filtered from " + totalRows.toLocaleString() + " total entries)";
   }
   if (totalCols > 0) {
      // When the frame is wider than the viewport, orient the user with the
      // visible column range (the toolbar no longer carries a column-window
      // readout); otherwise just report the total.
      var colRange = visibleColumnRangeText();
      text += colRange !== ""
         ? colRange
         : ", " + totalCols.toLocaleString() +
           (totalCols === 1 ? " total column" : " total columns");
   }
   if (textEl) textEl.textContent = text;

   var sortText = "";
   if (sortColumn >= 0 && sortDirection && cols) {
      var sortPos = posForAbsColIndex(sortColumn);
      if (sortPos >= 0 && cols[sortPos]) {
         var dirText = sortDirection === "asc" ? "ascending" : "descending";
         sortText = "Sorted by: " + cols[sortPos].col_name + " (" + dirText + ")";
      }
   }
   setSortStatus(sortText);
};

var buildRow = function(r) {
   var rowData = rowCache.get(r);
   if (!rowData) return null;

   var tr = document.createElement("tr");
   tr.setAttribute("data-row", r);
   tr.setAttribute("role", "row");
   // aria-rowindex is 1-based and includes the header row.
   tr.setAttribute("aria-rowindex", String(r + 2));
   // Zebra striping is driven by data-row parity, not DOM position. The tbody
   // contains a leading spacer row plus only the virtual window of data rows,
   // so :nth-child would flip the stripe pattern as the window slides.
   if (r % 2 === 1) tr.classList.add("odd-row");

   // Render only the pinned columns plus the visible column window (with
   // left/right spacers for the rest) -- see "Column virtualization". `pos`
   // is the columnOrder position, matching activeCol's coordinate space.
   appendWindowedCells(
      tr,
      function(pos) {
         var colIdx = columnOrder[pos];
         var clazz = getColClass(cols[colIdx]);
         var td = createCell(rowData[colIdx], colIdx, rowData, clazz);
         td.setAttribute("role", "gridcell");
         // Record the columnOrder position so click handling can map a cell
         // back to its column without relying on DOM position (spacer cells
         // make tr.children non-1:1 with columnOrder).
         td.setAttribute("data-col-pos", pos);
         if (r === activeRow && pos === activeCol) {
            td.classList.add("activeCell");
            // Stable id so the viewport's aria-activedescendant can refer
            // to this cell. Re-applied here because rows are recreated
            // when they scroll back into view.
            td.id = activeCellId(r, pos);
         }
         return td;
      },
      "td"
   );

   return tr;
};

var createSpacerRow = function(colSpan) {
   var tr = document.createElement("tr");
   tr.className = "spacer-row";
   var td = document.createElement("td");
   td.colSpan = colSpan;
   td.style.padding = "0";
   td.style.border = "none";
   td.style.height = "0px";
   tr.appendChild(td);
   return tr;
};

var updateSpacerRowHeight = function(spacerTr, heightPx) {
   if (spacerTr && spacerTr.firstChild) {
      spacerTr.firstChild.style.height = heightPx + "px";
   }
};

// Render rows visible in the current scroll window. Two paths:
//   - Incremental (default): patch the existing DOM, adding/removing only
//     rows that crossed the window edge. Fast; used for normal scroll.
//   - Full rebuild (forceRebuild=true): wipe and re-render the whole window.
//     Required after fetches, sort/filter, sidebar toggle, resize, etc. --
//     anything that invalidates row content or window layout.
var renderVisibleRows = function(forceRebuild) {
   var viewport = document.getElementById("gridViewport");
   var tbody = document.getElementById("gridBody");
   if (!viewport || !tbody || !cols) return;

   var scrollTop = viewport.scrollTop;
   var viewportH = viewport.clientHeight;
   var activeRows = filteredRows;

   if (activeRows === 0) {
      tbody.innerHTML = "";
      renderedRowElements.clear();
      topSpacerRow = null;
      bottomSpacerRow = null;
      renderStart = 0;
      renderEnd = 0;
      return;
   }

   var firstVisible = Math.floor(scrollTop / ROW_HEIGHT);
   var visibleCount = Math.ceil(viewportH / ROW_HEIGHT);

   var newStart = Math.max(0, firstVisible - BUFFER_ROWS);
   var newEnd = Math.min(activeRows - 1, firstVisible + visibleCount + BUFFER_ROWS);

   // Skip if the render window hasn't changed
   if (!forceRebuild && newStart === renderStart && newEnd === renderEnd) {
      return;
   }

   // Check if we need to fetch more data. A block is fetched when absent
   // from the cache OR present but incomplete for the current column set
   // (remapped across a column-window slide; see blockIsCurrent).
   var firstBlock = Math.floor(newStart / FETCH_SIZE) * FETCH_SIZE;
   for (var blockStart = firstBlock; blockStart <= newEnd; blockStart += FETCH_SIZE) {
      if (!blockIsCurrent(blockStart) && !pendingFetches.has(blockStart + "-" + FETCH_SIZE)) {
         fetchRows(blockStart, FETCH_SIZE);
      }
   }

   // Prefetch ahead
   var aheadStart = Math.floor(newEnd / FETCH_SIZE) * FETCH_SIZE + FETCH_SIZE;
   if (aheadStart < activeRows && !blockIsCurrent(aheadStart)) {
      fetchRows(aheadStart, FETCH_SIZE);
   }

   // Recompute pinned offsets for data cells
   cachedPinnedOffsets = getPinnedOffsets().offsets;

   // Spacer rows span exactly the windowed cell count (pinned + spacers +
   // window), not the full column count -- see renderedColumnCount.
   var colSpan = renderedColumnCount();

   // --- Full rebuild (force, or no existing spacers) ---
   if (forceRebuild || !topSpacerRow || !bottomSpacerRow) {
      renderedRowElements.clear();

      // Build into a fragment so the tbody only takes a single layout hit
      // for the whole rebuild rather than one per row.
      var fragment = document.createDocumentFragment();
      topSpacerRow = createSpacerRow(colSpan);
      fragment.appendChild(topSpacerRow);

      // Render only the contiguous prefix from newStart. If a row is missing
      // from cache (out-of-order fetch), stop here -- the unrendered tail
      // becomes part of the bottom spacer so cached rows downstream of the
      // gap don't get displayed at the wrong vertical position.
      var lastRendered = newStart - 1;
      for (var r = newStart; r <= newEnd; r++) {
         var tr = buildRow(r);
         if (!tr) break;
         fragment.appendChild(tr);
         renderedRowElements.set(r, tr);
         lastRendered = r;
      }

      bottomSpacerRow = createSpacerRow(colSpan);
      fragment.appendChild(bottomSpacerRow);

      tbody.innerHTML = "";
      tbody.appendChild(fragment);

      updateSpacerRowHeight(topSpacerRow, newStart * ROW_HEIGHT);
      updateSpacerRowHeight(bottomSpacerRow,
         Math.max(0, activeRows - lastRendered - 1) * ROW_HEIGHT);

      renderStart = newStart;
      renderEnd = newEnd;
      return;
   }

   // --- Incremental update ---

   // Remove rows that are no longer in the window
   // Scrolling down: remove from top (renderStart .. newStart-1)
   // Scrolling up: remove from bottom (newEnd+1 .. renderEnd)
   for (var r = renderStart; r < newStart; r++) {
      var el = renderedRowElements.get(r);
      if (el) { el.remove(); renderedRowElements.delete(r); }
   }
   for (var r = newEnd + 1; r <= renderEnd; r++) {
      var el = renderedRowElements.get(r);
      if (el) { el.remove(); renderedRowElements.delete(r); }
   }

   // Add new rows at the top (newStart .. renderStart-1)
   // Insert before the first data row (right after topSpacerRow). If a row
   // is missing from cache, bail out and re-enter via the full-rebuild
   // path so the contiguous-prefix logic can position rows correctly --
   // skipping rows here would leave subsequent ones at the wrong offset.
   var missingRow = false;
   var insertBeforeTop = topSpacerRow.nextSibling;
   for (var r = Math.min(renderStart - 1, newEnd); r >= newStart; r--) {
      if (!renderedRowElements.has(r)) {
         var tr = buildRow(r);
         if (!tr) { missingRow = true; break; }
         tbody.insertBefore(tr, insertBeforeTop);
         renderedRowElements.set(r, tr);
         insertBeforeTop = tr;
      }
   }

   // Add new rows at the bottom (renderEnd+1 .. newEnd), insert before the
   // bottom spacer. Same fall-back rule as the top edge.
   if (!missingRow) {
      for (var r = Math.max(renderEnd + 1, newStart); r <= newEnd; r++) {
         if (!renderedRowElements.has(r)) {
            var tr = buildRow(r);
            if (!tr) { missingRow = true; break; }
            tbody.insertBefore(tr, bottomSpacerRow);
            renderedRowElements.set(r, tr);
         }
      }
   }

   if (missingRow) {
      // Drop the spacers so the next pass takes the full-rebuild branch,
      // which handles missing rows via the contiguous-prefix rule.
      topSpacerRow = null;
      bottomSpacerRow = null;
      renderVisibleRows(true);
      return;
   }

   // Update spacer heights
   updateSpacerRowHeight(topSpacerRow, newStart * ROW_HEIGHT);
   updateSpacerRowHeight(bottomSpacerRow, (activeRows - newEnd - 1) * ROW_HEIGHT);

   renderStart = newStart;
   renderEnd = newEnd;
};

// Info bar update is debounced separately at a longer interval -- reading
// scrollTop for the "Showing X to Y" text forces style+layout recalc,
// which is expensive during fast scrolling.
var debouncedInfoBar = debounce(TIMING.infoBarDebounce, updateInfoBar);

// RAF-throttle scroll (via pendingScrollRaf): render at most once per
// animation frame so virtual scroll updates happen during the scroll,
// not 16ms after it stops.
var onScroll = function() {
   if (pendingScrollRaf) return;
   pendingScrollRaf = requestAnimationFrame(function() {
      pendingScrollRaf = 0;
      var viewport = document.getElementById("gridViewport");
      if (!viewport) return;
      lastScrollTop = viewport.scrollTop;
      lastScrollLeft = viewport.scrollLeft;
      // Horizontal scroll may slide the column window; when it does, rebuild
      // the header for the new window and force a full row rebuild (existing
      // rows carry the old window's cells). Vertical-only scroll keeps the
      // cheap incremental row path.
      var colsChanged = syncColumnWindow();
      renderVisibleRows(colsChanged);
      debouncedInfoBar();

      // Horizontal scroll may have moved the viewport near (or past) the
      // fetched window's edge; recenter the window on it when so.
      maybeSlideForScroll();
   });
};
onScroll.cancel = function() {
   if (pendingScrollRaf) {
      cancelAnimationFrame(pendingScrollRaf);
      pendingScrollRaf = 0;
   }
};

// Update custom scrollbar thumb position; coalesce to one per frame.
var onScrollbarUpdate = function() {
   showScrollbars();
   if (!pendingScrollbarRaf) {
      pendingScrollbarRaf = requestAnimationFrame(function() {
         pendingScrollbarRaf = 0;
         updateCustomScrollbars();
      });
   }
};

// Force a final re-render when scrolling stops -- but not during custom
// scrollbar drags, which generate spurious scrollend events.
var onScrollEnd = function() {
   if (anyScrollbarDragging()) return;
   onScroll.cancel();
   var viewport = document.getElementById("gridViewport");
   if (!viewport) return;
   lastScrollTop = viewport.scrollTop;
   lastScrollLeft = viewport.scrollLeft;
   var colsChanged = syncColumnWindow();
   renderVisibleRows(colsChanged);
   updateInfoBar();
   updateCustomScrollbars();
   maybeSlideForScroll();

   // Persist the settled scroll position so it survives a close/reopen reload,
   // alongside pins/sort/filters. scrollend fires once per gesture (including
   // keyboard-driven scrolls), so this stays cheap.
   saveState();
};

// Window resize handler: debounce wrapper is created once here so its
// internal timer state survives across bootstraps and addEventListener
// stays idempotent.
var onResize = debounce(TIMING.resizeDebounce, function() {
   applyPinnedColumns();
   renderVisibleRows(true);
   updateInfoBar();
   // A resize changes the panel's clientHeight (and thus how many entries fit
   // and the overscroll tail), so rebuild the sidebar's visible window.
   renderSidebarWindow(true);
   updateCustomScrollbars();

   // A resize can change whether (and how far) the grid scrolls horizontally
   // without any scroll event firing; surface the auto-hide scrollbars so the
   // affordance reflects the new layout. update() above leaves non-scrollable
   // axes hidden, so this only reveals bars that actually overflow.
   showScrollbars();
});

// ==========================================================================
// Sidebar
// ==========================================================================

// Render a column's histogram into the sidebar. Drawn to a single <canvas>
// rather than one <rect> per bin: across dozens of numeric columns the SVG
// approach put thousands of vector nodes on the page, and repainting them
// during grid scroll caused multi-second stalls with the summary panel open
// (#17806). A canvas is one raster element, far cheaper for the compositor.
//
// Two modes share the renderer: numeric histograms (breaks + counts) and
// categorical frequency bars (labels + counts, breaks null). Categorical
// bars draw in an alternate color with per-bar gaps, cueing that the x-axis
// is a set of discrete values rather than a continuous range.
var createSparkline = function(breaks, counts, labels, breakLabels) {
   if (!counts || counts.length === 0) return null;
   if (!breaks && !labels) return null;

   var max = 0;
   var total = 0;
   for (var i = 0; i < counts.length; i++) {
      if (counts[i] > max) max = counts[i];
      total += counts[i];
   }
   if (max === 0) return null;

   // Wrapper div for positioning the tooltip
   var wrapper = document.createElement("div");
   wrapper.className = "sparkline-wrapper";

   // Fixed intrinsic resolution; CSS stretches the canvas to the sidebar
   // width (width: 100%). Decoupling from the live container width means
   // rendering doesn't depend on layout being settled (e.g. mid-transition
   // when the panel is opening). Scale the backing store by devicePixelRatio
   // so bars stay crisp on HiDPI displays.
   var LOGICAL_W = 120;
   var LOGICAL_H = 24;
   var dpr = window.devicePixelRatio || 1;
   var canvas = document.createElement("canvas");
   canvas.width = Math.max(1, Math.round(LOGICAL_W * dpr));
   canvas.height = Math.max(1, Math.round(LOGICAL_H * dpr));

   // A 2D context can be unavailable (context loss, headless/sandboxed, or
   // exhausted canvas memory). Bail out entirely rather than return a wrapper
   // around an un-drawable canvas, which would render as an empty box that
   // still shows a hover tooltip. Mirrors the max === 0 early return above.
   var ctx = canvas.getContext("2d");
   if (!ctx) return null;

   // Resolve the bar color from CSS so theming still applies; a canvas
   // can't pick up the old .sparkline-bar rule on its own. Categorical bars
   // (labels), date/time histograms (numeric breaks + breakLabels), and plain
   // numeric histograms each get a distinct hue.
   var styles = getComputedStyle(document.documentElement);
   var barColor;
   if (labels)
      barColor = styles.getPropertyValue("--grid-spark-categorical").trim() || "#2ba36b";
   else if (breakLabels)
      barColor = styles.getPropertyValue("--grid-spark-date").trim() || "#9575cd";
   else
      barColor = styles.getPropertyValue("--grid-spark-numeric").trim() || "#4d9de0";

   // Histogram bins tile seamlessly (contiguous ranges); categorical bars
   // get a small gap so discrete values read as separate bars.
   var barGap = labels ? Math.max(1, Math.round(dpr)) : 0;

   // Redraw all bars, drawing the hovered bar (if any) at full opacity to
   // mimic the per-bar :hover highlight the SVG version had. Cheap to call
   // on every hover change -- one short fill loop (one fillRect per bin).
   var hoverBin = -1;
   var drawBars = function(highlightBin) {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      ctx.fillStyle = barColor;
      for (var j = 0; j < counts.length; j++) {
         var h = (counts[j] / max) * canvas.height;
         // Snap bar edges to device pixels so adjacent bars tile without
         // seams or sub-pixel gaps.
         var x0 = Math.round((j / counts.length) * canvas.width);
         var x1 = Math.round(((j + 1) / counts.length) * canvas.width);
         ctx.globalAlpha = (j === highlightBin) ? 1 : 0.6;
         ctx.fillRect(x0, canvas.height - h, Math.max(1, x1 - x0 - barGap), h);
      }
   };
   drawBars(hoverBin);

   wrapper.appendChild(canvas);

   // Tooltip on hover
   var tooltip = document.createElement("div");
   tooltip.className = "sparkline-tooltip";
   tooltip.style.display = "none";
   wrapper.appendChild(tooltip);

   canvas.addEventListener("mousemove", function(evt) {
      // The canvas has no per-bar nodes, so derive the hovered bin from the
      // cursor's horizontal position within the displayed canvas.
      var rect = canvas.getBoundingClientRect();
      var bin = rect.width === 0 ? -1 :
         Math.floor((evt.clientX - rect.left) / rect.width * counts.length);
      if (bin < 0 || bin >= counts.length) {
         tooltip.style.display = "none";
         if (hoverBin !== -1) { hoverBin = -1; drawBars(hoverBin); }
         return;
      }

      // Repaint with the hovered bar highlighted when the bin changes.
      if (bin !== hoverBin) { hoverBin = bin; drawBars(hoverBin); }

      var count = counts[bin];
      var pct = total > 0 ? ((count / total) * 100).toFixed(1) : "0";

      var headlineLines;
      if (labels) {
         // Categorical bar: the headline is the value itself, truncated so
         // a long string can't blow the tooltip out to absurd widths. An
         // explicit NA factor level arrives as JSON null; label it "NA"
         // rather than rendering the literal string "null".
         var lbl = labels[bin] === null ? "NA" : String(labels[bin]);
         if (lbl.length > 40)
            lbl = lbl.substring(0, 40) + "...";
         headlineLines = [lbl];
      } else if (breakLabels && breakLabels.length > bin + 1) {
         // Date/datetime histogram: the numeric breaks are epoch values. Show
         // the formatted bin bounds on their own Start/End lines -- a single
         // "Range: lo to hi" line gets over-long once times are included.
         headlineLines = [
            "Start: " + breakLabels[bin],
            "End: " + breakLabels[bin + 1]
         ];
      } else {
         // breaks arrive from R as strings (col_breaks is as.character'd
         // server-side); coerce here so arithmetic doesn't fall into string
         // concatenation via the `+` operator.
         var lo = Number(breaks[bin]);
         var hi = Number(breaks[bin + 1]);

         // For integer-binned histograms (server emits breaks like
         // 0.5, 1.5, 2.5, ... so each bin holds one integer value), show the
         // integer rather than the awkward "Range: 0.5 to 1.5".
         var mid = (lo + hi) / 2;
         var isIntegerBin = (hi - lo) === 1 && Number.isInteger(mid);
         headlineLines = [isIntegerBin
            ? "Value: " + mid
            : "Range: " + formatCompactNum(lo) + " to " + formatCompactNum(hi)];
      }

      // Build with DOM nodes rather than innerHTML so future format changes
      // can't accidentally interpret data values as markup.
      tooltip.textContent = "";
      for (var li = 0; li < headlineLines.length; li++) {
         tooltip.appendChild(document.createTextNode(headlineLines[li]));
         tooltip.appendChild(document.createElement("br"));
      }
      tooltip.appendChild(document.createTextNode(
         "Count: " + count.toLocaleString() + " (" + pct + "%)"));
      tooltip.style.display = "";

      // Keep a wide tooltip (date ranges especially) from being clipped at the
      // viewport's right edge: anchored at the wrapper's left by default, but
      // nudged left by however much it overflows. Reset first so each bin
      // re-measures from the default anchor.
      tooltip.style.left = "0px";
      var docWidth = document.documentElement.clientWidth;
      var overflowRight = tooltip.getBoundingClientRect().right - (docWidth - 4);
      if (overflowRight > 0)
         tooltip.style.left = (-overflowRight) + "px";
   });

   canvas.addEventListener("mouseleave", function() {
      tooltip.style.display = "none";
      if (hoverBin !== -1) { hoverBin = -1; drawBars(hoverBin); }
   });

   return wrapper;
};

// The most-specific R class of a column (its class()[1]), used for the
// human-readable type label. Falls back to the typeof() if class is absent.
var colClassLabel = function(col) {
   if (col.col_class && col.col_class.length > 0)
      return col.col_class[0];
   return col.col_type || "";
};

var typeLabel = function(col) {
   var type = colClassLabel(col);
   // Map common R classes to short labels
   var map = {
      "character": "chr",
      "complex": "cpl",
      "Date": "date",
      "double": "dbl",
      "factor": "fct",
      "integer": "int",
      "logical": "lgl",
      "numeric": "dbl",
      "ordered": "ord",
      "POSIXct": "dttm",
      "POSIXlt": "dttm",
      "raw": "raw"
   };
   return map[type] || type;
};

// Compact formatter for axis-style values (sparkline bin bounds, the
// footer's min/max range). Integer values (including 0) render without a
// fractional part: "0" rather than "0.00", "5" rather than "5.0".
var formatCompactNum = function(n) {
   if (Number.isInteger(n)) return n.toLocaleString();
   if (Math.abs(n) >= 100) return Math.round(n).toLocaleString();
   if (Math.abs(n) >= 1) return n.toFixed(1);
   if (Math.abs(n) >= 0.01) return n.toFixed(2);
   return n.toPrecision(3);
};

// Append one of the range interval's punctuation adornments ("[", ", ",
// "]") to the footer summary as a dimmed span, visually separating the
// notation from the numbers themselves (whose locale formatting may
// include comma thousands separators).
var appendRangePunct = function(el, text) {
   var punct = document.createElement("span");
   punct.className = "range-punct";
   punct.textContent = text;
   el.appendChild(punct);
};

// Render a Date/POSIXct min/max range into the sidebar footer, compactly:
//   - Date columns (labels carry no time part): "[min, max]".
//   - POSIXct spanning multiple days: dates only, "[minDate, maxDate]".
//   - POSIXct within a single day: the date once, then the times,
//     "<date> [minTime, maxTime]".
// When time precision is dropped, the full labels go in a title tooltip.
// Labels are R's format() output ("YYYY-MM-DD" or "YYYY-MM-DD HH:MM:SS"), so the
// first space separates the date from the time.
var appendDateRangeSummary = function(el, minLabel, maxLabel) {
   var minSp = minLabel.indexOf(" ");
   var maxSp = maxLabel.indexOf(" ");

   if (minSp < 0 || maxSp < 0) {
      appendRangePunct(el, "[");
      el.appendChild(document.createTextNode(minLabel));
      appendRangePunct(el, ", ");
      el.appendChild(document.createTextNode(maxLabel));
      appendRangePunct(el, "]");
      return;
   }

   var minDate = minLabel.substring(0, minSp), minTime = minLabel.substring(minSp + 1);
   var maxDate = maxLabel.substring(0, maxSp), maxTime = maxLabel.substring(maxSp + 1);
   el.title = minLabel + " to " + maxLabel;

   if (minDate === maxDate) {
      el.appendChild(document.createTextNode(minDate + " "));
      appendRangePunct(el, "[");
      el.appendChild(document.createTextNode(minTime));
      appendRangePunct(el, ", ");
      el.appendChild(document.createTextNode(maxTime));
      appendRangePunct(el, "]");
   } else {
      appendRangePunct(el, "[");
      el.appendChild(document.createTextNode(minDate));
      appendRangePunct(el, ", ");
      el.appendChild(document.createTextNode(maxDate));
      appendRangePunct(el, "]");
   }
};

var formatStatValue = function(val) {
   if (val === null || val === undefined) return "--";
   if (typeof val === "number") {
      if (Number.isInteger(val)) return val.toLocaleString();
      if (Math.abs(val) >= 100) return val.toFixed(1);
      if (Math.abs(val) >= 1) return val.toFixed(2);
      return val.toPrecision(3);
   }
   return String(val);
};

var renderColumnStats = function(container, data, colType) {
   container.innerHTML = "";

   if (!data || data.error) {
      container.textContent = data ? data.error : "Error loading summary";
      return;
   }

   var table = document.createElement("table");
   table.className = "sidebar-stats-table";

   var addRow = function(label, value) {
      var tr = document.createElement("tr");
      var tdLabel = document.createElement("td");
      tdLabel.className = "stats-label";
      tdLabel.textContent = label;
      var tdValue = document.createElement("td");
      tdValue.className = "stats-value";
      tdValue.textContent = formatStatValue(value);
      tr.appendChild(tdLabel);
      tr.appendChild(tdValue);
      table.appendChild(tr);
   };

   // Type-specific stats
   if (colType === "numeric" && data.min !== undefined) {
      addRow("Unique", data.n_unique);
      addRow("Min", data.min);
      addRow("Max", data.max);
      addRow("Mean", data.mean);
      addRow("Median", data.median);
      addRow("SD", data.sd);
   } else if (colType === "character") {
      addRow("Unique", data.n_unique);
      if (data.min_length !== undefined) {
         addRow("Min length", data.min_length);
         addRow("Max length", data.max_length);
      }
      if (data.n_empty !== undefined) addRow("Empty", data.n_empty);
   } else if (colType === "factor" && data.top_levels) {
      for (var i = 0; i < data.top_levels.length; i++) {
         addRow(data.top_levels[i], data.top_counts[i]);
      }
   } else if (colType === "boolean") {
      addRow("TRUE", data.n_true);
      addRow("FALSE", data.n_false);
   } else if (data.min !== undefined) {
      // Date/datetime
      addRow("Min", data.min);
      addRow("Max", data.max);
      // Timezone (POSIXct only; absent for Date and tz-less columns).
      if (data.tz) addRow("Timezone", data.tz);
   }

   // If no type-specific stats were added (unsupported column type, or a
   // column whose only values are NA), surface that explicitly rather
   // than leaving the panel blank.
   if (!table.firstChild) {
      var msg = document.createElement("div");
      msg.className = "stats-empty";
      msg.textContent = (data.n_na === data.n && data.n > 0)
         ? "All values are missing."
         : "No summary available for this column.";
      container.appendChild(msg);
      return;
   }

   container.appendChild(table);
};

// Sidebar toggle handlers, module-scoped so re-registration in initSidebar
// is idempotent (addEventListener dedupes on the same function reference).
// #sidebarToggle is a static element that survives re-bootstraps; inline
// closures here would stack one listener per bootstrap, so a single click
// would toggle the sidebar multiple times after a data refresh.
var onSidebarToggleActivate = function(evt) {
   evt.preventDefault();
   toggleSidebar();
};

var onSidebarToggleKeyDown = function(evt) {
   if (evt.key === "Enter" || evt.key === " ") onSidebarToggleActivate(evt);
};

// The columns the sidebar lists: the complete index once loaded, otherwise
// the fetched grid window (identity only) as a fast first paint. Both yield
// items carrying col_name/col_type/col_class/col_index, so entry construction
// is uniform. Rownames (col 0 of the grid window) is excluded.
var sidebarColumnList = function() {
   if (sidebarColumns)
      return sidebarColumns;
   var list = [];
   if (cols) {
      for (var i = 1; i < cols.length; i++)
         list.push(cols[i]);
   }
   return list;
};

// Fetch the whole frame's lightweight column index once, then rebuild the
// sidebar so it lists every column (not just the fetched window). Cheap
// enough for very wide frames; the per-column summaries stay lazy.
var ensureSidebarColumns = function() {
   if (sidebarColumns || sidebarColumnsFetching || dataMode !== "server")
      return;
   sidebarColumnsFetching = true;
   var loc = parseLocationUrl();
   var params = {
      env: loc.env,
      obj: loc.obj,
      cache_key: loc.cacheKey,
      show: "column_index"
   };
   gridDataFetch(buildFormData(params))
      .then(function(result) {
         sidebarColumnsFetching = false;
         if (result && !result.error && result.columns && result.columns.length) {
            sidebarColumns = result.columns;
            rebuildSidebarPreservingScroll();
         }
      })
      .catch(function(err) {
         sidebarColumnsFetching = false;
         console.warn("fetchColumnIndex failed:", err);
      });
};

// The summary descriptor for a column (absolute index) matching the current
// filter state, or null if not yet loaded. Window columns resolve from the
// grid metadata (filteredSummaries when filtered, else `cols`); off-window
// columns from the lazy cache.
var getSidebarSummary = function(absIdx) {
   if (filteredSummaries) {
      if (filteredSummaries[absIdx])
         return filteredSummaries[absIdx];
   } else {
      var pos = posForAbsColIndex(absIdx);
      if (pos >= 0)
         return cols[pos];
   }
   return sidebarLazySummaries[absIdx] || null;
};

// Percentage denominator for the sidebar summaries: the filtered row count
// when a filter is active, otherwise the whole-frame row count (from the
// metadata, not the transiently-zeroed totalRows -- see initSidebar).
var sidebarSummaryRowCount = function() {
   if (filteredSummaries)
      return filteredSummariesRowCount;
   return (cols && cols[0] && typeof cols[0].total_rows === "number" &&
           cols[0].total_rows > 0)
      ? cols[0].total_rows
      : totalRows;
};

// Fetch summary descriptors for a set of absolute column indices (matching
// the current filter state) and stash them in the lazy cache, then invoke
// the callback. Reuses show=cols (the same describe the grid window uses).
var fetchSidebarSummaries = function(absList, callback) {
   if (absList.length === 0) { callback(); return; }
   var loc = parseLocationUrl();
   var params = {
      env: loc.env,
      obj: loc.obj,
      cache_key: loc.cacheKey,
      show: "cols",
      max_rows: maxRows,
      columns_requested: absList.join(",")
   };
   if (hasActiveRowFilter()) {
      params["filtered"] = 1;
      appendTransformParams(params);
   }
   gridDataFetch(buildFormData(params))
      .then(function(result) {
         if (result && !result.error && result.length) {
            prepareColumnResponse(result);
            for (var i = 0; i < result.length; i++) {
               var entry = result[i];
               if (typeof entry.col_index === "number" && entry.col_index >= 1)
                  sidebarLazySummaries[entry.col_index] = entry;
            }
         }
         callback();
      })
      .catch(function(err) {
         console.warn("fetchSidebarSummaries failed:", err);
         callback();
      });
};

// Fetch a single column's FULL-FRAME describe descriptor (no filter applied)
// for its sidebar filter popup, caching it in filterDescriptors. Unlike the
// lazy sidebar summaries this never sends the active filter, so the brush spans
// the column's whole range.
var fetchFilterDescriptor = function(absIdx, callback) {
   var loc = parseLocationUrl();
   var params = {
      env: loc.env,
      obj: loc.obj,
      cache_key: loc.cacheKey,
      show: "cols",
      max_rows: maxRows,
      columns_requested: String(absIdx)
   };
   gridDataFetch(buildFormData(params))
      .then(function(result) {
         if (result && !result.error && result.length) {
            prepareColumnResponse(result);
            for (var i = 0; i < result.length; i++) {
               var entry = result[i];
               if (typeof entry.col_index === "number" && entry.col_index >= 1)
                  filterDescriptors[entry.col_index] = entry;
            }
         }
         callback(filterDescriptors[absIdx] || null);
      })
      .catch(function(err) {
         console.warn("fetchFilterDescriptor failed:", err);
         callback(null);
      });
};

// Resolve a column's full-frame filter descriptor: from the fetched grid window
// (`cols`, which always describes the full frame) when present, else the cache,
// else a fetch. Invokes callback with the descriptor (or null).
var resolveFilterDescriptor = function(absIdx, callback) {
   var pos = posForAbsColIndex(absIdx);
   if (pos >= 0 && cols[pos]) {
      callback(cols[pos]);
      return;
   }
   if (filterDescriptors[absIdx]) {
      callback(filterDescriptors[absIdx]);
      return;
   }
   fetchFilterDescriptor(absIdx, callback);
};

// Open a column's filter popup from its sidebar icon. The first activation
// resolves the descriptor and builds the typed widget anchored to the icon
// (binding the icon's click to the popup via invokeFilterPopup); once wired,
// subsequent clicks toggle through that bound handler, so this is a no-op then.
var openColumnFilterFromSidebar = function(icon, absIdx) {
   // Already wired: toggle through the widget's own popup controller.
   if (icon.dvFilterController) {
      icon.dvFilterController.open();
      return;
   }
   // A descriptor fetch from a prior click is still in flight.
   if (icon.dvFilterWiring)
      return;

   icon.dvFilterWiring = true;
   resolveFilterDescriptor(absIdx, function(desc) {
      icon.dvFilterWiring = false;

      // A sidebar rebuild / resetGridState may have detached this icon while
      // the descriptor fetch was in flight; anchoring a popup to an off-DOM
      // node would position it at (0,0). Bail rather than open a stray popup.
      if (!icon.isConnected)
         return;
      if (!desc || !isFilterableSearchType(desc.col_search_type))
         return;

      var widget = buildTypedFilterUI(absIdx, desc, function() {
         // On dismiss, re-sync the icon's active state with the filter store.
         updateSidebarColumnIndicators();
      }, icon);

      var ctrl = widget && widget.dvFilterController;
      if (!ctrl)
         return;

      icon.dvFilterController = ctrl;
      ctrl.open();
   });
};

// Debounced flush of the IntersectionObserver's queued abs indices: fetch the
// ones still missing a summary, then populate their (still-present) entries.
var flushSidebarPendingFetch = debounce(120, function() {
   var content = document.getElementById("sidebarContent");
   if (!content) { sidebarPendingFetch = {}; return; }

   var want = [];
   var populatedAny = false;
   for (var key in sidebarPendingFetch) {
      if (!sidebarPendingFetch.hasOwnProperty(key)) continue;
      var abs = parseInt(key, 10);
      var have = getSidebarSummary(abs);
      if (have) {
         // Summary already on hand -- typically a column that slid into the
         // fetched window after its (shell) entry was built. Populate it
         // directly rather than refetching; this is the in-place equivalent of
         // the re-seed the full sidebar rebuild used to do on every slide.
         var readyEntry = content.querySelector(
            '.sidebar-col[data-col-idx="' + abs + '"]');
         if (readyEntry) {
            populateEntrySummary(readyEntry, have);
            populatedAny = true;
         }
      } else {
         want.push(abs);
      }
   }
   sidebarPendingFetch = {};

   if (populatedAny) {
      if (sidebarVisible) renderPendingSparklines();
      if (sidebarScrollbar_) sidebarScrollbar_.update();
   }

   if (want.length === 0)
      return;

   var token = drawCounter;
   fetchSidebarSummaries(want, function() {
      // A filter/search/refresh since we issued the fetch invalidated the
      // cache keying; the rebuild it triggered will re-observe and refetch.
      if (token !== drawCounter)
         return;
      var liveContent = document.getElementById("sidebarContent");
      if (!liveContent)
         return;
      for (var i = 0; i < want.length; i++) {
         var summary = getSidebarSummary(want[i]);
         if (!summary)
            continue;
         var entry = liveContent.querySelector(
            '.sidebar-col[data-col-idx="' + want[i] + '"]');
         if (entry)
            populateEntrySummary(entry, summary);
      }
      // The entries just populated registered sparklines; draw them (the
      // panel is open, since the observer only fires for visible entries).
      if (sidebarVisible) renderPendingSparklines();
      if (sidebarScrollbar_) sidebarScrollbar_.update();
   });
});

// Fill an entry's summary content (sparkline + footer range/NA/unique) from a
// describe descriptor. Idempotent: skips entries already populated (so a
// double IntersectionObserver fire or a seed+observe race can't duplicate the
// sparkline). The column summary is supplied directly via the summary parameter.
var populateEntrySummary = function(entry, summary) {
   if (entry.getAttribute("data-summary-loaded") === "1")
      return;
   entry.setAttribute("data-summary-loaded", "1");

   var rowCount = sidebarSummaryRowCount();
   var footer = entry.querySelector(".sidebar-col-footer");

   // Sparkline (numeric histogram or categorical bars) drawn into the entry's
   // reserved fixed-height slot (kept empty for columns without one, so the
   // entry stays the constant height the virtualizer assumes). Deferred-drawn
   // via pendingSparklines_ + renderPendingSparklines.
   var sparkSlot = entry.querySelector(".sidebar-sparkline");
   if (sparkSlot && hasHistogram(summary)) {
      pendingSparklines_.push({
         container: sparkSlot,
         breaks: summary.col_breaks,
         counts: summary.col_counts,
         labels: null,
         breakLabels: summary.col_break_labels || null
      });
   } else if (sparkSlot && hasCategoryCounts(summary)) {
      pendingSparklines_.push({
         container: sparkSlot,
         breaks: null,
         counts: summary.col_cat_counts,
         labels: summary.col_cat_vals
      });
   }

   // Footer: type-specific summary (left) + NA stat (right).
   var summaryEl = footer.querySelector(".sidebar-col-summary");
   if (hasHistogram(summary) &&
       typeof summary.col_min === "number" &&
       typeof summary.col_max === "number") {
      summaryEl.classList.add("range");
      appendRangePunct(summaryEl, "[");
      summaryEl.appendChild(document.createTextNode(formatCompactNum(summary.col_min)));
      appendRangePunct(summaryEl, ", ");
      summaryEl.appendChild(document.createTextNode(formatCompactNum(summary.col_max)));
      appendRangePunct(summaryEl, "]");
   } else if (summary.col_min_label && summary.col_max_label) {
      // Date/datetime range: the min/max arrive pre-formatted (col_min/col_max
      // are deliberately omitted for dates so the numeric branch above is
      // skipped). Rendered compactly -- see appendDateRangeSummary.
      summaryEl.classList.add("range");
      appendDateRangeSummary(summaryEl, summary.col_min_label, summary.col_max_label);
   } else {
      // Factor level count is structural (read from the summary's col_vals);
      // distinct-value count is data-dependent. Enrich with the dominant
      // value when there's no category sparkline and "top" isn't every-row.
      var catText = "";
      if (isFactorColumn(summary) && summary.col_vals) {
         catText = summary.col_vals.length.toLocaleString() + " levels";
      } else if (typeof summary.col_n_unique === "number") {
         catText = summary.col_n_unique.toLocaleString() + " unique";
      }
      if (catText &&
          typeof summary.col_top_count === "number" &&
          summary.col_top_count > 1 &&
          rowCount > 0) {
         var topPct = (summary.col_top_count / rowCount) * 100;
         catText += " · top: " + summary.col_top_value +
            " (" + (topPct < 1 ? "<1" : Math.round(topPct)) + "%)";
      }
      summaryEl.textContent = catText;
      if (catText)
         summaryEl.title = catText;
   }

   var naEl = footer.querySelector(".sidebar-col-na");
   var naCount = summary.col_na_count || 0;
   if (naCount > 0 && rowCount > 0) {
      var naPct = (naCount / rowCount) * 100;
      naEl.textContent = (naPct < 1 ? "<1" : Math.round(naPct)) + "% NA";
      naEl.title = naCount.toLocaleString() + " missing values";
      naEl.classList.remove("zero");
   } else {
      naEl.textContent = "0% NA";
      naEl.classList.add("zero");
      naEl.title = "No missing values";
   }
};

// Apply the current pin/sort/filter indicator state to a single built entry.
// Shared by buildSidebarEntry (at build time) and updateSidebarColumnIndicators
// (when state changes), keyed off the entry's absolute data-col-idx.
var applySidebarEntryIndicators = function(entry) {
   var absIdx = parseInt(entry.getAttribute("data-col-idx"), 10);
   if (isNaN(absIdx)) return;
   var nameEl = entry.querySelector(".sidebar-col-name");
   var colName = nameEl ? nameEl.textContent : "";

   var pinEl = entry.querySelector(".sidebar-pin-icon");
   if (pinEl) {
      var pinned = pinnedColumns.has(absIdx);
      pinEl.classList.toggle("pinned", pinned);
      pinEl.title = pinned ? "Unpin column" : "Pin column";
      pinEl.setAttribute("aria-pressed", pinned ? "true" : "false");
      pinEl.setAttribute("aria-label",
         (pinned ? "Unpin column " : "Pin column ") + colName);
   }

   var sortEl = entry.querySelector(".sidebar-sort-icon");
   if (sortEl && !sortEl.classList.contains("disabled")) {
      var dir = (absIdx === sortColumn) ? sortDirection : "";
      sortEl.classList.toggle("sorting_asc", dir === "asc");
      sortEl.classList.toggle("sorting_desc", dir === "desc");
      var action;
      if (dir === "asc") {
         action = "Sort column " + colName + " descending";
      } else if (dir === "desc") {
         action = "Remove sort on column " + colName;
      } else {
         action = "Sort column " + colName + " ascending";
      }
      sortEl.title = action;
      sortEl.setAttribute("aria-label", action);
   }

   var filterEl = entry.querySelector(".sidebar-filter-icon");
   if (filterEl) {
      var filterRaw = cachedFilterValues[absIdx];
      var filtered = !!filterRaw;
      filterEl.classList.toggle("active", filtered);
      if (filtered) {
         var desc = describeFilterValue(filterRaw);
         filterEl.title = desc ? "Filtered: " + desc : "Filtered";
         filterEl.setAttribute("aria-label",
            "Edit filter for column " + colName + (desc ? " (" + desc + ")" : ""));
      } else {
         filterEl.title = "Filter column";
         filterEl.setAttribute("aria-label", "Filter column " + colName);
      }
   }
};

// Build one sidebar entry's DOM. No per-entry listeners -- those are delegated
// to the content container. Identity (name, type, pin/sort/filter icons) is
// always present; the summary (sparkline + range/NA) is seeded from cache if
// available, else queued for the debounced lazy fetch. The sparkline lives in a
// fixed-height slot reserved up front so every entry is the constant
// --sidebar-entry-height the virtualizer assumes.
var buildSidebarEntry = function(col, absIdx, virtIndex) {
   var entry = document.createElement("div");
   entry.className = "sidebar-col";
   // data-col-idx is the ABSOLUTE column index (1-based); the delegated handlers
   // and summary loader key off it (it identifies the column across the whole
   // frame, not its position in the fetched window).
   entry.setAttribute("data-col-idx", absIdx);
   entry.setAttribute("role", "listitem");
   // The list is virtualized, so only the visible entries are in the DOM;
   // setsize/posinset tell assistive tech the true list length and this entry's
   // place in it rather than just "N of <visible-count>".
   entry.setAttribute("aria-setsize", sidebarListCols.length);
   entry.setAttribute("aria-posinset", virtIndex + 1);
   // Stash the stats category so the delegated expand handler can pass it to the
   // stats popover without re-deriving it from `col`.
   entry.setAttribute("data-stats-cat", statsCategory(col));

   var header = document.createElement("div");
   header.className = "sidebar-col-header";
   header.setAttribute("role", "button");
   header.setAttribute("tabindex", "0");
   header.setAttribute("aria-label", "Scroll to column " + col.col_name);

   var expandBtn = document.createElement("span");
   expandBtn.className = "sidebar-expand-btn";
   expandBtn.title = "Show column summary";
   expandBtn.setAttribute("role", "button");
   expandBtn.setAttribute("tabindex", "0");
   expandBtn.setAttribute("aria-expanded", "false");
   expandBtn.setAttribute("aria-label", "Toggle summary for column " + col.col_name);
   header.appendChild(expandBtn);

   var pinIcon = document.createElement("span");
   pinIcon.className = "pin-icon sidebar-pin-icon";
   pinIcon.setAttribute("role", "button");
   pinIcon.setAttribute("tabindex", "0");
   header.appendChild(pinIcon);

   var name = document.createElement("span");
   name.className = "sidebar-col-name";
   name.textContent = col.col_name;
   name.title = col.col_name;
   header.appendChild(name);

   var type = document.createElement("span");
   type.className = "sidebar-col-type";
   type.textContent = "<" + typeLabel(col) + ">";
   if (col.col_tz)
      type.title = "Timezone: " + col.col_tz;
   header.appendChild(type);

   var filterIcon = document.createElement("span");
   filterIcon.className = "sidebar-filter-icon";
   filterIcon.setAttribute("role", "button");
   filterIcon.setAttribute("tabindex", "0");
   filterIcon.setAttribute("aria-label", "Filter column " + col.col_name);
   header.appendChild(filterIcon);

   // Sort icon at the right edge. Non-sortable columns (list / data.frame) keep
   // an inert disabled placeholder so the type labels stay aligned; when
   // ordering is disabled for the whole grid no icon is created.
   if (ordering) {
      var sortIcon = document.createElement("span");
      sortIcon.className = "sidebar-sort-icon";
      if (isColumnSortable(col)) {
         sortIcon.setAttribute("role", "button");
         sortIcon.setAttribute("tabindex", "0");
      } else {
         sortIcon.classList.add("disabled");
      }
      header.appendChild(sortIcon);
   }

   entry.appendChild(header);

   // Reserved sparkline slot (always present so the entry stays the constant
   // height, whether or not the column has -- or has loaded -- a sparkline).
   var spark = document.createElement("div");
   spark.className = "sidebar-sparkline";
   entry.appendChild(spark);

   var footer = document.createElement("div");
   footer.className = "sidebar-col-footer";
   var summaryEl = document.createElement("span");
   summaryEl.className = "sidebar-col-summary";
   footer.appendChild(summaryEl);
   var naEl = document.createElement("span");
   naEl.className = "sidebar-col-na";
   footer.appendChild(naEl);
   entry.appendChild(footer);

   applySidebarEntryIndicators(entry);

   var seeded = getSidebarSummary(absIdx);
   if (seeded)
      populateEntrySummary(entry, seeded);
   else
      sidebarPendingFetch[absIdx] = true;

   return entry;
};

// Fixed entry height the virtualizer's offset math (offset = index x H) relies
// on. Read once from the --sidebar-entry-height CSS custom property so the value
// has a single source of truth (the :root rule in DataViewer.css), rather than a
// JS literal that must be kept in sync with the CSS. Falls back to 78 if
// unavailable.
var sidebarEntryHeightCache = 0;
var sidebarEntryHeight = function() {
   if (!sidebarEntryHeightCache) {
      var v = parseInt(getComputedStyle(document.documentElement)
         .getPropertyValue("--sidebar-entry-height"), 10);
      sidebarEntryHeightCache = (v > 0) ? v : 78;
   }
   return sidebarEntryHeightCache;
};

// Build (only) the sidebar entries whose vertical band intersects the panel
// viewport, sizing the off-window range with the top/bottom spacers. The grid-
// row analogue is renderVisibleRows. Cheap: the one forced layout is reading the
// panel's own clientHeight, and at most ~visible+buffer entries are built.
var renderSidebarWindow = function(force) {
   var content = document.getElementById("sidebarContent");
   if (!content || !sidebarRenderMid) return;

   var n = sidebarListCols.length;
   var H = sidebarEntryHeight();
   var scrollTop = content.scrollTop;
   var clientH = content.clientHeight;

   var first, last;
   if (n === 0) {
      first = 0;
      last = -1;
   } else {
      first = Math.floor(scrollTop / H) - SIDEBAR_BUFFER_ENTRIES;
      last = Math.ceil((scrollTop + clientH) / H) + SIDEBAR_BUFFER_ENTRIES;
      if (first < 0) first = 0;
      if (last > n - 1) last = n - 1;
   }

   if (!force && first === sidebarWinStart && last === sidebarWinEnd)
      return;
   sidebarWinStart = first;
   sidebarWinEnd = last;

   // The stats popover is anchored to an entry we're about to destroy; close it
   // so it can't float detached over the rebuilt list. (Filter popups are NOT
   // dismissed here: a filter's own apply triggers a sidebar refresh/rebuild,
   // and the popup is expected to stay open through that -- it self-heals on
   // light-dismiss if its anchor later scrolls away.)
   if (sidebarStatsPopup) dismissSidebarStats();

   // Spacers stand in for the off-window entries; the bottom one adds an
   // overscroll tail so the last entry can be scrolled toward the top of the
   // panel rather than bottoming out at its lower edge. Only when the list
   // actually overflows -- otherwise a short list would gain phantom scroll
   // space past its end (matching the old overflow-only overscroll padding).
   var overscroll = (n * H > clientH) ? Math.max(0, clientH - H) : 0;
   var tail = (last < 0) ? n : (n - 1 - last);
   sidebarRenderTop.style.height = (first * H) + "px";
   sidebarRenderBottom.style.height = (Math.max(0, tail) * H + overscroll) + "px";

   // Drop sparklines queued for the entries we're about to destroy; the rebuilt
   // entries re-queue their own (otherwise renderPendingSparklines could draw
   // into a now-detached slot).
   pendingSparklines_ = [];

   sidebarRenderMid.innerHTML = "";
   if (last >= first) {
      var frag = document.createDocumentFragment();
      for (var i = first; i <= last; i++) {
         var col = sidebarListCols[i];
         var absIdx = (typeof col.col_index === "number") ? col.col_index : i + 1;
         frag.appendChild(buildSidebarEntry(col, absIdx, i));
      }
      sidebarRenderMid.appendChild(frag);
   }

   // Draw the seeded sparklines and fetch any summaries the new entries queued
   // -- but only while the panel is shown. A collapsed panel can still have a
   // non-zero clientHeight, so fetching here would eagerly pull a screenful of
   // summaries the user can't see; toggleSidebar/onActivate rebuild on open and
   // fetch then (matching the old IntersectionObserver, which never fired for a
   // hidden panel).
   if (sidebarVisible) {
      renderPendingSparklines();
      flushSidebarPendingFetch();
   }
   if (sidebarScrollbar_) sidebarScrollbar_.update();
};

// The virtual index (position in sidebarListCols) of an absolute column index,
// or -1 if it isn't listed. Used by flashSidebarColumn to scroll to a column
// whose entry may not currently be built.
var sidebarVirtIndexForAbs = function(absIdx) {
   var idx = sidebarIndexByAbs[absIdx];
   return (typeof idx === "number") ? idx : -1;
};

// --- Delegated sidebar entry interactions --------------------------------
// Built entries come and go as the window scrolls, so a single set of handlers
// lives on the persistent content container and dispatches by the closest
// affordance + the entry's absolute index.

var onSidebarContentClick = function(evt) {
   var t = evt.target;
   if (!t || !t.closest) return;
   var entry = t.closest(".sidebar-col");
   if (!entry || !sidebarRenderMid || !sidebarRenderMid.contains(entry)) return;
   var absIdx = parseInt(entry.getAttribute("data-col-idx"), 10);
   if (isNaN(absIdx)) return;

   var expandEl = t.closest(".sidebar-expand-btn");
   if (expandEl) {
      evt.stopPropagation(); evt.preventDefault();
      openSidebarStats(expandEl, absIdx, entry.getAttribute("data-stats-cat"));
      return;
   }
   if (t.closest(".sidebar-pin-icon")) {
      evt.stopPropagation(); evt.preventDefault();
      togglePinColumn(absIdx);
      return;
   }
   var sortEl = t.closest(".sidebar-sort-icon");
   if (sortEl) {
      if (sortEl.classList.contains("disabled")) return;
      evt.stopPropagation(); evt.preventDefault();
      handleSortClick(absIdx);
      return;
   }
   var filterEl = t.closest(".sidebar-filter-icon");
   if (filterEl) {
      evt.stopPropagation(); evt.preventDefault();
      // The first activation builds the widget (which binds the icon's own
      // click via invokeFilterPopup); once bound, that handler toggles it, so
      // this path only kicks off the initial build.
      if (filterEl.dvFilterController) return;
      openColumnFilterFromSidebar(filterEl, absIdx);
      return;
   }
   // Anywhere else in the entry: navigate to the column.
   goToColumn(absIdx);
};

var onSidebarContentKeydown = function(evt) {
   if (evt.key !== "Enter" && evt.key !== " ") return;
   var t = evt.target;
   if (!t || !t.classList || !t.closest) return;
   var entry = t.closest(".sidebar-col");
   if (!entry || !sidebarRenderMid || !sidebarRenderMid.contains(entry)) return;
   var absIdx = parseInt(entry.getAttribute("data-col-idx"), 10);
   if (isNaN(absIdx)) return;

   if (t.classList.contains("sidebar-expand-btn")) {
      evt.preventDefault(); evt.stopPropagation();
      openSidebarStats(t, absIdx, entry.getAttribute("data-stats-cat"));
   } else if (t.classList.contains("sidebar-pin-icon")) {
      evt.preventDefault(); evt.stopPropagation();
      togglePinColumn(absIdx);
   } else if (t.classList.contains("sidebar-sort-icon")) {
      if (t.classList.contains("disabled")) return;
      evt.preventDefault(); evt.stopPropagation();
      handleSortClick(absIdx);
   } else if (t.classList.contains("sidebar-filter-icon")) {
      evt.preventDefault(); evt.stopPropagation();
      openColumnFilterFromSidebar(t, absIdx);
   } else if (t.classList.contains("sidebar-col-header")) {
      evt.preventDefault();
      goToColumn(absIdx);
   }
};

// --- Detailed-stats popover ----------------------------------------------
// Expanding a column's stats opens a floating popover anchored to its expand
// button rather than growing the entry inline -- which keeps every entry the
// constant height the virtualizer requires. One open at a time; it cooperates
// with the filter popups through dismissActivePopup.
var sidebarStatsPopup = null;   // the open popover element, or null
var sidebarStatsAbs = -1;       // absolute column index it is showing

var dismissSidebarStats = function() {
   if (!sidebarStatsPopup) return false;
   if (sidebarStatsPopup.parentNode)
      document.body.removeChild(sidebarStatsPopup);
   document.body.removeEventListener("click", sidebarStatsLightDismiss);
   document.body.removeEventListener("keydown", sidebarStatsEscDismiss);
   sidebarStatsPopup = null;
   sidebarStatsAbs = -1;
   if (dismissActivePopup === dismissSidebarStats)
      dismissActivePopup = null;
   // Clear the expanded state on whichever expand button is currently built
   // (the original anchor may have scrolled out of the window).
   var content = document.getElementById("sidebarContent");
   if (content) {
      var open = content.querySelectorAll(".sidebar-expand-btn.expanded");
      for (var i = 0; i < open.length; i++) {
         open[i].classList.remove("expanded");
         open[i].setAttribute("aria-expanded", "false");
      }
   }
   return true;
};

var sidebarStatsLightDismiss = function(evt) {
   if (sidebarStatsPopup && sidebarStatsPopup.contains(evt.target))
      return;
   dismissSidebarStats();
};

var sidebarStatsEscDismiss = function(evt) {
   if (evt.keyCode === 27) dismissSidebarStats();
};

// Position a body-appended popover just under `anchorEl`, centered within the
// sidebar panel and clamped on-screen (mirrors invokeFilterPopup's placement).
var positionSidebarPopover = function(popup, anchorEl) {
   var rect = anchorEl.getBoundingClientRect();
   var left = rect.left - 4;
   var panel = anchorEl.closest ? anchorEl.closest("#sidebarPanel") : null;
   if (panel) {
      var sb = panel.getBoundingClientRect();
      left = sb.left + (sb.width - popup.offsetWidth) / 2;
   }
   if (popup.offsetWidth + left > document.body.offsetWidth)
      left = document.body.offsetWidth - popup.offsetWidth;
   if (left < 0) left = 0;

   // The stats popover can be tall (numeric/factor); for an anchor low in the
   // panel, opening downward would run it off the bottom of the viewport. Flip
   // it above the anchor when it doesn't fit below, then clamp to the top edge.
   var viewH = document.documentElement.clientHeight || document.body.offsetHeight;
   var top = rect.bottom + 2;
   if (top + popup.offsetHeight > viewH) {
      var above = rect.top - 2 - popup.offsetHeight;
      top = (above >= 0) ? above : Math.max(0, viewH - popup.offsetHeight);
   }

   popup.style.top = top + "px";
   popup.style.left = left + "px";
};

var openSidebarStats = function(anchorEl, absIdx, colType) {
   // Toggle closed if it's already open for this column.
   if (sidebarStatsPopup && sidebarStatsAbs === absIdx) {
      dismissSidebarStats();
      return;
   }
   // Dismiss any other open popup (a filter popup, or stats for another column).
   if (dismissActivePopup) dismissActivePopup(true);
   dismissSidebarStats();

   var popup = document.createElement("div");
   popup.className = "filterPopup sidebar-stats-popup";
   var loading = document.createElement("div");
   loading.className = "stats-empty";
   loading.textContent = "Loading...";
   popup.appendChild(loading);
   document.body.appendChild(popup);
   sidebarStatsPopup = popup;
   sidebarStatsAbs = absIdx;
   positionSidebarPopover(popup, anchorEl);

   anchorEl.classList.add("expanded");
   anchorEl.setAttribute("aria-expanded", "true");

   document.body.addEventListener("click", sidebarStatsLightDismiss);
   document.body.addEventListener("keydown", sidebarStatsEscDismiss);
   dismissActivePopup = dismissSidebarStats;

   fetchColumnSummary(absIdx, function(data) {
      // Superseded or dismissed while the fetch was in flight.
      if (sidebarStatsPopup !== popup) return;
      if (data === null) { dismissSidebarStats(); return; }
      renderColumnStats(popup, data, colType);
      positionSidebarPopover(popup, anchorEl);
   });
};

var initSidebar = function() {
   var content = document.getElementById("sidebarContent");
   var toggle = document.getElementById("sidebarToggle");
   if (!content || !toggle || !cols) return;

   toggle.innerHTML = "";
   toggle.setAttribute("role", "button");
   toggle.setAttribute("tabindex", "0");
   toggle.setAttribute("aria-controls", "sidebarContent");
   toggle.setAttribute("aria-expanded", sidebarVisible ? "true" : "false");
   toggle.setAttribute("aria-label", "Toggle column summary panel");
   // The host toolbar's Summary button already names the panel, so the header
   // shows the column count instead of repeating "Summary". The sidebar lists
   // every column of the frame, so the count is the frame total. While the
   // complete index is still loading it falls back to the fetched window, so
   // show "N of M" during that brief transient (and for the rare frame whose
   // index failed to load).
   var listedCols = sidebarColumnList().length;

   var toggleLabel = document.createElement("span");
   toggleLabel.className = "sidebar-toggle-label";
   if (sidebarColumns === null && totalCols > listedCols) {
      toggleLabel.textContent = listedCols.toLocaleString() + " of " +
         totalCols.toLocaleString() + " columns";
   } else {
      toggleLabel.textContent = totalCols.toLocaleString() +
         (totalCols === 1 ? " column" : " columns");
   }
   toggle.appendChild(toggleLabel);

   // When the summaries describe a filtered subset rather than the whole
   // frame, say so -- the histograms/ranges/NA% otherwise look like they
   // describe the full object. (filteredSummaries is set only while a column
   // filter or global search is active.)
   if (filteredSummaries !== null) {
      var filteredTag = document.createElement("span");
      filteredTag.className = "sidebar-toggle-filtered";
      filteredTag.textContent = " (filtered)";
      filteredTag.title = "Summaries reflect the current filter and search";
      toggleLabel.appendChild(filteredTag);
   }

   // Help icon: opens a small dialog explaining the summary entries. Unlike
   // the close glyph below, activation must NOT reach the toggle -- it opens
   // the dialog rather than collapsing the panel -- so both handlers stop
   // propagation. A real button (focusable, labelled), since it does
   // something other than what the header announces.
   var toggleHelp = document.createElement("span");
   toggleHelp.className = "sidebar-toggle-help";
   toggleHelp.textContent = "?";
   toggleHelp.setAttribute("role", "button");
   toggleHelp.setAttribute("tabindex", "0");
   toggleHelp.setAttribute("aria-label", "About column summaries");
   toggleHelp.title = "About column summaries";
   var onToggleHelpActivate = function(evt) {
      evt.stopPropagation();
      evt.preventDefault();
      showSidebarHelp(toggleHelp);
   };
   toggleHelp.addEventListener("click", onToggleHelpActivate);
   toggleHelp.addEventListener("keydown", function(evt) {
      if (evt.key === "Enter" || evt.key === " ")
         onToggleHelpActivate(evt);
   });
   toggle.appendChild(toggleHelp);

   // Decorative close glyph; clicks bubble to the toggle, which remains the
   // panel's single accessible control.
   var toggleClose = document.createElement("span");
   toggleClose.className = "sidebar-toggle-close";
   toggleClose.setAttribute("aria-hidden", "true");
   toggleClose.textContent = "\u00D7";
   toggle.appendChild(toggleClose);

   toggle.addEventListener("click", onSidebarToggleActivate);
   toggle.addEventListener("keydown", onSidebarToggleKeyDown);

   content.setAttribute("role", "list");

   content.innerHTML = "";

   // Drop any sparklines registered by a previous initSidebar -- their
   // containers were just removed by the innerHTML reset above.
   pendingSparklines_ = [];

   // Reset the lazy summary-fetch queue for this rebuild.
   sidebarPendingFetch = {};

   // Snapshot the listed columns (rowname excluded) with an abs-index lookup,
   // then build the virtualized structure: a top spacer, a container for the
   // entries currently in the visible window, and a bottom spacer (which also
   // carries the overscroll tail). renderSidebarWindow fills the container with
   // only the entries whose vertical band intersects the panel viewport, so
   // initSidebar is O(visible) rather than O(all columns).
   sidebarListCols = [];
   sidebarIndexByAbs = {};
   var allSidebarCols = sidebarColumnList();
   for (var si = 0; si < allSidebarCols.length; si++) {
      var sc = allSidebarCols[si];
      if (isRownameColumn(sc)) continue;
      var scAbs = (typeof sc.col_index === "number")
         ? sc.col_index : sidebarListCols.length + 1;
      sidebarIndexByAbs[scAbs] = sidebarListCols.length;
      sidebarListCols.push(sc);
   }

   sidebarRenderTop = document.createElement("div");
   sidebarRenderTop.className = "sidebar-virt-spacer";
   sidebarRenderTop.setAttribute("aria-hidden", "true");
   sidebarRenderMid = document.createElement("div");
   sidebarRenderBottom = document.createElement("div");
   sidebarRenderBottom.className = "sidebar-virt-spacer";
   sidebarRenderBottom.setAttribute("aria-hidden", "true");
   content.appendChild(sidebarRenderTop);
   content.appendChild(sidebarRenderMid);
   content.appendChild(sidebarRenderBottom);
   sidebarWinStart = -1;
   sidebarWinEnd = -1;

   // Entry interactions are delegated to the persistent content container
   // (built entries come and go as the window scrolls). addEventListener is
   // idempotent for these module-scoped handlers across rebuilds.
   content.addEventListener("click", onSidebarContentClick);
   content.addEventListener("keydown", onSidebarContentKeydown);

   // Kick off loading the complete column index if we're still showing only
   // the fetched window; it rebuilds the sidebar when it lands.
   ensureSidebarColumns();

   // Apply initial sidebar visibility before measuring so renderSidebarWindow
   // reads the real (expanded) clientHeight.
   var panel = document.getElementById("sidebarPanel");
   if (panel) {
      panel.classList.toggle("expanded", sidebarVisible);
   }

   // Build the initial visible window. Reading clientHeight here forces a
   // layout, but only of the two spacers plus the handful of visible entries
   // (not every column) -- which is the whole point of the virtualization.
   renderSidebarWindow(true);

   // Attach the floating scrollbar after the spacers establish the scroll range.
   attachSidebarScrollbar();
   if (sidebarScrollbar_) sidebarScrollbar_.update();
};

// Sync the sidebar's pin and sort icons with the module pin/sort state.
// Called after any pin or sort mutation -- whether it originated from the
// grid header, the keyboard, or the sidebar icons themselves -- and at the
// end of initSidebar to apply the initial (possibly restored) state.
// Re-apply pin/sort/filter indicator state to the currently-built entries.
// With virtualization only the visible window exists in the DOM; off-window
// entries pick up the current state when they are next built (buildSidebarEntry
// calls applySidebarEntryIndicators). The per-entry logic is shared with that
// build path via applySidebarEntryIndicators.
var updateSidebarColumnIndicators = function() {
   var content = document.getElementById("sidebarContent");
   if (!content) return;
   var entries = content.querySelectorAll(".sidebar-col");
   for (var i = 0; i < entries.length; i++)
      applySidebarEntryIndicators(entries[i]);
};

// ==========================================================================
// Sidebar help dialog
// ==========================================================================

// Element to restore focus to when the help dialog closes (the header's
// help icon). Module-scoped: the dialog outlives sidebar rebuilds.
var sidebarHelpReturnFocus_ = null;

// Show the dialog explaining the structure of the sidebar's column
// summaries, building it on first use. It is appended to <body> rather
// than the sidebar so a data refresh (which rebuilds the sidebar DOM)
// can't tear it down mid-read.
var showSidebarHelp = function(returnFocusEl) {
   var overlay = document.getElementById("sidebarHelpOverlay");
   if (!overlay) {
      overlay = buildSidebarHelpOverlay();
      document.body.appendChild(overlay);
   }
   overlay.style.display = "";
   sidebarHelpReturnFocus_ = returnFocusEl || null;

   var dialog = overlay.querySelector(".sidebar-help-dialog");
   if (dialog) dialog.focus();
};

var hideSidebarHelp = function() {
   var overlay = document.getElementById("sidebarHelpOverlay");
   if (overlay) overlay.style.display = "none";

   // Restore focus to the opener if it's still in the document (the
   // sidebar may have been rebuilt while the dialog was open).
   if (sidebarHelpReturnFocus_ && sidebarHelpReturnFocus_.isConnected)
      sidebarHelpReturnFocus_.focus();
   sidebarHelpReturnFocus_ = null;
};

var buildSidebarHelpOverlay = function() {
   var overlay = document.createElement("div");
   overlay.id = "sidebarHelpOverlay";

   // Dismiss on backdrop click; clicks inside the dialog hit the dialog
   // node (or its children), not the overlay itself.
   overlay.addEventListener("mousedown", function(evt) {
      if (evt.target === overlay) hideSidebarHelp();
   });

   var dialog = document.createElement("div");
   dialog.className = "sidebar-help-dialog";
   dialog.setAttribute("role", "dialog");
   dialog.setAttribute("aria-modal", "true");
   dialog.setAttribute("aria-labelledby", "sidebarHelpTitle");
   dialog.setAttribute("tabindex", "-1");

   var title = document.createElement("div");
   title.className = "sidebar-help-title";
   title.id = "sidebarHelpTitle";
   title.textContent = "Column summaries";
   dialog.appendChild(title);

   var closeBtn = document.createElement("span");
   closeBtn.className = "sidebar-help-close";
   closeBtn.textContent = "\u00D7";
   closeBtn.setAttribute("role", "button");
   closeBtn.setAttribute("tabindex", "0");
   closeBtn.setAttribute("aria-label", "Close");
   closeBtn.addEventListener("click", hideSidebarHelp);
   closeBtn.addEventListener("keydown", function(evt) {
      if (evt.key === "Enter" || evt.key === " ") {
         evt.preventDefault();
         hideSidebarHelp();
      }
   });
   dialog.appendChild(closeBtn);

   dialog.addEventListener("keydown", function(evt) {
      if (evt.key === "Escape") {
         evt.stopPropagation();
         hideSidebarHelp();
      } else if (evt.key === "Tab") {
         // Minimal focus trap, as aria-modal promises one: the close glyph
         // is the dialog's only tabbable element, so park focus there
         // rather than letting Tab walk into the grid behind the backdrop.
         evt.preventDefault();
         closeBtn.focus();
      }
   });

   var addSection = function(heading) {
      var section = document.createElement("div");
      section.className = "sidebar-help-section";
      var head = document.createElement("div");
      head.className = "sidebar-help-heading";
      head.textContent = heading;
      section.appendChild(head);
      dialog.appendChild(section);
      return section;
   };

   var addLine = function(section, text, swatchClass) {
      var line = document.createElement("div");
      line.className = "sidebar-help-line";
      if (swatchClass) {
         var swatch = document.createElement("span");
         swatch.className = "sidebar-help-swatch " + swatchClass;
         swatch.setAttribute("aria-hidden", "true");
         line.appendChild(swatch);
      }
      line.appendChild(document.createTextNode(text));
      section.appendChild(line);
   };

   var headerSec = addSection("Header");
   addLine(headerSec,
      "Each entry names a column and its type. Click an entry to scroll " +
      "the grid to that column; the pin and sort icons work just like " +
      "their counterparts in the grid header, and the funnel icon filters " +
      "the column (see Filter below). For date and date-time columns, hover " +
      "the type label to see the timezone.");

   var plotSec = addSection("Mini-plot");
   addLine(plotSec,
      "Numeric columns draw a histogram of their finite values.",
      "numeric");
   addLine(plotSec,
      "Date and date-time columns draw a histogram over time.",
      "date");
   addLine(plotSec,
      "Factor and character columns with few distinct values draw one " +
      "bar per value: factors in level order, characters most frequent " +
      "first. Hover a bar for details.",
      "categorical");

   var statsSec = addSection("Summary line");
   addLine(statsSec,
      "The numeric or date/time data range as [min, max] (a date-time range " +
      "within a single day shows the date once with a time range), or the " +
      "number of factor levels / distinct values -- plus the most frequent " +
      "value when there are too many to chart. Distinct values are not " +
      "counted for very large character columns. The right-hand side shows " +
      "the percentage of missing values.");

   var filterSec = addSection("Filter");
   addLine(filterSec,
      "The funnel icon opens a filter for the column. Numeric and date/time " +
      "columns brush a range on the histogram (or type one); factor, logical, " +
      "and text columns pick or match a value. The grid updates as you adjust " +
      "the filter. Use the checkmark to confirm and close, or the x to clear " +
      "it; clicking away leaves the filter in place.");

   var detailsSec = addSection("Details");
   addLine(detailsSec,
      "The triangle expands a panel of detailed statistics, computed " +
      "on demand; for date-time columns this includes the timezone.");

   overlay.appendChild(dialog);
   return overlay;
};

// Draw any sparklines whose canvas hasn't been rendered yet. Idempotent and
// cheap to call repeatedly -- once drained, subsequent calls are no-ops, so
// it is safe to invoke on every panel-open.
var renderPendingSparklines = function() {
   if (pendingSparklines_.length === 0) return;
   var items = pendingSparklines_;
   pendingSparklines_ = [];
   for (var i = 0; i < items.length; i++) {
      var spark = createSparkline(items[i].breaks, items[i].counts,
                                  items[i].labels, items[i].breakLabels);
      if (spark) {
         items[i].container.appendChild(spark);
      }
      // No drawable histogram (all-equal values, or no 2D context): leave the
      // reserved slot empty so the entry keeps its constant height.
   }
};

var toggleSidebar = function() {
   var panel = document.getElementById("sidebarPanel");
   if (!panel) return;
   sidebarVisible = !sidebarVisible;
   if (sidebarVisible) {
      panel.classList.add("expanded");
      // Draw deferred sparklines the first time the panel is opened.
      renderPendingSparklines();
   } else {
      panel.classList.remove("expanded");
   }
   var toggle = document.getElementById("sidebarToggle");
   if (toggle) toggle.setAttribute("aria-expanded", sidebarVisible ? "true" : "false");
   // Trigger grid resize after transition
   setTimeout(function() {
      // viewport.clientWidth changes when the sidebar opens/closes, which
      // feeds into the overscroll calculation in applyPinnedColumns --
      // recompute paddingRight so it matches the new viewport width.
      applyPinnedColumns();
      renderVisibleRows(true);
      updateInfoBar();
      // Opening the panel gives it a real clientHeight; (re)build its visible
      // entry window now that the transition has settled.
      renderSidebarWindow(true);
      updateCustomScrollbars();
   }, TIMING.sidebarTransition);
   saveState();
   if (window.sidebarStateCallback) window.sidebarStateCallback(sidebarVisible);
};

// Scroll the viewport so the column at columnOrder position `pos` is visible.
// Geometry is derived from measuredWidths (not a rendered <th>), so this works
// for columns outside the current render window; unpinned columns sit to the
// right of the left unfetched span. With `center`, the column is scrolled to
// the middle of the unpinned viewport region (go-to-column jumps want context
// on both sides); otherwise it's the minimal scroll that brings an off-edge
// column flush with the nearest edge, accounting for the sticky pinned columns
// occluding the left. Returns true (and updates lastScrollLeft) when it
// actually scrolls; a no-op when the column is already where it needs to be.
var scrollColumnPosIntoView = function(pos, center) {
   var viewport = document.getElementById("gridViewport");
   if (!viewport || pos < 0 || pos >= columnOrder.length) return false;

   var offs = columnOffsets();
   var colLeft = offs[pos] + (pos >= firstUnpinnedPos() ? leftSpanWidth() : 0);
   var colWidth = measuredWidths[pos] || 0;
   var pinnedWidth = getPinnedOffsets().totalWidth;
   var viewLeft = viewport.scrollLeft;
   var viewWidth = viewport.clientWidth;

   var newLeft = viewLeft;
   if (center) {
      var fullyVisible = colLeft >= viewLeft + pinnedWidth &&
                         colLeft + colWidth <= viewLeft + viewWidth;
      if (!fullyVisible) {
         var region = Math.max(0, viewWidth - pinnedWidth);
         newLeft = Math.max(0,
            colLeft - pinnedWidth - Math.max(0, (region - colWidth) / 2));
      }
   } else if (colLeft < viewLeft + pinnedWidth) {
      newLeft = Math.max(0, colLeft - pinnedWidth);
   } else if (colLeft + colWidth > viewLeft + viewWidth) {
      newLeft = colLeft + colWidth - viewWidth;
   }

   if (newLeft === viewLeft) return false;
   setViewportScrollLeft(viewport, newLeft);
   return true;
};

// Briefly highlight a column's header so the user can spot where a jump
// landed. No-op when the header isn't rendered.
var flashColumnHeader = function(colIdx) {
   var th = getHeaderCell(colIdx);
   if (!th) return;
   th.classList.add("highlight-flash");
   setTimeout(function() {
      th.classList.remove("highlight-flash");
   }, TIMING.columnFlash);
};

// Scroll a column's sidebar entry into view and flash it, mirroring the header
// flash on a go-to-column jump so the summary panel tracks the jump (the inverse
// of clicking a sidebar entry, which scrolls the grid). No-op when the panel is
// collapsed or the column isn't listed. Deliberately does NOT move keyboard
// focus -- the jump leaves focus on the grid so arrow keys drive the data.
// absIdx is the ABSOLUTE column index.
var flashSidebarColumn = function(absIdx) {
   if (!sidebarVisible) return;
   var content = document.getElementById("sidebarContent");
   if (!content) return;

   // The target entry may not currently be built; resolve its virtual index and
   // compute its offset from the constant entry height.
   var idx = sidebarVirtIndexForAbs(absIdx);
   if (idx < 0) return;
   var H = sidebarEntryHeight();
   var top = idx * H;
   var viewTop = content.scrollTop;
   var viewH = content.clientHeight;

   // Center the entry, but only when it isn't already fully visible -- so a jump
   // to a nearby, already-on-screen column doesn't jolt the list.
   if (top < viewTop || top + H > viewTop + viewH) {
      content.scrollTop = Math.max(0, top - Math.max(0, (viewH - H) / 2));
   }

   // Build the window at the new scroll position so the entry exists to flash.
   renderSidebarWindow(false);
   if (sidebarScrollbar_) sidebarScrollbar_.update();

   var entry = content.querySelector('.sidebar-col[data-col-idx="' + absIdx + '"]');
   if (!entry) return;
   entry.classList.add("highlight-flash");
   setTimeout(function() {
      entry.classList.remove("highlight-flash");
   }, TIMING.columnFlash);
};

// ==========================================================================
// Active Cell / Keyboard Navigation
// ==========================================================================

var getActiveCellTd = function() {
   if (activeRow < 0 || activeCol < 0) return null;
   var tr = renderedRowElements.get(activeRow);
   if (!tr) return null;
   // Can't index by activeCol: tr.children is the windowed cell set (pinned +
   // spacers + window), not 1:1 with columnOrder. Every rendered cell records
   // its columnOrder position in data-col-pos (set by buildRow), so match on
   // that. We must NOT look up by the activeCell id here: buildRow assigns that
   // id only to the cell that is *already* active, so a freshly clicked or
   // navigated-to cell would not have it yet and setActiveCell could never find
   // the cell to mark -- the highlight would only appear after a later render
   // pass rebuilt the row. Returns null when the column is outside the rendered
   // window (caller handles the absent case).
   return tr.querySelector('[data-col-pos="' + activeCol + '"]');
};

var activeCellId = function(row, col) {
   return "rsGridCell_" + row + "_" + col;
};

var getActiveHeaderTh = function() {
   if (activeHeaderCol < 0) return null;
   // activeHeaderCol is a columnOrder position; resolve to the rendered <th>
   // by its column index (may be null if outside the column window).
   return getHeaderCell(columnOrder[activeHeaderCol]);
};

// Mirror the active descendant on the viewport so screen readers can
// follow virtual focus into the grid. The viewport is the focused
// element; the active cell or header is its activedescendant.
var setViewportActiveDescendant = function(id) {
   var viewport = document.getElementById("gridViewport");
   if (!viewport) return;
   if (id) {
      viewport.setAttribute("aria-activedescendant", id);
   } else {
      viewport.removeAttribute("aria-activedescendant");
   }
};

// Used by click handlers (cell, header, pin) to bring focus into the
// grid -- without it, subsequent keystrokes would route to whatever was
// focused before the click instead of onGridKeyDown.
var focusGridViewport = function() {
   var viewport = document.getElementById("gridViewport");
   if (viewport && viewport.focus) viewport.focus({ preventScroll: true });
};

var clearActiveCell = function() {
   var td = getActiveCellTd();
   if (td) {
      td.classList.remove("activeCell");
      td.removeAttribute("id");
   }
   activeRow = -1;
   activeCol = -1;
   if (activeHeaderCol < 0) setViewportActiveDescendant(null);
};

var clearActiveHeader = function() {
   var th = getActiveHeaderTh();
   if (th) th.classList.remove("activeHeader");
   activeHeaderCol = -1;
   if (activeRow < 0 || activeCol < 0) setViewportActiveDescendant(null);
};

var ensureActiveCellVisible = function() {
   var viewport = document.getElementById("gridViewport");
   if (!viewport) return;

   // Vertical: rows are uniform-height so we can compute target scrollTop
   // directly without consulting the DOM (the row may not be rendered yet
   // -- the scroll itself triggers the render).
   //
   // rowTop/rowBottom are offsets within the table body, which starts
   // header-height px into the scroll content; setting scrollTop = rowTop
   // therefore lands the row exactly below the sticky header. The bottom
   // edge must use the body-visible height, not the raw clientHeight --
   // the header (and the horizontal scrollbar overlay) occlude part of the
   // viewport, and using clientHeight left bottom-edge scrolls short by the
   // header height (#17958).
   var rowTop = activeRow * ROW_HEIGHT;
   var rowBottom = rowTop + ROW_HEIGHT;
   var viewTop = viewport.scrollTop;
   var bodyHeight = visibleBodyHeight(viewport);
   if (rowTop < viewTop) {
      setViewportScrollTop(viewport, rowTop);
   } else if (rowBottom > viewTop + bodyHeight) {
      setViewportScrollTop(viewport, rowBottom - bodyHeight);
   }

   // Horizontal: scroll the active column flush with the nearest edge if it's
   // off-screen (no-op when already visible).
   if (activeCol < 0 || activeCol >= columnOrder.length) return;
   scrollColumnPosIntoView(activeCol, false);

   // Bring the (possibly new) column window into the DOM synchronously so the
   // active cell is rendered for the caller to mark and for screen readers.
   if (syncColumnWindow()) renderVisibleRows(true);
};

var setActiveCell = function(row, col) {
   var maxRow = filteredRows - 1;
   var maxCol = columnOrder.length - 1;
   if (maxRow < 0 || maxCol < 0) return;
   row = Math.max(0, Math.min(maxRow, row));
   col = Math.max(0, Math.min(maxCol, col));
   if (row === activeRow && col === activeCol && activeHeaderCol < 0) {
      // Defensive reapply: any future caller that rebuilds rows without
      // restoring the active cell would leave the model and DOM out of
      // sync; idempotent class/id/aria reassignment closes that gap.
      var existing = getActiveCellTd();
      if (existing) {
         existing.classList.add("activeCell");
         existing.id = activeCellId(row, col);
      }
      setViewportActiveDescendant(activeCellId(row, col));
      return;
   }

   if (activeHeaderCol >= 0) {
      var prevTh = getActiveHeaderTh();
      if (prevTh) prevTh.classList.remove("activeHeader");
      activeHeaderCol = -1;
   }

   var prev = getActiveCellTd();
   if (prev) {
      prev.classList.remove("activeCell");
      prev.removeAttribute("id");
   }

   activeRow = row;
   activeCol = col;

   ensureActiveCellVisible();

   // Apply to the new td if it's currently rendered. If it isn't yet,
   // ensureActiveCellVisible will have scrolled it into view; the next
   // renderVisibleRows call (triggered by the scroll event) will pick up
   // the .activeCell class and id via buildRow.
   var next = getActiveCellTd();
   if (next) {
      next.classList.add("activeCell");
      next.id = activeCellId(row, col);
   }
   // The activedescendant id may briefly point at an element that
   // doesn't exist yet (cell scrolled into view but not rendered until
   // the scroll event fires renderVisibleRows). buildRow re-applies the
   // id when the td finally materializes, so screen readers see a valid
   // descendant by the time keyboard movement settles.
   setViewportActiveDescendant(activeCellId(row, col));
};

var ensureActiveHeaderVisible = function() {
   var viewport = document.getElementById("gridViewport");
   if (!viewport) return;
   if (activeHeaderCol < 0 || activeHeaderCol >= columnOrder.length) return;

   // Scroll the active header flush with the nearest edge if it's off-screen
   // (no-op when already visible).
   scrollColumnPosIntoView(activeHeaderCol, false);

   if (syncColumnWindow()) renderVisibleRows(true);
};

var setActiveHeader = function(col) {
   var maxCol = columnOrder.length - 1;
   if (maxCol < 0) return;
   col = Math.max(0, Math.min(maxCol, col));
   if (col === activeHeaderCol) {
      // Defensive reapply: any future caller that rebuilds headers without
      // restoring the active header would leave the model and DOM out of
      // sync; idempotent class/aria reassignment closes that gap.
      var existingTh = getActiveHeaderTh();
      if (existingTh) {
         existingTh.classList.add("activeHeader");
         setViewportActiveDescendant(existingTh.id);
      } else {
         setViewportActiveDescendant(null);
      }
      return;
   }

   if (activeRow >= 0 || activeCol >= 0) {
      var prevCellTd = getActiveCellTd();
      if (prevCellTd) {
         prevCellTd.classList.remove("activeCell");
         prevCellTd.removeAttribute("id");
      }
      activeRow = -1;
      activeCol = -1;
   }

   var prevTh = getActiveHeaderTh();
   if (prevTh) prevTh.classList.remove("activeHeader");

   activeHeaderCol = col;
   ensureActiveHeaderVisible();

   var nextTh = getActiveHeaderTh();
   if (nextTh) {
      nextTh.classList.add("activeHeader");
      setViewportActiveDescendant(nextTh.id);
   } else {
      setViewportActiveDescendant(null);
   }
};

var copyActiveCell = function() {
   var td = getActiveCellTd();
   if (!td) return false;
   var text = td.textContent || "";
   if (navigator.clipboard && navigator.clipboard.writeText) {
      // Async path: optimistically claim success and let the caller
      // preventDefault() the keystroke; the .catch() is for diagnostics
      // since the browser already swallowed the default copy gesture.
      navigator.clipboard.writeText(text).catch(function(err) {
         console.warn("Clipboard write failed:", err);
      });
      return true;
   }
   // Pre-Clipboard-API fallback. execCommand("copy") is deprecated but
   // still widely supported and remains the only synchronous path.
   var ta = document.createElement("textarea");
   ta.value = text;
   ta.style.position = "fixed";
   ta.style.opacity = "0";
   document.body.appendChild(ta);
   ta.select();
   var ok = false;
   try { ok = document.execCommand("copy"); } catch (e) { /* ok stays false */ }
   document.body.removeChild(ta);
   return ok;
};

var onGridKeyDown = function(evt) {
   // Ignore keystrokes that are clearly meant for inputs inside the grid
   // (filter popups, search boxes injected over the viewport).
   var t = evt.target;
   if (t && (t.tagName === "INPUT" || t.tagName === "TEXTAREA" || t.isContentEditable)) {
      return;
   }

   var key = evt.key;
   var isCopy = (evt.ctrlKey || evt.metaKey) && (key === "c" || key === "C");

   var maxRow = filteredRows - 1;
   var maxCol = columnOrder.length - 1;
   if (maxCol < 0) return;

   // Header-mode actions: Enter/Space cycles sort; P toggles pin. Both
   // are no-ops on the rownames column. Header-mode dispatch runs before
   // the copy block so header-specific handling for Ctrl+C (e.g. copying
   // the column name) can be added here without the body-mode copy path
   // pre-empting it.
   if (activeHeaderCol >= 0) {
      var origCol = columnOrder[activeHeaderCol];
      var col = cols[origCol];
      var isRownames = col && isRownameColumn(col);
      if (key === "Enter" || key === " ") {
         evt.preventDefault();
         if (ordering && isColumnSortable(col)) {
            handleSortClick(absColIndex(origCol));
         }
         return;
      }
      if (key === "p" || key === "P") {
         evt.preventDefault();
         if (!isRownames) togglePinColumn(absColIndex(origCol));
         return;
      }
   }

   if (isCopy) {
      // If the user has a non-empty native text selection (e.g. drag-selected
      // across multiple cells), let the browser copy it. Overriding with the
      // single active cell here would clobber a multi-cell selection -- the
      // active-cell copy is only the fallback when nothing is selected.
      var sel = window.getSelection ? window.getSelection() : null;
      var hasSelection = sel && !sel.isCollapsed && sel.toString().length > 0;
      if (!hasSelection && copyActiveCell()) evt.preventDefault();
      return;
   }

   var navKeys = {
      "ArrowUp": 1, "ArrowDown": 1, "ArrowLeft": 1, "ArrowRight": 1,
      "Home": 1, "End": 1, "PageUp": 1, "PageDown": 1
   };
   if (!navKeys[key]) return;

   evt.preventDefault();

   // First navigation keypress with no active descendant lands on (0, 0)
   // (or the first header if the body is empty) without applying the
   // delta -- otherwise ArrowDown would skip straight to row 1.
   if (activeRow < 0 && activeCol < 0 && activeHeaderCol < 0) {
      if (maxRow >= 0) setActiveCell(0, 0);
      else setActiveHeader(0);
      return;
   }

   // Header-mode arrow navigation. ArrowDown drops back into the body at
   // the same column; Home/End jump to the first/last column; Left/Right
   // walk between headers; PageUp/PageDown are inert.
   if (activeHeaderCol >= 0) {
      switch (key) {
         case "ArrowDown":
            if (maxRow >= 0) setActiveCell(0, activeHeaderCol);
            return;
         case "ArrowUp":
            return;
         case "ArrowLeft":
            setActiveHeader(activeHeaderCol - 1);
            return;
         case "ArrowRight":
            setActiveHeader(activeHeaderCol + 1);
            return;
         case "Home":
            setActiveHeader(0);
            return;
         case "End":
            setActiveHeader(maxCol);
            return;
         case "PageUp":
         case "PageDown":
            return;
      }
   }

   if (maxRow < 0) return;

   // ArrowUp from row 0 enters header mode at the same column. Other
   // arrow keys fall through to the body movement below.
   if (key === "ArrowUp" && activeRow === 0) {
      setActiveHeader(activeCol);
      return;
   }

   var r = activeRow;
   var c = activeCol;
   var pageRows = Math.max(
      1,
      Math.floor(document.getElementById("gridViewport").clientHeight / ROW_HEIGHT) - 1);

   // Home/End move vertically within the current column (with or without
   // Ctrl), matching the pre-rewrite viewer where they scrolled the grid to
   // the top/bottom without changing the horizontal position (#17958).
   switch (key) {
      case "ArrowUp":    r = r - 1; break;
      case "ArrowDown":  r = r + 1; break;
      case "ArrowLeft":  c = c - 1; break;
      case "ArrowRight": c = c + 1; break;
      case "Home":       r = 0;      break;
      case "End":        r = maxRow; break;
      case "PageUp":     r = r - pageRows; break;
      case "PageDown":   r = r + pageRows; break;
   }

   setActiveCell(r, c);
};

var onGridCellClick = function(evt) {
   // Ignore clicks on cell-internal interactive elements (drill-in links,
   // filter triggers) -- their own handlers should run undisturbed.
   var t = evt.target;
   if (!t) return;
   if (t.closest && t.closest("a, button, input, .viewerLink")) return;

   var td = t.closest ? t.closest("td") : null;
   if (!td) return;
   var tr = td.parentElement;
   if (!tr || !tr.hasAttribute("data-row")) return;

   var row = parseInt(tr.getAttribute("data-row"), 10);
   if (isNaN(row)) return;

   // The cell carries its columnOrder position (data-col-pos); spacer cells
   // have none. Don't derive it from DOM position -- spacers make tr.children
   // non-1:1 with columnOrder.
   var col = parseInt(td.getAttribute("data-col-pos"), 10);
   if (isNaN(col)) return;

   setActiveCell(row, col);

   // Focus the viewport so subsequent keystrokes route through the grid
   // keyboard handler. preventScroll keeps the click from yanking the
   // user out of their visual context.
   focusGridViewport();
};

// ==========================================================================
// Custom Scrollbars
// ==========================================================================

// Floating-overlay scrollbar attached to a scrollable element. Each instance
// owns its own DOM, drag/track-click handlers, and activity-based fade
// timer. Returns { update, destroy, show, isDragging }.
//
// `viewport`  -- the scrollable element whose scroll position the bar reflects.
// `host`      -- the positioned ancestor that the bar is appended into. Must
//               have position:relative (or absolute/fixed); see the CSS
//               rules on .custom-scrollbar.{vertical,horizontal}.
// `axis`      -- "vertical" or "horizontal".
// `options.getInsets`     -- optional () => {top, bottom, left, right}: gaps
//               between the bar and the host edges. Used for the grid's
//               sticky header and pinned-column offsets.
// `options.onDragEnd`     -- optional callback fired when a thumb/track drag
//               completes. The grid uses this to re-render the info bar.
var attachCustomScrollbar = function(viewport, host, axis, options) {
   options = options || {};
   var isVertical = (axis === "vertical");

   var bar = document.createElement("div");
   bar.className = "custom-scrollbar " + axis;
   var track = document.createElement("div");
   track.className = "scrollbar-track";
   var thumb = document.createElement("div");
   thumb.className = "scrollbar-thumb";
   track.appendChild(thumb);
   bar.appendChild(track);
   host.appendChild(bar);

   var hideTimer = 0;
   var dragging = false;

   var scheduleHide = function() {
      clearTimeout(hideTimer);
      hideTimer = setTimeout(function() {
         bar.classList.remove("visible");
      }, TIMING.scrollbarHide);
   };

   var show = function() {
      bar.classList.add("visible");
      scheduleHide();
   };

   bar.addEventListener("mouseenter", show);
   bar.addEventListener("mouseleave", scheduleHide);

   thumb.addEventListener("mousedown", function(evt) {
      evt.preventDefault();
      evt.stopPropagation();
      thumb.classList.add("dragging");
      dragging = true;

      var dragStart = isVertical ? evt.clientY : evt.clientX;
      var scrollStart = isVertical ? viewport.scrollTop : viewport.scrollLeft;
      // Cache layout reads once at drag start -- they don't change during a
      // drag and reading them in onMove forces expensive reflow.
      var trackSize = isVertical ? bar.clientHeight : bar.clientWidth;
      var contentSize = isVertical
         ? viewport.scrollHeight - viewport.clientHeight
         : viewport.scrollWidth - viewport.clientWidth;
      var thumbSize = isVertical ? thumb.offsetHeight : thumb.offsetWidth;
      var ratio = (trackSize - thumbSize) > 0
         ? contentSize / (trackSize - thumbSize) : 0;

      var onMove = function(e) {
         var delta = (isVertical ? e.clientY : e.clientX) - dragStart;
         if (ratio > 0) {
            var newPos = scrollStart + delta * ratio;
            if (isVertical) viewport.scrollTop = newPos;
            else viewport.scrollLeft = newPos;
         }
      };
      var onUp = function() {
         thumb.classList.remove("dragging");
         dragging = false;
         document.removeEventListener("mousemove", onMove);
         document.removeEventListener("mouseup", onUp);
         if (options.onDragEnd) options.onDragEnd();
         scheduleHide();
      };

      document.addEventListener("mousemove", onMove);
      document.addEventListener("mouseup", onUp);
   });

   track.addEventListener("mousedown", function(evt) {
      if (evt.target === thumb) return;
      evt.preventDefault();

      var jump = function(e) {
         var rect = track.getBoundingClientRect();
         var pos = isVertical
            ? (e.clientY - rect.top) / rect.height
            : (e.clientX - rect.left) / rect.width;
         pos = Math.max(0, Math.min(1, pos));
         if (isVertical) {
            viewport.scrollTop = pos *
               (viewport.scrollHeight - viewport.clientHeight);
         } else {
            viewport.scrollLeft = pos *
               (viewport.scrollWidth - viewport.clientWidth);
         }
      };

      dragging = true;
      jump(evt);

      var onMove = function(e) { e.preventDefault(); jump(e); };
      var onUp = function() {
         dragging = false;
         document.removeEventListener("mousemove", onMove);
         document.removeEventListener("mouseup", onUp);
         if (options.onDragEnd) options.onDragEnd();
         scheduleHide();
      };

      document.addEventListener("mousemove", onMove);
      document.addEventListener("mouseup", onUp);
   });

   var update = function() {
      var contentSize = isVertical ? viewport.scrollHeight : viewport.scrollWidth;
      var viewSize = isVertical ? viewport.clientHeight : viewport.clientWidth;
      var scrollPos = isVertical ? viewport.scrollTop : viewport.scrollLeft;

      var insets = options.getInsets ? options.getInsets() : {};
      var insetTop = insets.top || 0;
      var insetBottom = insets.bottom || 0;
      var insetLeft = insets.left || 0;
      var insetRight = insets.right || 0;

      var hostSize = isVertical ? host.offsetHeight : host.offsetWidth;
      // Clamp to >= 0 so oversized insets can't produce negative track
      // sizes (which break thumb positioning math).
      var barSize = Math.max(0, hostSize -
         (isVertical ? insetTop + insetBottom : insetLeft + insetRight));

      var hasScroll = contentSize > viewSize + 1;

      if (isVertical) {
         bar.style.top = insetTop + "px";
         bar.style.height = barSize + "px";
      } else {
         bar.style.left = insetLeft + "px";
         bar.style.width = barSize + "px";
      }

      if (hasScroll && barSize > 0) {
         var thumbSize = Math.max(20, (viewSize / contentSize) * barSize);
         var maxScroll = contentSize - viewSize;
         var thumbPos = maxScroll > 0
            ? (scrollPos / maxScroll) * (barSize - thumbSize) : 0;
         if (isVertical) {
            thumb.style.height = thumbSize + "px";
            thumb.style.top = thumbPos + "px";
         } else {
            thumb.style.width = thumbSize + "px";
            thumb.style.left = thumbPos + "px";
         }
         bar.style.display = "";
      } else {
         bar.style.display = "none";
      }
   };

   var destroy = function() {
      clearTimeout(hideTimer);
      if (bar.parentNode) bar.parentNode.removeChild(bar);
   };

   return {
      element: bar,
      update: update,
      destroy: destroy,
      show: show,
      isDragging: function() { return dragging; }
   };
};

var anyScrollbarDragging = function() {
   return (gridScrollbarV_ && gridScrollbarV_.isDragging()) ||
          (gridScrollbarH_ && gridScrollbarH_.isDragging()) ||
          (sidebarScrollbar_ && sidebarScrollbar_.isDragging());
};

var createCustomScrollbars = function() {
   var gridPanel = document.getElementById("gridPanel");
   var viewport = document.getElementById("gridViewport");
   if (!gridPanel || !viewport) return;

   gridScrollbarV_ = attachCustomScrollbar(viewport, gridPanel, "vertical", {
      getInsets: function() {
         var thead = document.getElementById("data_cols");
         var headerH = thead && thead.parentElement
            ? thead.parentElement.offsetHeight : 0;
         var hasHScroll = viewport.scrollWidth > viewport.clientWidth + 1;
         // The bar's host is #gridPanel, which includes the info bar
         // below the scrollable viewport. Add its height to the bottom
         // inset so the bar doesn't extend over the info bar area.
         var infoBar = document.getElementById("rsGridData_info");
         var infoBarH = infoBar ? infoBar.offsetHeight : 0;
         return {
            top: headerH,
            bottom: infoBarH + (hasHScroll ? 10 : 0)
         };
      },
      onDragEnd: updateInfoBar
   });

   gridScrollbarH_ = attachCustomScrollbar(viewport, gridPanel, "horizontal", {
      getInsets: function() {
         var pinnedWidth = getPinnedOffsets().totalWidth;
         var hasVScroll = viewport.scrollHeight > viewport.clientHeight + 1;
         return { left: pinnedWidth, right: hasVScroll ? 10 : 0 };
      },
      onDragEnd: updateInfoBar
   });
};

// Module-scoped sidebar scroll listener -- defined once so addEventListener
// is idempotent across re-bootstraps. Reads sidebarScrollbar_ at call time
// so it's safe to register before/after attachCustomScrollbar fires.
var onSidebarScroll = function() {
   // A stats popover is anchored to a (now-moving) entry; dismiss it on scroll.
   if (sidebarStatsPopup) dismissSidebarStats();
   if (sidebarScrollbar_) {
      sidebarScrollbar_.show();
      sidebarScrollbar_.update();
   }
   // Re-render the virtual entry window, coalesced to one pass per frame.
   if (sidebarScrollRaf) return;
   sidebarScrollRaf = requestAnimationFrame(function() {
      sidebarScrollRaf = 0;
      renderSidebarWindow(false);
   });
};

// Vertical scrollbar for the Summary sidebar. Appended below the toggle
// label so the bar tracks only the scrollable content list. The scroll range
// (and thus the overscroll past the last entry) is set by the virtualizer's
// top/bottom spacers, so there is no padding to recompute here.
var attachSidebarScrollbar = function() {
   var sidebarPanel = document.getElementById("sidebarPanel");
   var sidebarContent = document.getElementById("sidebarContent");
   var sidebarToggle = document.getElementById("sidebarToggle");
   if (!sidebarPanel || !sidebarContent) return;

   // Tear down any previous instance to avoid stacking scrollbar overlays.
   // The scroll listener is module-scoped and idempotent, so no need to
   // detach/reattach it.
   if (sidebarScrollbar_) {
      sidebarScrollbar_.destroy();
      sidebarScrollbar_ = null;
   }

   sidebarScrollbar_ = attachCustomScrollbar(
      sidebarContent, sidebarPanel, "vertical", {
         getInsets: function() {
            return { top: sidebarToggle ? sidebarToggle.offsetHeight : 0 };
         }
      });

   sidebarContent.addEventListener("scroll", onSidebarScroll);
};

var updateCustomScrollbars = function() {
   if (gridScrollbarV_) gridScrollbarV_.update();
   if (gridScrollbarH_) gridScrollbarH_.update();
   if (sidebarScrollbar_) sidebarScrollbar_.update();
   updateColumnOverflowState();
};

// Last column-overflow state pushed to the host (null = not yet pushed).
// The host shows its "Go to Column..." button whenever the frame's columns
// overflow the viewport -- which can change on resize, sidebar toggle,
// column resize, pin changes, or a data refresh, so the grid pushes the
// state from updateCustomScrollbars (called on every layout change) rather
// than the host polling.
var lastColumnOverflow = null;

var updateColumnOverflowState = function() {
   if (!cols || totalTableWidth <= 0)
      return;
   var viewport = document.getElementById("gridViewport");
   if (!viewport)
      return;

   // totalTableWidth is the frame's full content width (fetched columns
   // plus estimated unfetched spans). Compare against the viewport rather
   // than scrollWidth, which includes the overscroll padding and would
   // read as "overflowing" for every frame.
   var overflow = totalTableWidth > viewport.clientWidth + 1;
   if (overflow !== lastColumnOverflow) {
      lastColumnOverflow = overflow;
      if (window.columnOverflowCallback)
         window.columnOverflowCallback(overflow);
   }
};

var showScrollbars = function() {
   if (gridScrollbarV_) gridScrollbarV_.show();
   if (gridScrollbarH_) gridScrollbarH_.show();
};

var destroyCustomScrollbars = function() {
   if (gridScrollbarV_) { gridScrollbarV_.destroy(); gridScrollbarV_ = null; }
   if (gridScrollbarH_) { gridScrollbarH_.destroy(); gridScrollbarH_ = null; }
   if (sidebarScrollbar_) { sidebarScrollbar_.destroy(); sidebarScrollbar_ = null; }
   var sidebarContent = document.getElementById("sidebarContent");
   if (sidebarContent) {
      sidebarContent.removeEventListener("scroll", onSidebarScroll);
   }
};

// ==========================================================================
// Grid Lifecycle
// ==========================================================================

// Normalize a fetched column-metadata response in place. R serializes
// col_breaks via as.character() to preserve full numeric precision over the
// JSON wire (avoiding the precision loss that an R double -> JSON-number
// round-trip can introduce). Convert back to numbers here so the rest of the
// client can do arithmetic on them.
var prepareColumnResponse = function(resCols) {
   for (var i = 0; i < resCols.length; i++) {
      if (resCols[i].col_breaks) {
         for (var j = 0; j < resCols[i].col_breaks.length; j++) {
            if (typeof resCols[i].col_breaks[j] === "string")
               resCols[i].col_breaks[j] = parseFloat(resCols[i].col_breaks[j]);
         }
      }
   }
};

// Install a freshly fetched column-metadata response as the current `cols`:
// refine it, recompute the column signature, the frame totals, and the fetched-
// window bounds the span layout derives from. Shared by the bootstrap (initGrid)
// and the in-place column-window slide (applyColumnWindowUpdate) so those two
// paths can't drift. Assumes columnOffset and maxDisplayColumns are already set
// for the window being installed.
var installColumnResponse = function(resCols) {
   prepareColumnResponse(resCols);

   cols = resCols;
   colsSig = colsRequestList().join(",");

   var resTotalCols = cols[0].total_cols;
   totalCols = resTotalCols > 0 ? resTotalCols : cols.length - 1;
   if (cols[0].total_rows > 0)
      totalRows = cols[0].total_rows;

   // Record the fetched window bounds the span layout derives from.
   fetchedWindowStart = columnOffset + 1;
   fetchedWindowEnd = maxDisplayColumns > 0
      ? Math.min(columnOffset + maxDisplayColumns, totalCols)
      : totalCols;
};

var initGrid = function(resCols, data) {
   if (resCols.error) {
      showError(resCols.error);

      // Unwind the bootstrap: leaving the flag set would silently swallow
      // every subsequent column-pagination action once the session recovers.
      bootstrapping = false;
      return;
   }

   // This bootstrap succeeded; clear any error overlay left by a previously
   // failed one so the rebuilt grid is actually visible.
   hideError();

   var loc = parseLocationUrl();

   // Determine max display columns. Resolved before installColumnResponse so the
   // column signature and fetched-window bounds it computes use the final value.
   if (loc.maxCols) {
      maxDisplayColumns = loc.maxCols > 0 ? loc.maxCols : resCols.length;
   } else if (loc.maxDisplayColumns > 0) {
      maxDisplayColumns = loc.maxDisplayColumns;
   } else {
      maxDisplayColumns = resCols.length;
   }

   if (loc.maxRows > 0) {
      maxRows = loc.maxRows;
   }

   installColumnResponse(resCols);

   // Apply the data_viewer_show_summary preference as the default, but only
   // on the first bootstrap of this frame. When applySavedState will restore
   // a saved sidebar choice (its fingerprint matches the current frame), skip
   // the overwrite so the saved choice wins. Once the default has been
   // resolved, never re-apply it: a re-bootstrap from a data refresh or
   // column pagination must keep the user's live choice, even when a
   // column-structure change (e.g. `df$new <- ...`) flipped the fingerprint
   // and invalidated the saved state -- otherwise adding a column would
   // silently toggle the sidebar back to the preference default.
   var savedState = pendingSavedState;
   var willRestoreSidebar = savedState &&
      typeof savedState.sidebarVisible === "boolean" &&
      savedState.columns === columnFingerprint();
   if (!willRestoreSidebar && !sidebarDefaultResolved) {
      sidebarVisible = loc.showSummary;
   }
   sidebarDefaultResolved = true;

   // Validate the primed selection (pinned/sort/filters) against the fetched
   // columns and restore window-dependent saved state. Headers are built next,
   // so pinning order must be settled here; fetchRows below needs sort/filters.
   var stateDiscarded = applySavedState(savedState);

   // If the fingerprint didn't match, the columns we requested may have
   // included pinned indices from an unrelated frame (now cleared). Re-bootstrap
   // once with a clean window so we don't display the wrong prepended columns.
   if (stateDiscarded && primedPinnedCount > 0) {
      primedPinnedCount = 0;
      pendingSavedState = null;
      bootstrap();
      return;
   }

   // Build headers (pinned columns first, then unpinned). autoSizeColumns
   // computes per-column widths from data, picks the initial column window,
   // and builds the windowed header row (applying widths + sort indicators).
   // It re-runs after the first row fetch below to refine widths with real
   // cell values.
   columnOrder = getColumnOrder();
   var thead = document.getElementById("data_cols");
   thead.innerHTML = "";
   autoSizeColumns();

   // Initialize sidebar
   initSidebar();

   // Handle data import mode (data provided directly)
   if (data) {
      initWithData(data);
      autoSizeColumns();
      applyPinnedColumns();
   } else {
      // Server mode: fetch initial batch, then auto-size columns
      fetchRows(0, FETCH_SIZE, function() {
         autoSizeColumns();
         applyPinnedColumns();
         restoreScrollAfterRefresh();
         // Saved state may have restored filters/search; the sidebar built
         // above shows whole-frame stats, so refresh it to the filtered view.
         if (hasActiveRowFilter())
            refreshSidebarSummaries();
      });
   }

   // Set up scroll handler. addEventListener is idempotent on the same
   // function reference, so the module-scoped listeners declared below can
   // be re-registered safely even if a previous bootstrap is still in flight.
   var viewport = document.getElementById("gridViewport");
   if (viewport) {
      viewport.addEventListener("scroll", onScroll);
      viewport.addEventListener("scroll", onScrollbarUpdate);
      viewport.addEventListener("scrollend", onScrollEnd);

      // Keyboard navigation + cell click. tabindex=0 makes the viewport
      // focusable so the keydown listener fires; clicking a cell focuses
      // it explicitly (preventScroll keeps the visual context stable).
      viewport.setAttribute("tabindex", "0");
      viewport.addEventListener("keydown", onGridKeyDown);
   }
   var gridBody = document.getElementById("gridBody");
   if (gridBody) {
      gridBody.addEventListener("click", onGridCellClick);
   }

   // Create custom scrollbars
   createCustomScrollbars();
   updateCustomScrollbars();

   // Resize handler -- recompute pinned overscroll padding too, since it
   // depends on viewport width.
   window.addEventListener("resize", onResize);

   // Sync the host's latched-state mirror with sidebarVisible after URL
   // params + saved state have both resolved -- the callback may have
   // been registered before bootstrap ran with a different default.
   if (window.sidebarStateCallback) window.sidebarStateCallback(sidebarVisible);

   // Reveal the filter row when saved state restored active per-column filters,
   // so they're visible and editable instead of being applied with no UI
   // (#17830). Filter-row visibility itself isn't persisted -- it's derived
   // from whether any filter values came back. setFilterUIVisible records a
   // post-init action, so this also survives column-pagination re-bootstraps;
   // the replay loop below applies it once the headers are built.
   if (Object.keys(cachedFilterValues).length > 0) {
      window.setFilterUIVisible(true);
   }

   // Sync the host's filter latch with the resolved filter-row visibility,
   // mirroring the sidebar callback above. The callback may have been
   // registered before bootstrap resolved the restored filters.
   if (window.filterStateCallback) window.filterStateCallback(isFilterUIVisible());

   // Run post-init actions
   for (var actionName in postInitActions) {
      if (postInitActions[actionName]) {
         postInitActions[actionName]();
      }
   }

   bootstrapping = false;
};

var initWithData = function(data) {
   // Data import preview mode: all data provided at once
   if (data.length === 0 || (data[0] && data[0].length === 0)) {
      totalRows = 0;
      filteredRows = 0;
      return;
   }

   // data is column-major: data[colIdx][rowIdx]
   var numRows = data[0].length;
   totalRows = numRows;
   filteredRows = numRows;

   for (var r = 0; r < numRows; r++) {
      var row = [r + 1]; // 1-based row index (0 is the NA sentinel)
      for (var c = 0; c < data.length; c++) {
         row.push(data[c][r]);
      }
      rowCache.set(r, row);
   }

   // Preview mode never fetches; mark every block complete for the current
   // column set so blockIsCurrent doesn't report them as needing one.
   for (var b = 0; b < numRows; b += FETCH_SIZE) {
      blockColsSig.set(b, colsSig);
   }

   renderVisibleRows();
   updateInfoBar();
};

// Reset all per-grid transient state so a fresh bootstrap starts clean.
// Persistent configuration (rowNumbers, ordering, displayNullsAsNAs, viewer
// callbacks, sidebarVisible) is intentionally not touched here -- those
// survive across data refreshes.
var resetGridState = function() {
   // Scroll position: cleared together with the viewport scrollTop reset in
   // destroyGrid so a stale value can't be restored by onActivate after a
   // refresh.
   lastScrollTop = 0;
   lastScrollLeft = 0;

   // Data
   cols = null;
   colsSig = "";
   // Invalidate the cached column count. buildRequestedColumns clamps the
   // requested window to totalCols, so a stale value left over from the
   // previous frame would cap the fetch at the old column count and drop any
   // columns added since (e.g. `df$new <- ...`). 0 means "unknown"; the window
   // is then requested in full and the server clamps it to the real frame.
   // Reset here (rather than in invalidateCache) because resetGridState only
   // runs from bootstrap, which always re-fetches columns and repopulates this.
   totalCols = 0;
   fetchedWindowStart = 1;
   fetchedWindowEnd = 0;
   slideInFlightGen = 0;
   sortColumn = -1;
   sortDirection = "";
   cachedSearch = "";
   cachedFilterValues = {};
   pinnedColumns.clear();
   columnOrder = [];
   activeRow = -1;
   activeCol = -1;
   activeHeaderCol = -1;
   // The viewport persists across destroyGrid; clear its activedescendant so
   // it can't reference an id whose td/th was just removed from the DOM.
   setViewportActiveDescendant(null);

   // Column widths
   measuredWidths = [];
   manualWidths = {};
   origColWidths = [];
   totalTableWidth = 0;

   // Render window
   renderStart = 0;
   renderEnd = 0;
   colWinStart = -1;
   colWinEnd = -1;
   // Injected header-UI registry is scoped to a single grid lifecycle (it
   // drives reinjectHeaderUI as the column window slides). The next bootstrap
   // re-populates it from the postInitActions replay, so drop it here rather
   // than letting it leak header controls into a reset/refreshed grid.
   activeHeaderUIs = {};
   renderedRowElements.clear();
   topSpacerRow = null;
   bottomSpacerRow = null;
   cachedPinnedOffsets = {};
   invalidatePinnedOffsets();
   needsAutoSize = false;
   deferredHeaderRebuild = false;

   // Resize -- also clear the body class in case a drag was in progress when
   // teardown was triggered.
   didResize = false;
   resizingColIdx = null;
   initResizeX = null;
   initResizingWidth = null;
   origTableWidth = null;
   resizingBoundsExceeded = 0;
   if (typeof document !== "undefined" && document.body) {
      document.body.classList.remove("col-resizing");
   }

   // Popups
   if (dismissActivePopup) dismissActivePopup(true);
   dismissActivePopup = null;
   columnsPopup = null;
   activeColumnInfo = {};
   // A refresh can rename columns; refetch names next time they're needed.
   columnNamesCache = null;
   // Filtered summaries are recomputed on the next filter/search after the
   // rebuild; start clean so a stale map can't drive the fresh sidebar.
   filteredSummaries = null;
   filteredSummariesRowCount = 0;

   // Complete-index sidebar state: the column set and any lazily-fetched
   // summaries belong to the previous frame; drop them so the rebuilt sidebar
   // re-fetches cleanly.
   sidebarColumns = null;
   sidebarColumnsFetching = false;
   sidebarLazySummaries = {};
   filterDescriptors = {};
   sidebarPendingFetch = {};
   if (sidebarStatsPopup) dismissSidebarStats();
};

// ==========================================================================
// State Persistence
// ==========================================================================
//
// Per-object UI state (pinned columns, sidebar visibility, manual column
// widths, sort order, filter values) is saved to localStorage keyed by
// env+obj. State is loaded on bootstrap and applied before the first row
// fetch so it shows up in the initial render. Saves are debounced.
//
// STATE_VERSION / STATE_KEY_PREFIX are declared with the rest of the
// module-scoped constants up top.

var stateKey = function() {
   var loc = parseLocationUrl();
   if (!loc.obj) return null;
   // Encode each component so colons inside an env or obj name can't collide
   // with the separator, and an empty env (common for global-env objects)
   // simply becomes an empty segment without a sentinel.
   return STATE_KEY_PREFIX +
      encodeURIComponent(loc.env || "") + ":" +
      encodeURIComponent(loc.obj);
};

// Fingerprint of the underlying frame's column structure. The server
// computes this from the full column names (not the paginated slice) and
// returns it on the rownames metadata, so the value stays stable as the
// user pages through columns. Anything that mutates names(x) on the
// server (reassignment, `names(df) <- ...`, `df$new <- ...`) flips the
// fingerprint; applySavedState uses it to ignore state saved against an
// incompatible frame -- otherwise width/sort/filter indices and per-
// column filter values would be applied silently to mismatched columns.
//
// Falls back to a per-load random sentinel when the server omits the
// fingerprint, so missing data always mismatches saved state instead of
// silently passing the equality check.
var columnFingerprint = function() {
   if (!cols || !cols[0]) return MISSING_FINGERPRINT_SENTINEL;
   var fp = cols[0].cols_fingerprint;
   return typeof fp === "string" && fp.length > 0
      ? fp
      : MISSING_FINGERPRINT_SENTINEL;
};

var saveState = function() {
   var key = stateKey();
   if (!key) return;
   // Don't persist before the grid is initialized: columnFingerprint() would
   // fall back to its missing-data sentinel and totalRows would be stale, so the
   // entry could never validate on reload. (saveState now also fires from scroll
   // settling and tab deactivation, either of which can race an early teardown.)
   if (!cols) return;
   // pinnedColumns/sort/filters/manualWidths are stored by absolute column
   // identity (col_index in the full frame), so they survive column
   // pagination.
   var state = {
      version: STATE_VERSION,
      columns: columnFingerprint(),
      pinnedColumns: Array.from(pinnedColumns),
      sidebarVisible: sidebarVisible,
      manualWidths: Object.assign({}, manualWidths),
      sort: sortColumn >= 0 ? { col: sortColumn, dir: sortDirection } : null,
      filters: Object.assign({}, cachedFilterValues),
      // Scroll offset, keyed to the unfiltered row count so a frame whose size
      // changed while closed reopens at the top -- the same guard refreshes use
      // (see restoreScrollAfterRefresh). lastScroll* is kept current by onScroll/
      // onScrollEnd/onDeactivate, so it's the live position at any save.
      scroll: { top: lastScrollTop, left: lastScrollLeft, rows: totalRows }
   };
   try {
      localStorage.setItem(key, JSON.stringify(state));
   } catch (e) {
      // localStorage may be unavailable (private browsing), full, or
      // blocked by a stricter site policy. Warn once per session so a
      // confused user has something to grep for; silent failure here
      // means pin/sort/filter state appears to silently revert on reload.
      if (!persistWarned) {
         persistWarned = true;
         console.warn("Data viewer: failed to persist UI state to localStorage:", e);
      }
   }
};

var loadSavedState = function() {
   var key = stateKey();
   if (!key) return null;
   var raw;
   try {
      raw = localStorage.getItem(key);
   } catch (e) {
      return null;
   }
   if (!raw) return null;
   try {
      var state = JSON.parse(raw);
      if (state.version !== STATE_VERSION) {
         // Stale schema; remove so it doesn't accumulate forever.
         // Log once so support can correlate user reports of "my pins
         // disappeared after upgrade" with the schema bump.
         try { localStorage.removeItem(key); } catch (_) {}
         console.info(
            "Data viewer: discarding saved state from older schema (v" +
            state.version + " -> v" + STATE_VERSION + ")");
         return null;
      }
      return state;
   } catch (e) {
      // Corrupt JSON; drop it.
      try { localStorage.removeItem(key); } catch (_) {}
      return null;
   }
};

var clearSavedState = function() {
   var key = stateKey();
   if (!key) return;
   try { localStorage.removeItem(key); } catch (e) { /* quota / disabled storage */ }
};

// Populate the pinned/sort/filter selection from saved state before `cols` is
// available. These are all keyed by absolute column identity, so they can be
// resolved without the fetched columns -- which is what lets the column
// request include pinned columns outside the visible window. The fingerprint
// cannot be checked yet (it comes from the server response), so applySavedState
// validates it once `cols` arrives and clears the selection on a mismatch.
var primeSelectionState = function() {
   pendingSavedState = loadSavedState();
   pinnedColumns.clear();
   sortColumn = -1;
   sortDirection = "";
   cachedFilterValues = {};
   primedPinnedCount = 0;

   var state = pendingSavedState;
   if (!state)
      return;

   if (Array.isArray(state.pinnedColumns)) {
      state.pinnedColumns.forEach(function(idx) {
         if (typeof idx === "number" && idx >= 1)
            pinnedColumns.add(idx);
      });
   }
   primedPinnedCount = pinnedColumns.size;

   if (state.sort &&
       typeof state.sort.col === "number" && state.sort.col >= 1 &&
       (state.sort.dir === "asc" || state.sort.dir === "desc")) {
      sortColumn = state.sort.col;
      sortDirection = state.sort.dir;
   }

   if (state.filters && typeof state.filters === "object") {
      for (var absIdx in state.filters) {
         if (!state.filters.hasOwnProperty(absIdx))
            continue;
         var val = state.filters[absIdx];
         if (typeof val === "string" && val.length > 0)
            cachedFilterValues[absIdx] = val;
      }
   }
};

// Validate the primed selection against the freshly fetched columns and apply
// the window-dependent saved state (sidebar, manual widths). Must be called
// after `cols`/`totalCols` are populated but before the first row fetch (so
// sort/filters are sent) and before headers are built (so pinning order is
// correct). Returns true if the saved state was discarded as incompatible.
var applySavedState = function(state) {
   if (!state || !cols)
      return false;
   // Drop state saved against a different column structure: applying it to an
   // unrelated frame would corrupt the user's view (e.g. a typed filter string
   // landing on a numeric column) with no obvious recovery. Fail-closed: if
   // state.columns is missing or non-string (older format, corrupt payload),
   // discard rather than apply blind.
   if (typeof state.columns !== "string" ||
       state.columns !== columnFingerprint()) {
      clearSavedState();
      pinnedColumns.clear();
      sortColumn = -1;
      sortDirection = "";
      cachedFilterValues = {};
      return true;
   }

   // Selection (pins/sort/filters) was primed by primeSelectionState; clamp it
   // to the frame now that the column count is known.
   pinnedColumns.forEach(function(absIdx) {
      if (absIdx > totalCols)
         pinnedColumns.delete(absIdx);
   });
   if (sortColumn > totalCols) {
      sortColumn = -1;
      sortDirection = "";
   }

   if (typeof state.sidebarVisible === "boolean") {
      sidebarVisible = state.sidebarVisible;
   }

   // manualWidths is keyed by absolute column index. Legacy array-shaped
   // payloads were positional within a column page and could land widths on
   // the wrong columns after pagination, so they're ignored rather than
   // misapplied (the Array.isArray exclusion).
   if (state.manualWidths &&
       typeof state.manualWidths === "object" &&
       !Array.isArray(state.manualWidths)) {
      for (var key in state.manualWidths) {
         if (!state.manualWidths.hasOwnProperty(key)) continue;
         var absIdx = parseInt(key, 10);
         var w = state.manualWidths[key];
         if (!isNaN(absIdx) && absIdx >= 0 && typeof w === "number" && w > 0)
            manualWidths[absIdx] = w;
      }
   }

   // Restore the saved scroll position through the same path a refresh uses:
   // restoreScrollAfterRefresh (run after the first row fetch) re-checks the
   // row count before applying, so a frame whose row count changed while closed
   // still opens at the top. Defer to a live refresh capture if one already owns
   // the slot (a refresh-driven rebuild carries a fresher position than the
   // persisted one), and only when the fingerprint matched -- a mismatch returns
   // early above without reaching here.
   if (!activeScrollRestore &&
       state.scroll &&
       typeof state.scroll.top === "number" &&
       typeof state.scroll.left === "number" &&
       typeof state.scroll.rows === "number") {
      activeScrollRestore = {
         top: state.scroll.top,
         left: state.scroll.left,
         rows: state.scroll.rows
      };
   }

   return false;
};

// Apply asc/desc CSS classes and aria-sort on header cells based on current
// sort state. The single render path for sort indicators: used by
// handleSortClick / clearSort and after the header window is rebuilt (e.g.
// when restoring saved state).
var applySortIndicators = function() {
   var thead = document.getElementById("data_cols");
   if (!thead) return;
   for (var i = 0; i < thead.children.length; i++) {
      var th = thead.children[i];
      var colIdx = parseInt(th.getAttribute("data-col-idx"), 10);
      // Skip spacer cells (col-spacer) that stand in for off-window columns.
      if (isNaN(colIdx)) continue;
      th.classList.remove("sorting", "sorting_asc", "sorting_desc");
      var sorted = absColIndex(colIdx) === sortColumn && sortDirection;
      if (sorted) {
         th.classList.add("sorting_" + sortDirection);
      } else {
         th.classList.add("sorting");
      }
      // Only sortable headers carry aria-sort (set in createHeader).
      if (th.hasAttribute("aria-sort")) {
         th.setAttribute("aria-sort",
            sorted ? sortAriaValue(sortDirection) : "none");
      }
   }
};

var destroyGrid = function() {
   resetGridState();
   invalidateCache();
   destroyCustomScrollbars();

   // Clear DOM
   var thead = document.getElementById("data_cols");
   if (thead) thead.innerHTML = "";
   var tbody = document.getElementById("gridBody");
   if (tbody) { tbody.innerHTML = ""; }

   var infoText = document.getElementById("rsGridData_info_text");
   if (infoText) infoText.textContent = "";
   setSortStatus("");

   var viewport = document.getElementById("gridViewport");
   if (viewport) {
      viewport.removeEventListener("scroll", onScroll);
      viewport.removeEventListener("scroll", onScrollbarUpdate);
      viewport.removeEventListener("scrollend", onScrollEnd);
      viewport.scrollTop = 0;
      viewport.scrollLeft = 0;
   }
   window.removeEventListener("resize", onResize);
   if (pendingScrollbarRaf) {
      cancelAnimationFrame(pendingScrollbarRaf);
      pendingScrollbarRaf = 0;
   }
   onScroll.cancel();
};

var bootstrap = function(data) {
   bootstrapping = true;
   var generation = ++bootstrapGeneration;

   // Take ownership of any pending scroll-restore intent for this rebuild only.
   // Clearing the handoff slot here (rather than relying on the restore callback
   // to consume it) means a prior refresh that aborted before its callback ran
   // cannot leak its captured position onto this unrelated bootstrap.
   activeScrollRestore = pendingScrollRestore;
   pendingScrollRestore = null;

   destroyGrid();

   // Prime the pinned/sort/filter selection from saved state before the column
   // fetch so the request can include pinned columns outside the visible
   // window. The fingerprint is validated once `cols` arrives (initGrid ->
   // applySavedState); a mismatch clears the primed selection.
   primeSelectionState();

   if (!data) {
      fetchColumns(function(result) {
         // A newer bootstrap superseded this one while the column fetch was
         // in flight; let that bootstrap's own response drive initGrid.
         if (generation !== bootstrapGeneration) return;
         initGrid(result);
      });
   } else {
      // Data provided directly (data import preview)
      if (data.columns) {
         initGrid(data.columns, data.data);
      } else {
         // Malformed or partial preview payload. destroyGrid() above already
         // wiped the table, so surface an error rather than leaving a blank
         // viewer (and a stranded bootstrapping flag) with no indication.
         bootstrapping = false;
         showError("The data preview is unavailable or could not be displayed.");
      }
   }
};

// ==========================================================================
// Filter/Header UI Visibility
// ==========================================================================

// `markerClass` distinguishes the different injected UIs (filter vs.
// column-definitions) so toggling one doesn't accidentally remove the
// other; it also makes the show path idempotent -- a stray duplicate
// setFilterUIVisible(true) clears any existing instances before adding,
// instead of stacking copies on top of each other.
//
// postInitActions keys are also marker-scoped so a queued
// setFilterUIVisible(true) doesn't get clobbered by a queued
// setColumnDefinitionsUIVisible(true).
var setHeaderUIVisible = function(visible, initialize, hide, markerClass) {
   var thead = document.getElementById("data_cols");

   // Record the desired visibility so it's reapplied after a bootstrap
   // (column pagination / refresh): initGrid replays postInitActions, which is
   // the single source of truth for header-UI visibility across grid reloads.
   // Recorded unconditionally -- not just when cols is null -- so a filter
   // enabled after the grid is ready also survives column paging, rather than
   // relying on activeHeaderUIs (which is cleared on teardown).
   postInitActions["setHeaderUIVisible:" + markerClass] = visible
      ? function() { setHeaderUIVisible(true, initialize, hide, markerClass); }
      : null;

   if (thead === null || cols === null) {
      return false;
   }

   // Remember (or forget) this UI so reinjectHeaderUI re-applies it to headers
   // created as the column window slides within the current render. Cleared on
   // teardown (resetGridState); the postInitActions replay above re-populates
   // it on the next bootstrap.
   if (visible) {
      activeHeaderUIs[markerClass] = initialize;
   } else {
      delete activeHeaderUIs[markerClass];
   }

   if (!visible && hide) {
      hide(thead);
      if (dismissActivePopup) dismissActivePopup(true);
   }

   for (var i = 0; i < thead.children.length; i++) {
      var th = thead.children[i];
      var colIdx = parseInt(th.getAttribute("data-col-idx"), 10);
      // Skip spacer cells (col-spacer), which stand in for off-window columns
      // and carry no data-col-idx.
      if (isNaN(colIdx)) continue;
      var col = cols[colIdx];

      // Always strip any existing instances of this marker first so
      // visible=true is idempotent and visible=false fully cleans up
      // (querySelector would have only handled one match).
      var existing = th.querySelectorAll("." + markerClass);
      for (var k = 0; k < existing.length; k++) {
         th.removeChild(existing[k]);
      }

      if (visible) {
         var el = initialize(th, col, colIdx);
         if (el) {
            el.classList.add(markerClass);
            th.appendChild(el);
         }
      }
   }

   renderVisibleRows(true);
   updateCustomScrollbars();
   return true;
};

// ==========================================================================
// Column Window Sliding (pagination without a rebuild)
// ==========================================================================

// Capture the column under the viewport's left edge (and its on-screen pixel
// offset) so a relayout can restore the user's visual position even though
// column widths -- estimated for unfetched spans, measured for fetched ones --
// change as the window slides.
var captureScrollAnchor = function() {
   var viewport = document.getElementById("gridViewport");
   if (!viewport || !cols) return null;
   var pinnedW = getPinnedOffsets().totalWidth;
   var abs = absColAtContentX(viewport.scrollLeft + pinnedW);
   return { abs: abs, offsetPx: layoutXOfAbs(abs) - viewport.scrollLeft };
};

var restoreScrollAnchor = function(anchor) {
   var viewport = document.getElementById("gridViewport");
   if (!viewport || !anchor) return;
   setViewportScrollLeft(viewport, layoutXOfAbs(anchor.abs) - anchor.offsetPx);
};

// Swap the grid over to a freshly fetched column window without tearing the
// grid down. Everything a bootstrap would reset survives: scroll position,
// pins/sort/filters (live state, not re-read from storage), the sidebar's
// visibility, header-attached UI, and the row cache (remapped so overlap
// columns -- rownames, pinned, any shared window -- render immediately while
// the new columns' data is refetched).
//
// options.targetAbs, when given, scrolls the viewport so that absolute column
// is centered in the unpinned viewport region -- used by the pagination
// buttons and go-to-column. Otherwise the current visual position is preserved
// via a scroll anchor.
var applyColumnWindowUpdate = function(resCols, options) {
   var targetAbs = options && options.targetAbs > 0 ? options.targetAbs : -1;
   var anchor = targetAbs > 0 ? null : captureScrollAnchor();

   // Capture active cell/header identity before swapping `cols`
   // (absColIndex resolves through it); remapped to the new window below.
   // installColumnResponse refines resCols but doesn't touch the active `cols`,
   // so these captures still read the old window.
   var activeCellAbs = (activeRow >= 0 && activeCol >= 0)
      ? absColIndex(columnOrder[activeCol]) : -1;
   var activeHeaderAbs = (activeHeaderCol >= 0)
      ? absColIndex(columnOrder[activeHeaderCol]) : -1;

   var oldCols = cols;
   installColumnResponse(resCols);

   // Carry cached rows over to the new column alignment, then retire the
   // old window's in-flight fetches: their rows would land misaligned.
   remapRowCache(oldCols, cols);

   // Drop every block signature that doesn't match the new window. remapRowCache
   // wipes a remapped block's non-overlap cells but leaves its stored signature
   // alone; without this purge a jump-away-and-back gesture (window A -> B -> A)
   // restores the original colsSig, so a block wiped during the round trip would
   // read as current (blockColsSig === colsSig) while holding undefined cells --
   // rendering a permanently blank band until an unrelated invalidateCache.
   // Removing the non-matching entry forces such blocks to refetch when scrolled
   // back into view (the remapped rows stay in rowCache for skeleton rendering).
   blockColsSig.forEach(function(sig, blockStart) {
      if (sig !== colsSig)
         blockColsSig.delete(blockStart);
   });

   pendingFetches.forEach(function(controller) { controller.abort(); });
   pendingFetches.clear();
   drawCounter++;

   columnOrder = getColumnOrder();
   invalidatePinnedOffsets();

   // Re-resolve the active cell/header in the new window; columns that
   // slid out of the fetched set lose their highlight.
   var remapDisplayIdx = function(absIdx) {
      if (absIdx < 0) return -1;
      var pos = posForAbsColIndex(absIdx);
      return pos >= 0 ? columnOrder.indexOf(pos) : -1;
   };
   activeCol = remapDisplayIdx(activeCellAbs);
   if (activeCol < 0) activeRow = -1;
   activeHeaderCol = remapDisplayIdx(activeHeaderAbs);

   // The header row must be rebuilt for the new window, but autoSizeColumns
   // defers the rebuild while a filter editor is open (to protect it from
   // teardown mid-edit). Close any open editor instead -- its column may not
   // even exist in the new window.
   if (dismissActivePopup) dismissActivePopup(true);
   var activeEl = document.activeElement;
   if (activeEl && activeEl.classList && activeEl.classList.contains("textFilterBox"))
      activeEl.blur();
   deferredHeaderRebuild = false;

   // Rebuild layout for the new window: widths, headers (autoSizeColumns
   // re-injects any active header UI) and pinned offsets.
   autoSizeColumns();
   applyPinnedColumns();

   // The sidebar is a complete index of the WHOLE frame (sidebarColumns), so a
   // window slide does NOT change which entries it lists -- only which columns
   // now have window-backed summaries, and for unfiltered data those are
   // identical to the lazily-cached summaries the entries already show. Once the
   // complete index has loaded, rebuilding the entire entry list (initSidebar)
   // on every slide is pure DOM churn and a real scroll-time cost (it dominated
   // slide profiles), so skip it: pin/sort/filter indicators are frame-global
   // and unchanged by a scroll, and entries that slid into the window are filled
   // in lazily as they scroll into the sidebar viewport (flushSidebarPendingFetch
   // now populates already-available summaries, not just freshly-fetched ones).
   //
   // The full rebuild is still required before the index loads, when the listed
   // entries ARE the fetched window and therefore change with the slide.
   if (sidebarColumns === null) {
      var sidebarContent = document.getElementById("sidebarContent");
      var sidebarScrollTop = sidebarContent ? sidebarContent.scrollTop : 0;
      initSidebar();
      if (sidebarContent)
         sidebarContent.scrollTop = sidebarScrollTop;
   }

   // When a filter is active the new window's columns aren't in the filtered-
   // summary map yet; refetch so their sidebar entries reflect the filter rather
   // than whole-frame stats. This rebuilds the sidebar asynchronously when the
   // summaries land, so it never blocks the slide.
   if (hasActiveRowFilter())
      refreshSidebarSummaries();

   // Restore the user's visual position against the new layout: either pin
   // the requested column centered in the unpinned region (pagination buttons)
   // or re-derive scrollLeft from the anchor captured before the relayout. Then
   // recompute the render window against the settled scroll position.
   var viewport = document.getElementById("gridViewport");
   if (targetAbs > 0 && viewport) {
      // Center the target in the unpinned viewport region (matching
      // revealColumnCentered for already-fetched targets).
      var pinnedW = getPinnedOffsets().totalWidth;
      var tPos = posForAbsColIndex(targetAbs);
      var tOrderPos = tPos >= 0 ? columnOrder.indexOf(tPos) : -1;
      var tWidth = tOrderPos >= 0
         ? (measuredWidths[tOrderPos] || DEFAULT_COL_WIDTH)
         : DEFAULT_COL_WIDTH;
      var region = Math.max(0, viewport.clientWidth - pinnedW);
      setViewportScrollLeft(viewport,
         layoutXOfAbs(targetAbs) - pinnedW - Math.max(0, (region - tWidth) / 2));
   } else if (!anyScrollbarDragging()) {
      // Don't fight an in-progress scrollbar drag for the scroll position;
      // the drag is the user's statement of where they want to be.
      restoreScrollAnchor(anchor);
   }
   syncColumnWindow();

   // Sync the viewport's aria-activedescendant with the remapped active
   // cell/header (cleared when neither survived the slide).
   if (activeRow >= 0 && activeCol >= 0) {
      setViewportActiveDescendant(activeCellId(activeRow, activeCol));
   } else {
      var activeTh = getActiveHeaderTh();
      setViewportActiveDescendant(activeTh ? activeTh.id : null);
   }

   // Refetch the first visible block with a width-refinement callback (the
   // initial autoSize above could only sample overlap cells), then render
   // the remapped skeleton; renderVisibleRows issues fetches for any other
   // incomplete visible blocks.
   var scrollTop = viewport ? viewport.scrollTop : 0;
   var firstVisible = Math.floor(scrollTop / ROW_HEIGHT);
   var blockStart =
      Math.floor(Math.max(0, firstVisible - BUFFER_ROWS) / FETCH_SIZE) * FETCH_SIZE;
   fetchRows(blockStart, FETCH_SIZE, function() {
      autoSizeColumns();
      applyPinnedColumns();
   });

   renderVisibleRows(true);
   updateInfoBar();
   updateCustomScrollbars();

   // A targeted jump flashes its landing column (header + sidebar entry) so
   // the user can spot where it landed.
   if (targetAbs > 0) {
      var targetPos = posForAbsColIndex(targetAbs);
      if (targetPos >= 0) {
         flashColumnHeader(targetPos);
         flashSidebarColumn(targetAbs);
      }
   }

   // The user may have kept scrolling while this window was in flight;
   // immediately evaluate whether another slide is already warranted.
   maybeSlideForScroll();
};

// Fetch column metadata for the current columnOffset/maxDisplayColumns and
// apply it incrementally. Shares the bootstrap generation token so a slide
// superseded by a newer slide or a full bootstrap (data refresh) is dropped.
var slideColumnWindow = function(options) {
   if (bootstrapping || cols === null)
      return;

   var generation = ++bootstrapGeneration;
   slideInFlightGen = generation;
   fetchColumns(function(result) {
      // Clear the in-flight gate before the staleness check so a superseded
      // or failed slide can't permanently block scroll-driven slides.
      if (slideInFlightGen === generation)
         slideInFlightGen = 0;
      if (generation !== bootstrapGeneration) return;
      if (result.error) {
         // The slide advanced columnOffset before fetching; on failure roll it
         // back to the still-current fetched window. Otherwise columnOffset and
         // fetchedWindowStart stay desynced and maybeSlideForScroll's
         // "newOffset === columnOffset" guard treats an identical retry as a
         // no-op, leaving the viewport parked over blank span columns.
         columnOffset = fetchedWindowStart - 1;
         showError(result.error);
         return;
      }
      hideError();
      applyColumnWindowUpdate(result, options);
   });
};

// Scroll-driven sliding: when the visible column range pokes outside the
// fetched window (i.e. unfetched span columns are on screen), recenter the
// window on the viewport. Called from the scroll handlers and again when a
// slide lands. At most one metadata fetch is in flight at a time
// (slideInFlightGen); the landing slide re-evaluates, so fast scrubbing
// converges on the final position with a short chain of fetches rather than
// one per scroll event.
//
// The trigger is deliberately strict -- visible range OUTSIDE the window, not
// merely near its edge -- so that a pagination jump (which lands the view
// exactly at the window's left edge by design) doesn't immediately recenter
// itself away from the page the user asked for.
var maybeSlideForScroll = function() {
   if (!cols || bootstrapping || slideInFlightGen || dataMode !== "server")
      return;
   if (totalCols <= 0 || maxDisplayColumns <= 0 || totalCols <= maxDisplayColumns)
      return;
   // Layout (and thus the x -> column mapping) isn't meaningful until the
   // first width measurement has run.
   if (measuredWidths.length === 0)
      return;

   var viewport = document.getElementById("gridViewport");
   if (!viewport) return;

   var pinnedW = getPinnedOffsets().totalWidth;
   var vLo = absColAtContentX(viewport.scrollLeft + pinnedW);
   var vHi = absColAtContentX(viewport.scrollLeft + viewport.clientWidth);

   var needSlide =
      (vLo < fetchedWindowStart && fetchedWindowStart > 1) ||
      (vHi > fetchedWindowEnd && fetchedWindowEnd < totalCols);
   if (!needSlide) return;

   var center = Math.round((vLo + vHi) / 2);
   var newOffset = Math.max(0, Math.min(totalCols - maxDisplayColumns,
      center - Math.floor(maxDisplayColumns / 2) - 1));
   if (newOffset === columnOffset) return;

   columnOffset = newOffset;
   slideColumnWindow();
};

// Jump to an absolute (1-based) column. A column already in the fetched set
// scrolls into view directly; anything else slides the window to contain the
// target (centered, clamping the window start at the end of the frame) and
// centers it in the viewport. Either way the landing header flashes briefly.
var goToColumn = function(column) {
   if (bootstrapping) return;

   // A failed bootstrap leaves no grid (cols === null, only possible here
   // when not bootstrapping) and an unknown totalCols (0). Treat any nav
   // action on a dead grid as "retry": the bootstrap re-fetches the current
   // window and repopulates totalCols.
   if (cols === null) {
      bootstrap();
      return;
   }

   var abs = Math.round(column);
   if (!isFinite(abs) || abs < 1) return;
   if (totalCols > 0) abs = Math.min(abs, totalCols);

   // Already fetched: bring it into view directly (centered, with the
   // highlight flash).
   var pos = posForAbsColIndex(abs);
   if (pos >= 0) {
      revealColumnCentered(pos);
      return;
   }

   // Center the fetched window on the target as well as the viewport: a
   // jump wants context on both sides, and a window starting at the target
   // would put unfetched (blank) span columns to its immediate left.
   if (maxDisplayColumns <= 0) return;
   columnOffset = Math.max(0, Math.min(totalCols - maxDisplayColumns,
      abs - 1 - Math.floor(maxDisplayColumns / 2)));
   slideColumnWindow({ targetAbs: abs });
};

// Bring a fetched column (cols position) into view for a go-to jump: if it
// is already fully visible just flash it; otherwise scroll it to the center
// of the unpinned viewport region (a jump wants context on both sides --
// the same convention as go-to-line in editors).
var revealColumnCentered = function(colIdx) {
   var pos = columnOrder.indexOf(colIdx);
   if (pos < 0) return;

   // Center the column in the unpinned viewport region (no-op when it's
   // already fully visible); bring the new window into the DOM when it scrolls.
   if (scrollColumnPosIntoView(pos, true)) {
      if (syncColumnWindow()) renderVisibleRows(true);
   }
   flashColumnHeader(colIdx);
   // flashSidebarColumn keys off the absolute index; colIdx here is a cols
   // position, so map it through absColIndex.
   flashSidebarColumn(absColIndex(colIdx));
};

// ==========================================================================
// Go To Column matching
// ==========================================================================
//
// Backs the host toolbar's go-to-column search box (a typeahead in the
// spirit of Go to File/Function, living in the GWT toolbar): the box asks
// for matches via window.matchColumns and jumps via window.goToColumn.

var GOTO_MAX_RESULTS = 12;

// Sparse fallback name list built from the fetched columns (index abs-1 ->
// name), used when the whole frame's names are unavailable (fetch still in
// flight or unsupported, e.g. data-import preview mode).
var namesFromCols = function() {
   var arr = [];
   if (!cols) return arr;
   for (var i = 1; i < cols.length; i++) {
      var abs = cols[i].col_index;
      if (typeof abs === "number" && abs >= 1)
         arr[abs - 1] = cols[i].col_name;
   }
   return arr;
};

// Match a query against the frame's column names: numeric queries offer a
// direct index jump first; name matches rank prefix matches ahead of
// substring matches, in column order, capped at GOTO_MAX_RESULTS.
var buildGoToMatches = function(query, names) {
   var matches = [];
   var q = query.trim();
   if (q.length === 0)
      return matches;

   if (/^\d+$/.test(q)) {
      var idx = parseInt(q, 10);
      if (idx >= 1) {
         if (totalCols > 0)
            idx = Math.min(idx, totalCols);
         var idxName = (names && names[idx - 1]) || "";
         matches.push({ idx: idx, name: idxName, isIndexJump: true });
      }
   }

   if (names) {
      var qLower = q.toLowerCase();
      var starts = [], contains = [];
      for (var i = 0; i < names.length; i++) {
         if (starts.length >= GOTO_MAX_RESULTS)
            break;
         var nm = names[i];
         if (!nm) continue;
         var at = String(nm).toLowerCase().indexOf(qLower);
         if (at === 0)
            starts.push({ idx: i + 1, name: nm });
         else if (at > 0 && contains.length < GOTO_MAX_RESULTS)
            contains.push({ idx: i + 1, name: nm });
      }
      var byName = starts.concat(contains);
      for (var j = 0; j < byName.length && matches.length < GOTO_MAX_RESULTS; j++) {
         matches.push(byName[j]);
      }
   }
   return matches;
};

// Resolve ranked matches for a go-to-column query, fetching the frame's
// full name list on first use. The host's search box calls this through
// window.matchColumns; until (or unless -- e.g. import preview) the fetch
// resolves, matching falls back to the fetched window's names.
var matchColumnsAsync = function(query, callback) {
   if (!cols) {
      callback([]);
      return;
   }

   var q = String(query === null || query === undefined ? "" : query);
   if (columnNamesCache === null && dataMode === "server") {
      fetchColumnNames(function(fetched) {
         callback(buildGoToMatches(q, fetched || namesFromCols()));
      });
   } else {
      callback(buildGoToMatches(q, columnNamesCache || namesFromCols()));
   }
};

// ==========================================================================
// Window API Exports
// ==========================================================================

// True when the filter row is shown. Reads the recorded postInitActions entry
// (the source of truth that survives column-pagination re-bootstraps) rather
// than the live DOM, so it stays correct before headers are rebuilt.
var isFilterUIVisible = function() {
   return !!postInitActions["setHeaderUIVisible:" + FILTER_UI_MARKER];
};

window.setFilterUIVisible = function(visible) {
   return setHeaderUIVisible(
      visible,
      function(th, col, i) {
         if (isFilterableSearchType(col.col_search_type)) {
            // createFilterUI keys off the absolute column index, not the window
            // position `i`. Route through absColIndex(i) (= col.col_index, with
            // the documented fallback to `i` when col_index is unavailable)
            // rather than reading col.col_index raw, so a missing col_index
            // can't collapse columns onto cachedFilterValues[undefined].
            return createFilterUI(absColIndex(i), col);
         }
         return null;
      },
      function() {
         var hadFilters = Object.keys(cachedFilterValues).length > 0;
         cachedFilterValues = {};
         if (hadFilters) {
            invalidateCache();
            fetchRows(0, FETCH_SIZE);
            // Filters cleared: restore the whole-frame summaries (search may
            // still be active, in which case this re-fetches for it).
            refreshSidebarSummaries();
            updateSidebarColumnIndicators();
            saveState();
         }
      },
      FILTER_UI_MARKER
   );
};

window.setColumnDefinitionsUIVisible = function(visible, onColOpen_, onColDismiss_) {
   onColumnOpen = onColOpen_ || onColumnOpen;
   onColumnDismiss = onColDismiss_ || onColumnDismiss;

   return setHeaderUIVisible(
      visible,
      function(th, col, i) {
         return createColumnTypesUI(th, i, col);
      },
      function(thead) {
         var wrappers = thead.querySelectorAll(".columnTypeWrapper");
         wrappers.forEach(function(w) { w.remove(); });
      },
      "column-types-injected-ui"
   );
};

window.refreshData = function() {
   // Preserve the user's scroll position across the rebuild for in-place data
   // changes (restoreScrollAfterRefresh decides whether to actually restore,
   // based on the row count).
   captureScrollForRefresh();
   bootstrap();
};

window.refreshAndReset = function() {
   // Reset View deliberately returns to the top, so drop any captured position.
   clearSavedState();
   pendingScrollRestore = null;
   // Also return to the first column. bootstrap()/resetGridState don't touch
   // columnOffset (a plain refresh keeps the horizontal position and restores
   // scrollLeft), but Reset View clears the scroll position, so a stale offset
   // would leave the fetched window parked far right while the viewport sits at
   // scrollLeft 0 over the blank left spacer span -- a blank grid until the
   // user scrolls. Reset it so the window and the viewport both start at column 1.
   columnOffset = 0;
   bootstrap();
};

window.toggleSidebar = function() {
   toggleSidebar();
};

// Same staleness rule as the column filters (see watchColumnSearch): drop a
// pending search apply when the search it would overwrite was changed
// externally after scheduling (resetGridState clears cachedSearch on
// refresh/teardown).
var watchGlobalSearch = function() {
   return cachedSearch;
};

var debouncedSearch = debounce(TIMING.searchDebounce, watchGlobalSearch, function(text) {
   if (text !== cachedSearch) {
      cachedSearch = text;
      clearActiveCell();
      invalidateCache();
      fetchRows(0, FETCH_SIZE, function() {
         scrollToTop();
      });
      // Search narrows the rows the summaries describe; recompute them.
      refreshSidebarSummaries();
   }
});

window.applySearch = function(text) {
   debouncedSearch(text);
};

window.onActivate = function() {
   // Restore scroll position and re-render
   var viewport = document.getElementById("gridViewport");
   if (viewport) {
      setViewportScrollTop(viewport, lastScrollTop);
      setViewportScrollLeft(viewport, lastScrollLeft);
   }

   // Re-run auto-sizing if the initial sizing happened while the tab
   // was hidden (measurements are wrong with no layout).
   if (needsAutoSize && cols) {
      needsAutoSize = false;
      autoSizeColumns();
      applyPinnedColumns();
   }

   renderVisibleRows(true);
   // The sidebar's visible-entry window is computed from the panel's
   // clientHeight; if the grid bootstrapped while hidden that was 0 and only a
   // stub window was built, so rebuild it now that the tab has real layout.
   renderSidebarWindow(true);
   updateInfoBar();
   updateCustomScrollbars();

   // Returning to the tab is not a scroll event, so the activity-based fade
   // would leave the (auto-hide) scrollbars hidden -- briefly show them so
   // the horizontal scroll affordance is visible again. update() above has
   // already set display:none on any axis that isn't scrollable, so this
   // only reveals bars that actually have overflow.
   showScrollbars();
};

window.onDeactivate = function() {
   // Save scroll position
   var viewport = document.getElementById("gridViewport");
   if (viewport) {
      lastScrollTop = viewport.scrollTop;
      lastScrollLeft = viewport.scrollLeft;
   }

   // Leaving the tab is a natural persist point: capture the final position to
   // localStorage in case the gesture that put us here didn't emit a scrollend
   // (e.g. a programmatic or interrupted scroll).
   saveState();
};

// Called from GWT when the data viewer tab is being closed (dismissType
// CLOSE, not MOVE). Discards the saved state so it doesn't accumulate in
// localStorage past the lifetime of the tab. This is best-effort only: the
// iframe is usually already detached by the time a close reaches us, so the
// host clears the same key directly via the stateKeyCallback path (#17830).
// Kept for the rare case where the frame is still live at dismiss.
window.onDismiss = function() {
   clearSavedState();
};

window.setData = function(data) {
   bootstrap(data);
};

window.setOption = function(option, value) {
   switch (option) {
      case "nullsAsNAs":
         displayNullsAsNAs = value === "true";
         break;
      case "status":
         statusTextOverride = value;
         break;
      case "ordering":
         ordering = value === "true";
         break;
      case "rowNumbers":
         rowNumbers = value === "true";
         break;
      case "dataViewerCallback":
         window.dataViewerCallback = value;
         break;
      case "listViewerCallback":
         window.listViewerCallback = value;
         break;
      case "columnOverflowCallback":
         window.columnOverflowCallback = value;
         // Push the current state immediately when known; registration can
         // land after the first layout pass has already run.
         if (lastColumnOverflow !== null) value(lastColumnOverflow);
         break;
      case "sidebarStateCallback":
         window.sidebarStateCallback = value;
         // Sync the host immediately with the current state, since the
         // callback typically registers after bootstrap has already
         // resolved sidebarVisible from URL params + saved state.
         value(sidebarVisible);
         break;
      case "filterStateCallback":
         window.filterStateCallback = value;
         // Sync immediately, then again at the end of bootstrap once saved
         // filters have been resolved (mirrors sidebarStateCallback).
         value(isFilterUIVisible());
         break;
      case "stateKeyCallback":
         window.stateKeyCallback = value;
         // Report the localStorage key for this object's saved UI state so the
         // host can clear it on explicit close. The host has to do the clearing
         // because by the time the close reaches us the data viewer iframe is
         // already detached (TabClosedEvent fires after the widget is removed),
         // so an in-frame clear would no-op (#17830). The key derives only from
         // the iframe URL (env+obj), which is available as soon as the frame
         // loads, so this does not need to wait for bootstrap.
         value(stateKey());
         break;
   }
};

// Default callbacks
window.dataViewerCallback = function(row, col) {
   alert("No viewer for data at " + col + ", " + row + ".");
};

window.listViewerCallback = function(row, col) {
   alert("No viewer for list at " + col + ", " + row + ".");
};

window.getActiveColumn = function() {
   return activeColumnInfo;
};

window.goToColumn = function(column) {
   goToColumn(column);
   // Jumps come from the host's go-to-column box; hand focus to the grid so
   // subsequent keystrokes navigate the data (mirrors Go to File/Function
   // returning focus to the editor).
   focusGridViewport();
};

window.matchColumns = function(query, callback) {
   matchColumnsAsync(query, callback);
};

// Slide the fetched column window to an explicit offset/size. No production
// caller remains (the old column-pagination UI was replaced by scroll-driven
// sliding), but this is retained as an automation hook: it is the only way to
// position the window at an exact offset, which the e2e pin-across-pagination
// test relies on to page a pinned column out of the fetched set deterministically.
window.setOffsetAndMaxColumns = function(newOffset, newMax) {
   if (bootstrapping) return;

   // Same dead-grid recovery rule as goToColumn: with totalCols unknown (0)
   // after a failed bootstrap, the guards below would swallow every request.
   if (cols === null) {
      bootstrap();
      return;
   }

   if (newOffset >= totalCols) return;
   if (newOffset >= 0) columnOffset = newOffset;
   // Always clamp maxDisplayColumns to the remaining column count: when
   // newMax is supplied we honor it (capped); otherwise we still need to
   // shrink the existing window so a bumped offset can't request a slice
   // running off the end.
   var cap = totalCols - columnOffset;
   if (newMax > 0) {
      maxDisplayColumns = Math.min(cap, newMax);
   } else {
      maxDisplayColumns = Math.min(cap, maxDisplayColumns);
   }

   // Slide to the new window in place. The active cell/header is remapped
   // by identity (or cleared when its column left the fetched set), and the
   // new window's first column scrolls to the left edge.
   slideColumnWindow({ targetAbs: columnOffset + 1 });
};

// ==========================================================================
// Initialization
// ==========================================================================

initResizeHandlers();

var loc = parseLocationUrl();
var dataMode = loc.dataSource || "server";

document.addEventListener("DOMContentLoaded", function() {
   // #rsGridData_info_sort_clear is a static element that survives
   // re-bootstraps, so wire its listener once here rather than per-grid.
   var sortClear = document.getElementById("rsGridData_info_sort_clear");
   if (sortClear)
      sortClear.addEventListener("click", clearSort);

   if (dataMode === "server") {
      bootstrap();
   }
});

})();
