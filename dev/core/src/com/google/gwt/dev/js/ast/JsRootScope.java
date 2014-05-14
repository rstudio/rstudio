/*
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.js.ast;

import com.google.gwt.dev.util.collect.Lists;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The root scope is the parent of every scope, it contains a list of browser built-in identifiers
 * that we should recognize and never obfuscate into.
 */
public final class JsRootScope extends JsScope {
  /*
   * NOTE: the startup sequence for this class is a bit tricky.
   */

  private static class SerializedForm implements Serializable {
    private Object readResolve() {
      return JsRootScope.INSTANCE;
    }
  }

  public static final JsRootScope INSTANCE;

  private static final String[] COMMON_BUILTINS = new String[] {
      // 15.1.1 Value Properties of the Global Object
      "NaN",
      "Infinity",
      "undefined",

      // 15.1.2 Function Properties of the Global Object
      "eval",
      "parseInt",
      "parseFloat",
      "isNaN",
      "isFinite",

      // 15.1.3 URI Handling Function Properties
      "decodeURI",
      "decodeURIComponent",
      "encodeURI",
      "encodeURIComponent",

      // 15.1.4 Constructor Properties of the Global Object
      "Object",
      "Function",
      "Array",
      "String",
      "Boolean",
      "Number",
      "Date",
      "RegExp",
      "Error",
      "EvalError",
      "RangeError",
      "ReferenceError",
      "SyntaxError",
      "TypeError",
      "URIError",

      // 15.1.5 Other Properties of the Global Object
      "Math",

      // 10.1.6 Activation Object
      "arguments",

      // B.2 Additional Properties (non-normative)
      "escape",
      "unescape",

      // Window props (https://developer.mozilla.org/en/DOM/window)
      "applicationCache",
      "closed",
      "Components",
      "content",
      "controllers",
      "crypto",
      "defaultStatus",
      "dialogArguments",
      "directories",
      "document",
      "frameElement",
      "frames",
      "fullScreen",
      "globalStorage",
      "history",
      "innerHeight",
      "innerWidth",
      "length",
      "location",
      "locationbar",
      "localStorage",
      "menubar",
      "mozInnerScreenX",
      "mozInnerScreenY",
      "mozScreenPixelsPerCssPixel",
      "name",
      "navigator",
      "opener",
      "outerHeight",
      "outerWidth",
      "pageXOffset",
      "pageYOffset",
      "parent",
      "personalbar",
      "pkcs11",
      "returnValue",
      "screen",
      "scrollbars",
      "scrollMaxX",
      "scrollMaxY",
      "self",
      "sessionStorage",
      "sidebar",
      "status",
      "statusbar",
      "toolbar",
      "top",
      "window",

      // Window methods (https://developer.mozilla.org/en/DOM/window)
      "alert",
      "addEventListener",
      "atob",
      "back",
      "blur",
      "btoa",
      "captureEvents",
      "clearInterval",
      "clearTimeout",
      "close",
      "confirm",
      "disableExternalCapture",
      "dispatchEvent",
      "dump",
      "enableExternalCapture",
      "escape",
      "find",
      "focus",
      "forward",
      "GeckoActiveXObject",
      "getAttention",
      "getAttentionWithCycleCount",
      "getComputedStyle",
      "getSelection",
      "home",
      "maximize",
      "minimize",
      "moveBy",
      "moveTo",
      "open",
      "openDialog",
      "postMessage",
      "print",
      "prompt",
      "QueryInterface",
      "releaseEvents",
      "removeEventListener",
      "resizeBy",
      "resizeTo",
      "restore",
      "routeEvent",
      "scroll",
      "scrollBy",
      "scrollByLines",
      "scrollByPages",
      "scrollTo",
      "setInterval",
      "setResizeable",
      "setTimeout",
      "showModalDialog",
      "sizeToContent",
      "stop",
      "uuescape",
      "updateCommands",
      "XPCNativeWrapper",
      "XPCSafeJSOjbectWrapper",

      // Mozilla Window event handlers, same cite
      "onabort",
      "onbeforeunload",
      "onchange",
      "onclick",
      "onclose",
      "oncontextmenu",
      "ondragdrop",
      "onerror",
      "onfocus",
      "onhashchange",
      "onkeydown",
      "onkeypress",
      "onkeyup",
      "onload",
      "onmousedown",
      "onmousemove",
      "onmouseout",
      "onmouseover",
      "onmouseup",
      "onmozorientation",
      "onpaint",
      "onreset",
      "onresize",
      "onscroll",
      "onselect",
      "onsubmit",
      "onunload",

      // Safari Web Content Guide
      // http://developer.apple.com/library/safari/#documentation/AppleApplications/Reference/SafariWebContent/SafariWebContent.pdf
      // WebKit Window member data, from WebKit DOM Reference
      // (http://developer.apple.com/safari/library/documentation/AppleApplications/Reference/WebKitDOMRef/DOMWindow_idl/Classes/DOMWindow/index.html)
      // TODO(fredsa) Many, many more functions and member data to add
      "ontouchcancel",
      "ontouchend",
      "ontouchmove",
      "ontouchstart",
      "ongesturestart",
      "ongesturechange",
      "ongestureend",

      // Media events
      "oncanplaythrough",
      "onended",
      "onprogress",

      // extra window methods
      "uneval",

      // keywords https://developer.mozilla.org/en/New_in_JavaScript_1.7,
      // https://developer.mozilla.org/en/New_in_JavaScript_1.8.1
      "getPrototypeOf",
      "let",

      // "future reserved words"
      "abstract",
      "int",
      "short",
      "boolean",
      "interface",
      "static",
      "byte",
      "long",
      "char",
      "final",
      "native",
      "synchronized",
      "float",
      "package",
      "throws",
      "goto",
      "private",
      "transient",
      "implements",
      "protected",
      "volatile",
      "double",
      "public",

      // IE methods
      // (http://msdn.microsoft.com/en-us/library/ms535873(VS.85).aspx#)
      "attachEvent",
      "clientInformation",
      "clipboardData",
      "createPopup",
      "dialogHeight",
      "dialogLeft",
      "dialogTop",
      "dialogWidth",
      "onafterprint",
      "onbeforedeactivate",
      "onbeforeprint",
      "oncontrolselect",
      "ondeactivate",
      "onhelp",
      "onresizeend",

      // Common browser-defined identifiers not defined in ECMAScript
      "event",
      "external",
      "Debug",
      "Enumerator",
      "Global",
      "Image",
      "ActiveXObject",
      "VBArray",
      "Components",

      // Functions commonly defined on Object
      "toString",
      "getClass",
      "constructor",
      "prototype",

      // Client-side JavaScript identifiers, which are needed for linkers
      // that don't ensure GWT's window != $wnd, document != $doc, etc.
      // Taken from the Rhino book, pg 715
      "Anchor", "Applet", "Attr", "Canvas", "CanvasGradient", "CanvasPattern",
      "CanvasRenderingContext2D", "CDATASection", "CharacterData", "Comment", "CSS2Properties",
      "CSSRule", "CSSStyleSheet", "Document", "DocumentFragment", "DocumentType", "DOMException",
      "DOMImplementation", "DOMParser", "Element", "Event", "ExternalInterface", "FlashPlayer",
      "Form", "Frame", "History", "HTMLCollection", "HTMLDocument", "HTMLElement", "IFrame",
      "Image", "Input", "JSObject", "KeyEvent", "Link", "Location", "MimeType", "MouseEvent",
      "Navigator", "Node", "NodeList", "Option", "Plugin", "ProcessingInstruction", "Range",
      "RangeException", "Screen", "Select", "Table", "TableCell", "TableRow", "TableSelection",
      "Text", "TextArea", "UIEvent", "Window", "XMLHttpRequest", "XMLSerializer", "XPathException",
      "XPathResult", "XSLTProcessor",

      /*
       * These keywords trigger the loading of the java-plugin. For the next-generation plugin, this
       * results in starting a new Java process.
       */
      "java", "Packages", "netscape", "sun", "JavaObject", "JavaClass", "JavaArray", "JavaMember",

      // GWT-defined identifiers
      "$wnd", "$doc", "$moduleName", "$moduleBase", "$gwt_version", "$sessionId", "gwtOnLoad",

      // typeMarker 'tM' field will break JSO detection on window object if nullMethod is called 'tM'
      "tM",

      // Identifiers used by JsStackEmulator; later set to obfuscatable
      "$stack", "$stackDepth", "$location",

      // TODO: prove why this is necessary or remove it
      "call",};

  static {
    INSTANCE = new JsRootScope();
  }

  private final Map<String, JsName> names = new LinkedHashMap<String, JsName>();

  private final JsName undefined;

  public JsRootScope() {
    super("Root");
    for (String ident : COMMON_BUILTINS) {
      names.put(ident, new JsRootName(this, ident));
    }
    undefined = names.get("undefined");
    assert undefined != null;
  }

  @Override
  public Iterable<JsName> getAllNames() {
    return Collections.unmodifiableCollection(names.values());
  }

  @Override
  public List<JsScope> getChildren() {
    return Lists.create();
  }

  @Override
  public JsScope getParent() {
    return null;
  }

  public JsName getUndefined() {
    return undefined;
  }

  @Override
  protected void addChild(JsScope child) {
    // Don't record children.
  }

  @Override
  protected JsName doCreateName(String ident, String shortIdent) {
    throw new UnsupportedOperationException("Cannot create new names in the root scope");
  }

  @Override
  protected JsName findExistingNameNoRecurse(String ident) {
    return names.get(ident);
  }

  private Object writeReplace() {
    return new SerializedForm();
  }
}
