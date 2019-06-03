// GWT code can be installed anywhere, but an iFrame is the best place if you
// want both variable isolation and runAsync support. Variable isolation is
// useful for avoiding conflicts with JavaScript libraries and critical if
// you want more than one GWT module on your page. The runAsync implementation
// will need to install additional chunks of code into the same iFrame later.
//
// By default, CrossSiteIFrameLinker will use this script to create the iFrame.
// It may be replaced by overriding CrossSiteIframeLinker.getJsInstallLocation()
// to return the name of a different resource file. The replacement script may
// optionally set this variable inside the iframe:
//
// $wnd - the location where the bootstrap module is defined. It should also
//        be the location where the __gwtStatsEvent function is defined.
//        If not set, the module will set $wnd to window.parent.

var frameDoc;

function getInstallLocationDoc() {
  setupInstallLocation();
  return frameDoc;
}

// This function is left for compatibility
// and may be used by custom linkers
function getInstallLocation() {
  return getInstallLocationDoc().body;
}

function setupInstallLocation() {
  if (frameDoc) { return; }
  // Create the script frame, making sure it's invisible, but not
  // "display:none", which keeps some browsers from running code in it.
  var scriptFrame = $doc.createElement('iframe');
  scriptFrame.id = '__MODULE_NAME__';
  scriptFrame.style.cssText = 'position:absolute; width:0; height:0; border:none; left: -1000px;'
    + ' top: -1000px;';
  scriptFrame.tabIndex = -1;
  scriptFrame.setAttribute('aria-hidden', 'true');
  scriptFrame.title = 'Intentionally blank';
  $doc.body.appendChild(scriptFrame);

  frameDoc = scriptFrame.contentWindow.document;

  // The following code is needed for proper operation in Firefox, Safari, and
  // Internet Explorer.
  //
  // In Firefox, this prevents the frame from re-loading asynchronously and
  // throwing away the current document.
  //
  // In IE, it ensures that the <body> element is immediately available.
  frameDoc.open();
  var doctype = (document.compatMode == 'CSS1Compat') ? '<!doctype html>' : '';
  frameDoc.write(doctype + '<html><head></head><body></body></html>');
  frameDoc.close();
}
