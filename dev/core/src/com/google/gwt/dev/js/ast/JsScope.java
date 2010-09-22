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
package com.google.gwt.dev.js.ast;

import com.google.gwt.dev.js.JsKeywords;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.collect.Maps;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A scope is a factory for creating and allocating
 * {@link com.google.gwt.dev.js.ast.JsName}s. A JavaScript AST is built in
 * terms of abstract name objects without worrying about obfuscation,
 * keyword/identifier blacklisting, and so on.
 * 
 * <p>
 * 
 * Scopes are associated with {@link com.google.gwt.dev.js.ast.JsFunction}s,
 * but the two are not equivalent. Functions <i>have</i> scopes, but a scope
 * does not necessarily have an associated Function. Examples of this include
 * the {@link com.google.gwt.dev.js.ast.JsRootScope} and synthetic scopes that
 * might be created by a client.
 * 
 * <p>
 * 
 * Scopes can have parents to provide constraints when allocating actual
 * identifiers for names. Specifically, names in child scopes are chosen such
 * that they do not conflict with names in their parent scopes. The ultimate
 * parent is usually the global scope (see
 * {@link com.google.gwt.dev.js.ast.JsProgram#getGlobalScope()}), but
 * parentless scopes are useful for managing names that are always accessed with
 * a qualifier and could therefore never be confused with the global scope
 * hierarchy.
 */
public class JsScope implements Serializable {

  /**
   * Prevents the client from programmatically creating an illegal ident.
   */
  private static String maybeMangleKeyword(String ident) {
    if (JsKeywords.isKeyword(ident)) {
      ident = ident + "_$";
    }
    return ident;
  }

  private List<JsScope> children = Collections.emptyList();
  private final String description;
  private Map<String, JsName> names = Collections.emptyMap();
  private final JsScope parent;

  /**
   * Create a scope with parent.
   */
  public JsScope(JsScope parent, String description) {
    assert (parent != null);
    this.description = description;
    this.parent = parent;
    parent.children = Lists.add(parent.children, this);
  }

  /**
   * Subclasses can be parentless.
   */
  protected JsScope(String description) {
    this.description = description;
    this.parent = null;
  }

  /**
   * Gets a name object associated with the specified ident in this scope,
   * creating it if necessary.
   * 
   * @param ident An identifier that is unique within this scope.
   */
  public JsName declareName(String ident) {
    ident = maybeMangleKeyword(ident);
    JsName name = findExistingNameNoRecurse(ident);
    if (name != null) {
      return name;
    }
    return doCreateName(ident, ident);
  }

  /**
   * Gets a name object associated with the specified ident in this scope,
   * creating it if necessary.
   * 
   * @param ident An identifier that is unique within this scope.
   * @param shortIdent A "pretty" name that does not have to be unique.
   * @throws IllegalArgumentException if ident already exists in this scope but
   *           the requested short name does not match the existing short name.
   */
  public JsName declareName(String ident, String shortIdent) {
    ident = maybeMangleKeyword(ident);
    shortIdent = maybeMangleKeyword(shortIdent);
    JsName name = findExistingNameNoRecurse(ident);
    if (name != null) {
      if (!name.getShortIdent().equals(shortIdent)) {
        throw new IllegalArgumentException("Requested short name " + shortIdent
            + " conflicts with preexisting short name " + name.getShortIdent()
            + " for identifier " + ident);
      }
      return name;
    }
    return doCreateName(ident, shortIdent);
  }

  /**
   * Attempts to find the name object for the specified ident, searching in this
   * scope, and if not found, in the parent scopes.
   * 
   * @return <code>null</code> if the identifier has no associated name
   */
  public final JsName findExistingName(String ident) {
    ident = maybeMangleKeyword(ident);
    JsName name = findExistingNameNoRecurse(ident);
    if (name == null && parent != null) {
      return parent.findExistingName(ident);
    }
    return name;
  }

  /**
   * Attempts to find an unobfuscatable name object for the specified ident,
   * searching in this scope, and if not found, in the parent scopes.
   * 
   * @return <code>null</code> if the identifier has no associated name
   */
  public final JsName findExistingUnobfuscatableName(String ident) {
    ident = maybeMangleKeyword(ident);
    JsName name = findExistingNameNoRecurse(ident);
    if (name != null && name.isObfuscatable()) {
      name = null;
    }
    if (name == null && parent != null) {
      return parent.findExistingUnobfuscatableName(ident);
    }
    return name;
  }

  /**
   * Returns an iterator for all the names defined by this scope.
   */
  public Iterator<JsName> getAllNames() {
      return names.values().iterator();
  }

  /**
   * Returns a list of this scope's child scopes.
   */
  public final List<JsScope> getChildren() {
    return children;
  }

  /**
   * Returns the parent scope of this scope, or <code>null</code> if this is
   * the root scope.
   */
  public final JsScope getParent() {
    return parent;
  }

  /**
   * Returns the associated program.
   */
  public JsProgram getProgram() {
    assert (parent != null) : "Subclasses must override getProgram() if they do not set a parent";
    return parent.getProgram();
  }

  @Override
  public final String toString() {
    if (parent != null) {
      return description + "->" + parent;
    } else {
      return description;
    }
  }

  /**
   * Creates a new name in this scope.
   */
  protected JsName doCreateName(String ident, String shortIdent) {
    JsName name = new JsName(this, ident, shortIdent);
    names = Maps.putOrdered(names, ident, name);
    return name;
  }

  /**
   * Attempts to find the name object for the specified ident, searching in this
   * scope only.
   * 
   * @return <code>null</code> if the identifier has no associated name
   */
  protected JsName findExistingNameNoRecurse(String ident) {
    return names.get(ident);
  }

}
