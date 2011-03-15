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
package org.hibernate.jsr303.tck.tests.constraints.constraintcomposition;

import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.UnitTestTreeLogger;

import static org.hibernate.jsr303.tck.util.TckGeneratorTestUtils.assertModuleFails;
import static org.hibernate.jsr303.tck.util.TckGeneratorTestUtils.getFullyQaulifiedModuleName;

import org.hibernate.jsr303.tck.util.TckCompileTestCase;

import javax.validation.ConstraintDefinitionException;
import javax.validation.UnexpectedTypeException;

/**
 * Test wrapper for {@link ConstraintCompositionTest} tests that are meant to
 * fail to compile.
 */
public class ConstraintCompositionCompileTest extends TckCompileTestCase {

  /**
   * Replacement for
   * {@link ConstraintCompositionTest#testAllComposingConstraintsMustBeApplicableToAnnotatedType()}
   * 
   * @throws UnableToCompleteException
   */
  public void testAllComposingConstraintsMustBeApplicableToAnnotatedType()
      throws UnableToCompleteException {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.expect(
        Type.ERROR,
        "No @org.hibernate.jsr303.tck.tests.constraints.constraintcomposition.NotEmpty("
            + "message={constraint.notEmpty}, payload=[], groups=[]) "
            + "ConstraintValidator for type int", UnexpectedTypeException.class);
    builder.setLowestLogLevel(Type.INFO);
    UnitTestTreeLogger testLogger = builder.createLogger();
    assertModuleFails(testLogger,
        getFullyQaulifiedModuleName(getClass(), "MustBeApplicableTest"),
        MustBeApplicableValidatorFactory.MustBeApplicableValidator.class,
        Shoe.class);
  }

  /**
   * Replacement for
   * {@link ConstraintCompositionTest#testOverriddenAttributesMustMatchInType()}
   * 
   * @throws UnableToCompleteException
   */
  public void testOverriddenAttributesMustMatchInType()
      throws UnableToCompleteException {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.expect(Type.ERROR, "Unable to create a validator for "
        + "org.hibernate.jsr303.tck.tests.constraints.constraintcomposition."
        + "ConstraintCompositionTest.DummyEntityWithZipCode "
        + "because The overriding type of a composite constraint must be "
        + "identical to the overridden one. "
        + "Expected int found class java.lang.String",
        ConstraintDefinitionException.class);
    builder.setLowestLogLevel(Type.INFO);
    UnitTestTreeLogger testLogger = builder.createLogger();
    assertModuleFails(
        testLogger,
        getFullyQaulifiedModuleName(getClass(),
            "OverriddenAttributesMustMatchInTypeTest"),
        OverriddenAttributesMustMatchInTypeValidatorFactory.OverriddenAttributesMustMatchInTypeValidator.class);
  }

}
