/*
 * Copyright 2011 Google Inc.
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
 * @fileoverview The implementation of the "Dev Mode On" and "Compile"
 * bookmarklets.
 *
 * The "Dev Mode On" bookmarklet displays a dialog at the top of the page
 * allowing the user to choose a GWT app to recompile. This dialog
 * contains a "Compile" bookmarklet for each available module.
 *
 * The "Compile" bookmarklet tells the code server to recompile a GWT app,
 * and if successful, reloads the current page with Super Dev Mode enabled.
 *
 * The bookmarklets themselves are just bootstrap scripts that set some
 * parameters on window and include this script. That way, users don't have
 * to delete and replace their bookmarklets when we change this code.
 */
(function() {

  // Set up globals needed for JSONP calls. (These persist between bookmarklet
  // calls, in case the user clicks the bookmarklet more than once.)
  var globals = window.__gwt_bookmarklet_globals;
  if (typeof globals == 'undefined') {
    globals = {
      callback_counter: 0,
      callbacks: {},
      dismissModuleDialog: function() {}
    };
    window.__gwt_bookmarklet_globals = globals;
  }

  /**
   * Deletes and returns the parameters passed in by the "Dev Mode On" or
   * "Compile" bookmarklet. We expect the bookmarklet to pass the following
   * parameters:
   * <ul>
   *   <li>server_url The top-level URL of the code server, including the
   *       trailing slash.
   *   <li>module_name (optional) The name of the module to compile. (Skips
   *   the module dialog if the module is available.)
   * </ul>
   * @return {Object} The parameters, or null if not found.
   */
  function getBookmarkletParams() {
    var params = window.__gwt_bookmarklet_params;
    if (!params) {
      return null;
    }
    delete window.__gwt_bookmarklet_params;
    return params;
  }

  function makeOverlay() {
    var overlay = document.createElement('div');
    overlay.style.zIndex = 1000;
    overlay.style.position = 'absolute';
    overlay.style.top = 0;
    overlay.style.left = 0;
    overlay.style.bottom = 0;
    overlay.style.right = 0;
    overlay.style.backgroundColor = '#000';
    overlay.style.opacity = '0.5';
    return overlay;
  }

  function makeDialog() {
    var dialog = document.createElement('div');
    dialog.style.zIndex = 1001;
    dialog.style.position = 'fixed';
    dialog.style.top = '20pt';
    dialog.style.left = '20pt';
    dialog.style.backgroundColor = 'white';
    dialog.style.border = '4px solid #ccc';
    dialog.style.padding = '1em';
    dialog.style.borderRadius = '5px';
    return dialog;
  }

  function makeBookmarklet(name, javascript) {
    var result = document.createElement('a');
    result.style.fontSize = '12pt';
    result.style.color = '#000';
    result.style.textDecoration = 'none';
    result.style.backgroundColor = '#ddd';
    result.style.marginLeft = '1em';
    result.style.borderBottom = '1px solid black';
    result.style.padding = '3pt';
    result.setAttribute('href', 'javascript:' + encodeURIComponent(javascript));
    result.textContent = name;
    result.title = 'Tip: drag this button to the bookmark bar';
    return result;
  }

  function makeCompileBookmarklet(codeserver_url, module_name) {
    var bookmarklets_js = codeserver_url + 'dev_mode_on.js';
    var javascript = '{ window.__gwt_bookmarklet_params = {'
        + 'server_url:\'' + codeserver_url + '\','
        + 'module_name:\'' + module_name + '\'};'
        + ' var s = document.createElement(\'script\');'
        + ' s.src = \'' + bookmarklets_js + '\';'
        + ' void(document.getElementsByTagName(\'head\')[0].appendChild(s));}';
    return makeBookmarklet('Compile', javascript);
  }

  /**
   * Determines whether we can recompile a module and see the results. If not,
   * explains why not.
   * @param module_name {string}
   * @return {string} The error message, or null if there is no error and
   *     a recompile will succeed.
   */
  function getCannotCompileError(module_name) {
    var modules_on_codeserver = window.__gwt_codeserver_config.moduleNames;
    if (modules_on_codeserver.indexOf(module_name) < 0) {
      return 'The code server isn\'t configured to compile this module';
    }

    var modules_on_page = window.__gwt_activeModules;
    if (!modules_on_page || !(module_name in modules_on_page)) {
      return 'The current page doesn\'t have this module';
    }

    var mod = modules_on_page[module_name];

    var dev_mode_key = '__gwtDevModeHook:' + module_name;
    var dev_mode_on = mod['superdevmode'] ||
        window.sessionStorage[dev_mode_key];

    if (!dev_mode_on && !mod.canRedirect) {
      return 'This module doesn\'t have Super Dev Mode enabled';
    }

    // looks okay
    return null;
  }

  /**
   * Displays the "Choose module to compile" dialog.
   * @param codeserver_url {string} The URL of the code server that will
   *    compile the chosen module.
   */
  function showModuleDialog(codeserver_url) {

    function makeHeader() {
      var message = document.createElement('div');
      message.style.fontSize = '24pt';
      message.textContent = 'Choose a module to recompile:';
      return message;
    }

    function makeModuleItem(mod) {
      var module_name = mod.moduleName;

      var result = document.createElement('li');
      result.style.fontSize = '14pt';
      result.appendChild(document.createTextNode(module_name));

      var error = getCannotCompileError(module_name);
      if (error) {
        result.style.color = 'gray';
        result.title = error;
        return result;
      }

      var button = makeCompileBookmarklet(codeserver_url, module_name);
      button.className = 'module_' + module_name;
      result.appendChild(document.createTextNode(' '));
      result.appendChild(button);
      return result;
    }

    function makeCodeServerLink() {
      var link = document.createElement('a');
      link.style.fontSize = '10pt';
      link.style.marginTop = '10px';
      link.setAttribute('href', codeserver_url);
      link.setAttribute('target', '_blank');
      link.appendChild(document.createTextNode('code server'));
      var div = document.createElement('div');
      div.appendChild(link);
      return div;
    }

    var active_modules = window.__gwt_activeModules;

    var moduleList = document.createElement('ol');
    for (var module_name in active_modules) {
      moduleList.appendChild(makeModuleItem(active_modules[module_name]));
    }

    // Assemble the dialog.
    var dialog = makeDialog();
    if (moduleList.hasChildNodes()) {
      dialog.appendChild(makeHeader());
      dialog.appendChild(moduleList);
    } else {
      dialog.appendChild(document.createTextNode(
          'Can\'t find any GWT Modules on this page.'));
    }
    dialog.appendChild(makeCodeServerLink());

    // Grey out everything under the dialog.
    var overlay = makeOverlay();

    var body = document.getElementsByTagName('body')[0];
    body.appendChild(overlay);
    body.appendChild(dialog);

    globals.dismissModuleDialog = function() {
      body.removeChild(dialog);
      body.removeChild(overlay);
      globals.dismissModuleDialog = function() {}; // uninstall
    };

    // Clicking outside the module dialog dismisses it.
    overlay.onclick = function() {
      globals.dismissModuleDialog();
    };
  }

  /**
   * Displays the "Compiling..." dialog.
   * @param {string} text A line of text to display.
   * @return {Object} An object representing the dialog. It
   *     has one method, showError.
   */
  function showCompilingDialog(text) {
    // Grey out everything under the dialog.
    var overlay = makeOverlay();
    var dialog = makeDialog();

    var message = document.createElement('div');
    message.style.fontSize = '24pt';
    message.textContent = text;

    dialog.appendChild(message);

    var body = document.getElementsByTagName('body')[0];
    body.appendChild(overlay);
    body.appendChild(dialog);

    var result = {};

    /**
     * Updates the dialog with an error message.
     * @param {string} errorText The text to display in red.
     * @param {string} log_url A URL that the error message should link to.
     * @param {function()} onClickTryAgain A callback for the
     *     "Try Again" button.
     */
    result.showError = function(errorText, log_url, onClickTryAgain) {
      var error = document.createElement('a');
      error.setAttribute('href', log_url);
      error.setAttribute('target', 'gwt_dev_mode_log');
      error.innerText = errorText;
      error.style.color = 'red';
      error.style.textDecoration = 'underline';
      message.appendChild(error);

      var button = document.createElement('button');
      button.style.fontSize = '16pt';
      button.textContent = 'Try Again';
      button.onclick = function() {
        body.removeChild(dialog);
        body.removeChild(overlay);
        onClickTryAgain();
      };
      dialog.appendChild(button);
    };

    return result;
  }

  /**
   * Makes a JSONP call. Assumes that the callback parameter is named
   * "callback". Handles multiple callbacks in flight at once.
   * @param {string} url_prefix A URL prefix that ends with '?' or '&'.
   * @param {function(Object)} callback A function to call with the result
   * of the JSONP call.
   */
  function callJsonp(url_prefix, callback) {
    var callback_id = 'c' + globals.callback_counter++;
    globals.callbacks[callback_id] = function(json) {
      delete globals.callbacks[callback_id];
      callback(json);
    };

    var url = url_prefix + '_callback=__gwt_bookmarklet_globals.callbacks.' +
        callback_id;

    var script = document.createElement('script');
    script.src = url;
    document.getElementsByTagName('head')[0].appendChild(script);
  }

  /**
   * Gets the binding properties to use for this recompile. The properties
   * come from the GWT application on the current page, with a fallback
   * to a cached value in session storage. (The cached value will normally
   * be used if the current page is already using dev mode.)
   * @param {string} module_name Identifies the GWT app we are recompiling.
   * @param {function} get_prop_map Returns the app's binding properties.
   * @return {string} Zero or more encoded url parameters; the string is
   * either empty or ends with '&'.
   */
  function getBindingParameters(module_name, get_prop_map) {
    var session_key = '__gwtDevModeSession:' + module_name;

    var prop_map = get_prop_map();
    var props = [];
    for (var key in prop_map) {
      props.push(encodePair(key, prop_map[key]));
    }

    if (!props.length) {
      // There is only one permutation, maybe because we're in dev mode already.
      // Use the cached value if present.
      var cached = window.sessionStorage[session_key];
      return cached || '';
    }

    var encoded = props.join('&') + '&';
    // Cache it for the next recompile.
    window.sessionStorage[session_key] = encoded;
    return encoded;
  }

  function encodePair(key, value) {
    return encodeURIComponent(key) + '=' + encodeURIComponent(value);
  }

  /**
   * Tells the GWT application to replace itself with a different version that
   * the code server is serving, then reloads the page. (This only works if
   * the GWT application was compiled with the new dev mode hook turned on in
   * the GWT linker.)
   *
   * @param {string} module_name The module to replace (after rename).
   * @param {string} codeserver_url The code server to use
   *     (with trailing slash).
   */
  function reloadInDevMode(module_name, codeserver_url) {
    var key = '__gwtDevModeHook:' + module_name;
    sessionStorage[key] = codeserver_url + module_name + '/' +
        module_name + '.nocache.js';
    window.location.reload();
  }

  /**
   * Displays the "compiling" dialog and starts the recompile.
   * @param {string} module_name The module to replace (after rename).
   * @param {string} codeserver_url The code server to use.
   * @param {function} get_prop_map Returns the app's binding properties.
   */
  function compile(module_name, codeserver_url, get_prop_map) {
    var dialog = showCompilingDialog('Compiling ' + module_name + '...');

    function onClickTryAgain() {
      // Just start over from the beginning.
      compile(module_name, codeserver_url, get_prop_map);
    }

    function onCompileFinished(json) {
      if (json.status != 'ok') {
        var log_url = codeserver_url + 'log/' + module_name;
        dialog.showError(json.status, log_url, onClickTryAgain);
        return;
      }
      reloadInDevMode(module_name, codeserver_url);
    }

    var url_prefix = codeserver_url + 'recompile/' + module_name + '?' +
    getBindingParameters(module_name, get_prop_map);
    callJsonp(url_prefix, onCompileFinished);
  }

  /**
   * Determines whether we should run the "Dev Mode On" or "Compile"
   * bookmarklet.
   * @param params {map} Parameters passed in by the bookmarklet.
   */
  function runBookmarklet(params) {
    if (!params || !params.server_url) {
      window.alert('Need to reinstall the bookmarklets.');
      return;
    }

    var module_name = params.module_name;

    var error = null;
    if (module_name) {
      error = getCannotCompileError(module_name);
    }

    if (module_name && !error) {
      // The user clicked a "Compile" bookmarklet and we believe it will
      // succeed.
      var active_modules = window.__gwt_activeModules;
      compile(module_name, params.server_url,
          active_modules[module_name].bindings);
    } else {
      // The user clicked the "Dev Mode On" bookmarklet or something is wrong.
      showModuleDialog(params.server_url);
    }
  }

  // remove previous module dialog, if present
  if (globals.dismissModuleDialog) {
    globals.dismissModuleDialog();
  }

  runBookmarklet(getBookmarkletParams());
})();
