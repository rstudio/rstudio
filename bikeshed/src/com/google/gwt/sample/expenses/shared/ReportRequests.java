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
import com.google.gwt.requestfactory.shared.EntityListRequest;
import com.google.gwt.requestfactory.shared.Slot;
import com.google.gwt.sample.expenses.client.ValuesImpl;
import com.google.gwt.user.client.ui.HasValueList;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.ValueStore;
import com.google.gwt.valuestore.shared.Values;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * "Generated" from static methods of
 * {@link com.google.gwt.sample.expenses.domain.Employee}
 */
public class ReportRequests {

  public ReportRequests(ValueStore values) {
  }

  public EntityListRequest<Report> findReportsByEmployee(
      final Slot<Employee, String> id) {

    return new EntityListRequest<Report>() {
      Set<Property<Report, ?>> properties = new HashSet<Property<Report, ?>>();
      private HasValueList<Values<Report>> watcher;

      public void fire() {

        // TODO: need some way to track that this request has been issued so that
        // we don't issue another request that arrives while we are waiting for
        // the response.
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
            "/expenses/data?methodName="
                + MethodName.FIND_REPORTS_BY_EMPLOYEE.name() + "&id="
                + id.getEntity().getId());
        builder.setCallback(new RequestCallback() {

          public void onError(Request request, Throwable exception) {
            // shell.error.setInnerText(SERVER_ERROR);
          }

          public void onResponseReceived(Request request, Response response) {
            if (200 == response.getStatusCode()) {
              String text = response.getText();
              JsArray<ValuesImpl<Report>> valueArray = ValuesImpl.arrayFromJson(text);
              List<Values<Report>> valueList = new ArrayList<Values<Report>>(
                  valueArray.length());
              for (int i = 0; i < valueArray.length(); i++) {
                ValuesImpl<Report> values = valueArray.get(i);
                String id2 = values.get(Report.ID);
                Integer version = values.get(Report.VERSION);
                values.setPropertyHolder(new Report(id2,
                    version));
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

      public EntityListRequest<Report> forProperty(Property<Report, ?> property) {
        properties.add(property);
        return this;
      }

      public EntityListRequest<Report> to(HasValueList<Values<Report>> watcher) {
        this.watcher = watcher;
        return this;
      }
    };
  }
}
