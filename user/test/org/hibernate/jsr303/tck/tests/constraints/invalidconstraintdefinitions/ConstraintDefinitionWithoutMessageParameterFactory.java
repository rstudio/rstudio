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
package org.hibernate.jsr303.tck.tests.constraints.invalidconstraintdefinitions;

import com.google.gwt.core.client.GWT;
import com.google.gwt.validation.client.AbstractGwtValidatorFactory;
import com.google.gwt.validation.client.GwtValidation;
import com.google.gwt.validation.client.impl.AbstractGwtValidator;

import org.hibernate.jsr303.tck.tests.constraints.invalidconstraintdefinitions.InvalidConstraintDefinitionsTest.DummyEntityNoMessage;

import javax.validation.Validator;

/**
 * ValidatorFactory for
 * {@link InvalidConstraintDefinitionsTest#testConstraintDefinitionWithoutMessageParameter()}
 */
public final class ConstraintDefinitionWithoutMessageParameterFactory extends
    AbstractGwtValidatorFactory {

  /**
   * Validator for
   * {@link InvalidConstraintDefinitionsTest#testConstraintDefinitionWithoutMessageParameter()}
   */
  @GwtValidation(value = {DummyEntityNoMessage.class})
  public static interface TestValidator extends
      Validator {
  }

  @Override
  public AbstractGwtValidator createValidator() {
    return GWT.create(TestValidator.class);
  }
}
