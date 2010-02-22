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

import com.google.gwt.cells.client.Cell;
import com.google.gwt.cells.client.TextCell;
import com.google.gwt.list.shared.AsyncListModel;
import com.google.gwt.list.shared.ListModel;

import java.util.ArrayList;
import java.util.List;

/**
 * A factory for generating {@link AbstractTreeNodeFactory} based on the value
 * type.
 */
public class MyTreeModel implements TreeModel {

  private static final int FANOUT = 5;

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

  /**
   * A list of strings.
   */
  private static class StringListModel extends AsyncListModel<String> {

    public StringListModel(final String value, final String delim) {
      super(new DataSource<String>() {
        public void requestData(AsyncListModel<String> listModel) {
          listModel.updateDataSize(FANOUT, true);
          List<String> values = new ArrayList<String>();
          for (int i = 0; i < FANOUT; i++) {
            values.add(value + delim + i);
          }
          listModel.updateViewData(0, FANOUT, values);
        }
      });
    }
  }

  /**
   * A list of integers.
   */
  private static class IntegerListModel extends AsyncListModel<Integer> {

    public IntegerListModel() {
      super(new DataSource<Integer>() {
        public void requestData(AsyncListModel<Integer> listModel) {
          listModel.updateDataSize(FANOUT, true);
          List<Integer> values = new ArrayList<Integer>();
          for (int i = 0; i < FANOUT; i++) {
            values.add(i);
          }
          listModel.updateViewData(0, FANOUT, values);
        }
      });
    }
  }

  public TreeNodeFactory<?> createTreeNodeFactory(Object value) {
    if (value instanceof String) {
      return createTreeNodeFactoryHelper((String) value);
    } else if (value instanceof Integer) {
      return createTreeNodeFactoryHelper((Integer) value);
    }

    // Unhandled type.
    String type = value.getClass().getName();
    throw new IllegalArgumentException("Unsupported object type: " + type);
  }

  private TreeNodeFactory<?> createTreeNodeFactoryHelper(final Integer value) {
    ListModel<String> listModel = new StringListModel(value.toString(), ".");
    return new DefaultTreeNodeFactory<String>(listModel, STRING_CELL, this);
  }

  private TreeNodeFactory<?> createTreeNodeFactoryHelper(final String value) {
    if (value.endsWith("2")) {
      ListModel<String> listModel = new StringListModel(value.toString(), "-");
      return new DefaultTreeNodeFactory<String>(listModel, STRING_CELL, this);
    } else {
      ListModel<Integer> listModel = new IntegerListModel();
      return new DefaultTreeNodeFactory<Integer>(listModel, INTEGER_CELL, this);
    }
  }
}
