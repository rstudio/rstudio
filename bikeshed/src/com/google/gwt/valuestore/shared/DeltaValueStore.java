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
package com.google.gwt.valuestore.shared;

/**
 * Set of changes to a ValueStore.
 */
public interface DeltaValueStore extends ValueStore {
  Record create(Record existingRecord);

  void delete(Record record);

  /**
   * Return true if there are outstanding changes that have not been
   * communicated to the server yet. Note that it is illegal to call this method
   * after a request using it has been fired.
   */
  boolean isChanged();

  <V> void set(Property<V> property, Record record, V value);

  /**
   * Returns true if all validations have passed. May notify subscribers that
   * implement {@link com.google.gwt.user.client.ui.HasErrors HasErrors} of new
   * validation errors.
   */
  boolean validate();
}
