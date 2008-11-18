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
 * Opening a tree item flickers in IE7 because all of the children are shown for
 * an instant, and then the animation continues normally.
 */
public class Issue2338 extends AbstractIssue {

  @Override
  public Widget createIssue() {
    Tree tree = new Tree();
    tree.setAnimationEnabled(true);
    TreeItem root = tree.addItem("Root");
    for (int i = 0; i < 5; i++) {
      root.addItem("Item " + i);
    }
    return tree;
  }

  @Override
  public String getInstructions() {
    return "Open the root node and you should not see a flicker";
  }

  @Override
  public String getSummary() {
    return "Tree animation flickers when expanding a TreeItem";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

}
