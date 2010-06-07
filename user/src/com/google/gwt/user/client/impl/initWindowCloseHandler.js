function __gwt_initWindowCloseHandler(beforeunload, unload) {
  var wnd = window
  , oldOnBeforeUnload = wnd.onbeforeunload
  , oldOnUnload = wnd.onunload;
  
  wnd.onbeforeunload = function(evt) {
    var ret, oldRet;
    try {
      ret = beforeunload();
    } finally {
      oldRet = oldOnBeforeUnload && oldOnBeforeUnload(evt);
    }
    // Avoid returning null as IE6 will coerce it into a string.
    // Ensure that "" gets returned properly.
    if (ret != null) {
      return ret;
    }
    if (oldRet != null) {
      return oldRet;
    }
    // returns undefined.
  };
  
  wnd.onunload = function(evt) {
    try {
      unload();
    } finally {
      oldOnUnload && oldOnUnload(evt);
      wnd.onresize = null;
      wnd.onscroll = null;
      wnd.onbeforeunload = null;
      wnd.onunload = null;
    }
  };
  
  // Remove the reference once we've initialize the handler
  wnd.__gwt_initWindowCloseHandler = undefined;
}
