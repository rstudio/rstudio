/*
 * Copyright 2014 Google Inc.
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

var Dialog = $namespace.lib.Dialog;
var PropertySource = $namespace.lib.PropertySource;
var Recompiler = $namespace.lib.Recompiler;
var BaseUrlProvider = $namespace.lib.BaseUrlProvider;
var MetaTagParser = $namespace.lib.MetaTagParser;
//Publish a global variable to let others know that we have been loaded
$wnd.__gwt_sdm = $wnd.__gwt_sdm || {};
$wnd.__gwt_sdm.loaded = true;

/**
 * Construct the main class.
 *
 * @constructor
 * @param {string} moduleName
 * @param {Function} initPropertyProviders
 */
function Main(moduleName, initPropertyProviders){
  var metaTagParser = new MetaTagParser(moduleName);
  var propertySource = new PropertySource(moduleName, initPropertyProviders, metaTagParser);
  this.__moduleName = moduleName;
  this.__dialog = new Dialog();
  this.__recompiler = new Recompiler(moduleName, propertySource.computeBindingProperties());
  this.__baseUrlProvider = new BaseUrlProvider(moduleName, metaTagParser);
}

/**
 * Compile the current gwt module.
 */
Main.prototype.compile = function() {
  var that = this;
  // Export the module base of the server running the backend. (Returned by GWT.getModuleBaseURL.)
  // (If we are using the bookmarklets with an external server, the dev mode hook has already set
  // this up to point somewhere different, so don't override it.)
  var moduleBaseKey = '__gwtDevModeHook:' + this.__moduleName + ':moduleBase';
  if (!$wnd[moduleBaseKey]) {
    $wnd[moduleBaseKey] = this.__baseUrlProvider.getBaseUrl();
  }

  this.__dialog.clear();
  this.__dialog.add(this.__dialog.createTextElement("div", "12pt", "Compiling " + this.__moduleName));
  this.__dialog.show();
  this.__recompiler.compile(function(result) {
    that.__dialog.clear();
    if (result.status != 'ok') {
      that.__renderError(result);
    } else {
      that.__dialog.hide();
      that.__recompiler.loadApp();
    }
  });
};

/**
 * Render an error if compile failed.
 * @param {object} result - the jsonp object from the compile server.
 */
Main.prototype.__renderError = function(result) {
  var that = this;
  var link = this.__dialog.createTextElement('a', '16pt', result.status);
  link.setAttribute('href', this.__recompiler.getLogUrl());
  link.setAttribute('target', 'gwt_dev_mode_log');
  link.style.color = 'red';
  link.style.textDecoration = 'underline';
  this.__dialog.add(link);

  var button = this.__dialog.createTextElement('button', '12pt', 'Try Again');
  button.onclick = function() {
    that.compile();
  };
  button.style.marginLeft = '10px';
  this.__dialog.add(button);
};

new Main(__moduleName, __initPropertyProviders).compile();
