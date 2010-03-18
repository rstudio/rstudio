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
import com.google.gwt.requestfactory.shared.FieldRef;
import com.google.gwt.sample.expenses.shared.EmployeeRef;
import com.google.gwt.sample.expenses.shared.ExpenseRequestFactory;
import com.google.gwt.sample.expenses.shared.ReportRef;
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
 * "Generated" from static methods of
 * {@link com.google.gwt.sample.expenses.server.domain.Employee}.
 */
public class ReportRequestImpl implements ExpenseRequestFactory.ReportRequest {

  @SuppressWarnings("unused")
  public ReportRequestImpl(ValueStore values) {
  }

  public EntityListRequest<ReportRef> findReportsByEmployee(
      final FieldRef<EmployeeRef, String> id) {

    return new EntityListRequest<ReportRef>() {
      Set<Property<ReportRef, ?>> properties = new HashSet<Property<ReportRef, ?>>();
      private HasValueList<Values<ReportRef>> watcher;

      public void fire() {

        // TODO: need some way to track that this request has been issued so
        // that we don't issue another request that arrives while we are waiting
        // for the response.
        RequestBuilder builder = new RequestBuilder(
            RequestBuilder.GET,
            "/expenses/data?methodName="
                + MethodName.FIND_REPORTS_BY_EMPLOYEE.name()
                + UrlParameterManager.getUrlFragment(new Object[] {id.getEntity().getId()}));
        builder.setCallback(new RequestCallback() {

          public void onError(Request request, Throwable exception) {
            // shell.error.setInnerText(SERVER_ERROR);
          }

          public void onResponseReceived(Request request, Response response) {
            if (200 == response.getStatusCode()) {
              String text = response.getText();
              JsArray<ValuesImpl<ReportRef>> valueArray = ValuesImpl.arrayFromJson(text);
              List<Values<ReportRef>> valueList = new ArrayList<Values<ReportRef>>(
                  valueArray.length());
              for (int i = 0; i < valueArray.length(); i++) {
                ValuesImpl<ReportRef> values = valueArray.get(i);
                String id2 = values.get(ReportRef.ID);
                Integer version = values.get(ReportRef.VERSION);
                values.setPropertyHolder(new ReportRef(id2,
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

      public EntityListRequest<ReportRef> forProperties(
          Collection<Property<ReportRef, ?>> properties) {
        for (Property<ReportRef, ?> property : properties) {
          forProperty(property);
        }
        return this;
      }

      public EntityListRequest<ReportRef> forProperty(Property<ReportRef, ?> property) {
        properties.add(property);
        return this;
      }


      public EntityListRequest<ReportRef> to(HasValueList<Values<ReportRef>> watcher) {
        this.watcher = watcher;
        return this;
      }
    };
  }

  public EntityListRequest<ReportRef> findAllReports() {
    return new EntityListRequest<ReportRef>() {
      private HasValueList<Values<ReportRef>> watcher;

      public void fire() {

        // TODO: need someway to track that this request has been issued so that
        // we don't issue another request that arrives while we are waiting for
        // the response.
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
            "/expenses/data?methodName=" + MethodName.FIND_ALL_REPORTS.name());
        builder.setCallback(new RequestCallback() {

          public void onError(Request request, Throwable exception) {
            // shell.error.setInnerText(SERVER_ERROR);
          }

          public void onResponseReceived(Request request, Response response) {
            if (200 == response.getStatusCode()) {
              String text = response.getText();
              JsArray<ValuesImpl<ReportRef>> valueArray = ValuesImpl.arrayFromJson(text);
              // Handy for FireBug snooping
//              Document.get().getBody().setPropertyJSO("foo", valueArray);
              List<Values<ReportRef>> valueList = new ArrayList<Values<ReportRef>>(
                  valueArray.length());
              for (int i = 0; i < valueArray.length(); i++) {
                ValuesImpl<ReportRef> values = valueArray.get(i);
                values.setPropertyHolder(new ReportRef(values.get(ReportRef.ID),
                    values.get(ReportRef.VERSION)));
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
      
      public EntityListRequest<ReportRef> forProperties(
          Collection<Property<ReportRef, ?>> properties) {
        for (Property<ReportRef, ?> property : properties) {
          forProperty(property);
        }
        return this;
      }

      public EntityListRequest<ReportRef> forProperty(
          Property<ReportRef, ?> property) {
        return this;
      }

      public EntityListRequest<ReportRef> to(
          HasValueList<Values<ReportRef>> watcher) {
        this.watcher = watcher;
        return this;
      }
    };
  }
}
