$(document).ready(function() {
  var frameId = window.location.search.substr(1);
  $.ajax({
      url: "../grid_shape?frame_id=" + frameId})
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

    // activate the data table
    $("#data").dataTable({
      "processing": true,
      "serverSide": true,
      "pagingType": "full_numbers",
      "dom": "tp", // show table, pagination
      "ajax": {
        "url": "../grid_data", 
        "data": function(d) {
          d.frame_id = frameId;
        }
      }
    });
  });
});
