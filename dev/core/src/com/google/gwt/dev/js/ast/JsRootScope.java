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
package com.google.gwt.dev.js.ast;

import com.google.gwt.dev.js.JsKeywords;

import java.util.HashMap;
import java.util.Map;

/**
 * The root scope is the parent of every scope. It is used to enforce
 * blacklisting of keywords at every scope.
 */
public final class JsRootScope extends JsScope {

  private final JsProgram program;

  private final Map/* <String, JsUnobfuscatableName> */unobfuscatableNames = new HashMap();

  public JsRootScope(JsProgram program) {
    this.program = program;
    ctorAddKnownGlobalSymbols();
  }

  // @Override
  public JsName findExistingName(String ident) {
    JsName name = (JsName) unobfuscatableNames.get(ident);
    if (name != null) {
      return name;
    }
    return super.findExistingName(ident);
  }

  // @Override
  public JsUnobfuscatableName getOrCreateUnobfuscatableName(String ident) {
    if (JsKeywords.isKeyword(ident.toCharArray())) {
      throw new IllegalArgumentException("Cannot create identifier " + ident
          + "; that name is a reserved word.");
    }
    JsUnobfuscatableName name = (JsUnobfuscatableName) unobfuscatableNames.get(ident);
    if (name == null) {
      name = new JsUnobfuscatableName(this, ident);
      unobfuscatableNames.put(ident, name);
    }
    return name;
  }

  // @Override
  public JsProgram getProgram() {
    return program;
  }

  // @Override
  public boolean hasUnobfuscatableName(String result) {
    return unobfuscatableNames.containsKey(result);
  }

  // @Override
  public String toString() {
    return getDescription();
  }

  JsName createSpecialUnobfuscatableName(String ident) {
    // this is for the debugger statement; ignore keyword restriction
    JsUnobfuscatableName name = (JsUnobfuscatableName) unobfuscatableNames.get(ident);
    if (name == null) {
      name = new JsUnobfuscatableName(this, ident);
      unobfuscatableNames.put(ident, name);
    }
    return name;
  }

  private void ctorAddKnownGlobalSymbols() {
    String[] commonBuiltins = new String[] {
        "ActiveXObject", "Array", "Boolean", "Date", "Debug", "Enumerator",
        "Error", "Function", "Global", "Image", "Math", "Number", "Object",
        "RegExp", "String", "VBArray", "window", "document", "event",
        "arguments", "call", "toString", "$wnd", "$doc", "$moduleName"};

    for (int i = 0; i < commonBuiltins.length; i++) {
      String ident = commonBuiltins[i];
      this.getOrCreateUnobfuscatableName(ident);
    }
  }

}
