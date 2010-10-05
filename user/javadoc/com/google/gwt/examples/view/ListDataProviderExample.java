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
package com.google.gwt.examples.view;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;

import java.util.List;

/**
 * Example of {@link ListDataProvider}.
 */
public class ListDataProviderExample implements EntryPoint {

  public void onModuleLoad() {
    // Create a CellList.
    CellList<String> cellList = new CellList<String>(new TextCell());

    // Create a list data provider.
    final ListDataProvider<String> dataProvider = new ListDataProvider<String>();

    // Add the cellList to the dataProvider.
    dataProvider.addDataDisplay(cellList);

    // Create a form to add values to the data provider.
    final TextBox valueBox = new TextBox();
    valueBox.setText("Enter new value");
    Button addButton = new Button("Add value", new ClickHandler() {
      public void onClick(ClickEvent event) {
        // Get the value from the text box.
        String newValue = valueBox.getText();

        // Get the underlying list from data dataProvider.
        List<String> list = dataProvider.getList();

        // Add the value to the list. The dataProvider will update the cellList.
        list.add(newValue);
      }
    });

    // Add the widgets to the root panel.
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.add(valueBox);
    vPanel.add(addButton);
    vPanel.add(cellList);
    RootPanel.get().add(vPanel);
  }
}