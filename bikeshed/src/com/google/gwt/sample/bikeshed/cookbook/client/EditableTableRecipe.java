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

import com.google.gwt.bikeshed.cells.client.EditTextCell;
import com.google.gwt.bikeshed.cells.client.FieldUpdater;
import com.google.gwt.bikeshed.cells.client.TextCell;
import com.google.gwt.bikeshed.list.client.Column;
import com.google.gwt.bikeshed.list.client.Header;
import com.google.gwt.bikeshed.list.client.PagingTableListView;
import com.google.gwt.bikeshed.list.shared.ListViewAdapter;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

/**
 * Editable table recipe.
 */
public class EditableTableRecipe extends Recipe {

  private static class EditTextColumn extends Column<String, String, String> {
    public EditTextColumn() {
      super(new EditTextCell());
    }

    @Override
    public String getValue(String object) {
      return object;
    }
  }

  public EditableTableRecipe() {
    super("Editable Table");
  }

  @Override
  protected Widget createWidget() {
    final ListViewAdapter<String> adapter = new ListViewAdapter<String>();
    final PagingTableListView<String> table = new PagingTableListView<String>(adapter, 10);
    adapter.addView(table);

    for (int i = 0; i < 25; ++i) {
      adapter.getList().add("" + i);
    }

    EditTextColumn column = new EditTextColumn();
    Header<String> header = new Header<String>(TextCell.getInstance()) {
      @Override
      public String getValue() {
        return "<b>item</b>";
      }
    };
    table.addColumn(column, header);

    column.setFieldUpdater(new FieldUpdater<String, String, String>() {
      public void update(int index, String object, String value, String viewData) {
        adapter.getList().set(index, value);
      }
    });

    FlowPanel plusMinusPanel = new FlowPanel();
    Button addBtn = new Button("+", new ClickHandler() {
      public void onClick(ClickEvent event) {
        List<String> list = adapter.getList();
        list.add("" + list.size());
      }
    });
    Button removeBtn = new Button("-", new ClickHandler() {
      public void onClick(ClickEvent event) {
        int size = adapter.getList().size();
        if (size > 0) {
          adapter.getList().remove(size - 1);
        }
      }
    });
    plusMinusPanel.add(addBtn);
    plusMinusPanel.add(removeBtn);

    FlowPanel nextPrevPanel = new FlowPanel();
    Button prevBtn = new Button("<", new ClickHandler() {
      public void onClick(ClickEvent event) {
        table.previousPage();
      }
    });
    Button nextBtn = new Button(">", new ClickHandler() {
      public void onClick(ClickEvent event) {
        table.nextPage();
      }
    });
    nextPrevPanel.add(prevBtn);
    nextPrevPanel.add(nextBtn);

    FlowPanel fp = new FlowPanel();
    fp.add(table);
    fp.add(nextPrevPanel);
    fp.add(plusMinusPanel);
    return fp;
  }
}
