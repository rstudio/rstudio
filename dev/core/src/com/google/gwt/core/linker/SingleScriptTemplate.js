/*
 * Copyright 2007 Google Inc.
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

window.__MODULE_FUNC__ = function() {
  // ---------------- INTERNAL GLOBALS ----------------
  
  // Cache symbols locally for good obfuscation
  var $wnd = window
  ,$doc = document
  
  // These variables gate calling gwtOnLoad; all must be true to start
  ,gwtOnLoad, bodyDone

  // If non-empty, an alternate base url for this module
  ,base = ''
  
  // A map of properties that were declared in meta tags
  ,metaProps = {}
  
  // Maps property names onto sets of legal values for that property.
  ,values = []
  
  // Maps property names onto a function to compute that property.
  ,providers = []
  
  // A multi-tier lookup map that uses actual property values to quickly find
  // the strong name of the cache.js file to load.
  ,answers = []

  // Error functions.  Default unset in compiled mode, may be set by meta props.
  ,onLoadErrorFunc, propertyErrorFunc
  
  ; // end of global vars

  // ------------------ TRUE GLOBALS ------------------

  // Maps to synchronize the loading of styles and scripts; resources are loaded
  // only once, even when multiple modules depend on them.  This API must not
  // change across GWT versions.
  if (!$wnd.__gwt_stylesLoaded) { $wnd.__gwt_stylesLoaded = {}; }
  if (!$wnd.__gwt_scriptsLoaded) { $wnd.__gwt_scriptsLoaded = {}; }

  // --------------- INTERNAL FUNCTIONS ---------------

  function isHostedMode() {
    try {
      return ($wnd.external && $wnd.external.gwtOnLoad &&
          ($wnd.location.search.indexOf('gwt.hybrid') == -1));
    } catch (e) {
      // Defensive: some versions of IE7 reportedly can throw an exception
      // evaluating "external.gwtOnLoad".
      return false;
    }
  }
  
  // Called by onScriptLoad() and onload(). It causes
  // the specified module to be cranked up.
  //
  function maybeStartModule() {
    // TODO: it may not be necessary to check gwtOnLoad here.
    if (gwtOnLoad && bodyDone) {
      gwtOnLoad(onLoadErrorFunc, '__MODULE_NAME__', base);
    }
  }

  // Determine our own script's URL via magic :)
  //
  function computeScriptBase() {
    // see if gwt.js left a marker for us
    var thisScript, markerScript;

    // try writing a marker
    $doc.write('<script id="__gwt_marker___MODULE_NAME__"></script>');
    markerScript = $doc.getElementById("__gwt_marker___MODULE_NAME__");
    if (markerScript) {
      // this script should be the previous element
      thisScript = markerScript.previousSibling;
    }

    function getDirectoryOfFile(path) {
      var eq = path.lastIndexOf('/');
      return (eq >= 0) ? path.substring(0, eq + 1) : '';
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
        var loc = $doc.location;
        var href = loc.href;
        base = getDirectoryOfFile(href.substr(0, href.length
        	- loc.hash.length));
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
  
  // Called to slurp up all <meta> tags:
  // gwt:property, gwt:onPropertyErrorFn, gwt:onLoadErrorFn
  //
  function processMetas() {
    var metas = document.getElementsByTagName('meta');
    for (var i = 0, n = metas.length; i < n; ++i) {
      var meta = metas[i], name = meta.getAttribute('name'), content;
  
      if (name) {
        if (name == 'gwt:property') {
          content = meta.getAttribute('content');
          if (content) {
            var value, eq = content.indexOf('=');
            if (eq >= 0) {
              name = content.substring(0, eq);
              value = content.substring(eq+1);
            } else {
              name = content;
              value = '';
            }
            metaProps[name] = value;
          }
        } else if (name == 'gwt:onPropertyErrorFn') {
          content = meta.getAttribute('content');
          if (content) {
            try {
              propertyErrorFunc = eval(content);
            } catch (e) {
              alert('Bad handler \"' + content +
                '\" for \"gwt:onPropertyErrorFn\"');
            }
          }
        } else if (name == 'gwt:onLoadErrorFn') {
          content = meta.getAttribute('content');
          if (content) {
            try {
              onLoadErrorFunc = eval(content);
            } catch (e) {
              alert('Bad handler \"' + content + '\" for \"gwt:onLoadErrorFn\"');
            }
          }
        }
      }
    }
  }

  /**
   * Determines whether or not a particular property value is allowed. Called by
   * property providers.
   * 
   * @param propName the name of the property being checked
   * @param propValue the property value being tested
   */
  function __gwt_isKnownPropertyValue(propName, propValue) {
    return propValue in values[propName];
  }

  /**
   * Returns a meta property value, if any.  Used by DefaultPropertyProvider.
   */
  function __gwt_getMetaProperty(name) {
    var value = metaProps[name];
    return (value == null) ? null : value;
  }
  
  // --------------- EXPOSED FUNCTIONS ----------------

  // Called when the compiled script identified by moduleName is done loading.
  //
  __MODULE_FUNC__.onScriptLoad = function(gwtOnLoadFunc) {
    // remove this whole function from the global namespace to allow GC
    __MODULE_FUNC__ = null;
    gwtOnLoad = gwtOnLoadFunc;
    maybeStartModule();
  }

  // --------------- STRAIGHT-LINE CODE ---------------

  // do it early for compile/browse rebasing
  computeScriptBase();
  processMetas();
  
  // --------------- WINDOW ONLOAD HOOK ---------------

  var onBodyDoneTimerId;
  function onBodyDone() {
    if (!bodyDone) {
      bodyDone = true;
// __MODULE_STYLES_BEGIN__
     // Style resources are injected here to prevent operation aborted errors on ie
// __MODULE_STYLES_END__
      maybeStartModule();

      if ($doc.removeEventListener) {
        $doc.removeEventListener("DOMContentLoaded", onBodyDone, false);
      }
      if (onBodyDoneTimerId) {
        clearInterval(onBodyDoneTimerId);
      }
    }
  }

  // For everyone that supports DOMContentLoaded.
  if ($doc.addEventListener) {
    $doc.addEventListener("DOMContentLoaded", onBodyDone, false);
  }

  // Fallback. If onBodyDone() gets fired twice, it's not a big deal.
  var onBodyDoneTimerId = setInterval(function() {
    if (/loaded|complete/.test($doc.readyState)) {
      onBodyDone();
    }
  }, 50);

// __MODULE_SCRIPTS_BEGIN__
  // Script resources are injected here
// __MODULE_SCRIPTS_END__
}

// Called from compiled code to hook the window's resize & load events (the
// code running in the script frame is not allowed to hook these directly).
// 
// Notes:
// 1) We declare it here in the global scope so that it won't closure the
// internals of the module func.
//
// 2) We hang it off the module func to avoid polluting the global namespace.
//
// 3) This function will be copied directly into the script namespace.
//
__MODULE_FUNC__.__gwt_initHandlers = function(resize, beforeunload, unload) {
  var $wnd = window
  , oldOnResize = $wnd.onresize
  , oldOnBeforeUnload = $wnd.onbeforeunload
  , oldOnUnload = $wnd.onunload
  ;

  $wnd.onresize = function(evt) {
   try {
     resize();
   } finally {
     oldOnResize && oldOnResize(evt);
   }
  };
  
  $wnd.onbeforeunload = function(evt) {
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
  
  $wnd.onunload = function(evt) {
    try {
      unload();
    } finally {
      oldOnUnload && oldOnUnload(evt);
      $wnd.onresize = null;
      $wnd.onbeforeunload = null;
      $wnd.onunload = null;
    }
  };
};

__MODULE_FUNC__();
