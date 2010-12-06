/*
 * Copyright 2007 Google Inc.
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
 * An enumeration constant declared in an enumerated type.
 */
public class JEnumConstant extends JField implements
    com.google.gwt.core.ext.typeinfo.JEnumConstant {
  private final int ordinal;

  JEnumConstant(JClassType enclosingType, String name,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
      int ordinal) {
    super(enclosingType, name, declaredAnnotations);
    this.ordinal = ordinal;
  }

  /**
   * Returns the ordinal value for this enumeration constant.
   * 
   * @return ordinal value for this enumeration constant
   */
  public int getOrdinal() {
    return ordinal;
  }

  @Override
  public JEnumConstant isEnumConstant() {
    return this;
  }
}
