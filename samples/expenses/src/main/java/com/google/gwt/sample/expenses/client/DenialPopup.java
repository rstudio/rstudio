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
package com.google.gwt.sample.expenses.client;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.sample.expenses.client.style.Styles;
import com.google.gwt.sample.expenses.shared.ExpenseProxy;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;

/**
 * The popup used to enter the rejection reason.
 */
  class DenialPopup extends PopupPanel {
  private final Button cancelButton = new Button("Cancel",
      new ClickHandler() {
        public void onClick(ClickEvent event) {
          reasonDenied = "";
          hide();
        }
      });
  private final Button confirmButton = new Button("Confirm",
      new ClickHandler() {
        public void onClick(ClickEvent event) {
          reasonDenied = reasonBox.getText();
          hide();
        }
      });

  private ExpenseProxy expenseRecord;
  private final TextBox reasonBox = new TextBox();
  private String reasonDenied;

  public DenialPopup() {
    super(false, true);
    setStyleName(Styles.common().popupPanel());
    setGlassEnabled(true);
    confirmButton.setWidth("11ex");
    cancelButton.setWidth("11ex");
    reasonBox.getElement().getStyle().setMarginLeft(10.0, Unit.PX);
    reasonBox.getElement().getStyle().setMarginRight(10.0, Unit.PX);

    HorizontalPanel hPanel = new HorizontalPanel();
    hPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    hPanel.add(new HTML("<b>Reason:</b>"));
    hPanel.add(reasonBox);
    hPanel.add(confirmButton);
    hPanel.add(cancelButton);
    setWidget(hPanel);
    cancelButton.getElement().getParentElement().getStyle().setPaddingLeft(
        5.0, Unit.PX);
  }

  public ExpenseProxy getExpenseRecord() {
    return expenseRecord;
  }

  public String getReasonDenied() {
    return reasonDenied;
  }

  public void popup() {
    center();
    reasonBox.setFocus(true);
  }

  public void setExpenseRecord(ExpenseProxy expenseRecord) {
    this.expenseRecord = expenseRecord;
  }

  public void setReasonDenied(String reasonDenied) {
    this.reasonDenied = reasonDenied;
    reasonBox.setText(reasonDenied);
  }
}