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
package com.google.gwt.sample.expenses.gen;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.requestfactory.shared.EntityListRequest;
import com.google.gwt.sample.expenses.shared.EmployeeKey;
import com.google.gwt.sample.expenses.shared.ExpenseRequestFactory;
import com.google.gwt.user.client.ui.HasValueList;
import com.google.gwt.valuestore.client.ValuesImpl;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.ValueStore;
import com.google.gwt.valuestore.shared.Values;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * "Generated" from static methods of {@link com.google.gwt.sample.expenses.server.domain.Employee}.
 */
public class EmployeeRequestImpl implements ExpenseRequestFactory.EmployeeRequest {

  @SuppressWarnings("unused") // TODO next step is to use it
  private ValueStore valueStore;

  public EmployeeRequestImpl(ValueStore valueStore) {
    this.valueStore = valueStore;
  }

  public EntityListRequest<EmployeeKey> findAllEmployees() {
    
    
    return new EntityListRequest<EmployeeKey>() {
      private HasValueList<Values<EmployeeKey>> watcher;
      private Set<Property<EmployeeKey, ?>> properties = new HashSet<Property<EmployeeKey, ?>>();

      public void fire() {
        // TODO: accumulate and batch fire requests, e.g. once batch per event loop
        // TODO: cache and short circuit find requests
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
            "/expenses/data?methodName=" + MethodName.FIND_ALL_EMPLOYEES.name());
        builder.setCallback(new RequestCallback() {

          public void onError(Request request, Throwable exception) {
            // shell.error.setInnerText(SERVER_ERROR);
          }

          public void onResponseReceived(Request request, Response response) {
            if (200 == response.getStatusCode()) {
              String text = response.getText();
//              DeltaValueStore deltaStore = valueStore.edit();
              JsArray<ValuesImpl<EmployeeKey>> valueArray = ValuesImpl.arrayFromJson(text);
              List<Values<EmployeeKey>> valueList = new ArrayList<Values<EmployeeKey>>(
                  valueArray.length());
              for (int i = 0; i < valueArray.length(); i++) {
                ValuesImpl<EmployeeKey> values = valueArray.get(i);
                values.setPropertyHolder(EmployeeKey.get());
//                deltaStore.setValue(propertyHolder, properties, values);
                valueList.add(values);
              }

//              valueStore.subscribe(watcher, valueList, properties);
//              deltaStore.commit();
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
      }

      public EntityListRequest<EmployeeKey> forProperties(
          Collection<Property<EmployeeKey, ?>> properties) {
        for (Property<EmployeeKey, ?> property : properties) {
          forProperty(property);
        }
        return this;
      }

      public EntityListRequest<EmployeeKey> forProperty(
          Property<EmployeeKey, ?> property) {
        properties.add(property);
        return this;
      }

      public EntityListRequest<EmployeeKey> to(
          HasValueList<Values<EmployeeKey>> watcher) {
        this.watcher = watcher;
        return this;
      }
    };
  }
}
