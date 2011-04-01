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
package com.google.gwt.reference.microbenchmark.client;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.user.client.ui.Widget;

/**
 * Run by {@link WidgetCreation}, see {@link TestCreateTableDom.Maker#name} for
 * details.
 */
public class TestCreateTableDom extends Widget {
  public static class Maker extends WidgetCreation.Maker {
    Maker() {
      super(Util.TABLE_ROW_COUNT + "x" + Util.TABLE_COLUMN_COUNT
          + " table via DOM api calls, no widgets");
    }

    @Override
    public Widget make() {
      return new TestCreateTableDom();
    }
  }

  TestCreateTableDom() {
    // This table should match the structure defined in Util#createTableHtml().
    TableElement table = Document.get().createTableElement();
    TableSectionElement tbody = Document.get().createTBodyElement();
    table.appendChild(tbody);
    for (int row = 0; row < Util.TABLE_ROW_COUNT; row++) {
      TableRowElement tr = Document.get().createTRElement();
      tbody.appendChild(tr);
      if (row % 2 == 0) {
        tr.addClassName("evenRow");
      } else {
        tr.addClassName("oddRow");
      }
      for (int column = 0; column < Util.TABLE_COLUMN_COUNT; column++) {
        TableCellElement td = Document.get().createTDElement();
        td.setAlign("center");
        td.setVAlign("middle");
        td.appendChild(createCellContents(row, column));
      }
    }
    setElement(table);
  }

  /**
   * Create the contents of a cell.
   * 
   * @param row the row index
   * @param column the column index
   * @return the cell contents as an element
   */
  Element createCellContents(int row, int column) {
    DivElement div = Document.get().createDivElement();
    div.setInnerHTML("Cell " + row + ":" + column);
    return div;
  }
}
