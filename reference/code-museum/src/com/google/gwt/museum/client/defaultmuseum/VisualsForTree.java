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
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.SimpleCheckBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A simple tree used to quickly exercise tree behavior.
 */
public class VisualsForTree extends AbstractIssue {

  static class DelegatingFocusPanel extends HorizontalPanel implements
      Focusable {

    public int getTabIndex() {
      return getFocusable().getTabIndex();
    }

    public void setAccessKey(char key) {
      getFocusable().setAccessKey(key);
    }

    public void setFocus(boolean focused) {
      getFocusable().setFocus(focused);
    }

    public void setTabIndex(int index) {
      getFocusable().setTabIndex(index);
    }

    private Focusable getFocusable() {
      for (Widget widget : this.getChildren()) {
        if (widget instanceof Focusable) {
          return (Focusable) widget;
        }
      }
      throw new IllegalArgumentException("No focusable children to focus on");
    }
  }

  public static Tree createTree() {
    Tree tree = new Tree();
    TreeItem a = new TreeItem("a");
    TreeItem b = new TreeItem(
        "b, though this is a very, very long text field in order to trigger text wrapping bugs, if there are any such bugs currently in the tree.");
    TreeItem ba = new TreeItem("b.a");
    TreeItem bb = new TreeItem("b.b");
    TreeItem bba = new TreeItem("b.b.a");
    TreeItem bc = new TreeItem("b.c");
    TreeItem c = new TreeItem("c");
    TreeItem d = new TreeItem(new RadioButton("myradio",
        "I should line up nicely"));
    TreeItem e = new TreeItem(new CheckBox("I should line up nicely"));
    TreeItem f = new TreeItem(new CheckBox("I should also line up nicely"));
    f.addItem(new CheckBox("me to"));
    SimplePanel panel = new SimplePanel();
    panel.setWidget(new Label("There should not be any space above me"));
    TreeItem g = new TreeItem(panel);

    tree.setSelectedItem(b);
    tree.addItem(a);
    tree.addItem(b);
    tree.addItem(c);
    tree.addItem(d);
    tree.addItem(e);
    tree.addItem(f);
    tree.addItem(g);
    b.addItem(ba);
    b.addItem(bb);
    bb.addItem(bba);
    b.addItem(bc);

    // Focus checks
    DelegatingFocusPanel focus = new DelegatingFocusPanel();
    focus.add(new Label("first check box should have focus "));
    focus.add(new SimpleCheckBox());
    focus.add(new SimpleCheckBox());

    final DelegatingFocusPanel focus2 = new DelegatingFocusPanel();
    focus2.add(new Label("second check box should have focus "));
    focus2.add(new SimpleCheckBox());
    focus2.add(new SimpleCheckBox());

    TreeItem customFocus = new TreeItem(focus2) {
      @Override
      public Focusable getFocusable() {
        return (Focusable) focus2.getWidget(2);
      }
    };
    tree.addItem(focus);
    tree.addItem(customFocus);
    return tree;
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
