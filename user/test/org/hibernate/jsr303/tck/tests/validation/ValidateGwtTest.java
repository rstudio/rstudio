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
 * Test wrapper for {@link ValidateTest}.
 */
public class ValidateGwtTest extends AbstractValidationTest {

  private final ValidateTest delegate = new ValidateTest();

  public void testConstraintDescriptorWithoutExplicitGroup() {
    delegate.testConstraintDescriptorWithoutExplicitGroup();
  }

  public void testConstraintViolation() {
    delegate.testConstraintViolation();
  }

  public void testGraphValidationWithArray() {
    delegate.testGraphValidationWithArray();
  }

  public void testGraphValidationWithList() {
    delegate.testGraphValidationWithList();
  }

  public void testMultipleConstraintViolationOfDifferentTypes() {
    delegate.testMultipleConstraintViolationOfDifferentTypes();
  }

  public void testMultipleViolationOfTheSameType() {
    delegate.testMultipleViolationOfTheSameType();
  }

  public void testNullParameterToGetConstraintsForClass() {
    try {
      delegate.testNullParameterToGetConstraintsForClass();
      fail("Expected a " + IllegalArgumentException.class);
    } catch (IllegalArgumentException ignore) {
      // Expected
    }
  }

  public void testObjectTraversion() {
    delegate.testObjectTraversion();
  }

  public void testOnlyFirstGroupInSequenceGetEvaluated() {
    delegate.testOnlyFirstGroupInSequenceGetEvaluated();
  }

  public void testPassingNullAsGroup() {
    delegate.testPassingNullAsGroup();
  }

  public void testUnexpectedExceptionsInValidateGetWrappedInValidationExceptions() {
    try {
      delegate.testUnexpectedExceptionsInValidateGetWrappedInValidationExceptions();
      fail("Expected a " + ValidationException.class);
    } catch (ValidationException ignore) {
      // Expected
    }
  }

  public void testValidateWithNullGroup() {
    try {
      delegate.testValidateWithNullGroup();
      fail("Expected a " + IllegalArgumentException.class);
    } catch (IllegalArgumentException ignore) {
      // Expected
    }
  }

  public void testValidateWithNullValue() {
    try {
      delegate.testValidateWithNullValue();
      fail("Expected a " + IllegalArgumentException.class);
    } catch (IllegalArgumentException ignore) {
      // Expected
    }
  }

  public void testValidationIsPolymorphic() {
    delegate.testValidationIsPolymorphic();
  }
}
