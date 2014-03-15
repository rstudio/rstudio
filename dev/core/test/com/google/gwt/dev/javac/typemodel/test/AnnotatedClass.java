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
package com.google.gwt.dev.javac.typemodel.test;

import com.google.gwt.dev.javac.typemodel.test.ClassLiteralReferenceAnnotation.Foo;

/**
 * Used to test that a {@link com.google.gwt.core.ext.typeinfo.TypeOracle
 * TypeOracle} will correctly report the presence of annotations on the
 * different annotatable elements.
 */
@SourceRetentionAnnotation
@ClassLiteralReferenceAnnotation(Foo.class)
@TestAnnotation(value = "Class", nestedAnnotation = @NestedAnnotation("Foo"))
public class AnnotatedClass {
  @SourceRetentionAnnotation
  @TestAnnotation("Field")
  private int annotatedField;

  @SourceRetentionAnnotation
  @TestAnnotation("Constructor")
  public AnnotatedClass() {
  }

  @SourceRetentionAnnotation
  @TestAnnotation("Method")
  public void annotatedMethod() {
  }

  public void methodWithAnnotatedParameter(
      @SourceRetentionAnnotation @TestAnnotation("Parameter") int annotatedParameter) {
  }

  @TestAnnotation(value = "Method", arrayWithImplicitArrayInitializer = {
      String.class, Foo.class})
  void annotatedWithArrayOfClasses() {
  }
}
