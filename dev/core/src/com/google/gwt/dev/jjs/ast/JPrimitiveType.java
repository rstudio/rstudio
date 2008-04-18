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

/**
 * Base class for all Java primitive types.
 */
public class JPrimitiveType extends JType {

  private final String signatureName;
  private final String wrapperTypeName;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JPrimitiveType(JProgram program, String name, String signatureName,
      String wrapperTypeName, JLiteral defaultValue) {
    super(program, null, name, defaultValue);
    this.signatureName = signatureName;
    this.wrapperTypeName = wrapperTypeName;
  }

  /**
   * Returns a literal which has been coerced to this type, or <code>null</code>
   * if no such coercion is possible.
   */
  public JValueLiteral coerceLiteral(JValueLiteral value) {
    JLiteral defaultValue = getDefaultValue();
    if (defaultValue instanceof JValueLiteral) {
      JValueLiteral defaultValueLiteral = (JValueLiteral) defaultValue;
      return defaultValueLiteral.cloneFrom(value);
    }
    return null;
  }

  @Override
  public String getClassLiteralFactoryMethod() {
    return "Class.createForPrimitive";
  }

  public String getJavahSignatureName() {
    return signatureName;
  }

  public String getJsniSignatureName() {
    return signatureName;
  }

  public String getWrapperTypeName() {
    return wrapperTypeName;
  }

  public boolean isFinal() {
    return true;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }

}
