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
package com.google.gwt.dev.js;

import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsScope;

/**
 * A namer that uses long, fully qualified names for maximum unambiguous
 * debuggability.
 */
public class JsVerboseNamer extends JsNamer {

  public static void exec(JsProgram program, PropertyOracle[] propertyOracles) {
    new JsVerboseNamer(program, propertyOracles).execImpl();
  }

  public JsVerboseNamer(JsProgram program, PropertyOracle[] propertyOracles) {
    super(program, propertyOracles);
  }

  @Override
  protected void reset() {
    // Nothing to do.
  }

  @Override
  protected void visit(JsScope scope) {
    // Visit children.
    for (JsScope child : scope.getChildren()) {
      visit(child);
    }

    // Visit all my idents.
    for (JsName name : scope.getAllNames()) {
      if (!referenced.contains(name)) {
        // Don't allocate idents for non-referenced names.
        continue;
      }

      if (!name.isObfuscatable()) {
        // Unobfuscatable names become themselves.
        name.setShortIdent(name.getIdent());
        continue;
      }

      String fullIdent = name.getIdent();
      // Fixes package-info.java classes.
      fullIdent = fullIdent.replace("-", "_");
      if (!isLegal(fullIdent)) {
        String checkIdent;
        for (int i = 0; true; ++i) {
          checkIdent = fullIdent + "_" + i;
          if (isLegal(checkIdent)) {
            break;
          }
        }
        name.setShortIdent(checkIdent);
      } else {
        // set each name's short ident to its full ident
        name.setShortIdent(fullIdent);
      }
    }
  }

  protected boolean isLegal(String newIdent) {
    return reserved.isAvailable(newIdent);
  }
}
