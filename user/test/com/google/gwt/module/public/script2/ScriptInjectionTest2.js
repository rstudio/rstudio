
// Make the module wait to start to ensure it's really gated by the 
// module-ready function. Intentionally waits longer than 
// ScriptInjectionTest2.js.

setTimeout(init2, 4000);

function init2() {
  isScriptTwoReady = function () {  
    return 'yes2';
  }
}

