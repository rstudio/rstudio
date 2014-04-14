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

import com.google.gwt.dev.jjs.Correlation.Literal;
import com.google.gwt.dev.jjs.SourceInfo;

/**
 * Java class literal expression.
 *
 * NOTE: This class is modeled as if it were a JFieldRef to a field declared in
 * ClassLiteralHolder. That field contains the class object allocation
 * initializer.
 */
public class JClassLiteral extends JLiteral {

  private static SourceInfo addCorrelation(SourceInfo info) {
    info.addCorrelation(info.getCorrelator().by(Literal.CLASS));
    return info;
  }

  private JField field;

  private JType refType;

  public JClassLiteral(SourceInfo sourceInfo, JType type) {
    super(addCorrelation(sourceInfo));
    refType = type;
  }

  /**
   * Returns the field holding my allocated object.
   */
  public JField getField() {
    return field;
  }

  public JType getRefType() {
    return refType;
  }

  @Override
  public JType getType() {
    return field.getType();
  }

  /**
   * Resolve an external reference during AST stitching.
   */
  public void resolve(JType newType) {
    assert newType.replaces(refType);
    refType = newType;
  }

  /**
   * @param field the field to set
   */
  public void setField(JField field) {
    assert field != null;
    this.field = field;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }
}
