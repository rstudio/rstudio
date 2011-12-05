/******************************************************************************
 * Variables used by the Plugin
 *****************************************************************************/
var $entry;
var $hostedHtmlVersion="2.1";

var $errFn;
var $moduleName;
var $moduleBase;
var __gwt_getProperty;

/******************************************************************************
 * WRITE ME - what does this invokes stuff do??? Probably related to invoking
 * calls...
 *****************************************************************************/
var __gwt_javaInvokes = [];

// Wrapper to call JS methods, which we need both to be able to supply a
// different this for method lookup and to get the exception back
function __gwt_jsInvoke(thisObj, methodName) {
 try {
   var args = Array.prototype.slice.call(arguments, 2);
   return [0, window[methodName].apply(thisObj, args)];
 } catch (e) {
   return [1, e];
 }
}

function __gwt_makeJavaInvoke(argCount) {
  return __gwt_javaInvokes[argCount] || __gwt_doMakeJavaInvoke(argCount);
}

function __gwt_doMakeJavaInvoke(argCount) {
  // IE6 won't eval() anonymous functions except as r-values
  var argList = "";
  for (var i = 0; i < argCount; i++) {
    argList += ",p" + i;
  }
  var argListNoComma = argList.substring(1);

  return eval(
    "__gwt_javaInvokes[" + argCount + "] =\n" +
    "  function(thisObj, dispId" + argList + ") {\n" +
    "    var result = __static(dispId, thisObj" + argList + ");\n" +
    "    if (result[0]) {\n" +
    "      throw result[1];\n" +
    "    } else {\n" +
    "      return result[1];\n" +
    "    }\n" +
    "  }\n"
  ); 
}


/******************************************************************************
 * Functions used to create tear-offs of Java methods.  Each function corresponds
 * to exactly one dispId, and also embeds the argument count.  We get the "this"
 * value from the context in which the function is being executed.
 * Function-object identity is preserved by caching in a sparse array.
 *****************************************************************************/
var __gwt_tearOffs = [];
var __gwt_tearOffGenerators = [];
function __gwt_makeTearOff(proxy, dispId, argCount) {
  return __gwt_tearOffs[dispId] || __gwt_doMakeTearOff(dispId, argCount);
}

function __gwt_doMakeTearOff(dispId, argCount) {
  return __gwt_tearOffs[dispId] = 
      (__gwt_tearOffGenerators[argCount] || __gwt_doMakeTearOffGenerator(argCount))(dispId);
}

function __gwt_doMakeTearOffGenerator(argCount) {
  // IE6 won't eval() anonymous functions except as r-values
  var argList = "";
  for (var i = 0; i < argCount; i++) {
    argList += ",p" + i;
  }
  var argListNoComma = argList.substring(1);

  return eval(
    "__gwt_tearOffGenerators[" + argCount + "] =\n" +
    "  function(dispId) {\n" +
    "    return function(" + argListNoComma + ") {\n" +
    "      var result = __static(dispId, this" + argList + ");\n" +
    "      if (result[0]) {\n" +
    "        throw result[1];\n" +
    "      } else {\n" +
    "        return result[1];\n" +
    "      }\n" +
    "    }\n" +
    "  }\n"
  ); 
}


/******************************************************************************
 *Code to give visual feedback when something goes wrong in Dev Mode
 *****************************************************************************/
function __gwt_disconnected() {
  // Prevent double-invocation.
  window.__gwt_disconnected = new Function();
  // Do it in a timeout so we can be sure we have a clean stack.
  window.setTimeout(__gwt_disconnected_impl, 1);
}

function __gwt_disconnected_impl() {
  __gwt_displayGlassMessage('GWT Code Server Disconnected',
      'Most likely, you closed GWT Development Mode. Or, you might have lost '
      + 'network connectivity. To fix this, try restarting GWT Development Mode and '
      + 'refresh this page.');
}

// Keep track of z-index to allow layering of multiple glass messages
var __gwt_glassMessageZIndex = 2147483647;

// Note this method is also used by ModuleSpace.java
function __gwt_displayGlassMessage(summary, details) {
  var topWin = window.top;
  var topDoc = topWin.document;
  var outer = topDoc.createElement("div");
  // Do not insert whitespace or outer.firstChild will get a text node.
  outer.innerHTML = 
    '<div style="position:absolute;z-index:' + __gwt_glassMessageZIndex-- +
    ';left:50px;top:50px;width:600px;color:#FFF;font-family:verdana;text-align:left;">' +
    '<div style="font-size:30px;font-weight:bold;">' + simpleEscape(summary) + '</div>' +
    '<div style="font-size:15px;">' + simpleEscape(details) + '</div>' +
    '</div>' +
    '<div style="position:absolute;z-index:' + __gwt_glassMessageZIndex-- +
    ';left:0px;top:0px;right:0px;bottom:0px;filter:alpha(opacity=60);opacity:0.6;background-color:#000;"></div>'
  ;
  topDoc.body.appendChild(outer);
  var glass = outer.firstChild;
  var glassStyle = glass.style;

  // Scroll to the top and remove scrollbars.
  topWin.scrollTo(0, 0);
  if (topDoc.compatMode == "BackCompat") {
    topDoc.body.style["overflow"] = "hidden";
  } else {
    topDoc.documentElement.style["overflow"] = "hidden";
  }

  // Steal focus.
  glass.focus();

  if ((navigator.userAgent.indexOf("MSIE") >= 0) && (topDoc.compatMode == "BackCompat")) {
    // IE quirks mode doesn't support right/bottom, but does support this.
    glassStyle.width = "125%";
    glassStyle.height = "100%";
  } else if (navigator.userAgent.indexOf("MSIE 6") >= 0) {
    // IE6 doesn't have a real standards mode, so we have to use hacks.
    glassStyle.width = "125%"; // Get past scroll bar area.
    // Nasty CSS; onresize would be better but the outer window won't let us add a listener IE.
    glassStyle.setExpression("height", "document.documentElement.clientHeight");
  }

  $doc.title = simpleEscape(summary) + " [" + $doc.title + "]";
}


/******************************************************************************
 * Other functions called by the Plugin
 *****************************************************************************/
function __gwt_makeResult(isException, result) {
  return [isException, result];
}

//should be prefixed with "__gwt_"
function fireOnModuleLoadStart(className) {
  $sendStats("moduleStartup", "onModuleLoadStart");
}


/******************************************************************************
 * Helper functions for the Development Mode startup code. Listed alphabetically
 *****************************************************************************/
function disconnectPlugin() {
  try {
    // wrap in try/catch since plugins are not required to supply this
    plugin.disconnect();
  } catch (e) {
  }
}

function doBrowserSpecificFixes() {
  var ua = navigator.userAgent.toLowerCase();
  if (ua.indexOf("gecko") != -1) {
    // install eval wrapper on FF to avoid EvalError problem
    var __eval = window.eval;
    window.eval = function(s) {
      return __eval(s);
    }
  }
  if (ua.indexOf("chrome") != -1) {
    // work around __gwt_ObjectId appearing in JS objects
    var hop = window.Object.prototype.hasOwnProperty;
    window.Object.prototype.hasOwnProperty = function(prop) {
      return prop != "__gwt_ObjectId" && hop.call(this, prop);
    };
    var hop2 = window.Object.prototype.propertyIsEnumerable;
    window.Object.prototype.propertyIsEnumerable = function(prop) {
      return prop != "__gwt_ObjectId" && hop2.call(this, prop);
    };
    // do the same in the main window if it is different from our window
    if ($wnd != window) {
      var hop3 = $wnd.Object.prototype.hasOwnProperty;
      $wnd.Object.prototype.hasOwnProperty = function(prop) {
        return prop != "__gwt_ObjectId" && hop3.call(this, prop);
      };
      var hop4 = $wnd.Object.prototype.propertyIsEnumerable;
      $wnd.Object.prototype.propertyIsEnumerable = function(prop) {
        return prop != "__gwt_ObjectId" && hop4.call(this, prop);
      };
    }
  }
}

function embedPlugin() {
  var embed = document.createElement('embed');
  embed.id = 'pluginEmbed';
  embed.type = 'application/x-gwt-hosted-mode';
  embed.width = '10';
  embed.height = '20';

  var obj = document.createElement('object');
  obj.id = 'pluginObject';
  obj.classid = 'clsid:1D6156B6-002B-49E7-B5CA-C138FB843B4E';

  document.body.appendChild(embed);
  document.body.appendChild(obj);
}

function findPluginObject() {
  try {
    return document.getElementById('pluginObject');
  } catch (e) {
    return null;
  }
}

function findPluginEmbed() {
  try {
    return document.getElementById('pluginEmbed')
  } catch (e) {
    return null;
  }
}

function findPluginXPCOM() {
  try {
    return __gwt_HostedModePlugin;
  } catch (e) {
    return null;
  }
}

function getCodeServer() {
  var server = "localhost:9997";
  var query = $wnd.location.search;
  var idx = query.indexOf("gwt.codesvr=");
  if (idx >= 0) {
    idx += 12;  // "gwt.codesvr=".length == 12
  } else {
    idx = query.indexOf("gwt.codesvr.__MODULE_NAME__=");
    idx += (13 + "__MODULE_NAME__".length);  // 
  }
  if (idx >= 0) {
    var amp = query.indexOf("&", idx);
    if (amp >= 0) {
      server = query.substring(idx, amp);
    } else {
      server = query.substring(idx);
    }
    // According to RFC 3986, some of this component's characters (e.g., ':')
    // are reserved and *may* be escaped.
    return decodeURIComponent(server);
  }
}

function generateSessionId() {
  var ASCII_EXCLAMATION = 33;
  var ASCII_TILDE = 126;
  var chars = [];
  for (var i = 0; i < 16; ++i) {
    chars.push(Math.floor(ASCII_EXCLAMATION
    + Math.random() * (ASCII_TILDE - ASCII_EXCLAMATION + 1)));
  }
  return String.fromCharCode.apply(null, chars);
}

function loadIframe(url) {
  var topDoc = window.top.document;

  // create an iframe
  var iframeDiv = topDoc.createElement("div");
  iframeDiv.innerHTML = "<iframe scrolling=no frameborder=0 src='" + url + "'>";
  var iframe = iframeDiv.firstChild;

  // mess with the iframe style a little
  var iframeStyle = iframe.style;
  iframeStyle.position = "absolute";
  iframeStyle.borderWidth = "0";
  iframeStyle.left = "0";
  iframeStyle.top = "0";
  iframeStyle.width = "100%";
  iframeStyle.backgroundColor = "#ffffff";
  iframeStyle.zIndex = "1";
  iframeStyle.height = "100%";

  // update the top window's document's body's style
  var hostBodyStyle = window.top.document.body.style; 
  hostBodyStyle.margin = "0";
  hostBodyStyle.height = iframeStyle.height;
  hostBodyStyle.overflow = "hidden";

  // insert the iframe
  topDoc.body.insertBefore(iframe, topDoc.body.firstChild);
}

function pluginConnectionError(codeServer) {
  if ($errFn) {
    $errFn($moduleName);
  } else {
    __gwt_displayGlassMessage("Plugin failed to connect to Development Mode server at " + codeServer,
        "Follow the underlying troubleshooting instructions");
    loadIframe("http://code.google.com/p/google-web-toolkit/wiki/TroubleshootingOOPHM");
  }
}

function simpleEscape(originalString) {
  return originalString.replace("&","&amp;")
    .replace("<","&lt;")
    .replace(">","&gt;")
    .replace("\'", "&#39;")
    .replace("\"","&quot;");
}

function tryConnectingToPlugin(sessionId, url) {
  // Note that the order is important
  var pluginFinders = [findPluginXPCOM, findPluginObject, findPluginEmbed];
  var codeServer = getCodeServer();
  var plugin = null;
  for (var i = 0; i < pluginFinders.length; ++i) {
    try {
      var maybePlugin = pluginFinders[i]();
      if (maybePlugin != null && maybePlugin.init(window)) {
        plugin = maybePlugin;
        break;
      }
    } catch (e) {
    }
  }

  if (plugin == null) {
    // Plugin initialization failed. Show the missing-plugin page.
    return null;
  }
  if (!plugin.connect(url, sessionId, codeServer, $moduleName,
                      $hostedHtmlVersion)) {
    // Connection failed. Show the error alert and troubleshooting page.
    pluginConnectionError(codeServer);
  }

  return plugin;
}


/******************************************************************************
 * Development Mode startup code
 *****************************************************************************/
function gwtOnLoad(errFn, moduleName, moduleBase, softPermutationId, computePropValue) {
  $errFn = errFn;
  $moduleName = moduleName;
  $moduleBase = moduleBase;
  __gwt_getProperty = computePropValue;
  
  window.onunload = function() { };
  doBrowserSpecificFixes();

  if (!findPluginXPCOM()) {
    embedPlugin();
  }

  var topWin = window.top;
  if (!topWin.__gwt_SessionID) {
    topWin.__gwt_SessionID = generateSessionId();
  }

  var plugin = tryConnectingToPlugin(topWin.__gwt_SessionID, topWin.location.href);
  if (plugin == null) {
    loadIframe("http://gwt.google.com/missing-plugin/");
  } else {
    window.onUnload = disconnectPlugin();
  }
}
