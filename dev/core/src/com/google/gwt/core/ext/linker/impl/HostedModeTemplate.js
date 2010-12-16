/*
 * Copyright 2008 Google Inc.
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

function __MODULE_FUNC__() {
  // ---------------- INTERNAL GLOBALS ----------------

  // Cache symbols locally for good obfuscation
  var $wnd = window
  ,$doc = document
  ,$stats = $wnd.__gwtStatsEvent ? function(a) {return $wnd.__gwtStatsEvent(a);} : null

  // These variables gate calling gwtOnLoad; all must be true to start
  ,scriptsDone, loadDone, bodyDone

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

  // Only fire the event if really Development Mode; in Production Mode the
  // compiled selection script will fire its own startup event.
  if (isHostedMode()) {
    $stats && $stats({
      moduleName: '__MODULE_NAME__',
      sessionId: $sessionId,
      subSystem: 'startup',
      evtGroup: 'bootstrap', 
      millis:(new Date()).getTime(), 
      type: 'begin'
    });
  }

  // ------------------ TRUE GLOBALS ------------------

  // Maps to synchronize the loading of styles and scripts; resources are loaded
  // only once, even when multiple modules depend on them.  This API must not
  // change across GWT versions.
  if (!$wnd.__gwt_stylesLoaded) { $wnd.__gwt_stylesLoaded = {}; }
  if (!$wnd.__gwt_scriptsLoaded) { $wnd.__gwt_scriptsLoaded = {}; }

  // --------------- INTERNAL FUNCTIONS ---------------

  function isHostedMode() {
    var result = false;
    try {
      var query = $wnd.location.search;
      return (query.indexOf('gwt.codesvr=') != -1
          || query.indexOf('gwt.hosted=') != -1 
          || ($wnd.external && $wnd.external.gwtOnLoad)) &&
          (query.indexOf('gwt.hybrid') == -1);
    } catch (e) {
      // Defensive: some versions of IE7 reportedly can throw an exception
      // evaluating "external.gwtOnLoad".
    }
    isHostedMode = function() { return result; };
    return result;
  }

  // Called by onScriptLoad(), onInjectionDone(), and onload(). It causes
  // the specified module to be cranked up.
  //
  function maybeStartModule() {
    if (scriptsDone && loadDone) {
      var iframe = $doc.getElementById('__MODULE_NAME__');
      var frameWnd = iframe.contentWindow;
      // inject Development Mode property evaluation function
      if (isHostedMode()) {
        frameWnd.__gwt_getProperty = function(name) {
          return computePropValue(name);
        };
      }
      // remove this whole function from the global namespace to allow GC
      __MODULE_FUNC__ = null;
      // JavaToJavaScriptCompiler logs onModuleLoadStart for each EntryPoint.
      frameWnd.gwtOnLoad(onLoadErrorFunc, '__MODULE_NAME__', base);
      // Record when the module EntryPoints return.
      $stats && $stats({
        moduleName: '__MODULE_NAME__',
        sessionId: $sessionId,
        subSystem: 'startup',
        evtGroup: 'moduleStartup',
        millis:(new Date()).getTime(),
        type: 'end'
      });
    }
  }

  // Determine our own script's URL via magic :)
  // This function produces one side-effect, it sets base to the module's
  // base url.
  //
  function computeScriptBase() {
    var thisScript
    ,markerId = "__gwt_marker___MODULE_NAME__"
    ,markerScript;

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

  // Deferred-binding mapper function.  Sets a value into the several-level-deep
  // answers map. The keys are specified by a non-zero-length propValArray,
  // which should be a flat array target property values. Used by the generated
  // PERMUTATIONS code.
  //
  function unflattenKeylistIntoAnswers(propValArray, value) {
    var answer = answers;
    for (var i = 0, n = propValArray.length - 1; i < n; ++i) {
      // lazy initialize an empty object for the current key if needed
      answer = answer[propValArray[i]] || (answer[propValArray[i]] = []);
    }
    // set the final one to the value
    answer[propValArray[n]] = value;
  }

  // Computes the value of a given property.  propName must be a valid property
  // name. Used by the generated PERMUTATIONS code.
  //
  function computePropValue(propName) {
    var value = providers[propName](), allowedValuesMap = values[propName];
    if (value in allowedValuesMap) {
      return value;
    }
    var allowedValuesList = [];
    for (var k in allowedValuesMap) {
      allowedValuesList[allowedValuesMap[k]] = k;
    }
    if (propertyErrorFunc) {
      propertyErrorFunc(propName, allowedValuesList, value);
    }
    throw null;
  }

  var frameInjected;
  function maybeInjectFrame() {
    if (!frameInjected) {
      frameInjected = true;
      var iframe = $doc.createElement('iframe');
      // Prevents mixed mode security in IE6/7.
      iframe.src = "javascript:''";
      iframe.id = "__MODULE_NAME__";
      iframe.style.cssText = "position:absolute;width:0;height:0;border:none";
      iframe.tabIndex = -1;
      // Due to an IE6/7 refresh quirk, this must be an appendChild.
      $doc.body.appendChild(iframe);
      
      /* 
       * The src has to be set after the iframe is attached to the DOM to avoid
       * refresh quirks in Safari.  We have to use the location.replace trick to
       * avoid FF2 refresh quirks.
       */
      $stats && $stats({
        moduleName:'__MODULE_NAME__',
        sessionId: $sessionId,
        subSystem:'startup', 
        evtGroup: 'moduleStartup', 
        millis:(new Date()).getTime(), 
        type: 'moduleRequested'
      });
      iframe.contentWindow.location.replace(base + strongName);
    }
  }

  // --------------- PROPERTY PROVIDERS --------------- 

// __PROPERTIES_BEGIN__
// __PROPERTIES_END__

  // --------------- EXPOSED FUNCTIONS ----------------

  // Called when the compiled script identified by moduleName is done loading.
  //
  __MODULE_FUNC__.onScriptLoad = function() {
    // IE7 bookmark bug. A phantom (presumably cached) version of our compiled iframe
    // can call onScriptLoad before we even properly inject the iframe. So if this is
    // called before the frame was injected ... it is completely bogus.
    if (frameInjected) {
      // Mark this module's script as done loading and (possibly) start the module.
      loadDone = true;
      maybeStartModule();
    }
  }

  // Called when the script injection is complete.
  //
  __MODULE_FUNC__.onInjectionDone = function() {
    // Mark this module's script injection done and (possibly) start the module.
    scriptsDone = true;
    $stats && $stats({
      moduleName:'__MODULE_NAME__',
      sessionId: $sessionId,
      subSystem:'startup', 
      evtGroup: 'loadExternalRefs', 
      millis:(new Date()).getTime(), 
      type: 'end'
    });
    maybeStartModule();
  }

  // --------------- STRAIGHT-LINE CODE ---------------

  // do it early for compile/browse rebasing
  computeScriptBase();

// __SHELL_SERVLET_ONLY_BEGIN__
  // Force shell servlet to serve compiled output for Production Mode
  if (!isHostedMode()) {
    $doc.write('<script src="' + base + '__MODULE_NAME__.nocache.js?compiled"></script>');
    return;
  }

  // Default shell servlet load error function
  //
  onLoadErrorFunc = function() {
    alert('Failed to load module __MODULE_NAME__' +
      '".\nPlease see the log in the development shell for details.');
  };

  // Default shell servlet property error function
  //
  propertyErrorFunc = function(propName, allowedValues, badValue) {
    var msg = 'While attempting to load module __MODULE_NAME__, property \"'
      + propName;
    if (badValue != null) {
      msg += '\" was set to the unexpected value \"' + badValue + '\"';
    } else {
      msg += '\" was not specified';
    }
    msg += 'Allowed values: ' + allowedValues;
    alert(msg);
  };

// __SHELL_SERVLET_ONLY_END__

  processMetas();

  // --------------- WINDOW ONLOAD HOOK ---------------

  $stats && $stats({
    moduleName:'__MODULE_NAME__',
    sessionId: $sessionId,
    subSystem:'startup', 
    evtGroup: 'bootstrap', 
    millis:(new Date()).getTime(), 
    type: 'selectingPermutation'
  });

  var strongName;
  if (isHostedMode()) {
    strongName = "hosted.html?__MODULE_FUNC__";
    // Hang an expando for hosted.html to be able to grab the module name early.
    __MODULE_FUNC__.moduleName = '__MODULE_NAME__';
  } else {
    try {
// __PERMUTATIONS_BEGIN__
      // Permutation logic
// __PERMUTATIONS_END__
    } catch (e) {
      // intentionally silent on property failure
      return;
    }
  }

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
    $doc.addEventListener("DOMContentLoaded", function() {
      maybeInjectFrame();
      onBodyDone();
    }, false);
  }

  // Fallback. If onBodyDone() gets fired twice, it's not a big deal.
  var onBodyDoneTimerId = setInterval(function() {
    if (/loaded|complete/.test($doc.readyState)) {
      maybeInjectFrame();
      onBodyDone();
    }
  }, 50);

  $stats && $stats({
    moduleName:'__MODULE_NAME__',
    sessionId: $sessionId,
    subSystem:'startup', 
    evtGroup: 'bootstrap', 
    millis:(new Date()).getTime(), 
    type: 'end'
  });

  $stats && $stats({
    moduleName:'__MODULE_NAME__',
    sessionId: $sessionId,
    subSystem:'startup',
    evtGroup: 'loadExternalRefs',
    millis:(new Date()).getTime(),
    type: 'begin'
  });

// __MODULE_SCRIPTS_BEGIN__
  // Script resources are injected here
// __MODULE_SCRIPTS_END__

  $doc.write('<script defer="defer">__MODULE_FUNC__.onInjectionDone(\'__MODULE_NAME__\')</script>');
}

__MODULE_FUNC__();
