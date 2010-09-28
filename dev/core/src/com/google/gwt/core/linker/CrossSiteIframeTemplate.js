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

  /****************************************************************************
   * Internal Global Variables
   ***************************************************************************/
  // Cache symbols locally for good obfuscation
  var $wnd = window
  ,$doc = document

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

  ;

  sendStats('bootstrap', 'begin');

  /****************************************************************************
   * Internal Helper Functions
   ***************************************************************************/

  // Get the name of the filename which contains the GWT code (usually something
  // like 12BA3D5...21E.js) or devmode.js if we are in hosted mode.
  // Also sets the softPermutationId variable if appropriate.
  function getCompiledCodeFilename() {
    if (isHostedMode()) {
      return base + "__HOSTED_FILENAME__";
    }
    var strongName;
    try {
      // __PERMUTATIONS_BEGIN__
      // Permutation logic is injected here. this code populates the 
      // answers variable.
      // __PERMUTATIONS_END__
      var idx = strongName.indexOf(':');
      if (idx != -1) {
        softPermutationId = +(strongName.substring(idx + 1));
        strongName = strongName.substring(0, idx);
      }
    } catch (e) {
      // intentionally silent on property failure
    }
    return base + strongName + '.cache.js';
  }

  // Write a script tag to the element returned by getInstallLocation. We then
  // either set the content of that script tag to be the code, or set the src
  // tag if the code is actually a URL.
  function installCode(code, isUrl) {
    var docbody = getInstallLocation();

    // Inject the fetched script into the script frame.
    // The script will call onScriptInstalled.
    var script = getInstallLocationDoc().createElement('script');
    script.language='javascript';
    if (isUrl) {
      script.src = code;
    } else {
      script.text = code;
    }
    docbody.appendChild(script);

    // Remove the tags to shrink the DOM a little.
    // It should have installed its code immediately after being added.
    docbody.removeChild(script);
  }

  function isBodyLoaded() {
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
   * Internal Helper functions that have been broken out into their own .js
   * files for readability and for easy sharing between linkers.  The linker
   * code will inject these functions in these placeholders.
   ***************************************************************************/
  // Provides the getInstallLocation() function
  __INSTALL_LOCATION__

  // Provides the processMetas() function, and sets the metaProps,
  // onLoadErrorFunc and propertyErrorFunc variables
  __PROCESS_METAS__

  // Provides the computeScriptBase() function, which sets the base variable
  __COMPUTE_SCRIPT_BASE__

  // Provides the setupWaitForBodyLoad() function
  __WAIT_FOR_BODY_LOADED__

  // Provides functions used by the generated PERMUTATIONS code. 
  __PERMUTATIONS__


  /****************************************************************************
   * WRITE ME
   ***************************************************************************/
  // __PROPERTIES_BEGIN__
  // Properties logic is injected here. This code populates the values and
  // providers variables
  // __PROPERTIES_END__

  // Determines whether or not a particular property value is allowed. Called by
  // property providers.
  function __gwt_isKnownPropertyValue(propName, propValue) {
    return propValue in values[propName];
  }

  // Returns a meta property value, if any.  Used by DefaultPropertyProvider.
  function __gwt_getMetaProperty(name) {
    var value = metaProps[name];
    return (value == null) ? null : value;
  }


  /****************************************************************************
   * Exposed Functions and Variables
   ***************************************************************************/
  // Exposed for the convenience of the devmode.js and md5.js files
  __MODULE_FUNC__.__sendStats = sendStats;

  // Exposed for the call made to gwtOnLoad. Some are not figured out yet, so
  // assign them later, once the values are known.
  __MODULE_FUNC__.__moduleName = '__MODULE_NAME__';
  __MODULE_FUNC__.__errFn;
  __MODULE_FUNC__.__moduleBase;
  __MODULE_FUNC__.__softPermutationId;

  // Exposed for devmode.js
  __MODULE_FUNC__.__computePropValue = computePropValue;


  /****************************************************************************
   * Bootstrap startup code
   ***************************************************************************/

  var startDownloadImmediately = __START_DOWNLOAD_IMMEDIATELY__;
  if (isHostedMode()) {
    // since hosted.js doesn't have the necessary wrappings, we always install
    // a script tag in the iframe rather than using a giant string literal.
    // If the devmode.js file went throught the same processing as the
    // md5.js files, this could go away.
    startDownloadImmediately = false;
  }

  processMetas();
  base = computeScriptBase();
  __MODULE_FUNC__.__errFn = onLoadErrorFunc;
  __MODULE_FUNC__.__moduleBase = base;

  sendStats('bootstrap', 'selectingPermutation');
  var filename = getCompiledCodeFilename();
  __MODULE_FUNC__.__softPermutationId = softPermutationId;
  sendStats('bootstrap', 'end');

  // For now, send this dummy statistic since some people are depending on it
  // being present. TODO(unnurg): remove this statistic soon
  sendStats('loadExternalRefs', 'begin');
  sendStats('loadExternalRefs', 'end');

  sendStats('moduleStartup', 'moduleRequested');
  if (startDownloadImmediately) {
    // Set up a script tag to start downloading immediately, as well as a
    // callback to install the code once it is downloaded and the body is loaded.
    __MODULE_FUNC__.onScriptDownloaded = function(code) {
      setupWaitForBodyLoad(function() {
        installCode(code, false);
      });
    };
    if (isBodyLoaded()) {
      // if the body is loaded, then the tag to download the script can be added
      // in a non-destructive manner
      var script = document.createElement('script');
      script.src = filename;
      $doc.getElementsByTagName('head')[0].appendChild(script);
    } else {
      // if the doc has not yet loaded, go ahead and do a destructive
      // document.write since we want to immediately start the download.
      // Note that we cannot append an element to the doc if it is still loading
      // since this would cause problems in IE.
      $doc.write("<script src='" + filename + "'></scr" + "ipt>");
    }
  } else {
    // Just pass along the filename so tha a script tag can be installed in the
    // iframe to download it.  Since we will be adding the iframe to the body,
    // we still need to wait for the body to load before going forward.
    setupWaitForBodyLoad(function() {
      installCode(filename, true);
    });
  }
}
__MODULE_FUNC__();
