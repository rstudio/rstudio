/*jshint browser:true, strict:false, curly:false, indent:3*/

/*
 * dtviewer.js
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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

// show row numbers in index column
var rowNumbers = true;

// offset from which to start rendering columns
var columnOffset = 0;

// maximum number of columns to draw
// the default is maintained separately for view element considerations
var defaultMaxColumns = 50;
var maxColumns = defaultMaxColumns;

// boolean for whether bootstrapping is occurring, used to
// rate limit certain events
var bootstrapping = false;

// show an error--not recoverable; user must click 'retry' to reload 
var showError = function(msg) {
  console.log("error: " + msg);
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
    "\"": "&quot;",
    " ":  "&nbsp;" };
  return html.replace(/[&<> ]/g, function(ch) { return replacements[ch]; });
};

var parseLocationUrl = function() {
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

var initDataTable = function(resCols, data) {

  console.log("initdatatable");
  console.log(resCols);
  console.log(data);

  if (resCols.error) {
    showError(cols.error);
    return;
  }

  // due to the jquery magic done in dataTables with rewriting variables and
  // the amount of window parameters we're already using this is a sane fit
  // for setting constants from dtviewer to dataTables
//  window.dataTableMaxColumns = Math.max(0, cols.length - 1); 
//  window.dataTableColumnOffset = columnOffset;
  
  // look up the query parameters
  var parsedLocation = parseLocationUrl();
  var env = parsedLocation.env, obj = parsedLocation.obj, cacheKey = parsedLocation.cacheKey;
  maxColumns = defaultMaxColumns = parsedLocation.maxCols;

  if (!data) {
    $.ajax({
       url: "../grid_data",
       type: "POST",
       data: "env="+env+ "&obj="+obj+ "&cache_key="+cacheKey+ "&show=data"+ "&column_offset="+columnOffset+ "&max_columns="+maxColumns
       })
       .done(function(result) {
         console.log("data?");
         console.log(result);
       })
       .fail(function(jqXHR) {
        if (jqXHR.responseText[0] !== "{")
          showError(jqXHR.responseText);
        else {
          var result = $.parseJSON(jqXHR.responseText);

          if (result.error) {
              showError(result.error);
          } else {
              showError("The object could not be displayed.");
          }
        }
      });
    }

};

var loadDataFromUrl = function(callback) {
  console.log("loading data from url");

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

var bootstrap = function(data) {

  console.log("bootstrap");
  console.log(data);

  boostrapping = true;

  if (!data) {
    loadDataFromUrl(function(result) {
      $(document).ready(function() {
        if (result) {
          initDataTable($.parseJSON(result));
        }
      });
    });
  }
  else {
    $(document).ready(function() {
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
  }
};

window.setFilterUIVisible = function(visible) {
};

// called from RStudio to toggle the filter UI 
window.setColumnDefinitionsUIVisible = function(visible, onColOpen, onColDismiss) {
};

// called from RStudio when the underlying object changes
window.refreshData = function(structureChanged, sizeChanged) {
  this.console.log("refreshData");
};

// called from RStudio to apply a column-wide search.
window.applySearch = function(text) {
};

window.onActivate = function() {
};

window.onDeactivate = function() {
};

window.setData = function(data) {
  this.console.log("setData");
  this.console.log(data);
  bootstrap(data);
};

window.setOption = function(option, value) {
  this.console.log("setOption");
};

var parsedLocation = parseLocationUrl();
var dataMode = parsedLocation && parsedLocation.dataSource ? parsedLocation.dataSource : "server";

// start the first request
if (dataMode === "server") {
  bootstrap();
}

})();

