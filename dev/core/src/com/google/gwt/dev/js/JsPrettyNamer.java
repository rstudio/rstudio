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

import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsRootScope;
import com.google.gwt.dev.js.ast.JsScope;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A namer that uses short, readable idents to maximize reability.
 */
public class JsPrettyNamer {

  public static void exec(JsProgram program) {
    new JsPrettyNamer(program).execImpl();
  }

  /**
   * Communicates to a parent scope all the idents used by all child scopes.
   */
  private Set childIdents = null;

  private final JsProgram program;

  public JsPrettyNamer(JsProgram program) {
    this.program = program;
  }

  private void execImpl() {
    visit(program.getRootScope());
  }

  private boolean isLegal(JsScope scope, Set childIdents, String newIdent) {
    if (JsKeywords.isKeyword(newIdent)) {
      return false;
    }
    if (childIdents.contains(newIdent)) {
      // one of my children already claimed this ident
      return false;
    }
    /*
     * Never obfuscate a name into an identifier that conflicts with an existing
     * unobfuscatable name! It's okay if it conflicts with an existing
     * obfuscatable name; that name will get obfuscated out of the way.
     */
    return (scope.findExistingUnobfuscatableName(newIdent) == null);
  }

  private void visit(JsScope scope) {
    // Save off the childIdents which is currently being computed for my parent.
    Set myChildIdents = childIdents;

    /*
     * Visit my children first. Reset childIdents so that my children will get a
     * clean slate: I do not communicate to my children.
     */
    childIdents = new HashSet();
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

      String newIdent = name.getShortIdent();
      if (!isLegal(scope, childIdents, newIdent)) {
        String checkIdent = newIdent;
        for (int i = 0; true; ++i) {
          checkIdent = newIdent + "_" + i;
          if (isLegal(scope, childIdents, checkIdent)) {
            break;
          }
        }
        name.setShortIdent(checkIdent);
      } else {
        // nothing to do; the short name is already good
      }
      childIdents.add(name.getShortIdent());
    }
    myChildIdents.addAll(childIdents);
    childIdents = myChildIdents;
  }
}
