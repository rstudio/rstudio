/*
 * init.js
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

var dismissActivePopup = function() { };

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
  document.getElementById("error").innerText = msg;
  document.getElementById("data").style.display = "none";
};

// simple HTML escaping (avoid XSS in data)
var escapeHtml = function(html) {
  var replacements = {
    "<":  "&lt;",
    ">":  "&gt;",
    "&":  "&amp;" };
  return html.replace(/[&<>]/g, function(ch) { return replacements[ch]; });
};

// render a text cell--if no search is active, just renders the data literally;
// when search is active, highlights the portion of the text that matches the
// search
var renderTextCell = function(data, type, row, meta) {
  var search = table.search();
  if (search.length > 0) {
    var idx = data.toLowerCase().indexOf(search.toLowerCase());
    if (idx >= 0) {
      return escapeHtml(data.substring(0, idx)) + '<span class="searchMatch">' + 
             escapeHtml(data.substring(idx, idx + search.length)) + '</span>' + 
             escapeHtml(data.substring(idx + search.length, data.length));
    }
    return escapeHtml(data);
  }
  return escapeHtml(data);
};

// render a number cell
var renderNumberCell = function(data, type, row, meta) {
  return '<div class="numberCell">' + 
         renderTextCell(data, type, row, meta) + 
         '</div>';
};

// applies a new size to the table--called on init, on tab activate (from
// RStudio), and when the window size changes
var sizeDataTable = function(recalc) {
  // don't apply a zero height
  if (window.innerHeight < 1) {
    return;
  }

  // recalculate rows at new size if needed (used when our initial init
  // happened in a hidden tab)
  if (recalc) {
    table.settings().scroller().measure(false);
  }

  // adjust scroll body height accordingly
  var scrollBody = $(".dataTables_scrollBody");
  if (scrollBody && scrollBody.length > 0) {
    scrollBody.css("height", 
      window.innerHeight - ($("thead").height() + 25));
    $("#data").DataTable().columns.adjust().draw();
  }
};

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
  }, 10);
};


var createNumericFilterUI = function(idx, col, onDismiss) {
  var ele = document.createElement("div");
  invokeFilterPopup(ele, function(popup) {
    popup.className += " numericFilterPopup";
    var min = col.col_min.toString();
    var max = col.col_max.toString();
    var val = table.columns(idx).search()[0];
    if (val.indexOf("-") > 0) {
      var range = val.split("-");
      min = range[0];
      max = range[1];
    } else if (!isNaN(parseInt(val))) {
      min = parseInt(val);
      max = parseInt(val);
    }
    var minVal = document.createElement("div");
    minVal.innerText = min;
    minVal.className = "numMin selected";
    popup.appendChild(minVal);
    var maxVal = document.createElement("div");
    maxVal.innerText = max;
    maxVal.className = "numMax selected";
    popup.appendChild(maxVal);
    var slider = document.createElement("div");
    slider.className = "numSlider";
    popup.appendChild(slider);
    var updateView = debounce(function() {
      searchText = minVal.innerText === min && maxVal.innerText === max ? 
        "" : minVal.innerText + "-" + maxVal.innerText;
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
        minVal.innerText = ui.values[0];
        maxVal.innerText = ui.values[1];
        updateView();
      }
    });
  }, onDismiss, false);
  ele.innerText = "[...]";
  return ele;
};

var createFactorFilterUI = function(idx, col, onDismiss) {
  var ele = document.createElement("div");
  var display = document.createElement("span");
  display.innerHTML = "&nbsp;";
  ele.appendChild(display);

  var setValHandler = function(factor, text) {
      return function(evt) {
        table.columns(idx).search(factor.toString()).draw();
        display.innerText = text;
      };
  };
  invokeFilterPopup(ele, function(popup) {
    var list = document.createElement("div");
    list.className = "factorList";
    var val = table.columns(idx).search()[0];
    var current = val.length > 0 ? parseInt(val) : 0;
    for (var i = 0; i < col.col_vals.length; i++) {
      var opt = document.createElement("div");
      opt.innerText = col.col_vals[i];
      opt.className = "factorListItem";
      opt.addEventListener("click", setValHandler(i + 1, col.col_vals[i]));
      list.appendChild(opt);
    }
    popup.appendChild(list);
  }, onDismiss, false);

  return ele;
};

var createTextFilterUI = function(idx, col, onDismiss) {
  var ele = document.createElement("div");
  var input = document.createElement("input");
  input.type = "text";
  input.className = "textFilterBox";
  input.value = table.columns(idx).search()[0];
  var updateView = debounce(function() {
          table.columns(idx).search(input.value).draw();
    }, 200);
  input.addEventListener("keyup", function(evt) {
    // TODO: handle Esc
    updateView();
  });
  input.addEventListener("blur", function(evt) {
    onDismiss();
  });
  ele.addEventListener("click", function(evt) {
    input.focus();
    evt.preventDefault();
    evt.stopPropagation();
  });
  ele.appendChild(input);
  return ele;
};

var invokeFilterPopup = function (ele, buildPopup, onDismiss, dismissOnClick) {
  var popup = null;

  var dismissPopup = function() {
    if (popup) {
      document.body.removeChild(popup);
      document.body.removeEventListener(checkLightDismiss);
      popup = null;
      onDismiss();
      return true;
    }
    return false;
  };
  
  var checkLightDismiss = function(evt) {
    if (popup && (!dismissOnClick || !popup.contains(evt.target))) {
      dismissPopup();
    }
  };

  ele.addEventListener("click", function(evt) {
    // dismiss any other popup
    if (dismissActivePopup != dismissPopup) {
      dismissActivePopup();
    }
    if (popup) {
      dismissPopup();
    } else {
      popup = createFilterPopup();
      buildPopup(popup);
      popup.style.top = ($(ele).offset().top + 20) + "px";
      popup.style.left = ($(ele).offset().left - 4)  + "px";
      document.body.appendChild(popup);
      document.body.addEventListener("click", checkLightDismiss);
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
    dismissActivePopup();
    table.columns(idx).search("").draw();
    setUnfiltered();
    evt.preventDefault();
    evt.stopPropagation();
  });
  host.appendChild(clear);

  val = document.createElement("div");
  val.innerText = "All";
  val.addEventListener("click", function(evt) {
    if (col.col_type === "numeric") {
      ui = createNumericFilterUI(idx, col, onDismiss);
    } else if (col.col_type === "factor") {
      ui = createFactorFilterUI(idx, col, onDismiss);
    } else if (col.col_type === "character") {
      ui = createTextFilterUI(idx, col, onDismiss);
    }
    if (ui) {
      ui.className += " filterValue";
      host.replaceChild(ui, val);
      host.className = "colFilter filtered";
      clear.style.display = "block";
      ui.click();
      evt.preventDefault();
      evt.stopPropagation();
    }
  });

  host.appendChild(val);
  host.style.display = "none";
  return host;
};

var createFilterPopup = function() {
  var filterUI = document.createElement("div");
  filterUI.className = "filterPopup";
  return filterUI;
};

var showFilterUI = function(idx, col) {
  var filter = document.getElementById("filterValues");
  var currentColValue = 
    table.columns(idx).search()[0];
  filter.innerHTML = "";
  if (col.col_type === "character") {
    // build filter UI for character fields 
    var input = document.createElement("input");
    input.type = "text";
    input.value = currentColValue;
    getFilterColValue = function() {
      return input.value;
    };
    getFilterDisplayValue = function() {
      return input.value === "" ? "All" : input.value;
    };
    filter.appendChild(input);
    input.focus();
  } else if (col.col_type === "factor") {
    // build filter UI for factor fields
    var sel = document.createElement("select");
    var all = document.createElement("option");
    all.value = "";
    all.innerText = "All";
    sel.appendChild(all);
    var current = currentColValue.length > 0 ? parseInt(currentColValue) : 0;
    for (var i = 0; i < col.col_vals.length; i++) {
      var opt = document.createElement("option");
      opt.value = i + 1;
      opt.innerText = col.col_vals[i];
      sel.appendChild(opt);
    }
    if (current > 0)
      sel.selectedIndex = current;
    getFilterColValue = function() {
      return sel.children[sel.selectedIndex].value;
    };
    getFilterDisplayValue = function() {
      return sel.children[sel.selectedIndex].innerText;
    };
    filter.appendChild(sel);
  } else if (col.col_type === "numeric") {
    // build filter UI for numeric fields
    var min = col.col_min.toString();
    var max = col.col_max.toString();
    if (currentColValue.indexOf("-") > 0) {
      var range = currentColValue.split("-");
      min = range[0];
      max = range[1];
    } else if (!isNaN(parseInt(currentColValue))) {
      min = parseInt(currentColValue);
      max = parseInt(currentColValue);
    }
    var minVal = document.createElement("div");
    minVal.innerText = min;
    minVal.className = "numMin selected";
    filter.appendChild(minVal);
    var maxVal = document.createElement("div");
    maxVal.innerText = max;
    maxVal.className = "numMax selected";
    filter.appendChild(maxVal);

    var slider = document.createElement("div");
    slider.className = "numSlider";
    $(slider).slider({
      range:  true,
      min:    col.col_min,
      max:    col.col_max,
      step:   Math.min(stepPrecision(col.col_min.toString()), 
                       stepPrecision(col.col_max.toString())),
      values: [min, max],
      slide:  function(event, ui) {
        minVal.innerText = ui.values[0];
        maxVal.innerText = ui.values[1];
      }
    });
    getFilterColValue = function() {
      if (minVal.innerText === col.col_min.toString() &&
          maxVal.innerText === col.col_max.toString())
        // no restrictions
        return "";
      else if (minVal.innerText === maxVal.innerText)
        // min and max are identical
        return minVal.innerText;
      else
        // show a range
        return minVal.innerText + "-" + maxVal.innerText;
    };
    getFilterDisplayValue = function() {
      if (minVal.innerText === col.col_min.toString() &&
          maxVal.innerText === col.col_max.toString())
        return "All";
      else if (minVal.innerText === maxVal.innerText)
        return minVal.innerText;
      else
        return minVal.innerText + " - " + maxVal.innerText;
    };
    filter.appendChild(slider);
  }

  // position the filter box by the column to filter
  var filterUI = document.getElementById("filterUI");
  var thead = document.getElementById("data_cols");
  var scroll = document.getElementById("data").parentElement.scrollLeft;

  filterUI.style.left = ((thead.children[idx].offsetLeft - scroll) + 7) + "px";
  filterUI.style.top = thead.children[idx].offsetHeight + "px";
  filterUI.style.display = "block";

  filterColIdx = idx;
};

var hideFilterUI = function() {
  document.getElementById("filterUI").style.display = "none";
  document.getElementById("filterValues").innerHTML = "";
};

var updateColFilterDisplay = function() {
  var filterDisplay = document.getElementById("data_cols")
                              .children[filterColIdx].children[1];
  filterDisplay.innerText = getFilterDisplayValue();
  filterDisplay.className = getFilterColValue().length > 0 ? 
    "colFilter filtered" : "colFilter unfiltered";
};

var createHeader = function(idx, col) {
  var th = document.createElement("th");

  // add the title
  var title = document.createElement("div");
  title.innerText = col.col_name;
  th.appendChild(title);
  th.title = col.col_type;
  if (col.col_type === "numeric") {
    th.title += " with range " + col.col_min + " - " + col.col_max;
  } else if (col.col_type === "factor") {
    th.title += " with " + col.col_vals.length + " levels";
  }

  if (col.col_type === "numeric" || 
      col.col_type === "character" ||
      col.col_type === "factor")  {
    var filter = createFilterUI(idx, col);
    th.appendChild(filter);
  }

  return th;
};

var initDataTable = function() {
  // look up the query parameters
  var env = "", obj = "", cacheKey = "";
  var query = window.location.search.substring(1);
  var queryVars = query.split("&");
  for (var i = 0; i < queryVars.length; i++) {
    var queryVar = queryVars[i].split("=");
    if (queryVar[0] == "env") {
      env = queryVar[1];
    } else if (queryVar[0] == "obj") {
      obj = queryVar[1];
    } else if (queryVar[0] == "cache_key") {
      cacheKey = queryVar[1];
    }
  }
  $.ajax({
      url: "../grid_data?show=cols&" + window.location.search.substring(1)})
  .done(function(cols) {
    // parse result
    cols = $.parseJSON(cols);

    if (cols.error) {
      showError(cols.error);
      return;
    }

    // keep track of which columns are numeric and which are text (we use
    // different renderers for these types)
    var numberCols = [];
    var textCols = [];

    // add each column
    var thead = document.getElementById("data_cols");
    for (var i = 0; i < cols.length; i++) {
      // create table header
      thead.appendChild(createHeader(i, cols[i]));
      if (cols[i].col_type === "numeric") {
        numberCols.push(i);
      } else {
        textCols.push(i);
      }
    }
    var scrollHeight = window.innerHeight - (thead.clientHeight + 2);

    // activate the data table
    $("#data").dataTable({
      "processing": true,
      "serverSide": true,
      "pagingType": "full_numbers",
      "pageLength": 25,
      "scrollY": scrollHeight + "px",
      "scrollX": true,
      "dom": "tiS", 
      "deferRender": true,
      "columnDefs": [ {
        "targets": numberCols,
        "render": renderNumberCell
        }, {
        "targets": textCols,
        "render": renderTextCell
        }],
      "ajax": {
        "url": "../grid_data", 
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
        },
       }
    });

    table = $("#data").DataTable();

    // perform initial sizing and listen for size changes
    sizeDataTable(false);
    window.addEventListener("resize", function() { sizeDataTable(false); });
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

$(document).ready(function() {
  runAfterSizing(initDataTable);
});


// Exports -------------------------------------------------------------------

// called from RStudio to toggle the filter UI 
window.setFilterUIVisible = function(visible) {
  var thead = document.getElementById("data_cols");
  if (!visible) {
    // clear all the filter data
    $("#data").DataTable().columns().search("");
  }
  for (var i = 0; i < thead.children.length; i++) {
    if (thead.children[i].children.length > 1) {
      var filter = thead.children[i].children[1];
      filter.style.display = visible ? "block" : "none";
    }
  }
  sizeDataTable(false);
};

// called from RStudio when the underlying object changes
window.refreshData = function(structureChanged) {
  if (structureChanged) {
    // structure changed--this necessitates a full refresh
    window.location.reload();
  } else {
    // structure didn't change, so just reload data. 
    var t = $("#data").DataTable();
    var s = t.settings();
    var pos = $(".dataTables_scrollBody").scrollTop();
    var row = s.scroller().pixelsToRow(pos);

    // reload data, then snap to that row
    t.ajax.reload(function() {
      s.scrollToRow(row, false);
    },false);
  }
};

// called from RStudio to apply a column-wide search.
// consider: called per keystroke; throttle?
window.applySearch = function(text) {
  var t = $("#data").DataTable();
  if (text != t.search()) {
    t.search(text).draw();
  }
};

window.applySizeChange = function() {
  runAfterSizing(function() { sizeDataTable(true); });
};

})();
