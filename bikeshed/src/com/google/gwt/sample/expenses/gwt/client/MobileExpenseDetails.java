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

import com.google.gwt.core.client.GWT;
import com.google.gwt.sample.expenses.gwt.request.ExpenseRecord;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import java.util.Date;

/**
 * TODO
 */
public class MobileExpenseDetails extends Composite {

  interface Binder extends UiBinder<Widget, MobileExpenseDetails> { }
  private static Binder BINDER = GWT.create(Binder.class);

  @UiField TextBox nameText, categoryText, priceText;
  @UiField ListBox dateYear, dateMonth, dateDay;

  public MobileExpenseDetails() {
    initWidget(BINDER.createAndBindUi(this));

    populateList(dateYear, 2000, 2010);
    populateList(dateMonth, 1, 12);
    populateList(dateDay, 1, 31);
  }

  private void populateList(ListBox list, int start, int end) {
    for (int i = start; i <= end; ++i) {
      if (i < 10) {
        list.addItem("0" + i);
      } else {
        list.addItem("" + i);
      }
    }
  }

  public void show(ExpenseRecord expense) {
    nameText.setText(expense.getDescription());
    categoryText.setText(expense.getCategory());
    priceText.setText(ExpensesMobile.formatCurrency(expense.getAmount().intValue()));

    Date d = expense.getDate();
    dateYear.setSelectedIndex(d.getYear() + 1900 - 2000);
    dateMonth.setSelectedIndex(d.getMonth());
    dateDay.setSelectedIndex(d.getDate() - 1);
  }
}
