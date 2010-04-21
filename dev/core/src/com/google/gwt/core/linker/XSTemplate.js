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
  
  // Called by onScriptLoad() and onload(). It causes
  // the specified module to be cranked up.
  //
  function maybeStartModule() {
    // TODO: it may not be necessary to check gwtOnLoad here.
    if (gwtOnLoad && bodyDone) {
      gwtOnLoad(onLoadErrorFunc, '__MODULE_NAME__', base, softPermutationId);
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

  // --------------- PROPERTY PROVIDERS --------------- 

// __PROPERTIES_BEGIN__
// __PROPERTIES_END__

  // --------------- EXPOSED FUNCTIONS ----------------

  // Called when the compiled script identified by moduleName is done loading.
  //
  __MODULE_FUNC__.onScriptLoad = function(gwtOnLoadFunc) {
    // remove the callback to prevent it being called twice
    __MODULE_FUNC__.onScriptLoad = null;
    gwtOnLoad = gwtOnLoadFunc;
    maybeStartModule();
  }

  // --------------- STRAIGHT-LINE CODE ---------------

  if (isHostedMode()) {
    alert("Cross-site hosted mode not yet implemented. See issue " +
      "http://code.google.com/p/google-web-toolkit/issues/detail?id=2079");
    return;
  }

  // do it early for compile/browse rebasing
  processMetas();
  computeScriptBase();

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
  try {
// __PERMUTATIONS_BEGIN__
    // Permutation logic
// __PERMUTATIONS_END__
    var idx = strongName.indexOf(':');
    if (idx != -1) {
      softPermutationId = Number(strongName.substring(idx + 1));
      strongName = strongName.substring(0, idx);
    }
  } catch (e) {
    // intentionally silent on property failure
    return;
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

  // This is a bit ugly, but serves a purpose. We need to ensure that the stats
  // script runs before the compiled script. If they are both doc.write()n in
  // sequence, that should be the effect. Except on IE it turns out that a
  // script injected via doc.write() can execute immediately! Adding 'defer'
  // attributes to both seemed to fix this, but caused startup problems for
  // some apps. The final solution was simply to inject the compiled script
  // from *within* the stats script, guaranteeing order at the expense of near
  // total inscrutability :(
  var compiledScriptTag = '"<script src=\\"' + base + strongName + '.cache.js\\"></scr" + "ipt>"';
  $doc.write('<scr' + 'ipt><!-' + '-\n'
    + 'window.__gwtStatsEvent && window.__gwtStatsEvent({'
    + 'moduleName:"__MODULE_NAME__", sessionId:window.__gwtStatsSessionId, subSystem:"startup",'
    + 'evtGroup: "loadExternalRefs", millis:(new Date()).getTime(),'
    + 'type: "end"});'
    + 'window.__gwtStatsEvent && window.__gwtStatsEvent({'
    + 'moduleName:"__MODULE_NAME__", sessionId:window.__gwtStatsSessionId, subSystem:"startup",'
    + 'evtGroup: "moduleStartup", millis:(new Date()).getTime(),'
    + 'type: "moduleRequested"});'
    + 'document.write(' + compiledScriptTag + ');'
    + '\n-' + '-></scr' + 'ipt>');
}

__MODULE_FUNC__();
