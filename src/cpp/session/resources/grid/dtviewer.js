/*jshint browser:true, strict:false, curly:false, indent:3*/

/*
 * dtviewer.js
 *
 * Copyright (C) 2021 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
(function () {
  // the data table itself
  var table;

  // the column definitions from the server
  var cols;

  // the column currently being resized
  var resizingColIdx = null;

  // the column widths (on initial draw)
  var origColWidths = [];

  // the initial width of the column that we are resizing
  var initResizingWidth = null;

  // the starting table width before resize
  var origTableWidth = null;

  // initial resize X position
  var initResizeX = null;

  // the amount that resizing bounds have exceeded
  var resizingBoundsExceeded = 0;

  // dismiss the active filter popup, if any
  var dismissActivePopup = null;

  // cached search/filter values--it's expensive to pull these from a new API
  // instance every time we render a cell, so we cache them
  var cachedSearch = "";
  var cachedFilterValues = [];

  // the height of the table at the last time we adjusted it to fit its window
  var lastHeight = 0;

  // the table header height of the table at the last time we adjusted it to fit
  var lastHeaderHeight = 0;

  // scroll handlers; these are detached when the data viewer is hidden
  var detachedHandlers = [];
  var lastScrollPos = 0;

  // display nulls as NAs
  var displayNullsAsNAs = false;

  // status text (replaces "Showing x of y...")
  var statusTextOverride = null;

  // enables ordering in the table
  var ordering = true;

  // show row numbers in index column
  var rowNumbers = true;

  // list of calls to defer after table is init (e.g. showing headers)
  var postInitActions = {};

  // callback to trigger column options
  var onColumnOpen;

  // callback to dismiss column options
  var onColumnDismiss;

  // reference to the column being opened
  var columnsPopup = null;

  // Active column properties
  var activeColumnInfo = {};

  // manually adjusted widths of each column
  var manualWidths = [];

  // offset from which to start rendering columns
  var columnOffset = 0;

  // maximum number of columns to draw
  // the default is maintained separately for view element considerations
  var defaultMaxColumns = 50;
  var maxColumns = defaultMaxColumns;

  // boolean for whether bootstrapping is occurring, used to
  // rate limit certain events
  var bootstrapping = false;

  // helper for creating a tag with properties + content
  // (created as a string)
  var createTag = function (tag, content, attributes) {
    // if content is an object and attributes is undefined,
    // treat this as request to create tag with attributes
    // but no content
    if (typeof content === "object" && typeof attributes === "undefined") {
      attributes = content;
      content = "";
    }

    // ensure attributes is an object
    attributes = attributes || {};

    // compute inner attributes
    var parts = [];
    for (var key in attributes) {
      // extract value
      var value = attributes[key];

      // join arrays of values
      if (Object.prototype.toString.call(value) === "[object Array]") value = value.join(" ");

      // skip non-string values
      if (typeof value !== "string") continue;

      // push attribute
      parts.push(key + '="' + value.replace(/"/g, "&quot;") + '"');
    }

    // build the final html
    var opener = "<" + tag + " " + parts.join(" ") + ">";
    var closer = "</" + tag + ">";
    return opener + (content || "") + closer;
  };

  var isHeaderWidthMismatched = function () {
    // find the elements to measure (they may not exist)
    var rs = document.getElementById("rsGridData");
    if (!rs || !rs.firstChild.clientWidth || !rs.firstChild.clientWidth > 0) return false;
    var sh = document.getElementsByClassName("dataTables_scrollHeadInner");
    if (sh.length === 0 || !sh[0].firstChild || !sh[0].firstChild.firstChild) return false;

    // match the widths
    return rs.firstChild.clientWidth !== sh[0].firstChild.firstChild.clientWidth;
  };

  // update search/filter value cache
  var updateCachedSearchFilter = function () {
    if (table) {
      cachedSearch = table.search();
      cachedFilterValues = [];
      for (var idx = 0; idx < table.columns()[0].length; idx++) {
        cachedFilterValues.push(table.columns(idx).search()[0]);
      }
    }
  };

  // throttle to avoid redrawing the table too frequently (when filtering,
  // resizing, etc.)
  var debounce = function (func, wait) {
    var timeout;
    return function () {
      var context = this,
        args = arguments;
      var later = function () {
        timeout = null;
        func.apply(context, args);
      };
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
    };
  };

  // show an error--not recoverable; user must click 'retry' to reload
  var showError = function (msg) {
    document.getElementById("errorWrapper").style.display = "block";
    document.getElementById("errorMask").style.display = "block";
    document.getElementById("error").textContent = msg;
    var rsGridData = document.getElementById("rsGridData");
    if (rsGridData) rsGridData.style.display = "none";
  };

  // simple HTML escaping (avoid XSS in data)
  var escapeHtml = function (html) {
    if (!html) return "";

    // handle special cells
    if (typeof html === "number") return html.toString();

    // in other types, replace special characters
    var replacements = {
      "<": "&lt;",
      ">": "&gt;",
      "&": "&amp;",
      '"': "&quot;",
      " ": "&nbsp;",
    };
    return html.replace(/[&<> ]/g, function (ch) {
      return replacements[ch];
    });
  };

  var highlightSearchMatch = function (data, search, pos) {
    return (
      escapeHtml(data.substring(0, pos)) +
      '<span class="searchMatch">' +
      escapeHtml(data.substring(pos, pos + search.length)) +
      "</span>" +
      escapeHtml(data.substring(pos + search.length, data.length))
    );
  };

  // render cell contents--if no search is active, just renders the data
  // literally; when search is active, highlights the portion of the text that
  // matches the search
  var renderCellContents = function (data, type, row, meta, clazz) {
    // usually data is a string; 0 is a special value signifying NA
    if (data === 0 || (displayNullsAsNAs && data === null)) {
      return '<span class="naCell">NA</span>';
    }

    if (clazz === "dataCell") {
      // a little ugly: R deparses data cell values as list(col = val, col = val...).  when rendering
      // a data cell which appears to have this format, count the assignment tokens to get a variable
      // count and show that as a summary.
      if (data.substring(0, 5) === "list(" && data.indexOf("=") > 0) {
        var varCount = data.split("=").length - 1;
        data = "" + varCount + " variable";
        if (varCount > 1) data += "s";
      }
    } else if (cachedSearch.length > 0) {
      // if row matches because of a global search, highlight that
      var idx = data.toLowerCase().indexOf(cachedSearch.toLowerCase());
      if (idx >= 0) {
        return highlightSearchMatch(data, cachedSearch, idx);
      }
    }

    // if row matches because of a column search (characterwise), highlight that
    if (meta.col < cachedFilterValues.length) {
      var colSearch = cachedFilterValues[meta.col];
      if (colSearch.indexOf("character|") === 0) {
        colSearch = decodeURIComponent(parseSearchString(colSearch));
        var colIdx = data.toLowerCase().indexOf(colSearch.toLowerCase());
        if (colIdx >= 0) {
          return highlightSearchMatch(data, colSearch, colIdx);
        }
      }
    }

    var escaped = escapeHtml(data);

    // special additional rendering for cells which themselves contain data frames or lists:
    // these include an icon that can be clicked to view contents
    if (clazz === "dataCell" || clazz === "listCell") {
      escaped =
        "<i>" +
        escaped +
        "</i> " +
        '<a class="viewerLink" href="javascript:window.' +
        (clazz === "dataCell" ? "dataViewerCallback" : "listViewerCallback") +
        "(" +
        row[0] +
        ", " +
        (meta.col + columnOffset) +
        ')">' +
        '<img src="' +
        (clazz === "dataCell" ? "data-viewer.png" : "object-viewer.png") +
        '" class="viewerImage" /></a>';
    }

    return escaped;
  };

  var renderCellClass = function (data, type, row, meta, clazz) {
    // render cell contents
    var contents = renderCellContents(data, type, row, meta, clazz);

    // compute classes for tag
    var classes = [clazz];

    // treat data with more than 10 characters as 'long'
    if (contents.length >= 10) classes.push("largeCell");

    // compute title (if any)
    var title;
    if (typeof data === "string") title = data;

    // produce tag
    return createTag("div", contents, {
      class: classes,
      title: title,
    });
  };

  // render a number cell
  var renderNumberCell = function (data, type, row, meta) {
    return renderCellClass(data, type, row, meta, "numberCell");
  };

  // render a text cell
  var renderTextCell = function (data, type, row, meta) {
    return renderCellClass(data, type, row, meta, "textCell");
  };

  // render a data cell
  var renderDataCell = function (data, type, row, meta) {
    return renderCellClass(data, type, row, meta, "dataCell");
  };

  // render a list cell
  var renderListCell = function (data, type, row, meta) {
    return renderCellClass(data, type, row, meta, "listCell");
  };

  // restores scroll information lost on tab switch
  var restoreScrollHandlers = function () {
    var scrollBody = $(".dataTables_scrollBody");
    if (scrollBody) {
      // reattach handlers
      /*
      for (var i = 0; i < detachedHandlers.length; i++) {
        scrollBody.on("scroll", detachedHandlers[i]);
      }
      */

      // restore position
      if (lastScrollPos) scrollBody.scrollTop(lastScrollPos);
    }

    // clean state
    detachedHandlers = [];
    lastScrollPos = 0;
  };

  var syncWidth = function () {
    // shrink container to width of first row; reschedule size if first row
    // hasn't been drawn yet
    var rsGridData = document.getElementById("rsGridData");
    if (!rsGridData || !rsGridData.firstChild || rsGridData.firstChild.clientWidth === 0) {
      return false;
    }
    rsGridData.style.width = rsGridData.firstChild.clientWidth + "px";
    return true;
  };

  // applies a new size to the table--called on init, on tab activate (from
  // RStudio), and when the window size changes
  var sizeDataTable = function (force) {
    // reattach any detached scroll handlers
    restoreScrollHandlers();

    // don't apply a zero height
    if (window.innerHeight < 1) {
      return;
    }

    var thead = document.getElementById("data_cols");
    var theadHeight = thead ? thead.clientHeight : 0;

    // ignore if height hasn't changed
    if (lastHeight === window.innerHeight && lastHeaderHeight === theadHeight && !force) {
      return;
    }

    lastHeight = window.innerHeight;
    lastHeaderHeight = theadHeight;

    // adjust scroll body height accordingly
    var scrollBody = $(".dataTables_scrollBody");
    if (scrollBody && scrollBody.length > 0) {
      // apply the new height
      scrollBody.css("height", window.innerHeight - ($("thead").height() + 25));
    }

    // apply new size
    if (table) {
      table.settings().scroller().measure(false);
      table.draw();
    }
  };

  var debouncedDataTableSize = debounce(sizeDataTable, 75);

  // run a function after window size stops changing
  var runAfterSizing = function (func) {
    var height = window.innerHeight;
    var interval = window.setInterval(function () {
      if (height === window.innerHeight) {
        window.clearInterval(interval);
        func();
      } else {
        height = window.innerHeight;
      }
    }, 25);
  };

  var loadingTimer = 0;
  var preDrawCallback = function () {
    // when the loading indicator is shown by the scroller, apply a style to
    // transition it smoothly into view after a few ms
    window.clearTimeout(loadingTimer);
    loadingTimer = window.setTimeout(function () {
      var indicator = $(".DTS_Loading");
      if (indicator && !indicator.hasClass("showLoading")) {
        indicator.addClass("showLoading");
      }
    }, 100);

    // synchronize table with content
    syncWidth();

    // prior to drawing, update cached search/filter values
    updateCachedSearchFilter();
  };

  var postDrawCallback = function () {
    // cols might not be initialized at this point
    if (!cols) {
      return;
    }

    var indicator = $(".DTS_Loading");
    if (indicator) {
      indicator.removeClass("showLoading");
    }

    // re-apply manual column sizes to the cells in the first row
    var bcols = $(".dataTables_scrollBody #data_cols th");
    var hcols = $(".dataTables_scrollHead #data_cols th");
    var delta = 0;
    for (var i = 0; i < cols.length; i++) {
      if (typeof manualWidths[i] === "undefined") continue;
      var col = bcols.eq(i);
      delta += manualWidths[i] - col.width();
      col.width(manualWidths[i]);
      hcols.eq(i).width(manualWidths[i]);
    }

    // adjust table if some column sizes differed from their natural size
    if (delta !== 0) {
      $("#rsGridData").width($("#rsGridData").width() + delta);
    }

    // Check to see whether the header widths are out of sync after drawing --
    // unfortunately this is a possibility since DataTables doesn't know the
    // width of the table until after the draw is complete. If the widths don't
    // match, we resize the main table body to match its content, then do an
    // in-place redraw of the scrolling-related elements (header, etc.), using an
    // internal API (without which a redraw would also page in data from the
    // server, etc).
    if (isHeaderWidthMismatched()) {
      syncWidth();
      $.fn.dataTableExt.internal._fnScrollDraw($("#rsGridData").DataTable().settings()[0]);
    }
    window.clearTimeout(loadingTimer);
  };

  // returns the effective search value for a column (strips the type prefix)
  var parseSearchString = function (val) {
    var pipe = val.indexOf("|");
    if (pipe > 0) {
      return val.substr(pipe + 1);
    }
    return val;
  };

  var parseSearchVal = function (idx) {
    return parseSearchString(table.columns(idx).search()[0]);
  };

  var createNumericFilterUI = function (idx, col, onDismiss) {
    var ele = document.createElement("div");
    invokeFilterPopup(
      ele,
      function (popup) {
        popup.className += " numericFilterPopup";
        var min = col.col_breaks[0].toString();
        var max = col.col_breaks[col.col_breaks.length - 1].toString();
        var val = parseSearchVal(idx);
        if (val.indexOf("_") > 0) {
          var range = val.split("_");
          min = range[0];
          max = range[1];
        } else if (!isNaN(parseFloat(val))) {
          min = parseFloat(val);
          max = parseFloat(val);
        }

        var filterFromRange = function (start, end) {
          if (Math.abs(start - end) === 0) return "" + start;
          return start + " - " + end;
        };

        // create textbox to show range selected by histogram
        var numVal = document.createElement("input");
        numVal.type = "text";
        numVal.className = "numValueBox";
        numVal.style.textAlign = "center";
        numVal.value = filterFromRange(min, max);

        // update view to show expression
        var updateView = debounce(function (val) {
          var searchText = "";

          // discard invalid characters
          val = val.replace(/[^-0-9 .]/, "");

          // just one number?
          var digit = val.match(/^\s*-?\d+\.?\d*\s*$/);
          if (digit !== null && digit.length > 0) {
            searchText = digit[0];
          } else {
            var matches = val.match(/^\s*(-?\d+\.?\d*)\s*-\s*(-?\d+\.?\d*)\s*/);
            if (matches !== null && matches.length > 2) {
              // we found a properly formatted query; check to make sure it actually reduces the data
              // set before applying
              if (
                Math.abs(parseFloat(matches[1]) - min) !== 0 ||
                Math.abs(parseFloat(matches[2]) - max) !== 0
              ) {
                searchText = matches[1] + "_" + matches[2];
              }
            }
          }

          if (searchText.length > 0) {
            // we found a query! apply it.
            searchText = "numeric|" + searchText;
          }
          table.columns(idx).search(searchText).draw();
        }, 200);
        numVal.addEventListener("change", function () {
          updateView(numVal.value);
        });
        numVal.addEventListener("click", function (evt) {
          // prevent clicks into the value box from invoking light dismiss
          evt.stopPropagation();
        });
        numVal.addEventListener("keydown", function (evt) {
          // dismiss when user finishes typing in the value box
          if (!dismissActivePopup) return;
          else if (evt.keyCode === 27) dismissActivePopup(false);
          else if (evt.keyCode === 13) dismissActivePopup(true);
        });

        var updateText = function (start, end) {
          numVal.value = filterFromRange(start, end);
          updateView(numVal.value);
        };

        var histBrush = document.createElement("div");
        histBrush.className = "numHist";

        // default to selecting everything
        var binStart = 0;
        var binEnd = col.col_breaks.length - 2;

        // find the bins that best fit the current min/max values
        for (var i = 0; i < col.col_breaks.length; i++) {
          if (Math.abs(col.col_breaks[i] - min) < Math.abs(col.col_breaks[binStart] - min)) {
            binStart = i;
          }
          if (i === 0) continue;
          if (Math.abs(col.col_breaks[i] - max) < Math.abs(col.col_breaks[binEnd] - max)) {
            binEnd = i - 1;
          }
        }

        // select just one bin in the single bin case
        if (binEnd < binStart) {
          binStart = binEnd;
        }

        // create histogram
        hist(
          histBrush, // element to host histogram
          col.col_breaks, // array of endpoints for bins
          col.col_counts, // count of data points in each bin
          binStart, // index of first selected bin
          binEnd, // index of last selected bin
          function (start, end) {
            updateText(start, end);
          }
        );
        popup.appendChild(histBrush);

        popup.appendChild(numVal);
      },
      onDismiss,
      false
    );
    ele.textContent = "[...]";
    return ele;
  };

  // shared among factor and text filter UI
  var createTextFilterBox = function (ele, idx, col, onDismiss) {
    var input = document.createElement("input");
    input.type = "text";
    input.className = "textFilterBox";

    // apply the search filter value if this column is filtered as character
    var searchvals = table.columns(idx).search()[0].split("|");
    if (searchvals.length > 1 && searchvals[0] === "character") input.value = searchvals[1];

    var updateView = debounce(function () {
      table
        .columns(idx)
        .search("character|" + encodeURIComponent(input.value))
        .draw();
    }, 200);
    input.addEventListener("keyup", function (evt) {
      updateView();
    });
    input.addEventListener("keydown", function (evt) {
      if (evt.keyCode === 27) {
        onDismiss();
      }
    });
    ele.addEventListener("click", function (evt) {
      input.focus();
      evt.preventDefault();
      evt.stopPropagation();
    });
    ele.appendChild(input);
    return input;
  };

  var createFactorFilterUI = function (idx, col, onDismiss) {
    var ele = document.createElement("div");
    var input = createTextFilterBox(ele, idx, col, onDismiss);
    input.addEventListener("keyup", function (evt) {
      // when the user starts typing in the text box, hide the drop list
      if (dismissActivePopup) {
        dismissActivePopup(false);
      }
    });
    input.addEventListener("blur", function (evt) {
      if (!dismissActivePopup) onDismiss();
    });
    input.addEventListener("focus", function (evt) {
      if (dismissActivePopup) dismissActivePopup(false);
    });

    var setValHandler = function (factor, text) {
      return function (evt) {
        var searchText = "factor|" + factor.toString();
        table.columns(idx).search(searchText).draw();
        input.value = text;
      };
    };

    invokeFilterPopup(
      ele,
      function (popup) {
        var list = document.createElement("div");
        list.className = "choiceList";
        var current = 0;
        var searchvals = table.columns(idx).search()[0].split("|");
        if (searchvals.length > 1 && searchvals[0] === "factor")
          current = parseInt(searchvals[1]);
        for (var i = 0; i < col.col_vals.length; i++) {
          var opt = document.createElement("div");
          opt.textContent = col.col_vals[i];
          opt.className = "choiceListItem";
          opt.addEventListener("click", setValHandler(i + 1, col.col_vals[i]));
          list.appendChild(opt);
        }
        popup.appendChild(list);
      },
      onDismiss,
      false
    );

    return ele;
  };

  var createTextFilterUI = function (idx, col, onDismiss) {
    var ele = document.createElement("div");
    var input = createTextFilterBox(ele, idx, col, onDismiss);
    input.addEventListener("blur", function (evt) {
      onDismiss();
    });
    input.addEventListener("focus", function (evt) {
      if (dismissActivePopup) dismissActivePopup(true);
    });
    return ele;
  };

  var createBooleanFilterUI = function (idx, col, onDismiss) {
    var ele = document.createElement("div");
    var display = document.createElement("span");
    display.innerHTML = "&nbsp;";
    ele.appendChild(display);

    var setBoolValHandler = function (text) {
      return function (evt) {
        var searchText = "boolean|" + text;
        table.columns(idx).search(searchText).draw();
        display.textContent = text;
      };
    };

    invokeFilterPopup(
      ele,
      function (popup) {
        var list = document.createElement("div");
        list.className = "choiceList";
        var values = ["TRUE", "FALSE"];
        for (var logical in values) {
          var opt = document.createElement("div");
          opt.textContent = values[logical];
          opt.className = "choiceListItem";
          opt.addEventListener("click", setBoolValHandler(values[logical]));

          list.appendChild(opt);
        }
        popup.appendChild(list);
      },
      onDismiss,
      false
    );

    return ele;
  };

  var invokeFilterPopup = function (ele, buildPopup, onDismiss, dismissOnClick) {
    var popup = null;

    var dismissPopup = function (actionComplete) {
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

    var checkLightDismiss = function (evt) {
      if (popup && (!dismissOnClick || !popup.contains(evt.target))) {
        dismissPopup(true);
      }
    };

    var checkEscDismiss = function (evt) {
      if (popup && evt.keyCode === 27) {
        dismissPopup(true);
      }
    };

    ele.addEventListener("click", function (evt) {
      // dismiss any other popup
      if (dismissActivePopup && dismissActivePopup != dismissPopup) {
        dismissActivePopup(true);
      }
      if (popup) {
        dismissPopup(true);
      } else {
        popup = createFilterPopup();
        var popupInfo = buildPopup(popup);
        document.body.appendChild(popup);

        // compute position
        var top = $(ele).offset().top + (!popupInfo ? 20 : popupInfo.top);
        var left = $(ele).offset().left + (!popupInfo ? -4 : popupInfo.left);
        if (popupInfo && popupInfo.width) {
          $(popup).width(popupInfo.width(ele));
        }

        // ensure we're not outside the body
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

  var createFilterUI = function (idx, col) {
    // the index coming into this function is for absolute data purposes
    // since this is a visual-centric function we operate based on the visible index
    var visualIndex = idx - columnOffset;

    // don't filter rownames column
    if (visualIndex < 1) {
      return;
    }

    var host = document.createElement("div");
    var val = null,
      ui = null;
    host.className = "colFilter unfiltered";

    var setUnfiltered = function () {
      if (ui !== null) {
        if (ui.parentNode === host) host.replaceChild(val, ui);
        ui = null;
      }
      host.className = "colFilter unfiltered";
      clear.style.display = "none";
    };

    var onDismiss = function () {
      if (table.columns(visualIndex).search()[0].length === 0) {
        setUnfiltered();
      }
    };

    var clear = document.createElement("img");
    clear.src = "datatables/images/clear_filter.png";
    clear.className = "clearFilter";
    clear.style.display = "none";
    clear.addEventListener("click", function (evt) {
      if (dismissActivePopup) dismissActivePopup(true);
      table.columns(visualIndex).search("").draw();
      setUnfiltered();
      evt.preventDefault();
      evt.stopPropagation();
    });
    host.appendChild(clear);

    val = document.createElement("div");
    val.textContent = "All";
    val.addEventListener("click", function (evt) {
      if (col.col_search_type === "numeric") {
        ui = createNumericFilterUI(visualIndex, col, onDismiss);
      } else if (col.col_search_type === "factor") {
        ui = createFactorFilterUI(visualIndex, col, onDismiss);
      } else if (col.col_search_type === "character") {
        ui = createTextFilterUI(visualIndex, col, onDismiss);
      } else if (col.col_search_type === "boolean") {
        ui = createBooleanFilterUI(visualIndex, col, onDismiss);
      }
      if (ui) {
        ui.className += " filterValue";
        host.replaceChild(ui, val);
        host.className = "colFilter filtered";
        clear.style.display = "block";
        var click = document.createEvent("MouseEvents");
        click.initEvent("click", true, false);
        ui.dispatchEvent(click);
        evt.preventDefault();
        evt.stopPropagation();
      }
    });

    host.appendChild(val);
    return host;
  };

  var createColumnTypesUI = function (th, idx, col) {
    var host = document.createElement("div");
    host.className = "columnTypeWrapper";

    var checkLightDismiss = function (evt) {
      if (columnsPopup && onColumnDismiss && !columnsPopup.contains(evt.target))
        onColumnDismiss();
    };

    var checkEscDismiss = function (evt) {
      if (evt.keyCode === 27) {
        if (onColumnDismiss) onColumnDismiss();
      }
    };

    var val = document.createElement("div");
    val.textContent =
      "(" + (col.col_type_assigned ? col.col_type_assigned : col.col_type_r) + ")";
    val.className = "columnTypeHeader";

    th.className = th.className + " columnClickable";
    th.addEventListener("click", function (evt) {
      if (columnsPopup == null || columnsPopup != th) {
        columnsPopup = th;
        activeColumnInfo = {
          left: $(host).offset().left - 5,
          top: $(host).parent().height() + 11,
          width: $(th).outerWidth() - 1,
          index: idx,
          name: col.col_name,
        };
        if (onColumnOpen) onColumnOpen();

        evt.preventDefault();
        evt.stopPropagation();
      } else {
        columnsPopup = null;
        if (onColumnDismiss) onColumnDismiss();
      }
    });

    document.body.addEventListener("click", checkLightDismiss);
    document.body.addEventListener("keydown", checkEscDismiss);

    host.appendChild(val);
    return host;
  };

  var createFilterPopup = function () {
    var filterUI = document.createElement("div");
    filterUI.className = "filterPopup";
    return filterUI;
  };

  var createHeader = function (idx, col) {
    var th = document.createElement("th");

    // wrapper for cell contents
    var interior = document.createElement("div");
    interior.className = "headerCell";

    // add the title
    var title = document.createElement("div");
    title.textContent = col.col_name;
    interior.appendChild(title);
    if (col.col_type === "rownames") {
      th.title = "row names";
    } else {
      th.title = "column " + (idx - columnOffset) + ": " + col.col_type;
    }
    if (col.col_type === "numeric") {
      th.title +=
        " with range " + col.col_breaks[0] + " - " + col.col_breaks[col.col_breaks.length - 1];
    } else if (col.col_type === "factor") {
      th.title += " with " + col.col_vals.length + " levels";
    }

    if (idx === 0) {
      th.className = "first-child sorting";
    }

    // add the column label, if it has one
    if (col.col_label && col.col_label.length > 0) {
      var label = document.createElement("div");
      label.className = "colLabel";
      label.textContent = col.col_label;
      label.title = col.col_label;
      interior.appendChild(label);
    }

    // add a grabber for resizing
    var resizer = document.createElement("div");
    resizer.className = "resizer";
    resizer.setAttribute("data-col", idx);
    interior.appendChild(resizer);

    th.appendChild(interior);

    return th;
  };

  var parseLocationUrl = function () {
    var parsedLocation = {};

    parsedLocation.env = parsedLocation.obj = parsedLocation.cacheKey = parsedLocation.id = "";
    parsedLocation.maxCols = defaultMaxColumns;

    var query = window.location.search.substring(1);
    var queryVars = query.split("&");
    for (var i = 0; i < queryVars.length; i++) {
      var queryVar = queryVars[i].split("=");
      if (queryVar[0] == "env") {
        parsedLocation.env = queryVar[1];
      } else if (queryVar[0] == "obj") {
        parsedLocation.obj = queryVar[1];
      } else if (queryVar[0] == "cache_key") {
        parsedLocation.cacheKey = queryVar[1];
      } else if (queryVar[0] == "data_source") {
        parsedLocation.dataSource = queryVar[1];
      } else if (queryVar[0] == "id") {
        parsedLocation.id = queryVar[1];
      } else if (queryVar[0] == "max_cols") {
        parsedLocation.maxCols = parseInt(queryVar[1], 10);
      }
    }

    return parsedLocation;
  };

  var initDataTableLoad = function (result) {
    table = $("#rsGridData").DataTable();

    // datatables has a bug wherein it sometimes thinks an LTR browser is RTL if
    // the LTR browser is at >100% zoom; this causes layout problems, so force
    // into LTR mode as we don't support RTL here.
    $.fn.dataTableSettings[0].oBrowser.bScrollbarLeft = false;

    // listen for size changes
    debouncedDataTableSize();
    window.addEventListener("resize", function () {
      debouncedDataTableSize();
    });

    // trigger post-init actions
    for (var actionName in postInitActions) {
      if (postInitActions[actionName]) {
        postInitActions[actionName]();
      }
    }

    bootstrapping = false;
  };

  var initDataTable = function (resCols, data) {
    if (resCols.error) {
      showError(cols.error);
      return;
    }

    // an issue was discovered late in the RStudio v1.2 release cycle whereby
    // attempts to render data tables containing large numbers could fail, due to
    // an issue wherein our JSON serializer would incorrectly serialize large
    // numbers. to avoid churning the JSON serializer so close to release, we
    // instead transmit these columns as strings and then convert back to numeric
    // here.
    for (var i = 0; i < resCols.length; i++) {
      var entry = resCols[i];
      if (entry.hasOwnProperty("col_breaks")) {
        var col_breaks = entry["col_breaks"];
        for (var j = 0; j < col_breaks.length; j++) {
          if (typeof col_breaks[j] === "string") col_breaks[j] = parseFloat(col_breaks[j]);
        }
      }
    }

    // save reference to column data
    cols = resCols;

    // due to the jquery magic done in dataTables with rewriting variables and
    // the amount of window parameters we're already using this is a sane fit
    // for setting constants from dtviewer to dataTables
    window.dataTableMaxColumns = Math.max(0, cols.length - 1);
    window.dataTableColumnOffset = columnOffset;

    // look up the query parameters
    var parsedLocation = parseLocationUrl();
    var env = parsedLocation.env,
      obj = parsedLocation.obj,
      cacheKey = parsedLocation.cacheKey;
    maxColumns = defaultMaxColumns = parsedLocation.maxCols;

    // keep track of column types for later render
    var typeIndices = {
      numeric: [],
      "data.frame": [],
      list: [],
      text: [],
    };

    // add each column, offset this and only add as many as current maxColumns
    var thead = document.getElementById("data_cols");
    for (j = 0; j < cols.length && j <= maxColumns + columnOffset; j++) {
      // create table header
      thead.appendChild(createHeader(j <= 0 ? j : j + columnOffset, cols[j]));
      var colType = cols[j].col_type;
      if (colType === "numeric" || colType === "data.frame" || colType === "list") {
        typeIndices[colType].push(j);
      } else {
        typeIndices["text"].push(j);
      }

      // start at 0 for the dummy column but then one time increment by initialIndex
      if (j <= 0) {
        j = columnOffset;
      }
    }
    addResizeHandlers(thead);
    addGlobalResizeHandlers();

    var scrollHeight = window.innerHeight - (thead.clientHeight + 2);

    var dataTableAjax = null;
    var dataTableData = null;
    var dataTableColumnDefs = null;
    var dataTableColumns = null;

    if (!data) {
      dataTableAjax = {
        url: "../grid_data",
        type: "POST",
        data: function (d) {
          d.env = env;
          d.obj = obj;
          d.cache_key = cacheKey;
          d.show = "data";
          d.column_offset = columnOffset;
          d.max_columns = maxColumns;
        },
        error: function (jqXHR) {
          if (jqXHR.responseText[0] !== "{") showError(jqXHR.responseText);
          else {
            var result = $.parseJSON(jqXHR.responseText);
            if (result.error) {
              showError(result.error);
            } else {
              showError("The data could not be displayed.");
            }
          }
        },
      };
      dataTableColumnDefs = [
        {
          targets: typeIndices["numeric"],
          render: renderNumberCell,
        },
        {
          targets: typeIndices["text"],
          render: renderTextCell,
        },
        {
          targets: typeIndices["list"],
          render: renderListCell,
        },
        {
          targets: typeIndices["data.frame"],
          render: renderDataCell,
        },
        {
          targets: "_all",
          width: "4em",
        },
        {
          targets: 0,
          sClass: "first-child sorting",
          width: "4em",
          orderable: true,
        },
      ];
    } else {
      // Create an empty array of data to be use as a map in the callback
      dataTableData = [];
      if (data.length > 0) {
        for (i = 0; i < data[0].length; i++) {
          dataTableData.push(i);
        }
      }

      dataTableColumns =
        resCols.length > 0
          ? cols.map(function (e, idx) {
              var className = rowNumbers && idx === 0 ? "first-child sorting" : null;
              if (e.col_disabled) {
                className = "disabledColumn";
              }

              return {
                sClass: className,
                visible: !rowNumbers && idx === 0 ? false : true,
                data: function (row, type, set, meta) {
                  return meta.col === 0
                    ? meta.row
                    : data
                    ? data[meta.col - 1][meta.row]
                    : null;
                },
                width: "4em",
                render: e.col_type === "numeric" ? renderNumberCell : renderTextCell,
              };
            })
          : [{}];
    }

    // activate the data table
    $("#rsGridData").dataTable({
      processing: true,
      serverSide: dataTableData ? false : true,
      autoWidth: false,
      pagingType: "full_numbers",
      pageLength: 25,
      scrollY: scrollHeight + "px",
      scrollX: true,
      scroller: {
        rowHeight: 23, // sync w/ CSS (scroller auto row height is busted)
        loadingIndicator: true, // show loading indicator when loading
      },
      preDrawCallback: preDrawCallback,
      drawCallback: postDrawCallback,
      dom: "tiS",
      deferRender: true,
      columnDefs: dataTableColumnDefs,
      ajax: dataTableAjax,
      data: dataTableData,
      columns: dataTableColumns,
      fnInfoCallback: !statusTextOverride
        ? null
        : function (oSettings, iStart, iEnd, iMax, iTotal, sPre) {
            return statusTextOverride;
          },
      ordering: ordering,
    });

    initDataTableLoad();

    // update the GWT column widget
    window.columnFrameCallback(columnOffset, maxColumns);
  };

  var debouncedSearch = debounce(function (text) {
    if (text != table.search()) {
      table.search(text).draw();
    }
  }, 100);

  var loadDataFromUrl = function (callback) {
    // call the server to get data shape
    $.ajax({
      url: "../grid_data",
      data: "show=cols&" + window.location.search.substring(1),
      type: "POST",
    })
      .done(function (result) {
        callback(result);
      })
      .fail(function (jqXHR) {
        if (jqXHR.responseText[0] !== "{") showError(jqXHR.responseText);
        else {
          var result = $.parseJSON(jqXHR.responseText);

          if (result.error) {
            showError(result.error);
          } else {
            showError("The object could not be displayed.");
          }
        }
      });
  };

  var addGlobalResizeHandlers = function () {
    // if we've already done the setup, don't add the handlers again
    if (!!document.resizeHandlersInit) return;

    document.resizeHandlersInit = true;

    var applyDelta = function (delta) {
      if (!resizingColIdx) return;

      var colWidth = initResizingWidth + delta;

      // don't allow resizing beneath minimum size. prefer
      // the original column width, but for large columns allow
      // resizing to minimum of 100 pixels
      var minColWidth = origColWidths[resizingColIdx] || 50;
      if (minColWidth > 100) minColWidth = 100;

      if (delta < 0 && colWidth < minColWidth) {
        resizingBoundsExceeded += delta;
        return;
      }

      // if positive delta, consume exceeded bounds before returning to resize
      // mode
      if (delta > 0 && resizingBoundsExceeded < 0) {
        resizingBoundsExceeded += delta;
        if (resizingBoundsExceeded < 0) {
          // didn't consume all bounds
          return;
        } else {
          // consumed all bounds; resize remaining portion of motion
          delta = resizingBoundsExceeded;
          colWidth = initResizingWidth + delta;
        }
      }

      // resize the column in the given direction
      $(".dataTables_scrollHeadInner table").width(origTableWidth + delta);

      var grid = $("#rsGridData");
      if (grid.hasClass("autoSize")) {
        // if the data table is still in auto size mode, we need to switch it
        // to fixed layout

        // observe and manually apply column widths in preparation for
        // transition to a fixed layout
        var head = $(".dataTables_scrollHead #data_cols th");
        var body = $(".dataTables_scrollBody #data_cols th");
        for (var i = 0; i < Math.min(head.length, body.length); i++) {
          var thHead = head.eq(i),
            thBody = body.eq(i);
          thHead.width(thHead.width());
          thBody.width(thBody.width());
          manualWidths[i] = thBody.width();
        }

        // switch table out of auto size mode and into manual size mode
        grid.addClass("manualSize");
        grid.removeClass("autoSize");
      }

      // adjust header width and width of first column
      $("#data_cols th:nth-child(" + resizingColIdx + ")").width(colWidth);
      grid.width(origTableWidth + delta);

      // record manual width for re-apply on redraw
      manualWidths[resizingColIdx - (rowNumbers ? 1 : 0)] = colWidth;
    };

    var endResize = function () {
      // end the resize operation
      $("#rsGridData td:nth-child(" + resizingColIdx + ")").css("border-right-color", "");
      resizingColIdx = null;
    };
    $("body").on("mousemove", function (evt) {
      if (resizingColIdx !== null) {
        // if we have an active resize column, resize it by the amount given
        var original = evt.originalEvent;
        applyDelta(original.clientX - initResizeX);
        evt.preventDefault();
      }
    });
    $("body").on("click", function (evt) {
      if (resizingColIdx !== null) {
        // ignore clicks while resizing
        evt.stopPropagation();
      }
    });
    $("body").on("mouseup", function (evt) {
      if (resizingColIdx !== null) {
        // end resizing if active
        endResize();
        evt.stopPropagation();
      }
    });
    $("body").on("mouseleave", function (evt) {
      if (resizingColIdx !== null) {
        // the mouse left; treat this as a cancel (since leaving means we
        // won't get a corresponding mouseup)
        applyDelta(0);
        endResize();
      }
    });
  };

  var addResizeHandlers = function (ele) {
    $(ele).on("mousedown", function (evt) {
      var original = evt.originalEvent;
      if (original.target.className === "resizer") {
        // when the mouse is clicked on the resizer, enter resize mode; figure
        // out which column we're targeting and set up the initial sizes
        resizingColIdx = parseInt(original.target.getAttribute("data-col"));

        // account for row names column
        if (rowNumbers) resizingColIdx++;

        // disable propagation of clicks from the resizer to the outer cell
        $(original.target).on("click", function (evt) {
          return false;
        });

        initResizeX = original.clientX;

        initResizingWidth = $("#data_cols th:nth-child(" + resizingColIdx + ")").width();
        origTableWidth = $("#rsGridData").width();
        resizingBoundsExceeded = 0;

        if (typeof origColWidths[resizingColIdx] === "undefined")
          origColWidths[resizingColIdx] = initResizingWidth;

        $("#rsGridData td:nth-child(" + resizingColIdx + ")").css(
          "border-right-color",
          "#A0A0FF"
        );
        evt.preventDefault();
      }
    });
  };

  // bootstrapping:
  // 1. clean up state (we re-bootstrap whenever table structure changes)
  // 2. make the request to get the shape of the data object to be viewed
  //   (we want this to start as soon as possible so the shape can be prepared
  //   on the server while we wait for the geometry to finish initializing on
  //   the client)
  // 3. wait for the document to be ready
  // 4. wait for the window size to stop changing (RStudio animates tab opening)
  // 5. initialize the data table
  var bootstrap = function (data) {
    boostrapping = true;

    // dismiss any active popups
    if (dismissActivePopup) dismissActivePopup(true);

    // clean state
    table = null;
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
    lastHeight = 0;
    lastHeaderHeight = 0;
    lastScrollPos = 0;
    detachedHandlers = [];

    // when datatables is initialized on an element, it adds a bunch of goo
    // around the element to handle scrolling, etc.--we need to pull the whole
    // thing from the DOM so we get a clean re-init
    oldEle = document.getElementById("rsGridData_wrapper");
    if (oldEle) {
      oldEle.parentNode.removeChild(oldEle);
      oldEle = null;
    }

    // make a new one, but don't hook it up yet (the document may not exist at
    // this point)
    var newEle = document.createElement("table");
    newEle.id = "rsGridData";
    newEle.className = "dataTable autoSize";
    newEle.setAttribute("cellspacing", "0");
    newEle.innerHTML = "<thead>" + "   <tr id='data_cols'>" + "   </tr>" + "</thead>";
    addResizeHandlers(newEle);

    if (!data) {
      loadDataFromUrl(function (result) {
        $(document).ready(function () {
          document.body.appendChild(newEle);
          runAfterSizing(function () {
            if (result) {
              initDataTable($.parseJSON(result));
            }
          });
        });
      });
    } else {
      $(document).ready(function () {
        document.body.appendChild(newEle);
        runAfterSizing(function () {
          // Assign line numbers:
          if (data.data) {
            data.data = data.data.map(function (e, idx) {
              var eWithNumber = e;
              eWithNumber[""] = idx + 1;
              return eWithNumber;
            });
          } else {
            data.data = [[]];
          }

          if (data.columns) {
            initDataTable(data.columns, data.data);
          }
        });
      });
    }
  };

  var setHeaderUIVisible = function (visible, initialize, hide) {
    var thead = document.getElementById("data_cols");

    // it's possible the dable is getting redrawn right now; if it is, defer
    // this request.
    if (thead === null || table === null || cols === null) {
      postInitActions["setHeaderUIVisible"] = visible
        ? function () {
            setHeaderUIVisible(true, initialize);
          }
        : null;
      return false;
    }

    if (!visible) {
      hide(thead);
      // close any popup
      if (dismissActivePopup) dismissActivePopup(true);
    }
    for (var i = 0; i < thead.children.length; i++) {
      var colIdx = i + (rowNumbers ? 0 : 1) + columnOffset;
      var col = cols[colIdx];
      var th = thead.children[i];
      if (visible) {
        var headerElement = initialize(th, col, colIdx);
        if (headerElement) {
          th.appendChild(headerElement);
        }
      } else if (th.children.length > 1) {
        th.removeChild(th.lastChild);
      }
    }
    sizeDataTable(true);
    return true;
  };

  // Exports -------------------------------------------------------------------

  // called from RStudio to toggle the filter UI
  window.setFilterUIVisible = function (visible) {
    var setFilterUIVisiblePerColumn = function (th, col, i) {
      if (
        col.col_search_type === "numeric" ||
        col.col_search_type === "character" ||
        col.col_search_type === "factor" ||
        col.col_search_type === "boolean"
      ) {
        return createFilterUI(i, col);
      }

      return null;
    };

    var hideFilterUI = function () {
      // clear all the filter data
      table.columns().search("");
    };

    return setHeaderUIVisible(visible, setFilterUIVisiblePerColumn, hideFilterUI);
  };

  // called from RStudio to toggle the filter UI
  window.setColumnDefinitionsUIVisible = function (visible, onColOpen, onColDismiss) {
    var setColumnDefinitionsUIVisiblePerColumn = function (th, col, i) {
      return createColumnTypesUI(th, i, col);
    };

    var hideColumnTypeUI = function (thead) {
      $(".columnTypeWrapper").remove();
    };

    onColumnOpen = onColOpen ? onColOpen : onColumnOpen;
    onColumnDismiss = onColDismiss ? onColDismiss : onColumnDismiss;

    return setHeaderUIVisible(visible, setColumnDefinitionsUIVisiblePerColumn, hideColumnTypeUI);
  };

  // called from RStudio when the underlying object changes
  window.refreshData = function () {
    // restore any scroll handlers (this can get called on tab activate)
    restoreScrollHandlers();
    bootstrap();
  };

  // called from RStudio to apply a column-wide search.
  window.applySearch = function (text) {
    debouncedSearch(text);
  };

  window.onActivate = function () {
    // resize the table once animation finishes
    debouncedDataTableSize(false);
  };

  window.onDeactivate = function () {
    // In Firefox, the browser scrolls the viewport to the top when the tab is
    // reactivated before any of our own event handlers fire. This triggers the
    // scroller to redraw the table from the server starting from the first row,
    // as though the user had scrolled the viewport to the top.
    //
    // It isn't possible to suppress this event, so when the tab is deactivated,
    // we unwire all the event handlers from the scrolling region, and reattach
    // them on activate.

    // save current scroll position
    var scrollBody = $(".dataTables_scrollBody");
    if (scrollBody === null || scrollBody.length === 0) {
      return;
    }
    lastScrollPos = scrollBody.scrollTop();

    // save all the of the scroll event handlers
    /*
    detachedHandlers = [];
    var scrollEvents = $._data(scrollBody[0], "events");
    jQuery.each(scrollEvents.scroll, function (k, v) {
      detachedHandlers.push(v.handler);
    });

    // detach all scroll event handlers
    scrollBody.off("scroll");
    */
  };

  window.setData = function (data) {
    bootstrap(data);
  };

  window.setOption = function (option, value) {
    switch (option) {
      case "nullsAsNAs":
        displayNullsAsNAs = value === "true" ? true : false;
        break;
      case "status":
        statusTextOverride = value;
        break;
      case "ordering":
        ordering = value === "true" ? true : false;
        break;
      case "rowNumbers":
        rowNumbers = value === "true" ? true : false;
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

  // default viewer for data cells
  window.dataViewerCallback = function (row, col) {
    alert("No viewer for data at " + col + ", " + row + ".");
  };

  // default viewer for list cells
  window.listViewerCallback = function (row, col) {
    alert("No viewer for list at " + col + ", " + row + ".");
  };

  // callback for updating the GWT column widget
  window.columnFrameCallback = function () {};

  window.getActiveColumn = function () {
    return activeColumnInfo;
  };

  window.nextColumnPage = function () {
    if (bootstrapping) {
      return;
    }

    var newOffset = Math.max(
      0,
      Math.min(cols.length - 1 - maxColumns, columnOffset + maxColumns)
    );
    if (columnOffset != newOffset) {
      columnOffset = newOffset;
      bootstrap();
    }
  };

  window.prevColumnPage = function () {
    if (bootstrapping) {
      return;
    }

    var newOffset = Math.max(
      0,
      Math.min(cols.length - 1 - maxColumns, columnOffset - maxColumns)
    );
    if (columnOffset != newOffset) {
      columnOffset = newOffset;
      bootstrap();
    }
  };

  window.firstColumnPage = function () {
    if (bootstrapping) {
      return;
    }

    if (columnOffset != 0) {
      columnOffset = 0;
      bootstrap();
    }
  };

  window.lastColumnPage = function () {
    if (bootstrapping) {
      return;
    }

    if (columnOffset != cols.length - 1 - maxColumns) {
      columnOffset = cols.length - 1 - maxColumns;
      bootstrap();
    }
  };

  window.setOffsetAndMaxColumns = function (newOffset, newMax) {
    if (bootstrapping) {
      return;
    }
    if (newOffset >= cols.length) {
      return;
    }

    if (newOffset > 0) {
      columnOffset = newOffset;
    }
    if (newMax > 0) {
      newMax = Math.min(cols.length - 1 - newOffset, newMax);
      maxColumns = newMax;
    }
    bootstrap();
  };

  // return whether to show the column frame UI elements
  window.isLimitedColumnFrame = function () {
    return cols.length > defaultMaxColumns;
  };

  var parsedLocation = parseLocationUrl();
  var dataMode =
    parsedLocation && parsedLocation.dataSource ? parsedLocation.dataSource : "server";

  // start the first request
  if (dataMode === "server") {
    bootstrap();
  }
})();
