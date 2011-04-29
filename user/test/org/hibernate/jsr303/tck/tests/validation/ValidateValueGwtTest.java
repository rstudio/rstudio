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
package org.hibernate.jsr303.tck.tests.validation;


/**
 * Test wrapper for {@link ValidateValueTest}.
 */
public class ValidateValueGwtTest extends AbstractValidationTest {

  private final ValidateValueTest delegate = new ValidateValueTest();

  public void testExistingPropertyWoConstraintsNorCascaded() {
    delegate.testExistingPropertyWoConstraintsNorCascaded();
  }

  public void testValidateValue() {
    delegate.testValidateValue();
  }

  public void testValidateValueFailure() {
    delegate.testValidateValueFailure();
  }

  public void testValidateValuePassingNullAsGroup() {
    delegate.testValidateValuePassingNullAsGroup();
  }

  public void testValidateValueSuccess() {
    delegate.testValidateValueSuccess();
  }

  public void testValidateValueWithEmptyPropertyPath() {
    delegate.testValidateValueWithEmptyPropertyPath();
  }

  public void testValidateValueWithInvalidPropertyPath() {
    delegate.testValidateValueWithInvalidPropertyPath();
  }

  public void testValidateValueWithNullObject() {
    delegate.testValidateValueWithNullObject();
  }

  public void testValidateValueWithNullPropertyName() {
    delegate.testValidateValueWithNullPropertyName();
  }

  public void testValidIsNotHonoredValidateValue() {
    delegate.testValidIsNotHonoredValidateValue();
  }
}
