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

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

/**
 * Run by {@link MicrobenchmarkSurvey}, see name for details.
 */
public class TestCreateTableDomWithEvents extends TestCreateTableDom {
  public static class Maker extends MicrobenchmarkSurvey.WidgetMaker {

    Maker() {
      super("Create " + Util.TABLE_ROW_COUNT + "x" + Util.TABLE_COLUMN_COUNT
          + " table via DOM api calls, no widgets, sink events on each cell");
    }

    @Override
    public Widget make() {
      return new TestCreateTableDomWithEvents();
    }
  }

  public static class Updater extends TestCreateTableDom.Updater {
    Updater() {
      super("Replace rows in " + Util.TABLE_ROW_COUNT + "x" + Util.TABLE_COLUMN_COUNT
          + " table via DOM api calls, no widgets, sink events on each cell");
    }

    @Override
    protected TestCreateTableDom make() {
      return new TestCreateTableDomWithEvents();
    }
  }

  @Override
  Element createCellContents(int row, int column) {
    Element div = super.createCellContents(row, column);
    Event.sinkEvents(div, Event.ONCLICK);
    return div;
  }
}
