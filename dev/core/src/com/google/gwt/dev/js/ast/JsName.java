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
package com.google.gwt.dev.js.ast;

/**
 * An abstract base class for named JavaScript objects.
 */
public class JsName {

  private final String ident;
  private boolean isObfuscatable;
  private String shortIdent;

  /**
   * @param ident the unmangled ident to use for this name
   */
  JsName(String ident, String shortIdent) {
    this.ident = ident;
    this.shortIdent = shortIdent;
    this.isObfuscatable = true;
  }

  public String getIdent() {
    return ident;
  }

  public String getShortIdent() {
    return shortIdent;
  }

  public boolean isObfuscatable() {
    return isObfuscatable;
  }

  public JsNameRef makeRef() {
    return new JsNameRef(this);
  }

  public void setObfuscatable(boolean isObfuscatable) {
    this.isObfuscatable = isObfuscatable;
  }

  public void setShortIdent(String shortIdent) {
    this.shortIdent = shortIdent;
  }

  @Override
  public String toString() {
    return ident;
  }

}
