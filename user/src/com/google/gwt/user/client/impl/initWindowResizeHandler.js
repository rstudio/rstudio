function __gwt_initWindowResizeHandler(resize) {
  var wnd = window, oldOnResize = wnd.onresize;
  
  wnd.onresize = function(evt) {
    try {
      resize();
    } finally {
      oldOnResize && oldOnResize(evt);
    }
  };
  
  // Remove the reference once we've initialize the handler
  wnd.__gwt_initWindowResizeHandler = undefined;
}
