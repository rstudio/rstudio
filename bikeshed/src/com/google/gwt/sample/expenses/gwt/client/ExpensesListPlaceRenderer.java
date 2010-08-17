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
package com.google.gwt.sample.expenses.gwt.client;

import com.google.gwt.app.place.ProxyListPlace;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpensesEntityTypesProcessor;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.text.shared.AbstractRenderer;

/**
 * Renders {@link ProxyListPlace}s for display to users.
 */
//TODO i18n
public class ExpensesListPlaceRenderer extends AbstractRenderer<ProxyListPlace> {

  public String render(ProxyListPlace object) {
    return new ExpensesEntityTypesProcessor<String>() {
      @Override
      public void handleEmployee(EmployeeRecord isNull) {
        setResult("Employees");
      }
      @Override
      public void handleReport(ReportRecord isNull) {
        setResult("Reports");
      }
    }.process(object.getProxyClass());
  }
}
