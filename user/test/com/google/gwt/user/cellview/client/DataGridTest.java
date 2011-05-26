/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.user.cellview.client;

import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;

/**
 * Tests for {@link DataGrid}.
 */
public class DataGridTest extends AbstractCellTableTestBase<DataGrid<String>> {

  @Override
  protected DataGrid<String> createAbstractHasData() {
    return new DataGrid<String>();
  }

  @Override
  protected TableCellElement getBodyElement(DataGrid<String> table, int row, int column) {
    TableElement tableElem = table.tableData.getElement().cast();
    TableSectionElement tbody = tableElem.getTBodies().getItem(0);
    TableRowElement tr = tbody.getRows().getItem(row);
    return tr.getCells().getItem(column);
  }

  @Override
  protected int getHeaderCount(DataGrid<String> table) {
    TableElement tableElem = table.tableHeader.getElement().cast();
    TableSectionElement thead = tableElem.getTHead();
    TableRowElement tr = thead.getRows().getItem(0);
    return tr.getCells().getLength();
  }

  @Override
  protected TableCellElement getHeaderElement(DataGrid<String> table, int column) {
    TableElement tableElem = table.tableHeader.getElement().cast();
    TableSectionElement thead = tableElem.getTHead();
    TableRowElement tr = thead.getRows().getItem(0);
    return tr.getCells().getItem(column);
  }
}
