/*
 * Copyright 2006 Google Inc.
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
 * Java initialized local variable statement.
 */
public class JLocalDeclarationStatement extends JStatement {

  public final Holder initializer = new Holder();
  private final Holder/* <JLocalRef> */localRef = new Holder/* <JLocalRef> */();

  public JLocalDeclarationStatement(JProgram program, JLocalRef localRef,
      JExpression intializer) {
    super(program);
    this.localRef.set(localRef);
    this.initializer.set(intializer);
  }

  public JExpression getInitializer() {
    return initializer.get();
  }

  public JLocalRef getLocalRef() {
    return (JLocalRef) localRef.get();
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      localRef.traverse(visitor);
      initializer.traverse(visitor);
    }
    visitor.endVisit(this);
  }

}
