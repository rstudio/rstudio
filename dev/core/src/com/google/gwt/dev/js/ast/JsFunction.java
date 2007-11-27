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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a JavaScript function expression.
 */
public class JsFunction extends JsExpression implements HasName {

  protected JsBlock body;
  protected final List<JsParameter> params = new ArrayList<JsParameter>();
  protected final JsScope scope;
  private JsName name;
  private boolean fromJava;
  private boolean executeOnce;

  /**
   * Creates an anonymous function.
   */
  public JsFunction(JsScope parent) {
    this(parent, null, false);
  }

  /**
   * Creates a function that is not derived from Java source.
   */
  public JsFunction(JsScope parent, JsName name) {
    this(parent, name, false);
  }

  /**
   * Creates a named function, possibly derived from Java source.
   */
  public JsFunction(JsScope parent, JsName name, boolean fromJava) {
    assert (parent != null);
    this.fromJava = fromJava;
    setName(name);
    String scopeName = (name == null) ? "<anonymous>" : name.getIdent();
    scopeName = "function " + scopeName;
    this.scope = new JsScope(parent, scopeName);
  }

  public JsBlock getBody() {
    return body;
  }

  /**
   * If true, this indicates that only the first invocation of the function will
   * have any effects. Subsequent invocations may be considered to be no-op
   * calls whose return value is ignored.
   */
  public boolean getExecuteOnce() {
    return executeOnce;
  }

  public JsName getName() {
    return name;
  }

  public List<JsParameter> getParameters() {
    return params;
  }

  public JsScope getScope() {
    return scope;
  }

  public boolean isFromJava() {
    return fromJava;
  }

  public void setBody(JsBlock body) {
    this.body = body;
  }

  public void setExecuteOnce(boolean executeOnce) {
    this.executeOnce = executeOnce;
  }

  public void setFromJava(boolean fromJava) {
    this.fromJava = fromJava;
  }

  public void setName(JsName name) {
    this.name = name;
    if (name != null) {
      if (isFromJava()) {
        name.setStaticRef(this);
      }
    }
  }

  public void traverse(JsVisitor v, JsContext<JsExpression> ctx) {
    if (v.visit(this, ctx)) {
      v.acceptWithInsertRemove(params);
      body = v.accept(body);
    }
    v.endVisit(this, ctx);
  }
}
