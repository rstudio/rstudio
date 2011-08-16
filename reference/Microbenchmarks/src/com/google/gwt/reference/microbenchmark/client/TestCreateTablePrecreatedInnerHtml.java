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

import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.user.client.ui.Widget;

/**
 * Run by {@link MicrobenchmarkSurvey}, see name for details.
 */
public class TestCreateTablePrecreatedInnerHtml extends Widget {
  public static class Maker extends MicrobenchmarkSurvey.WidgetMaker {

    private final String tableHtml = Util.createTableHtml();

    Maker() {
      super("Create " + Util.TABLE_ROW_COUNT + "x" + Util.TABLE_COLUMN_COUNT
          + " tbody via precreated innerHTML String, no widgets");
    }

    @Override
    public Widget make() {
      return new TestCreateTablePrecreatedInnerHtml(tableHtml);
    }
  }

  public static class Updater extends
      MicrobenchmarkSurvey.WidgetUpdater<TestCreateTablePrecreatedInnerHtml> {

    private final String tableRowsHtml = Util.createTableRowsHtml();

    Updater() {
      super("Update " + Util.TABLE_ROW_COUNT + "x" + Util.TABLE_COLUMN_COUNT
          + " table via precreated innerHTML String, no widgets");
    }

    @Override
    protected TestCreateTablePrecreatedInnerHtml make() {
      return new TestCreateTablePrecreatedInnerHtml(Util.createTableHtml());
    }

    @Override
    protected void updateWidget(TestCreateTablePrecreatedInnerHtml w) {
      w.replaceAllRows(tableRowsHtml);
    }
  }

  private final TableElement table;
  private final TableSectionElement tableBody;

  private TestCreateTablePrecreatedInnerHtml(String tableHtml) {
    table = Util.fromHtml(tableHtml).cast();
    setElement(table);
    tableBody = table.getTBodies().getItem(0).cast();
  }

  private void replaceAllRows(String tableRowsHtml) {
    Util.replaceTableBodyRows(tableBody, tableRowsHtml);
  }
}
