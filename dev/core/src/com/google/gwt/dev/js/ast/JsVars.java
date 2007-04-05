/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.js.ast;

import java.util.Iterator;

/**
 * A JavaScript <code>var</code> statement.
 */
public class JsVars extends JsStatement {

  /**
   * A var declared using the JavaScript <code>var</code> statement.
   */
  public static class JsVar extends JsNode implements HasName {

    private JsExpression initExpr;

    private final JsName name;

    public JsVar(JsName name) {
      this.name = name;
    }

    public JsExpression getInitExpr() {
      return initExpr;
    }

    public JsName getName() {
      return name;
    }

    public void setInitExpr(JsExpression initExpr) {
      this.initExpr = initExpr;
    }

    public void traverse(JsVisitor v, JsContext ctx) {
      if (v.visit(this, ctx)) {
        if (initExpr != null) {
          initExpr = v.accept(initExpr);
        }
      }
      v.endVisit(this, ctx);
    }
  }

  private static class JsVarCollection extends JsCollection {
    
    public void add(JsVar expr) {
      super.addNode(expr);
    }

    public void add(int index, JsVar expr) {
      super.addNode(index, expr);
    }

    public JsVar get(int i) {
      return (JsVar) super.getNode(i);
    }

    public void set(int i, JsVar expr) {
      super.setNode(i, expr);
    }
  }
  
  private final JsVarCollection vars = new JsVarCollection();

  public JsVars() {
  }

  public void add(JsVar var) {
    vars.add(var);
  }

  public boolean isEmpty() {
    return vars.isEmpty();
  }

  // Iterator returns JsVar objects
  public Iterator iterator() {
    return vars.iterator();
  }

  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      v.acceptWithInsertRemove(vars);
    }
    v.endVisit(this, ctx);
  }
}
