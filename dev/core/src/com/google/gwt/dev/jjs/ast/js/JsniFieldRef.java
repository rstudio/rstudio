// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast.js;

import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JVisitor;

/**
 * JSNI reference to a Java field.
 */
public class JsniFieldRef extends JFieldRef {

  public JsniFieldRef(JProgram program, JField field,
      JReferenceType enclosingType) {
    super(program, null, field, enclosingType);
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
    }
    visitor.endVisit(this);
  }
}
