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

var ROW_HEIGHT = 23;
var BUFFER_ROWS = 200;
var FETCH_SIZE = 500;
var FETCH_THRESHOLD = 100;

// User-facing timings, all in milliseconds. Tweak together when tuning feel.
var TIMING = {
   filterDebounce: 200,         // numeric/text filter input -> applyFilters
   searchDebounce: 100,         // global search input -> applyFilters
   infoBarDebounce: 150,        // "Showing X to Y" text update during scroll
   resizeDebounce: 75,          // window resize -> relayout
   sidebarTransition: 200,      // CSS transition duration for sidebar expand/collapse
   columnFlash: 1000,           // duration of the highlight-flash on scrollToColumn
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

// Current sort state
var sortColumn = -1;
var sortDirection = ""; // "", "asc", "desc"

// Cached search/filter values
var cachedSearch = "";
var cachedFilterValues = [];

// Scroll position preservation
var lastScrollTop = 0;
var lastScrollLeft = 0;

// Row data cache
var rowCache = new Map();
var totalRows = 0;
var filteredRows = 0;
var drawCounter = 0;

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
var manualWidths = [];

// Filter popup state
var dismissActivePopup = null;

// Column type popup state
var columnsPopup = null;
var activeColumnInfo = {};
var onColumnOpen = null;
var onColumnDismiss = null;

// Bootstrapping flag
var bootstrapping = false;

// Post-init deferred actions
var postInitActions = {};

// Sidebar state
var sidebarVisible = true;

// Pinned columns (set of column indices; column 0/rownames is always implicitly pinned)
var pinnedColumns = new Set();

// Set when initial autoSize ran with a hidden viewport (offsetHeight === 0);
// the next onActivate re-measures.
var needsAutoSize = false;

// Listener refs -- kept at module scope so destroyGrid can remove them and
// avoid duplicate handlers across bootstrap cycles.
var scrollUpdateListener = null;
var scrollbarUpdateListener = null;
var scrollEndListener = null;
var resizeListener = null;
var pendingScrollbarRaf = 0;

// ==========================================================================
// Utilities
// ==========================================================================

var debounce = function(func, wait) {
   var timeout;
   var debounced = function() {
      var context = this, args = arguments;
      clearTimeout(timeout);
      timeout = setTimeout(function() {
         func.apply(context, args);
      }, wait);
   };
   debounced.cancel = function() {
      clearTimeout(timeout);
   };
   return debounced;
};

var escapeHtml = function(html) {
   if (html === null || html === undefined) return "";
   var s = (typeof html === "string") ? html : String(html);
   var replacements = { "<": "&lt;", ">": "&gt;", "&": "&amp;", '"': "&quot;" };
   return s.replace(/[&<>"]/g, function(ch) { return replacements[ch]; });
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
// On non-2xx, throws an Error whose message is the response body.
var gridDataFetch = function(body, signal) {
   var init = {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: body
   };
   if (signal) init.signal = signal;
   return fetch("../grid_data", init).then(function(response) {
      if (!response.ok) return response.text().then(function(t) { throw new Error(t); });
      return response.json();
   });
};

var fetchColumnSummary = function(columnIndex, callback) {
   var params = "show=column_summary&column=" + columnIndex +
      "&" + window.location.search.substring(1);

   gridDataFetch(params)
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
         // forever.
         if (callback) callback({ error: "Failed to load summary" });
      });
};

var fetchColumns = function(callback) {
   var params = "show=cols&" + window.location.search.substring(1) +
      "&column_offset=" + columnOffset;

   gridDataFetch(params)
      .then(function(result) {
         if (result.error) { showError(result.error); return; }
         callback(result);
      })
      .catch(function(err) {
         try {
            var parsed = JSON.parse(err.message);
            if (parsed.error) showError(parsed.error);
            else showError(err.message);
         } catch(e) {
            showError(err.message || "The object could not be displayed.");
         }
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
      column_offset: columnOffset,
      max_display_columns: maxDisplayColumns,
      max_rows: maxRows,
      "search[value]": cachedSearch
   };

   // Add sort parameters
   if (sortColumn >= 0 && sortDirection) {
      params["order[0][column]"] = sortColumn;
      params["order[0][dir]"] = sortDirection;
   }

   // Add column filter parameters
   for (var i = 0; i < cachedFilterValues.length; i++) {
      params["columns[" + i + "][search][value]"] = cachedFilterValues[i];
   }

   var controller = new AbortController();
   pendingFetches.set(key, controller);

   gridDataFetch(buildFormData(params), controller.signal)
      .then(function(result) {
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
         if (callback) callback();
         renderVisibleRows(true);
         updateInfoBar();
         updateSpacerHeight();
      })
      .catch(function(err) {
         pendingFetches.delete(key);
         if (err.name === "AbortError") return;
         try {
            var parsed = JSON.parse(err.message);
            if (parsed.error) showError(parsed.error);
         } catch(e) {
            // Ignore non-JSON errors during fetch
         }
      });
};

var invalidateCache = function() {
   rowCache.clear();
   // Abort all pending fetches
   pendingFetches.forEach(function(controller) { controller.abort(); });
   pendingFetches.clear();
   totalRows = 0;
   filteredRows = 0;
   // Bump the staleness token so any in-flight responses that slip past
   // AbortController are discarded by fetchRows' response handler.
   drawCounter++;
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
   if (!didHighlight && colIdx < cachedFilterValues.length) {
      var colSearch = cachedFilterValues[colIdx];
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
      var cbCol = colIdx + columnOffset;
      // rowData[0] arrives JSON-encoded for character row names and as a plain
      // numeric string (or a number, in preview mode) for automatic ones;
      // JSON.parse handles both.
      var cbRow;
      try { cbRow = JSON.parse(rowData[0]); } catch(e) { cbRow = rowData[0]; }

      var linkEl = document.createElement("a");
      linkEl.className = "viewerLink";
      linkEl.href = "#";
      linkEl.addEventListener("click", function(evt) {
         evt.preventDefault();
         var fn = window[cbName];
         if (typeof fn === "function") fn(cbRow, cbCol);
      });
      var imgEl = document.createElement("img");
      imgEl.className = "viewerImage";
      imgEl.src = clazz === "dataCell" ? "data-viewer.png" : "object-viewer.png";
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

// Cached pinned offsets -- recomputed in applyPinnedColumns and renderVisibleRows
var cachedPinnedOffsets = {};

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

var getColClass = function(col) {
   if (col.col_type === "numeric") return "numberCell";
   if (col.col_type === "data.frame") return "dataCell";
   if (col.col_type === "list") return "listCell";
   return "textCell";
};

var MIN_COL_WIDTH = 50;
var MAX_COL_WIDTH = 300;
var MAX_COL_WIDTH_CHAR = 500;
var DEFAULT_COL_WIDTH = 120;

// Measured column widths after auto-sizing (indexed by column position)
var measuredWidths = [];

var createHeader = function(idx, col) {
   var th = document.createElement("th");
   th.className = "sorting";
   th.setAttribute("data-col-idx", idx);

   if (idx === 0 && rowNumbers) {
      th.classList.add("pinned");
   }

   var interior = document.createElement("div");
   interior.className = "headerCell";

   // Pin icon to the left of the label (not shown for rownames -- always pinned)
   if (!(idx === 0 && rowNumbers)) {
      var pinIcon = document.createElement("span");
      pinIcon.className = "pin-icon";
      if (pinnedColumns.has(idx)) pinIcon.classList.add("pinned");
      pinIcon.title = "Pin column";
      pinIcon.addEventListener("click", function(evt) {
         evt.stopPropagation();
         togglePinColumn(idx);
      });
      interior.appendChild(pinIcon);
   }

   var title = document.createElement("span");
   title.textContent = col.col_name;
   interior.appendChild(title);

   // Tooltip
   if (col.col_type === "rownames") {
      th.title = "row names";
   } else {
      th.title = "column " + (idx + columnOffset) + ": " + col.col_type;
      if (col.col_type === "numeric" && col.col_breaks && col.col_breaks.length > 0) {
         th.title += " with range " + col.col_breaks[0] +
            " - " + col.col_breaks[col.col_breaks.length - 1];
      } else if (col.col_type === "factor" && col.col_vals) {
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

   // Sort click handler
   if (ordering && col.col_type !== "rownames") {
      th.style.cursor = "pointer";
      th.addEventListener("click", function(evt) {
         if (evt.target.className === "resizer") return;
         if (didResize) { didResize = false; return; }
         handleSortClick(idx, th);
      });
   }

   return th;
};

var handleSortClick = function(colIdx, th) {
   var thead = document.getElementById("data_cols");
   var headers = thead.children;

   // Cycle: unsorted -> asc -> desc -> unsorted
   var newDir = "";
   if (sortColumn !== colIdx) {
      newDir = "asc";
   } else if (sortDirection === "asc") {
      newDir = "desc";
   } else if (sortDirection === "desc") {
      newDir = "";
   } else {
      newDir = "asc";
   }

   // Update sort state
   sortColumn = newDir ? colIdx : -1;
   sortDirection = newDir;

   // Update header classes
   for (var i = 0; i < headers.length; i++) {
      headers[i].classList.remove("sorting_asc", "sorting_desc");
      if (!headers[i].classList.contains("sorting")) {
         headers[i].classList.add("sorting");
      }
   }
   if (newDir === "asc") {
      th.classList.remove("sorting");
      th.classList.add("sorting_asc");
   } else if (newDir === "desc") {
      th.classList.remove("sorting");
      th.classList.add("sorting_desc");
   }

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

// Returns the column render order: pinned columns first (in original order),
// then unpinned columns (in original order). Column 0 (rownames) is always first.
var getColumnOrder = function() {
   var colCount = cols ? Math.min(cols.length, maxDisplayColumns + 1) : 0;
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

// Cached column order -- recomputed when pinning changes
var columnOrder = [];

var isColumnPinned = function(colIdx) {
   return (colIdx === 0 && rowNumbers) || pinnedColumns.has(colIdx);
};

var togglePinColumn = function(colIdx) {
   if (pinnedColumns.has(colIdx)) {
      pinnedColumns.delete(colIdx);
   } else {
      pinnedColumns.add(colIdx);
   }
   invalidatePinnedOffsets();
   rebuildHeaders();
   renderVisibleRows(true);
   saveState();
};

// Rebuild headers in the current column order (pinned first, then unpinned).
// Reorder existing <th> elements rather than recreating them so any attached
// filter UI / popup state is preserved across pin toggles.
var rebuildHeaders = function() {
   var thead = document.getElementById("data_cols");
   if (!thead || !cols) return;

   columnOrder = getColumnOrder();

   var existing = {};
   for (var i = 0; i < thead.children.length; i++) {
      var th = thead.children[i];
      existing[parseInt(th.getAttribute("data-col-idx"), 10)] = th;
   }

   for (var i = 0; i < columnOrder.length; i++) {
      var colIdx = columnOrder[i];
      var th = existing[colIdx] || createHeader(colIdx, cols[colIdx]);
      thead.appendChild(th);
      delete existing[colIdx];
   }

   // Remove any leftover headers whose columns are no longer in columnOrder
   // (defensive: keeps thead consistent if the visible column set shrinks).
   for (var leftoverIdx in existing) {
      if (existing.hasOwnProperty(leftoverIdx)) {
         thead.removeChild(existing[leftoverIdx]);
      }
   }

   autoSizeColumns();
   applyPinnedColumns();
};

// Compute the cumulative left offset for each pinned column based on render order.
// Returns { offsets: { colIdx: leftPx, ... }, totalWidth: number }.
//
// Result is cached. Each entry costs an offsetWidth read which forces layout,
// and this function is called from renderVisibleRows on every scroll. Callers
// that change column widths or pinning must invalidate via
// invalidatePinnedOffsets().
var pinnedOffsetsCache = null;

var invalidatePinnedOffsets = function() {
   pinnedOffsetsCache = null;
};

var getPinnedOffsets = function() {
   if (pinnedOffsetsCache !== null) return pinnedOffsetsCache;

   var offsets = {};
   var cumulative = 0;
   var thead = document.getElementById("data_cols");
   if (!thead) return { offsets: offsets, totalWidth: 0 };

   for (var i = 0; i < columnOrder.length; i++) {
      var colIdx = columnOrder[i];
      if (!isColumnPinned(colIdx)) break; // pinned columns are all at the front
      offsets[colIdx] = cumulative;
      var th = thead.children[i];
      cumulative += th ? th.offsetWidth : 0;
   }
   pinnedOffsetsCache = { offsets: offsets, totalWidth: cumulative };
   return pinnedOffsetsCache;
};

// Apply pinned styling to header cells
var applyPinnedColumns = function() {
   var thead = document.getElementById("data_cols");
   if (!thead) return;

   var pinned = getPinnedOffsets();

   for (var i = 0; i < thead.children.length; i++) {
      var th = thead.children[i];
      var colIdx = columnOrder[i];
      var pinIcon = th.querySelector(".pin-icon");

      if (isColumnPinned(colIdx)) {
         th.classList.add("pinned");
         th.style.left = (pinned.offsets[colIdx] || 0) + "px";
         if (pinIcon) pinIcon.classList.add("pinned");
      } else {
         th.classList.remove("pinned");
         th.style.left = "";
         if (pinIcon) pinIcon.classList.remove("pinned");
      }
   }

   // Horizontal overscroll: lets the user scroll the rightmost column to sit
   // just past the pinned columns for side-by-side context. Only meaningful
   // when content is wider than the viewport -- otherwise it would introduce
   // a phantom scrollbar over empty space.
   var viewport = document.getElementById("gridViewport");
   var table = document.getElementById("rsGridData");
   if (viewport && table) {
      var currentPad = parseFloat(table.style.paddingRight) || 0;
      var contentWidth = table.offsetWidth - currentPad;
      var overscroll = contentWidth > viewport.clientWidth
         ? Math.max(0, viewport.clientWidth - pinned.totalWidth)
         : 0;
      table.style.paddingRight = overscroll + "px";
   }
};

// ==========================================================================
// Column Auto-Sizing
// ==========================================================================

// Measure the natural text width of a string using a 2d canvas. Avoids the
// layout flush that an offsetWidth read on a DOM element would force, which
// matters when measuring 100+ rows x N columns at startup.
var measureCanvas = null;
var measureCtx = null;
var measureTextWidth = function(text, bold) {
   if (!measureCtx) {
      measureCanvas = document.createElement("canvas");
      measureCtx = measureCanvas.getContext("2d");
   }
   measureCtx.font = (bold ? "bold " : "") +
      "11px 'DejaVu Sans', 'Lucida Grande', 'Segoe UI', Verdana, Helvetica, sans-serif";
   return Math.ceil(measureCtx.measureText(text).width);
};

// Average character width, derived from a representative reference string at
// the same font/size as the data cells. Used to translate the server-provided
// col_max_chars hint into a pixel width without measuring every cell.
var AVG_CHAR_REF_STRING =
   "0123456789 ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz .,-_";
var avgCharWidthCache = 0;
var avgCharWidth = function() {
   if (!avgCharWidthCache) {
      avgCharWidthCache =
         measureTextWidth(AVG_CHAR_REF_STRING, false) / AVG_CHAR_REF_STRING.length;
   }
   return avgCharWidthCache;
};

// Compute column widths by measuring header text and a sample of cached cell
// values, then apply them with table-layout:fixed.
var autoSizeColumns = function() {
   var thead = document.getElementById("data_cols");
   if (!thead || !thead.children.length || !cols) return;

   // If the viewport isn't visible (e.g. background tab), measurements
   // will be wrong. Flag and bail; onActivate will re-run sizing once the
   // tab has real layout.
   var viewport = document.getElementById("gridViewport");
   if (viewport && viewport.offsetHeight === 0) {
      needsAutoSize = true;
      return;
   }

   var colCount = thead.children.length;
   measuredWidths = [];
   var totalWidth = 0;

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

   for (var i = 0; i < colCount; i++) {
      var th = thead.children[i];
      var colIdx = parseInt(th.getAttribute("data-col-idx"), 10);
      if (isNaN(colIdx)) colIdx = i;
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

      // If the server provided a max-chars hint, derive a baseline cell
      // width from it (avgCharWidth x N + chrome). This is exact for
      // numeric/integer/Date/POSIXct/logical columns, exact-by-bound for
      // character/factor (max nchar across values), and absent for column
      // types whose accurate length would require expensive formatting.
      // We still sample below -- the hint is a baseline, not a ceiling.
      if (typeof col.col_max_chars === "number" && col.col_max_chars > 0) {
         var hintW = Math.ceil(col.col_max_chars * avgCharWidth()) + cellExtra;
         if (hintW > maxW) maxW = hintW;
      }

      // Measure a sample of cell values from the cache. When the hint is
      // present this catches cases where the avg-char-width estimate
      // under-counts (e.g., a column of unusually wide glyphs); when it
      // isn't, this is the sole source of width data for the column.
      var sampleSize = Math.min(rowCache.size, 100);
      for (var r = 0; r < sampleSize; r++) {
         var row = rowCache.get(r);
         if (!row) continue;
         var cellVal = row[colIdx];
         if (cellVal === 0 || cellVal === null || cellVal === undefined) {
            // NA -- short
            cellVal = "NA";
         }
         var cellText = String(cellVal);
         // For row names (col 0), the value is JSON-encoded
         if (isRowNames) {
            try { cellText = JSON.parse(cellText).toString(); } catch(e) {}
         }
         var cellW = measureTextWidth(cellText, false) + cellExtra;
         if (cellW > maxW) maxW = cellW;
      }

      // Manual widths (user resize) take precedence over computed widths.
      // Otherwise clamp to min/max -- allow wider max for character columns.
      var w;
      if (typeof manualWidths[colIdx] === "number" && manualWidths[colIdx] > 0) {
         w = manualWidths[colIdx];
      } else {
         var upperBound = (col.col_type === "character") ? MAX_COL_WIDTH_CHAR : MAX_COL_WIDTH;
         w = Math.max(MIN_COL_WIDTH, Math.min(upperBound, maxW));
      }
      measuredWidths.push(w);
      totalWidth += w;
   }

   // Apply widths and lock to fixed layout
   for (var i = 0; i < thead.children.length; i++) {
      thead.children[i].style.width = measuredWidths[i] + "px";
   }

   var table = document.getElementById("rsGridData");
   if (table) {
      table.style.width = totalWidth + "px";
      table.style.tableLayout = "fixed";
   }

   invalidatePinnedOffsets();
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
         origTableWidth = document.getElementById("rsGridData").offsetWidth;
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
      manualWidths[resizingColIdx] = colWidth;
   }

   // Apply width to table
   var table = document.getElementById("rsGridData");
   if (table) {
      table.style.width = (origTableWidth + delta) + "px";
   }

   invalidatePinnedOffsets();
   // Note: saveState fires once at end of drag (in endResize), not on every
   // mousemove -- repeated localStorage writes during a drag would cause jank.
};

// ==========================================================================
// Filter UI
// ==========================================================================

var createFilterUI = function(idx, col) {
   if (idx < 1) return null;

   var host = document.createElement("div");
   var val = null, ui = null;
   host.className = "colFilter unfiltered";

   var setUnfiltered = function() {
      if (ui !== null) {
         if (ui.parentNode === host) host.replaceChild(val, ui);
         ui = null;
      }
      host.className = "colFilter unfiltered";
      clear.style.display = "none";
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
   val.addEventListener("click", function(evt) {
      if (col.col_search_type === "numeric") {
         ui = createNumericFilterUI(idx, col, onDismiss);
      } else if (col.col_search_type === "factor") {
         ui = createFactorFilterUI(idx, col, onDismiss);
      } else if (col.col_search_type === "character") {
         ui = createTextFilterUI(idx, col, onDismiss);
      } else if (col.col_search_type === "boolean") {
         ui = createBooleanFilterUI(idx, col, onDismiss);
      }
      if (ui) {
         ui.classList.add("filterValue");
         host.replaceChild(ui, val);
         host.className = "colFilter filtered";
         clear.style.display = "block";
         ui.dispatchEvent(new MouseEvent("click", { bubbles: true }));
         evt.preventDefault();
         evt.stopPropagation();
      }
   });

   host.appendChild(val);
   return host;
};

var getColumnSearch = function(idx) {
   return cachedFilterValues[idx] || "";
};

var setColumnSearch = function(idx, val) {
   while (cachedFilterValues.length <= idx) cachedFilterValues.push("");
   cachedFilterValues[idx] = val;
};

var applyFilters = function() {
   invalidateCache();
   fetchRows(0, FETCH_SIZE, function() {
      scrollToTop();
   });
   saveState();
};

var createNumericFilterUI = function(idx, col, onDismiss) {
   var ele = document.createElement("div");

   invokeFilterPopup(ele, function(popup) {
      popup.classList.add("numericFilterPopup");
      var min = col.col_breaks[0].toString();
      var max = col.col_breaks[col.col_breaks.length - 1].toString();
      var val = parseSearchString(getColumnSearch(idx));
      if (val.indexOf("_") > 0) {
         var range = val.split("_");
         min = range[0]; max = range[1];
      } else if (!isNaN(parseFloat(val)) && val.length > 0) {
         min = parseFloat(val); max = parseFloat(val);
      }

      var filterFromRange = function(s, e) {
         if (Math.abs(s - e) === 0) return "" + s;
         return s + " - " + e;
      };

      var numVal = document.createElement("input");
      numVal.type = "text";
      numVal.className = "numValueBox";
      numVal.style.textAlign = "center";
      numVal.value = filterFromRange(min, max);

      // Numeric tokens accept optional scientific-notation suffix (1e10,
      // 2.5e-5). The range separator is `-` surrounded by required
      // whitespace, which disambiguates from a leading negative sign.
      var NUM = "-?\\d+\\.?\\d*(?:[eE][+-]?\\d+)?";
      var SINGLE_RE = new RegExp("^\\s*" + NUM + "\\s*$");
      var RANGE_RE = new RegExp(
         "^\\s*(" + NUM + ")\\s*-\\s*(" + NUM + ")\\s*");

      var updateView = debounce(function(v) {
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
      }, TIMING.filterDebounce);

      numVal.addEventListener("change", function() { updateView(numVal.value); });
      numVal.addEventListener("click", function(evt) { evt.stopPropagation(); });
      numVal.addEventListener("keydown", function(evt) {
         if (!dismissActivePopup) return;
         if (evt.keyCode === 27) dismissActivePopup(false);
         else if (evt.keyCode === 13) dismissActivePopup(true);
      });

      var updateText = function(start, end) {
         numVal.value = filterFromRange(start, end);
         updateView(numVal.value);
      };

      var histBrush = document.createElement("div");
      histBrush.className = "numHist";

      var binStart = 0;
      var binEnd = col.col_breaks.length - 2;

      for (var i = 0; i < col.col_breaks.length; i++) {
         if (Math.abs(col.col_breaks[i] - min) < Math.abs(col.col_breaks[binStart] - min))
            binStart = i;
         if (i === 0) continue;
         if (Math.abs(col.col_breaks[i] - max) < Math.abs(col.col_breaks[binEnd] - max))
            binEnd = i - 1;
      }
      if (binEnd < binStart) binStart = binEnd;

      // Use the existing hist.js for interactive histogram
      hist(histBrush, col.col_breaks, col.col_counts, binStart, binEnd, updateText);
      popup.appendChild(histBrush);
      popup.appendChild(numVal);
   }, onDismiss);

   ele.textContent = "[...]";
   return ele;
};

var createTextFilterBox = function(ele, idx, col, onDismiss) {
   var input = document.createElement("input");
   input.type = "text";
   input.className = "textFilterBox";

   var search = getColumnSearch(idx).split("|");
   if (search.length > 1 && search[0] === "character")
      input.value = decodeURIComponent(search[1]);

   var updateView = debounce(function() {
      setColumnSearch(idx, "character|" + encodeURIComponent(input.value));
      applyFilters();
   }, TIMING.filterDebounce);

   input.addEventListener("keyup", function() { updateView(); });
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

var createFactorFilterUI = function(idx, col, onDismiss) {
   var ele = document.createElement("div");
   var input = createTextFilterBox(ele, idx, col, onDismiss);
   input.addEventListener("keyup", function() {
      if (dismissActivePopup) dismissActivePopup(false);
   });
   input.addEventListener("blur", function() {
      if (!dismissActivePopup) onDismiss();
   });
   input.addEventListener("focus", function() {
      if (dismissActivePopup) dismissActivePopup(false);
   });

   invokeFilterPopup(ele, function(popup) {
      var list = document.createElement("div");
      list.className = "choiceList";
      var current = 0;
      var searchVals = getColumnSearch(idx).split("|");
      if (searchVals.length > 1 && searchVals[0] === "factor")
         current = parseInt(searchVals[1]);

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

var createTextFilterUI = function(idx, col, onDismiss) {
   var ele = document.createElement("div");
   var input = createTextFilterBox(ele, idx, col, onDismiss);
   input.addEventListener("blur", function() { onDismiss(); });
   input.addEventListener("focus", function() {
      if (dismissActivePopup) dismissActivePopup(true);
   });
   return ele;
};

var createBooleanFilterUI = function(idx, col, onDismiss) {
   var ele = document.createElement("div");
   var display = document.createElement("span");
   display.innerHTML = "&nbsp;";
   ele.appendChild(display);

   invokeFilterPopup(ele, function(popup) {
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

   ele.addEventListener("click", function(evt) {
      if (dismissActivePopup && dismissActivePopup !== dismissPopup) {
         dismissActivePopup(true);
      }
      if (popup) {
         dismissPopup(true);
      } else {
         popup = document.createElement("div");
         popup.className = "filterPopup";
         var popupInfo = buildPopup(popup);
         document.body.appendChild(popup);

         var rect = ele.getBoundingClientRect();
         var top = rect.bottom + (!popupInfo ? 0 : (popupInfo.top || 0));
         var left = rect.left + (!popupInfo ? -4 : (popupInfo.left || -4));

         if (popup.offsetWidth + left > document.body.offsetWidth) {
            left = document.body.offsetWidth - popup.offsetWidth;
         }

         popup.style.top = top + "px";
         popup.style.left = left + "px";

         document.body.addEventListener("click", checkLightDismiss);
         document.body.addEventListener("keydown", checkEscDismiss);
         dismissActivePopup = dismissPopup;
      }
      evt.preventDefault();
      evt.stopPropagation();
   });
};

// ==========================================================================
// Column Type UI (data import mode)
// ==========================================================================

var createColumnTypesUI = function(th, idx, col) {
   var host = document.createElement("div");
   host.className = "columnTypeWrapper";

   var val = document.createElement("div");
   val.textContent = "(" + (col.col_type_assigned ? col.col_type_assigned : col.col_type_r) + ")";
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
   if (viewport) viewport.scrollTop = 0;
};

// No-op: spacer height is now handled by top/bottom spacer <tr> elements
// inside renderVisibleRows. Table minHeight is never set.
var updateSpacerHeight = function() {};

var updateInfoBar = function() {
   // Suppress info bar updates during custom scrollbar drags -- reading
   // scrollTop forces style+layout recalc which causes jank. The info
   // bar is updated once when the drag ends.
   if (anyScrollbarDragging()) return;

   var info = document.getElementById("rsGridData_info");
   if (!info) return;

   if (statusTextOverride) {
      info.textContent = statusTextOverride;
      return;
   }

   var activeRows = filteredRows;
   if (totalRows === 0) {
      info.textContent = "";
      return;
   }

   // Count a row as "in view" when at least 50% of it is visible.
   //
   // The viewport's full height is not the same as the area in which data
   // rows are actually visible: the sticky <thead> overlays the top, and
   // when horizontal scroll is active the custom horizontal scrollbar
   // overlays the bottom 10px. Compute an effective body-visible height
   // and use that for the bottom edge.
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
      var viewportH = viewport ? viewport.clientHeight : 0;
      var headerEl = document.getElementById("data_cols");
      var headerH = (headerEl && headerEl.parentElement)
         ? headerEl.parentElement.offsetHeight : 0;
      var hasHScroll = viewport
         ? viewport.scrollWidth > viewport.clientWidth + 1 : false;
      var bodyH = Math.max(0, viewportH - headerH - (hasHScroll ? 10 : 0));
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
      text += " (filtered from " + totalRows.toLocaleString() + " total)";
   }
   info.textContent = text;
};

var buildRow = function(r) {
   var rowData = rowCache.get(r);
   if (!rowData) return null;

   var tr = document.createElement("tr");
   tr.setAttribute("data-row", r);

   for (var c = 0; c < columnOrder.length; c++) {
      var colIdx = columnOrder[c];
      var clazz = getColClass(cols[colIdx]);
      var td = createCell(rowData[colIdx], colIdx, rowData, clazz);
      tr.appendChild(td);
   }

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

var renderVisibleRows = function(force) {
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
   if (!force && newStart === renderStart && newEnd === renderEnd) {
      return;
   }

   // Check if we need to fetch more data
   var firstBlock = Math.floor(newStart / FETCH_SIZE) * FETCH_SIZE;
   for (var blockStart = firstBlock; blockStart <= newEnd; blockStart += FETCH_SIZE) {
      if (!rowCache.has(blockStart) && !pendingFetches.has(blockStart + "-" + FETCH_SIZE)) {
         fetchRows(blockStart, FETCH_SIZE);
      }
   }

   // Prefetch ahead
   var aheadStart = Math.floor(newEnd / FETCH_SIZE) * FETCH_SIZE + FETCH_SIZE;
   if (aheadStart < activeRows && !rowCache.has(aheadStart)) {
      fetchRows(aheadStart, FETCH_SIZE);
   }

   // Recompute pinned offsets for data cells
   cachedPinnedOffsets = getPinnedOffsets().offsets;

   var colSpan = columnOrder.length || 1;

   // --- Full rebuild (force, or no existing spacers) ---
   if (force || !topSpacerRow || !bottomSpacerRow) {
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
   // Insert before the first data row (right after topSpacerRow)
   var insertBeforeTop = topSpacerRow.nextSibling;
   for (var r = Math.min(renderStart - 1, newEnd); r >= newStart; r--) {
      if (!renderedRowElements.has(r)) {
         var tr = buildRow(r);
         if (tr) {
            tbody.insertBefore(tr, insertBeforeTop);
            renderedRowElements.set(r, tr);
            insertBeforeTop = tr;
         }
      }
   }

   // Add new rows at the bottom (renderEnd+1 .. newEnd)
   // Insert before the bottom spacer
   for (var r = Math.max(renderEnd + 1, newStart); r <= newEnd; r++) {
      if (!renderedRowElements.has(r)) {
         var tr = buildRow(r);
         if (tr) {
            tbody.insertBefore(tr, bottomSpacerRow);
            renderedRowElements.set(r, tr);
         }
      }
   }

   // Update spacer heights
   var renderedCount = renderedRowElements.size;
   var expectedCount = newEnd - newStart + 1;
   var skipped = expectedCount - renderedCount;
   updateSpacerRowHeight(topSpacerRow, newStart * ROW_HEIGHT);
   updateSpacerRowHeight(bottomSpacerRow, (activeRows - newEnd - 1 + skipped) * ROW_HEIGHT);

   renderStart = newStart;
   renderEnd = newEnd;
};

// Info bar update is debounced separately at a longer interval -- reading
// scrollTop for the "Showing X to Y" text forces style+layout recalc,
// which is expensive during fast scrolling.
var debouncedInfoBar = debounce(updateInfoBar, TIMING.infoBarDebounce);

// RAF-throttle scroll: render at most once per animation frame so virtual
// scroll updates happen during the scroll, not 16ms after it stops.
var pendingScrollRaf = 0;
var onScroll = function() {
   if (pendingScrollRaf) return;
   pendingScrollRaf = requestAnimationFrame(function() {
      pendingScrollRaf = 0;
      var viewport = document.getElementById("gridViewport");
      if (!viewport) return;
      lastScrollTop = viewport.scrollTop;
      lastScrollLeft = viewport.scrollLeft;
      renderVisibleRows();
      debouncedInfoBar();
   });
};
onScroll.cancel = function() {
   if (pendingScrollRaf) {
      cancelAnimationFrame(pendingScrollRaf);
      pendingScrollRaf = 0;
   }
};

// ==========================================================================
// Sidebar
// ==========================================================================

var createSparklineSVG = function(breaks, counts) {
   if (!breaks || !counts || counts.length === 0) return null;

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

   var svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
   svg.setAttribute("viewBox", "0 0 " + counts.length + " 20");
   svg.setAttribute("preserveAspectRatio", "none");

   for (var j = 0; j < counts.length; j++) {
      var h = (counts[j] / max) * 20;
      var rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
      rect.setAttribute("x", j);
      rect.setAttribute("y", 20 - h);
      rect.setAttribute("width", 1);
      rect.setAttribute("height", h);
      rect.setAttribute("class", "sparkline-bar");
      rect.setAttribute("data-bin", j);
      svg.appendChild(rect);
   }

   wrapper.appendChild(svg);

   // Tooltip on hover
   var tooltip = document.createElement("div");
   tooltip.className = "sparkline-tooltip";
   tooltip.style.display = "none";
   wrapper.appendChild(tooltip);

   var formatNum = function(n) {
      // Round to reasonable precision
      if (Math.abs(n) >= 100) return Math.round(n).toLocaleString();
      if (Math.abs(n) >= 1) return n.toFixed(1);
      if (Math.abs(n) >= 0.01) return n.toFixed(2);
      return n.toPrecision(3);
   };

   svg.addEventListener("mousemove", function(evt) {
      var target = evt.target;
      var bin = target.getAttribute("data-bin");
      if (bin === null) { tooltip.style.display = "none"; return; }
      bin = parseInt(bin, 10);

      var lo = breaks[bin];
      var hi = breaks[bin + 1];
      var count = counts[bin];
      var pct = total > 0 ? ((count / total) * 100).toFixed(1) : "0";

      // Build with DOM nodes rather than innerHTML so future format changes
      // can't accidentally interpret data values as markup.
      tooltip.textContent = "";
      tooltip.appendChild(document.createTextNode(
         "Range: " + formatNum(lo) + " to " + formatNum(hi)));
      tooltip.appendChild(document.createElement("br"));
      tooltip.appendChild(document.createTextNode(
         "Count: " + count.toLocaleString() + " (" + pct + "%)"));
      tooltip.style.display = "";
   });

   svg.addEventListener("mouseleave", function() {
      tooltip.style.display = "none";
   });

   return wrapper;
};

var typeLabel = function(col) {
   var type = col.col_type_r || col.col_type || "";
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
   }

   container.appendChild(table);
};

// Tracks how many sidebar column-summary fetches are in flight so the shared
// spinner only hides when all of them have completed (rather than the first
// one back hiding the spinner while others are still pending).
var pendingSummaryFetches = 0;

var initSidebar = function() {
   var content = document.getElementById("sidebarContent");
   var toggle = document.getElementById("sidebarToggle");
   if (!content || !toggle || !cols) return;

   toggle.innerHTML = "";
   var toggleLabel = document.createElement("span");
   toggleLabel.textContent = "Summary";
   toggle.appendChild(toggleLabel);

   var toggleSpinner = document.createElement("span");
   toggleSpinner.className = "sidebar-spinner";
   toggleSpinner.style.display = "none";
   toggleSpinner.id = "sidebarSpinner";
   toggle.appendChild(toggleSpinner);

   toggle.addEventListener("click", function() {
      toggleSidebar();
   });

   content.innerHTML = "";

   for (var i = 0; i < cols.length; i++) {
      var col = cols[i];
      if (col.col_type === "rownames") continue;

      var entry = document.createElement("div");
      entry.className = "sidebar-col";
      entry.setAttribute("data-col-idx", i);

      // Header row: expand button + name + type
      var header = document.createElement("div");
      header.className = "sidebar-col-header";

      var expandBtn = document.createElement("span");
      expandBtn.className = "sidebar-expand-btn";
      expandBtn.title = "Show column summary";
      header.appendChild(expandBtn);

      var name = document.createElement("span");
      name.className = "sidebar-col-name";
      name.textContent = col.col_name;
      name.title = col.col_name;
      header.appendChild(name);

      var type = document.createElement("span");
      type.className = "sidebar-col-type";
      type.textContent = "<" + typeLabel(col) + ">";
      header.appendChild(type);

      entry.appendChild(header);

      // Sparkline histogram for numeric columns
      if (col.col_type === "numeric" && col.col_breaks && col.col_counts) {
         var sparkContainer = document.createElement("div");
         sparkContainer.className = "sidebar-sparkline";
         var svg = createSparklineSVG(col.col_breaks, col.col_counts);
         if (svg) sparkContainer.appendChild(svg);
         entry.appendChild(sparkContainer);
      }

      // Footer row: type-specific summary + NA count
      var footer = document.createElement("div");
      footer.className = "sidebar-col-footer";

      // Type-specific summary (left)
      var summaryText = "";
      if (col.col_type === "factor" && col.col_vals) {
         summaryText = col.col_vals.length + " levels";
      } else if (col.col_type === "boolean") {
         summaryText = "logical";
      }
      if (summaryText) {
         var summaryEl = document.createElement("span");
         summaryEl.className = "sidebar-col-summary";
         summaryEl.textContent = summaryText;
         footer.appendChild(summaryEl);
      }

      // NA count (right)
      var naCount = col.col_na_count || 0;
      if (naCount > 0 && totalRows > 0) {
         var naPct = ((naCount / totalRows) * 100);
         var naText = (naPct < 1 ? "<1" : Math.round(naPct)) + "% NA";
         var naEl = document.createElement("span");
         naEl.className = "sidebar-col-na";
         naEl.textContent = naText;
         naEl.title = naCount.toLocaleString() + " missing values";
         footer.appendChild(naEl);
      }

      if (footer.childNodes.length > 0) {
         entry.appendChild(footer);
      }

      // Stats panel (hidden until expanded)
      var statsPanel = document.createElement("div");
      statsPanel.className = "sidebar-stats";
      statsPanel.style.display = "none";
      entry.appendChild(statsPanel);

      // Click entry to scroll; click expand button to toggle stats
      (function(colIdx, statsEl, btnEl, colType) {
         var expanded = false;
         var loaded = false;

         btnEl.addEventListener("click", function(evt) {
            evt.stopPropagation();
            expanded = !expanded;
            btnEl.classList.toggle("expanded", expanded);

            if (expanded) {
               if (!loaded) {
                  // Show spinner while at least one summary fetch is in flight
                  var spinner = document.getElementById("sidebarSpinner");
                  pendingSummaryFetches++;
                  if (spinner) spinner.style.display = "";
                  fetchColumnSummary(colIdx + columnOffset, function(data) {
                     pendingSummaryFetches = Math.max(0, pendingSummaryFetches - 1);
                     if (spinner && pendingSummaryFetches === 0) {
                        spinner.style.display = "none";
                     }
                     // Aborted requests pass null -- leave loaded=false so a
                     // re-expand can retry.
                     if (data === null) return;
                     loaded = true;
                     renderColumnStats(statsEl, data, colType);
                     statsEl.style.display = "";
                     if (sidebarScrollbar_) sidebarScrollbar_.update();
                  });
               } else {
                  statsEl.style.display = "";
                  if (sidebarScrollbar_) sidebarScrollbar_.update();
               }
            } else {
               statsEl.style.display = "none";
               if (sidebarScrollbar_) sidebarScrollbar_.update();
            }
         });

         entry.addEventListener("click", function(evt) {
            scrollToColumn(colIdx);
         });
      })(i, statsPanel, expandBtn, col.col_type);

      content.appendChild(entry);
   }

   // Apply initial sidebar visibility -- toggle authoritatively so a previous
   // "expanded" class from an earlier bootstrap doesn't override saved state.
   var panel = document.getElementById("sidebarPanel");
   if (panel) {
      panel.classList.toggle("expanded", sidebarVisible);
   }

   // Attach the floating sidebar scrollbar after the content is in place
   // (so its initial measurement reflects the populated entries).
   attachSidebarScrollbar();
   if (sidebarScrollbar_) sidebarScrollbar_.update();
};

var toggleSidebar = function() {
   var panel = document.getElementById("sidebarPanel");
   if (!panel) return;
   sidebarVisible = !sidebarVisible;
   if (sidebarVisible) {
      panel.classList.add("expanded");
   } else {
      panel.classList.remove("expanded");
   }
   // Trigger grid resize after transition
   setTimeout(function() {
      renderVisibleRows(true);
      updateInfoBar();
      updateCustomScrollbars();
   }, TIMING.sidebarTransition);
   saveState();
};

var scrollToColumn = function(colIdx) {
   var viewport = document.getElementById("gridViewport");
   var thead = document.getElementById("data_cols");
   if (!viewport || !thead) return;

   var th = getHeaderCell(colIdx);
   if (!th) return;

   // Scroll horizontally so the column is visible
   var colLeft = th.offsetLeft;
   var colWidth = th.offsetWidth;
   var viewLeft = viewport.scrollLeft;
   var viewWidth = viewport.clientWidth;

   // Account for pinned column width
   var pinnedWidth = 0;
   if (rowNumbers && thead.children[0]) {
      pinnedWidth = thead.children[0].offsetWidth;
   }

   if (colLeft < viewLeft + pinnedWidth) {
      viewport.scrollLeft = colLeft - pinnedWidth;
   } else if (colLeft + colWidth > viewLeft + viewWidth) {
      viewport.scrollLeft = colLeft + colWidth - viewWidth;
   }

   // Briefly highlight the column header
   th.classList.add("highlight-flash");
   setTimeout(function() {
      th.classList.remove("highlight-flash");
   }, TIMING.columnFlash);
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

// Grid scrollbars (vertical + horizontal); sidebar gets its own vertical bar
// when initSidebar runs.
var gridScrollbarV_ = null;
var gridScrollbarH_ = null;
var sidebarScrollbar_ = null;
var sidebarScrollListener_ = null;

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

// Vertical scrollbar for the Summary sidebar. Appended below the toggle
// label so the bar tracks only the scrollable content list.
var attachSidebarScrollbar = function() {
   var sidebarPanel = document.getElementById("sidebarPanel");
   var sidebarContent = document.getElementById("sidebarContent");
   var sidebarToggle = document.getElementById("sidebarToggle");
   if (!sidebarPanel || !sidebarContent) return;

   // Tear down any previous instance + its scroll listener to avoid
   // accumulating handlers across re-bootstraps.
   if (sidebarScrollbar_) {
      sidebarScrollbar_.destroy();
      sidebarScrollbar_ = null;
   }
   if (sidebarScrollListener_) {
      sidebarContent.removeEventListener("scroll", sidebarScrollListener_);
      sidebarScrollListener_ = null;
   }

   sidebarScrollbar_ = attachCustomScrollbar(
      sidebarContent, sidebarPanel, "vertical", {
         getInsets: function() {
            return { top: sidebarToggle ? sidebarToggle.offsetHeight : 0 };
         }
      });

   sidebarScrollListener_ = function() {
      if (!sidebarScrollbar_) return;
      sidebarScrollbar_.show();
      sidebarScrollbar_.update();
   };
   sidebarContent.addEventListener("scroll", sidebarScrollListener_);
};

var updateCustomScrollbars = function() {
   if (gridScrollbarV_) gridScrollbarV_.update();
   if (gridScrollbarH_) gridScrollbarH_.update();
   if (sidebarScrollbar_) sidebarScrollbar_.update();
};

var showScrollbars = function() {
   if (gridScrollbarV_) gridScrollbarV_.show();
   if (gridScrollbarH_) gridScrollbarH_.show();
};

var destroyCustomScrollbars = function() {
   if (gridScrollbarV_) { gridScrollbarV_.destroy(); gridScrollbarV_ = null; }
   if (gridScrollbarH_) { gridScrollbarH_.destroy(); gridScrollbarH_ = null; }
   if (sidebarScrollbar_) { sidebarScrollbar_.destroy(); sidebarScrollbar_ = null; }
   if (sidebarScrollListener_) {
      var sidebarContent = document.getElementById("sidebarContent");
      if (sidebarContent) {
         sidebarContent.removeEventListener("scroll", sidebarScrollListener_);
      }
      sidebarScrollListener_ = null;
   }
};

// ==========================================================================
// Grid Lifecycle
// ==========================================================================

var initGrid = function(resCols, data) {
   if (resCols.error) {
      showError(resCols.error);
      return;
   }

   // Parse numeric col_breaks from strings (legacy JSON serializer issue)
   for (var i = 0; i < resCols.length; i++) {
      if (resCols[i].col_breaks) {
         for (var j = 0; j < resCols[i].col_breaks.length; j++) {
            if (typeof resCols[i].col_breaks[j] === "string")
               resCols[i].col_breaks[j] = parseFloat(resCols[i].col_breaks[j]);
         }
      }
   }

   cols = resCols;

   var loc = parseLocationUrl();

   // Determine max display columns
   if (loc.maxCols) {
      maxDisplayColumns = loc.maxCols > 0 ? loc.maxCols : cols.length;
   } else if (loc.maxDisplayColumns > 0) {
      maxDisplayColumns = loc.maxDisplayColumns;
   } else {
      maxDisplayColumns = cols.length;
   }

   if (loc.maxRows > 0) {
      maxRows = loc.maxRows;
   }

   // Total columns/rows from rownames metadata
   var resTotalCols = cols[0].total_cols;
   totalCols = resTotalCols > 0 ? resTotalCols : cols.length - 1;
   var resTotalRows = cols[0].total_rows;
   if (resTotalRows > 0) totalRows = resTotalRows;

   window.dataTableMaxColumns = totalCols;
   window.dataTableColumnOffset = 0;

   // Apply the data_viewer_show_summary preference as the default; saved
   // per-object state (below) overrides it when present.
   sidebarVisible = loc.showSummary;

   // Restore saved per-object UI state before headers are built (so pinning
   // order is correct) and before fetchRows (so sort/filters are sent).
   applySavedState(loadSavedState());

   // Build headers (pinned columns first, then unpinned)
   columnOrder = getColumnOrder();
   var thead = document.getElementById("data_cols");
   thead.innerHTML = "";

   for (var c = 0; c < columnOrder.length; c++) {
      var colIdx = columnOrder[c];
      thead.appendChild(createHeader(colIdx, cols[colIdx]));
   }
   applySortIndicators();

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
         updateSpacerHeight();
      });
   }

   // Set up scroll handler
   var viewport = document.getElementById("gridViewport");
   if (viewport) {
      scrollUpdateListener = onScroll;
      viewport.addEventListener("scroll", scrollUpdateListener);

      // Update custom scrollbar thumb position -- coalesce to one per frame
      scrollbarUpdateListener = function() {
         showScrollbars();
         if (!pendingScrollbarRaf) {
            pendingScrollbarRaf = requestAnimationFrame(function() {
               pendingScrollbarRaf = 0;
               updateCustomScrollbars();
            });
         }
      };
      viewport.addEventListener("scroll", scrollbarUpdateListener);

      // Force a final re-render when scrolling stops -- but not during
      // custom scrollbar drags, which generate spurious scrollend events
      scrollEndListener = function() {
         if (anyScrollbarDragging()) return;
         onScroll.cancel();
         lastScrollTop = viewport.scrollTop;
         lastScrollLeft = viewport.scrollLeft;
         renderVisibleRows();
         updateInfoBar();
         updateCustomScrollbars();
      };
      viewport.addEventListener("scrollend", scrollEndListener);
   }

   // Create custom scrollbars
   createCustomScrollbars();
   updateCustomScrollbars();

   // Set up resize handler -- recompute pinned overscroll padding too, since
   // it depends on viewport width.
   resizeListener = debounce(function() {
      applyPinnedColumns();
      renderVisibleRows(true);
      updateInfoBar();
      updateCustomScrollbars();
   }, TIMING.resizeDebounce);
   window.addEventListener("resize", resizeListener);

   // Update column frame callback
   window.columnFrameCallback(columnOffset, maxDisplayColumns);

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

   updateSpacerHeight();
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
   sortColumn = -1;
   sortDirection = "";
   cachedSearch = "";
   cachedFilterValues = [];
   pinnedColumns.clear();
   columnOrder = [];

   // Column widths
   measuredWidths = [];
   manualWidths = [];
   origColWidths = [];

   // Render window
   renderStart = 0;
   renderEnd = 0;
   renderedRowElements.clear();
   topSpacerRow = null;
   bottomSpacerRow = null;
   cachedPinnedOffsets = {};
   invalidatePinnedOffsets();
   needsAutoSize = false;

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

   // Sidebar
   pendingSummaryFetches = 0;
};

// ==========================================================================
// State Persistence
// ==========================================================================
//
// Per-object UI state (pinned columns, sidebar visibility, manual column
// widths, sort order, filter values) is saved to localStorage keyed by
// env+obj. State is loaded on bootstrap and applied before the first row
// fetch so it shows up in the initial render. Saves are debounced.

var STATE_VERSION = 1;
var STATE_KEY_PREFIX = "rstudio.dataViewer:";

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

var saveState = function() {
   var key = stateKey();
   if (!key) return;
   var state = {
      version: STATE_VERSION,
      pinnedColumns: Array.from(pinnedColumns),
      sidebarVisible: sidebarVisible,
      manualWidths: manualWidths.slice(),
      sort: sortColumn >= 0 ? { col: sortColumn, dir: sortDirection } : null,
      filters: cachedFilterValues.slice()
   };
   try {
      localStorage.setItem(key, JSON.stringify(state));
   } catch (e) {
      // localStorage may be unavailable (private browsing) or full
   }
};

var loadSavedState = function() {
   var key = stateKey();
   if (!key) return null;
   try {
      var raw = localStorage.getItem(key);
      if (!raw) return null;
      var state = JSON.parse(raw);
      if (state.version !== STATE_VERSION) return null;
      return state;
   } catch (e) {
      return null;
   }
};

var clearSavedState = function() {
   var key = stateKey();
   if (!key) return;
   try { localStorage.removeItem(key); } catch (e) {}
};

// Apply restored state. Must be called after `cols` is populated but before
// the first row fetch (so sort/filters are included in fetch params) and
// before headers are built (so pinning order is correct).
var applySavedState = function(state) {
   if (!state || !cols) return;
   var colCount = cols.length;

   if (Array.isArray(state.pinnedColumns)) {
      state.pinnedColumns.forEach(function(idx) {
         // Allow idx 0 -- when rowNumbers is false, the first column is a
         // regular data column that may be user-pinned.
         if (typeof idx === "number" && idx >= 0 && idx < colCount) {
            pinnedColumns.add(idx);
         }
      });
   }

   if (typeof state.sidebarVisible === "boolean") {
      sidebarVisible = state.sidebarVisible;
   }

   if (Array.isArray(state.manualWidths)) {
      for (var i = 0; i < state.manualWidths.length && i < colCount; i++) {
         var w = state.manualWidths[i];
         if (typeof w === "number" && w > 0) manualWidths[i] = w;
      }
   }

   if (state.sort &&
       typeof state.sort.col === "number" && state.sort.col < colCount &&
       (state.sort.dir === "asc" || state.sort.dir === "desc")) {
      sortColumn = state.sort.col;
      sortDirection = state.sort.dir;
   }

   if (Array.isArray(state.filters)) {
      cachedFilterValues = state.filters.slice(0, colCount);
   }
};

// Apply asc/desc CSS classes on header cells based on current sort state.
// Mirrors the inline class manipulation in handleSortClick, but works from
// the cached sortColumn/sortDirection (used after restoring saved state).
var applySortIndicators = function() {
   var thead = document.getElementById("data_cols");
   if (!thead) return;
   for (var i = 0; i < thead.children.length; i++) {
      var th = thead.children[i];
      th.classList.remove("sorting", "sorting_asc", "sorting_desc");
      var colIdx = parseInt(th.getAttribute("data-col-idx"), 10);
      if (colIdx === sortColumn && sortDirection) {
         th.classList.add("sorting_" + sortDirection);
      } else {
         th.classList.add("sorting");
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

   var info = document.getElementById("rsGridData_info");
   if (info) info.textContent = "";

   var viewport = document.getElementById("gridViewport");
   if (viewport) {
      if (scrollUpdateListener) {
         viewport.removeEventListener("scroll", scrollUpdateListener);
         scrollUpdateListener = null;
      }
      if (scrollbarUpdateListener) {
         viewport.removeEventListener("scroll", scrollbarUpdateListener);
         scrollbarUpdateListener = null;
      }
      if (scrollEndListener) {
         viewport.removeEventListener("scrollend", scrollEndListener);
         scrollEndListener = null;
      }
      viewport.scrollTop = 0;
      viewport.scrollLeft = 0;
   }
   if (resizeListener) {
      window.removeEventListener("resize", resizeListener);
      resizeListener = null;
   }
   if (pendingScrollbarRaf) {
      cancelAnimationFrame(pendingScrollbarRaf);
      pendingScrollbarRaf = 0;
   }
   onScroll.cancel();
};

var bootstrap = function(data) {
   bootstrapping = true;
   destroyGrid();

   if (!data) {
      fetchColumns(function(result) {
         initGrid(result);
      });
   } else {
      // Data provided directly (data import preview)
      if (data.columns) {
         initGrid(data.columns, data.data);
      }
   }
};

// ==========================================================================
// Filter/Header UI Visibility
// ==========================================================================

var setHeaderUIVisible = function(visible, initialize, hide) {
   var thead = document.getElementById("data_cols");

   if (thead === null || cols === null) {
      postInitActions["setHeaderUIVisible"] = visible
         ? function() { setHeaderUIVisible(true, initialize, hide); }
         : null;
      return false;
   }

   if (!visible && hide) {
      hide(thead);
      if (dismissActivePopup) dismissActivePopup(true);
   }

   for (var i = 0; i < thead.children.length; i++) {
      var th = thead.children[i];
      var colIdx = parseInt(th.getAttribute("data-col-idx"), 10);
      var col = cols[colIdx];

      if (visible) {
         var el = initialize(th, col, colIdx);
         if (el) {
            el.classList.add("header-injected-ui");
            th.appendChild(el);
         }
      } else {
         // Remove only the injected UI, never the permanent .resizer or
         // .headerCell. Marker class is set when the UI is appended above.
         var injected = th.querySelector(".header-injected-ui");
         if (injected) th.removeChild(injected);
      }
   }

   renderVisibleRows(true);
   updateCustomScrollbars();
   return true;
};

// ==========================================================================
// Column Pagination
// ==========================================================================

var columnNav = function(newOffset) {
   if (bootstrapping) return;
   newOffset = Math.max(0, Math.min(totalCols - maxDisplayColumns, newOffset));
   if (columnOffset !== newOffset) {
      columnOffset = newOffset;
      bootstrap();
   }
};

// ==========================================================================
// Window API Exports
// ==========================================================================

window.setFilterUIVisible = function(visible) {
   return setHeaderUIVisible(
      visible,
      function(th, col, i) {
         if (col.col_search_type === "numeric" ||
             col.col_search_type === "character" ||
             col.col_search_type === "factor" ||
             col.col_search_type === "boolean") {
            return createFilterUI(i, col);
         }
         return null;
      },
      function() {
         var hadFilters = cachedFilterValues.some(function(v) {
            return v && v.length > 0;
         });
         cachedFilterValues = [];
         if (hadFilters) {
            invalidateCache();
            fetchRows(0, FETCH_SIZE);
            saveState();
         }
      }
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
      }
   );
};

window.refreshData = function() {
   bootstrap();
};

window.toggleSidebar = function() {
   toggleSidebar();
};

var debouncedSearch = debounce(function(text) {
   if (text !== cachedSearch) {
      cachedSearch = text;
      invalidateCache();
      fetchRows(0, FETCH_SIZE, function() {
         scrollToTop();
      });
   }
}, TIMING.searchDebounce);

window.applySearch = function(text) {
   debouncedSearch(text);
};

window.onActivate = function() {
   // Restore scroll position and re-render
   var viewport = document.getElementById("gridViewport");
   if (viewport) {
      viewport.scrollTop = lastScrollTop;
      viewport.scrollLeft = lastScrollLeft;
   }

   // Re-run auto-sizing if the initial sizing happened while the tab
   // was hidden (measurements are wrong with no layout).
   if (needsAutoSize && cols) {
      needsAutoSize = false;
      autoSizeColumns();
      applyPinnedColumns();
   }

   renderVisibleRows(true);
   updateInfoBar();
   updateCustomScrollbars();
};

window.onDeactivate = function() {
   // Save scroll position
   var viewport = document.getElementById("gridViewport");
   if (viewport) {
      lastScrollTop = viewport.scrollTop;
      lastScrollLeft = viewport.scrollLeft;
   }
};

// Called from GWT when the data viewer tab is being closed (dismissType
// CLOSE, not MOVE). Discard the saved state so it doesn't accumulate in
// localStorage past the lifetime of the tab.
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
      case "columnFrameCallback":
         window.columnFrameCallback = value;
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

window.columnFrameCallback = function() {};

window.getActiveColumn = function() {
   return activeColumnInfo;
};

window.nextColumnPage = function() {
   columnNav(columnOffset + maxDisplayColumns);
};

window.prevColumnPage = function() {
   columnNav(columnOffset - maxDisplayColumns);
};

window.firstColumnPage = function() {
   columnNav(0);
};

window.lastColumnPage = function() {
   columnNav(totalCols - maxDisplayColumns);
};

window.setOffsetAndMaxColumns = function(newOffset, newMax) {
   if (bootstrapping) return;
   if (newOffset >= totalCols) return;
   if (newOffset >= 0) columnOffset = newOffset;
   if (newMax > 0) {
      maxDisplayColumns = Math.min(totalCols - columnOffset, newMax);
   }
   bootstrap();
};

window.isLimitedColumnFrame = function() {
   return totalCols > maxDisplayColumns;
};

// Expose these for GWT interop (DataTable.java sets them)
window.dataTableMaxColumns = 0;
window.dataTableColumnOffset = 0;

// ==========================================================================
// Initialization
// ==========================================================================

initResizeHandlers();

var loc = parseLocationUrl();
var dataMode = loc.dataSource || "server";

document.addEventListener("DOMContentLoaded", function() {
   if (dataMode === "server") {
      bootstrap();
   }
});

})();
