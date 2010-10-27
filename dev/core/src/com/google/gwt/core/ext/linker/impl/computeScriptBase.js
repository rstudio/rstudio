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
  // First, check if the meta properties give the baseUrl
  var metaVal = __gwt_getMetaProperty('baseUrl');
  // Note: the base variable should not be defined in this function because in
  // the older templates (like IFrameTemplate.js), base is defined outside this
  // function, and they rely on the fact that calling computeScriptBase will set
  // that base variable rather than using the return value. Instead, we define
  // a tempBase variable, and then right before we return, we also set the
  // base variable for backwards compatability
  var tempBase = '';

  if (metaVal != null) {
    tempBase = metaVal;
    base = tempBase;
    return tempBase;
  }

  // The baseUrl will be similar to the URL for this script's URL
  var thisScript;

  // By default, this script looks like something/moduleName.nocache.js
  // so look for a script tag that looks like that
  var scriptTags = $doc.getElementsByTagName('script');
  for (var i = 0; i < scriptTags.length; ++i) {
    if (scriptTags[i].src.indexOf('__MODULE_NAME__.nocache.js') != -1) {
      thisScript = scriptTags[i];
    }
  }

  // If the user renamed their script tag, we'll use a fancier method to find
  // it. Note that this will not work in the Late Loading case due to the
  // document.write call.
  if (!thisScript) {
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
    }
  }

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
  };

  if (thisScript && thisScript.src) {
    // Compute our base url
    tempBase = getDirectoryOfFile(thisScript.src);
  }

  // Make the base URL absolute
  if (tempBase == '') {
    // If there's a base tag, use it.
    var baseElements = $doc.getElementsByTagName('base');
    if (baseElements.length > 0) {
      // It's always the last parsed base tag that will apply to this script.
      tempBase = baseElements[baseElements.length - 1].href;
    } else {
      // No base tag; the base must be the same as the document location.
      tempBase = getDirectoryOfFile($doc.location.href);
    }
  } else if ((tempBase.match(/^\w+:\/\//))) {
    // If the URL is obviously absolute, do nothing.
  } else {
    // Probably a relative URL; use magic to make the browser absolutify it.
    // I wish there were a better way to do this, but this seems the only
    // sure way!  (A side benefit is it preloads clear.cache.gif)
    // Note: this trick is harmless if the URL was really already absolute.
    var img = $doc.createElement("img");
    img.src = tempBase + 'clear.cache.gif';
    tempBase = getDirectoryOfFile(img.src);
  }

  if (markerScript) {
    // remove the marker element
    markerScript.parentNode.removeChild(markerScript);
  }

  base = tempBase;
  return tempBase;
}
