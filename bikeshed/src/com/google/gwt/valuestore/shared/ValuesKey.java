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

import java.util.Set;

/**
 * The set of {@link Property properties} for {@link ValueStore} records
 * of a particular type.
 *
 * @param <K> type of this key
 */
public interface ValuesKey<K extends ValuesKey<K>> {

  /**
   * @return the set of properties such records have, as well as those of any
   *         supertypes
   */
  Set<? extends Property<K, ?>> all();

  /**
   * @return an event to announce changes to the given record
   */
  ValuesChangedEvent<K, ?> createChangeEvent(Values<K> values);

  /**
   * @return the id property for records of this type
   */
  Property<K, String> getId();

  /**
   * @return the id property for records of this type
   */
  Property<K, Integer> getVersion();
}
