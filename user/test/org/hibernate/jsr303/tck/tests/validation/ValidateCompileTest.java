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

import org.hibernate.jsr303.tck.util.TckCompileTestCase;

import javax.validation.ValidationException;

/**
 * Test wrapper for {@link ValidateTest} methods that are suppose to fail to
 * compile.
 */
public class ValidateCompileTest extends TckCompileTestCase {

  public void testValidatedPropertyDoesNotFollowJavaBeansConvention() {
    assertValidatorFailsToCompile(
        TckCompileTestValidatorFactory.GwtValidator.class,
        ValidationException.class, "Unable to create a validator for "
            + "org.hibernate.jsr303.tck.tests.validation.Boy "
            + "because Annotated methods must follow the "
            + "JavaBeans naming convention. age() does not.");
  }
}