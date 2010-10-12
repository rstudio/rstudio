function installScript(filename) {
  // Provides the getInstallLocation() and getInstallLocationDoc() functions
  __INSTALL_LOCATION__

  // Provides the setupWaitForBodyLoad() and isBodyLoaded() functions
  __WAIT_FOR_BODY_LOADED__

  function installCode(code) {
    var docbody = getInstallLocation();
    var script = getInstallLocationDoc().createElement('script');
    script.language='javascript';
    script.text = code;
    docbody.appendChild(script);

    // Remove the tags to shrink the DOM a little.
    // It should have installed its code immediately after being added.
    docbody.removeChild(script);
  }
  
  // Set up a script tag to start downloading immediately, as well as a
  // callback to install the code once it is downloaded and the body is loaded.
  __MODULE_FUNC__.onScriptDownloaded = function(code) {
    setupWaitForBodyLoad(function() {
      installCode(code);
    });
  };
}
