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
package com.google.gwt.dev.js;

import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsRootScope;
import com.google.gwt.dev.js.ast.JsScope;

import java.util.Iterator;
import java.util.List;

/**
 * A namer that uses long, fully qualified names for maximum unambiguous
 * debuggability.
 */
public class JsVerboseNamer {

  public static void exec(JsProgram program) {
    new JsVerboseNamer(program).execImpl();
  }

  private final JsProgram program;

  public JsVerboseNamer(JsProgram program) {
    this.program = program;
  }

  private void execImpl() {
    visit(program.getRootScope());
  }

  private boolean isLegal(JsScope scope, String newIdent) {
    // only keywords are forbidden
    return !JsKeywords.isKeyword(newIdent);
  }

  private void visit(JsScope scope) {
    // Visit children.
    List children = scope.getChildren();
    for (Iterator it = children.iterator(); it.hasNext();) {
      visit((JsScope) it.next());
    }

    JsRootScope rootScope = program.getRootScope();
    if (scope == rootScope) {
      return;
    }

    // Visit all my idents.
    for (Iterator it = scope.getAllNames(); it.hasNext();) {
      JsName name = (JsName) it.next();
      if (!name.isObfuscatable()) {
        // Unobfuscatable names become themselves.
        name.setShortIdent(name.getIdent());
        continue;
      }

      String fullIdent = name.getIdent();
      if (!isLegal(scope, fullIdent)) {
        String checkIdent = fullIdent;
        for (int i = 0; true; ++i) {
          checkIdent = fullIdent + "_" + i;
          if (isLegal(scope, checkIdent)) {
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
}
