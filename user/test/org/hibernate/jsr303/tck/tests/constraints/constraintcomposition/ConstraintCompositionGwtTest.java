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

import com.google.gwt.junit.client.GWTTestCase;

import org.hibernate.jsr303.tck.util.client.Failing;

/**
 * Wraps {@link ConstraintCompositionTest}.
 */
public class ConstraintCompositionGwtTest extends GWTTestCase {
  private final ConstraintCompositionTest delegate = new ConstraintCompositionTest();

  @Override
  public String getModuleName() {
    return "org.hibernate.jsr303.tck.tests.constraints.constraintcomposition.TckTest";
  }

  @Failing(issue = 5799)
  public void testAllComposingConstraintsMustBeApplicableToAnnotatedType() {
    delegate.testAllComposingConstraintsMustBeApplicableToAnnotatedType();
  }

  @Failing(issue = 5799)
  public void testAttributesDefinedOnComposingConstraints() {
    delegate.testAttributesDefinedOnComposingConstraints();
  }

  @Failing(issue = 5882)
  public void testComposedConstraints() {
    delegate.testComposedConstraints();
    fail("This test only fails on IE.  Forcing it to fail here to keep the counts even. See issue 5882.");
  }

  @Failing(issue = 5882)
  public void testComposedConstraintsAreRecursive() {
    delegate.testComposedConstraintsAreRecursive();
    fail("This test only fails on IE.  Forcing it to fail here to keep the counts even. See issue 5882.");
  }

  @Failing(issue = 5799)
  public void testEachFailingConstraintCreatesConstraintViolation() {
    delegate.testEachFailingConstraintCreatesConstraintViolation();
  }

  public void testGroupsDefinedOnMainAnnotationAreInherited() {
    delegate.testGroupsDefinedOnMainAnnotationAreInherited();
  }

  @Failing(issue = 5799)
  public void testOnlySingleConstraintViolation() {
    delegate.testOnlySingleConstraintViolation();
  }

  @Failing(issue = 5799)
  public void testOverriddenAttributesMustMatchInType() {
    delegate.testOverriddenAttributesMustMatchInType();
  }

  public void testPayloadPropagationInComposedConstraints() {
    delegate.testPayloadPropagationInComposedConstraints();
  }

  @Failing(issue = 5799)
  public void testValidationOfMainAnnotationIsAlsoApplied() {
    delegate.testValidationOfMainAnnotationIsAlsoApplied();
  }
}
