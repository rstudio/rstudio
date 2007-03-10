/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;

/**
 * TODO: document me.
 */
public class TreeProfile extends WidgetProfile {

  // Using 10*10,20*10,20*20,40*20, 40*40, 80*40,80*80 to get answers. 40*20 and
  // 80*40
  // breaks the square pattern because the closest squares are a bit too far off
  public void testTiming() throws Exception {

    int[] flatTree = {50};
    timing(flatTree);

    flatTree[0] = 100;
    timing(flatTree);
    flatTree[0] = 200;
    timing(flatTree);
    flatTree[0] = 400;
    timing(flatTree);
    flatTree[0] = 800;
    timing(flatTree);

    int[] bushy = {2, 2, 2, 2};
    timing(bushy);
    int[] bushy2 = {2, 2, 2, 2, 2};
    timing(bushy2);
    int[] bushy3 = {2, 2, 2, 2, 2, 2};
    timing(bushy3);
    int[] bushy4 = {2, 2, 2, 2, 2, 2, 2};
    timing(bushy4);
    int[] bushy5 = {2, 2, 2, 2, 2, 2, 2, 2};
    timing(bushy5);
    int[] bushy6 = {2, 2, 2, 2, 2, 2, 2, 2, 2};
    timing(bushy6);
    int[] bushy7 = {2, 2, 2, 2, 2, 2, 2, 2, 2, 2};
    timing(bushy7);
    throw new Exception("Finished Profile");
  }

  public void tastMemory() throws Exception {
    int[] flatTree = {100};
    for (int i = 0; i < 30; i++) {
      timing(flatTree);
      Window.alert("forcing event pump");
    }
    throw new Exception("Finished Profile");
  }

  static int run = 0;

  public void timing(final int[] branches) {
    Timer t = new Timer() {
      public void run() {
        ++run;
      }
    };
    createTree(branches);
  }

  Tree createTree(int[] branches) {
    String branchName = "[";
    for (int i = 0; i < branches.length; i++) {
      if (i != 0) {
        branchName += ", ";
      }
      branchName += branches[i];
    }
    branchName = "Created tree with branches " + branchName + "]";
    resetTimer();
    Tree t = new Tree();
    RootPanel.get().add(t);
    for (int i = 0; i < branches[0]; i++) {
      TreeItem item = new TreeItem(branches[0] + "-" + i);
      t.addItem(item);
      if (branches.length > 1) {
        createTreeItem(item, branches, 1);
      }
    }
    timing(branchName);
    return t;
  }

  void createTreeItem(TreeItem branch, int[] branches, int marker) {
    for (int i = 0; i < branches[marker]; i++) {
      TreeItem child = new TreeItem();
      child.setText(branches[marker] + "-" + i);
      branch.addItem(child);
      if (marker + 1 < branches.length) {
        createTreeItem(child, branches, marker + 1);
      }
    }
  }

  Tree createCheckBoxTree(int[] branches) {
    String branchName = "[";
    for (int i = 0; i < branches.length; i++) {
      if (i != 0) {
        branchName += ", ";
      }
      branchName += branches[i];
    }
    branchName = "Created checkbox tree with branches " + branchName + "]";
    resetTimer();
    Tree t = new Tree();
    RootPanel.get().add(t);
    for (int i = 0; i < branches[0]; i++) {
      TreeItem item = new TreeItem(branches[0] + "-" + i);
      t.addItem(item);
      if (branches.length > 1) {
        createCheckBoxTreeItem(item, branches, 1);
      }
    }
    timing(branchName);
    return t;
  }

  void createCheckBoxTreeItem(TreeItem branch, int[] branches, int marker) {
    for (int i = 0; i < branches[marker]; i++) {
      TreeItem child = new TreeItem(new CheckBox(branches[marker] + "-" + i));
      branch.addItem(child);
      if (marker + 1 < branches.length) {
        createLabelTreeItem(child, branches, marker + 1);
      }
    }
  }

  Tree createLabelTree(int[] branches) {
    String branchName = "[";
    for (int i = 0; i < branches.length; i++) {
      if (i != 0) {
        branchName += ", ";
      }
      branchName += branches[i];
    }
    branchName = "Created label tree with branches " + branchName + "]";
    resetTimer();
    Tree t = new Tree();
    RootPanel.get().add(t);
    for (int i = 0; i < branches[0]; i++) {
      TreeItem item = new TreeItem(new Label(branches[0] + "-" + i));
      t.addItem(item);
      if (branches.length > 1) {

        createLabelTreeItem(item, branches, 1);
      }
    }
    timing(branchName);
    return t;
  }

  void createLabelTreeItem(TreeItem branch, int[] branches, int marker) {
    for (int i = 0; i < branches[marker]; i++) {
      TreeItem child = new TreeItem(new Label(branches[marker] + "-" + i));
      branch.addItem(child);
      if (marker + 1 < branches.length) {
        createLabelTreeItem(child, branches, marker + 1);
      }
    }
  }
}
