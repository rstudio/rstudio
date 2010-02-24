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
package com.google.gwt.sample.tree.client;

import com.google.gwt.cells.client.ButtonCell;
import com.google.gwt.cells.client.Cell;
import com.google.gwt.cells.client.TextCell;
import com.google.gwt.cells.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.list.shared.AsyncListModel;
import com.google.gwt.list.shared.ListModel;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * A demo TreeModel.
 */
public class MyTreeViewModel implements TreeViewModel {

  private static class IntegerListModel extends AsyncListModel<Integer> {
    public IntegerListModel(final int length) {
      super(new DataSource<Integer>() {
        public void requestData(AsyncListModel<Integer> listModel) {
          listModel.updateDataSize(1, true);
          List<Integer> values = new ArrayList<Integer>(1);
          values.add(length);
          listModel.updateViewData(0, 1, values);
        }
      });
    }
  }

  private static class StringListModel extends AsyncListModel<String> {
    public StringListModel(final String value) {
      super(new DataSource<String>() {
        public void requestData(final AsyncListModel<String> listModel) {
          String prefix = value.endsWith("...") ? value.substring(0,
              value.length() - 3) : value;
          dataService.getNext(prefix, new AsyncCallback<List<String>>() {
            public void onFailure(Throwable caught) {
              Window.alert("Error: " + caught);
            }

            public void onSuccess(List<String> result) {
              listModel.updateDataSize(result.size(), true);
              listModel.updateViewData(0, result.size(), result);
            }
          });
        }
      });
    }
  }

  private static final TreeServiceAsync dataService = GWT.create(TreeService.class);

  /**
   * The cell used to render integers.
   */
  private static final Cell<Integer> INTEGER_CELL = new Cell<Integer>() {
    @Override
    public void render(Integer value, StringBuilder sb) {
      sb.append(value);
    }
  };

  /**
   * The cell used to render strings.
   */
  private static final Cell<String> STRING_CELL = new TextCell();

  public <T> NodeInfo<?> getNodeInfo(T value, TreeNodeView<T> treeNodeView) {
    if (value instanceof String) {
      return getNodeInfoHelper((String) value);
    } else if (value instanceof Integer) {
      return getNodeInfoHelper((Integer) value);
    }

    // Unhandled type.
    String type = value.getClass().getName();
    throw new IllegalArgumentException("Unsupported object type: " + type);
  }

  @SuppressWarnings("unused")
  private NodeInfo<?> getNodeInfoHelper(final Integer value) {
    return null;
  }

  private NodeInfo<?> getNodeInfoHelper(final String value) {
    if (value.endsWith("...")) {
      ListModel<String> listModel = new StringListModel(value.toString());
      return new DefaultNodeInfo<String>(listModel, new ButtonCell(),
          new ValueUpdater<String>() {
            public void update(String value) {
              Window.alert("Clicked: " + value);
            }
          });
    } else {
      ListModel<Integer> listModel = new IntegerListModel(value.length());
      return new DefaultNodeInfo<Integer>(listModel, INTEGER_CELL,
          new ValueUpdater<Integer>() {
            public void update(Integer value) {
              Window.alert("Integer = " + value);
            }
          });
    }
  }
}
