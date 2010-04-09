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
package com.google.gwt.sample.bikeshed.cookbook.client;

/**
 * A field with a pending value and an 'is invalid' flag.
 *
 * @param <T> the value type of the field
 */
public interface ValidatableField<T> {
  T getValue();
  boolean isInvalid();
  void setInvalid(boolean isInvalid);
  void setValue(T value);

  /**
   * Default implementation of ValidatableField.
   *
   * @param <T> the value type of the field
   */
  public static class DefaultValidatableField<T> implements ValidatableField<T> {
    static int genserial = 0; // debugging
    int serial; // debugging
    boolean isInvalid;
    T value;

    public DefaultValidatableField(T value) {
      this.serial = genserial++;
      this.value = value;
    }

    public DefaultValidatableField(ValidatableField<T> other) {
      if (other instanceof DefaultValidatableField<?>) {
        this.serial = ((DefaultValidatableField<T>) other).serial;
      }
      this.value = other.getValue();
      this.isInvalid = other.isInvalid();
    }

    public T getValue() {
      return value;
    }

    public boolean isInvalid() {
      return isInvalid;
    }

    public void setInvalid(boolean isInvalid) {
      this.isInvalid = isInvalid;
    }

    public void setValue(T value) {
      this.value = value;
    }
  }
}
