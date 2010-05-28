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
package com.google.gwt.sample.expenses.gwt.client.place;

import com.google.gwt.valuestore.shared.Record;

/**
 * Place in an app that lists
 * {@link com.google.gwt.valuestore.shared.ValueStore ValueStore} records of a
 * particular type.
 */
public class ListScaffoldPlace extends ScaffoldPlace {
  private final Class<? extends Record> type;

  /**
   * @param key the schema of the entities at this place
   */
  public ListScaffoldPlace(Class<? extends Record> type) {
    assert null != type;
    this.type = type;
  }

  @Override
  public void accept(ScaffoldPlaceProcessor visitor) {
    visitor.process(this);
  }

  @Override
  public <T> T acceptFilter(ScaffoldPlaceFilter<T> filter) {
    return filter.filter(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }
    ListScaffoldPlace other = (ListScaffoldPlace) obj;

    if (!type.equals(other.type)) {
      return false;
    }
    
    return true;
  }

  public Class<? extends Record> getType() {
    return type;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }
}
