function isBodyLoaded() {
  if (typeof $doc.readyState == "undefined") {
    // FF 3.5 and below does not have readyState, but it does allow us to
    // append to the body before it has finished loading, so we return whether
    // the body element exists. Note that for very few apps, this may cause
    // problems because they do something in onModuleLoad that assumes the body
    // is loaded.  For those apps, we provide an alternative implementation
    // in isBodyLoadedFf35Fix.js
    return (typeof $doc.body != "undefined" && $doc.body != null);
  }
  return (/loaded|complete/.test($doc.readyState));
}
