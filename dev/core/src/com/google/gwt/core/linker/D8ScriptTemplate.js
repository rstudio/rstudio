/*
 * Copyright 2013 Google Inc.
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


// ---------------- D8 GLOBALS ----------------
window = this;
// Alias self as well since it is a magic var sometimes provided
// by the webworker linker.
self = window;
document = {};
window.document = document;
window.__d8warning = function(funcName){
  print("your code is calling " + funcName + " which does not exist in d8");
};
window.setTimeout = function() { window.__d8warning("setTimeout")};
window.clearTimeout = function() { window.__d8warning("clearTimeout")};
window.clearInterval = function() { window.__d8warning("clearInterval");};
window.setInterval = function() { window.__d8warning("setInterval"); };
navigator = {};
navigator.userAgent = {};
navigator.userAgent.toLowerCase = function(){ return "webkit";};
console = {};
console.log = function(msg){print(msg);};
window.console = console;
$stats= function(){};
$sessionId = "";

function __MODULE_FUNC__() {}
// ---------------- BOOT GLOBALS ----------------

// Cache symbols locally for good obfuscation
var $wnd = window
,$doc = document
// If non-empty, an alternate base url for this module
,base = ''

// Provides the module with the soft permutation id
,softPermutationId = 0

; // end of global vars

// ------------------ TRUE GLOBALS ------------------

// Maps to synchronize the loading of styles and scripts; resources are loaded
// only once, even when multiple modules depend on them.  This API must not
// change across GWT versions.
if (!$wnd.__gwt_stylesLoaded) { $wnd.__gwt_stylesLoaded = {}; }
if (!$wnd.__gwt_scriptsLoaded) { $wnd.__gwt_scriptsLoaded = {}; }

// --------------- EXPOSED FUNCTIONS ----------------

// Called when the compiled script identified by moduleName is done loading.
//
__MODULE_FUNC__.onScriptLoad = function(gwtOnLoadFunc) {
  // remove this whole function from the global namespace to allow GC
  __MODULE_FUNC__ = null;
  gwtOnLoadFunc(null, '__MODULE_NAME__', base, softPermutationId);
}
var strongName;
// __PERMUTATIONS_BEGIN__
  // Permutation logic
// __PERMUTATIONS_END__
var idx = strongName.indexOf(':');
if (idx != -1) {
  softPermutationId = Number(strongName.substring(idx + 1));
}
// __MODULE_SCRIPTS_BEGIN__
  // Script resources are injected here
// __MODULE_SCRIPTS_END__
