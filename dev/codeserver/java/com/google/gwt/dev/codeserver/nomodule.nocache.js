/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

/**
 * @fileoverview A stub used in place of nocache.js files for modules that haven't been
 * compiled yet.
 *
 * When the stub is loaded, it finds the appropriate code server and requests that it should
 * recompile one module and reload the page.
 *
 * <p>It assumes the following variables are predefined:
 * <ul>
 *   <li>moduleName is the name of the module to compile. (Output module name.)
 *   <li>providers is a map from binding properties (strings) to no-argument functions that
 *   compute the value of the binding variable for that property (string).
 *   <li>values is a map from a binding property name (string) to the set of possible
 *   values for that binding property (represented as a map with string keys).
 * </ul>
 *
 * <p>This script should be wrapped in a function so that the variables defined here aren't global.
 */

// These variables are used by property providers.
// (We also use them below.)
var $wnd = window;
var $doc = document;

/**
 * The URL used previously to load this module or null if Super Dev Mode is not on for this module.
 */
var previousUrl = $wnd.sessionStorage.getItem('__gwtDevModeHook:' + moduleName);

/**
 * The URL of the front page of the code server to use to compile the module.
 */
var codeServerUrl = (function () {

  /**
   * Converts a relative URL to absolute and returns the front page (ending with a slash).
   */
  function getFrontPage(url) {
    var a = $doc.createElement('a');
    a.href = url;
    return a.protocol + '//' + a.host + '/';
  }

  /**
   * Returns true if string 's' has the given suffix.
   */
  function hasSuffix(s, suffix) {
    var startPos =  s.length - suffix.length;
    return s.indexOf(suffix, startPos) == startPos;
  }

  /**
   * Returns true if the given URL ends with {moduleName}.nocache.js,
   * ignoring any query string or hash.
   */
  function isModuleNoCacheJs(url) {
    // Remove trailing query string and/or fragment
    url = url.split("?")[0].split("#")[0];
    return hasSuffix(url, '/' + moduleName + '.nocache.js');
  }

  // If Super Dev Mode is already running for this module, use the same code server again.
  if (previousUrl) {
    return getFrontPage(previousUrl);
  }

  // Since Super Dev Mode is not on, an HTML page somewhere is directly including
  // this nocache.js file from the code server.
  //
  // Some people run codeserver behind a proxy, for example to enable HTTPS support.
  // So, we should figure out this script's URL from the client's point of view,
  // similar to how computeScriptBase() does it but simpler. We do this by searching
  // all script tags for the URL.
  var scriptTagsToSearch = $doc.getElementsByTagName('script');
  for (var i = 0; ; i++) {
    var tag = scriptTagsToSearch[i];
    if (!tag) {
      break; // end of list; not found
    }
    if (tag && isModuleNoCacheJs(tag.src)) {
      return getFrontPage(tag.src);
    }
  }

  throw "unable to find the script tag that includes " + moduleName + ".nocache.js";
}());

var activeModules = $wnd.__gwt_activeModules = $wnd.__gwt_activeModules || {};
if (!activeModules[moduleName]) {
  // dev_mode_on.js checks for this.
  activeModules[moduleName] = {canRedirect: true};
}

/**
 * A function returning a map from binding property names to values.
 */
var getPropMap = (function() {

  var module = activeModules[moduleName];
  if (module.bindings) {
    // The module is already running, so use the same bindings.
    // (In this case, a dev mode hook probably redirected to this script.)
    return module.bindings;
  }

  /**
   * Returns the value of the given binding property name.
   */
  function computePropValue(propName) {
    var val = providers[propName]();
    // sanity check
    var allowedValuesMap = values[propName];
    if (val in allowedValuesMap) {
      return val;
    } else {
      console.log("provider for " + propName + " returned unexpected value: '" + val + "'");
      throw "can't compute binding property value for " + propName;
    }
  }

  return function () {
    var result = {};
    for (var key in values) {
      if (values.hasOwnProperty(key)) {
        result[key] = computePropValue(key);
      }
    }
    return result;
  };
}());

// Load the bookmarklet.
window.__gwt_bookmarklet_params = {
  'server_url' : codeServerUrl,
  'module_name': moduleName,
  'getPropMap': getPropMap
};
var script = document.createElement('script');
script.src = codeServerUrl + 'dev_mode_on.js';
document.getElementsByTagName('head')[0].appendChild(script);
