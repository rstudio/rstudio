/*
 * Copyright 2008 Google Inc.
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
 * A namer that uses short, unrecognizable idents to minimize generated code
 * size.
 */
public class JsObfuscateNamer extends JsNamer implements FreshNameGenerator {

  /**
   * A lookup table of base-64 chars we use to encode idents.
   */
  private static final char[] sBase64Chars = new char[]{
      'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
      'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B',
      'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
      'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '$', '_', '0', '1',
      '2', '3', '4', '5', '6', '7', '8', '9'};

  public static FreshNameGenerator exec(JsProgram program) {
    return exec(program, null);
  }

  public static FreshNameGenerator exec(JsProgram program, PropertyOracle[] propertyOracles) {
    JsObfuscateNamer namer = new JsObfuscateNamer(program, propertyOracles);
    namer.execImpl();
    return namer;
  }

  /**
   * Returns a valid unused obfuscated top scope name by keeping track of the last (highest)
   * name produced.
   */
  @Override
  public String getFreshName() {
    String newIdent;
    while (true) {
      // Get the next possible obfuscated name
      newIdent = makeObfuscatedIdent(maxId++);
      if (isLegal(program.getScope(), newIdent)) {
        break;
      }
    }
    return newIdent;
  }

  /**
   * Communicates to a parent scope the maximum id used by any of its children.
   */
  private int maxChildId = 0;

  /**
   * Remember the maximum ChildIdAssigned so that new names can safely be obtained without
   * running the global renaming again.
   */
  private int maxId = -1;
  /**
   * A temp buffer big enough to hold at least 32 bits worth of base-64 chars.
   */
  private final char[] sIdentBuf = new char[6];

  public JsObfuscateNamer(JsProgram program, PropertyOracle[] propertyOracles) {
    super(program, propertyOracles);
  }

  @Override
  protected void reset() {
    maxChildId = 0;
  }

  @Override
  protected void visit(JsScope scope) {
    // Save off the maxChildId which is currently being computed for my parent.
    int mySiblingsMaxId = maxChildId;

    /*
     * Visit my children first. Reset maxChildId so that my children will get a
     * clean slate: I do not communicate to my children.
     */
    maxChildId = 0;
    for (JsScope child : scope.getChildren()) {
      visit(child);
    }
    // maxChildId is now the max of all of my children's ids

    // Visit my idents.
    int curId = maxChildId;
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

      String newIdent;
      while (true) {
        // Get the next possible obfuscated name
        newIdent = makeObfuscatedIdent(curId++);
        if (isLegal(scope, newIdent)) {
          break;
        }
      }
      name.setShortIdent(newIdent);
    }

    maxChildId = Math.max(mySiblingsMaxId, curId);
    maxId = Math.max(maxId, maxChildId);
  }

  private boolean isLegal(JsScope scope, String newIdent) {
    if (!reserved.isAvailable(newIdent)) {
      return false;
    }
    /*
     * Never obfuscate a name into an identifier that conflicts with an existing
     * unobfuscatable name! It's okay if it conflicts with an existing
     * obfuscatable name, since that name will get obfuscated to something else
     * anyway.
     */
    return (scope.findExistingUnobfuscatableName(newIdent) == null);
  }

  private String makeObfuscatedIdent(int id) {
    // Use base-54 for the first character of the identifier,
    // so that we don't use any numbers (which are illegal at
    // the beginning of an identifier).
    //
    int i = 0;
    sIdentBuf[i++] = sBase64Chars[id % 54];
    id /= 54;

    // Use base-64 for the rest of the identifier.
    //
    while (id != 0) {
      sIdentBuf[i++] = sBase64Chars[id & 0x3f];
      id >>= 6;
    }

    return new String(sIdentBuf, 0, i);
  }
}
