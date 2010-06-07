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

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListViewAdapter;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionModel;

import java.util.List;

/**
 * Basic table recipe.
 */
public class BasicTableRecipe extends Recipe {

  public BasicTableRecipe() {
    super("Basic Table");
  }

  @Override
  protected Widget createWidget() {
    final ListViewAdapter<String> adapter = new ListViewAdapter<String>();
    final CellTable<String> table = new CellTable<String>();
    adapter.addView(table);

    // Add a selection model.
    final SelectionModel<String> selectionModel = new MultiSelectionModel<String>();
    table.setSelectionModel(selectionModel);

    // Add some data to the table
    for (int i = 0; i < 25; ++i) {
      adapter.getList().add("Item " + i);
    }

    // Checkbox column tied to selection.
    Column<String, Boolean> checkboxCol = new Column<String, Boolean>(
        new CheckboxCell()) {
      @Override
      public boolean dependsOnSelection() {
        return true;
      }

      @Override
      public Boolean getValue(String object) {
        return selectionModel.isSelected(object);
      }
    };
    table.addColumn(checkboxCol);
    checkboxCol.setFieldUpdater(new FieldUpdater<String, Boolean>() {
      public void update(int index, String object, Boolean value) {
        selectionModel.setSelected(object, value);
      }
    });

    // String column.
    table.addColumn(new IdentityColumn<String>(new TextCell()),
        "TextCell", "TextCell");

    // Button column tied to selection.
    Column<String, String> buttonCol = new Column<String, String>(
        new ButtonCell()) {

      @Override
      public boolean dependsOnSelection() {
        return true;
      }

      @Override
      public String getValue(String object) {
        if (selectionModel.isSelected(object)) {
          return "Unselect";
        } else {
          return "Select";
        }
      }
    };
    buttonCol.setFieldUpdater(new FieldUpdater<String, String>() {
      public void update(int index, String object, String value) {
        selectionModel.setSelected(object, !selectionModel.isSelected(object));
        Window.alert("You clicked: " + object);
      }
    });
    table.addColumn(buttonCol, "ButtonCell", "ButtonCell");

    // Add a Pager to control the table.
    SimplePager<String> pager = new SimplePager<String>(table);

    // Add buttons to increase the size of the table.
    Button addBtn = new Button("Add Data Row", new ClickHandler() {
      public void onClick(ClickEvent event) {
        List<String> list = adapter.getList();
        list.add("item " + list.size());
      }
    });
    Button removeBtn = new Button("Remove Data Row", new ClickHandler() {
      public void onClick(ClickEvent event) {
        int size = adapter.getList().size();
        if (size > 0) {
          adapter.getList().remove(size - 1);
        }
      }
    });

    FlowPanel fp = new FlowPanel();
    fp.add(table);
    fp.add(pager);
    fp.add(addBtn);
    fp.add(removeBtn);
    return fp;
  }
}
