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
package com.google.gwt.dev.javac.typemodel;

import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.collect.Lists;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

/**
 * Common superclass for {@link JMethod} and {@link JConstructor}.
 */
public abstract class JAbstractMethod implements
    com.google.gwt.core.ext.typeinfo.JAbstractMethod {

  private final ImmutableAnnotations annotations;

  private boolean isVarArgs = false;

  private int modifierBits;

  private final String name;

  private List<JParameter> params = Lists.create();

  private String[] realParameterNames = null;

  private List<JClassType> thrownTypes = Lists.create();

  private List<JTypeParameter> typeParams = Lists.create();

  JAbstractMethod(JAbstractMethod srcMethod) {
    this.annotations = srcMethod.annotations;
    this.isVarArgs = srcMethod.isVarArgs;
    this.modifierBits = srcMethod.modifierBits;
    this.name = srcMethod.name;
  }

  // Only the builder can construct
  JAbstractMethod(String name,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
      JTypeParameter[] jtypeParameters) {
    this.name = StringInterner.get().intern(name);
    annotations = ImmutableAnnotations.EMPTY.plus(declaredAnnotations);

    if (jtypeParameters != null) {
      typeParams = Lists.create(jtypeParameters);
    }
  }

  @Override
  public JParameter findParameter(String name) {
    for (JParameter param : params) {
      if (param.getName().equals(name)) {
        return param;
      }
    }
    return null;
  }

  @Override
  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    return annotations.getAnnotation(annotationClass);
  }

  @Override
  public Annotation[] getAnnotations() {
    return annotations.getAnnotations();
  }

  @Override
  public Annotation[] getDeclaredAnnotations() {
    return annotations.getDeclaredAnnotations();
  }

  /**
   * Gets the type in which this method or constructor was declared.
   */
  @Override
  public abstract JClassType getEnclosingType();

  @Override
  public JType[] getErasedParameterTypes() {
    JType[] types = new JType[params.size()];
    for (int i = 0; i < types.length; ++i) {
      types[i] = params.get(i).getType().getErasedType();
    }
    return types;
  }

  /**
   * Returns a string contating a JSNI reference to the method.
   *
   * @return <code>@package.Class::method(Lpackage/Param;...)</code>
   */
  @Override
  public abstract String getJsniSignature();

  @Override
  @Deprecated
  public final String[][] getMetaData(String tagName) {
    return TypeOracle.NO_STRING_ARR_ARR;
  }

  @Override
  @Deprecated
  public final String[] getMetaDataTags() {
    return TypeOracle.NO_STRINGS;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public JParameter[] getParameters() {
    // TODO(jat): where do we handle fake arg names?
    return params.toArray(TypeOracle.NO_JPARAMS);
  }

  @Override
  public JType[] getParameterTypes() {
    final JType[] paramTypes = new JType[params.size()];
    for (int i = 0; i < paramTypes.length; ++i) {
      paramTypes[i] = params.get(i).getType();
    }
    return paramTypes;
  }

  @Override
  public abstract String getReadableDeclaration();

  @Override
  public JClassType[] getThrows() {
    return thrownTypes.toArray(TypeOracle.NO_JCLASSES);
  }

  @Override
  public JTypeParameter[] getTypeParameters() {
    return typeParams.toArray(new JTypeParameter[typeParams.size()]);
  }

  @Override
  public JAnnotationMethod isAnnotationMethod() {
    return null;
  }

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    return annotations.isAnnotationPresent(annotationClass);
  }

  @Override
  public abstract JConstructor isConstructor();

  @Override
  public boolean isDefaultAccess() {
    return 0 == (modifierBits & (TypeOracle.MOD_PUBLIC | TypeOracle.MOD_PRIVATE | TypeOracle.MOD_PROTECTED));
  }

  @Override
  public abstract JMethod isMethod();

  @Override
  public boolean isPrivate() {
    return 0 != (modifierBits & TypeOracle.MOD_PRIVATE);
  }

  @Override
  public boolean isProtected() {
    return 0 != (modifierBits & TypeOracle.MOD_PROTECTED);
  }

  @Override
  public boolean isPublic() {
    return 0 != (modifierBits & TypeOracle.MOD_PUBLIC);
  }

  @Override
  public boolean isVarArgs() {
    return isVarArgs;
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
        JArrayType arrayType = (JArrayType) param.getType().isArray();
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
      for (JClassType thrownType : thrownTypes) {
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

  void addModifierBits(int bits) {
    modifierBits |= bits;
  }

  void addParameter(JParameter param) {
    params = Lists.add(params, param);
  }

  void addThrows(JClassType type) {
    thrownTypes = Lists.add(thrownTypes, type);
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
        String realParameterName;
        if (realParameterNames == null) {
          realParameterName = StringInterner.get().intern("arg" + i);
        } else {
          realParameterName = StringInterner.get().intern(realParameterNames[i]);
        }
        return realParameterName;
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

  void setVarArgs() {
    isVarArgs = true;
  }

  private void fetchRealParameterNames() {
    realParameterNames = getEnclosingType().getOracle().getJavaSourceParser().getArguments(
        this);
  }
}
