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

import com.google.gwt.dev.util.collect.Lists;

import java.util.List;

/**
 * A normal scope that has a parent and children.
 */
public abstract class JsNestingScope extends JsScope {

  /**
   * Transient because children will add themselves to the parent after deserialization.
   */
  private transient List<JsScope> children = Lists.create();

  private JsScope parent;

  /**
   * Create a scope with parent.
   */
  public JsNestingScope(JsScope parent, String description) {
    super(description);
    assert (parent != null);
    this.parent = parent;
    parent.addChild(this);
  }

  /**
   * Returns a list of this scope's child scopes.
   */
  @Override
  public final List<JsScope> getChildren() {
    return children;
  }

  /**
   * Returns the parent scope of this scope, or <code>null</code> if this is the root scope.
   */
  @Override
  public final JsScope getParent() {
    return parent;
  }

  public void nestInto(JsScope newParent) {
    assert getParent() == JsRootScope.INSTANCE;
    parent = newParent;
    parent.addChild(this);
  }

  @Override
  protected final void addChild(JsScope child) {
    children = Lists.add(children, child);
  }

  protected Object readResolve() {
    children = Lists.create();
    parent.addChild(this);
    return this;
  }

}
