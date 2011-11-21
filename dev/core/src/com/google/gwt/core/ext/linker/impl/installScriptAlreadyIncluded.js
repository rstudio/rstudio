// Installs a script which has already been downloaded (usually because the 
// script contents are combined with the bootstrap script in cases like SSSS).
// Since the script contents are wrapped in a call to onScriptDownloaded, all
// we do here is set up that function, which will install the contents in
// a script tag appended to the install location.
function installScript(filename) {
  // Provides the setupWaitForBodyLoad() function
  __WAIT_FOR_BODY_LOADED__

  function installCode(code) {
    var docbody = getInstallLocation();
    for (var i = 0; i < code.length; i++) {
      var script = getInstallLocationDoc().createElement('script');
      script.language='javascript';
      script.text = code[i];
      docbody.appendChild(script);

      // Unless we're in pretty mode, remove the tags to shrink the DOM a little.
      // It should have installed its code immediately after being added.
      __START_OBFUSCATED_ONLY__
      docbody.removeChild(script);
      __END_OBFUSCATED_ONLY__
    }
  }
  
  // Set up a script tag to start downloading immediately, as well as a
  // callback to install the code once it is downloaded and the body is loaded.
  __MODULE_FUNC__.onScriptDownloaded = function(code) {
    setupWaitForBodyLoad(function() {
      installCode(code);
    });
  };
}
