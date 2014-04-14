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

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Represents a parameter in a declaration.
 */
public class JParameter implements com.google.gwt.core.ext.typeinfo.JParameter {

  private final ImmutableAnnotations annotations;

  private boolean argNameIsReal;

  private final JAbstractMethod enclosingMethod;

  private String name;

  private JType type;

  /**
   * Creates a new JParameter from an existing one.
   */
  JParameter(JAbstractMethod enclosingMethod, JParameter srcParam) {
    this.enclosingMethod = enclosingMethod;
    this.type = srcParam.type;
    this.name = srcParam.name;
    this.argNameIsReal = srcParam.argNameIsReal;
    this.annotations = srcParam.annotations;
  }

  JParameter(JAbstractMethod enclosingMethod, JType type, String name,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
      boolean argNameIsReal) {
    this.enclosingMethod = enclosingMethod;
    this.type = type;
    this.name = StringInterner.get().intern(name);
    this.argNameIsReal = argNameIsReal;

    enclosingMethod.addParameter(this);

    annotations = ImmutableAnnotations.EMPTY.plus(declaredAnnotations);
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

  @Override
  public JAbstractMethod getEnclosingMethod() {
    return enclosingMethod;
  }

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
    if (!argNameIsReal) {
      name = enclosingMethod.getRealParameterName(this);
      argNameIsReal = true;
    }
    return name;
  }

  @Override
  public JType getType() {
    return type;
  }

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    return annotations.isAnnotationPresent(annotationClass);
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(type.getParameterizedQualifiedSourceName());
    sb.append(" ");
    sb.append(getName());
    return sb.toString();
  }

  // Only called by JAbstractMethod after real parameter names are fetched.
  void setName(String name) {
    this.name = StringInterner.get().intern(name);
  }

  // Called when parameter types are found to be parameterized
  void setType(JType type) {
    this.type = type;
  }
}
