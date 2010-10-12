function installScript(filename) {
  // Provides the getInstallLocation() and getInstallLocationDoc() functions
  __INSTALL_LOCATION__

  // Provides the setupWaitForBodyLoad() and isBodyLoaded() functions
  __WAIT_FOR_BODY_LOADED__
  
  function installCode(code) {
    var docbody = getInstallLocation();
    var script = getInstallLocationDoc().createElement('script');
    script.language='javascript';
    script.src = code;
    sendStats('moduleStartup', 'moduleRequested');
    docbody.appendChild(script);

    // Remove the tags to shrink the DOM a little.
    // It should have installed its code immediately after being added.
    docbody.removeChild(script);
  }

  // Just pass along the filename so that a script tag can be installed in the
  // iframe to download it.  Since we will be adding the iframe to the body,
  // we still need to wait for the body to load before going forward.
  setupWaitForBodyLoad(function() {
    installCode(filename);
  });
}
