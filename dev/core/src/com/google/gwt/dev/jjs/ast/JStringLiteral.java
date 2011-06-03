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
 * Java literal expression that evaluates to a string.
 */
public class JStringLiteral extends JValueLiteral {

  private JClassType stringType;
  private final String value;

  public JStringLiteral(SourceInfo sourceInfo, String value, JClassType stringType) {
    super(sourceInfo);
    this.value = value;
    this.stringType = stringType;
    assert stringType.getName().equals("java.lang.String");
  }

  @Override
  public JValueLiteral cloneFrom(JValueLiteral value) {
    throw new UnsupportedOperationException();
  }

  public JNonNullType getType() {
    return stringType.getNonNull();
  }

  public String getValue() {
    return value;
  }

  @Override
  public Object getValueObj() {
    return value;
  }

  /**
   * Resolve an external references during AST stitching.
   */
  public void resolve(JClassType stringType) {
    assert stringType.replaces(this.stringType);
    this.stringType = stringType;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }
}
