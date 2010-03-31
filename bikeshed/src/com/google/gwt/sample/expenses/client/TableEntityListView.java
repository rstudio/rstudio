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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

/**
 * Interim table based implementation of {@link EntityListView}. Will be replaced
 * by some descendant of {@link com.google.gwt.bikeshed.list.client.PagingTableListView<}
 */
public class TableEntityListView extends Widget implements EntityListView {
  interface Binder extends UiBinder<Element, TableEntityListView> {
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField HeadingElement heading;
  @UiField TableRowElement header;
  @UiField TableElement table;

  private List<Row> values;

  public TableEntityListView() {
    setElement(BINDER.createAndBindUi(this));
    addDomHandler(new ClickHandler() {

      public void onClick(ClickEvent event) {
        EventTarget target = event.getNativeEvent().getEventTarget();
        if (Element.is(target)) {
          Element e = Element.as(target);
          String tagName = e.getTagName().toLowerCase();
          if ("td".equals(tagName)) {
            TableCellElement cell = TableCellElement.as(e);
            int rowIndex = TableRowElement.as(cell.getParentElement()).getRowIndex();
//            int cellIndex = cell.getCellIndex();
            values.get(rowIndex - 1).getShowDetailsCommand().execute(); 
          }
        }
      }
      
    }, ClickEvent.getType());
  }

  public void setColumnNames(List<String> names) {
    NodeList<TableCellElement> cells = header.getCells();
    for (int i = 0; i < names.size(); i++) {
      cells.getItem(i).setInnerText(names.get(i));
    }
  }

  public void setHeading(String text) {
    heading.setInnerText(text);
  }

  public void setRowData(List<Row> newValues) {
    this.values = newValues;
    int r = 1; // skip header
    NodeList<TableRowElement> tableRows = table.getRows();

    for (int i = 0; i < newValues.size(); i++) {
      List<String> valueRow = newValues.get(i).getValues();

      if (r < tableRows.getLength()) {
        reuseRow(r, tableRows, valueRow);
      } else {
        TableRowElement tableRow = Document.get().createTRElement();
        for (String s : valueRow) {
          TableCellElement tableCell = Document.get().createTDElement();
          tableCell.setInnerText(s);
          tableRow.appendChild(tableCell);
        }
        table.appendChild(tableRow);
      }
      r++;
    }
    while (r < tableRows.getLength()) {
      table.removeChild(tableRows.getItem(r));
    }
  }

  private void reuseRow(int r, NodeList<TableRowElement> tableRows,
      List<String> valueRow) {
    TableRowElement tableRow = tableRows.getItem(r);
    NodeList<TableCellElement> tableCells = tableRow.getCells();

    for (int i = 0; i < valueRow.size(); i++) {
      tableCells.getItem(i).setInnerText(valueRow.get(i));
    }
  }
}
