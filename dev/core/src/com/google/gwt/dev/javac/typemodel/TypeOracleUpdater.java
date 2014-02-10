/*
 * Copyright 2010 Google Inc.
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

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Updates an existing {@link TypeOracle}. This class provides package-level access to a TypeOracle
 * that is under construction. New classes can be added at any time, but should never be removed.
 *
 * This class's API is an implementation detail and should not be considered stable. It is subject
 * to change.
 */
public class TypeOracleUpdater {

  /**
   * The TypeOracle being updated.
   */
  protected final TypeOracle typeOracle;

  public TypeOracleUpdater(TypeOracle typeOracle) {
    this.typeOracle = typeOracle;
  }

  protected void addAnnotations(JPackage pkg,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
    pkg.addAnnotations(declaredAnnotations);
  }

  protected void addAnnotations(JRealClassType type,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
    type.addAnnotations(declaredAnnotations);
  }

  protected void addImplementedInterface(JRealClassType type, JClassType intf) {
    type.addImplementedInterface(intf);
  }

  protected void addModifierBits(JAbstractMethod method, int modifierBits) {
    method.addModifierBits(modifierBits);
  }

  protected void addModifierBits(JField jfield, int modifierBits) {
    jfield.addModifierBits(modifierBits);
  }

  protected void addThrows(JAbstractMethod method, JClassType exception) {
    method.addThrows(exception);
  }

  protected void finish() {
    typeOracle.finish();
  }

  protected JAnnotationMethod newAnnotationMethod(JRealClassType enclosingType,
      String name,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
      JTypeParameter[] jtypeParameters, Object defaultValue) {
    return new JAnnotationMethod(enclosingType, name, declaredAnnotations,
        jtypeParameters, defaultValue);
  }

  protected JAnnotationType newAnnotationType(JPackage pkg,
      String enclosingSimpleName, String simpleName) {
    return new JAnnotationType(typeOracle, pkg, enclosingSimpleName, simpleName);
  }

  protected JConstructor newConstructor(JRealClassType type, String name,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
      JTypeParameter[] typeParams) {
    return new JConstructor(type, name, declaredAnnotations, typeParams);
  }

  protected JEnumConstant newEnumConstant(JRealClassType type, String name,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
      int ordinal) {
    return new JEnumConstant(type, name, declaredAnnotations, ordinal);
  }

  protected JEnumType newEnumType(JPackage pkg, String enclosingSimpleName,
      String simpleName) {
    return new JEnumType(typeOracle, pkg, enclosingSimpleName, simpleName);
  }

  protected JField newField(JRealClassType type, String name,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
    return new JField(type, name, declaredAnnotations);
  }

  protected JMethod newMethod(JClassType type, String name,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
      JTypeParameter[] typeParams) {
    return new JMethod(type, name, declaredAnnotations, typeParams);
  }

  protected void newParameter(JAbstractMethod method, JType argType,
      String argName,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
      boolean argNamesAreReal) {
    new JParameter(method, argType, argName, declaredAnnotations,
        argNamesAreReal);
  }

  protected JRealClassType newRealClassType(JPackage pkg,
      String enclosingSimpleName, String simpleName, boolean isInterface) {
    return new JRealClassType(typeOracle, pkg, enclosingSimpleName, simpleName,
        isInterface);
  }

  protected void setEnclosingType(JRealClassType type, JClassType enclosingType) {
    type.setEnclosingType(enclosingType);
  }

  protected void setFieldType(JField jfield, JType fieldType) {
    jfield.setType(fieldType);
  }

  protected void setReturnType(JAbstractMethod method, JType returnType) {
    ((JMethod) method).setReturnType(returnType);
  }

  protected void setSuperClass(JRealClassType type, JClassType superType) {
    type.setSuperclass(superType);
  }

  protected void setVarArgs(JAbstractMethod method) {
    method.setVarArgs();
  }
}
