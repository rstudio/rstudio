/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.validation.example.client;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.validation.example.client.ExampleValidatorFactory.ServerGroup;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;

/**
 * Tests used to verify that constraints belonging to a group which is not specified in the
 * {@link com.google.gwt.validation.client.GwtValidation &#064;GwtValidation}
 * are not considered (and are not compiled).
 */
public class NotSpecifiedGroupsTest extends GWTTestCase {

  private Validator validator;

  @Override
  public String getModuleName() {
    return "com.google.gwt.validation.example.ValidationExample";
  }

  public void testConstraintsNotInSpecifiedGroupsAreNotConsidered() {
    MyClass obj = new MyClass();
    Set<ConstraintViolation<MyClass>> violations = validator.validate(obj);
    assertEquals(1, violations.size());
  }

  @Override
  protected final void gwtSetUp() throws Exception {
    super.gwtSetUp();
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  /**
   * Used for testing client/server constraints via groups.
   */
  @ServerConstraint(groups = ServerGroup.class)
  public static class MyClass {
    @NotNull
    public String name;
  }
}
