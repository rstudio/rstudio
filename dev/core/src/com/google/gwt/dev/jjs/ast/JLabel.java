// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Should we have a JLabelRef also?
 */
public class JLabel extends JNode implements HasName {

  public final String name;
  
  public JLabel(JProgram program, String name) {
    super(program);
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
    }
    visitor.endVisit(this);
  }

}
