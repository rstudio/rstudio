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
package com.google.gwt.sample.bikeshed.tree.client;

import com.google.gwt.bikeshed.list.shared.SelectionModel.AbstractSelectionModel;
import com.google.gwt.bikeshed.tree.client.SideBySideTreeView;
import com.google.gwt.bikeshed.tree.client.StandardTreeView;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.Set;
import java.util.TreeSet;

/**
 * A demo of the asynchronous Tree model.
 */
public class TreeSample implements EntryPoint {

  class MySelectionModel extends AbstractSelectionModel<Object> {

    private Label label;
    private Set<Object> selectedSet = new TreeSet<Object>();

    public MySelectionModel(Label label) {
      this.label = label;
    }

    public boolean isSelected(Object object) {
      return selectedSet.contains(object);
    }

    public void setSelected(Object object, boolean selected) {
      if (selected) {
        selectedSet.add(object);
      } else {
        selectedSet.remove(object);
      }
      label.setText("Selected " + selectedSet.toString());
      scheduleSelectionChangeEvent();
    }
  }

  public void onModuleLoad() {
    Label label1 = new Label();
    MySelectionModel selectionModel1 = new MySelectionModel(label1);

    StandardTreeView stree = new StandardTreeView(new MyTreeViewModel(
        selectionModel1), "...");
    stree.setSelectionModel(selectionModel1);
    stree.setAnimationEnabled(true);

    RootPanel.get().add(stree);
    RootPanel.get().add(new HTML("<hr>"));
    RootPanel.get().add(label1);
    RootPanel.get().add(new HTML("<hr>"));

    Label label2 = new Label();
    MySelectionModel selectionModel2 = new MySelectionModel(label2);
    SideBySideTreeView sstree = new SideBySideTreeView(new MyTreeViewModel(
        selectionModel2), "...", 100, 4);
    sstree.setSelectionModel(selectionModel2);
    sstree.setHeight("200px");

    RootPanel.get().add(sstree);
    RootPanel.get().add(new HTML("<hr>"));
    RootPanel.get().add(label2);
    RootPanel.get().add(new HTML("<hr>"));
  }
}
