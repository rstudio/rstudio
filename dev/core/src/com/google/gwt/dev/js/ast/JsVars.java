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
package com.google.gwt.dev.js.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A JavaScript <code>var</code> statement.
 */
public class JsVars extends JsStatement {

  /**
   * A var declared using the JavaScript <code>var</code> statement.
   */
  public static class JsVar implements HasName {

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

    private JsExpression initExpr;
    private final JsName name;
  }

  public JsVars() {
  }

  public void add(JsVar var) {
    vars.add(var);
  }

  // Iterator returns JsVar objects
  public Iterator iterator() {
    return vars.iterator();
  }

  public void traverse(JsVisitor v) {
    if (v.visit(this)) {
      for (Iterator iter = vars.iterator(); iter.hasNext();) {
        JsVar var = (JsVar) iter.next();
        JsExpression expr = var.getInitExpr();
        if (expr != null) {
          expr.traverse(v);
        }
      }
    }
    v.endVisit(this);
  }

  private final List vars = new ArrayList();
}
