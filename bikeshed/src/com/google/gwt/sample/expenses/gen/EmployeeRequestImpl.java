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
import com.google.gwt.sample.expenses.shared.EmployeeRef;
import com.google.gwt.sample.expenses.shared.ExpenseRequestFactory;
import com.google.gwt.user.client.ui.HasValueList;
import com.google.gwt.valuestore.client.ValuesImpl;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.ValueStore;
import com.google.gwt.valuestore.shared.Values;

import java.util.ArrayList;
import java.util.List;

/**
 * "Generated" from static methods of {@link com.google.gwt.sample.expenses.server.domain.Employee}.
 */
public class EmployeeRequestImpl implements ExpenseRequestFactory.EmployeeRequest {

  @SuppressWarnings("unused")
  public EmployeeRequestImpl(ValueStore values) {
  }

  public EntityListRequest<EmployeeRef> findAllEmployees() {
    return new EntityListRequest<EmployeeRef>() {
      private HasValueList<Values<EmployeeRef>> watcher;

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
              JsArray<ValuesImpl<EmployeeRef>> valueArray = ValuesImpl.arrayFromJson(text);
              // Handy for FireBug snooping
//              Document.get().getBody().setPropertyJSO("foo", valueArray);
              List<Values<EmployeeRef>> valueList = new ArrayList<Values<EmployeeRef>>(
                  valueArray.length());
              for (int i = 0; i < valueArray.length(); i++) {
                ValuesImpl<EmployeeRef> values = valueArray.get(i);
                values.setPropertyHolder(new EmployeeRef(values.get(EmployeeRef.ID),
                    values.get(EmployeeRef.VERSION)));
                valueList.add(values);
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
      }
      
      public EntityListRequest<EmployeeRef> forProperty(
          Property<EmployeeRef, ?> property) {
        return this;
      }

      public EntityListRequest<EmployeeRef> to(
          HasValueList<Values<EmployeeRef>> watcher) {
        this.watcher = watcher;
        return this;
      }
    };
  }
}
