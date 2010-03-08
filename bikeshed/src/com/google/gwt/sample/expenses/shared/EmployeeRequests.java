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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.requestfactory.shared.Entity;
import com.google.gwt.requestfactory.shared.EntityListRequest;
import com.google.gwt.sample.expenses.client.ValuesImpl;
import com.google.gwt.user.client.ui.HasValueList;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.ValueStore;
import com.google.gwt.valuestore.shared.Values;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Generated" from static methods of
 * {@link com.google.gwt.sample.expenses.domain.Employee}.
 */
public class EmployeeRequests {

  private final ValueStore values;

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

        // TODO: need someway to track that this request has been issued so that
        // we don't issue another request that arrives while we are waiting for
        // the response.
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
            "/expenses/data?methodName=" + MethodName.FIND_ALL_EMPLOYEES.name());
        builder.setCallback(new RequestCallback() {

          public void onError(Request request, Throwable exception) {
            // shell.error.setInnerText(SERVER_ERROR);
          }

          public void onResponseReceived(Request request, Response response) {
            if (200 == response.getStatusCode()) {
              String text = response.getText();
              JsArray<ValuesImpl<Employee>> valueArray = ValuesImpl.arrayFromJson(text);
              List<Values<Employee>> valueList = new ArrayList<Values<Employee>>(
                  valueArray.length());
              for (int i = 0; i < valueArray.length(); i++) {
                valueList.add(valueArray.get(i));
              }
              watcher.setValueList(valueList);
            } else {
              // shell.error.setInnerText(SERVER_ERROR + " ("
              // + response.getStatusText() + ")");
            }
          }
        });

        try {
          builder.send();
        } catch (RequestException e) {
          // shell.error.setInnerText(SERVER_ERROR + " (" + e.getMessage() +
          // ")");
        }

        // values.subscribe(watcher, future, properties);

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
