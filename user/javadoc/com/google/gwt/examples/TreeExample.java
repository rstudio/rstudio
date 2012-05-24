/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.examples;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class TreeExample implements EntryPoint {

  @Override
  public void onModuleLoad() {
    // Create a tree with a few items in it.
    TreeItem root = new TreeItem();
    root.setText("root");
    root.addTextItem("item0");
    root.addTextItem("item1");
    root.addTextItem("item2");

    // Add a CheckBox to the tree
    TreeItem item = new TreeItem(new CheckBox("item3"));
    root.addItem(item);

    Tree t = new Tree();
    t.addItem(root);

    // Add it to the root panel.
    RootPanel.get().add(t);
  }
}
