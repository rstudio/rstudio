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
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A simple tree used to quickly exercise tree behavior.
 */
public class VisualsForTree extends AbstractIssue {
  public static Tree createTree() {
    Tree t = new Tree();
    TreeItem a = new TreeItem("a");
    TreeItem b = new TreeItem(
        "b, though this is a very, very long text field in order to trigger text wrapping bugs, if there are any such bugs currently in the tree.");
    TreeItem ba = new TreeItem("b.a");
    TreeItem bb = new TreeItem("b.b");
    TreeItem bba = new TreeItem("b.b.a");
    TreeItem bc = new TreeItem("b.c");
    TreeItem c = new TreeItem("c");

    t.setSelectedItem(b);
    t.addItem(a);
    t.addItem(b);
    t.addItem(c);
    b.addItem(ba);
    b.addItem(bb);
    bb.addItem(bba);
    b.addItem(bc);
    return t;
  }

  @Override
  public Widget createIssue() {
    VerticalPanel p = new VerticalPanel();
    p.add(createTree());
    return p;
  }

  @Override
  public String getInstructions() {
    return "Open each node, make sure everything looks right";
  }

  @Override
  public String getSummary() {
    return "simple tree, used for generic tree tests";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

}
