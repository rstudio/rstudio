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
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.collect.Lists;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A Java method implementation.
 */
public class JMethod extends JNode implements HasEnclosingType, HasName, HasType, CanBeAbstract,
    CanBeSetFinal, CanBeNative, CanBeStatic {

  private static class ExternalSerializedForm implements Serializable {

    private final JDeclaredType enclosingType;
    private final String signature;

    public ExternalSerializedForm(JMethod method) {
      enclosingType = method.getEnclosingType();
      signature = method.getSignature();
    }

    private Object readResolve() {
      String name = signature.substring(0, signature.indexOf('('));
      JMethod result =
          new JMethod(SourceOrigin.UNKNOWN, name, enclosingType, null, false, false, false, false);
      result.signature = signature;
      return result;
    }
  }

  private static final String TRACE_METHOD_WILDCARD = "*";

  private static void trace(String title, String code) {
    System.out.println("---------------------------");
    System.out.println(title + ":");
    System.out.println("---------------------------");
    System.out.println(code);
  }

  protected transient String signature;

  /**
   * Special serialization treatment.
   */
  private transient JAbstractMethodBody body = null;
  private JMethod directOverride;
  private final JDeclaredType enclosingType;
  private boolean isAbstract;
  private boolean isFinal;
  private final boolean isPrivate;
  private final boolean isStatic;
  private boolean isSynthetic = false;
  private final String name;
  private List<JType> originalParamTypes;
  private JType originalReturnType;
  private List<JParameter> params = Collections.emptyList();
  private JType returnType;
  private List<JClassType> thrownExceptions = Collections.emptyList();
  private boolean trace = false;
  private boolean traceFirst = true;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  public JMethod(SourceInfo info, String name, JDeclaredType enclosingType, JType returnType,
      boolean isAbstract, boolean isStatic, boolean isFinal, boolean isPrivate) {
    super(info);
    this.name = StringInterner.get().intern(name);
    this.enclosingType = enclosingType;
    this.returnType = returnType;
    this.isAbstract = isAbstract;
    this.isStatic = isStatic;
    this.isFinal = isFinal;
    this.isPrivate = isPrivate;
  }

  /**
   * Adds a parameter to this method.
   */
  public void addParam(JParameter x) {
    params = Lists.add(params, x);
  }

  public void addThrownException(JClassType exceptionType) {
    thrownExceptions = Lists.add(thrownExceptions, exceptionType);
  }

  public void addThrownExceptions(List<JClassType> exceptionTypes) {
    thrownExceptions = Lists.addAll(thrownExceptions, exceptionTypes);
  }

  /**
   * Returns true if this method can participate in virtual dispatch. Returns
   * true for non-private instance methods; false for static methods, private
   * instance methods, and constructors.
   */
  public boolean canBePolymorphic() {
    return !isStatic() && !isPrivate();
  }

  public void freezeParamTypes() {
    List<JType> paramTypes = new ArrayList<JType>();
    for (JParameter param : params) {
      paramTypes.add(param.getType());
    }
    setOriginalTypes(returnType, paramTypes);
  }

  public JAbstractMethodBody getBody() {
    assert !enclosingType.isExternal() : "External types do not have method bodies.";
    return body;
  }

  /**
   * Returns the method this method directly overrides, or <code>null</code>.
   */
  public JMethod getDirectOverride() {
    return directOverride;
  }

  public JDeclaredType getEnclosingType() {
    return enclosingType;
  }

  public String getName() {
    return name;
  }

  public List<JType> getOriginalParamTypes() {
    return originalParamTypes;
  }

  public JType getOriginalReturnType() {
    return originalReturnType;
  }

  /**
   * Returns the parameters of this method.
   */
  public List<JParameter> getParams() {
    return params;
  }

  public String getSignature() {
    if (signature == null) {
      StringBuilder sb = new StringBuilder();
      sb.append(getName());
      sb.append('(');
      for (JType type : getOriginalParamTypes()) {
        sb.append(type.getJsniSignatureName());
      }
      sb.append(')');
      sb.append(getOriginalReturnType().getJsniSignatureName());
      signature = sb.toString();
    }
    return signature;
  }

  public List<JClassType> getThrownExceptions() {
    return thrownExceptions;
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

  public boolean isSynthetic() {
    return isSynthetic;
  }

  public boolean isTrace() {
    return trace;
  }

  /**
   * Returns <code>true</code> if this method can participate in instance
   * dispatch.
   */
  public boolean needsVtable() {
    return !isStatic();
  }

  /**
   * Removes the parameter at the specified index.
   */
  public void removeParam(int index) {
    params = Lists.remove(params, index);
  }

  public void setAbstract(boolean isAbstract) {
    this.isAbstract = isAbstract;
  }

  public void setBody(JAbstractMethodBody body) {
    this.body = body;
    if (body != null) {
      body.setMethod(this);
    }
  }

  /**
   * Returns the method this method directly overrides.
   */
  public void setDirectOverride(JMethod directOverride) {
    assert canBePolymorphic();
    assert this.directOverride == null;
    assert directOverride.getEnclosingType() instanceof JClassType;
    this.directOverride = directOverride;
  }

  public void setFinal() {
    isFinal = true;
  }

  public void setOriginalTypes(JType returnType, List<JType> paramTypes) {
    if (originalParamTypes != null) {
      throw new InternalCompilerException("Param types already frozen");
    }
    originalReturnType = returnType;
    originalParamTypes = Lists.normalize(paramTypes);

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

  public void setSynthetic() {
    isSynthetic = true;
  }

  public void setTrace() {
    this.trace = true;
  }

  public void setType(JType newType) {
    returnType = newType;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    String before = null;
    before = traceBefore(visitor);
    if (visitor.visit(this, ctx)) {
      visitChildren(visitor);
    }
    visitor.endVisit(this, ctx);
    traceAfter(visitor, before);
  }

  protected void traceAfter(JVisitor visitor, String before) {
    if (trace && visitor instanceof JModVisitor) {
      String after = this.toSource();
      if (!after.equals(before)) {
        String title = visitor.getClass().getSimpleName();
        trace(title, after);
      }
    }
  }

  protected String traceBefore(JVisitor visitor) {
    if (trace && visitor instanceof JModVisitor) {
      String source = this.toSource();
      if (traceFirst) {
        traceFirst = false;
        trace("JAVA INITIAL", source);
      }
      return source;
    }
    return null;
  }

  protected void visitChildren(JVisitor visitor) {
    params = visitor.acceptImmutable(params);
    if (body != null) {
      body = (JAbstractMethodBody) visitor.accept(body);
    }
  }

  protected Object writeReplace() {
    if (enclosingType != null && enclosingType.isExternal()) {
      return new ExternalSerializedForm(this);
    } else {
      return this;
    }
  }

  /**
   * See {@link #writeBody(ObjectOutputStream)}.
   * 
   * @see #writeBody(ObjectOutputStream)
   */
  void readBody(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    body = (JAbstractMethodBody) stream.readObject();
  }

  /**
   * After all types, fields, and methods are written to the stream, this method
   * writes method bodies to the stream.
   * 
   * @see JProgram#writeObject(ObjectOutputStream)
   */
  void writeBody(ObjectOutputStream stream) throws IOException {
    stream.writeObject(body);
  }
}
