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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.list.shared.ListEvent;
import com.google.gwt.list.shared.ListHandler;
import com.google.gwt.list.shared.SizeChangeEvent;
import com.google.gwt.sample.tree.shared.TreeNode;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.ArrayList;
import java.util.List;

/**
 * A demo of the asynchronous Tree model.
 */
public class Tree implements EntryPoint {
  
  static class FakeTreeNode extends TreeNode<String> {
    static int gensym = 1;
    private static final int FANOUT = 5;

    FakeTreeNode(int value) {
      this.nodeData = "" + value;
    }

    @Override
    protected void onRangeChanged(int start, int length) {
      // TODO: use start, length
      for (ListHandler<TreeNode<String>> handler : handlers) {
        handler.onSizeChanged(new SizeChangeEvent(FANOUT, true));
        List<TreeNode<String>> values = new ArrayList<TreeNode<String>>(FANOUT);
        for (int i = 0; i < FANOUT; i++) {
          values.add(new FakeTreeNode(gensym++));
        }
        handler.onDataChanged(new ListEvent<TreeNode<String>>(0, FANOUT, values));
      }
    }
  }

  public void onModuleLoad() {
    TreeNode<String> root = new FakeTreeNode(0);
    TreeView<String> treeView = new TreeView<String>(root);
    RootPanel.get().add(treeView);
  }
}
