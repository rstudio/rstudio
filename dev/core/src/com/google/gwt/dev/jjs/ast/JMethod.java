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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A Java method implementation.
 */
public final class JMethod extends JNode implements HasEnclosingType, HasName,
    HasType, HasSettableType, CanBeAbstract, CanBeFinal, CanBeSetFinal,
    CanBeNative, CanBeStatic {

  /**
   * References to any methods which this method overrides. This should be an
   * EXHAUSTIVE list, that is, if C overrides B overrides A, then C's overrides
   * list will contain both A and B.
   */
  public final List/* <JMethod> */overrides = new ArrayList/* <JMethod> */();

  public final ArrayList/* <JParameter> */params = new ArrayList/* <JParameter> */();
  public final ArrayList/* <JClassType> */thrownExceptions = new ArrayList/* <JClassType> */();
  private JAbstractMethodBody body = null;
  private final JReferenceType enclosingType;
  private final boolean isAbstract;
  private boolean isFinal;
  private final boolean isPrivate;
  private final boolean isStatic;
  private final String name;
  private ArrayList/* <JType> */originalParamTypes;
  private JType returnType;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  public JMethod(JProgram program, SourceInfo info, String name,
      JReferenceType enclosingType, JType returnType, boolean isAbstract,
      boolean isStatic, boolean isFinal, boolean isPrivate) {
    super(program, info);
    this.name = name;
    this.enclosingType = enclosingType;
    this.returnType = returnType;
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

  public JAbstractMethodBody getBody() {
    return body;
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
    if (body == null) {
      return false;
    } else {
      return body.isNative();
    }
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public boolean isStatic() {
    return isStatic;
  }

  public void setBody(JAbstractMethodBody body) {
    if (body != null) {
      body.setMethod(null);
    }
    this.body = body;
    body.setMethod(this);
  }

  public void setFinal(boolean b) {
    isFinal = b;
  }

  public void setType(JType newType) {
    returnType = newType;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.accept(params);
      if (body != null) {
        body = (JAbstractMethodBody) visitor.accept(body);
      }
    }
    visitor.endVisit(this, ctx);
  }

}
