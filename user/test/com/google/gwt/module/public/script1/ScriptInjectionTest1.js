
// Make the module wait to start to ensure it's really gated by the 
// module-ready function.
setTimeout(init1, 2000);

function init1() {
  isScriptOneReady = function () {  
    return 'yes1';
  }
}

