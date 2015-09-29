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

import com.google.gwt.dev.common.InliningMode;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a JavaScript function expression.
 */
public final class JsFunction extends JsLiteral implements HasName {

  protected JsBlock body;
  protected final List<JsParameter> params = new ArrayList<JsParameter>();
  protected final JsScope scope;
  private boolean isClinit;
  private boolean fromJava;
  private JsFunction superClinit;
  private JsName name;
  private InliningMode inliningMode = InliningMode.NORMAL;

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

  /**
   * Creates a dummy JsFunction (only used by createSentinel).
   */
  private JsFunction() {
    super(SourceOrigin.UNKNOWN);
    this.scope = null;
  }

  /**
   * Creates a dummy JsFunction object to be used as a sentinel.
   *
   * @return a dummy JsFunction object.
   */
  public static JsFunction createSentinel() {
    return new JsFunction();
  }

  public JsBlock getBody() {
    return body;
  }

  /**
   * Returns whether this function is the implementation of a class initiliazer. Class initializers
   * need only be executed once, hence the optimizers can remove subsequent calls.
   */
  public boolean isClinit() {
    return isClinit;
  }

  public JsFunction getSuperClinit() {
    return superClinit;
  }

  public InliningMode getInliningMode() {
    return inliningMode;
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

  @Override
  public boolean isBooleanFalse() {
    return false;
  }

  @Override
  public boolean isBooleanTrue() {
    return true;
  }

  @Override
  public boolean isDefinitelyNull() {
    return false;
  }

  public boolean isFromJava() {
    return fromJava;
  }

  public boolean isInliningAllowed() {
    return inliningMode != InliningMode.DO_NOT_INLINE;
  }

  public void markAsClinit() {
    this.isClinit = true;
  }

  public void setBody(JsBlock body) {
    this.body = body;
  }

  public void setFromJava(boolean fromJava) {
    this.fromJava = fromJava;
  }

  public void setSuperClinit(JsFunction superClinit) {
    this.superClinit = superClinit;
  }

  public void setInliningMode(InliningMode inliningMode) {
    this.inliningMode = inliningMode;
  }

  public void setName(JsName name) {
    this.name = name;
    if (name != null) {
      if (isFromJava()) {
        name.setStaticRef(this);
      }
    }
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      v.acceptWithInsertRemove(params);
      body = v.accept(body);
    }
    v.endVisit(this, ctx);
  }
}
