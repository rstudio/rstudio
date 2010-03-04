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
package com.google.gwt.sample.expenses.shared;

import com.google.gwt.requestfactory.shared.Entity;
import com.google.gwt.requestfactory.shared.EntityListRequest;
import com.google.gwt.user.client.ui.HasValueList;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.ValueStore;
import com.google.gwt.valuestore.shared.Values;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * "Generated" from static methods of {@link com.google.gwt.sample.expenses.domain.Employee}
 */
public class EmployeeRequests {

  private final ValueStore values;
  private int nextFutureId = -1;

  private final Map<Object, Entity<?>> futures = new HashMap<Object, Entity<?>>();

  /**
   * @param dataService
   * @param deltas
   */
  public EmployeeRequests(ValueStore values) {
    this.values = values;
  }

  public EntityListRequest<Employee> findAllEmployees() {
    return new EntityListRequest<Employee>() {
      Set<Property<Employee, ?>> properties = new HashSet<Property<Employee, ?>>();
      private HasValueList<Values<Employee>> watcher;

      public void fire() {
        Employee future = new Employee("" + nextFutureId--, null);
        futures.put(future.getId(), future);
        values.subscribe(watcher, future, properties);

        // TODO(rjrjr) now make the call, and in the callback replace the future
        // with the real id. No no no, no need for the future thing.
        // Just make the subscription in the callback.
      }

      public EntityListRequest<Employee> forProperty(
          Property<Employee, ?> property) {
        properties.add(property);
        return this;
      }

      public EntityListRequest<Employee> to(
          HasValueList<Values<Employee>> watcher) {
        this.watcher = watcher;
        return this;
      }
    };
  }
}
