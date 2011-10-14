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

import com.google.gwt.core.client.GWT;
import com.google.gwt.validation.client.spi.GwtConfigurationState;
import com.google.gwt.validation.client.spi.GwtValidationProvider;

import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.ValidatorFactory;
import javax.validation.spi.BootstrapState;
import javax.validation.spi.ConfigurationState;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * Base GWT {@link Configuration}.
 */
public abstract class BaseGwtConfiguration implements
    Configuration<BaseGwtConfiguration> {

  protected final GwtValidationProvider provider;
  protected final BootstrapState state;

  public BaseGwtConfiguration(GwtValidationProvider gwtValidationProvider,
      BootstrapState state) {
    provider = gwtValidationProvider;
    this.state = state;
  }

  public final BaseGwtConfiguration addProperty(String name, String value) {
    // TODO(nchalko) implement.
    return this;
  }

  public final ValidatorFactory buildValidatorFactory() {
    ConfigurationState configurationState = new GwtConfigurationState();
    return provider.buildValidatorFactory(configurationState);
  }

  public final BaseGwtConfiguration constraintValidatorFactory(
      ConstraintValidatorFactory constraintValidatorFactory) {
    // TODO(nchalko) implement.
    return this;
  }

  public final ConstraintValidatorFactory getDefaultConstraintValidatorFactory() {
    return GWT.create(ConstraintValidatorFactory.class);
  }

  public final MessageInterpolator getDefaultMessageInterpolator() {
    return GWT.create(MessageInterpolator.class);
  }

  public final TraversableResolver getDefaultTraversableResolver() {
    return GWT.create(TraversableResolver.class);
  }

  public final BaseGwtConfiguration ignoreXmlConfiguration() {
    // TODO(nchalko) implement.
    return this;
  }

  public final BaseGwtConfiguration messageInterpolator(
      MessageInterpolator interpolator) {
    // TODO(nchalko) implement.
    return this;
  }

  public final BaseGwtConfiguration traversableResolver(
      TraversableResolver resolver) {
    // TODO(nchalko) implement.
    return this;
  }

}