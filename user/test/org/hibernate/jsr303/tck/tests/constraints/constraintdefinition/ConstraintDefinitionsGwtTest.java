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
package org.hibernate.jsr303.tck.tests.constraints.constraintdefinition;

import com.google.gwt.junit.client.GWTTestCase;

import org.hibernate.jsr303.tck.util.client.NonTckTest;

/**
 * Test wrapper for {@link ConstraintDefinitionsTest}.
 */
public class ConstraintDefinitionsGwtTest extends GWTTestCase {

  private ConstraintDefinitionsTest delegate = new ConstraintDefinitionsTest();

  @Override
  public String getModuleName() {
    return "org.hibernate.jsr303.tck.tests.constraints.constraintdefinition.TckTest";
  }

  public void testConstraintWithCustomAttributes() {
    delegate.testConstraintWithCustomAttributes();
  }

  public void testDefaultGroupAssumedWhenNoGroupsSpecified() {
    delegate.testDefaultGroupAssumedWhenNoGroupsSpecified();
  }

  @NonTckTest
  public void testThereMustBeOnePassingTest(){}
}
