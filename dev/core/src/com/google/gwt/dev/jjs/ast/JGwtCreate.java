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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a GWT.create() call before deferred binding decisions are
 * finalized. Replaced with the entry call for the appropriate rebind result in
 * that permutation.
 */
public class JGwtCreate extends JExpression implements HasSettableType {

  public static JExpression createInstantiationExpression(JProgram program,
      SourceInfo info, JClassType classType) {
    /*
     * Find the appropriate (noArg) constructor. In our AST, constructors are
     * instance methods that should be qualified with a new expression.
     */
    JMethod noArgCtor = null;
    for (int i = 0; i < classType.methods.size(); ++i) {
      JMethod ctor = classType.methods.get(i);
      if (ctor.getName().equals(classType.getShortName())) {
        if (ctor.params.size() == 0) {
          noArgCtor = ctor;
        }
      }
    }
    if (noArgCtor == null) {
      return null;
    }
    // Call it, using a new expression as a qualifier
    JNewInstance newInstance = new JNewInstance(program, info, classType);
    return new JMethodCall(program, info, newInstance, noArgCtor);
  }

  private final ArrayList<JExpression> instantiationExpressions = new ArrayList<JExpression>();
  private final List<JClassType> resultTypes;
  private final JReferenceType sourceType;

  private JType type;

  public JGwtCreate(JProgram program, SourceInfo info,
      JReferenceType sourceType, List<JClassType> resultTypes) {
    super(program, info);
    this.sourceType = sourceType;
    this.resultTypes = resultTypes;

    // Initially object; will be updated by type tightening.
    this.type = program.getTypeJavaLangObject();

    for (JClassType classType : resultTypes) {
      JExpression expr = createInstantiationExpression(program, info, classType);
      assert expr != null;
      instantiationExpressions.add(expr);
    }
  }

  public ArrayList<JExpression> getInstantiationExpressions() {
    return instantiationExpressions;
  }

  public List<JClassType> getResultTypes() {
    return resultTypes;
  }

  public JReferenceType getSourceType() {
    return sourceType;
  }

  public JType getType() {
    return type;
  }

  public boolean hasSideEffects() {
    for (JExpression expr : instantiationExpressions) {
      if (expr.hasSideEffects()) {
        return true;
      }
    }
    return false;
  }

  public void setType(JType newType) {
    type = newType;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.accept(instantiationExpressions);
    }
    visitor.endVisit(this, ctx);
  }
}
