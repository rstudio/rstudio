/*
 * Copyright 2007 Google Inc.
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

import java.util.Collections;

/**
 * A special scope used only for catch blocks. It only holds a single symbol: the catch argument's
 * name.
 */
public class JsCatchScope extends JsNestingScope {

  private final JsName name;

  public JsCatchScope(JsScope parent, String ident) {
    super(parent, "Catch scope");
    this.name = new JsName(this, ident, ident);
  }

  @Override
  public Iterable<JsName> getAllNames() {
    return Collections.singleton(name);
  }

  @Override
  protected JsName doCreateName(String ident, String shortIdent) {
    // Declare into parent scope!
    return getParent().declareName(ident, shortIdent);
  }

  @Override
  protected JsName findExistingNameNoRecurse(String ident) {
    if (name.getIdent().equals(ident)) {
      return name;
    } else {
      return null;
    }
  }

}
