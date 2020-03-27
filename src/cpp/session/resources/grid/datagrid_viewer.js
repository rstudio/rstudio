(function(){

window.setData = function(data) {
//  this.console.log("setdata");
//  this.document.getElementById("test_div").innerHTML = data;

      let d = [];
      for (let i = 0; i < 10000; i++) {
         let row = {};
         for (let j = 0; j < 50; j++) {
            row[j] = "row" + i + " col " + j;
         }
         d.push(row);
      }

      const myGrid = new fin.Hypergrid(null, {});
      myGrid.setData(d);
};

window.setOption = function(option, value) {
};
})();