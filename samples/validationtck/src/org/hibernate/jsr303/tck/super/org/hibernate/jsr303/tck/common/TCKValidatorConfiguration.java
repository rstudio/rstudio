// $Id: TCKValidatorConfiguration.java 17620 2009-10-04 19:19:28Z hardy.ferentschik $
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
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.ValidatorFactory;
import javax.validation.spi.ValidationProvider;

/**
 * Modified by Google:
 * <ul>
 * <li>Changed InputStream to String</li>
 * </ul>
 * @author Hardy Ferentschik
 */
public class TCKValidatorConfiguration implements Configuration<TCKValidatorConfiguration> {
  private final ValidationProvider provider;

  public TCKValidatorConfiguration() {
    provider = null;
  }

  public TCKValidatorConfiguration(ValidationProvider provider) {
    this.provider = provider;
  }

  public TCKValidatorConfiguration ignoreXmlConfiguration() {
    throw new UnsupportedOperationException();
  }

  public TCKValidatorConfiguration messageInterpolator(MessageInterpolator interpolator) {
    throw new UnsupportedOperationException();
  }

  public TCKValidatorConfiguration traversableResolver(TraversableResolver resolver) {
    throw new UnsupportedOperationException();
  }

  public TCKValidatorConfiguration constraintValidatorFactory(ConstraintValidatorFactory constraintValidatorFactory) {
    throw new UnsupportedOperationException();
  }

  public TCKValidatorConfiguration addMapping(String stream) {
    throw new UnsupportedOperationException();
  }

  public TCKValidatorConfiguration addProperty(String name, String value) {
    throw new UnsupportedOperationException();
  }

  public MessageInterpolator getDefaultMessageInterpolator() {
    throw new UnsupportedOperationException();
  }

  public TraversableResolver getDefaultTraversableResolver() {
    throw new UnsupportedOperationException();
  }

  public ConstraintValidatorFactory getDefaultConstraintValidatorFactory() {
    throw new UnsupportedOperationException();
  }

  public ValidatorFactory buildValidatorFactory() {
    return provider.buildValidatorFactory( null );
  }

}
