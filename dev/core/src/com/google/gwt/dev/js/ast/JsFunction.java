/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.js.ast;

import com.google.gwt.dev.jjs.SourceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a JavaScript function expression.
 */
public final class JsFunction extends JsLiteral implements HasName {

  private static void trace(String title, String code) {
    System.out.println("---------------------------");
    System.out.println(title + ":");
    System.out.println("---------------------------");
    System.out.println(code);
  }

  protected JsBlock body;
  protected final List<JsParameter> params = new ArrayList<JsParameter>();
  protected final JsScope scope;
  private boolean artificiallyRescued;
  private boolean executeOnce;
  private boolean fromJava;
  private JsFunction impliedExecute;
  private JsName name;
  private boolean trace = false;
  private boolean traceFirst = true;

  /**
   * Creates an anonymous function.
   */
  public JsFunction(SourceInfo sourceInfo, JsScope parent) {
    this(sourceInfo, parent, null, false);
  }

  /**
   * Creates a function that is not derived from Java source.
   */
  public JsFunction(SourceInfo sourceInfo, JsScope parent, JsName name) {
    this(sourceInfo, parent, name, false);
  }

  /**
   * Creates a named function, possibly derived from Java source.
   */
  public JsFunction(SourceInfo sourceInfo, JsScope parent, JsName name, boolean fromJava) {
    super(sourceInfo);
    assert (parent != null);
    this.fromJava = fromJava;
    setName(name);
    String scopeName = (name == null) ? "<anonymous>" : name.getIdent();
    scopeName = "function " + scopeName;
    this.scope = new JsNormalScope(parent, scopeName);
  }

  public JsBlock getBody() {
    return body;
  }

  /**
   * If true, this indicates that only the first invocation of the function will have any effects.
   * Subsequent invocations may be considered to be no-op calls whose return value is ignored.
   */
  public boolean getExecuteOnce() {
    return executeOnce;
  }

  public JsFunction getImpliedExecute() {
    return impliedExecute;
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.FUNCTION;
  }

  @Override
  public JsName getName() {
    return name;
  }

  public List<JsParameter> getParameters() {
    return params;
  }

  public JsScope getScope() {
    return scope;
  }

  @Override
  public boolean hasSideEffects() {
    // If there's a name, the name is assigned to.
    return name != null;
  }

  public boolean isArtificiallyRescued() {
    return artificiallyRescued;
  }

  @Override
  public boolean isBooleanFalse() {
    return false;
  }

  @Override
  public boolean isBooleanTrue() {
    return true;
  }

  @Override
  public boolean isDefinitelyNotNull() {
    return true;
  }

  @Override
  public boolean isDefinitelyNull() {
    return false;
  }

  public boolean isFromJava() {
    return fromJava;
  }

  public void setArtificiallyRescued(boolean rescued) {
    this.artificiallyRescued = rescued;
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

  public void setImpliedExecute(JsFunction impliedExecute) {
    this.impliedExecute = impliedExecute;
  }

  public void setName(JsName name) {
    this.name = name;
    if (name != null) {
      if (isFromJava()) {
        name.setStaticRef(this);
      }
    }
  }

  public void setTrace() {
    this.trace = true;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    String before = null;
    if (trace && v instanceof JsModVisitor) {
      before = this.toSource();
      if (traceFirst) {
        traceFirst = false;
        trace("SCRIPT INITIAL", before);
      }
    }
    if (v.visit(this, ctx)) {
      v.acceptWithInsertRemove(params);
      body = v.accept(body);
    }
    v.endVisit(this, ctx);
    if (trace && v instanceof JsModVisitor) {
      String after = this.toSource();
      if (!after.equals(before)) {
        String title = v.getClass().getSimpleName();
        trace(title, after);
      }
    }
  }
}
