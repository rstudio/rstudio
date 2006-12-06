// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast.js;

import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.js.ast.JsFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * A Java native method that is implemented in JSNI code.  
 */
public class JsniMethod extends JMethod {

  private JsFunction jsFunction = null;
  public final List/*<JsniFieldRef>*/ jsniFieldRefs = new ArrayList/*<JsniFieldRef>*/();
  public final List/*<JsniMethodRef>*/ jsniMethodRefs = new ArrayList/*<JsniMethodRef>*/();

  public JsniMethod(JProgram program, String name,
      JReferenceType enclosingType, JType returnType, boolean isStatic,
      boolean isFinal, boolean isPrivate) {
    super(program, name, enclosingType, returnType, false, isStatic, isFinal,
      isPrivate);
  }

  public boolean isNative() {
    return true;
  }

  public JsFunction getFunc() {
    assert (this.jsFunction != null);
    return jsFunction;
  }

  public void setFunc(JsFunction jsFunction) {
    assert (this.jsFunction == null);
    this.jsFunction = jsFunction;
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      for (int i = 0; i < params.size(); ++i) {
        JParameter param = (JParameter) params.get(i);
        param.traverse(visitor);
      }
      for (int i = 0; i < jsniFieldRefs.size(); ++i) {
        JsniFieldRef fieldRef = (JsniFieldRef) jsniFieldRefs.get(i);
        fieldRef.traverse(visitor);
      }
      for (int i = 0; i < jsniMethodRefs.size(); ++i) {
        JsniMethodRef methodRef = (JsniMethodRef) jsniMethodRefs.get(i);
        methodRef.traverse(visitor);
      }
    }
    visitor.endVisit(this);
  }

}
