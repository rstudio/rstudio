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
package com.google.gwt.validation.client.constraints;

import java.math.BigDecimal;

import javax.validation.ConstraintValidator;
import javax.validation.constraints.DecimalMin;

/**
 * Abstract {@link DecimalMin} constraint validator implementation for a
 * <code>T</code>.
 *
 * @param <T> the type of object to validate
 */
public abstract class AbstractDecimalMinValidator<T> implements
    ConstraintValidator<DecimalMin, T> {

  private BigDecimal min;

  public final void initialize(DecimalMin constraintAnnotation) {
    try {
      min = new BigDecimal(constraintAnnotation.value());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(constraintAnnotation.value()
          + " does not represent a valid BigDecimal format", e);
    }
  }

  protected final boolean isValid(BigDecimal bigValue) {
    return min.compareTo(bigValue) <= 0;
  }
}