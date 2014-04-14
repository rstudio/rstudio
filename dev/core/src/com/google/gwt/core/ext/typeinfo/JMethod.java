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
 * Represents a method declaration.
 */
public interface JMethod extends JAbstractMethod {

  /**
   * Returns a {@code String} representing the source code declaration
   * of this method, containing access modifiers, type parameters,
   * return type, method name, parameter list, and throws.
   * Doesn't include the method body or trailing semicolon.
   *
   * @param noAccess if true, print no access modifiers
   * @param noNative if true, don't print the native modifier
   * @param noStatic if true, don't print the static modifier
   * @param noFinal if true, don't print the final modifier
   * @param noAbstract if true, don't print the abstract modifier
   */
  String getReadableDeclaration(boolean noAccess, boolean noNative,
      boolean noStatic, boolean noFinal, boolean noAbstract);

  JType getReturnType();

  boolean isAbstract();

  boolean isFinal();

  boolean isNative();

  boolean isStatic();
}
