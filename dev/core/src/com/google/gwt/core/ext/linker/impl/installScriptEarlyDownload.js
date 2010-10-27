// Installs the script by immediately appending a script tag to the body head
// with the src set, to get the script contents. The script contents are then
// installed into a script tag which is added to the install location (because
// the script contents will be wrapped in a call to onScriptDownloaded()).
function installScript(filename) {
  // Provides the getInstallLocation() and getInstallLocationDoc() functions
  __INSTALL_LOCATION__

  // Provides the setupWaitForBodyLoad() function
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
  sendStats('moduleStartup', 'moduleRequested');
  if (isBodyLoaded()) {
    // if the body is loaded, then the tag to download the script can be added
    // in a non-destructive manner
    var script = document.createElement('script');
    script.src = filename;
    $doc.getElementsByTagName('head')[0].appendChild(script);
  } else {
    // if the doc has not yet loaded, go ahead and do a destructive
    // document.write since we want to immediately start the download.
    // Note that we cannot append an element to the doc if it is still loading
    // since this would cause problems in IE.
    $doc.write("<script src='" + filename + "'></scr" + "ipt>");
  }
}
