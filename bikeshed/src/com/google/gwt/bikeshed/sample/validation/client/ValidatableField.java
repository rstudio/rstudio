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
package com.google.gwt.bikeshed.sample.validation.client;

/**
 * A field value with a pending future value and a valid flag.
 *
 * @param <T> the value type of the field
 */
public interface ValidatableField<T> {
  T getPendingValue();
  T getValue();
  boolean isInvalid();
  void setInvalid(boolean isInvalid);
  void setPendingValue(T pendingValue);
  void setValue(T value);

  /**
   * Default implementation of ValidatableField.
   *
   * @param <T> the value type of the field
   */
  public static class DefaultValidatableField<T> implements ValidatableField<T> {
    static int genserial = 0;
    int serial;
    boolean isInvalid;
    T pendingValue;
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
      this.pendingValue = other.getPendingValue();
      this.isInvalid = other.isInvalid();
    }

    public T getPendingValue() {
      return pendingValue;
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

    public void setPendingValue(T pendingValue) {
      this.pendingValue = pendingValue;
    }

    public void setValue(T value) {
      this.value = value;
    }
  }
}
