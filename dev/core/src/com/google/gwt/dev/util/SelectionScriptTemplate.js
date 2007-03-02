/*
 * Copyright 2006 Google Inc.
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
  // ---------------- INTERNAL GLOBALS ----------------
  
  // Cache symbols locally for good obfuscation
  var wnd = window;
  var external = wnd.external;
  
  // These two variables gate calling gwtOnLoad; both must be true to start
  var scriptsDone, loadDone;
  
  // A map of properties that were declared in meta tags
  var __gwt_metaProps = {};
  
  // A map of module rebasings
  var __gwt_base = {};
  
  // These variables contain deferred-binding properties, values, and
  // providers.
  //
  var props = [];
  var values = [];
  var providers = [];
  
  // Property answers go here
  var answers = [];

  // ------------------ TRUE GLOBALS ------------------

  // Maps to synchronize the loading of styles and scripts; resources are loaded
  // only once, even when multiple modules depend on them.  This API must not
  // change across GWT versions.
  if (!wnd.__gwt_stylesLoaded) { wnd.__gwt_stylesLoaded = {}; }
  if (!wnd.__gwt_scriptsLoaded) { wnd.__gwt_scriptsLoaded = {}; }

  // --------------- INTERNAL FUNCTIONS ---------------

  // The default module load error function; may be overwritten via meta props
  //
  function __gwt_onLoadError() {
    alert('Failed to load module __MODULE_NAME__' +
      '".\nPlease see the log in the development shell for details.');
  }

  // The default bad property error function; may be overwritten via meta props
  //
  function __gwt_onPropertyError(propName, allowedValues, badValue) {
    var msg = 'While attempting to load module __MODULE_NAME__, property \"'
      + propName;
    if (badValue != null) {
      msg += '\" was set to the unexpected value \"' + badValue + '\"';
    } else {
      msg += '\" was not specified';
    }
    msg += 'Allowed values: ' + allowedValues;
    alert(msg);
  }
  

  function isHostedMode() {
    return (external && external.gwtOnLoad &&
        (document.location.href.indexOf('gwt.hybrid') == -1));
  }
  

  // Called by both onScriptLoad() and onInjectionDone(). It causes
  // the specified module to be cranked up.
  //
  function maybeStartModule() {
    if (scriptsDone && loadDone) {
      var iframe = document.getElementById('__MODULE_NAME__');
      var frameWnd = iframe.contentWindow;
      // copy the init handlers function into the iframe
      frameWnd.__gwt_initHandlers = __MODULE_FUNC__.__gwt_initHandlers;
      // remove this whole function from the global namespace to allow GC
      __MODULE_FUNC__ = null;
      iframe.contentWindow.gwtOnLoad(__gwt_onLoadError, '__MODULE_NAME__');
    }
  }
  
  // Called to slurp up all <meta> tags:
  // gwt:property, gwt:base, gwt:onPropertyErrorFn, gwt:onLoadErrorFn
  //
  function processMetas() {
    var metas = document.getElementsByTagName('meta');
  
    for (var i = 0, n = metas.length; i < n; ++i) {
      var meta = metas[i];
      var name = meta.getAttribute('name');
  
      if (name) {
        if (name == 'gwt:property') {
          var content = meta.getAttribute('content');
          if (content) {
            var name = content, value = '';
            var eq = content.indexOf('=');
            if (eq != -1) {
              name = content.substring(0, eq);
              value = content.substring(eq+1);
            }
            __gwt_metaProps[name] = value;
          }
        } else if (name == 'gwt:onPropertyErrorFn') {
          var content = meta.getAttribute('content');
          if (content) {
            try {
              __gwt_onPropertyError = eval(content);
            } catch (e) {
              alert('Bad handler \"' + content +
                '\" for \"gwt:onPropertyErrorFn\"');
            }
          }
        } else if (name == 'gwt:onLoadErrorFn') {
          var content = meta.getAttribute('content');
          if (content) {
            try {
              __gwt_onLoadError = eval(content);
            } catch (e) {
              alert('Bad handler \"' + content + '\" for \"gwt:onLoadErrorFn\"');
            }
          }
        } else if (name == 'gwt:base') {
          var content = meta.getAttribute('content');
          var eqPos = content.lastIndexOf('=');
          if (eqPos == -1) {
            continue;
          }
          var moduleBase = content.substring(0, eqPos);
          var moduleName = content.substring(eqPos + 1);
          __gwt_base[moduleName] = moduleBase;
        }
      }
    }
  }

  /**
   * Determines whether or not a particular property value is allowed. Called by
   * property providers.
   * 
   * @param propName the name of the property being checked
   * @param propValue the property value being tested
   */
  function __gwt_isKnownPropertyValue(propName, propValue) {
    return propValue in values[propName];
  }

  /**
   * Returns a meta property value, if any.  Used by DefaultPropertyProvider.
   */
  function __gwt_getMetaProperty(name) {
    var value = __gwt_metaProps[name];
    return (value == null) ? null : value;
  }

  // Deferred-binding mapper function.
  //
  function O(a,v) {
    var answer = answers;
    var i = -1;
    var n = a.length - 1;
    while (++i < n) {
      if (!(a[i] in answer)) {
        answer[a[i]] = [];
      }
      answer = answer[a[i]];
    }
    answer[a[n]] = v;
  }
  
  // --------------- PROPERTY PROVIDERS ---------------

// __PROPERTIES_BEGIN__
// __PROPERTIES_END__

  // --------------- EXPOSED FUNCTIONS ----------------

  // Called when the script injection is complete.
  //
  __MODULE_FUNC__.onInjectionDone = function() {
    // Mark this module's script injection done and (possibly) start the module.
    scriptsDone = true;
    maybeStartModule();
  }

  // Called when the compiled script identified by moduleName is done loading.
  //
  __MODULE_FUNC__.onScriptLoad = function() {
    // Mark this module's script as done loading and (possibly) start the module.
    loadDone = true;
    maybeStartModule();
  }
  
  // --------------- STRAIGHT-LINE CODE ---------------

// __SHELL_SERVLET_ONLY_BEGIN__
  // Force shell servlet to serve compiled output for web mode
  if (!isHostedMode()) {
    document.write('<script src="__MODULE_NAME__.nocache.js?compiled"></script>');
    return;
  }
// __SHELL_SERVLET_ONLY_END__

  processMetas();

  var strongName;
  if (isHostedMode()) {
    // In hosted mode, inject the script frame directly.
    var iframe = document.createElement('iframe');
    iframe.id = '__MODULE_NAME__';
    iframe.style.width = '0px';
    iframe.style.height = '0px';
    iframe.style.border = '0px';
    document.body.appendChild(iframe);

    iframe.src = 'blank.html';
    iframe.onload = function() {
      var frameWnd = iframe.contentWindow;
      frameWnd.$wnd = wnd;
      frameWnd.$doc = wnd.document;

      // inject hosted mode property evaluation function
      frameWnd.__gwt_getProperty = function(name) {
        return providers[name]();
      };

      // inject gwtOnLoad
      frameWnd.gwtOnLoad = function(errFn, modName) {
        if (!external.gwtOnLoad(frameWnd, modName)) {
          errFn(modName);
        }
      }

      // Hook the iframe's onunload, so that the hosted browser has a chance
      // to clean up its ModuleSpaces.
      frameWnd.onunload = function() {
        external.gwtOnLoad(frameWnd, null);
      };

      __MODULE_FUNC__.onScriptLoad();
    };
  } else {
    try {
// __PERMUTATIONS_BEGIN__
      // Permutation logic
// __PERMUTATIONS_END__
    } catch (e) {
      // intentionally silent on property failure
      return;
    }  

	  // TODO: do we still need this query stuff?
	  var query = location.search;
	  query = query.substring(0, query.indexOf('&'));

	  var base = __gwt_base['__MODULE_NAME__'];
	  var newUrl = (base ? base + '/' : '') + strongName + '.cache.html' + query;
	  document.write('<iframe id="__MODULE_NAME__" style="width:0;height:0;border:0" src="' + newUrl + '"></iframe>');
  }

// __MODULE_DEPS_BEGIN__
  // Module dependencies, such as scripts and css
// __MODULE_DEPS_END__
  document.write('<script>__MODULE_FUNC__.onInjectionDone(\'__MODULE_NAME__\')</script>');
}

// Called from compiled code to hook the window's resize & load events (the
// code running in the script frame is not allowed to hook these directly).
// 
// Notes:
// 1) We declare it here in the global scope so that it won't closure the
// internals of the module func.
//
// 2) We hang it off the module func to avoid polluting the global namespace.
//
// 3) This function will be copied directly into the script frame window!
//
__MODULE_FUNC__.__gwt_initHandlers = function(resize, beforeunload, unload) {
  var wnd = window;
  var oldOnResize = wnd.onresize;
  wnd.onresize = function() {
   resize();
   if (oldOnResize)
     oldOnResize();
  };
  
  var oldOnBeforeUnload = wnd.onbeforeunload;
  wnd.onbeforeunload = function() {
   var ret = beforeunload();
  
   var oldRet;
   if (oldOnBeforeUnload)
     oldRet = oldOnBeforeUnload();
  
   if (ret !== null)
     return ret;
   return oldRet;
  };
  
  var oldOnUnload = wnd.onunload;
  wnd.onunload = function() {
   unload();
   if (oldOnUnload)
     oldOnUnload();
  };
}

__MODULE_FUNC__();
