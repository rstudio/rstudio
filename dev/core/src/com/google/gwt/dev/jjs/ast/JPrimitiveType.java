// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Base class for all Java primitive types.  
 */
public class JPrimitiveType extends JType {

  private final String signatureName;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JPrimitiveType(JProgram program, String name, String signatureName, JLiteral defaultValue) {
    super(program, name, defaultValue);
    this.signatureName = signatureName;
  }

  public String getJavahSignatureName() {
    return signatureName;
  }

  public String getJsniSignatureName() {
    return signatureName;
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
    }
    visitor.endVisit(this);
  }

}
