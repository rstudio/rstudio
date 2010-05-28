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
package com.google.gwt.sample.bikeshed.cookbook.client;

import com.google.gwt.bikeshed.list.client.CellList;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListViewAdapter;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;

import java.util.List;

/**
 * {@link CellList} Recipe.
 */
public class CellListRecipe extends Recipe {

  public CellListRecipe() {
    super("Cell List");
  }

  @Override
  protected Widget createWidget() {
    ListViewAdapter<String> adapter = new ListViewAdapter<String>();
    final List<String> list = adapter.getList();
    for (int i = 0; i < 40; i++) {
      list.add("" + ((i + 10) * 1000));
    }

    final CellList<String> cellList = new CellList<String>(new TextCell());
    cellList.setPageSize(10);
    final SelectionModel<String> selectionModel = new SingleSelectionModel<String>();
    cellList.setSelectionModel(selectionModel);
    adapter.addView(cellList);

    new Timer() {
      int index = 0;

      @Override
      public void run() {
        if (cellList.isAttached()) {
          incrementValue(index);
          incrementValue(index + 15);
          index = (index + 1) % 10;
        }
        schedule(100);
      }

      private void incrementValue(int i) {
        // Set the value at index.
        String oldValue = list.get(i);
        int number = Integer.parseInt(oldValue);
        String newValue = "" + (number + 1);
        if (selectionModel.isSelected(oldValue)) {
          // Move the selection with the value.
          // TODO(jlabanca): Use a DTO with a unique ID instead.
          selectionModel.setSelected(newValue, true);
        }
        list.set(i, newValue);
      }
    }.schedule(100);

    // Add a Pager to control the table.
    SimplePager<String> pager = new SimplePager<String>(cellList);

    FlowPanel fp = new FlowPanel();
    fp.add(cellList);
    fp.add(pager);
    return fp;
  }
}
