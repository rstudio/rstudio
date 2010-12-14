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
package com.google.gwt.validation.client;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;

/**
 * GWT {@link ValidatorContext}.
 */
public class GwtValidatorContext implements ValidatorContext {

  public ValidatorContext constraintValidatorFactory(
      ConstraintValidatorFactory factory) {
    // TODO(nchalko) implement
    return this;
  }

  public Validator getValidator() {
    // TODO(nchalko) implement
    return null;
  }

  public ValidatorContext messageInterpolator(
      MessageInterpolator messageInterpolator) {
    // TODO(nchalko) implement
    return this;
  }

  public ValidatorContext traversableResolver(
      TraversableResolver traversableResolver) {
    // TODO(nchalko) implement
    return this;
  }

}
