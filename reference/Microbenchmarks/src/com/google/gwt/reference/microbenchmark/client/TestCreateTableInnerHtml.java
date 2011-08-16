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
public class TestCreateTableInnerHtml extends Widget {
  public static class Maker extends MicrobenchmarkSurvey.WidgetMaker {
    Maker() {
      super("Create " + Util.TABLE_ROW_COUNT + "x" + Util.TABLE_COLUMN_COUNT
          + " table via innerHTML built with StringBuilder, no widgets");
    }

    @Override
    public Widget make() {
      return new TestCreateTableInnerHtml();
    }
  }

  public static class Updater extends MicrobenchmarkSurvey.WidgetUpdater<TestCreateTableInnerHtml> {
    Updater() {
      super("Update " + Util.TABLE_ROW_COUNT + "x" + Util.TABLE_COLUMN_COUNT
          + " tbody via innerHTML built with StringBuilder, no widgets");
    }

    @Override
    protected TestCreateTableInnerHtml make() {
      return new TestCreateTableInnerHtml();
    }

    @Override
    protected void updateWidget(TestCreateTableInnerHtml w) {
      w.replaceAllRows();
    }
  }

  private final TableElement table;
  private final TableSectionElement  tableBody;

  private TestCreateTableInnerHtml() {
    table = Util.fromHtml(Util.createTableHtml()).cast();
    setElement(table);
    tableBody = table.getTBodies().getItem(0).cast();
  }

  private void replaceAllRows() {
    Util.replaceTableBodyRows(tableBody, Util.createTableRowsHtml());
  }
}
