/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * Determine our own script's URL by trying various things
 *
 * First - use the baseUrl meta tag if it exists
 * Second - look for a script tag with the src set to MODULE_NAME.nocache.js and
 *   if it's found, use it to determine the baseUrl
 * Third - if the page is not already loaded, try to use some document.write
 *   magic to install a temporary tag and use that to determine the baseUrl.
 * 
 * This is included into the selection scripts
 * wherever COMPUTE_SCRIPT_BASE appears with underlines
 * on each side.
 */

function computeScriptBase() {

  // ================ Helper methods to process tags ======================  
  // Gets the part of a url up to and including the 'path' portion.
  function getDirectoryOfFile(path) {
    // Truncate starting at the first '?' or '#', whichever comes first. 
    var hashIndex = path.lastIndexOf('#');
    if (hashIndex == -1) {
      hashIndex = path.length;
    }
    var queryIndex = path.indexOf('?');
    if (queryIndex == -1) {
      queryIndex = path.length;
    }
    var slashIndex = path.lastIndexOf('/', Math.min(queryIndex, hashIndex));
    return (slashIndex >= 0) ? path.substring(0, slashIndex + 1) : '';
  }

  function ensureAbsoluteUrl(url) {
    if ((url.match(/^\w+:\/\//))) {
      // If the URL is obviously absolute, do nothing.
    } else {
      // Probably a relative URL; use magic to make the browser absolutify it.
      // I wish there were a better way to do this, but this seems the only
      // sure way!  (A side benefit is it preloads clear.cache.gif)
      // Note: this trick is harmless if the URL was really already absolute.
      var img = $doc.createElement("img");
      img.src = url + 'clear.cache.gif';
      url = getDirectoryOfFile(img.src);
    }
    return url;
  }
  
// =============== Various methods to try finding the base =================  
  function tryMetaTag() {
    var metaVal = __gwt_getMetaProperty('baseUrl');
    if (metaVal != null) {
      return metaVal;
    }
    return '';
  }
    
  function tryNocacheJsTag() {
    // By default, this script looks like something/moduleName.nocache.js
    // so look for a script tag that looks like that
    var scriptTags = $doc.getElementsByTagName('script');
    for (var i = 0; i < scriptTags.length; ++i) {
      if (scriptTags[i].src.indexOf('__MODULE_NAME__.nocache.js') != -1) {
        return getDirectoryOfFile(scriptTags[i].src);
      }
    }
    return '';
  }

  function tryMarkerScript() {
    // If the user renamed their script tag, we'll use a fancier method to find
    // it. Note that this will not work in the Late Loading case due to the
    // document.write call.
    var thisScript;
    if (typeof isBodyLoaded == 'undefined' || !isBodyLoaded()) {
      // Put in a marker script element which should be the first script tag after
      // the tag we're looking for. To find it, we start at the marker and walk
      // backwards until we find a script.
      var markerId = "__gwt_marker___MODULE_NAME__";
      var markerScript;
      $doc.write('<script id="' + markerId + '"></script>');
      markerScript = $doc.getElementById(markerId);
      thisScript = markerScript && markerScript.previousSibling;
      while (thisScript && thisScript.tagName != 'SCRIPT') {
        thisScript = thisScript.previousSibling;
      }
      if (markerScript) {
        markerScript.parentNode.removeChild(markerScript);
      }
      if (thisScript && thisScript.src) {
        return getDirectoryOfFile(thisScript.src);
      }
    }
    return '';
  }

  function tryBaseTag() {
    var baseElements = $doc.getElementsByTagName('base');
    if (baseElements.length > 0) {
      // It's always the last parsed base tag that will apply to this script.
      return baseElements[baseElements.length - 1].href;
    }
    return '';
  }

  function isLocationOk() {
    var loc = $doc.location;
    return loc.href ==
        (loc.protocol + "//" + loc.host + loc.pathname + loc.search + loc.hash);
  }

// ================ Inline Code =============================================
  var tempBase = tryMetaTag();
  if (tempBase == '') {
    tempBase = tryNocacheJsTag();
  }
  if (tempBase == '') {
    tempBase = tryMarkerScript();
  }
  if (tempBase == '') {
    tempBase = tryBaseTag();
  }
  if (tempBase == '' && isLocationOk()) {
    // last resort
    tempBase = getDirectoryOfFile($doc.location.href);
  }  
  
  tempBase = ensureAbsoluteUrl(tempBase);
  
  // Note: the base variable should not be defined in this function because in
  // the older templates (like IFrameTemplate.js), base is defined outside this
  // function, and they rely on the fact that calling computeScriptBase will set
  // that base variable rather than using the return value.
  base = tempBase;
  return tempBase;
}
