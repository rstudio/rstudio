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
package org.hibernate.jsr303.tck.tests.constraints.customconstraint;

import com.google.gwt.junit.client.GWTTestCase;

import javax.validation.ValidationException;

/**
 * Test wrapper for {@link CustomConstraintValidatorTest}.
 */
public class CustomConstraintValidatorGwtTest extends GWTTestCase {
  private final CustomConstraintValidatorTest delegate = new CustomConstraintValidatorTest();

  @Override
  public String getModuleName() {
    return "org.hibernate.jsr303.tck.tests.constraints.customconstraint.TckTest";
  }

  public void testDefaultPropertyPath() {
    delegate.testDefaultPropertyPath();
  }

  public void testIsValidIsCalledForEachValidation() {
    delegate.testIsValidIsCalledForEachValidation();
  }

  public void testNonInterpolatedMessageParameterIsUsed() {
    delegate.testNonInterpolatedMessageParameterIsUsed();
  }

  public void testOneConstraintViolationPerFailingConstraint() {
    delegate.testOneConstraintViolationPerFailingConstraint();
  }

  public void testRightValidatorIsSelectedAndInitializedCalled() {
    delegate.testRightValidatorIsSelectedAndInitializedCalled();
  }

  public void testRuntimeExceptionFromInitializeIsWrapped() {
    try {
      delegate.testRuntimeExceptionFromInitializeIsWrapped();
      fail("Expected a " + ValidationException.class);
    } catch (ValidationException expected) {
      // expected
    }
  }

  public void testRuntimeExceptionFromIsValidIsWrapped() {
    try {
      delegate.testRuntimeExceptionFromIsValidIsWrapped();
      fail("Expected a " + ValidationException.class);
    } catch (ValidationException expected) {
      // expected
    }
  }
}
