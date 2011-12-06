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

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.util.StringInterner;

import java.io.Serializable;

/**
 * An abstract base class for named JavaScript objects.
 */
public class JsName implements Serializable {
  private final JsScope enclosing;
  private final String ident;
  private boolean isObfuscatable;
  private String shortIdent;

  /**
   * A back-reference to the JsNode that the JsName refers to.
   */
  private JsNode staticRef;

  /**
   * @param ident the unmangled ident to use for this name
   */
  JsName(JsScope enclosing, String ident, String shortIdent) {
    this.enclosing = enclosing;
    this.ident = StringInterner.get().intern(ident);
    this.shortIdent = StringInterner.get().intern(shortIdent);
    this.isObfuscatable = true;
  }

  public JsScope getEnclosing() {
    return enclosing;
  }

  public String getIdent() {
    return ident;
  }

  public String getShortIdent() {
    return shortIdent;
  }

  public JsNode getStaticRef() {
    return staticRef;
  }

  public boolean isObfuscatable() {
    return isObfuscatable;
  }

  public JsNameRef makeRef(SourceInfo sourceInfo) {
    return new JsNameRef(sourceInfo, this);
  }

  public void setObfuscatable(boolean isObfuscatable) {
    this.isObfuscatable = isObfuscatable;
  }

  public void setShortIdent(String shortIdent) {
    this.shortIdent = StringInterner.get().intern(shortIdent);
  }

  /**
   * Should never be called except on immutable stuff.
   */
  public void setStaticRef(JsNode node) {
    this.staticRef = node;
  }

  @Override
  public String toString() {
    return ident;
  }
}
