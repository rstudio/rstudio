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
package com.google.gwt.dev.javac.typemodel.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This type is used to test how the
 * {@link com.google.gwt.core.ext.typeinfo.TypeOracle TypeOracle} deals with
 * annotations.
 */
// tests depend on this being available
@Retention(RetentionPolicy.RUNTIME)
@Target({
    ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD,
    ElementType.METHOD, ElementType.PACKAGE, ElementType.PARAMETER,
    ElementType.TYPE})
public @interface TestAnnotation {
  /**
   * Used to test initialization using conditional statements.
   */
  boolean useMinLong = true;

  /**
   * Used to test initialization using SingleNameReferences.
   */
  String defaultStringValue = "Hello There";

  /**
   * Tests that implicit array initializers are handled correctly.
   */
  Class<?>[] arrayWithImplicitArrayInitializer() default Object.class;

  /**
   * Tests default value initialization of class literals.
   */
  Class<?> classLiteral() default Object.class;

  /**
   * Tests that an empty array initializer is handled correctly.
   */
  int[] emptyArray() default {};

  /**
   * Tests array default values.
   */
  int[] intArrayValue() default {1, 2, 3};

  /**
   * Tests default values using conditional statements.
   */
  long longValue() default useMinLong ? Long.MIN_VALUE : Long.MAX_VALUE;

  /**
   * Tests element default values that are themselves annotations.
   */
  NestedAnnotation nestedAnnotation() default @NestedAnnotation("Not assigned");

  /**
   * Tests default value initialization via a QualifiedNameReference.
   */
  String stringValue() default TestAnnotation.defaultStringValue;

  /**
   * Default value for the annotation.
   */
  String value();
}
