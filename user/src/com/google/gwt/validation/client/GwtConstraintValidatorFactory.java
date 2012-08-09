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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * GWT does not support {@link ConstraintValidatorFactory} use
 * {@link com.google.gwt.core.client.GWT#create(Class) GWT.create(Class)} instead. Using this
 * class throws a {@link UnsupportedOperationException}.
 */
public final class GwtConstraintValidatorFactory implements
    ConstraintValidatorFactory {

  /**
   * Always throws {@link UnsupportedOperationException}.
   * 
   * @throws UnsupportedOperationException
   */
  @Override
  public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
    throw new UnsupportedOperationException("GWT does not support "
        + ConstraintValidatorFactory.class.getName()
        + " use GWT.create instead");
  }

}
