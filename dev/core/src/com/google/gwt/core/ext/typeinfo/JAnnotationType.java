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
package com.google.gwt.core.ext.typeinfo;

import java.util.Arrays;

/**
 * Type representing an annotation type.
 */
public class JAnnotationType extends JRealClassType {

  public JAnnotationType(TypeOracle oracle, CompilationUnitProvider cup,
      JPackage declaringPackage, JClassType enclosingType, boolean isLocalType,
      String name, int declStart, int declEnd, int bodyStart, int bodyEnd,
      boolean isInterface) {
    super(oracle, cup, declaringPackage, enclosingType, isLocalType, name,
        declStart, declEnd, bodyStart, bodyEnd, isInterface);
  }

  @Override
  public JAnnotationMethod getMethod(String name, JType[] paramTypes)
      throws NotFoundException {
    return (JAnnotationMethod) super.getMethod(name, paramTypes);
  }

  @Override
  public JAnnotationMethod[] getMethods() {
    JMethod[] methodArray = super.getMethods();
    return Arrays.asList(methodArray).toArray(new JAnnotationMethod[0]);
  }

  @Override
  public JAnnotationMethod[] getOverridableMethods() {
    JMethod[] methodArray = super.getOverridableMethods();
    return Arrays.asList(methodArray).toArray(new JAnnotationMethod[0]);
  }

  @Override
  public JAnnotationType isAnnotation() {
    return this;
  }

}
