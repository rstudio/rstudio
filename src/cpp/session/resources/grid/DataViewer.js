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
var BUFFER_ROWS = 50;
var FETCH_SIZE = 200;
var FETCH_THRESHOLD = 100;

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
var sidebarVisible = false;

// Pinned columns (set of column indices; column 0/rownames is always implicitly pinned)
var pinnedColumns = new Set();

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
   if (!html) return "";
   if (typeof html === "number") return html.toString();
   var replacements = {
      "<": "&lt;", ">": "&gt;", "&": "&amp;", '"': "&quot;", " ": "&nbsp;"
   };
   return html.replace(/[&<>" ]/g, function(ch) { return replacements[ch]; });
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
      maxDisplayColumns: -1, maxCols: 0, maxRows: 0
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

var fetchColumns = function(callback) {
   var params = "show=cols&" + window.location.search.substring(1) +
      "&column_offset=" + columnOffset;

   fetch("../grid_data", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: params
   })
   .then(function(response) {
      if (!response.ok) return response.text().then(function(t) { throw new Error(t); });
      return response.json();
   })
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

   var loc = parseLocationUrl();
   var params = {
      env: loc.env,
      obj: loc.obj,
      cache_key: loc.cacheKey,
      show: "data",
      start: start,
      length: length,
      draw: ++drawCounter,
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

   fetch("../grid_data", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: buildFormData(params),
      signal: controller.signal
   })
   .then(function(response) {
      if (!response.ok) return response.text().then(function(t) { throw new Error(t); });
      return response.json();
   })
   .then(function(result) {
      pendingFetches.delete(key);
      if (result.error) { showError(result.error); return; }
      totalRows = result.recordsTotal;
      filteredRows = result.recordsFiltered;
      // Store rows in cache
      for (var i = 0; i < result.data.length; i++) {
         rowCache.set(start + i, result.data[i]);
      }
      if (callback) callback();
      renderVisibleRows();
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
};

// ==========================================================================
// Cell Rendering
// ==========================================================================

var renderCellContents = function(data, colIdx, rowData, clazz) {
   // NA handling: 0 means NA, or null when displayNullsAsNAs is set
   if (data === 0 || (displayNullsAsNAs && data === null)) {
      return '<span class="naCell">NA</span>';
   }

   var didHighlight = false;

   // Row name column: parse JSON
   if (rowNumbers && colIdx === 0) {
      data = JSON.parse(data).toString();
   }

   if (clazz === "dataCell") {
      if (data.substring(0, 5) === "list(" && data.indexOf("=") > 0) {
         var varCount = data.split("=").length - 1;
         var varLabel = varCount > 1 ? "variables" : "variable";
         data = varCount + " " + varLabel;
      }
   } else if (cachedSearch.length > 0) {
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

   var escaped = didHighlight ? data : escapeHtml(data);

   // Data/list cell links
   if (clazz === "dataCell" || clazz === "listCell") {
      var cbName = clazz === "dataCell" ? "dataViewerCallback" : "listViewerCallback";
      var cbRow = rowData[0];
      var cbCol = colIdx + columnOffset;
      var href = "javascript:window." + cbName + "(" + cbRow + ", " + cbCol + ")";
      var linkEl = document.createElement("a");
      linkEl.className = "viewerLink";
      linkEl.href = href;
      var imgEl = document.createElement("img");
      imgEl.className = "viewerImage";
      imgEl.src = clazz === "dataCell" ? "data-viewer.png" : "object-viewer.png";
      linkEl.appendChild(imgEl);
      escaped = "<i>" + escaped + "</i> " + linkEl.outerHTML;
   }

   return escaped;
};

// Cached pinned offsets — recomputed in applyPinnedColumns and renderVisibleRows
var cachedPinnedOffsets = {};

var createCell = function(data, colIdx, rowData, clazz) {
   var td = document.createElement("td");
   var contents = renderCellContents(data, colIdx, rowData, clazz);

   // Determine classes
   var classes = [clazz];
   if (contents.length >= 10) classes.push("largeCell");
   if (isColumnPinned(colIdx)) {
      classes.push("pinned");
      td.style.left = (cachedPinnedOffsets[colIdx] || 0) + "px";
   }

   td.className = classes.join(" ");
   td.innerHTML = contents;

   if (typeof data === "string") td.title = data;

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

   // Pin icon to the left of the label (not shown for rownames — always pinned)
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

   // Resize handle — appended to <th> directly (not inside headerCell)
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

// Cached column order — recomputed when pinning changes
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
   rebuildHeaders();
   renderVisibleRows();
};

// Rebuild headers in the current column order (pinned first, then unpinned)
var rebuildHeaders = function() {
   var thead = document.getElementById("data_cols");
   if (!thead || !cols) return;

   columnOrder = getColumnOrder();

   thead.innerHTML = "";
   for (var i = 0; i < columnOrder.length; i++) {
      var colIdx = columnOrder[i];
      var th = createHeader(colIdx, cols[colIdx]);
      thead.appendChild(th);
   }

   autoSizeColumns();
   applyPinnedColumns();
};

// Compute the cumulative left offset for each pinned column based on render order
var getPinnedOffsets = function() {
   var offsets = {};
   var cumulative = 0;
   var thead = document.getElementById("data_cols");
   if (!thead) return offsets;

   for (var i = 0; i < columnOrder.length; i++) {
      var colIdx = columnOrder[i];
      if (!isColumnPinned(colIdx)) break; // pinned columns are all at the front
      offsets[colIdx] = cumulative;
      var th = thead.children[i];
      cumulative += th ? th.offsetWidth : 0;
   }
   return offsets;
};

// Apply pinned styling to header cells
var applyPinnedColumns = function() {
   var thead = document.getElementById("data_cols");
   if (!thead) return;

   var offsets = getPinnedOffsets();

   for (var i = 0; i < thead.children.length; i++) {
      var th = thead.children[i];
      var colIdx = columnOrder[i];
      var pinIcon = th.querySelector(".pin-icon");

      if (isColumnPinned(colIdx)) {
         th.classList.add("pinned");
         th.style.left = (offsets[colIdx] || 0) + "px";
         if (pinIcon) pinIcon.classList.add("pinned");
      } else {
         th.classList.remove("pinned");
         th.style.left = "";
         if (pinIcon) pinIcon.classList.remove("pinned");
      }
   }
};

// ==========================================================================
// Column Auto-Sizing
// ==========================================================================

// Measure the natural text width of a string using a hidden off-screen element.
// The element is created once and reused for all measurements.
var measureEl = null;
var measureTextWidth = function(text, bold) {
   if (!measureEl) {
      measureEl = document.createElement("div");
      measureEl.style.cssText =
         "position:absolute;visibility:hidden;height:auto;width:auto;" +
         "white-space:nowrap;font-size:11px;" +
         "font-family:'DejaVu Sans','Lucida Grande','Segoe UI',Verdana,Helvetica,sans-serif;" +
         "padding:0 5px;";
      document.body.appendChild(measureEl);
   }
   measureEl.style.fontWeight = bold ? "bold" : "normal";
   measureEl.textContent = text;
   return measureEl.offsetWidth;
};

// Compute column widths by measuring header text and a sample of cached cell
// values, then apply them with table-layout:fixed.
var autoSizeColumns = function() {
   var table = document.getElementById("rsGridData");
   var thead = document.getElementById("data_cols");
   if (!table || !thead || !thead.children.length || !cols) return;

   var colCount = thead.children.length;
   measuredWidths = [];
   var totalWidth = 0;

   // Padding + border + sort indicator allowance per cell
   var cellPadding = 28;
   // Extra space for the pin icon in non-rownames columns
   var pinIconWidth = 20;

   for (var i = 0; i < colCount; i++) {
      var col = cols[i];

      // Measure header text width (bold)
      var headerText = col.col_name || "";
      var headerExtra = cellPadding + ((i === 0 && rowNumbers) ? 0 : pinIconWidth);
      var maxW = measureTextWidth(headerText, true) + headerExtra;

      // Measure a sample of cell values from the cache
      var sampleSize = Math.min(rowCache.size, 100);
      for (var r = 0; r < sampleSize; r++) {
         var row = rowCache.get(r);
         if (!row) continue;
         var cellVal = row[i];
         if (cellVal === 0 || cellVal === null || cellVal === undefined) {
            // NA — short
            cellVal = "NA";
         }
         var cellText = String(cellVal);
         // For row names (col 0), the value is JSON-encoded
         if (i === 0 && rowNumbers) {
            try { cellText = JSON.parse(cellText).toString(); } catch(e) {}
         }
         var cellW = measureTextWidth(cellText, i === 0 && rowNumbers) + cellPadding;
         if (cellW > maxW) maxW = cellW;
      }

      // Clamp to min/max — allow wider max for character columns
      var upperBound = (col.col_type === "character") ? MAX_COL_WIDTH_CHAR : MAX_COL_WIDTH;
      var w = Math.max(MIN_COL_WIDTH, Math.min(upperBound, maxW));
      measuredWidths.push(w);
      totalWidth += w;
   }

   // If columns are narrower than the viewport, distribute extra space
   var viewport = document.getElementById("gridViewport");
   var viewportWidth = viewport ? viewport.clientWidth : 0;
   if (totalWidth < viewportWidth && measuredWidths.length > 0) {
      var extra = viewportWidth - totalWidth;
      var perCol = extra / measuredWidths.length;
      for (var i = 0; i < measuredWidths.length; i++) {
         measuredWidths[i] = Math.round(measuredWidths[i] + perCol);
      }
      totalWidth = viewportWidth;
   }

   // Apply widths and lock to fixed layout
   for (var i = 0; i < thead.children.length; i++) {
      thead.children[i].style.width = measuredWidths[i] + "px";
   }

   table.style.width = totalWidth + "px";
   table.style.tableLayout = "fixed";
};

// ==========================================================================
// Column Resize
// ==========================================================================

var initResizeHandlers = function() {
   document.addEventListener("mousedown", function(evt) {
      if (evt.target.className !== "resizer") return;

      resizingColIdx = parseInt(evt.target.getAttribute("data-col"));
      didResize = true;
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

      evt.preventDefault();
   });

   document.addEventListener("mousemove", function(evt) {
      if (resizingColIdx === null) return;
      var delta = evt.clientX - initResizeX;
      applyResizeDelta(delta);
      evt.preventDefault();
   });

   document.addEventListener("mouseup", function(evt) {
      if (resizingColIdx === null) return;
      resizingColIdx = null;
   });

   document.addEventListener("mouseleave", function(evt) {
      if (resizingColIdx === null) return;
      resizingColIdx = null;
   });
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

      var updateView = debounce(function(v) {
         var searchText = "";
         v = v.replace(/[^-0-9 .]/g, "");
         var digit = v.match(/^\s*-?\d+\.?\d*\s*$/);
         if (digit !== null && digit.length > 0) {
            searchText = digit[0].trim();
         } else {
            var matches = v.match(/^\s*(-?\d+\.?\d*)\s*-\s*(-?\d+\.?\d*)\s*/);
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
      }, 200);

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
   }, onDismiss, false);

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
   }, 200);

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
   }, onDismiss, false);

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
   }, onDismiss, false);

   return ele;
};

var invokeFilterPopup = function(ele, buildPopup, onDismiss, dismissOnClick) {
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

   var checkLightDismiss = function(evt) {
      if (popup && (!dismissOnClick || !popup.contains(evt.target))) {
         dismissPopup(true);
      }
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
      if (columnsPopup == null || columnsPopup !== th) {
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

   var viewport = document.getElementById("gridViewport");
   var scrollTop = viewport ? viewport.scrollTop : 0;
   var viewportH = viewport ? viewport.clientHeight : 0;
   var first = Math.floor(scrollTop / ROW_HEIGHT) + 1;
   var last = Math.min(first + Math.ceil(viewportH / ROW_HEIGHT) - 1, activeRows);

   var text = "Showing " + first.toLocaleString() + " to " + last.toLocaleString() +
      " of " + activeRows.toLocaleString() + " entries";
   if (filteredRows > 0 && filteredRows < totalRows) {
      text += " (filtered from " + totalRows.toLocaleString() + " total)";
   }
   info.textContent = text;
};

var renderVisibleRows = function() {
   var viewport = document.getElementById("gridViewport");
   var tbody = document.getElementById("gridBody");
   if (!viewport || !tbody || !cols) return;

   var scrollTop = viewport.scrollTop;
   var viewportH = viewport.clientHeight;
   var activeRows = filteredRows;

   if (activeRows === 0) {
      tbody.innerHTML = "";
      return;
   }

   var firstVisible = Math.floor(scrollTop / ROW_HEIGHT);
   var visibleCount = Math.ceil(viewportH / ROW_HEIGHT);

   var newStart = Math.max(0, firstVisible - BUFFER_ROWS);
   var newEnd = Math.min(activeRows - 1, firstVisible + visibleCount + BUFFER_ROWS);

   // Check if we need to fetch more data — iterate over block-aligned
   // boundaries so we don't miss blocks when newStart is unaligned.
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
   cachedPinnedOffsets = getPinnedOffsets();

   // Build rows, counting how many we actually render (some may be
   // uncached and skipped). The bottom spacer absorbs the deficit.
   var fragment = document.createDocumentFragment();
   var renderedCount = 0;

   for (var r = newStart; r <= newEnd; r++) {
      var rowData = rowCache.get(r);
      if (!rowData) continue;
      renderedCount++;

      var tr = document.createElement("tr");

      for (var c = 0; c < columnOrder.length; c++) {
         var colIdx = columnOrder[c];
         var cellData = colIdx === 0 ? rowData[0] : rowData[colIdx];
         var clazz = getColClass(cols[colIdx]);
         if (colIdx === 0 && rowNumbers) clazz = "textCell";
         var td = createCell(cellData, colIdx, rowData, clazz);
         tr.appendChild(td);
      }

      fragment.appendChild(tr);
   }

   // Spacer heights. Skipped (uncached) rows are added to the bottom
   // spacer so the total height stays correct:
   // topSpacer + renderedCount * ROW_HEIGHT + bottomSpacer = activeRows * ROW_HEIGHT
   var colSpan = columnOrder.length || 1;
   var expectedCount = newEnd - newStart + 1;
   var skippedRows = expectedCount - renderedCount;

   // Update tbody with spacer row + data rows
   tbody.innerHTML = "";

   // Top spacer — pushes data rows to their correct vertical position.
   if (newStart > 0) {
      var spacerTr = document.createElement("tr");
      spacerTr.className = "spacer-row";
      var spacerTd = document.createElement("td");
      spacerTd.colSpan = colSpan;
      spacerTd.style.height = (newStart * ROW_HEIGHT) + "px";
      spacerTd.style.padding = "0";
      spacerTd.style.border = "none";
      spacerTr.appendChild(spacerTd);
      tbody.appendChild(spacerTr);
   }

   tbody.appendChild(fragment);

   // Bottom spacer — fills the remaining scroll height, plus absorbs
   // any height deficit from skipped (uncached) rows.
   var remainingRows = activeRows - newEnd - 1 + skippedRows;
   if (remainingRows > 0) {
      var bottomTr = document.createElement("tr");
      bottomTr.className = "spacer-row";
      var bottomTd = document.createElement("td");
      bottomTd.colSpan = colSpan;
      bottomTd.style.height = (remainingRows * ROW_HEIGHT) + "px";
      bottomTd.style.padding = "0";
      bottomTd.style.border = "none";
      bottomTr.appendChild(bottomTd);
      tbody.appendChild(bottomTr);
   }

   renderStart = newStart;
   renderEnd = newEnd;
};

var onScroll = debounce(function() {
   var viewport = document.getElementById("gridViewport");
   if (!viewport) return;
   lastScrollTop = viewport.scrollTop;
   lastScrollLeft = viewport.scrollLeft;
   renderVisibleRows();
   updateInfoBar();
}, 16);

// ==========================================================================
// Sidebar
// ==========================================================================

var createSparklineSVG = function(breaks, counts) {
   if (!breaks || !counts || counts.length === 0) return null;

   var max = 0;
   for (var i = 0; i < counts.length; i++) {
      if (counts[i] > max) max = counts[i];
   }
   if (max === 0) return null;

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
      svg.appendChild(rect);
   }

   return svg;
};

var typeLabel = function(col) {
   var type = col.col_type_r || col.col_type || "";
   // Map common R types to short labels
   var map = {
      "integer": "int", "double": "dbl", "character": "chr",
      "logical": "lgl", "complex": "cpl", "raw": "raw",
      "factor": "fct", "Date": "date", "POSIXct": "dttm",
      "POSIXlt": "dttm", "numeric": "dbl"
   };
   return map[type] || type;
};

var initSidebar = function() {
   var content = document.getElementById("sidebarContent");
   var toggle = document.getElementById("sidebarToggle");
   if (!content || !toggle || !cols) return;

   toggle.textContent = "Columns";
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

      // Header row: name + type
      var header = document.createElement("div");
      header.className = "sidebar-col-header";

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

      // Factor summary
      if (col.col_type === "factor" && col.col_vals) {
         var summary = document.createElement("div");
         summary.className = "sidebar-col-summary";
         summary.textContent = col.col_vals.length + " levels";
         entry.appendChild(summary);
      }

      // Boolean summary
      if (col.col_type === "boolean") {
         var boolSummary = document.createElement("div");
         boolSummary.className = "sidebar-col-summary";
         boolSummary.textContent = "logical";
         entry.appendChild(boolSummary);
      }

      // Click to scroll grid to column
      (function(colIdx) {
         entry.addEventListener("click", function() {
            scrollToColumn(colIdx);
         });
      })(i);

      content.appendChild(entry);
   }
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
      renderVisibleRows();
      updateInfoBar();
   }, 200);
};

var scrollToColumn = function(colIdx) {
   var viewport = document.getElementById("gridViewport");
   var thead = document.getElementById("data_cols");
   if (!viewport || !thead) return;

   var th = thead.children[colIdx];
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

   if (loc.maxRows != null && loc.maxRows > 0) {
      maxRows = loc.maxRows;
   }

   // Total columns from rownames metadata
   var resTotalCols = cols[0].total_cols;
   totalCols = resTotalCols > 0 ? resTotalCols : cols.length - 1;

   window.dataTableMaxColumns = totalCols;
   window.dataTableColumnOffset = 0;

   // Build headers (pinned columns first, then unpinned)
   columnOrder = getColumnOrder();
   var thead = document.getElementById("data_cols");
   thead.innerHTML = "";

   for (var c = 0; c < columnOrder.length; c++) {
      var colIdx = columnOrder[c];
      thead.appendChild(createHeader(colIdx, cols[colIdx]));
   }

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
      viewport.addEventListener("scroll", onScroll);
      // Force a final re-render when scrolling stops — the debounced handler
      // can miss the final position in some edge cases (e.g. holding the
      // scrollbar still then releasing).
      viewport.addEventListener("scrollend", function() {
         onScroll.cancel();
         lastScrollTop = viewport.scrollTop;
         lastScrollLeft = viewport.scrollLeft;
         renderVisibleRows();
         updateInfoBar();
      });
   }

   // Set up resize handler
   window.addEventListener("resize", debounce(function() {
      renderVisibleRows();
      updateInfoBar();
   }, 75));

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

var destroyGrid = function() {
   // Dismiss any active popups
   if (dismissActivePopup) dismissActivePopup(true);

   // Clean state
   cols = null;
   resizingColIdx = null;
   origColWidths = [];
   initResizingWidth = null;
   origTableWidth = null;
   initResizeX = null;
   resizingBoundsExceeded = 0;
   dismissActivePopup = null;
   cachedSearch = "";
   cachedFilterValues = [];
   sortColumn = -1;
   sortDirection = "";
   manualWidths = [];
   measuredWidths = [];
   pinnedColumns.clear();
   cachedPinnedOffsets = {};

   columnsPopup = null;
   activeColumnInfo = {};

   invalidateCache();

   // Clear DOM
   var thead = document.getElementById("data_cols");
   if (thead) thead.innerHTML = "";
   var tbody = document.getElementById("gridBody");
   if (tbody) { tbody.innerHTML = ""; }

   var info = document.getElementById("rsGridData_info");
   if (info) info.textContent = "";

   var viewport = document.getElementById("gridViewport");
   if (viewport) {
      viewport.removeEventListener("scroll", onScroll);
      viewport.scrollTop = 0;
      viewport.scrollLeft = 0;
   }
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
         if (el) th.appendChild(el);
      } else if (th.children.length > 1) {
         th.removeChild(th.lastChild);
      }
   }

   renderVisibleRows();
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

var debouncedSearch = debounce(function(text) {
   if (text !== cachedSearch) {
      cachedSearch = text;
      invalidateCache();
      fetchRows(0, FETCH_SIZE, function() {
         scrollToTop();
      });
   }
}, 100);

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
   renderVisibleRows();
   updateInfoBar();
};

window.onDeactivate = function() {
   // Save scroll position
   var viewport = document.getElementById("gridViewport");
   if (viewport) {
      lastScrollTop = viewport.scrollTop;
      lastScrollLeft = viewport.scrollLeft;
   }
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
