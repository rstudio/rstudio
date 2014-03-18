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

  private String exportName;
  private boolean jsProperty;

  public void setExportName(String exportName) {
    this.exportName = exportName;
  }

  public String getExportName() {
    return exportName;
  }

  public void setJsProperty(boolean jsProperty) {
    this.jsProperty = jsProperty;
  }

  public boolean isJsProperty() {
    return jsProperty;
  }

  private static class ExternalSerializedForm implements Serializable {

    private final JDeclaredType enclosingType;
    private final String signature;

    public ExternalSerializedForm(JMethod method) {
      enclosingType = method.getEnclosingType();
      signature = method.getSignature();
    }

    private Object readResolve() {
      return new JMethod(signature, enclosingType, false);
    }
  }

  private static class ExternalSerializedNullMethod implements Serializable {
    public static final ExternalSerializedNullMethod INSTANCE = new ExternalSerializedNullMethod();

    private Object readResolve() {
      return NULL_METHOD;
    }
  }

  public static final JMethod NULL_METHOD = new JMethod(SourceOrigin.UNKNOWN, "nullMethod", null,
      JNullType.INSTANCE, false, false, true, AccessModifier.PUBLIC);

  private static final String TRACE_METHOD_WILDCARD = "*";

  static {
    NULL_METHOD.setSynthetic();
    NULL_METHOD.freezeParamTypes();
  }

  static boolean replaces(List<? extends JMethod> newMethods, List<? extends JMethod> oldMethods) {
    if (newMethods.size() != oldMethods.size()) {
      return false;
    }
    for (int i = 0, c = newMethods.size(); i < c; ++i) {
      if (!newMethods.get(i).replaces(oldMethods.get(i))) {
        return false;
      }
    }
    return true;
  }

  private static void trace(String title, String code) {
    System.out.println("---------------------------");
    System.out.println(title + ":");
    System.out.println("---------------------------");
    System.out.println(code);
  }

  protected transient String signature;

  /**
   * The access modifier; stored as an int to reduce memory / serialization
   * footprint.
   */
  private int access;

  /**
   * Special serialization treatment.
   */
  private transient JAbstractMethodBody body = null;
  private final JDeclaredType enclosingType;
  private boolean isAbstract;
  private boolean isFinal;
  private final boolean isStatic;
  private boolean isSynthetic = false;
  private final String name;

  private List<JType> originalParamTypes;
  private JType originalReturnType;

  /**
   * References to any methods which this method overrides. This should be an
   * EXHAUSTIVE list, that is, if C overrides B overrides A, then C's overrides
   * list will contain both A and B.
   */
  private List<JMethod> overriddenMethods = Collections.emptyList();

  private List<JParameter> params = Collections.emptyList();
  private JType returnType;
  private List<JClassType> thrownExceptions = Collections.emptyList();

  private boolean trace = false;

  private boolean traceFirst = true;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  public JMethod(SourceInfo info, String name, JDeclaredType enclosingType, JType returnType,
      boolean isAbstract, boolean isStatic, boolean isFinal, AccessModifier access) {
    super(info);
    this.name = StringInterner.get().intern(name);
    this.enclosingType = enclosingType;
    this.returnType = returnType;
    this.isAbstract = isAbstract;
    this.isStatic = isStatic;
    this.isFinal = isFinal;
    this.access = access.ordinal();
  }

  /**
   * Creates an externalized representation for a method that needs to be resolved.
   * Useful to refer to methods of magic classes during GwtAstBuilder execution.
   *
   * @param fullClassName the class where the method is defined.
   * @param signature the signature of the method (including its name).
   *
   */
  public static JMethod getExternalizedMethod(String fullClassName, String signature,
      boolean isStatic) {

    JClassType cls = new JClassType(fullClassName);
    return new JMethod(signature, cls, isStatic);
  }

  /**
   * Construct a bare-bones deserialized external method.
   */
  private JMethod(String signature, JDeclaredType enclosingType, boolean isStatic) {
    super(SourceOrigin.UNKNOWN);
    this.name = signature.substring(0, signature.indexOf('('));
    this.enclosingType = enclosingType;
    this.signature = signature;
    this.isAbstract = false;
    this.isStatic = isStatic;
    this.access = AccessModifier.PUBLIC.ordinal();
  }

  /**
   * Add a method that this method overrides.
   */
  public void addOverriddenMethod(JMethod toAdd) {
    assert canBePolymorphic();
    overriddenMethods = Lists.add(overriddenMethods, toAdd);
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

  /**
   * Returns true if this method overrides a package private method and increases its
   * visibility.
   */
  public boolean exposesOverriddenPackagePrivateMethod() {
    if (isPrivate() || isDefault()) {
      return false;
    }

    for (JMethod overriddenMethod : overriddenMethods) {
      if (overriddenMethod.getEnclosingType() instanceof JInterfaceType) {
        continue;
      }
      if (overriddenMethod.isDefault()) {
        return true;
      }
    }

    return false;
  }

  public AccessModifier getAccess() {
    return AccessModifier.values()[access];
  }

  public JAbstractMethodBody getBody() {
    assert !isExternal() : "External types do not have method bodies.";
    return body;
  }

  @Override
  public JDeclaredType getEnclosingType() {
    return enclosingType;
  }

  @Override
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
   * Returns the transitive closure of all the methods this method overrides.
   */
  public List<JMethod> getOverriddenMethods() {
    return overriddenMethods;
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
      if (!isConstructor()) {
        sb.append(getOriginalReturnType().getJsniSignatureName());
      } else {
        sb.append(" <init>");
      }
      signature = sb.toString();
    }
    return signature;
  }

  public List<JClassType> getThrownExceptions() {
    return thrownExceptions;
  }

  @Override
  public JType getType() {
    return returnType;
  }

  @Override
  public boolean isAbstract() {
    return isAbstract;
  }

  public boolean isConstructor() {
    return false;
  }

  public boolean isDefault() {
    return access == AccessModifier.DEFAULT.ordinal();
  }

  public boolean isExternal() {
    return getEnclosingType() != null && getEnclosingType().isExternal();
  }

  @Override
  public boolean isFinal() {
    return isFinal;
  }

  @Override
  public boolean isNative() {
    if (body == null) {
      return false;
    } else {
      return body.isNative();
    }
  }

  public boolean isPrivate() {
    return access == AccessModifier.PRIVATE.ordinal();
  }

  @Override
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

  /**
   * Resolve an external references during AST stitching.
   */
  public void resolve(JType originalReturnType, List<JType> originalParamTypes, JType returnType,
      List<JClassType> thrownExceptions) {
    if (getClass().desiredAssertionStatus()) {
      assert originalReturnType.replaces(this.originalReturnType);
      assert JType.replaces(originalParamTypes, this.originalParamTypes);
      assert returnType.replaces(this.returnType);
      assert JType.replaces(thrownExceptions, this.thrownExceptions);
    }
    this.originalReturnType = originalReturnType;
    this.originalParamTypes = Lists.normalize(originalParamTypes);
    this.returnType = returnType;
    this.thrownExceptions = Lists.normalize(thrownExceptions);
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

  @Override
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

  @Override
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
    if (isExternal()) {
      return new ExternalSerializedForm(this);
    } else if (this == NULL_METHOD) {
      return ExternalSerializedNullMethod.INSTANCE;
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

  boolean replaces(JMethod originalMethod) {
    if (this == originalMethod) {
      return true;
    }
    return originalMethod.isExternal() && originalMethod.getSignature().equals(this.getSignature())
        && this.getEnclosingType().replaces(originalMethod.getEnclosingType());
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
