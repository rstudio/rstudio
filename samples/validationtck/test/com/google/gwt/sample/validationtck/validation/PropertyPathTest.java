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

import com.google.gwt.sample.validationtck.util.Failing;
import com.google.gwt.sample.validationtck.util.NonTckTest;

/**
 * Test wrapper for
 * {@link org.hibernate.jsr303.tck.tests.validation.PropertyPathTest}.
 */
public class PropertyPathTest extends AbstractValidationTest {

  org.hibernate.jsr303.tck.tests.validation.PropertyPathTest delegate =
    new org.hibernate.jsr303.tck.tests.validation.PropertyPathTest();

  @NonTckTest
  public void testPlaceHolder() {
  }

  @Failing(issue = 5803)
  public void testPropertyPathSet() {
    delegate.testPropertyPathSet();
  }

  @Failing(issue = 5803)
  public void testPropertyPathTraversedObject() {
    delegate.testPropertyPathTraversedObject();
  }

  @Failing(issue = 5803)
  public void testPropertyPathWithArray() {
    delegate.testPropertyPathWithArray();
  }

  @Failing(issue = 5803)
  public void testPropertyPathWithConstraintViolationForRootObject() {
    delegate.testPropertyPathWithConstraintViolationForRootObject();
  }

  @Failing(issue = 5803)
  public void testPropertyPathWithList() {
    delegate.testPropertyPathWithList();
  }

  @Failing(issue = 5803)
  public void testPropertyPathWithMap() {
    delegate.testPropertyPathWithMap();
  }

}
