// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast.js;

import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVisitor;

/**
 * A call to a JSNI method.
 */
public class JsniMethodRef extends JMethodCall {

  public JsniMethodRef(JProgram program, JMethod method) {
    super(program, null, method);
    setCanBePolymorphic(true);
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
    }
    visitor.endVisit(this);
  }
}
