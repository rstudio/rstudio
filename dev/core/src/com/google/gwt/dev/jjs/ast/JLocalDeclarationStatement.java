// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java initialized local variable statement.
 */
public class JLocalDeclarationStatement extends JStatement {

  private final Holder/*<JLocalRef>*/ localRef = new Holder/*<JLocalRef>*/();
  public final Holder initializer = new Holder();

  public JLocalDeclarationStatement(JProgram program, JLocalRef localRef, JExpression intializer) {
    super(program);
    this.localRef.set(localRef);
    this.initializer.set(intializer);
  }

  public JLocalRef getLocalRef() {
    return (JLocalRef) localRef.get();
  }
  
  public JExpression getInitializer() {
    return initializer.get();
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      localRef.traverse(visitor);
      initializer.traverse(visitor);
    }
    visitor.endVisit(this);
  }

}
