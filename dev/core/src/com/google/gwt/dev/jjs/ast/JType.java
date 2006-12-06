// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Base class for any types entity.
 */
public abstract class JType extends JNode implements HasName {

  public String name;
  public JLiteral defaultValue;

  public JType(JProgram program, String name, JLiteral defaultValue) {
    super(program);
    this.name = name;
    this.defaultValue = defaultValue;
  }

  public String getName() {
    return name;
  }
  
  public abstract String getJavahSignatureName();
  
  public abstract String getJsniSignatureName();

  public JLiteral getDefaultValue() {
    return defaultValue;
  }

}
