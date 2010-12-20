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

import com.google.gwt.core.client.GWT;
import com.google.gwt.validation.client.impl.AbstractGwtValidator;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;

/**
 * GWT {@link ValidatorContext}.
 */
public class GwtValidatorContext implements ValidatorContext {

  private final AbstractGwtValidatorFactory validatorFactory = GWT.create(ValidatorFactory.class);

  private ConstraintValidatorFactory constraintValidatorfactory = validatorFactory.getConstraintValidatorFactory();
  private MessageInterpolator messageInterpolator = validatorFactory.getMessageInterpolator();
  private TraversableResolver traversableResolver = validatorFactory.getTraversableResolver();

  public ValidatorContext constraintValidatorFactory(
      ConstraintValidatorFactory factory) {
    if (factory == null) {
      this.constraintValidatorfactory = GWT.create(ConstraintValidatorFactory.class);
    } else {
      this.constraintValidatorfactory = factory;
    }
    return this;
  }

  public Validator getValidator() {
    AbstractGwtValidator validator = validatorFactory.createValidator();
    validator.init(constraintValidatorfactory, messageInterpolator,
        traversableResolver);
    return validator;
  }

  public ValidatorContext messageInterpolator(
      MessageInterpolator messageInterpolator) {
    if (messageInterpolator == null) {
      this.messageInterpolator = GWT.create(MessageInterpolator.class);
    } else {
      this.messageInterpolator = messageInterpolator;
    }
    return this;
  }

  public ValidatorContext traversableResolver(
      TraversableResolver traversableResolver) {
    if (traversableResolver == null) {
      this.traversableResolver = GWT.create(TraversableResolver.class);
    } else {
      this.traversableResolver = traversableResolver;
    }
    return this;
  }

}
