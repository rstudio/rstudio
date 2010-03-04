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

import java.util.Collection;
import java.util.List;

/**
 * Set of changes to a ValueStore.
 */
public interface DeltaValueStore extends ValueStore {
  <T, V> void editList(T entity, Property<T, List<V>> property,
      boolean replace, int index, Collection<V> newValues);

  boolean isChanged();

  <T, V> void setValue(T type, Property<T, V> property, V value);

  /**
   * Returns true if all validations have passed. May notify subscribers that
   * implement {@com.google.gwt.user.client.ui.HasErrors} of new validation
   * errors.
   */
  boolean validate();
}
