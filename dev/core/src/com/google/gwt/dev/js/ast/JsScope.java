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
package com.google.gwt.dev.js.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A scope is a factory for creating and allocating
 * {@link com.google.gwt.compiler.jjs.jsc.JsName}s. A JavaScript AST is built in
 * terms of abstract name objects without worrying about obfuscation,
 * keyword/identifier blacklisting, and so on.
 * 
 * <p>
 * 
 * Scopes are associated with {@link com.google.gwt.dev.js.ast.JsFunction}s, but
 * the two are not equivalent. Functions <i>have</i> scopes, but a scope does
 * not necessarily have an associated Function. Examples of this include the
 * {@link com.google.gwt.dev.js.ast.JsRootScope} and synthetic scopes that might
 * be created by a client.
 * 
 * <p>
 * 
 * Scopes can have parents to provide constraints when allocating actual
 * identifiers for names. Specifically, names in child scopes are chosen such
 * that they do not conflict with names in their parent scopes. The ultimate
 * parent is usually the global scope (see
 * {@link com.google.gwt.compiler.jjs.jsc.JsProgram#getGlobalScope()}), but
 * parentless scopes are useful for managing names that are always accessed with
 * a qualifier and could therefore never be confused with the global scope
 * heirarchy.
 */
public class JsScope {

  /**
   * Create a scope with parent.
   */
  public JsScope(JsScope parent) {
    assert (parent != null);
    this.parent = parent;
    this.parent.children.add(this);
  }

  /**
   * Subclasses can be parentless.
   */
  protected JsScope() {
    this.parent = null;
  }

  /**
   * Creates an obfuscatable name object associated with the specified ident in
   * this scope.
   * 
   * @param ident An identifier that must be unique within this scope.
   * @throws IllegalArgumentException if ident already exists in this scope.
   */
  public JsObfuscatableName createUniqueObfuscatableName(String ident) {
    if (obfuscatableNames.containsKey(ident)) {
      throw new IllegalArgumentException("Identifier already in use: " + ident);
    }
    return getOrCreateObfuscatableName(ident, ident);
  }

  /**
   * Creates an obfuscatable name object associated with the specified ident in
   * this scope.
   * 
   * @param ident An identifier that must be unique within this scope.
   * @param shortIdent A "pretty" name that does not have to be unique.
   * @throws IllegalArgumentException if ident already exists in this scope.
   */
  public JsObfuscatableName createUniqueObfuscatableName(String ident,
      String shortIdent) {
    if (obfuscatableNames.containsKey(ident)) {
      throw new IllegalArgumentException("Identifier already in use: " + ident);
    }
    return getOrCreateObfuscatableName(ident, shortIdent);
  }

  /**
   * Attempts to find the name object for the specified ident, searching in this
   * scope, and if not found, in the parent scopes.
   * 
   * @return <code>null</code> if the identifier has no associated name
   */
  public JsName findExistingName(String ident) {
    JsName name = (JsName) obfuscatableNames.get(ident);
    if (name != null) {
      return name;
    }

    if (parent != null) {
      return parent.findExistingName(ident);
    }

    return null;
  }

  public List/* <JsScope> */getChildren() {
    return children;
  }

  /**
   * Gets an obfuscatable name object associated with the specified ident in
   * this scope, creating it if necessary.
   * 
   * @param ident An identifier that is unique within this scope.
   */
  public JsObfuscatableName getOrCreateObfuscatableName(String ident) {
    JsObfuscatableName name = (JsObfuscatableName) obfuscatableNames.get(ident);
    if (name == null) {
      name = new JsObfuscatableName(this, ident, ident);
      obfuscatableNames.put(ident, name);
    }
    return name;
  }

  /**
   * Gets an obfuscatable name object associated with the specified ident in
   * this scope, creating it if necessary.
   * 
   * @param ident An identifier that is unique within this scope.
   * @param shortIdent A "pretty" name that does not have to be unique.
   * @throws IllegalArgumentException if ident already exists in this scope but
   *           the requested short name does not match the existing short name.
   */
  public JsObfuscatableName getOrCreateObfuscatableName(String ident,
      String shortIdent) {
    JsObfuscatableName name = (JsObfuscatableName) obfuscatableNames.get(ident);
    if (name == null) {
      name = new JsObfuscatableName(this, ident, shortIdent);
      obfuscatableNames.put(ident, name);
    } else {
      if (!name.getShortIdent().equals(shortIdent)) {
        throw new IllegalArgumentException("Requested short name " + shortIdent
          + " conflicts with preexisting short name " + name.getShortIdent()
          + " for identifier " + ident);
      }
    }
    return name;
  }

  /**
   * Gets an unobfuscatable name object associated with the specified ident in
   * this scope, creating it if necessary.
   * 
   * @param ident An identifier that is unique within this scope.
   * @throws IllegalArgumentException if ident is a reserved word.
   */
  public JsUnobfuscatableName getOrCreateUnobfuscatableName(String ident) {
    assert (parent != null) : "Subclasses must override getOrCreateUnobfuscatableName() if they do not set a parent";
    return parent.getOrCreateUnobfuscatableName(ident);
  }

  public JsScope getParent() {
    return parent;
  }

  public JsProgram getProgram() {
    assert (parent != null) : "Subclasses must override getProgram() if they do not set a parent";
    return parent.getProgram();
  }

  public boolean hasUnobfuscatableName(String ident) {
    assert (parent != null) : "Subclasses must override hasUnobfuscatableName() if they do not set a parent";
    return parent.hasUnobfuscatableName(ident);
  }

  public void setDescription(String desc) {
    this.description = desc;
  }

  public String toString() {
    assert (parent != null) : "Subclasses must override toString() if they do not set a parent";
    return description + "->" + parent;
  }

  protected String getDescription() {
    return description;
  }

  private final List/* <JsScope> */children = new ArrayList();
  private String description;
  private final Map/* <String, JsObfuscatableName> */obfuscatableNames = new HashMap();
  private final JsScope parent;

}
