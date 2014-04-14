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

import java.io.Serializable;

/**
 * A well-known name in the root scope.
 */
public class JsRootName extends JsName {

  private static class SerializedForm implements Serializable {
    private final String ident;

    public SerializedForm(String ident) {
      this.ident = ident;
    }

    private Object readResolve() {
      return JsRootScope.INSTANCE.findExistingName(ident);
    }
  }

  JsRootName(JsRootScope rootScope, String ident) {
    super(rootScope, ident, ident);
    super.setObfuscatable(false);
  }

  @Override
  public void setObfuscatable(boolean isObfuscatable) {
    throw new UnsupportedOperationException("Root names are immutable");
  }

  @Override
  public void setShortIdent(String shortIdent) {
    throw new UnsupportedOperationException("Root names are immutable");
  }

  @Override
  public void setStaticRef(JsNode node) {
    throw new UnsupportedOperationException("Root names are immutable");
  }

  private Object writeReplace() {
    return new SerializedForm(getIdent());
  }

}
