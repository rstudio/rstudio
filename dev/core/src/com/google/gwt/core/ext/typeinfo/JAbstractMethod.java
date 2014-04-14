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
package com.google.gwt.core.ext.typeinfo;

/**
 * Common interface for {@link JMethod} and {@link JConstructor}.
 */
@SuppressWarnings("deprecation")
public interface JAbstractMethod extends HasAnnotations, HasMetaData,
    HasTypeParameters {

  JParameter findParameter(String name);

  /**
   * Gets the type in which this method or constructor was declared.
   */
  JClassType getEnclosingType();

  JType[] getErasedParameterTypes();

  /**
   * Returns a string containing a JSNI reference to the method.
   *
   * @return <code>@package.Class::method(Lpackage/Param;...)</code>
   */
  String getJsniSignature();

  String getName();

  JParameter[] getParameters();

  JType[] getParameterTypes();

  String getReadableDeclaration();

  JClassType[] getThrows();

  JAnnotationMethod isAnnotationMethod();

  JConstructor isConstructor();

  boolean isDefaultAccess();

  JMethod isMethod();

  boolean isPrivate();

  boolean isProtected();

  boolean isPublic();

  boolean isVarArgs();

}
