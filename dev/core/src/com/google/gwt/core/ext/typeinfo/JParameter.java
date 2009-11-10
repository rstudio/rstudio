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

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Represents a parameter in a declaration.
 */
@SuppressWarnings("deprecation")
public class JParameter implements HasAnnotations, HasMetaData {

  private final Annotations annotations;

  private String name;

  private JType type;

  private final JAbstractMethod enclosingMethod;

  private boolean argNameIsReal;
  
  public JParameter(JAbstractMethod enclosingMethod, JType type,
      String name) {
    this(enclosingMethod, type, name, null);
  }

  public JParameter(JAbstractMethod enclosingMethod, JType type, String name,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
    this(enclosingMethod, type, name, declaredAnnotations, true);
  }

  public JParameter(JAbstractMethod enclosingMethod, JType type, String name,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
      boolean argNameIsReal) {
    this.enclosingMethod = enclosingMethod;
    this.type = type;
    this.name = name;
    this.argNameIsReal = argNameIsReal;

    enclosingMethod.addParameter(this);

    annotations = new Annotations(declaredAnnotations);
  }

  JParameter(JAbstractMethod enclosingMethod, JParameter srcParam) {
    this.enclosingMethod = enclosingMethod;
    this.type = srcParam.type;
    this.name = srcParam.name;
    this.annotations = new Annotations(srcParam.annotations);
  }

  @Deprecated
  public final void addMetaData(String tagName, String[] values) {
    throw new UnsupportedOperationException();
  }

  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    return annotations.getAnnotation(annotationClass);
  }

  public JAbstractMethod getEnclosingMethod() {
    return enclosingMethod;
  }

  @Deprecated
  public final String[][] getMetaData(String tagName) {
    return TypeOracle.NO_STRING_ARR_ARR;
  }

  @Deprecated
  public final String[] getMetaDataTags() {
    return TypeOracle.NO_STRINGS;
  }

  public String getName() {
    if (!argNameIsReal) {
      name = enclosingMethod.getRealParameterName(this);
      argNameIsReal = true;
    }
    return name;
  }

  public JType getType() {
    return type;
  }

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

  // Only called by JAbstractMethod after real parameter names are fetched.
  void setName(String name) {
    this.name = name;
  }

  // Called when parameter types are found to be parameterized
  void setType(JType type) {
    this.type = type;
  }
}
