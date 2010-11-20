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
package com.google.gwt.sample.validationtck.validation;

/**
 * Test wrapper for
 * {@link org.hibernate.jsr303.tck.tests.validation.PropertyPathTest}.
 */
public class PropertyPathTest extends AbstractValidationTest {

  org.hibernate.jsr303.tck.tests.validation.PropertyPathTest delegate =
    new org.hibernate.jsr303.tck.tests.validation.PropertyPathTest();

  public void testPropertyPathSet() {
    delegate.testPropertyPathSet();
  }

  public void testPropertyPathTraversedObject() {
    delegate.testPropertyPathTraversedObject();
  }

  public void testPropertyPathWithArray() {
    delegate.testPropertyPathWithArray();
  }

  public void testPropertyPathWithConstraintViolationForRootObject() {
    delegate.testPropertyPathWithConstraintViolationForRootObject();
  }

  public void testPropertyPathWithList() {
    delegate.testPropertyPathWithList();
  }

  public void testPropertyPathWithMap() {
    delegate.testPropertyPathWithMap();
  }

}
