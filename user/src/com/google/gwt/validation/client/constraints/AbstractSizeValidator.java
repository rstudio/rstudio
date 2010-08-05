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

import javax.validation.ConstraintValidator;
import javax.validation.constraints.Size;

/**
 * Abstract {@link Size} constraint validator implementation.
 *
 * @param <T>
 */
public abstract class AbstractSizeValidator<T> implements
    ConstraintValidator<Size, T> {

  private int min;
  private int max;

  public AbstractSizeValidator() {
    super();
  }

  public final void initialize(Size annotation) {
    if (!(annotation.min() >= 0)) {
      throw new IllegalArgumentException(
          "@Size.min must be a nonnegative nubmer");
    }
    if (!(annotation.max() >= 0)) {
      throw new IllegalArgumentException(
          "@Size.max must be a nonnegative nubmer");
    }
    if (!(annotation.min() <= annotation.max())) {
      throw new IllegalArgumentException(
          "@Size.min must be less than or equal to @Size.max");
    }
    min = annotation.min();
    max = annotation.max();
  }

  protected final boolean isLengthValid(int length) {
    return min <= length && length <= max;
  }
}