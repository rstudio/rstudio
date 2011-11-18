// Installs the script by immediately appending a script tag to the body head
// with the src set, to get the script contents. The script contents are then
// installed into a script tag which is added to the install location (because
// the script contents will be wrapped in a call to onScriptDownloaded()).
function installScript(filename) {
  // Provides the setupWaitForBodyLoad() function
  __WAIT_FOR_BODY_LOADED__

  function installCode(code) {
     function removeScript(body, element) {
      // Unless we're in pretty mode, remove the tags to shrink the DOM a little.
      // It should have installed its code immediately after being added.

      __START_OBFUSCATED_ONLY__
	  body.removeChild(element);
      __END_OBFUSCATED_ONLY__
    }

    var docbody = getInstallLocation();
    var doc = getInstallLocationDoc();
    var script;
    // for sourcemaps, we inject textNodes into the script element on Chrome
    if (navigator.userAgent.indexOf("Chrome") > -1 && window.JSON) {
      var scriptFrag = doc.createDocumentFragment()
      // surround code with eval until crbug #90707 
      scriptFrag.appendChild(doc.createTextNode("eval(\""));
      for (var i = 0; i < code.length; i++) {
        // escape newlines, backslashes, and quotes with JSON.stringify
        // rather than create multiple script tags which mess up line numbers, we use 1 tag, multiple text nodes
        var c = window.JSON.stringify(code[i]); 
        // trim beginning/end quotes
        scriptFrag.appendChild(doc.createTextNode(c.substring(1, c.length - 1)));
      }
      // close the eval
      scriptFrag.appendChild(doc.createTextNode("\");"));
      script = doc.createElement('script');
      script.language='javascript';
      script.appendChild(scriptFrag);
      docbody.appendChild(script);
      removeScript(docbody, script);
    } else {
      for (var i = 0; i < code.length; i++) {
        script = doc.createElement('script');
 	script.language='javascript';
	script.text = code[i];
        docbody.appendChild(script);
        removeScript(docbody, script);
      }
    }
  }

  // Set up a script tag to start downloading immediately, as well as a
  // callback to install the code once it is downloaded and the body is loaded.
  __MODULE_FUNC__.onScriptDownloaded = function(code) {
    setupWaitForBodyLoad(function() {
      installCode(code);
    });
  };
  sendStats('moduleStartup', 'moduleRequested');
  var script = $doc.createElement('script');
  script.src = filename;
  $doc.getElementsByTagName('head')[0].appendChild(script);
}
