/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.js.ast;

/**
 * The root scope is the parent of every scope. All identifiers in this scope
 * are not obfuscatable.
 */
public final class JsRootScope extends JsScope {

  private final JsProgram program;

  public JsRootScope(JsProgram program) {
    super("Root");
    this.program = program;
    ctorAddKnownGlobalSymbols();
  }

  @Override
  public JsProgram getProgram() {
    return program;
  }

  @Override
  protected JsName doCreateName(String ident, String shortIdent) {
    JsName name = super.doCreateName(ident, shortIdent);
    name.setObfuscatable(false);
    return name;
  }

  private void ctorAddKnownGlobalSymbols() {
    // Section references are from Ecma-262
    // (http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-262.pdf)
    String[] commonBuiltins = new String[] {
        // 15.1.1 Value Properties of the Global Object
        "NaN",
        "Infinity",
        "undefined",

        // 15.1.2 Function Properties of the Global Object
        "eval", "parseInt", "parseFloat",
        "isNan",
        "isFinite",

        // 15.1.3 URI Handling Function Properties
        "decodeURI", "decodeURIComponent",
        "encodeURI",
        "encodeURIComponent",

        // 15.1.4 Constructor Properties of the Global Object
        "Object", "Function", "Array", "String", "Boolean", "Number", "Date",
        "RegExp", "Error", "EvalError", "RangeError", "ReferenceError",
        "SyntaxError", "TypeError", "URIError",

        // 15.1.5 Other Properties of the Global Object
        "Math",

        // 10.1.6 Activation Object
        "arguments",

        // B.2 Additional Properties (non-normative)
        "escape", "unescape",

        // Window props (http://www.w3schools.com/HTMLDOM/dom_obj_window.asp)
        "closed", "defaultStatus", "document", "frames", "history", "length",
        "location", "name", "opener", "outerHeight", "outerWidth",
        "pageXOffset", "pageYOffset", "parent", "personalbar", "scrollbars",
        "self", "status", "statusbar", "toolbar", "top",

        // Window methods (http://www.w3schools.com/HTMLDOM/dom_obj_window.asp)
        "alert", "blur", "clearInterval", "clearTimeout", "close", "confirm",
        "createPopup", "focus", "moveBy", "moveTo", "open", "print", "prompt",
        "resizeBy", "resizeTo", "scrollBy", "scrollTo", "setInterval",
        "setTimeout",

        // IE event methods
        // (http://msdn.microsoft.com/en-us/library/ms535873(VS.85).aspx#)
        "onafterprint", "onbeforedeactivate", "onbeforeprint",
        "onbeforeunload", "onblur", "oncontrolselect", "ondeactivate",
        "onerror", "onfocus", "onhashchange  ", "onhelp", "onload", "onresize",
        "onresizeend", "onscroll",
        "onunload",

        // Common browser-defined identifiers not defined in ECMAScript
        "window", "dispatchEvent", "event", "external", "navigator", "screen",
        "Debug", "Enumerator", "Global", "Image", "ActiveXObject", "VBArray",
        "Components",

        // Functions commonly defined on Object
        "toString", "getClass", "constructor", "prototype",

        /*
         * These keywords trigger the loading of the java-plugin. For the
         * next-generation plugin, this results in starting a new Java process.
         */
        "java", "Packages", "netscape", "sun", "JavaObject", "JavaClass",
        "JavaArray", "JavaMember",

        // GWT-defined identifiers
        "$wnd", "$doc", "$moduleName", "$moduleBase", "$gwt_version",

        // Identifiers used by JsStackEmulator; later set to obfuscatable
        "$stack", "$stackDepth", "$location",

        // TODO: prove why this is necessary or remove it
        "call",};

    for (int i = 0; i < commonBuiltins.length; i++) {
      String ident = commonBuiltins[i];
      this.doCreateName(ident, ident);
    }
  }
}
