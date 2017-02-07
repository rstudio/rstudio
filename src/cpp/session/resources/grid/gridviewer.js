/*jshint browser:true, strict:false, curly:false, indent:3*/

/*
 * gridviewer.js
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

(function(){

// the data table itself
var table;   

// the column definitions from the server
var cols;

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
var activeColumnInfo = {
};

// manually adjusted widths of each column
var manualWidths = [];

var isHeaderWidthMismatched = function() {
  // find the elements to measure (they may not exist)
  var rs = document.getElementById("rsGridData");
  if (!rs || !rs.firstChild.clientWidth || !rs.firstChild.clientWidth > 0)
    return false;
  var sh = document.getElementsByClassName("dataTables_scrollHeadInner");
  if (sh.length === 0 || !sh[0].firstChild  || !sh[0].firstChild.firstChild)
    return false;

  // match the widths
  return rs.firstChild.clientWidth !== sh[0].firstChild.firstChild.clientWidth;
};

// update search/filter value cache
var updateCachedSearchFilter = function() {
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
var debounce = function(func, wait) {
	var timeout;
	return function() {
		var context = this, args = arguments;
		var later = function() {
			timeout = null;
			func.apply(context, args);
		};
		clearTimeout(timeout);
		timeout = setTimeout(later, wait);
	};
};

// used to determine the step precision for a number--e.g.:
// "10.4" has a step precision of 0.1
// "12.44" has a step precision of 0.01
// "2" has a step precision of 1
var stepPrecision = function(str) {
  var idx = str.indexOf(".");
  if (idx < 0) {
    return 1;
  }
  return Math.pow(10, (idx - str.length) + 1);
};

// show an error--not recoverable; user must click 'retry' to reload 
var showError = function(msg) {
  document.getElementById("errorWrapper").style.display = "block";
  document.getElementById("errorMask").style.display = "block";
  document.getElementById("error").textContent = msg;
  var rsGridData = document.getElementById("rsGridData");
  if (rsGridData)
    rsGridData.style.display = "none";
};

// simple HTML escaping (avoid XSS in data)
var escapeHtml = function(html) {
  if (!html)
    return "";

  // handle special cells
  if (typeof(html) === "number")
    return html.toString();

  // in other types, replace special characters
  var replacements = {
    "<":  "&lt;",
    ">":  "&gt;",
    "&":  "&amp;",
    "\"": "&quot;" };
  return html.replace(/[&<>]/g, function(ch) { return replacements[ch]; });
};

var highlightSearchMatch = function(data, search, pos) {
  return escapeHtml(data.substring(0, pos)) + '<span class="searchMatch">' + 
         escapeHtml(data.substring(pos, pos + search.length)) + '</span>' + 
         escapeHtml(data.substring(pos + search.length, data.length));

};

// render cell contents--if no search is active, just renders the data
// literally; when search is active, highlights the portion of the text that
// matches the search
var renderCellContents = function(data, type, row, meta) {

  // usually data is a string; 0 is a special value signifying NA 
  if (data === 0 || (displayNullsAsNAs && data === null)) {
    return '<span class="naCell">NA</span>';
  }

  // if row matches because of a global search, highlight that
  if (cachedSearch.length > 0) {
    var idx = data.toLowerCase().indexOf(cachedSearch.toLowerCase());
    if (idx >= 0) {
      return highlightSearchMatch(data, cachedSearch, idx);
    }
  }

  // if row matches because of a column search (characterwise), highlight that
  if (meta.col < cachedFilterValues.length) {
    var colSearch = cachedFilterValues[meta.col];
    if (colSearch.indexOf("character|") === 0) {
      colSearch = parseSearchString(colSearch);
      var colIdx = data.toLowerCase().indexOf(colSearch.toLowerCase());
      if (colIdx >= 0) {
        return highlightSearchMatch(data, colSearch, colIdx);
      }
    }
  }
  return escapeHtml(data);
};

var renderCellClass = function (data, type, row, meta, clazz) {
  var title = null;
  if (typeof(data) === "string") 
     title = data.replace(/\"/g, "&quot;");
  return '<div class="cell">' + 
         '<div class="' + clazz + '" ' + 
         (title !== null ? 'title="' + title + '"' : '') +
         '>' + 
         renderCellContents(data, type, row, meta) + '</div>' +
         '<div class="resizer" data-col="' + meta.col + '" /></div>';
};

// render a number cell
var renderNumberCell = function(data, type, row, meta) {
  return renderCellClass(data, type, row, meta, "numberCell");
};

// render a text cell
var renderTextCell = function(data, type, row, meta) {
  return renderCellClass(data, type, row, meta, "textCell");
};

// restores scroll information lost on tab switch
var restoreScrollHandlers = function() {
  var scrollBody = $(".dataTables_scrollBody");
  if (scrollBody) {
    // reattach handlers
    for (var i = 0; i < detachedHandlers.length; i++) {
      scrollBody.on("scroll", detachedHandlers[i]);
    }

    // restore position
    if (lastScrollPos)
      scrollBody.scrollTop(lastScrollPos);
  }

  // clean state
  detachedHandlers = [];
  lastScrollPos = 0;
};

var syncWidth = function() {
  // shrink container to width of first row; reschedule size if first row
  // hasn't been drawn yet
  var rsGridData = document.getElementById("rsGridData");
  if (!rsGridData || !rsGridData.firstChild ||
      rsGridData.firstChild.clientWidth === 0) {
    return false;
  }
  rsGridData.style.width = rsGridData.firstChild.clientWidth + "px";
  return true;
};

// applies a new size to the table--called on init, on tab activate (from
// RStudio), and when the window size changes
var sizeDataTable = function(force) {
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
    scrollBody.css("height", 
      window.innerHeight - ($("thead").height() + 25));
  }

  // apply new size
  if (table) {
    table.settings().scroller().measure(false);
    table.draw();
  }
};

var debouncedDataTableSize = debounce(sizeDataTable, 75);

// run a function after window size stops changing
var runAfterSizing = function(func) {
  var height = window.innerHeight;
  var interval = window.setInterval(function() {
    if (height === window.innerHeight) {
      window.clearInterval(interval);
      func();
    } else {
      height = window.innerHeight;
    }
  }, 25);
};

var loadingTimer = 0;
var preDrawCallback = function() {
  // when the loading indicator is shown by the scroller, apply a style to
  // transition it smoothly into view after a few ms
  window.clearTimeout(loadingTimer);
  loadingTimer = window.setTimeout(function() {
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

var postDrawCallback = function() {
  var indicator = $(".DTS_Loading");
  if (indicator) {
      indicator.removeClass("showLoading");
  }

  // re-apply manual column sizes to the cells in the first row
  var bcols = $(".dataTables_scrollBody #data_cols th");
  var hcols = $(".dataTables_scrollHead #data_cols th");
  var delta = 0;
  for (var i = 0; i < cols.length; i++) {
     if (typeof(manualWidths[i]) === "undefined")
        continue;
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
var parseSearchString = function(val) {
  var pipe = val.indexOf("|");
  if (pipe > 0) {
    return val.substr(pipe + 1);
  }
  return val;
};

var parseSearchVal = function(idx) {
  return parseSearchString(table.columns(idx).search()[0]);
};

var createNumericFilterUI = function(idx, col, onDismiss) {
  var ele = document.createElement("div");
  invokeFilterPopup(ele, function(popup) {
    popup.className += " numericFilterPopup";
    var min = col.col_min.toString();
    var max = col.col_max.toString();
    var val = parseSearchVal(idx);
    if (val.indexOf("_") > 0) {
      var range = val.split("_");
      min = range[0];
      max = range[1];
    } else if (!isNaN(parseFloat(val))) {
      min = parseFloat(val);
      max = parseFloat(val);
    }
    var minVal = document.createElement("div");
    minVal.textContent = min;
    minVal.className = "numMin selected";
    popup.appendChild(minVal);
    var maxVal = document.createElement("div");
    maxVal.textContent = max;
    maxVal.className = "numMax selected";
    popup.appendChild(maxVal);
    var slider = document.createElement("div");
    slider.className = "numSlider";
    popup.appendChild(slider);
    var updateView = debounce(function() {
      var searchText = 
        minVal.textContent === min && maxVal.textContent === max ? 
          "" :
          minVal.textContent + "_" + maxVal.textContent;
      if (searchText.length > 0) {
        searchText = "numeric|" + searchText;
      }
      table.columns(idx).search(searchText).draw();
    }, 200);
    $(slider).slider({
      range:  true,
      min:    col.col_min,
      max:    col.col_max,
      step:   Math.min(stepPrecision(col.col_min.toString()), 
                       stepPrecision(col.col_max.toString())),
      values: [min, max],
      slide:  function(event, ui) {
        minVal.textContent = ui.values[0];
        maxVal.textContent = ui.values[1];
        updateView();
      }
    });
  }, onDismiss, false);
  ele.textContent = "[...]";
  return ele;
};

// shared among factor and text filter UI
var createTextFilterBox = function(ele, idx, col, onDismiss) {
  var input = document.createElement("input");
  input.type = "text";
  input.className = "textFilterBox";

  // apply the search filter value if this column is filtered as character
  var searchvals = table.columns(idx).search()[0].split("|");
  if (searchvals.length > 1 && searchvals[0] === "character") 
    input.value = searchvals[1];

  var updateView = debounce(function() {
      table.columns(idx).search("character|" + input.value).draw();
    }, 200);
  input.addEventListener("keyup", function(evt) {
    updateView();
  });
  input.addEventListener("keydown", function(evt) {
    if (evt.keyCode === 27) {
      onDismiss();
    }
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
  input.addEventListener("keyup", function(evt) {
    // when the user starts typing in the text box, hide the drop list
    if (dismissActivePopup) {
      dismissActivePopup(false);
    }
  });
  input.addEventListener("blur", function(evt) {
    if (!dismissActivePopup) 
      onDismiss();
  });
  input.addEventListener("focus", function(evt) {
    if (dismissActivePopup)
      dismissActivePopup(false);
  });

  var setValHandler = function(factor, text) {
      return function(evt) {
        var searchText = "factor|" + factor.toString();
        table.columns(idx).search(searchText).draw();
        input.value = text;
      };
  };

  invokeFilterPopup(ele, function(popup) {
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
  }, onDismiss, false);

  return ele;
};

var createTextFilterUI = function(idx, col, onDismiss) {
  var ele = document.createElement("div");
  var input = createTextFilterBox(ele, idx, col, onDismiss);
  input.addEventListener("blur", function(evt) {
    onDismiss();
  });
  input.addEventListener("focus", function(evt) {
    if (dismissActivePopup)
      dismissActivePopup(true);
  });
  return ele;
};

var createBooleanFilterUI = function(idx, col, onDismiss) {
  var ele = document.createElement("div");
  var display = document.createElement("span");
  display.innerHTML = "&nbsp;";
  ele.appendChild(display);

  var setBoolValHandler = function(text) {
      return function(evt) {
        var searchText = "boolean|" + text;
        table.columns(idx).search(searchText).draw();
        display.textContent = text;
      };
  };

  invokeFilterPopup(ele, function(popup) {
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
  }, onDismiss, false);

  return ele;
};

var invokeFilterPopup = function (ele, buildPopup, onDismiss, dismissOnClick) {
  var popup = null;

  var dismissPopup = function(actionComplete) {
    if (popup) {
      document.body.removeChild(popup);
      document.body.removeEventListener("click", checkLightDismiss);
      document.body.removeEventListener("keydown", checkEscDismiss);
      dismissActivePopup = null;
      popup = null;
      if (actionComplete)
        onDismiss();
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
    if (popup && evt.keyCode === 27) {
      dismissPopup(true);
    }
  };

  ele.addEventListener("click", function(evt) {
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
        left = (document.body.offsetWidth - popup.offsetWidth);
      }

      popup.style.top =  top + "px";
      popup.style.left = left  + "px";
      document.body.addEventListener("click", checkLightDismiss);
      document.body.addEventListener("keydown", checkEscDismiss);
      dismissActivePopup = dismissPopup;
    }

    evt.preventDefault();
    evt.stopPropagation();
  });
};

var createFilterUI = function(idx, col) {
  var host = document.createElement("div");
  var val = null, ui = null;
  host.className = "colFilter unfiltered";

  var setUnfiltered = function() {
    if (ui !== null) {
      if (ui.parentNode === host)
        host.replaceChild(val, ui);
      ui = null;
    }
    host.className = "colFilter unfiltered";
    clear.style.display = "none";
  };

  var onDismiss = function() {
    if (table.columns(idx).search()[0].length === 0) {
      setUnfiltered();
    }
  };

  var clear = document.createElement("img");
  clear.src = "datatables/images/clear_filter.png";
  clear.className = "clearFilter";
  clear.style.display = "none";
  clear.addEventListener("click", function(evt) {
    if (dismissActivePopup)
      dismissActivePopup(true);
    table.columns(idx).search("").draw();
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

var createColumnTypesUI = function(th, idx, col) {
  var host = document.createElement("div");
  host.className = "columnTypeWrapper";

  var checkLightDismiss = function(evt) {
    if (columnsPopup && onColumnDismiss && !columnsPopup.contains(evt.target))
      onColumnDismiss();
  };

  var checkEscDismiss = function(evt) {
    if (evt.keyCode === 27) {
      if (onColumnDismiss)
        onColumnDismiss();
    }
  };

  var val = document.createElement("div");
  val.textContent = "(" + (col.col_type_assigned ? col.col_type_assigned : col.col_type_r) + ")";
  val.className = "columnTypeHeader";

  th.className = th.className + " columnClickable";
  th.addEventListener("click", function(evt) {
    if (columnsPopup == null || columnsPopup != th) {
      columnsPopup = th;
      activeColumnInfo = {
        left: $(host).offset().left - 5,
        top: $(host).parent().height() + 11,
        width: $(th).outerWidth() - 1,
        index: idx,
        name: col.col_name
      };
      if (onColumnOpen)
        onColumnOpen();

      evt.preventDefault();
      evt.stopPropagation();
    }
    else {
      columnsPopup = null;
      if (onColumnDismiss)
        onColumnDismiss();
    }
  });

  document.body.addEventListener("click", checkLightDismiss);
  document.body.addEventListener("keydown", checkEscDismiss);

  host.appendChild(val);
  return host;
};

var createFilterPopup = function() {
  var filterUI = document.createElement("div");
  filterUI.className = "filterPopup";
  return filterUI;
};

var createHeader = function(idx, col) {
  var th = document.createElement("th");

  // wrapper for cell contents
  var interior = document.createElement("div");
  interior.className = "headerCell";

  // add the title
  var title = document.createElement("div");
  title.textContent = col.col_name;
  interior.appendChild(title);
  th.title = "column " + idx + ": " + col.col_type;
  if (col.col_type === "numeric") {
    th.title += " with range " + col.col_min + " - " + col.col_max;
  } else if (col.col_type === "factor") {
    th.title += " with " + col.col_vals.length + " levels";
  }

  if (idx === 0) {
    th.className = "first-child";
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

var parseLocationUrl = function() {
  var parsedLocation = {};

  parsedLocation.env = parsedLocation.obj = parsedLocation.cacheKey = parsedLocation.id = "";

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
    }
  }

  return parsedLocation;
};

var initDataTableLoad = function(result) {
  table = $("#rsGridData").DataTable();

  // datatables has a bug wherein it sometimes thinks an LTR browser is RTL if
  // the LTR browser is at >100% zoom; this causes layout problems, so force
  // into LTR mode as we don't support RTL here.
  $.fn.dataTableSettings[0].oBrowser.bScrollbarLeft = false;

  // listen for size changes
  debouncedDataTableSize();
  window.addEventListener("resize", function() { 
    debouncedDataTableSize(); 
  });

  // trigger post-init actions
  for (var actionName in postInitActions) {
    if (postInitActions[actionName]) {
      postInitActions[actionName]();
    }
  }
};

var initDataTable = function(resCols, data) {
  if (resCols.error) {
    showError(cols.error);
    return;
  }
  cols = resCols;

  // look up the query parameters
  var parsedLocation = parseLocationUrl();
  var env = parsedLocation.env, obj = parsedLocation.obj, cacheKey = parsedLocation.cacheKey;

  // keep track of which columns are numeric and which are text (we use
  // different renderers for these types)
  var numberCols = [];
  var textCols = [];

  // add each column
  var thead = document.getElementById("data_cols");
  for (var j = 0; j < cols.length; j++) {
    // create table header
    thead.appendChild(createHeader(j, cols[j]));
    if (cols[j].col_type === "numeric") {
      numberCols.push(j);
    } else {
      textCols.push(j);
    }
  }
  addResizeHandlers(thead);
  var scrollHeight = window.innerHeight - (thead.clientHeight + 2);

  var dataTableAjax = null;
  var dataTableData = null;
  var dataTableColumnDefs = null;
  var dataTableColumns = null;

  if (!data) {
    dataTableAjax = {
      "url": "../grid_data", 
      "type": "POST",
      "data": function(d) {
        d.env = env;
        d.obj = obj;
        d.cache_key = cacheKey;
        d.show = "data";
      },
      "error": function(jqXHR) {
        if (jqXHR.responseText[0] !== "{")
          showError(jqXHR.responseText);
        else
        {
          var result = $.parseJSON(jqXHR.responseText);
          if (result.error) {
            showError(result.error);
          } else {
            showError("The data could not be displayed.");
          }
        }
      }
     };
    dataTableColumnDefs = [ {
        "targets": numberCols,
        "render": renderNumberCell
      }, {
        "targets": textCols,
        "render": renderTextCell
      }, {
        "targets": "_all",
        "width": "4em"
      }, {
        "targets": 0,
        "sClass": "first-child",
        "width": "4em",
        "orderable": true
      }];
  }
  else {
    // Create an empty array of data to be use as a map in the callback
    dataTableData = [];
    if (data.length > 0) {
      for (i = 0; i < data[0].length; i++) {
        dataTableData.push(i);
      }
    }

    dataTableColumns = resCols.length > 0 ? cols.map(function (e, idx) {
      var className = (rowNumbers && idx === 0) ? "first-child" : null;
      if (e.col_disabled) {
        className = "disabledColumn";
      }

      return {
        "sClass": className,
        "visible": (!rowNumbers && idx === 0) ? false : true,
        "data": function (row, type, set, meta) {
          return (meta.col === 0 ? meta.row : (data ? data[meta.col-1][meta.row] : null));
        },
        "width": "4em",
        "render": e.col_type === "numeric" ? renderNumberCell : renderTextCell
      };
    }) : [{}];
  }

  // activate the data table
  $("#rsGridData").dataTable({
    "processing": true,
    "serverSide": dataTableData ? false : true,
    "autoWidth": false,
    "pagingType": "full_numbers",
    "pageLength": 25,
    "scrollY": scrollHeight + "px",
    "scrollX": true,
    "scroller": {
      "rowHeight": 23,            // sync w/ CSS (scroller auto row height is busted)
      "loadingIndicator": true,   // show loading indicator when loading
    },
    "preDrawCallback": preDrawCallback,
    "drawCallback": postDrawCallback,
    "dom": "tiS", 
    "deferRender": true,
    "columnDefs": dataTableColumnDefs,
    "ajax": dataTableAjax,
    "data": dataTableData,
    "columns": dataTableColumns,
    "fnInfoCallback": !statusTextOverride ? null : function(oSettings, iStart, iEnd, iMax, iTotal, sPre) {
      return statusTextOverride;
    },
    "ordering": ordering
  });

  initDataTableLoad();
};

var debouncedSearch = debounce(function(text) {
  if (text != table.search()) {
    table.search(text).draw();
  }
}, 100);

var loadDataFromUrl = function(callback) {
  // call the server to get data shape
  $.ajax({
        url: "../grid_data",
        data: "show=cols&" + window.location.search.substring(1),
        type: "POST"})
    .done(function(result) {
      callback(result);
    })
    .fail(function(jqXHR)
    {
      if (jqXHR.responseText[0] !== "{")
        showError(jqXHR.responseText);
      else
      {
        var result = $.parseJSON(jqXHR.responseText);

        if (result.error) {
          showError(result.error);
        } else {
          showError("The object could not be displayed.");
        }
      }
    }); 
};

var addResizeHandlers = function(ele) {
   var col = null;            // the column currently being resized
   var initX = null;          // the initial X position of the resize
   var initColWidth = null;   // the initial width of the column
   var initTableWidth = null; // the initial width of the table
   var boundsExceeded = 0;
   var minColWidth = 40;

   var applyDelta = function(delta) {
      var colWidth = initColWidth + delta;

      // don't allow resizing beneath minimum size
      if (delta < 0 && colWidth < minColWidth) {
         boundsExceeded += delta;
         return;
      }

      // if positive delta, consume exceeded bounds before returning to resize
      // mode
      if (delta > 0 && boundsExceeded < 0) {
         boundsExceeded += delta;
         if (boundsExceeded < 0) {
            // didn't consume all bounds
            return;
         } else {
            // consumed all bounds; resize remaining portion of motion
            delta = boundsExceeded;
            colWidth = initColWidth + delta;
         }
      }

      // resize the column in the given direction
      $(".dataTables_scrollHeadInner table").width(initTableWidth + delta);

      var grid = $("#rsGridData");
      if (grid.hasClass("autoSize")) {
         // if the data table is still in auto size mode, we need to switch it 
         // to fixed layout
         
         // observe and manually apply column widths in preparation for 
         // transition to a fixed layout
         var head = $(".dataTables_scrollHead #data_cols th");
         var body = $(".dataTables_scrollBody #data_cols th");
         for (var i = 0; i < Math.min(head.length, body.length); i++) {
            var thHead = head.eq(i), thBody = body.eq(i);
            thHead.width(thHead.width());
            thBody.width(thBody.width());
            manualWidths[i] = thBody.width();
         }
         
         // switch table out of auto size mode and into manual size mode
         grid.addClass("manualSize");
         grid.removeClass("autoSize");
      }

      // adjust header width and width of first column
      $("#data_cols th:nth-child(" + col + ")").width(colWidth);
      grid.width(initTableWidth + delta);

      // record manual width for re-apply on redraw
      manualWidths[col - (rowNumbers ? 1 : 0)] = colWidth;
   };

   var endResize = function() {
      // end the resize operation
      $("#rsGridData td:nth-child(" + col + ")").css(
         "border-right-color", "");
      col = null;
   };

   $(ele).on("mousedown", function(evt) {
      var original = evt.originalEvent;
      if (original.target.className === "resizer") {
         // when the mouse is clicked on the resizer, enter resize mode; figure
         // out which column we're targeting and set up the initial sizes
         col = parseInt(original.target.getAttribute("data-col"));

         // account for row names column
         if (rowNumbers)
            col++;

         // disable propagation of clicks from the resizer to the outer cell
         $(original.target).on("click", function(evt) {
            return false;
         });

         initX = original.clientX;
         initColWidth = $("#data_cols th:nth-child(" + col + ")").width();
         initTableWidth = $("#rsGridData").width();
         boundsExceeded = 0;
         $("#rsGridData td:nth-child(" + col + ")").css(
            "border-right-color", "#A0A0FF");
         evt.preventDefault();
      }
   });
   $(ele).on("mousemove", function(evt) {
      if (col !== null) {
         // if we have an active resize column, resize it by the amount given
         var original = evt.originalEvent;
         applyDelta(original.clientX - initX);
         evt.preventDefault();
      }
   });
   $(ele).on("click", function(evt) {
      if (col !== null) {
         // ignore clicks while resizing
         evt.preventDefault();
      }
   });
   $(ele).on("mouseup", function(evt) {
      if (col !== null) {
         // end resizing if active
         endResize();
         evt.preventDefault();
      }
   });
   $(ele).on("mouseleave", function(evt) {
      if (col !== null) {
         // the mouse left; treat this as a cancel (since leaving means we 
         // won't get a corresponding mouseup)
         applyDelta(0);
         endResize();
      }
   });
}; 

// bootstrapping: 
// 1. clean up state (we re-bootstrap whenever table structure changes)
// 2. make the request to get the shape of the data object to be viewed 
//    (we want this to start as soon as possible so the shape can be prepared
//    on the server while we wait for the geometry to finish initializing on
//    the client)
// 3. wait for the document to be ready
// 4. wait for the window size to stop changing (RStudio animates tab opening)
// 5. initialize the data table
var bootstrap = function(data) {

  // dismiss any active popups
  if (dismissActivePopup)
    dismissActivePopup(true);

  // clean state
  table = null;   
  cols = null;
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
  newEle.innerHTML = "<thead>" +
                     "    <tr id='data_cols'>" +
                     "    </tr>" +
                     "</thead>";  
  addResizeHandlers(newEle);

  if (!data) {
    loadDataFromUrl(function(result) {
      $(document).ready(function() {
        document.body.appendChild(newEle);
        runAfterSizing(function() {
          if (result) {
            initDataTable($.parseJSON(result));
          }
        });
      });
    });
  }
  else {
    $(document).ready(function() {
      document.body.appendChild(newEle);
      runAfterSizing(function() {

        // Assign line numbers:
        if (data.data) {
          data.data = data.data.map(function (e, idx) {
            var eWithNumber = e;
            eWithNumber[""] = idx + 1;
            return eWithNumber;
          });
        }
        else {
          data.data = [
            []
          ];
        }

        if (data.columns) {
          initDataTable(data.columns, data.data);
        }
      });
    });
  }
};

var setHeaderUIVisible = function(visible, initialize, hide) {
  var thead = document.getElementById("data_cols");

  // it's possible the dable is getting redrawn right now; if it is, defer
  // this request.
  if (thead === null || table === null || cols === null) {
    postInitActions["setHeaderUIVisible"] = visible ? (function() {
      setHeaderUIVisible(true, initialize);
    }) : null;
    return false;
  }

  if (!visible) {
    hide(thead);
    // close any popup
    if (dismissActivePopup)
      dismissActivePopup(true);
  }
  for (var i = 0; i < Math.min(thead.children.length, cols.length); i++) {
    var colIdx = i + (rowNumbers ? 0 : 1);
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
window.setFilterUIVisible = function(visible) {
  var setFilterUIVisiblePerColumn = function(th, col, i) {
    if (col.col_search_type === "numeric" || 
        col.col_search_type === "character" ||
        col.col_search_type === "factor" ||
        col.col_search_type === "boolean")  {
      return createFilterUI(i, col);
    }

    return null;
  };

  var hideFilterUI = function() {
    // clear all the filter data
    table.columns().search("");
  };

  return setHeaderUIVisible(visible, setFilterUIVisiblePerColumn, hideFilterUI);
};

// called from RStudio to toggle the filter UI 
window.setColumnDefinitionsUIVisible = function(visible, onColOpen, onColDismiss) {
  var setColumnDefinitionsUIVisiblePerColumn = function(th, col, i) {
    return createColumnTypesUI(th, i, col);
  };

  var hideColumnTypeUI = function(thead) {
    $(".columnTypeWrapper").remove();
  };

  onColumnOpen = onColOpen ? onColOpen : onColumnOpen;
  onColumnDismiss = onColDismiss ? onColDismiss : onColumnDismiss;

  return setHeaderUIVisible(visible, setColumnDefinitionsUIVisiblePerColumn, hideColumnTypeUI);
};

// called from RStudio when the underlying object changes
window.refreshData = function(structureChanged, sizeChanged) {
  // restore any scroll handlers (this can get called on tab activate)
  restoreScrollHandlers();

  if (structureChanged) {
    // structure changed--this necessitates a full refresh
    bootstrap();
  } else {
    // structure didn't change, so just reload data. 
    var s = table.settings();
    var pos = $(".dataTables_scrollBody").scrollTop();
    var row = s.scroller().pixelsToRow(pos);

    // reload data, then snap to that row
    table.ajax.reload(function() {
      s.scrollToRow(row, false);
      if (sizeChanged) {
        debouncedDataTableSize();
      }
    },false);
  }
};

// called from RStudio to apply a column-wide search.
window.applySearch = function(text) {
  debouncedSearch(text);
};

window.onActivate = function() {
  // resize the table once animation finishes
  debouncedDataTableSize(false);
};

window.onDeactivate = function() {
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
  detachedHandlers = [];
  var scrollEvents = $._data(scrollBody[0], "events");
  jQuery.each(scrollEvents.scroll, function(k, v) {
    detachedHandlers.push(v.handler);
  });

  // detach all scroll event handlers
  scrollBody.off("scroll");
};

window.setData = function(data) {
  bootstrap(data);
};

window.setOption = function(option, value) {
  switch (option)
  {
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
  }
};

window.getActiveColumn = function() {
  return activeColumnInfo;
};

var parsedLocation = parseLocationUrl();
var dataMode = parsedLocation && parsedLocation.dataSource ? parsedLocation.dataSource : "server";

// start the first request
if (dataMode === "server") {
  bootstrap();
}

})();

