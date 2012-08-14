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

import java.util.List;

import javax.validation.Configuration;
import javax.validation.ValidationException;
import javax.validation.ValidationProviderResolver;
import javax.validation.ValidatorFactory;
import javax.validation.bootstrap.GenericBootstrap;
import javax.validation.bootstrap.ProviderSpecificBootstrap;
import javax.validation.spi.BootstrapState;
import javax.validation.spi.ValidationProvider;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * This class is the entry point for Bean Validation. Bootstrapping is done as follows:
 * 
 * <pre>{@code ValidatorFactory factory = Validation.buildDefaultValidatorFactory();}</pre>
 * 
 * Or, equivalently:
 * 
 * <pre>{@code
 * Configuration<?> configuration = Validation
 *    .byDefaultProvider()
 *    .configure();
 * ValidatorFactory factory = configuration.buildValidatorFactory();}
 * </pre>
 * 
 * Only the default provider is available for use, and thus the {@code byProvider} and 
 * {@code providerResolver} methods are not supported. Calling either of these methods will
 * generate an exception.
 * <p>
 * This class was modified by Google from the original
 * javax.validation.Validation source to make it suitable for GWT.
 */
public class Validation {

  // private class, not exposed
  private static class GenericGWTBootstrapImpl implements GenericBootstrap,
      BootstrapState {

    private ValidationProviderResolver defaultResolver;

    @Override
    public Configuration<?> configure() {
      ValidationProviderResolver aResolver = getDefaultValidationProviderResolver();

      List<ValidationProvider<?>> resolvers;
      try {
        resolvers = aResolver.getValidationProviders();
      } catch (RuntimeException re) {
        throw new ValidationException(
            "Unable to get available provider resolvers.", re);
      }

      if (resolvers.size() == 0) {
        // FIXME looks like an assertion error almost
        throw new ValidationException("Unable to find a default provider");
      }

      Configuration<?> config;
      try {
        config = aResolver.getValidationProviders().get(0).createGenericConfiguration(
            this);
      } catch (RuntimeException re) {
        throw new ValidationException("Unable to instantiate Configuration.",
            re);
      }

      return config;
    }

    @Override
    public ValidationProviderResolver getDefaultValidationProviderResolver() {
      if (defaultResolver == null) {
        defaultResolver = GWT.create(ValidationProviderResolver.class);
      }
      return defaultResolver;
    }

    @Override
    public ValidationProviderResolver getValidationProviderResolver() {
      return getDefaultValidationProviderResolver();
    }

    /**
     * Unsupported. Always throws an {@link UnsupportedOperationException}.
     * 
     * @throws UnsupportedOperationException
     */
    @Override
    public GenericBootstrap providerResolver(ValidationProviderResolver resolver) {
      throw new UnsupportedOperationException("GWT Validation does not support custom validator " +
          "provider resolvers");
    }
  }

  /**
   * Build and return a <code>ValidatorFactory</code> instance based on the
   * default Bean Validation provider.
   * <p/>
   * The provider list is resolved using the default validation provider
   * resolver logic.
   * <p/>
   * The code is semantically equivalent to
   * <code>Validation.byDefaultProvider().configure().buildValidatorFactory()</code>
   * 
   * @return <code>ValidatorFactory</code> instance.
   * 
   * @throws ValidationException if the ValidatorFactory cannot be built
   */
  public static ValidatorFactory buildDefaultValidatorFactory() {
    return byDefaultProvider().configure().buildValidatorFactory();
  }

  /**
   * Build a <code>Configuration</code>.
   *
   * <pre>
   * Configuration&lt?&gt; configuration = Validation
   *    .byDefaultProvider()
   *    .configure();
   * ValidatorFactory factory = configuration.buildValidatorFactory();
   * </pre>
   *
   * The first available provider will be returned.
   *
   * @return instance building a generic <code>Configuration</code> complaint
   *         with the bootstrap state provided.
   */
  public static GenericBootstrap byDefaultProvider() {
    return new GenericGWTBootstrapImpl();
  }

  /**
   * Unsupported. Always throws an {@link UnsupportedOperationException}.
   * @param providerType 
   * 
   * @throws UnsupportedOperationException
   */
  public static <T extends Configuration<T>,U extends ValidationProvider<T>>
      ProviderSpecificBootstrap<T> byProvider(Class<U> providerType) {
    throw new UnsupportedOperationException("GWT Validation does not support custom validator " +
        "providers");
  }
}
