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

/**
 * An abstract base class for named JavaScript objects.
 */
public abstract class JsName {

  /**
   * @param scope the scope in which this name is defined
   * @param ident the unmangled ident to use for this name
   */
  protected JsName(JsScope scope, String ident) {
    this.scope = scope;
    this.ident = ident;
  }

  public String getIdent() {
    return ident;
  }

  public JsScope getScope() {
    return scope;
  }

  public JsNameRef makeRef() {
    return new JsNameRef(this);
  }

  public String toString() {
    return ident;
  }

  public abstract boolean isObfuscatable();

  private final String ident;
  private final JsScope scope;
}
