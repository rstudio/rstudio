function loadExternalStylesheets() {
  // Setup for loading of external stylesheets. Resources are loaded
  // only once, even when multiple modules depend on them.  This API must not
  // change across GWT versions.
  if (!$wnd.__gwt_stylesLoaded) { $wnd.__gwt_stylesLoaded = {}; }

  function installOneStylesheet(stylesheetUrl, hrefExpr) {
    if (!__gwt_stylesLoaded[stylesheetUrl]) {
      if (isBodyLoaded()) {
        var l = $doc.createElement('link');
        l.setAttribute('rel', 'stylesheet');
        l.setAttribute('href', hrefExpr);
        $doc.getElementsByTagName('head')[0].appendChild(l);
      } else {
        $doc.write("<link id='' rel='stylesheet' href='" + hrefExpr + "'></li" + "nk>");
      }
      __gwt_stylesLoaded[stylesheetUrl] = true;
    }
  }

  sendStats('loadExternalRefs', 'begin');
  // __MODULE_STYLES__
  sendStats('loadExternalRefs', 'end');
}
