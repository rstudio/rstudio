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
  var $doc = document;

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
    try {
      delete window.__gwt_bookmarklet_params;
    } catch (e) {
      // Delete window.x throws and exception in IE8.
      window['__gwt_bookmarklet_params'] = null;
    }
    return params;
  }

  /**
   * Creates an element and populates it with text.
   * Ensures that a text is black, regardless of style sheet.
   * @param tagName {string}
   * @param fontSize {string}
   * @param text {string}
   */
  function makeTextElt(tagName, fontSize, text) {
    var elt = $doc.createElement(tagName);
    elt.style.color = 'black';
    elt.style.background = 'white';
    elt.style.fontSize = fontSize;
    elt.appendChild($doc.createTextNode(text));
    return elt;
  }

  function makeOverlay() {
    var overlay = $doc.createElement('div');
    overlay.style.zIndex = 1000000;
    overlay.style.position = 'absolute';
    overlay.style.top = 0;
    overlay.style.left = 0;
    overlay.style.bottom = 0;
    overlay.style.right = 0;
    overlay.style.background = 'black'; // darken background
    overlay.style.opacity = '0.5';
    return overlay;
  }

  function makeDialog() {
    var dialog = $doc.createElement('div');
    dialog.style.zIndex = 1000001;
    dialog.style.position = 'fixed';
    dialog.style.top = '20pt';
    dialog.style.left = '20pt';
    dialog.style.right = '20pt';
    dialog.style.color = 'black';
    dialog.style.background = 'white';
    dialog.style.border = '4px solid #ccc';
    dialog.style.padding = '1em';
    dialog.style.borderRadius = '5px';
    dialog.style.wordWrap = 'break-word';
    return dialog;
  }

  function makeBookmarklet(name, javascript) {
    var result = makeTextElt('a', '12pt', name);
    result.style.fontFamily = 'sans';
    result.style.textDecoration = 'none';
    result.style.background = '#ddd';
    result.style.border = '2px outset #ddd';
    result.style.padding = '3pt';
    result.setAttribute('href', 'javascript:' + encodeURIComponent(javascript));
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
    if (!isModuleOnCodeServer(module_name)) {
      return 'The code server isn\'t configured to compile this module.';
    }

    var modules_on_page = window.__gwt_activeModules;
    if (!modules_on_page || !(module_name in modules_on_page)) {
      return 'The current page doesn\'t have this module.';
    }

    var mod = modules_on_page[module_name];

    var dev_mode_key = '__gwtDevModeHook:' + module_name;
    var dev_mode_on = mod['superdevmode'] ||
        window.sessionStorage[dev_mode_key];

    if (!dev_mode_on && !mod.canRedirect) {
      return 'This module doesn\'t have Super Dev Mode enabled.';
    }

    // looks okay
    return null;
  }

  /**
   * Determines if the code server is configured to run the given module.
   * @param module_name {string}
   * @return {boolean} true if the code server supports the given module.
   */
  function isModuleOnCodeServer(module_name) {
    var modules_on_codeserver = window.__gwt_codeserver_config.moduleNames;
    // Support browsers without indexOf() (e.g. IE8).
    for (var i = 0; i < modules_on_codeserver.length; i++) {
      if (modules_on_codeserver[i] == module_name) {
        return true;
      }
    }
    return false;
  }

  /**
   * Displays the "Choose module to compile" dialog.
   * @param codeserver_url {string} The URL of the code server that will
   *    compile the chosen module.
   */
  function showModuleDialog(codeserver_url) {

    function makeHeader() {
      return makeTextElt('div', '20pt', 'Choose a module to recompile:');
    }

    function makeModuleRows(mod) {
      var module_name = mod.moduleName;
      var error = getCannotCompileError(module_name);
      var moduleColor = error ? 'grey' : 'black';

      var row = $doc.createElement('tr');

      // Bullet and module name
      var cell = $doc.createElement('td');
      var text = makeTextElt('span', '14pt', "\u2022 " + module_name + ": ");
      text.style.color = moduleColor;
      cell.appendChild(text);

      // Status (usually clickable)
      var status = makeTextElt('span', '14pt', mod.superdevmode ? "on" : "off");
      status.style.color = moduleColor;
      if (!error || status.superdevmode) {
        status.style.cursor = "pointer";
        status.onclick = function() {
          if (mod.superdevmode) {
            reloadWithoutDevMode(module_name);
          } else {
            reloadInDevMode(module_name, codeserver_url);
          }
          return false;
        };
        status.title = "Click to turn " +
            (mod.superdevmode ? "off." : "on without recompiling.") +
            " (Reloads the page.)";
      }
      cell.appendChild(status);

      row.appendChild(cell);

      if (!error) {
        // Compile button
        var button = makeCompileBookmarklet(codeserver_url, module_name);
        button.className = 'module_' + module_name;
        cell = $doc.createElement('td');
        cell.style.paddingLeft = '1em';
        cell.appendChild(button);
        row.appendChild(cell);
      }

      var rows = [row];
      if (error) {
        // Error message
        cell = makeTextElt('td', '10pt', error);
        cell.style.color = 'gray';

        row = $doc.createElement('tr');
        row.appendChild(cell);
        rows.push(row);
      }

      return rows;
    }

    function makeCodeServerLink() {
      var div = $doc.createElement('div');
      div.style.float = "right";

      var hostPort = codeserver_url.replace(/^http:\/\//, '').replace(/\/$/, '');
      var link = makeTextElt('a', '10pt', hostPort);
      link.style.textDecoration = "none";
      link.setAttribute('href', codeserver_url);
      link.setAttribute('target', '_blank');
      link.title = "The address of the server this bookmarklet uses to compile.";
      div.appendChild(link);
      return div;
    }

    var active_modules = window.__gwt_activeModules;

    var moduleTable = $doc.createElement('table');
    moduleTable.style.marginTop = "10px";
    for (var module_name in active_modules) {
      var rows = makeModuleRows(active_modules[module_name]);
      for (var i = 0; i < rows.length; i++) {
        moduleTable.appendChild(rows[i]);
      }
    }

    // Assemble the dialog.
    var dialog = makeDialog();
    if (moduleTable.hasChildNodes()) {
      dialog.appendChild(makeHeader());
      dialog.appendChild(moduleTable);
    } else {
      dialog.appendChild(makeTextElt('span', '16pt',
          'Can\'t find any GWT Modules on this page.'));
    }
    dialog.appendChild(makeCodeServerLink());

    // Grey out everything under the dialog.
    var overlay = makeOverlay();

    var body = $doc.getElementsByTagName('body')[0];
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

    var message = makeTextElt('div', '16pt', text);
    dialog.appendChild(message);

    var body = $doc.getElementsByTagName('body')[0];
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
      var error = makeTextElt('a', '16pt', errorText);
      error.setAttribute('href', log_url);
      error.setAttribute('target', 'gwt_dev_mode_log');
      error.style.color = 'red';
      error.style.textDecoration = 'underline';
      message.appendChild(error);

      var button = makeTextElt('button', '12pt', 'Try Again');
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

    var script = $doc.createElement('script');
    script.src = url;
    $doc.getElementsByTagName('head')[0].appendChild(script);
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
   * Turns dev mode off for the given module, then reloads the page.
   * @param {string} module_name
   */
  function reloadWithoutDevMode(module_name) {
    var key = '__gwtDevModeHook:' + module_name;
    sessionStorage.removeItem(key);
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
      globals.compiling = false;
      if (json.status != 'ok') {
        var log_url = codeserver_url + 'log/' + module_name;
        dialog.showError(json.status, log_url, onClickTryAgain);
        return;
      }
      reloadInDevMode(module_name, codeserver_url);
    }

    var url_prefix = codeserver_url + 'recompile/' + module_name + '?' +
    getBindingParameters(module_name, get_prop_map);
    globals.compiling = true;
    callJsonp(url_prefix, onCompileFinished);
  }

  /**
   * Determines whether we should run the "Dev Mode On" or "Compile"
   * bookmarklet.
   * @param params {map} Parameters passed in by the bookmarklet.
   */
  function runBookmarklet(params) {
    if (!!globals.compiling) {
      // A module is already being compiled.
      // We ignore the bookmarklet: the page will reload once the compilation
      // ends.
      return;
    }
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
