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
 * @fileoverview Stub for non-compiled modules.
 *
 * This script forces the proper reload + compilation in Super Dev Mode.
 */

(function() {
  var moduleName = '__MODULE_NAME__';  // Replaced by actual module name.

  // Active Super Dev Mode is assumed.
  var key = '__gwtDevModeHook:' + moduleName;
  if (!window.sessionStorage[key]) {
    alert('Unable to load Super Dev Mode version of ' + moduleName + '.');
    return;
  }
  var scriptLocation = window.sessionStorage[key];

  // Get the Super Dev Mode Server URL: use the HTML a.href parsing.
  var a = document.createElement('a');
  a.href = scriptLocation;
  var devServerUrl = a.protocol + '//' + a.host;

  // Load the bookmarklet.
  window.__gwt_bookmarklet_params = {
    'server_url' : devServerUrl + '/',
    'module_name': moduleName
  };
  var script = document.createElement('script');
  script.src = devServerUrl + '/dev_mode_on.js';
  document.getElementsByTagName('head')[0].appendChild(script);
})();
