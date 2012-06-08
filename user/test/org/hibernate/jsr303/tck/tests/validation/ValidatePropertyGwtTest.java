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

import javax.validation.ValidationException;

/**
 * Test wrapper for {@link ValidatePropertyTest}.
 */
public class ValidatePropertyGwtTest extends AbstractValidationTest {

  private final ValidatePropertyTest delegate = new ValidatePropertyTest();

  // Add Property Prefix so test name is unique in the suite.
  public void testPropertyIllegalArgumentExceptionIsThrownForNullValue() {
    delegate.testIllegalArgumentExceptionIsThrownForNullValue();
  }

  // Add Property Prefix so test name is unique in the suite.
  public void testPropertyPassingNullAsGroup() {
    delegate.testPassingNullAsGroup();
  }

  // Add Property Prefix so test name is unique in the suite.
  public void testPropertyUnexpectedExceptionsInValidatePropertyGetWrappedInValidationExceptions() {
    // Wrap the test to catch the @Test expected exception.
    try {
    delegate.testUnexpectedExceptionsInValidatePropertyGetWrappedInValidationExceptions();
      fail("Expected a " + ValidationException.class);
    } catch (ValidationException expected) {
    }
  }

  public void testValidateProperty() {
    delegate.testValidateProperty();
  }

  public void testValidatePropertyWithEmptyProperty() {
    delegate.testValidatePropertyWithEmptyProperty();
  }

  public void testValidatePropertyWithInvalidPropertyPath() {
    delegate.testValidatePropertyWithInvalidPropertyPath();
  }

  public void testValidatePropertyWithNullProperty() {
    delegate.testValidatePropertyWithNullProperty();
  }

  public void testValidIsNotHonoredValidateProperty() {
    delegate.testValidIsNotHonoredValidateProperty();
  }
}
