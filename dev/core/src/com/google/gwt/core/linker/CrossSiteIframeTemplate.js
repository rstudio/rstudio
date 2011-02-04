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
function __MODULE_FUNC__() {
  var $wnd = window;
  var $doc = document;
  sendStats('bootstrap', 'begin');

  /****************************************************************************
   * Internal Helper Functions
   ***************************************************************************/

  function isBodyLoaded() {
    if (typeof $doc.readyState == "undefined") {
      // FF 3.5 and below does not have readyState, but it does allow us to
      // append to the body before it has finished loading, so we return whether
      // the body element exists.
      return (typeof $doc.body != "undefined" && $doc.body != null);
    }
    return (/loaded|complete/.test($doc.readyState));
  }

  function isHostedMode() {
    var query = $wnd.location.search;
    return (query.indexOf('gwt.codesvr=') != -1);
  }

  // Helper function to send statistics to the __gwtStatsEvent function if it
  // exists.
  function sendStats(evtGroupString, typeString) {
    if ($wnd.__gwtStatsEvent) {
      $wnd.__gwtStatsEvent({
        moduleName: '__MODULE_NAME__',
        sessionId: $wnd.__gwtStatsSessionId,
        subSystem: 'startup',
        evtGroup: evtGroupString,
        millis:(new Date()).getTime(),
        type: typeString,
      });
    }
  }


  /****************************************************************************
   * Exposed Functions and Variables
   ***************************************************************************/
  // These are set by various parts of the bootstrapping code, but they always
  // need to exist, so give them all default values here.
  
  // Exposed for the convenience of the devmode.js and md5.js files
  __MODULE_FUNC__.__sendStats = sendStats;

  // Exposed for the call made to gwtOnLoad. Some are not figured out yet, so
  // assign them later, once the values are known.
  __MODULE_FUNC__.__moduleName = '__MODULE_NAME__';
  __MODULE_FUNC__.__errFn = null;
  __MODULE_FUNC__.__moduleBase = 'DUMMY';
  __MODULE_FUNC__.__softPermutationId = 0;

  // Exposed for devmode.js
  __MODULE_FUNC__.__computePropValue = null;

  // Exposed for property provider code
  var __gwt_isKnownPropertyValue = function() { return false; };
  var __gwt_getMetaProperty = function() { return null; };

  // Exposed for permutations code
  __propertyErrorFunction = null;


  /****************************************************************************
   * Internal Helper functions that have been broken out into their own .js
   * files for readability and for easy sharing between linkers.  The linker
   * code will inject these functions in these placeholders.
   ***************************************************************************/
  // Provides the installScript() function.
  __INSTALL_SCRIPT__

  // Provides the processMetas() function which sets the __gwt_getMetaProperty
  // __propertyErrorFunction and __MODULE_FUNC__.__errFn variables if needed
  __PROCESS_METAS__

  // Provides the computeScriptBase() function
  __COMPUTE_SCRIPT_BASE__

  // Provides the getCompiledCodeFilename() function which sets the 
  // __gwt_isKnownPropertyValue, MODULE_FUNC__.__computePropValue and
  // __MODULE_FUNC__.__softPermutationId variables if needed
  __PERMUTATIONS__

  // Provides the loadExternalStylesheets() function
  __LOAD_STYLESHEETS__

  /****************************************************************************
   * Bootstrap startup code
   ***************************************************************************/
  // Must be called before computeScriptBase() and getCompiledFilename()
  processMetas();

  // Must be set before getCompiledFilename() is called
  __MODULE_FUNC__.__moduleBase = computeScriptBase();

  // Must be done right before the "bootstrap" "end" stat is sent
  var filename = getCompiledCodeFilename();

  loadExternalStylesheets();

  sendStats('bootstrap', 'end');

  installScript(filename);

}
__MODULE_FUNC__();
