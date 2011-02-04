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
import com.google.gwt.dev.js.ast.JsScope;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A namer that uses short, readable idents to maximize reability.
 */
public class JsPrettyNamer extends JsNamer {

  public static void exec(JsProgram program) {
    new JsPrettyNamer(program).execImpl();
  }

  /**
   * Communicates to a parent scope all the idents used by all child scopes.
   */
  private Set<String> childIdents = null;

  public JsPrettyNamer(JsProgram program) {
    super(program);
  }

  @Override
  protected void reset() {
    childIdents = new HashSet<String>();
  }

  @Override
  protected void visit(JsScope scope) {
    // Save off the childIdents which is currently being computed for my parent.
    Set<String> myChildIdents = childIdents;

    /*
     * Visit my children first. Reset childIdents so that my children will get a
     * clean slate: I do not communicate to my children.
     */
    childIdents = new HashSet<String>();
    for (JsScope child : scope.getChildren()) {
      visit(child);
    }
    // Child idents now contains all idents my children are using.

    // The next integer to try as an identifier suffix.
    HashMap<String, Integer> startIdent = new HashMap<String, Integer>();

    // Visit all my idents.
    for (Iterator<JsName> it = scope.getAllNames(); it.hasNext();) {
      JsName name = it.next();
      if (!referenced.contains(name)) {
        // Don't allocate idents for non-referenced names.
        continue;
      }

      if (!name.isObfuscatable()) {
        // Unobfuscatable names become themselves.
        name.setShortIdent(name.getIdent());
        continue;
      }

      String newIdent = name.getShortIdent();
      if (!isLegal(scope, childIdents, newIdent)) {
        String checkIdent;

        // Start searching using a suffix hint stored in the scope.
        // We still do a search in case there is a collision with
        // a user-provided identifier
        Integer s = startIdent.get(newIdent);
        int suffix = (s == null) ? 0 : s.intValue();
        do {
          checkIdent = newIdent + "_" + suffix++;
        } while (!isLegal(scope, childIdents, checkIdent));
        startIdent.put(newIdent, suffix);
        name.setShortIdent(checkIdent);
      } else {
        // nothing to do; the short name is already good
      }
      childIdents.add(name.getShortIdent());
    }
    myChildIdents.addAll(childIdents);
    childIdents = myChildIdents;
  }

  private boolean isLegal(JsScope scope, Set<String> childIdents,
      String newIdent) {
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
}
