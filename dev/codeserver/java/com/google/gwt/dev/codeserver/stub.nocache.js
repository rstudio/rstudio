/*
 * Copyright 2014 Google Inc.
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

/**
 * This startup script is used when we run superdevmode from an app server.
 *
 * The main goal is to avoid installing bookmarklets for host:port/module
 * to load and recompile the application.
 */
(function($wnd, $doc){
  // Don't support browsers without session storage: IE6/7
  var badBrowser = 'Unable to load Super Dev Mode of "__MODULE_NAME__" because\n';
  if (!('sessionStorage' in $wnd)) {
    $wnd.alert(badBrowser +  'this browser does not support "sessionStorage".');
    return;
  }

  //We don't import properties.js so we have to update active modules here
  $wnd.__gwt_activeModules = $wnd.__gwt_activeModules || {};
  $wnd.__gwt_activeModules['__MODULE_NAME__'] = {
    'moduleName' : '__MODULE_NAME__',
    'bindings' : function() {
      return {};
    }
  };

  // Reuse compute script base
  __COMPUTE_SCRIPT_BASE__;

  // document.head does not exist in IE8
  var $head = $doc.head || $doc.getElementsByTagName('head')[0];

  // Quick way to compute the user.agent, it works almost the same than
  // UserAgentPropertyGenerator, but we cannot reuse it without depending
  // on gwt-user.jar.
  // This reduces compilation time since we only compile for one ua.
  var ua = $wnd.navigator.userAgent.toLowerCase();
  var docMode = $doc.documentMode || 0;
  ua = /webkit/.test(ua)? 'safari' : /gecko/.test(ua) || docMode > 10 ? 'gecko1_8' :
       /msie/.test(ua) && docMode > 7 ? 'ie' + docMode : '';
  if (!ua && docMode) {
    $wnd.alert(badBrowser +  'your browser is running "Compatibility View" for IE' + docMode + '.');
    return;
  }

  // We use a different key for each module so that we can turn on dev mode
  // independently for each.
  var devModeHookKey = '__gwtDevModeHook:__MODULE_NAME__';
  var devModeSessionKey = '__gwtDevModeSession:__MODULE_NAME__';

  // Compute some codeserver urls so as the user does not need bookmarklets
  var hostName = $wnd.location.hostname;
  var serverUrl = 'http://' + hostName + ':__SUPERDEV_PORT__';
  var nocacheUrl = serverUrl + '/__MODULE_NAME__/__MODULE_NAME__.nocache.js';

  // Save supder-devmode url in session
  $wnd.sessionStorage[devModeHookKey] = nocacheUrl;
  // Save user.agent in session
  $wnd.sessionStorage[devModeSessionKey] = 'user.agent=' + ua + '&';

  // Set bookmarklet params in window
  $wnd.__gwt_bookmarklet_params = {'server_url': serverUrl};
  // Save the original module base. (Returned by GWT.getModuleBaseURL.)
  $wnd[devModeHookKey + ':moduleBase'] = computeScriptBase();

  // Needed in the real nocache.js logic
  $wnd.__gwt_activeModules['__MODULE_NAME__'].canRedirect = true;
  $wnd.__gwt_activeModules['__MODULE_NAME__'].superdevmode = true;

  // Insert the superdevmode nocache script in the first position of the head
  var devModeScript = $doc.createElement('script');
  devModeScript.src = nocacheUrl;

  // Show a div in a corner for adding buttons to recompile the app.
  // We reuse the same div in all modules of this page for stacking buttons
  // and to make it available in jsni.
  // The user can remove this: .gwt-DevModeRefresh {display:none}
  $wnd.__gwt_compileElem = $wnd.__gwt_compileElem || $doc.createElement('div');
  $wnd.__gwt_compileElem.className = 'gwt-DevModeRefresh';

  // Create the compile button for this module
  var compileButton = $doc.createElement('div');
  $wnd.__gwt_compileElem.appendChild(compileButton);
  // Number of modules present in the window
  var moduleIdx = $wnd.__gwt_compileElem.childNodes.length;
  // Each button has a class with its index number
  var buttonClassName = 'gwt-DevModeCompile gwt-DevModeModule-' + moduleIdx;
  compileButton.className = buttonClassName;
  // The status message container
  compileButton.innerHTML = '<div></div>';
  // User knows who module to compile, hovering the button
  compileButton.title = 'Compile module:\n__MODULE_NAME__';

  // Use CSS so the app could change button style
  var compileStyle = $doc.createElement('style');
  compileStyle.language = 'text/css';
  $head.appendChild(compileStyle);
  var css =
    ".gwt-DevModeRefresh{" +
      "position:fixed;" +
      "right:3px;" +
      "bottom:3px;" +
      "font-family:arial;" +
      "font-size:1.8em;" +
      "cursor:pointer;" +
      "color:#B62323;" +
      "text-shadow:grey 1px 1px 3px;" +
      "z-index:2147483646;" +
      "white-space:nowrap;" +
    "}" +
    ".gwt-DevModeCompile{" +
      "position:relative;" +
      "float:left;" +
      "width:1em;" +
    "}" +
    ".gwt-DevModeCompile div{" +
      "position:absolute;" +
      "right:1em;" +
      "bottom:-3px;" +
      "font-size:0.3em;" +
      "opacity:1;" +
      "direction:rtl;" +
    "}" +
    ".gwt-DevModeCompile:before{" +
      "content:'\u21bb';" +
    "}" +
    ".gwt-DevModeCompiling:before{" +
      // IE8 fails when setting content here
      "opacity:0.1;" +
    "}" +
    ".gwt-DevModeCompile div:before{" +
      "content:'GWT';" +
    "}" +
    ".gwt-DevModeError div:before{" +
      "content:'FAILED';" +
    "}";
  // Only insert common css the first time
  css = (moduleIdx == 1 ? css : '') +
    ".gwt-DevModeModule-" + moduleIdx + ".gwt-DevModeCompiling div:before{" +
      "content:'COMPILING __MODULE_NAME__';" +
      "font-size:24px;" +
      "color:#d2d9ee;" +
    "}";
  if ('styleSheet' in compileStyle) {
    // IE8
    compileStyle.styleSheet.cssText = css;
  } else {
    compileStyle.appendChild($doc.createTextNode(css));
  }

  // Set a different compile function name per module
  var compileFunction = '__gwt_compile_' + moduleIdx;

  compileButton.onclick = function() {
    $wnd[compileFunction]();
  };

  // defer so as the body is ready
  setTimeout(function(){
    $head.insertBefore(devModeScript, $head.firstElementChild || $head.children[0]);
    $doc.body.appendChild($wnd.__gwt_compileElem);
  }, 1);

  // Flag to avoid compiling in parallel.
  var compiling = false;
  // Compile function available in window so as it can be run from jsni.
  // TODO(manolo): make Super Dev Mode script set this function in __gwt_activeModules
  $wnd[compileFunction] = function() {
    if (compiling) {
      return;
    }
    compiling = true;

    // Compute an unique name for each callback to avoid cache issues
    // in IE, and to avoid the same function being called twice.
    var callback = '__gwt_compile_callback_' + moduleIdx + '_' + new Date().getTime();
    $wnd[callback] = function(r) {
      if (r && r.status && r.status == 'ok') {
        $wnd.location.reload();
      }
      compileButton.className = buttonClassName + ' gwt-DevModeError';
      delete $wnd[callback];
      compiling = false;
    };

    // Insert the jsonp script to compile the current module
    // TODO(manolo): we don't have a way to detect when the server is unreachable,
    // maybe a request returning status='idle'
    var compileScript = $doc.createElement('script');
    compileScript.src = serverUrl +
      '/recompile/__MODULE_NAME__?user.agent=' + ua + '&_callback=' + callback;
    $head.appendChild(compileScript);
    compileButton.className = buttonClassName  + ' gwt-DevModeCompiling';
  }

  // Run this block after the app has been loaded.
  setTimeout(function(){
    // Maintaining the hook key in session can cause problems
    // if we try to run classic code server so we remove it
    // after a while.
    $wnd.sessionStorage.removeItem(devModeHookKey);

    // Re-attach compile button because sometimes app clears the dom
    $doc.body.appendChild($wnd.__gwt_compileElem);
  }, 2000);
})(window, document);
