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

import com.google.gwt.core.ext.UnableToCompleteException;

import org.hibernate.jsr303.tck.tests.constraints.constraintcomposition.OverriddenAttributesMustMatchInTypeValidatorFactory.OverriddenAttributesMustMatchInTypeValidator;
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
    assertBeanValidatorFailsToCompile(
        MustBeApplicableValidatorFactory.MustBeApplicableValidator.class,
        Shoe.class,
        UnexpectedTypeException.class,
        "No @org.hibernate.jsr303.tck.tests.constraints.constraintcomposition"
            + ".NotEmpty(message={constraint.notEmpty}, payload=[], groups=[]) "
            + "ConstraintValidator for type int");
  }

  /**
   * Replacement for
   * {@link ConstraintCompositionTest#testOverriddenAttributesMustMatchInType()}
   * 
   * @throws UnableToCompleteException
   */
  public void testOverriddenAttributesMustMatchInType()
      throws UnableToCompleteException {
    assertValidatorFailsToCompile(
        OverriddenAttributesMustMatchInTypeValidator.class,
        ConstraintDefinitionException.class,
        "Unable to create a validator for "
            + "org.hibernate.jsr303.tck.tests.constraints.constraintcomposition."
            + "ConstraintCompositionTest.DummyEntityWithZipCode "
            + "because The overriding type of a composite constraint must be "
            + "identical to the overridden one. "
            + "Expected int found class java.lang.String");
  }
}
