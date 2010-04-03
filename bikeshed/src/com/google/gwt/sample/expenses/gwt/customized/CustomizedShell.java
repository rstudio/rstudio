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
package com.google.gwt.sample.expenses.gwt.customized;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.sample.expenses.gwt.request.ReportChanged;
import com.google.gwt.sample.expenses.gwt.request.ReportKey;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TakesValueList;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Values;
import com.google.gwt.valuestore.shared.ValuesKey;

import java.util.Date;
import java.util.List;

/**
 * UI shell for expenses sample app. A horrible clump of stuff that should be
 * refactored into proper MVP pieces.
 */
public class CustomizedShell extends Composite implements TakesValueList<Values<ReportKey>>, ReportChanged.Handler {
  interface Listener {
    void setFirstPurpose(String purpose);
  }
  
  interface ShellUiBinder extends UiBinder<Widget, CustomizedShell> {
  } 

  private static ShellUiBinder uiBinder = GWT.create(ShellUiBinder.class);

  private Listener listener;

  @UiField
  Element error;
  @UiField
  TableElement table;
  @UiField
  TableRowElement header;
  @UiField
  ListBox users;
  @UiField
  TextBox purpose;
  @UiField
  Button save;
  private List<Values<ReportKey>> values;

  public CustomizedShell() {
    initWidget(uiBinder.createAndBindUi(this));
  }

  public List<Values<ReportKey>> getValues() {
    return values;
  }
  
  @UiHandler("purpose") 
  public void onPurposeChange(ValueChangeEvent<String> e) {
    listener.setFirstPurpose(e.getValue());
  }
  
  public void onReportChanged(ReportChanged event) {
    refresh();
  }
  
  @UiHandler("save")
  @SuppressWarnings("unused")
  public void onSaveClick(ClickEvent e) {
    listener.setFirstPurpose(purpose.getValue());
  }
  
  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public void setValueList(List<Values<ReportKey>> newValues) {
    this.values = newValues;
    refresh();
  }

  private void refresh() {
    int r = 1; // skip header
    NodeList<TableRowElement> tableRows = table.getRows();
    purpose.setText("");
    boolean enabled = values.size() > 0;
    purpose.setEnabled(enabled);
    save.setEnabled(enabled);
    for (int i = 0; i < values.size(); i++) {
      Values<ReportKey> valueRow = values.get(i);

      if (i == 0) {
        purpose.setText(valueRow.get(ReportKey.get().getPurpose()));
      }
      if (r < tableRows.getLength()) {
        reuseRow(r, tableRows, valueRow);
      } else {
        TableRowElement tableRow = Document.get().createTRElement();

        TableCellElement tableCell = Document.get().createTDElement();
        tableCell.setInnerText(renderDate(valueRow, ReportKey.get().getCreated()));
        tableRow.appendChild(tableCell);

        tableCell = Document.get().createTDElement();
        /* status goes here */
        tableRow.appendChild(tableCell);

        tableCell = Document.get().createTDElement();
        tableCell.setInnerText(valueRow.get(ReportKey.get().getPurpose()));
        tableRow.appendChild(tableCell);

        table.appendChild(tableRow);
      }
      r++;
    }
    while (r < tableRows.getLength()) {
      table.removeChild(tableRows.getItem(r));
    }
  }

  private <K extends ValuesKey<K>> String renderDate(Values<K> values, Property<K, Date> property) {
    return DateTimeFormat.getShortDateFormat().format(values.get(property));
  }

  /**
   * @param r
   * @param tableRows
   * @param valueRow
   */
  private void reuseRow(int r, NodeList<TableRowElement> tableRows,
      Values<ReportKey> valueRow) {
    TableRowElement tableRow = tableRows.getItem(r);
    NodeList<TableCellElement> tableCells = tableRow.getCells();

    // tableCells.getItem(0).setInnerText(valueRow.get(Report.instance().CREATED).toString());
    tableCells.getItem(2).setInnerText(valueRow.get(ReportKey.get().getPurpose()));
  }
}
