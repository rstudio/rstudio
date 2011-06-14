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
package org.hibernate.jsr303.tck.tests.constraints.builtinconstraints;

import com.google.gwt.junit.client.GWTTestCase;

import static org.hibernate.jsr303.tck.util.TestUtil.assertCorrectNumberOfViolations;
import static org.hibernate.jsr303.tck.util.TestUtil.assertCorrectPropertyPaths;

import org.hibernate.jsr303.tck.util.TestUtil;

import java.util.Date;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.Future;
import javax.validation.constraints.Past;

/**
 * Test wrapper for {@link BuiltinConstraintsTest}.
 * 
 * <p>
 * NOTE: Test the Future and Past constraints here by hand because Hibernate
 * does not use the super source version when creating the beanDescriptor for
 * use at compile time.
 */
public class BuiltinConstraintsGwtTest extends GWTTestCase {

  class FutureDummyEntity {

    @Future
    Date date;
  }

  class PastDummyEntity {

    @Past
    Date date;
  }

  private static final int DAYS_IN_MILLIS = 24 * 60 * 60 * 1000;

  private final BuiltinConstraintsTest delegate = new BuiltinConstraintsTest();

  @Override
  public String getModuleName() {
    return "org.hibernate.jsr303.tck.tests.constraints.builtinconstraints.TckTest";
  }

  public void testAssertFalseConstraint() {
    delegate.testAssertFalseConstraint();
  }

  public void testAssertTrueConstraint() {
    delegate.testAssertTrueConstraint();
  }

  public void testDecimalMaxConstraint() {
    delegate.testDecimalMaxConstraint();
  }

  public void testDecimalMinConstraint() {
    delegate.testDecimalMinConstraint();
  }

  public void testDigitsConstraint() {
    delegate.testDigitsConstraint();
  }

  public void testFutureConstraint() {
    Validator validator = TestUtil.getValidatorUnderTest();
    FutureDummyEntity dummy = new FutureDummyEntity();

    Set<ConstraintViolation<FutureDummyEntity>> constraintViolations = validator
        .validate(dummy);
    assertCorrectNumberOfViolations(constraintViolations, 0);

    Date now = new Date();
    dummy.date = new Date(now.getTime() - DAYS_IN_MILLIS);

    constraintViolations = validator.validate(dummy);
    assertCorrectNumberOfViolations(constraintViolations, 1);
    assertCorrectPropertyPaths(constraintViolations, "date");

    dummy.date = new Date(now.getTime() + DAYS_IN_MILLIS);
    constraintViolations = validator.validate(dummy);
    assertCorrectNumberOfViolations(constraintViolations, 0);
  }

  public void testMaxConstraint() {
    delegate.testMaxConstraint();
  }

  public void testMinConstraint() {
    delegate.testMinConstraint();
  }

  public void testNotNullConstraint() {
    delegate.testNotNullConstraint();
  }

  public void testNullConstraint() {
    delegate.testNullConstraint();
  }

  public void testPastConstraint() {
    Validator validator = TestUtil.getValidatorUnderTest();
    PastDummyEntity dummy = new PastDummyEntity();

    Set<ConstraintViolation<PastDummyEntity>> constraintViolations = validator
        .validate(dummy);
    assertCorrectNumberOfViolations(constraintViolations, 0);

    Date now = new Date();
    dummy.date = new Date(now.getTime() + DAYS_IN_MILLIS);

    constraintViolations = validator.validate(dummy);
    assertCorrectNumberOfViolations(constraintViolations, 1);
    assertCorrectPropertyPaths(constraintViolations, "date");

    dummy.date = new Date(now.getTime() - DAYS_IN_MILLIS);
    constraintViolations = validator.validate(dummy);
    assertCorrectNumberOfViolations(constraintViolations, 0);
  }

  public void testPatternConstraint() {
    delegate.testPatternConstraint();
  }

  public void testSizeConstraint() {
    delegate.testSizeConstraint();
  }

  public String toString() {
    return delegate.toString();
  }

}
