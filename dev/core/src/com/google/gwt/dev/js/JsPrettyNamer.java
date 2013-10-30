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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A namer that uses short, readable idents to maximize reability.
 */
public class JsPrettyNamer extends JsNamer {

  public static void exec(JsProgram program, PropertyOracle[] propertyOracles) {
    new JsPrettyNamer(program, propertyOracles).execImpl();
  }

  public JsPrettyNamer(JsProgram program, PropertyOracle[] propertyOracles) {
    super(program, propertyOracles);
  }

  @Override
  protected void reset() {
  }

  @Override
  protected void visit(JsScope scope) {
    changeNames(scope);
  }

  /**
   * Does a minimal fixup on the short names in the given scope and all its descendants.
   * Unobfuscatable names map to themselves and any names that conflict with a child scope
   * are renamed. Otherwise leaves the short names untouched.
   * @return every name used in the given scope or any of its descendants (after renaming).
   */
  private Set<String> changeNames(JsScope scope) {

    // First, change the names in all the child scopes.
    Set<String> childIdents = new HashSet<String>();
    for (JsScope child : scope.getChildren()) {
      childIdents.addAll(changeNames(child));
    }
    // childIdents now contains all idents my descendants are using.

    // The next integer to try as an identifier suffix.
    HashMap<String, Integer> suffixCounters = new HashMap<String, Integer>();

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

      String prefix = name.getShortIdent();
      if (!isLegal(scope, childIdents, prefix)) {

        // Start searching using a suffix hint stored in the scope.
        // We still do a search in case there is a collision with
        // a user-provided identifier

        Integer suffixOrNull = suffixCounters.get(prefix);
        int suffix = (suffixOrNull == null) ? 0 : suffixOrNull;

        String candidate;
        do {
          candidate = prefix + "_" + suffix++;
        } while (!isLegal(scope, childIdents, candidate));

        suffixCounters.put(prefix, suffix);
        name.setShortIdent(candidate);
      }
      // otherwise the short name is already good
    }

    // Finally, add the names used in this scope
    for (JsName name : scope.getAllNames()) {
      childIdents.add(name.getShortIdent());
    }
    return childIdents;
  }

  private boolean isLegal(JsScope scope, Set<String> childIdents, String candidate) {
    if (!isAvailableIdent(candidate)) {
      return false;
    }

    if (childIdents.contains(candidate)) {
      // one of my children already claimed this ident
      return false;
    }
    /*
     * Never obfuscate a name into an identifier that conflicts with an existing
     * unobfuscatable name! It's okay if it conflicts with an existing
     * obfuscatable name; that name will get obfuscated out of the way.
     */
    return (scope.findExistingUnobfuscatableName(candidate) == null);
  }
}
