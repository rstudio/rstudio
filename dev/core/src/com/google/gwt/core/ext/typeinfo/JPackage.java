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
 * Represents a logical package.
 */
public interface JPackage extends HasAnnotations {

  /**
   * Finds a type in this package.
   *
   * @param typeName the name of the type; use the <code>.</code> separator to
   *          find a nested type
   * @return the type, or <code>null</code> if the type does not exist in this
   *         package
   */
  JClassType findType(String typeName);

  /**
   * Finds a type in this package.
   *
   * @param typeName the name of the type; use additional array elements to find
   *          a nested type
   * @return the type, or <code>null</code> if the type does not exist in this
   *         package
   * @deprecated use {@link #findType(String)}
   */
  @Deprecated
  JClassType findType(String[] typeName);

  /**
   * Returns the name of the package.
   */
  String getName();

  /**
   * Finds a type in this package.
   *
   * @param typeName the name of the type; use the <code>.</code> separated to
   *          search for a nested type
   * @return the type, or <code>null</code> if the type does not exist in this
   *         package
   */
  JClassType getType(String typeName) throws NotFoundException;

  /**
   * Returns all top-level types in this package.
   */
  JClassType[] getTypes();

  /**
   * Returns <code>true</code> only for the default package.
   */
  boolean isDefault();
}
