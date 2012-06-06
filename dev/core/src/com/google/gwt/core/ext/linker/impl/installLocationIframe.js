// The GWT code can be installed anywhere, although an iFrame is the best
// approach if you want both variable isolation (useful in general, but
// critical if you want more than one GWT module on your page) and runAsync
// which will need to be able to install additional chunks of code into that
// isolated environment later on.

// The items that must be provided in any install location are:
// $wnd - the location where the bootstrap module is defined. Should also
//        be the location where the __gwtStatsEvent is defined

var frameDoc;

function getInstallLocationDoc() {
  setupInstallLocation();
  return frameDoc;
}

function getInstallLocation() {
  setupInstallLocation();
  return frameDoc.getElementsByTagName('body')[0];
}

function setupInstallLocation() {
  if (frameDoc) { return; }
  // Create the script frame, making sure it's invisible, but not
  // "display:none", which keeps some browsers from running code in it.
  var scriptFrame = $doc.createElement('iframe');
  scriptFrame.src = 'javascript:""';
  scriptFrame.id = '__MODULE_NAME__';
  scriptFrame.style.cssText = 'position:absolute; width:0; height:0; border:none; left: -1000px;'
    + ' top: -1000px;';
  scriptFrame.tabIndex = -1;
  $doc.body.appendChild(scriptFrame);

  frameDoc = scriptFrame.contentDocument;
  if (!frameDoc) {
    frameDoc = scriptFrame.contentWindow.document;
  }

  // The missing content has been seen on Safari 3 and firebug will
  // behave incorrectly on soft refresh unless we explicitly set the content
  // of the frame. However, we don't want to do this when runAsync calls
  // installCode, so we do it here when we create the iframe.
  frameDoc.open();
  frameDoc.write('<html><head></head><body></body></html>');
  frameDoc.close();

  var frameDocbody = frameDoc.getElementsByTagName('body')[0];
  var script = frameDoc.createElement('script');
  script.language='javascript';
  var temp = "var $wnd = window.parent;";
  script.text = temp;
  frameDocbody.appendChild(script);
}
