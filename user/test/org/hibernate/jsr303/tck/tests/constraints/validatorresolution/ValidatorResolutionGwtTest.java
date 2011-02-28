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
package org.hibernate.jsr303.tck.tests.constraints.validatorresolution;

import com.google.gwt.junit.client.GWTTestCase;

import org.hibernate.jsr303.tck.util.client.Failing;

/**
 * Wraps {@link ValidatorResolutionTest} .
 */
public class ValidatorResolutionGwtTest extends GWTTestCase {
  ValidatorResolutionTest delegate = new ValidatorResolutionTest();
  @Override
  public String getModuleName() {
    return "org.hibernate.jsr303.tck.tests.constraints.validatorresolution.TckTest";
  }

  public void testResolutionOfMinMaxForDifferentTypes() {
    delegate.testResolutionOfMinMaxForDifferentTypes();
  }

  @Failing(issue = 5806)
  public void testResolutionOfMultipleSizeValidators() {
    delegate.testResolutionOfMultipleSizeValidators();
  }

  public void testTargetedTypeIsField() {
    delegate.testTargetedTypeIsField();
  }

  public void testTargetedTypeIsGetter() {
    delegate.testTargetedTypeIsGetter();
  }

  public void testTargetTypeIsClass() {
    delegate.testTargetTypeIsClass();
  }

  public void testTargetTypeIsInterface() {
    delegate.testTargetTypeIsInterface();
  }

}
