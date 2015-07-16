/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.dev.jjs.SourceInfo;

import java.util.Collections;
import java.util.List;

/**
 * Represents a GWT.create() or a GWT.getProperty call before deferred binding decisions are
 * finalized. These nodes are replaced by the appropriate expressions by
 * {@link com.google.gwt.dev.jjs.impl.ResolvePermutationDependentValues}.
 */
public class JPermutationDependentValue extends JExpression {

  private enum Type { PROPERTY, TYPE_REBIND };

  private final List<JExpression> resultExpressions;
  private final List<String> resultValues;
  private final JExpression defaultValueExpression;
  private final String requestedValue;
  private final Type valueType;
  private JType type;

  private JPermutationDependentValue(SourceInfo info, Type valueType, String requestedValue,
      List<String> resultValues, JExpression defaultValueExpression, JType resultType,
      List<JExpression> resultExpressions) {
    super(info);
    this.requestedValue = requestedValue;
    this.resultValues = resultValues;
    this.type = resultType;
    this.resultExpressions = resultExpressions;
    this.valueType = valueType;
    this.defaultValueExpression = defaultValueExpression;
  }

  public static JPermutationDependentValue createTypeRebind(JProgram program, SourceInfo info,
      String requestedType, List<String> resultTypes, List<JExpression> instantiationExpressions) {
    return new JPermutationDependentValue(info, Type.TYPE_REBIND, requestedType, resultTypes, null,
        program.getTypeJavaLangObject(), instantiationExpressions);
  }

  public static JPermutationDependentValue createRuntimeProperty(
      JProgram program, SourceInfo info, String propertyName, JExpression defaultValueExpression) {
    return new JPermutationDependentValue(info, Type.PROPERTY, propertyName, null,
        defaultValueExpression, program.getTypeJavaLangString(),
        Collections.<JExpression>emptyList());
  }

  public JExpression getDefaultValueExpression() {
    return defaultValueExpression;
  }

  public List<JExpression> getResultExpressions() {
    return resultExpressions;
  }

  public List<String> getResultValues() {
    return resultValues;
  }

  public String getRequestedValue() {
    return requestedValue;
  }

  @Override
  public JType getType() {
    return type;
  }

  public boolean isTypeRebind() {
    return valueType == Type.TYPE_REBIND;
  }

  public boolean isProperty() {
    return valueType == Type.PROPERTY;
  }

  @Override
  public boolean hasSideEffects() {
    for (JExpression expr : resultExpressions) {
      if (expr.hasSideEffects()) {
        return true;
      }
    }
    return false;
  }

  public void setType(JType newType) {
    type = newType;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.accept(resultExpressions);
    }
    visitor.endVisit(this, ctx);
  }
}
