/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.collect.Maps;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * A normal scope that has a parent and children.
 */
public class JsNormalScope extends JsNestingScope {

  private Map<String, JsName> names = Collections.emptyMap();

  /**
   * Create a scope with parent.
   */
  public JsNormalScope(JsScope parent, String description) {
    super(parent, description);
  }

  /**
   * Returns an iterator for all the names defined by this scope.
   */
  @Override
  public Iterator<JsName> getAllNames() {
    return names.values().iterator();
  }

  /**
   * Creates a new name in this scope.
   */
  @Override
  protected JsName doCreateName(String ident, String shortIdent) {
    ident = StringInterner.get().intern(ident);
    shortIdent = StringInterner.get().intern(shortIdent);
    JsName name = new JsName(this, ident, shortIdent);
    names = Maps.putOrdered(names, ident, name);
    return name;
  }

  /**
   * Attempts to find the name object for the specified ident, searching in this scope only.
   * 
   * @return <code>null</code> if the identifier has no associated name
   */
  @Override
  protected JsName findExistingNameNoRecurse(String ident) {
    return names.get(ident);
  }
}
