/*
 * Copyright 2012 Google Inc.
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

package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.linker.impl.StandardSymbolData;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsVisitor;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Verify the validity of the symbol.
 *
 * This is by no means complete. At the Javascript AST, we no longer have the
 * knowledge we once had in the Java AST. Specify, given a name access.
 */
public class VerifySymbolMap extends JsVisitor {

  public static void exec(JsProgram jsProgram, JavaToJavaScriptMap jjsmap,
      Map<StandardSymbolData, JsName> symbolTable ) {
    new VerifySymbolMap(jjsmap, symbolTable).accept(jsProgram);
  }

  private final JavaToJavaScriptMap jjsmap;
  private final Set<String> nameMap = new HashSet<String>();

  private VerifySymbolMap(
      JavaToJavaScriptMap jjsmap,
      Map<StandardSymbolData, JsName> symbolTable) {
    this.jjsmap = jjsmap;
    for (Entry<StandardSymbolData, JsName> entry : symbolTable.entrySet()) {
      nameMap.add(entry.getValue().getIdent());
    }
  }

  @Override
  public void endVisit(JsNameRef x, JsContext ctx) {
    JsName name = x.getName();
    if (jjsmap.nameToField(name) != null ||
        jjsmap.nameToMethod(name) != null ||
        jjsmap.nameToType(name) != null) {
      String n = name.getIdent();
      if (!nameMap.contains(n)) {
        throw new InternalCompilerException("Missing symbol in SymbolMap: " + n);
      }
    }
  }
}
