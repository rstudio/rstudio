/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.js.ast;

import com.google.gwt.dev.js.JsKeywords;
import com.google.gwt.dev.util.StringInterner;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * A scope is a factory for creating and allocating {@link com.google.gwt.dev.js.ast.JsName}s. A
 * JavaScript AST is built in terms of abstract name objects without worrying about obfuscation,
 * keyword/identifier blacklisting, and so on.
 * 
 * <p>
 * 
 * Scopes are associated with {@link com.google.gwt.dev.js.ast.JsFunction}s, but the two are not
 * equivalent. Functions <i>have</i> scopes, but a scope does not necessarily have an associated
 * Function. Examples of this include the {@link com.google.gwt.dev.js.ast.JsRootScope} and
 * synthetic scopes that might be created by a client.
 * 
 * <p>
 * 
 * Scopes can have parents to provide constraints when allocating actual identifiers for names.
 * Specifically, names in child scopes are chosen such that they do not conflict with names in their
 * parent scopes. The ultimate parent is usually the global scope (see
 * {@link com.google.gwt.dev.js.ast.JsProgram#getGlobalScope()}), but parentless scopes are useful
 * for managing names that are always accessed with a qualifier and could therefore never be
 * confused with the global scope hierarchy.
 */
public abstract class JsScope implements Serializable {

  /**
   * Prevents the client from programmatically creating an illegal ident.
   */
  private static String maybeMangleKeyword(String ident) {
    if (JsKeywords.isKeyword(ident)) {
      ident = ident + "_$";
    }
    return StringInterner.get().intern(ident);
  }

  private final String description;

  protected JsScope(String description) {
    this.description = StringInterner.get().intern(description);
  }

  /**
   * Gets a name object associated with the specified ident in this scope, creating it if necessary.
   * 
   * @param ident An identifier that is unique within this scope.
   */
  public final JsName declareName(String ident) {
    ident = maybeMangleKeyword(ident);
    JsName name = findExistingNameNoRecurse(ident);
    if (name != null) {
      return name;
    }
    return doCreateName(ident, ident);
  }

  /**
   * Gets a name object associated with the specified ident in this scope, creating it if necessary.
   * 
   * @param ident An identifier that is unique within this scope.
   * @param shortIdent A "pretty" name that does not have to be unique.
   * @throws IllegalArgumentException if ident already exists in this scope but the requested short
   *           name does not match the existing short name.
   */
  public final JsName declareName(String ident, String shortIdent) {
    ident = maybeMangleKeyword(ident);
    shortIdent = maybeMangleKeyword(shortIdent);
    JsName name = findExistingNameNoRecurse(ident);
    if (name != null) {
      if (!name.getShortIdent().equals(shortIdent)) {
        throw new IllegalArgumentException("Requested short name " + shortIdent
            + " conflicts with preexisting short name " + name.getShortIdent() + " for identifier "
            + ident);
      }
      return name;
    }
    return doCreateName(ident, shortIdent);
  }

  /**
   * Attempts to find the name object for the specified ident, searching in this scope, and if not
   * found, in the parent scopes.
   * 
   * @return <code>null</code> if the identifier has no associated name
   */
  public final JsName findExistingName(String ident) {
    ident = maybeMangleKeyword(ident);
    JsName name = findExistingNameNoRecurse(ident);
    if (name == null && getParent() != null) {
      return getParent().findExistingName(ident);
    }
    return name;
  }

  /**
   * Attempts to find an unobfuscatable name object for the specified ident, searching in this
   * scope, and if not found, in the parent scopes.
   * 
   * @return <code>null</code> if the identifier has no associated name
   */
  public final JsName findExistingUnobfuscatableName(String ident) {
    ident = maybeMangleKeyword(ident);
    JsName name = findExistingNameNoRecurse(ident);
    if (name != null && name.isObfuscatable()) {
      name = null;
    }
    if (name == null && getParent() != null) {
      return getParent().findExistingUnobfuscatableName(ident);
    }
    return name;
  }

  /**
   * Returns an iterator for all the names defined by this scope.
   */
  public abstract Iterator<JsName> getAllNames();

  /**
   * Returns a list of this scope's child scopes.
   */
  public abstract List<JsScope> getChildren();

  /**
   * Returns the parent scope of this scope, or <code>null</code> if this is the root scope.
   */
  public abstract JsScope getParent();

  @Override
  public final String toString() {
    if (getParent() != null) {
      return description + "->" + getParent();
    } else {
      return description;
    }
  }

  protected abstract void addChild(JsScope child);

  /**
   * Creates a new name in this scope.
   */
  protected abstract JsName doCreateName(String ident, String shortIdent);

  /**
   * Attempts to find the name object for the specified ident, searching in this scope only.
   * 
   * @return <code>null</code> if the identifier has no associated name
   */
  protected abstract JsName findExistingNameNoRecurse(String ident);
}
