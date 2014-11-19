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
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * A Java method implementation.
 */
public class JMethod extends JNode implements HasEnclosingType, HasName, HasType, CanBeAbstract,
    CanBeSetFinal, CanBeNative, CanBeStatic {

  public static final Comparator<JMethod> BY_SIGNATURE_COMPARATOR = new Comparator<JMethod>() {
    @Override
    public int compare(JMethod m1, JMethod m2) {
      return m1.getSignature().compareTo(m2.getSignature());
    }
  };
  private String exportName;
  private boolean jsProperty;
  private Specialization specialization;
  private boolean noExport = false;
  private boolean inliningAllowed = true;
  private boolean hasSideEffects = true;

  public boolean isNoExport() {
    return noExport;
  }

  public void setNoExport(boolean noExport) {
    this.noExport = noExport;
  }

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

  public void setSpecialization(List<JType> paramTypes, JType returnsType, String targetMethod) {
    this.specialization = new Specialization(paramTypes, returnsType, targetMethod);
  }

  public Specialization getSpecialization() {
    return specialization;
  }

  public void removeSpecialization() {
    specialization = null;
  }

  public String getQualifiedExportName() {
    if (exportName.isEmpty()) {
      String qualifiedExportName = getEnclosingType().getQualifiedExportName();
      return this instanceof JConstructor ? qualifiedExportName :
          qualifiedExportName + "." + getLeafName();
    } else {
      return exportName;
    }
  }

  private String getLeafName() {
    String shortName = getName();
    return shortName.substring(shortName.lastIndexOf('$') + 1);
  }

  public boolean isInliningAllowed() {
    return inliningAllowed;
  }

  public void setInliningAllowed(boolean inliningAllowed) {
    this.inliningAllowed = inliningAllowed;
  }

  public boolean hasSideEffects() {
    return hasSideEffects;
  }

  public void setHasSideEffects(boolean hasSideEffects) {
    this.hasSideEffects = hasSideEffects;
  }

  /**
   * AST representation of @SpecializeMethod.
   */
  public static class Specialization implements Serializable {
    private List<JType> params;
    private JType returns;
    private String target;
    private JMethod targetMethod;

    public Specialization(List<JType> params, JType returns, String target) {
      this.params = params;
      this.returns = returns;
      this.target = target;
    }

    public List<JType> getParams() {
      return params;
    }

    public JType getReturns() {
      return returns;
    }

    public String getTarget() {
      return target;
    }

    public JMethod getTargetMethod() {
      return targetMethod;
    }

    public void resolve(List<JType> resolvedParams, JType resolvedReturn, JMethod targetMethod) {
      this.params = resolvedParams;
      this.returns = resolvedReturn;
      this.targetMethod = targetMethod;
    }

    public String getTargetSignature(JMethod instanceMethod) {
      return getTarget() + getParameterSignature(instanceMethod.getOriginalReturnType());
    }

    private String getParameterSignature(JType origReturnValue) {
      StringBuilder sb = new StringBuilder();
      getParamSignature(sb, params, returns != null ? returns : origReturnValue, false);
      return sb.toString();
    }
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
    this.name = StringInterner.get().intern(signature.substring(0, signature.indexOf('(')));
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
      getParamSignature(sb, getOriginalParamTypes(), getOriginalReturnType(), isConstructor());
      signature = sb.toString();
    }
    return signature;
  }

  public String getJsniSignature(boolean includeEnclosingClass, boolean includeReturnType) {
    StringBuilder sb = new StringBuilder();
    if (includeEnclosingClass) {
      sb.append(getEnclosingType().getName());
      sb.append("::");
    }
    sb.append(getName());
    sb.append('(');
    for (JType type : getOriginalParamTypes()) {
      sb.append(type.getJsniSignatureName());
    }
    sb.append(')');
    if (includeReturnType) {
      sb.append(originalReturnType.getJsniSignatureName());
    }
    return sb.toString();
  }

  private static void getParamSignature(StringBuilder sb,
      List<JType> params, JType returnType, boolean isCtor) {
    sb.append('(');
    for (JType type : params) {
      sb.append(type.getJsniSignatureName());
    }
    sb.append(')');
    if (!isCtor) {
      sb.append(returnType.getJsniSignatureName());
    } else {
      sb.append(" <init>");
    }
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
      String jsniSignature = getJsniSignature(false, true);
      trace = shouldTraceMethod(enclosingType.getShortName(), jsniSignature) ||
          shouldTraceMethod(enclosingType.getName(), jsniSignature);
    }
  }

  private boolean shouldTraceMethod(String className, String jsniSignature) {
    Set<String> set = JProgram.traceMethods.get(className);
    if (set == null) {
      return false;
    }

    return set.contains(name) || set.contains(jsniSignature) || set.contains(TRACE_METHOD_WILDCARD);
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
