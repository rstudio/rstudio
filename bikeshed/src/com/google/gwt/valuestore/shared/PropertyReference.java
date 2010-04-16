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
 * A pointer to a particular property value.
 *
 * @param <V> Value type
 */
public class PropertyReference<V> {
  private final Record record;
  private final Property<V> property;

  public PropertyReference(Record record, Property<V> property) {
    assert null != record;
    assert null != property;

    this.record = record;
    this.property = property;
  }

  public V get() {
    return record.get(property);
  }

  Property<V> getProperty() {
    return property;
  }
}
