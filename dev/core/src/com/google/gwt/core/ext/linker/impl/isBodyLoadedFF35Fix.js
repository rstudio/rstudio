function isBodyLoaded() {
  if (typeof $doc.readyState == "undefined") {
    // FF 3.5 and below does not have readyState, so this implementation takes
    // a conservative approach and returns false, forcing us to wait for the 
    // DOMContentLoaded event.  Note that this will not work for Late Loading
    // apps (since that event has already fired at GWT bootstrap time, so we
    // will wait/hang forever).  However, this approach is an option for non
    // Late Loaded apps that are seeing problems in FF3.5 because they need the
    // body to be loaded before onModuleLoad() is called. Note that GWT
    // bootstrap works fine with the standard apporoach in waitForBodyLoaded.js
    // this is just a fix for apps that do things in onModuleLoad that assume
    // the body is loaded.
    return false;
  }
  return (/loaded|complete/.test($doc.readyState));
}