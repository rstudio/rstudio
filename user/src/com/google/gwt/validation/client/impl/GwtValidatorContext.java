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
package com.google.gwt.validation.client.impl;

import com.google.gwt.validation.client.AbstractGwtValidatorFactory;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * GWT {@link ValidatorContext}.
 */
public final class GwtValidatorContext implements ValidatorContext {

  private final AbstractGwtValidatorFactory validatorFactory;

  private final ConstraintValidatorFactory factoryConstraintValidatorfactory;
  private final MessageInterpolator factoryMessageInterpolator;
  private final TraversableResolver factoryTraversableResolver;

  private ConstraintValidatorFactory constraintValidatorfactory = null;
  private MessageInterpolator messageInterpolator = null;
  private TraversableResolver traversableResolver = null;

  public GwtValidatorContext(AbstractGwtValidatorFactory validatorFactory) {
    this.validatorFactory = validatorFactory;

    factoryConstraintValidatorfactory = validatorFactory
        .getConstraintValidatorFactory();
    constraintValidatorfactory = validatorFactory
        .getConstraintValidatorFactory();

    factoryMessageInterpolator = validatorFactory.getMessageInterpolator();
    messageInterpolator = validatorFactory.getMessageInterpolator();

    factoryTraversableResolver = validatorFactory.getTraversableResolver();
    traversableResolver = validatorFactory.getTraversableResolver();
  }

  public ValidatorContext constraintValidatorFactory(
      ConstraintValidatorFactory constraintValidatorfactory) {
    if (constraintValidatorfactory == null) {
      this.constraintValidatorfactory = factoryConstraintValidatorfactory;
    } else {
      this.constraintValidatorfactory = constraintValidatorfactory;
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
      this.messageInterpolator = factoryMessageInterpolator;
    } else {
      this.messageInterpolator = messageInterpolator;
    }
    return this;
  }

  public ValidatorContext traversableResolver(
      TraversableResolver traversableResolver) {
    if (traversableResolver == null) {
      this.traversableResolver = factoryTraversableResolver;
    } else {
      this.traversableResolver = traversableResolver;
    }
    return this;
  }
}
