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
 * A namer that keeps the short ("pretty") identifier wherever possible.
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
   * Unobfuscatable names map to their full name and any names that conflict with a previous
   * idenitifier or any child scope's identifier are renamed. Otherwise the short name is
   * left as-is.
   * @return all names used in the given scope and any of its descendants (after renaming).
   */
  private Set<String> changeNames(JsScope scope) {

    // First, change the names in all the child scopes and remember that they're taken.
    Set<String> taken = new HashSet<String>();
    for (JsScope child : scope.getChildren()) {
      taken.addAll(changeNames(child));
    }

    // The next integer to try as an identifier suffix.
    HashMap<String, Integer> suffixCounters = new HashMap<String, Integer>();

    for (JsName name : scope.getAllNames()) {
      if (!referenced.contains(name)) {
        // Don't allocate idents for non-referenced names.
        continue;
      }
      rename(name, scope, taken, suffixCounters);
      taken.add(name.getShortIdent());
    }

    return taken;
  }

  /**
   * Changes the short identifier of one JsName.
   * @param taken the set of short identifiers that are already used (read-only)
   * @param suffixCounters contains the next suffix to use for each prefix (updated).
   */
  private void rename(JsName name, JsScope scope, Set<String> taken,
      HashMap<String, Integer> suffixCounters) {

    if (!name.isObfuscatable()) {
      // We must use the full name.
      name.setShortIdent(name.getIdent());
      return;
    }

    String prefix = name.getShortIdent();
    if (prefix.contains("-")) {
      // Fixes package-info.java classes.
      prefix = prefix.replace("-", "_");
      name.setShortIdent(prefix);
    }
    if (!isAvailable(prefix, scope, taken)) {

      // Start searching using a suffix hint stored in the scope.
      // We still do a search in case there is a collision with
      // a user-provided identifier

      Integer suffixOrNull = suffixCounters.get(prefix);
      int suffix = (suffixOrNull == null) ? 0 : suffixOrNull;

      String candidate;
      do {
        candidate = prefix + "_" + suffix++;
      } while (!isAvailable(candidate, scope, taken));

      suffixCounters.put(prefix, suffix);
      name.setShortIdent(candidate);
    }
    // otherwise the short name is already good
  }

  /**
   * Returns true if a candidate identifier is available in a scope.
   * @param taken the set of names that we've already used.
   */
  private boolean isAvailable(String candidate, JsScope scope, Set<String> taken) {
    if (!reserved.isAvailable(candidate)) {
      return false;
    }

    if (taken.contains(candidate)) {
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
