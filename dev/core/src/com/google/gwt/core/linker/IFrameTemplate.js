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
  ,$sessionId = $wnd.__gwtStatsSessionId ? $wnd.__gwtStatsSessionId : null

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

  // Provides the module with the soft permutation id
  ,softPermutationId = 0

  // Error functions.  Default unset in compiled mode, may be set by meta props.
  ,onLoadErrorFunc, propertyErrorFunc

  ; // end of global vars

  $stats && $stats({
    moduleName: '__MODULE_NAME__',
    sessionId: $sessionId,
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
      // inject hosted mode property evaluation function
      if (isHostedMode()) {
        frameWnd.__gwt_getProperty = function(name) {
          return computePropValue(name);
        };
      }
      // remove this whole function from the global namespace to allow GC
      __MODULE_FUNC__ = null;
      // JavaToJavaScriptCompiler logs onModuleLoadStart for each EntryPoint.
      frameWnd.gwtOnLoad(onLoadErrorFunc, '__MODULE_NAME__', base, softPermutationId);
      // Record when the module EntryPoints return.
      $stats && $stats({
        moduleName: '__MODULE_NAME__',
        sessionId: $sessionId,
        subSystem: 'startup',
        evtGroup: 'moduleStartup',
        millis:(new Date()).getTime(),
        type: 'end',
      });
    }
  }

  __COMPUTE_SCRIPT_BASE__
  __PROCESS_METAS__

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
      iframe.contentWindow.location.replace(base + initialHtml);
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
  processMetas();
  computeScriptBase();

  var strongName;
  var initialHtml;
  if (isHostedMode()) {
    if ($wnd.external && $wnd.external.initModule && $wnd.external.initModule('__MODULE_NAME__')) {
      // Refresh the page to update this selection script!
      $wnd.location.reload();
      return;
    }
    initialHtml = "__HOSTED_FILENAME__?__MODULE_FUNC__";
    strongName = "";
  }


  // --------------- WINDOW ONLOAD HOOK ---------------

  $stats && $stats({
    moduleName:'__MODULE_NAME__',
    sessionId: $sessionId,
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
      var idx = strongName.indexOf(':');
      if (idx != -1) {
        softPermutationId = Number(strongName.substring(idx + 1));
        strongName = strongName.substring(0, idx);
      }
      initialHtml = strongName + ".cache.html";
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

  // The 'defer' attribute here is a workaround for strange IE behavior where
  // <script> tags that are doc.writ()en execute *immediately*, rather than
  // in document-order, as they should. It has no effect on other browsers.
  $doc.write('<script defer="defer">__MODULE_FUNC__.onInjectionDone(\'__MODULE_NAME__\')</script>');
}

__MODULE_FUNC__();
