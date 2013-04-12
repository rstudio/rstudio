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

// A snippet of code that loads a different script if Super Dev Mode is enabled.
if ($wnd) {
  var devModePermitted = !!(__DEV_MODE_REDIRECT_HOOK_PERMITTED__);

  // Declare our existence.
  // (This assumes that properties.js has already set up the map.)
  $wnd.__gwt_activeModules["__MODULE_NAME__"].canRedirect = devModePermitted;

  if (devModePermitted) {
    // We use a different key for each module so that we can turn on dev mode
    // independently for each.
    var devModeKey = '__gwtDevModeHook:__MODULE_NAME__';

    // If dev mode is on, the bookmarklet previously saved the code server's URL
    // to session storage.
    var devModeUrl = $wnd.sessionStorage[devModeKey];

    if (devModeUrl && !$wnd[devModeKey]) {
      $wnd[devModeKey] = true; // Don't try to redirect more than once,

      // Save the original module base. (Returned by GWT.getModuleBaseURL.)
      $wnd[devModeKey + ':moduleBase'] = computeScriptBase();

      var devModeScript = $doc.createElement('script');
      devModeScript.src = devModeUrl;

      // The new script tag must come before the previous one so that
      // computeScriptBase will see it.
      var head = $doc.getElementsByTagName('head')[0];
      head.insertBefore(devModeScript, head.firstElementChild || head.children[0]);

      return false; // Skip the regular bootstrap.
    }
  }
}
