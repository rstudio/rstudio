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
package com.google.gwt.examples.cellview;

import com.google.gwt.cell.client.TextInputCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.Arrays;
import java.util.List;

/**
 * Example of using a {@link ValueUpdater} with a {@link CellList}.
 */
public class CellListValueUpdaterExample implements EntryPoint {

  /**
   * The list of data to display.
   */
  private static final List<String> DAYS = Arrays.asList("Sunday", "Monday",
      "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");

  public void onModuleLoad() {
    // Create a cell that will interact with a value updater.
    TextInputCell inputCell = new TextInputCell();

    // Create a CellList that uses the cell.
    CellList<String> cellList = new CellList<String>(inputCell);

    // Create a value updater that will be called when the value in a cell
    // changes.
    ValueUpdater<String> valueUpdater = new ValueUpdater<String>() {
      public void update(String newValue) {
        Window.alert("You typed: " + newValue);
      }
    };

    // Add the value updater to the cellList.
    cellList.setValueUpdater(valueUpdater);

    // Set the total row count. This isn't strictly necessary, but it affects
    // paging calculations, so its good habit to keep the row count up to date.
    cellList.setRowCount(DAYS.size(), true);

    // Push the data into the widget.
    cellList.setRowData(0, DAYS);

    // Add it to the root panel.
    RootPanel.get().add(cellList);
  }
}