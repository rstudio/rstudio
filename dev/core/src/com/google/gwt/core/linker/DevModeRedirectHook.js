/*
 * Copyright 2011 Google Inc.
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

// A snippet of code that loads a different script if dev mode is enabled.

// Declare our existence.
// (This assumes that properties.js has already set up the map.)
if ($wnd) {
  $wnd.__gwt_activeModules["__MODULE_NAME__"].canRedirect = true;
}

// We use a different key for each module so that we can turn on dev mode
// independently for each.
var devModeKey = '__gwtDevModeHook:__MODULE_NAME__';

// If dev mode is on, the Bookmarklet previously saved the code server's URL
// to session storage.
var devModeUrl = $wnd.sessionStorage[devModeKey];

if (devModeUrl && !$wnd[devModeKey]) {
  $wnd[devModeKey] = true; // Don't try to redirect more than once,
  var script = $doc.createElement('script');

  // save original module base
  $wnd[devModeKey + ':moduleBase'] = computeScriptBase();

  script.src = devModeUrl;
  var head = $doc.getElementsByTagName('head')[0];

  // The new script tag must come before the previous one so that
  // computeScriptBase will see it.
  head.insertBefore(script, head.firstElementChild);

  return false; // Skip the regular bootstrap.
}
