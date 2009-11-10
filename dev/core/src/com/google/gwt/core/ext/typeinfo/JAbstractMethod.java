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
package com.google.gwt.core.ext.typeinfo;

import com.google.gwt.dev.util.collect.Lists;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

/**
 * Common superclass for {@link JMethod} and {@link JConstructor}.
 */
@SuppressWarnings("deprecation")
public abstract class JAbstractMethod implements HasAnnotations, HasMetaData,
    HasTypeParameters {

  private final Annotations annotations;

  private boolean isVarArgs = false;

  private int modifierBits;

  private final String name;

  private List<JParameter> params = Lists.create();

  private List<JType> thrownTypes = Lists.create();

  private List<JTypeParameter> typeParams = Lists.create();

  private String[] realParameterNames = null;

  JAbstractMethod(JAbstractMethod srcMethod) {
    this.annotations = new Annotations(srcMethod.annotations);
    this.isVarArgs = srcMethod.isVarArgs;
    this.modifierBits = srcMethod.modifierBits;
    this.name = srcMethod.name;
  }

  // Only the builder can construct
  JAbstractMethod(String name,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
      JTypeParameter[] jtypeParameters) {
    this.name = name;
    annotations = new Annotations(declaredAnnotations);

    if (jtypeParameters != null) {
      typeParams = Lists.create(jtypeParameters);
    }
  }

  @Deprecated
  public final void addMetaData(String tagName, String[] values) {
    throw new UnsupportedOperationException();
  }

  public void addModifierBits(int bits) {
    modifierBits |= bits;
  }

  public void addThrows(JType type) {
    thrownTypes = Lists.add(thrownTypes, type);
  }

  public JParameter findParameter(String name) {
    for (JParameter param : params) {
      if (param.getName().equals(name)) {
        return param;
      }
    }
    return null;
  }

  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    return annotations.getAnnotation(annotationClass);
  }

  /**
   * Gets the type in which this method or constructor was declared.
   */
  public abstract JClassType getEnclosingType();

  /**
   * Returns a string contating a JSNI reference to the method.
   * 
   * @return <code>@package.Class::method(Lpackage/Param;...)</code>
   */
  public abstract String getJsniSignature();

  @Deprecated
  public final String[][] getMetaData(String tagName) {
    return TypeOracle.NO_STRING_ARR_ARR;
  }

  @Deprecated
  public final String[] getMetaDataTags() {
    return TypeOracle.NO_STRINGS;
  }

  public String getName() {
    return name;
  }

  public JParameter[] getParameters() {
    // TODO(jat): where do we handle fake arg names?
    return params.toArray(TypeOracle.NO_JPARAMS);
  }

  public abstract String getReadableDeclaration();

  public JType[] getThrows() {
    return thrownTypes.toArray(TypeOracle.NO_JTYPES);
  }

  public JTypeParameter[] getTypeParameters() {
    return typeParams.toArray(new JTypeParameter[typeParams.size()]);
  }

  public JAnnotationMethod isAnnotationMethod() {
    return null;
  }

  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    return annotations.isAnnotationPresent(annotationClass);
  }

  public abstract JConstructor isConstructor();

  public boolean isDefaultAccess() {
    return 0 == (modifierBits & (TypeOracle.MOD_PUBLIC | TypeOracle.MOD_PRIVATE | TypeOracle.MOD_PROTECTED));
  }

  public abstract JMethod isMethod();

  public boolean isPrivate() {
    return 0 != (modifierBits & TypeOracle.MOD_PRIVATE);
  }

  public boolean isProtected() {
    return 0 != (modifierBits & TypeOracle.MOD_PROTECTED);
  }

  public boolean isPublic() {
    return 0 != (modifierBits & TypeOracle.MOD_PUBLIC);
  }

  public boolean isVarArgs() {
    return isVarArgs;
  }

  public void setVarArgs() {
    isVarArgs = true;
  }

  protected int getModifierBits() {
    return modifierBits;
  }

  protected void toStringParamsAndThrows(StringBuilder sb) {
    sb.append("(");
    boolean needComma = false;
    for (int i = 0, c = params.size(); i < c; ++i) {
      JParameter param = params.get(i);
      if (needComma) {
        sb.append(", ");
      } else {
        needComma = true;
      }
      if (isVarArgs() && i == c - 1) {
        JArrayType arrayType = param.getType().isArray();
        assert (arrayType != null);
        sb.append(arrayType.getComponentType().getParameterizedQualifiedSourceName());
        sb.append("...");
      } else {
        sb.append(param.getType().getParameterizedQualifiedSourceName());
      }
      sb.append(" ");
      sb.append(param.getName());
    }
    sb.append(")");

    if (!thrownTypes.isEmpty()) {
      sb.append(" throws ");
      needComma = false;
      for (JType thrownType : thrownTypes) {
        if (needComma) {
          sb.append(", ");
        } else {
          needComma = true;
        }
        sb.append(thrownType.getParameterizedQualifiedSourceName());
      }
    }
  }

  protected void toStringTypeParams(StringBuilder sb) {
    sb.append("<");
    boolean needComma = false;
    for (JTypeParameter typeParam : typeParams) {
      if (needComma) {
        sb.append(", ");
      } else {
        needComma = true;
      }
      sb.append(typeParam.getQualifiedSourceName());
    }
    sb.append(">");
  }

  void addParameter(JParameter param) {
    params = Lists.add(params, param);
  }

  /**
   * NOTE: This method is for testing purposes only.
   */
  Annotation[] getAnnotations() {
    return annotations.getAnnotations();
  }

  /**
   * NOTE: This method is for testing purposes only.
   */
  Annotation[] getDeclaredAnnotations() {
    return annotations.getDeclaredAnnotations();
  }

  // Called only by a JParameter, passing itself as a reference for lookup.
  String getRealParameterName(JParameter parameter) {
    if (realParameterNames == null) {
      fetchRealParameterNames();
    }
    int n = params.size();
    for (int i = 0; i < n; ++i) {
      // Identity tests are ok since identity is durable within an oracle.
      if (params.get(i) == parameter) {
        return realParameterNames == null ? "arg" + i : realParameterNames[i];
      }
    }
    // TODO: report error if we are asked for an unknown JParameter?
    return null;
  }

  boolean hasParamTypes(JType[] paramTypes) {
    if (params.size() != paramTypes.length) {
      return false;
    }

    for (int i = 0; i < paramTypes.length; i++) {
      JParameter candidate = params.get(i);
      // Identity tests are ok since identity is durable within an oracle.
      //
      if (candidate.getType() != paramTypes[i]) {
        return false;
      }
    }
    return true;
  }

  private void fetchRealParameterNames() {
    realParameterNames = getEnclosingType().getOracle().getJavaSourceParser().getArguments(this);
  }
}
