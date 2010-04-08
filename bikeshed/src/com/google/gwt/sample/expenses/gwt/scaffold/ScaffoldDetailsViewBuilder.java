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
package com.google.gwt.sample.expenses.gwt.scaffold;

import com.google.gwt.sample.expenses.gwt.request.EmployeeKey;
import com.google.gwt.sample.expenses.gwt.request.ExpensesKey;
import com.google.gwt.sample.expenses.gwt.request.ExpensesKeyVisitor;
import com.google.gwt.sample.expenses.gwt.request.ReportKey;
import com.google.gwt.sample.expenses.gwt.ui.employee.EmployeeDetailsBuilder;
import com.google.gwt.sample.expenses.gwt.ui.report.ReportDetailsBuilder;
import com.google.gwt.valuestore.shared.Values;

/**
 * Builds the details view for a record.
 * <p>
 * TODO should instead be a finder of pretty uibinder-based widgets, not least
 * because we're rendering user strings here, which is dangerous
 */
public class ScaffoldDetailsViewBuilder {
  private final EmployeeDetailsBuilder employeeBuilder = new EmployeeDetailsBuilder();
  private final ReportDetailsBuilder reportBuilder = new ReportDetailsBuilder();

  @SuppressWarnings("unchecked")
  public void appendHtmlDescription(final StringBuilder list,
      final Values<? extends ExpensesKey<?>> entity) {

    // TODO These casts are nasty, but they probably wouldn't be necessary
    // if we were listening for request reponses they way we're supposed to

    entity.getKey().accept(new ExpensesKeyVisitor() {
      public void visit(EmployeeKey employeeKey) {
        employeeBuilder.append(list, (Values<EmployeeKey>) entity);
      }

      public void visit(ReportKey reportKey) {
        reportBuilder.append(list, (Values<ReportKey>) entity);
      }
    });
  }
}
