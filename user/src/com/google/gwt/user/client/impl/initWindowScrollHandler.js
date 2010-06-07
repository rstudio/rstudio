function __gwt_initWindowScrollHandler(scroll) {
  var wnd = window, oldOnScroll = wnd.onscroll;
  
  wnd.onscroll = function(evt) {
    try {
      scroll();
    } finally {
      oldOnScroll && oldOnScroll(evt);
    }
  };
  
  // Remove the reference once we've initialize the handler
  wnd.__gwt_initWindowScrollHandler = undefined;
}
