/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;

/**
 * <h1>gwt-TreeItem refers to the wrong element in TreeItem</h1>
 * 
 * <p>
 * gwt-TreeItem used to refer to the span that directly wrapped the text in a
 * TreeItem. Now it refers to the table element that holds the expand/collapse
 * image and the text. gwt-TreeItem-selected is still added to the span, so
 * there is an inconsistency here.
 * </p>
 */
public class Issue2290 extends AbstractIssue {

  @Override
  public Widget createIssue() {
    Tree tree = new Tree();
    TreeItem root = tree.addItem("Root Item");
    root.addItem("Item1");
    root.addItem("Item2");
    root.addItem("Item3");
    root.addItem("Item4");

    root.setState(true);
    tree.setSelectedItem(root);

    return tree;
  }

  @Override
  public String getInstructions() {
    return "The background of the Root Item, when selected, should be "
        + "completely red, with no visible green.";
  }

  @Override
  public String getSummary() {
    return "Tree background test";
  }

  @Override
  public boolean hasCSS() {
    return true;
  }
}
