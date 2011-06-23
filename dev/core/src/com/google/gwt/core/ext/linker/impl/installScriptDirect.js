// Installs the script directly, by simply appending a script tag with the
// src set to the correct location to the install location.
function installScript(filename) {
  // Provides the setupWaitForBodyLoad()function
  __WAIT_FOR_BODY_LOADED__
  
  function installCode(code) {
    var docbody = getInstallLocation();
    var script = getInstallLocationDoc().createElement('script');
    script.language='javascript';
    script.src = code;
    sendStats('moduleStartup', 'moduleRequested');
    docbody.appendChild(script);
  }

  // Just pass along the filename so that a script tag can be installed in the
  // iframe to download it.  Since we will be adding the iframe to the body,
  // we still need to wait for the body to load before going forward.
  setupWaitForBodyLoad(function() {
    installCode(filename);
  });
}
