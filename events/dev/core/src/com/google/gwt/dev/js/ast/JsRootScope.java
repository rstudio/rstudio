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

        // Common browser-defined identifiers not defined in ECMAScript
        "window", "document", "event", "location", "history", "external",
        "Debug", "Enumerator", "Global", "Image", "ActiveXObject", "VBArray",
        "Components",

        // Functions commonly defined on Object
        "toString", "getClass",

        // GWT-defined identifiers
        "$wnd", "$doc", "$moduleName", "$moduleBase", "$gwt_version",

        // TODO: prove why this is necessary or remove it
        "call",};

    for (int i = 0; i < commonBuiltins.length; i++) {
      String ident = commonBuiltins[i];
      this.doCreateName(ident, ident);
    }
  }
}
