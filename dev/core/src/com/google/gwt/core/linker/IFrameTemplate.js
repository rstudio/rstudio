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
  ,injectedScriptsDone, gwtCodeEvaluated, bodyDone, scriptRequestCompleted, gwtFrameCreated

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

  // Error functions.  Default unset in compiled mode, may be set by meta props
  ,onLoadErrorFunc, propertyErrorFunc

  // The frame that will contain the compiled script (created in
  // maybeCreateGwtFrame())
  ,scriptFrame
  
  // Holds the compiled script retreived via XHR until the body is loaded
  ,compiledScript = ""

  ; // end of global vars

  $stats && $stats({
    moduleName: '__MODULE_NAME__',
    subSystem: 'startup',
    evtGroup: 'bootstrap', 
    millis:(new Date()).getTime(), 
    type: 'begin',
  });

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
      return (query.indexOf('gwt.hosted=') != -1 
          || ($wnd.external && $wnd.external.gwtOnLoad)) &&
          (query.indexOf('gwt.hybrid') == -1);
    } catch (e) {
      // Defensive: some versions of IE7 reportedly can throw an exception
      // evaluating "external.gwtOnLoad".
    }
    isHostedMode = function() { return result; };
    return result;
  }

  // Called by onScriptLoad(), onScriptInjectionDone(), maybeCreateGwtFrame(), 
  // and onBodyDone(). It causes the specified module to be cranked up.
  //
  var moduleStarted = false;
  function maybeStartModule() {
    if (bodyDone && injectedScriptsDone && gwtCodeEvaluated && gwtFrameCreated && !moduleStarted) {
      moduleStarted = true;

      var frameWnd = scriptFrame.contentWindow;

      // inject hosted mode property evaluation function
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
        subSystem: 'startup',
        evtGroup: 'moduleStartup',
        millis:(new Date()).getTime(),
        type: 'end',
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

  // Creates a new XMLHttpRequest object. Used by fetchCompiledScript().
  //
  function newXhr() {
    // This is the same logic as in GWT's XMLHttpRequest wrapper. The 'else'
    // case is needed primarily for old IEs.
    if (window.XMLHttpRequest) {
      return new XMLHttpRequest();
    } else {
      try {
        return new ActiveXObject('MSXML2.XMLHTTP.3.0');
      } catch (e) {
        return new ActiveXObject("Microsoft.XMLHTTP");
      }
    }
  }

  // Fetches the compiled script via XHR, saving it to 'compiledScript'
  // to be added to the page later.
  //
  function fetchCompiledScript() {
    $stats && $stats({
      moduleName:'__MODULE_NAME__', 
      subSystem:'startup', 
      evtGroup: 'moduleStartup', 
      millis:(new Date()).getTime(), 
      type: 'moduleRequested'
    });

    // Fetch the contents via XHR.
    var xhr = newXhr();
    xhr.open('GET', base + initialHtml);
    xhr.onreadystatechange = function() {
      // 4 == DONE
      if (xhr.readyState == 4) {
        compiledScript = xhr.responseText;
        xhr = null;
        scriptRequestCompleted = true;
        maybeCreateGwtFrame();
      }
    };
    xhr.send(null);
  }

  // This is gated by bodyDone because we can't add elements to the
  // page until the body is ready. It's also gated by 'scriptRequestCompleted'
  // so it will not fire until the XHR returns, in case onBodyDone happens 
  // first.
  function maybeCreateGwtFrame() {
    if (bodyDone && scriptRequestCompleted && !gwtFrameCreated) {
      // Create the script frame, making sure it's invisible, but not
      // "display:none", which keeps some browsers from running code in it.
      scriptFrame = document.createElement('iframe');
      scriptFrame.src = 'javascript:""';
      scriptFrame.id = '__MODULE_NAME__';
      scriptFrame.style.cssText = 'position:absolute; width:0; height:0; border:none';
      scriptFrame.tabIndex = -1;
      document.body.appendChild(scriptFrame);

      // Expose the module function via the iframe's window.name property
      // (this is needed for the compiled script to call back into
      //  onScriptLoad()).
      var win = scriptFrame.contentWindow;
      if (isHostedMode()) {
        win.name = '__MODULE_FUNC__';
      }

      // Set this *before* calling doc.write(), because the linux hosted-mode
      // browser sometimes doesn't properly return from doc.write() if there are
      // a large number of script blocks (even though it works fine). Go figure.
      gwtFrameCreated = true;

      // Inject the fetched script into the script frame.
      // (this script will call onScriptLoad())
      var doc = win.document;
      doc.open();
      doc.write(compiledScript);
      doc.close();
    }
  }

  // --------------- PROPERTY PROVIDERS --------------- 

// __PROPERTIES_BEGIN__
// __PROPERTIES_END__

  // --------------- EXPOSED FUNCTIONS ----------------

  // Called when the compiled script identified by moduleName is done loading.
  //
  __MODULE_FUNC__.onScriptLoad = function() {
    // Mark this module's script as done loading and (possibly) start the
    // module.
    gwtCodeEvaluated = true;
    maybeStartModule();
  }

  // Called when the script injection is complete.
  //
  __MODULE_FUNC__.onScriptInjectionDone = function() {
    // Mark this module's script injection done and (possibly) start the module.
    injectedScriptsDone = true;
    $stats && $stats({
      moduleName:'__MODULE_NAME__', 
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

  var strongName;
  var initialHtml;
  if (isHostedMode()) {
    if ($wnd.external && $wnd.external.initModule && $wnd.external.initModule('__MODULE_NAME__')) {
      // Refresh the page to update this selection script!
      $wnd.location.reload();
      return;
    }

    initialHtml = "hosted.html";
    strongName = "";
  }

  processMetas();

  // ------- SELECT PERMUTATION AND FETCH SCRIPT -------

  $stats && $stats({
    moduleName:'__MODULE_NAME__', 
    subSystem:'startup', 
    evtGroup: 'bootstrap', 
    millis:(new Date()).getTime(), 
    type: 'selectingPermutation'
  });

  if (!isHostedMode()) {
    try {
// __PERMUTATIONS_BEGIN__
      // Permutation logic
// __PERMUTATIONS_END__
      initialHtml = strongName + ".cache.html";
    } catch (e) {
      // intentionally silent on property failure
      return;
    }
  }

  // Start the request for the compiled script.
  fetchCompiledScript();

  // --------------- WINDOW ONLOAD HOOK ---------------

  var onBodyDoneTimerId;
  function onBodyDone() {
    if (!bodyDone) {
      bodyDone = true;
      maybeCreateGwtFrame();
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
      onBodyDone();
    }, false);
  }

  // Fallback. If onBodyDone() gets fired twice, it's not a big deal.
  var onBodyDoneTimerId = setInterval(function() {
    if (/loaded|complete/.test($doc.readyState)) {
      onBodyDone();
    }
  }, 50);

  $stats && $stats({
    moduleName:'__MODULE_NAME__', 
    subSystem:'startup', 
    evtGroup: 'bootstrap', 
    millis:(new Date()).getTime(), 
    type: 'end'
  });

  $stats && $stats({
    moduleName:'__MODULE_NAME__', 
    subSystem:'startup', 
    evtGroup: 'loadExternalRefs', 
    millis:(new Date()).getTime(), 
    type: 'begin'
  });

// __MODULE_SCRIPTS_BEGIN__
  // Script resources are injected here
// __MODULE_SCRIPTS_END__

  // The 'defer' attribute here is a workaround for strange IE behavior where
  // <script> tags that are doc.writ()en execute *immediately*, rather than
  // in document-order, as they should. It has no effect on other browsers.
  $doc.write('<script defer="defer">__MODULE_FUNC__.onScriptInjectionDone(\'__MODULE_NAME__\')</script>');
}

__MODULE_FUNC__();
