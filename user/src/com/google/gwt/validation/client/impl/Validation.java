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
 * This class is the entry point for Bean Validation. There are three ways to
 * bootstrap it:
 * <ul>
 * <li>
 * The easiest approach is to build the default <code>ValidatorFactory</code>.
 * 
 * <pre>{@code ValidatorFactory factory = Validation.buildDefaultValidatorFactory();}</pre>
 * In this case, the default validation provider resolver will be used to locate
 * available providers. The chosen provider is defined as followed:
 * <ul>
 * <li>Since GWT does not support XML configuration, the first provider returned
 * by the <code>ValidationProviderResolver</code> instance is used.</li>
 * </ul>
 * </li>
 * <li>
 * The second bootstrap approach allows to choose a custom
 * <code>ValidationProviderResolver</code>. The chosen
 * <code>ValidationProvider</code> is then determined in the same way as in the
 * default bootstrapping case (see above).
 * 
 * <pre>{@code
 * Configuration<?> configuration = Validation
 *    .byDefaultProvider()
 *    .providerResolver( new MyResolverStrategy() )
 *    .configure();
 * ValidatorFactory factory = configuration.buildValidatorFactory();}
 * </pre>
 * </li>
 * <li>
 * The third approach allows you to specify explicitly and in a type safe
 * fashion the expected provider.
 * <p/>
 * Optionally you can choose a custom <code>ValidationProviderResolver</code>.
 * 
 * <pre>{@code
 * ACMEConfiguration configuration = Validation
 *    .byProvider(ACMEProvider.class)
 *    .providerResolver( new MyResolverStrategy() )  // optionally set the provider resolver
 *    .configure();
 * ValidatorFactory factory = configuration.buildValidatorFactory();}
 * </pre>
 * </li>
 * </ul>
 * Note:<br/>
 * <ul>
 * <li>
 * The <code>ValidatorFactory</code> object built by the bootstrap process
 * should be cached and shared amongst <code>Validator</code> consumers.</li>
 * <li>
 * This class is thread-safe.</li>
 * </ul>
 * 
 * This class was modified by Google from the original
 * javax.validation.Validation source to make it suitable for GWT.
 */
public class Validation {

  // private class, not exposed
  private static class GenericBootstrapImpl implements GenericBootstrap,
      BootstrapState {

    private ValidationProviderResolver defaultResolver;
    private ValidationProviderResolver resolver;

    public Configuration<?> configure() {
      ValidationProviderResolver aResolver = this.resolver == null
          ? getDefaultValidationProviderResolver() : this.resolver;

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

    public ValidationProviderResolver getDefaultValidationProviderResolver() {
      if (defaultResolver == null) {
        defaultResolver = GWT.create(ValidationProviderResolver.class);
      }
      return defaultResolver;
    }

    public ValidationProviderResolver getValidationProviderResolver() {
      return resolver;
    }

    public GenericBootstrap providerResolver(ValidationProviderResolver resolver) {
      this.resolver = resolver;
      return this;
    }
  }

  // private class, not exposed
  private static class ProviderSpecificBootstrapImpl
      <T extends Configuration<T>, U extends ValidationProvider<T>>
      implements ProviderSpecificBootstrap<T> {

    private ValidationProviderResolver resolver;
    private final Class<U> validationProviderClass;

    public ProviderSpecificBootstrapImpl(Class<U> validationProviderClass) {
      this.validationProviderClass = validationProviderClass;
    }

    /**
     * Determine the provider implementation suitable for byProvider(Class) and
     * delegate the creation of this specific Configuration subclass to the
     * provider.
     *
     * @return a Configuration sub interface implementation
     */
    public T configure() {
      if (validationProviderClass == null) {
        throw new ValidationException(
            "builder is mandatory. Use Validation.byDefaultProvider() to use the generic provider discovery mechanism");
      }
      // used mostly as a BootstrapState
      GenericBootstrapImpl state = new GenericBootstrapImpl();
      if (resolver == null) {
        resolver = state.getDefaultValidationProviderResolver();
      } else {
        // stay null if no resolver is defined
        state.providerResolver(resolver);
      }

      List<ValidationProvider<?>> resolvers;
      try {
        resolvers = resolver.getValidationProviders();
      } catch (RuntimeException re) {
        throw new ValidationException(
            "Unable to get available provider resolvers.", re);
      }

      for (ValidationProvider<?> provider : resolvers) {
        // GWT validation only support exact matches.
        if (validationProviderClass.equals(provider.getClass())) {
          @SuppressWarnings("unchecked")
          ValidationProvider<T> specificProvider = (ValidationProvider<T>) provider;
          return specificProvider.createSpecializedConfiguration(state);
        }
      }
      throw new ValidationException("Unable to find provider: "
          + validationProviderClass);
    }

    /**
     * Optionally define the provider resolver implementation used. If not
     * defined, use the default ValidationProviderResolver
     *
     * @param resolver ValidationProviderResolver implementation used
     *
     * @return self
     */
    public ProviderSpecificBootstrap<T> providerResolver(
        ValidationProviderResolver resolver) {
      this.resolver = resolver;
      return this;
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
   * Build a <code>Configuration</code>. The provider list is resolved using the
   * strategy provided to the bootstrap state.
   *
   * <pre>
   * Configuration&lt?&gt; configuration = Validation
   *    .byDefaultProvider()
   *    .providerResolver( new MyResolverStrategy() )
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
    return new GenericBootstrapImpl();
  }

  /**
   * Build a <code>Configuration</code> for a particular provider
   * implementation. Optionally overrides the provider resolution strategy used
   * to determine the provider.
   * <p/>
   * Used by applications targeting a specific provider programmatically.
   * <p/>
   *
   * <pre>
   * ACMEConfiguration configuration =
   *     Validation.byProvider(ACMEProvider.class)
   *             .providerResolver( new MyResolverStrategy() )
   *             .configure();
   * </pre>
   * , where <code>ACMEConfiguration</code> is the <code>Configuration</code>
   * sub interface uniquely identifying the ACME Bean Validation provider. and
   * <code>ACMEProvider</code> is the <code>ValidationProvider</code>
   * implementation of the ACME provider.
   *
   * @param providerType the <code>ValidationProvider</code> implementation type
   *
   * @return instance building a provider specific <code>Configuration</code>
   *         sub interface implementation.
   */
  public static <T extends Configuration<T>,U extends ValidationProvider<T>>
      ProviderSpecificBootstrap<T> byProvider(Class<U> providerType) {
    return new ProviderSpecificBootstrapImpl<T, U>(providerType);
  }
}
