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
package com.google.gwt.core.ext.soyc;

import java.util.SortedSet;

/**
 * Represents a reference type, such as a class or interface, in the compiled
 * output. Methods and fields of the original Java type will have been pruned by
 * the compiler, so the values returned by {@link #getFields()} and
 * {@link #getMethods()} may be incomplete when compared to the original Java
 * type.
 */
public interface ClassMember extends HasDependencies, Member {

  /**
   * Returns the fields of the ClassMember that have been retained in the
   * compiled output.
   */
  SortedSet<FieldMember> getFields();

  /**
   * Returns the methods of the ClassMember that have been retained in the
   * compiled output.
   */
  SortedSet<MethodMember> getMethods();

  /**
   * Returns the Java package from which the ClassMember originated.
   */
  String getPackage();
}