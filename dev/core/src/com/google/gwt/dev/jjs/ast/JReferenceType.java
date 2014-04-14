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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.SourceInfo;

/**
 * Base class for any reference type.
 */
public abstract class JReferenceType extends JType implements CanBeAbstract {

  private transient JNonNullType nonNullType;

  public JReferenceType(SourceInfo info, String name) {
    super(info, name);
  }

  /**
   * Returns <code>true</code> if it's possible for this type to be
   * <code>null</code>.
   *
   * @see JNonNullType
   */
  public boolean canBeNull() {
    return true;
  }

  @Override
  public final JLiteral getDefaultValue() {
    return JNullLiteral.INSTANCE;
  }

  @Override
  public String getJavahSignatureName() {
    return "L" + name.replaceAll("_", "_1").replace('.', '_') + "_2";
  }

  @Override
  public String getJsniSignatureName() {
    return "L" + name.replace('.', '/') + ';';
  }

  public JNonNullType getNonNull() {
    if (nonNullType == null) {
      nonNullType = new JNonNullType(this);
    }
    return nonNullType;
  }

  public String getShortName() {
    int dotpos = name.lastIndexOf('.');
    return name.substring(dotpos + 1);
  }

  /**
   * If this type is a non-null type, returns the underlying (original) type.
   */
  public JReferenceType getUnderlyingType() {
    return this;
  }

  @Override
  public boolean replaces(JType originalType) {
    return super.replaces(originalType)
        && canBeNull() == ((JReferenceType) originalType).canBeNull();
  }
}
