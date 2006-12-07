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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.impl.InternalCompilerException;

import java.util.ArrayList;
import java.util.List;

/**
 * A Java method implementation.
 */
public class JMethod extends JNode implements HasEnclosingType, HasName,
    HasType, HasSettableType, CanBeAbstract, CanBeFinal, CanBeSetFinal,
    CanBeNative, CanBeStatic {

  public final List/* <JMethod> */overrides = new ArrayList/* <JMethod> */();
  public final ArrayList/* <JClassType> */thrownExceptions = new ArrayList/* <JClassType> */();
  public final ArrayList/* <JParameter> */params = new ArrayList/* <JParameter> */();
  public final ArrayList/* <JLocal> */locals = new ArrayList/* <JLocal> */();
  public final JBlock body;
  /**
   * If this method overrides another methods, references to the declarations it
   * overrides. This should be an EXHAUSTIVE list, that is, if A->B->C, then C's
   * overrides list will contain both A and B.
   */
  private JType returnType;
  private final JReferenceType enclosingType;
  private final String name;
  private ArrayList/* <JType> */originalParamTypes;
  private final boolean isAbstract;
  private final boolean isStatic;
  private boolean isFinal;
  private final boolean isPrivate;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  public JMethod(JProgram program, String name, JReferenceType enclosingType,
      JType returnType, boolean isAbstract, boolean isStatic, boolean isFinal,
      boolean isPrivate) {
    super(program);
    this.name = name;
    this.enclosingType = enclosingType;
    this.returnType = returnType;
    this.body = new JBlock(program);
    this.isAbstract = isAbstract;
    this.isStatic = isStatic;
    this.isFinal = isFinal;
    this.isPrivate = isPrivate;
  }

  public void freezeParamTypes() {
    if (originalParamTypes != null) {
      throw new InternalCompilerException("Param types already frozen");
    }
    originalParamTypes = new ArrayList/* <JType> */();
    for (int i = 0; i < params.size(); ++i) {
      JParameter param = (JParameter) params.get(i);
      originalParamTypes.add(param.getType());
    }
  }

  public JReferenceType getEnclosingType() {
    return enclosingType;
  }

  public String getName() {
    return name;
  }

  public List/* <JType> */getOriginalParamTypes() {
    if (originalParamTypes == null) {
      return null;
    }
    return originalParamTypes;
  }

  public JType getType() {
    return returnType;
  }

  public boolean isAbstract() {
    return isAbstract;
  }

  public boolean isFinal() {
    return isFinal;
  }

  public boolean isNative() {
    return false;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public boolean isStatic() {
    return isStatic;
  }

  public void setFinal(boolean b) {
    isFinal = b;
  }

  public void setType(JType newType) {
    returnType = newType;
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      for (int i = 0; i < params.size(); ++i) {
        JParameter param = (JParameter) params.get(i);
        param.traverse(visitor);
      }
      for (int i = 0; i < locals.size(); ++i) {
        JLocal local = (JLocal) locals.get(i);
        local.traverse(visitor);
      }
      body.traverse(visitor);
    }
    visitor.endVisit(this);
  }

}
