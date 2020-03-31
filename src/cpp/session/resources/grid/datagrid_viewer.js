(function(){

window.setData = function(data) {
   const numRows = 1000;
   const numCols = 200;

 //  var columnDefs = [{
 //        headerName: "Make",
 //        field: "0"
 //     },
 //     {
 //        headerName: "Model",
 //        field: "1"
 //     },
 //     {
 //        headerName: "Price",
 //        field: "2"
 //     }
 //  ];

   let columnDefs = [];
   for (let i = 0; i < numCols; i++) {
      columnDefs.push({
         headerName: "" + i,
         field: "" + i
      });
   }

   let rowData = [];
   for (let i = 0; i < numRows; i++) {
      let row = {};
      for (let j = 0; j < numCols; j++) {
         row[j] = i + " " + j;
      }
      rowData.push(row);
   }

//   // specify the data
//   var rowData = [{
//         0: "Toyota",
//         1: "Celica",
//         2: 35000
//      },
//      {
//         0: "Ford",
//         1: "Mondeo",
//         2: 32000
//      },
//      {
//         0: "Porsche",
//         1: "Boxter",
//         2: 72000
//      }
//   ];

   // let the grid know which columns and what data to use
   var gridOptions = {
      columnDefs: columnDefs,
      rowData: rowData
   };

   let gridDiv = document.querySelector("#grid");
   new agGrid.Grid(gridDiv, gridOptions);
   };

   window.setOption = function (option, value) {};
})();