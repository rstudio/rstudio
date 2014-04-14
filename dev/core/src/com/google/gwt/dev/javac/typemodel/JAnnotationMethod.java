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

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Method declared on an annotation type.
 */
public class JAnnotationMethod extends JMethod implements
    com.google.gwt.core.ext.typeinfo.JAnnotationMethod {
  /**
   * Default value for this annotation element. <code>null</code> is not a valid
   * default value for an annotation element.
   */
  private final Object defaultValue;

  JAnnotationMethod(JClassType enclosingType, String name,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
      JTypeParameter[] jtypeParameters, Object defaultValue) {
    super(enclosingType, name, declaredAnnotations, jtypeParameters);
    this.defaultValue = defaultValue;
  }

  /**
   * Returns the default value for this annotation method or <code>null</code>
   * if there is not one.
   *
   * @return default value for this annotation method or <code>null</code> if
   *         there is not one
   */
  @Override
  public Object getDefaultValue() {
    return defaultValue;
  }

  @Override
  public JAnnotationMethod isAnnotationMethod() {
    return this;
  }
}
