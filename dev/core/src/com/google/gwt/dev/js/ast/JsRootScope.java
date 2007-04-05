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

  public JsProgram getProgram() {
    return program;
  }

  protected JsName doCreateName(String ident, String shortIdent) {
    JsName name = super.doCreateName(ident, shortIdent);
    name.setObfuscatable(false);
    return name;
  }

  private void ctorAddKnownGlobalSymbols() {
    // HACK: debugger is modelled as an ident even though it's really a keyword
    String[] commonBuiltins = new String[] {
        "ActiveXObject", "Array", "Boolean", "Date", "Debug", "Enumerator",
        "Error", "Function", "Global", "Image", "Math", "Number", "Object",
        "RegExp", "String", "VBArray", "window", "document", "event",
        "arguments", "call", "toString", "$wnd", "$doc", "$moduleName",
        "debugger", "undefined"};

    for (int i = 0; i < commonBuiltins.length; i++) {
      String ident = commonBuiltins[i];
      this.doCreateName(ident, ident);
    }
  }

}
