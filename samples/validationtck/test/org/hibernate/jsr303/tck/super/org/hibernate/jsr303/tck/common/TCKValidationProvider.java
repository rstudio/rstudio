// $Id: TCKValidationProvider.java 17620 2009-10-04 19:19:28Z hardy.ferentschik $
/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.jsr303.tck.common;

import javax.validation.Configuration;
import javax.validation.MessageInterpolator;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;
import javax.validation.TraversableResolver;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.spi.BootstrapState;
import javax.validation.spi.ConfigurationState;
import javax.validation.spi.ValidationProvider;

/**
 *
 * Modified by Google:
 * <ul>
 * <li>Replaced cast with a simple new.</li>
 * </ul>
 * @author Hardy Ferentschik
 */
public class TCKValidationProvider implements ValidationProvider<TCKValidatorConfiguration> {

  public TCKValidatorConfiguration createSpecializedConfiguration(BootstrapState state) {
    return  new TCKValidatorConfiguration( this );
  }

  public Configuration<?> createGenericConfiguration(BootstrapState state) {
    return new TCKValidatorConfiguration( this );
  }

  public ValidatorFactory buildValidatorFactory(ConfigurationState configurationState) {
    return new DummyValidatorFactory();
  }

  public static class DummyValidatorFactory implements ValidatorFactory {

    public Validator getValidator() {
      throw new UnsupportedOperationException();
    }

    public ValidatorContext usingContext() {
      throw new UnsupportedOperationException();
    }

    public MessageInterpolator getMessageInterpolator() {
      throw new UnsupportedOperationException();
    }

    public TraversableResolver getTraversableResolver() {
      throw new UnsupportedOperationException();
    }

    public ConstraintValidatorFactory getConstraintValidatorFactory() {
      throw new UnsupportedOperationException();
    }

    public <T> T unwrap(Class<T> type) {
      throw new UnsupportedOperationException();
    }
  }
}
