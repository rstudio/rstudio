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

import com.google.gwt.core.ext.UnableToCompleteException;

import org.hibernate.jsr303.tck.tests.constraints.validatorresolution.AmbiguousValidatorFactory.AmbiguousValidator;
import org.hibernate.jsr303.tck.tests.constraints.validatorresolution.UnexpectedTypeValidatorFactory.UnexpectedTypeValidator;
import org.hibernate.jsr303.tck.util.TckCompileTestCase;

import javax.validation.ValidationException;

/**
 * Wraps {@link ValidatorResolutionTest} .
 */
public class ValidatorResolutionCompileTest extends TckCompileTestCase {

  /**
   * Replaces {@link ValidatorResolutionTest#testAmbiguousValidatorResolution()}
   */
  public void testAmbiguousValidatorResolution()
      throws UnableToCompleteException {
    assertBeanValidatorFailsToCompile(
        AmbiguousValidator.class,
        Foo.class,
        ValidationException.class,
        "More than one maximally specific "
            + "@org.hibernate.jsr303.tck.tests.constraints.validatorresolution"
            + ".Ambiguous(message=foobar, payload=[], groups=[]) "
            + "ConstraintValidator for type "
            + "class org.hibernate.jsr303.tck.tests.constraints"
            + ".validatorresolution.Bar, found "
            + "[class org.hibernate.jsr303.tck.tests.constraints"
            + ".validatorresolution.Ambiguous$AmbiguousValidatorForDummy,"
            + " class org.hibernate.jsr303.tck.tests.constraints"
            + ".validatorresolution.Ambiguous$"
            + "AmbiguousValidatorForSerializable]");

  }

  public void testUnexpectedTypeInValidatorResolution()
      throws UnableToCompleteException {
    assertBeanValidatorFailsToCompile(UnexpectedTypeValidator.class, Bar.class,
        ValidationException.class,
        "No @javax.validation.constraints.Size(message="
            + "{javax.validation.constraints.Size.message},"
            + " min=0, max=2147483647, payload=[], groups=[]) "
            + "ConstraintValidator for type class java.lang.Integer");
  }
}
