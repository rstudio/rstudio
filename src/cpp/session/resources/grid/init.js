(function(){
var initDataTable = function() {
  // look up the query parameters
  var env = "", obj = "";
  var query = window.location.search.substring(1);
  var queryVars = query.split("&");
  for (var i = 0; i < queryVars.length; i++) {
    var queryVar = queryVars[i].split("=");
    if (queryVar[0] == "env") {
      env = queryVar[1];
    } else if (queryVar[0] == "obj") {
      obj = queryVar[1];
    }
  }
  $.ajax({
      url: "../grid_shape" + window.location.search})
  .done(function(cols){
    // parse result
    cols = $.parseJSON(cols);

    // add each column
    var thead = document.getElementById("data_cols");
    for (var i = 0; i < cols.length; i++) {
      var th = document.createElement("th");
      th.innerText = cols[i];
      thead.appendChild(th);
    }
    var scrollHeight = window.innerHeight - (thead.clientHeight + 2);

    // activate the data table
    var table = $("#data").dataTable({
      "processing": true,
      "serverSide": true,
      "pagingType": "full_numbers",
      "pageLength": 25,
      "scrollY": scrollHeight + "px",
      "dom": "tS", 
      "deferRender": true,
      "ajax": {
        "url": "../grid_data", 
        "data": function(d) {
          d.env = env;
          d.obj = obj;
        }
      }
    });
  });
};

$(document).ready(function() {
  window.setTimeout(function() {
      initDataTable();
  },200);
});

})();
