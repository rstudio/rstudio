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
import com.google.gwt.dev.util.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a GWT.create() call before deferred binding decisions are
 * finalized. Replaced with the entry call for the appropriate rebind result in
 * that permutation.
 */
public class JGwtCreate extends JExpression {

  public static JExpression createInstantiationExpression(SourceInfo info, JClassType classType,
      JDeclaredType enclosingType) {
    /*
     * Find the appropriate (noArg) constructor. In our AST, constructors are
     * instance methods that should be qualified with a new expression.
     */
    JConstructor noArgCtor = null;
    for (JMethod ctor : classType.getMethods()) {
      if (ctor instanceof JConstructor) {
        if (ctor.getOriginalParamTypes().size() == 0) {
          noArgCtor = (JConstructor) ctor;
          break;
        }
      }
    }
    if (noArgCtor == null) {
      return null;
    }
    // Call it, using a new expression as a qualifier
    return new JNewInstance(info, noArgCtor, enclosingType);
  }

  /**
   * Rebinds are always on a source type name.
   */
  public static String nameOf(JType type) {
    // TODO: replace with BinaryName.toSourceName(type.getName())?
    return type.getName().replace('$', '.');
  }

  static List<String> nameOf(Collection<? extends JType> types) {
    List<String> result = Lists.create();
    for (JType type : types) {
      result = Lists.add(result, nameOf(type));
    }
    return Lists.normalizeUnmodifiable(result);
  }

  private static ArrayList<JExpression> createInstantiationExpressions(SourceInfo info,
      Collection<JClassType> classTypes, JDeclaredType enclosingType) {
    ArrayList<JExpression> exprs = new ArrayList<JExpression>();
    for (JClassType classType : classTypes) {
      JExpression expr = createInstantiationExpression(info, classType, enclosingType);
      assert expr != null;
      exprs.add(expr);
    }
    return exprs;
  }

  private final ArrayList<JExpression> instantiationExpressions;

  private final List<String> resultTypes;

  private final String sourceType;

  /*
   * Initially object; will be updated by type tightening.
   */
  private JType type;

  /**
   * Public constructor used during AST creation.
   */
  public JGwtCreate(SourceInfo info, JReferenceType sourceType, Collection<JClassType> resultTypes,
      JType type, JDeclaredType enclosingType) {
    this(info, nameOf(sourceType), nameOf(resultTypes), type, createInstantiationExpressions(info,
        resultTypes, enclosingType));
  }

  /**
   * Constructor used for cloning an existing node.
   */
  public JGwtCreate(SourceInfo info, String sourceType, List<String> resultTypes, JType type,
      ArrayList<JExpression> instantiationExpressions) {
    super(info);
    this.sourceType = sourceType;
    this.resultTypes = resultTypes;
    this.type = type;
    this.instantiationExpressions = instantiationExpressions;
  }

  public ArrayList<JExpression> getInstantiationExpressions() {
    return instantiationExpressions;
  }

  public List<String> getResultTypes() {
    return resultTypes;
  }

  public String getSourceType() {
    return sourceType;
  }

  public JType getType() {
    return type;
  }

  @Override
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
