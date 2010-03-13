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

import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.HasValueList;

import java.util.Set;

/**
 * A store of property values. Allows interested parties to subscribe to changes
 * to particular sets of values.
 */
public interface ValueStore {
  /**
   * Most validations are per field or per id and set via annotation. Note that
   * validations are only actually enforced by in {@link DeltaValueStore}
   * instances spawned by {@link #edit()}
   */
  void addValidation(/* what's a validation. JSR 303? Learn from Pectin? */);

  /**
   * Returns an editable overlay view of the receiver.
   */
  DeltaValueStore edit();

  <T, V> void subscribe(HasValue<V> watcher, T propertyOwner,
      Property<T, V> property);

  <T, V> void subscribe(HasValueList<Values<T>> watcher, T propertyOwner,
      Set<Property<T, ?>> properties);
}
