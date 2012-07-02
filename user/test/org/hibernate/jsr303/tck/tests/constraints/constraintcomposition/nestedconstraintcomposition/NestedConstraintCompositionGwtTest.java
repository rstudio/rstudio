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
package org.hibernate.jsr303.tck.tests.constraints.constraintcomposition.nestedconstraintcomposition;

import com.google.gwt.junit.client.GWTTestCase;

import org.hibernate.jsr303.tck.util.client.NonTckTest;

/**
 * Test wrapper for {@link NestedConstraintCompositionTest}.
 */
public class NestedConstraintCompositionGwtTest extends GWTTestCase {

  private NestedConstraintCompositionTest delegate = new NestedConstraintCompositionTest();


  @Override
  public String getModuleName() {
    return "org.hibernate.jsr303.tck.tests.constraints.constraintcomposition.nestedconstraintcomposition.TckTest";
  }

  public void testCompositeConstraint1WithNestedConstraintSingleViolation() {
    delegate.testCompositeConstraint1WithNestedConstraintSingleViolation();
  }

  public void testCompositeConstraint2WithNestedConstraintSingleViolation() {
    delegate.testCompositeConstraint2WithNestedConstraintSingleViolation();
  }

  public void testCompositeConstraint3WithNestedConstraint() {
    delegate.testCompositeConstraint3WithNestedConstraint();
  }

  public void testCompositeConstraint4WithNestedConstraintSingleViolation() {
    delegate.testCompositeConstraint4WithNestedConstraintSingleViolation();
  }
  
  @NonTckTest
  public void testThereMustBeOnePassingTest() {}
}
