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

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A Java method implementation.
 */
public final class JMethod extends JNode implements HasEnclosingType, HasName,
    HasSettableType, CanBeAbstract, CanBeSetFinal, CanBeNative, CanBeStatic {

  private static final String TRACE_METHOD_WILDCARD = "*";

  private static void trace(String title, String code) {
    System.out.println("---------------------------");
    System.out.println(title + ":");
    System.out.println("---------------------------");
    System.out.println(code);
  }

  /**
   * References to any methods which this method overrides. This should be an
   * EXHAUSTIVE list, that is, if C overrides B overrides A, then C's overrides
   * list will contain both A and B.
   */
  public final List<JMethod> overrides = new ArrayList<JMethod>();

  public final ArrayList<JParameter> params = new ArrayList<JParameter>();
  public final ArrayList<JClassType> thrownExceptions = new ArrayList<JClassType>();
  private JAbstractMethodBody body = null;
  private final JReferenceType enclosingType;
  private final boolean isAbstract;
  private boolean isFinal;
  private final boolean isPrivate;
  private final boolean isStatic;
  private final String name;
  private List<JType> originalParamTypes;
  private JType returnType;
  private boolean trace = false;
  private boolean traceFirst = true;

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
    List<JType> paramTypes =  new ArrayList<JType>();
    for (JParameter param : params) {
      paramTypes.add(param.getType());
    }
    setOriginalParamTypes(paramTypes);
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

  public List<JType> getOriginalParamTypes() {
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

  public boolean isTrace() {
    return trace;
  }

  public void setBody(JAbstractMethodBody body) {
    if (body != null) {
      body.setMethod(null);
    }
    this.body = body;
    body.setMethod(this);
  }

  public void setFinal() {
    isFinal = true;
  }

  public void setOriginalParamTypes(List<JType> paramTypes) {
    if (originalParamTypes != null) {
      throw new InternalCompilerException("Param types already frozen");
    }
    originalParamTypes = paramTypes;
    

    // Determine if we should trace this method.
    if (enclosingType != null) {
      String jsniSig = JProgram.getJsniSig(this);
      Set<String> set = JProgram.traceMethods.get(enclosingType.getName());
      if (set != null
          && (set.contains(name) || set.contains(jsniSig) || set.contains(TRACE_METHOD_WILDCARD))) {
        trace = true;
      }
      // Try the short name.
      if (!trace && enclosingType != null) {
        set = JProgram.traceMethods.get(enclosingType.getShortName());
        if (set != null
            && (set.contains(name) || set.contains(jsniSig) || set.contains(TRACE_METHOD_WILDCARD))) {
          trace = true;
        }
      }
    }
  }

  public void setTrace() {
    this.trace = true;
  }

  public void setType(JType newType) {
    returnType = newType;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    String before = null;
    if (trace && visitor instanceof JModVisitor) {
      before = this.toSource();
      if (traceFirst) {
        traceFirst = false;
        trace("JAVA INITIAL", before);
      }
    }
    if (visitor.visit(this, ctx)) {
      visitor.accept(params);
      if (body != null) {
        body = (JAbstractMethodBody) visitor.accept(body);
      }
    }
    visitor.endVisit(this, ctx);
    if (trace && visitor instanceof JModVisitor) {
      String after = this.toSource();
      if (!after.equals(before)) {
        String title = visitor.getClass().getSimpleName();
        trace(title, after);
      }
    }
  }
}
