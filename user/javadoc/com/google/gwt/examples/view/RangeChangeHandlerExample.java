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
package com.google.gwt.examples.view;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Example of using a {@link RangeChangeEvent.Handler} to push data into a
 * {@link CellList} when the range changes.
 */
public class RangeChangeHandlerExample implements EntryPoint {

  @Override
  public void onModuleLoad() {
    // Create a CellList.
    final CellList<String> cellList = new CellList<String>(new TextCell());

    // Add a range change handler.
    cellList.addRangeChangeHandler(new RangeChangeEvent.Handler() {
      @Override
      public void onRangeChange(RangeChangeEvent event) {
        Range range = event.getNewRange();
        int start = range.getStart();
        int length = range.getLength();

        // Create the data to push into the view. At this point, you could send
        // an asynchronous RPC request to a server.
        List<String> data = new ArrayList<String>();
        for (int i = start; i < start + length; i++) {
          data.add("Item " + i);
        }

        // Push the data into the list.
        cellList.setRowData(start, data);
      }
    });

    // Force the cellList to fire an initial range change event.
    cellList.setVisibleRangeAndClearData(new Range(0, 25), true);

    // Create paging controls.
    SimplePager pager = new SimplePager();
    pager.setDisplay(cellList);

    // Add the widgets to the root panel.
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.add(pager);
    vPanel.add(cellList);
    RootPanel.get().add(vPanel);
  }
}