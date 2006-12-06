// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Base class for any storage location.
 */
public abstract class JVariable extends JNode implements CanBeFinal, HasName,
    HasType, HasSettableType {

  public JType type;
  public String name;
  public boolean isFinal;

  JVariable(JProgram program, String name, JType type, boolean isFinal) {
    super(program);
    this.name = name;
    this.type = type;
    this.isFinal = isFinal;
  }

  public String getName() {
    return name;
  }

  public JType getType() {
    return type;
  }

  public void setType(JType newType) {
    type = newType;
  }

  public boolean isFinal() {
    return isFinal;
  }

}
