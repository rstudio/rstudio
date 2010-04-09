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

import com.google.gwt.bikeshed.cells.client.ButtonCell;
import com.google.gwt.bikeshed.cells.client.Cell;
import com.google.gwt.bikeshed.cells.client.CheckboxCell;
import com.google.gwt.bikeshed.cells.client.FieldUpdater;
import com.google.gwt.bikeshed.cells.client.ValueUpdater;
import com.google.gwt.bikeshed.list.client.HasCell;
import com.google.gwt.bikeshed.list.shared.AbstractListModel;
import com.google.gwt.bikeshed.list.shared.ListModel;
import com.google.gwt.bikeshed.list.shared.SelectionModel;
import com.google.gwt.bikeshed.list.shared.ListRegistration;
import com.google.gwt.bikeshed.tree.client.TreeNode;
import com.google.gwt.bikeshed.tree.client.TreeViewModel;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * A demo TreeModel.
 */
public class MyTreeViewModel implements TreeViewModel {

  private static class IntegerListModel extends AbstractListModel<Integer> {
    int wordLength;

    public IntegerListModel(int wordLength) {
      this.wordLength = wordLength;
    }

    @Override
    protected void onRangeChanged(ListRegistration<Integer> reg, int start, int length) {
      List<Integer> values = new ArrayList<Integer>(1);
      values.add(wordLength);
      updateDataSize(1, true);
      updateViewData(0, 1, values);
    }
  }

  private static class StringListModel extends AbstractListModel<String> {
    String value;

    public StringListModel(final String value) {
      this.value = value;
    }

    @Override
    protected void onRangeChanged(ListRegistration<String> reg, int start, int length) {
      String prefix = value.endsWith("...") ? value.substring(0,
          value.length() - 3) : value;
      dataService.getNext(prefix, new AsyncCallback<List<String>>() {
        public void onFailure(Throwable caught) {
          String message = caught.getMessage();
          if (message.contains("Not logged in")) {
            // Force the user to login.
            Window.Location.reload();
          } else {
            Window.alert("ERROR: " + caught.getMessage());
          }
        }

        public void onSuccess(final List<String> result) {
          // Use a timer to simulate network delay.
          new Timer() {
            @Override
            public void run() {
              updateDataSize(result.size(), true);
              updateViewData(0, result.size(), result);
            }
          }.schedule(500);
        }
      });
    }
  }

  private static final TreeServiceAsync dataService = GWT.create(TreeService.class);

  /**
   * The cell used to render integers.
   */
  private static final Cell<Integer, Void> INTEGER_CELL = new Cell<Integer, Void>() {
    @Override
    public void render(Integer value, Void viewData, StringBuilder sb) {
      sb.append(value);
    }
  };

  private List<HasCell<String, ?, Void>> hasCells =
    new ArrayList<HasCell<String, ?, Void>>();

  public MyTreeViewModel(final SelectionModel<Object> selectionModel) {
    hasCells.add(new HasCell<String, Boolean, Void>() {
      public Cell<Boolean, Void> getCell() {
        return new CheckboxCell();
      }

      public FieldUpdater<String, Boolean, Void> getFieldUpdater() {
        return new FieldUpdater<String, Boolean, Void>() {
          public void update(int index, String object, Boolean value,
              Void viewData) {
            selectionModel.setSelected(object, value);
          }
        };
      }

      public Boolean getValue(String object) {
        return selectionModel.isSelected(object);
      }
    });
    hasCells.add(new HasCell<String, String, Void>() {
      public Cell<String, Void> getCell() {
        return ButtonCell.getInstance();
      }

      public FieldUpdater<String, String, Void> getFieldUpdater() {
        return new FieldUpdater<String, String, Void>() {
          public void update(int index, String object, String value,
              Void viewData) {
            Window.alert("Clicked " + object);
          }
        };
      }

      public String getValue(String object) {
        return object;
      }
    });
  }

  public <T> NodeInfo<?> getNodeInfo(T value, TreeNode<T> treeNode) {
    if (value instanceof String) {
      return getNodeInfoHelper((String) value);
    }

    // Unhandled type.
    String type = value.getClass().getName();
    throw new IllegalArgumentException("Unsupported object type: " + type);
  }

  public boolean isLeaf(Object value, TreeNode<?> parentNode) {
    return value instanceof Integer;
  }

  private NodeInfo<?> getNodeInfoHelper(final String value) {
    if (value.endsWith("...")) {
      ListModel<String> listModel = new StringListModel(value.toString());
      return new DefaultNodeInfo<String>(listModel, hasCells);
    } else {
      ListModel<Integer> listModel = new IntegerListModel(value.length());
      return new DefaultNodeInfo<Integer>(listModel, INTEGER_CELL,
          new ValueUpdater<Integer, Void>() {
            public void update(Integer value, Void viewData) {
              Window.alert("Integer = " + value);
            }
          });
    }
  }
}
