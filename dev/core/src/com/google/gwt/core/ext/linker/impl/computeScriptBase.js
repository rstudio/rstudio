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
 * Determine our own script's URL via magic :)
 * This function produces one side-effect, it sets base to the module's
 * base url.
 * 
 * This is included into the selection scripts
 * wherever COMPUTE_SCRIPT_BASE appears with underlines
 * on each side.
 */
function computeScriptBase() {
  var thisScript
  ,markerId = "__gwt_marker___MODULE_NAME__"
  ,markerScript;

  if (metaProps['baseUrl']) {
    base = metaProps['baseUrl'];
    return;
  }

  $doc.write('<script id="' + markerId + '"></script>');
  markerScript = $doc.getElementById(markerId);

  // Our script element is assumed to be the closest previous script element
  // to the marker, so start at the marker and walk backwards until we find
  // a script.
  thisScript = markerScript && markerScript.previousSibling;
  while (thisScript && thisScript.tagName != 'SCRIPT') {
    thisScript = thisScript.previousSibling;
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
    base = getDirectoryOfFile(thisScript.src);
  }

  // Make the base URL absolute
  if (base == '') {
    // If there's a base tag, use it.
    var baseElements = $doc.getElementsByTagName('base');
    if (baseElements.length > 0) {
      // It's always the last parsed base tag that will apply to this script.
      base = baseElements[baseElements.length - 1].href;
    } else {
      // No base tag; the base must be the same as the document location.
      base = getDirectoryOfFile($doc.location.href);
    }
  } else if ((base.match(/^\w+:\/\//))) {
    // If the URL is obviously absolute, do nothing.
  } else {
    // Probably a relative URL; use magic to make the browser absolutify it.
    // I wish there were a better way to do this, but this seems the only
    // sure way!  (A side benefit is it preloads clear.cache.gif)
    // Note: this trick is harmless if the URL was really already absolute.
    var img = $doc.createElement("img");
    img.src = base + 'clear.cache.gif';
    base = getDirectoryOfFile(img.src);
  }

  if (markerScript) {
    // remove the marker element
    markerScript.parentNode.removeChild(markerScript);
  }
}