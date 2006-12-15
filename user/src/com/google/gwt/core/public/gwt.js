// Copyright 2006 Google Inc.
// 
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License. You may obtain a copy of
// the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations under
// the License.
// 
// This startup script should be included in host pages either just after
// <body> or inside the <head> after module <meta> tags.
//

//////////////////////////////////////////////////////////////////////////////
// DynamicResources
//

function DynamicResources() {
  this.pendingElemsBySrc_ = {};
  this.pendingScriptElems_ = new Array();
}
DynamicResources.prototype = {};

// The array is set up such that, pairwise, the entries are (src, readyFnStr).
// Called once for each module that is attached to the host page.
// It is theoretically possible that addScripts() could be called reentrantly
// if the browser event loop is pumped during this function and an iframe loads; 
// we may want to enhance this method in the future to support that case.
DynamicResources.prototype.addScripts = function(scriptArray, insertBeforeElem) {
  var wasEmpty = (this.pendingScriptElems_.length == 0);
  var anyAdded = false;
  for (var i = 0, n = scriptArray.length; i < n; i += 2) {
    var src = scriptArray[i];
    if (this.pendingElemsBySrc_[src]) {
      // Don't load the same script twice.
      continue;
    }
    // Set up the element but don't add it to the DOM until its turn.
    anyAdded = true;
    var e = document.createElement("script");
    this.pendingElemsBySrc_[src] = e;
    var readyFn;
    eval("readyFn = " + scriptArray[i+1]);
    e.__readyFn = readyFn;
    e.type = "text/javascript";
    e.src = src;
    e.__insertBeforeElem = insertBeforeElem;
    this.pendingScriptElems_ = this.pendingScriptElems_.concat(e);
  }
  
  if (wasEmpty && anyAdded) {
    // Kickstart.
    this.injectScript(this.pendingScriptElems_[0]);
  }
}

DynamicResources.prototype.injectScript = function(scriptElem) {
  var parentElem = scriptElem.__insertBeforeElem.parentNode;
  parentElem.insertBefore(scriptElem, scriptElem.__insertBeforeElem);
}

DynamicResources.prototype.addStyles = function(styleSrcArray, insertBeforeElem) {
  var parent = insertBeforeElem.parentNode;
  for (var i = 0, n = styleSrcArray.length; i < n; ++i) {
    var src = styleSrcArray[i];
    if (this.pendingElemsBySrc_[src]) 
      continue;
    var e = document.createElement("link");
    this.pendingElemsBySrc_[src] = e;
    e.type = "text/css";
    e.rel = "stylesheet";
    e.href = src;
    parent.insertBefore(e, insertBeforeElem);
  }
}

DynamicResources.prototype.isReady = function() {
  var elems = this.pendingScriptElems_;
  if (elems.length > 0) {
    var e = elems[0];
    if (!e.__readyFn()) {
      // The pending script isn't ready yet.
      return false;
    }
    
    // The pending script has now finished loading. Enqueue the next, if any.
    e.__readyFn = null;
    elems.shift();
    if (elems.length > 0) {
      // There is another script.
      this.injectScript(elems[0]);
      return false;
    }
  }

  // There are no more pending scripts.
  return true;
}

//////////////////////////////////////////////////////////////////////////////
// ModuleControlBlock
//
function ModuleControlBlock(metaElem, rawName) {
  var parts = ["", rawName];
  var i = rawName.lastIndexOf("=");
  if (i != -1) {
    parts[0] = rawName.substring(0, i) + '/';
    parts[1] = rawName.substring(i+1);
  }

  this.metaElem_ = metaElem;
  this.baseUrl_ = parts[0];
  this.name_ = parts[1];
  this.compilationLoaded_ = false;
  this.frameWnd_ = null;
}
ModuleControlBlock.prototype = {};

/**
 * Determines whether this module is fully loaded and ready to run.
 */
ModuleControlBlock.prototype.isReady = function() {
  return this.compilationLoaded_;
};

/**
 * Called when the compilation for this module is loaded.
 */
ModuleControlBlock.prototype.compilationLoaded = function(frameWnd) {
  this.frameWnd_ = frameWnd;
  this.compilationLoaded_ = true;
}

/**
 * Gets the logical module name, not including a base url prefix if one was
 * specified.
 */
ModuleControlBlock.prototype.getName = function() {
  return this.name_;
}

/**
 * Gets the base URL of the module, guaranteed to end with a slash.
 */
ModuleControlBlock.prototype.getBaseURL = function() {
  return this.baseUrl_;
}

/**
 * Gets the window of the module's frame.
 */
ModuleControlBlock.prototype.getModuleFrameWindow = function() {
  return this.frameWnd_;
}

/**
 * Injects a set of dynamic scripts.
 * The array is set up such that, pairwise, the entries are (src, readyFnStr).
 */
ModuleControlBlock.prototype.addScripts = function(scriptSrcArray) {
  return ModuleControlBlocks.dynamicResources_.addScripts(scriptSrcArray, this.metaElem_);
}

/**
 * Injects a set of dynamic styles.
 */
ModuleControlBlock.prototype.addStyles = function(styleSrcArray) {
  return ModuleControlBlocks.dynamicResources_.addStyles(styleSrcArray, this.metaElem_);
}
 
//////////////////////////////////////////////////////////////////////////////
// ModuleControlBlocks
//
function ModuleControlBlocks() {
  this.blocks_ = [];
}
ModuleControlBlocks.dynamicResources_ = new DynamicResources(); // "static"
ModuleControlBlocks.prototype = {};

/**
 * Adds a module control control block for the named module.
 * @param metaElem the meta element that caused the module to be added
 * @param name the name of the module being added, optionally preceded by
 * an alternate base url of the form "_path_=_module_".
 */
ModuleControlBlocks.prototype.add = function(metaElem, name) {
  var mcb = new ModuleControlBlock(metaElem, name);
  this.blocks_ = this.blocks_.concat(mcb);
};

/**
 * Determines whether all the modules are loaded and ready to run.
 */
ModuleControlBlocks.prototype.isReady = function() {
  for (var i = 0, n = this.blocks_.length; i < n; ++i) {
    var mcb = this.blocks_[i];
    if (!mcb.isReady()) {
      return false;
    }
  }
  
  // Are there any pending dynamic resources (e.g. styles, scripts)?
  if (!ModuleControlBlocks.dynamicResources_.isReady()) {
    // No, we're still waiting on one or more dynamic resources.
    return false;
  }

  return true;
}

/**
 * Determines whether there are any module control blocks.
 */
ModuleControlBlocks.prototype.isEmpty = function() {
  return this.blocks_.length == 0;
}

/**
 * Gets the module control block at the specified index.
 */
ModuleControlBlocks.prototype.get = function(index) {
  return this.blocks_[index];
}

/**
 * Injects an iframe for each module.
 */
ModuleControlBlocks.prototype.injectFrames = function() {
  for (var i = 0, n = this.blocks_.length; i < n; ++i) {
    var mcb = this.blocks_[i];

    // Insert an iframe for the module
    var iframe = document.createElement("iframe");
    var selectorUrl = mcb.getBaseURL() + mcb.getName() + ".nocache.html";
    selectorUrl += "?" + (__gwt_isHosted() ? "h&" : "" ) + i;
    var unique = new Date().getTime();
    selectorUrl += "&" + unique;
    iframe.style.border = '0px';
    iframe.style.width = '0px';
    iframe.style.height = '0px';
    
    // Fragile browser-specific ordering issues below
    
/*@cc_on
    // prevent extra clicky noises on IE
    iframe.src = selectorUrl;
@*/
    
    if (document.body.firstChild) {
      document.body.insertBefore(iframe, document.body.firstChild);
    } else {
      document.body.appendChild(iframe);
    }
    
/*@cc_on
    // prevent extra clicky noises on IE
    return;
@*/

    if (iframe.contentWindow) {
      // Older Mozilla has a caching bug for the iframe and won't reload the nocache.
      iframe.contentWindow.location.replace(selectorUrl);
    } else {
      // Older Safari doesn't have a contentWindow.
      iframe.src = selectorUrl;
    }
  }
}

/**
 * Runs the entry point for each module.
 */
ModuleControlBlocks.prototype.run = function() {
  for (var i = 0, n = this.blocks_.length; i < n; ++i) {
    var mcb = this.blocks_[i];
    var name = mcb.getName();
    var frameWnd = mcb.getModuleFrameWindow();
    if (__gwt_isHosted()) {
      if (!window.external.gwtOnLoad(frameWnd, name)) {
        // Module failed to load.
        if (__gwt_onLoadError) {
            __gwt_onLoadError(name);
        } else {
            window.alert("Failed to load module '" + name + 
            "'.\nPlease see the log in the development shell for details.");
        }
      }
    } else {
      // The compilation itself handles calling the error function.
      frameWnd.gwtOnLoad(__gwt_onLoadError, name);
    }
  }
}

//////////////////////////////////////////////////////////////////////////////
// Globals
//

var __gwt_retryWaitMillis = 10;
var __gwt_isHostPageLoaded = false;
var __gwt_metaProps = {};
var __gwt_onPropertyError = null;
var __gwt_onLoadError = null;
var __gwt_moduleControlBlocks = new ModuleControlBlocks();

//////////////////////////////////////////////////////////////////////////////
// Common 
//

/**
 * Determines whether or not the page is being loaded in the GWT hosted browser.
 */
function __gwt_isHosted() {
  if (window.external && window.external.gwtOnLoad) {
    // gwt.hybrid makes the hosted browser pretend not to be
    if (document.location.href.indexOf("gwt.hybrid") == -1) {
      return true;
    }
  }
  return false;
}

/**
 * Tries to get a module control block based on a query string passed in from
 * the caller. Used by iframes to get references back to their mcbs.
 * @param queryString the entire query string as returned by location.search,
 * which notably includes the leading '?' if one is specified
 * @return the relevant module control block, or <code>null</code> if it cannot 
 * be derived based on <code>queryString</code>
 */
function __gwt_tryGetModuleControlBlock(queryString) {
  if (queryString.length > 0) {
    // The pattern is ?[h&]<index>[&<unique>]
    var queryString = queryString.substring(1);
    if (queryString.indexOf("h&") == 0) {
      // Ignore the hosted mode flag here; only GWTShellServlet cares about it.
      queryString = queryString.substring(2);
    }
    var pos = queryString.indexOf("&");
    if (pos >= 0) {
      queryString = queryString.substring(0, pos);
    }
    var mcbIndex = parseInt(queryString);
    if (!isNaN(mcbIndex)) {
      var mcb = __gwt_moduleControlBlocks.get(mcbIndex);
      return mcb;
    }
    // Ignore the unique number that remains on the query string.
  }
  return null;
}

/**
 * Parses meta tags from the host html.
 * 
 * <meta name="gwt:module" content="_module-name_">
 *    causes the specified module to be loaded
 *
 * <meta name="gwt:property" content="_name_=_value_">
 *    statically defines a deferred binding client property
 *
 * <meta name="gwt:onPropertyErrorFn" content="_fnName_">
 *    specifies the name of a function to call if a client property is set to 
 *    an invalid value (meaning that no matching compilation will be found)
 * 
 * <meta name="gwt:onLoadErrorFn" content="_fnName_">
 *    specifies the name of a function to call if an exception happens during 
 *    bootstrapping or if a module throws an exception out of onModuleLoad(); 
 *    the function should take a message parameter
 */
function __gwt_processMetas() {
  var metas = document.getElementsByTagName("meta");
  for (var i = 0, n = metas.length; i < n; ++i) {
    var meta = metas[i];
    var name = meta.getAttribute("name");
    if (name) {
      if (name == "gwt:module") {
        var moduleName = meta.getAttribute("content");
        if (moduleName) {
          __gwt_moduleControlBlocks.add(meta, moduleName);
        }
      } else if (name == "gwt:property") {
        var content = meta.getAttribute("content");
        if (content) {
          var name = content, value = "";
          var eq = content.indexOf("=");
          if (eq != -1) {
            name = content.substring(0, eq);
            value = content.substring(eq+1);
          }
          __gwt_metaProps[name] = value;
        }
      } else if (name == "gwt:onPropertyErrorFn") {
        var content = meta.getAttribute("content");
        if (content) {
          try {
            __gwt_onPropertyError = eval(content);
          } catch (e) {
            window.alert("Bad handler \"" + content + 
              "\" for \"gwt:onPropertyErrorFn\"");
          }
        }
      } else if (name == "gwt:onLoadErrorFn") {
        var content = meta.getAttribute("content");
        if (content) {
          try {
            __gwt_onLoadError = eval(content);
          } catch (e) {
            window.alert("Bad handler \"" + content + 
              "\" for \"gwt:onLoadErrorFn\"");
          }
        }
      }
    }
  }
}

/**
 * Determines the value of a deferred binding client property specified 
 * statically in host html.
 */
function __gwt_getMetaProperty(name) {
  var value = __gwt_metaProps[name];
  if (value) {
    return value;
  } else {
    return null;
  }
}

/**
 * Determines whether or not a particular property value is allowed.
 * @param wnd the caller's window object (not $wnd!)
 * @param propName the name of the property being checked
 * @param propValue the property value being tested
 */
function __gwt_isKnownPropertyValue(wnd, propName, propValue) {
  return propValue in wnd["values$" + propName];
}

/**
 * Called by the selection script when a property has a bad value or is missing.
 * 'allowedValues' is an array of strings. Can be hooked in the host page using 
 * gwt:onPropertyErrorFn.
 */
function __gwt_onBadProperty(moduleName, propName, allowedValues, badValue) {
  if (__gwt_onPropertyError) {
    __gwt_onPropertyError(moduleName, propName, allowedValues, badValue);
    return;
  } else {
    var msg = "While attempting to load module \"" + moduleName + "\", ";
    if (badValue != null) {
       msg += "property \"" + propName + "\" was set to the unexpected value \"" 
        + badValue + "\"";
    } else {
       msg += "property \"" + propName + "\" was not specified";
    }
    
    msg += "\n\nAllowed values: " + allowedValues;
   
    window.alert(msg);
  }
}

/**
 * Called directly from compiled code.
 */
function __gwt_initHandlers(resize, beforeunload, unload) {
   var oldOnResize = window.onresize;
   window.onresize = function() {
      resize();
      if (oldOnResize)
         oldOnResize();
   };

   var oldOnBeforeUnload = window.onbeforeunload;
   window.onbeforeunload = function() {
      var ret = beforeunload();

      var oldRet;
      if (oldOnBeforeUnload)
        oldRet = oldOnBeforeUnload();

      if (ret !== null)
        return ret;
      return oldRet;
   };

   var oldOnUnload = window.onunload;
   window.onunload = function() {
      unload();
      if (oldOnUnload)
         oldOnUnload();
   };
}

//////////////////////////////////////////////////////////////////////////////
// Hosted Mode
//
function __gwt_onUnloadHostedMode() {
    window.external.gwtOnLoad(null, null);
    if (__gwt_onUnloadHostedMode.oldUnloadHandler) {
        __gwt_onUnloadHostedMode.oldUnloadHandler();
    }
}

//////////////////////////////////////////////////////////////////////////////
// Bootstrap
//

/**
 * Waits until all startup preconditions are satisfied, then launches the 
 * user-defined startup code for each module.
 */
function __gwt_latchAndLaunch() {
  var ready = true;
  
  // Are there any compilations still pending?
  if (ready && !__gwt_moduleControlBlocks.isReady()) {
    // Yes, we're still waiting on one or more compilations.
    ready = false;
  }

  // Has the host html onload event fired?
  if (ready && !__gwt_isHostPageLoaded) {
    // No, the host html page hasn't fully loaded.
    ready = false;
  }
  
  // Are we ready to run user code?
  if (ready) {
    // Yes: run entry points.
    __gwt_moduleControlBlocks.run();
  } else {
    // No: try again soon.
    window.setTimeout(__gwt_latchAndLaunch, __gwt_retryWaitMillis);
  }
}

/**
 * Starts the module-loading sequence after meta tags have been processed and
 * the body element exists.
 */
function __gwt_loadModules() {
  // Make sure the body element exists before starting.
  if (!document.body) {
    // Try again soon.
    window.setTimeout(__gwt_loadModules, __gwt_retryWaitMillis);
    return;
  }

  // Inject a frame for each module.
  __gwt_moduleControlBlocks.injectFrames();

  // Try to launch module entry points once everything is ready.
  __gwt_latchAndLaunch();
}

/**
 * The very first thing to run, and it runs exactly once unconditionally.
 */
function __gwt_bootstrap() {
  // Hook onunload for hosted mode.
  if (__gwt_isHosted()) {
    __gwt_onUnloadHostedMode.oldUnloadHandler = window.onunload;
    window.onunload = __gwt_onUnloadHostedMode;
  }

  // Hook the current window onload handler.
  var oldHandler = window.onload;
  window.onload = function() {
    __gwt_isHostPageLoaded = true;
    if (oldHandler) {
      oldHandler();
    }
  };

  // Parse meta tags from host html.
  __gwt_processMetas();

  // Load any modules.
  __gwt_loadModules();
}

// Go.
__gwt_bootstrap();
