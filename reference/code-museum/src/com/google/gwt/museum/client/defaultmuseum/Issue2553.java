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
import com.google.gwt.user.client.ui.Widget;

/**
 * Test for http://code.google.com/p/google-web-toolkit/issues/detail?id=2553
 */
public class Issue2553 extends AbstractIssue {

  @Override
  public Widget createIssue() {
    Tree tree = new Tree();
    tree.addItem("This is a long text displayed in a tree item");
    tree.addItem("This is a long text displayed in a tree item and it's longer than the others");
    tree.addItem("This is a long text displayed in a tree item");
    return tree;
  }

  @Override
  public String getInstructions() {
    return "resize the browser to a smaller width than the tree's width";
  }

  @Override
  public String getSummary() {
    return "Word wrap of treeitem's text when browser width is too small";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

}
