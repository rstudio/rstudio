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

import com.google.gwt.cells.client.TextCell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.list.shared.AsyncListModel;
import com.google.gwt.list.shared.ListModel;
import com.google.gwt.list.shared.AsyncListModel.DataSource;
import com.google.gwt.sample.tree.shared.SimpleTreeNodeModel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Tree;

import java.util.ArrayList;
import java.util.List;

/**
 * A demo of the asynchronous Tree model.
 */
public class TreeEntryPoint implements EntryPoint {

  static class StringTreeNodeModel extends SimpleTreeNodeModel<String> {
    private static final int FANOUT = 5;
    private String nodeData;

    StringTreeNodeModel(final String nodeData) {
      super();
      this.nodeData = nodeData;
    }

    @Override
    public StringTreeNodeModel createChildModel(String nodeData) {
      if (Math.random() < 0.2) {
        return null;
      } else {
        return new StringTreeNodeModel(nodeData);
      }
    }

    @Override
    protected ListModel<String> createListModel() {
      return new AsyncListModel<String>(new DataSource<String>() {
        public void requestData(AsyncListModel<String> listModel) {
          listModel.updateDataSize(FANOUT, true);
          List<String> values = new ArrayList<String>();
          for (int i = 0; i < FANOUT; i++) {
            values.add(nodeData + "." + i);
          }
          listModel.updateViewData(0, FANOUT, values);
        }
      });
    }
  }
    
  public void onModuleLoad() {
    Tree tree = new Tree();

    StringTreeNodeModel model = new StringTreeNodeModel("Node 0");
    SimpleTreeNodeView<String> rootView = new SimpleTreeNodeView<String>("",
        model, new TextCell());
    tree.addItem(rootView.getTreeItem());

    RootPanel.get().add(tree);
  }
}
