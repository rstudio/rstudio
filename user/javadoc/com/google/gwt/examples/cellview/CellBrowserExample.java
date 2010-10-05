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
package com.google.gwt.examples.cellview;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.cellview.client.CellBrowser;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.TreeViewModel;

/**
 * Example of {@link CellBrowser}. This example shows a Browser consisting of
 * strings.
 */
public class CellBrowserExample implements EntryPoint {

  /**
   * The model that defines the nodes in the tree.
   */
  private static class CustomTreeModel implements TreeViewModel {

    /**
     * Get the {@link NodeInfo} that provides the children of the specified
     * value.
     */
    public <T> NodeInfo<?> getNodeInfo(T value) {
      /*
       * Create some data in a data provider. Use the parent value as a prefix
       * for the next level.
       */
      ListDataProvider<String> dataProvider = new ListDataProvider<String>();
      for (int i = 0; i < 2; i++) {
        dataProvider.getList().add(value + "." + String.valueOf(i));
      }

      // Return a node info that pairs the data with a cell.
      return new DefaultNodeInfo<String>(dataProvider, new TextCell());
    }

    /**
     * Check if the specified value represents a leaf node. Leaf nodes cannot be
     * opened.
     */
    public boolean isLeaf(Object value) {
      // The maximum length of a value is ten characters.
      return value.toString().length() > 10;
    }
  }

  public void onModuleLoad() {
    // Create a model for the browser.
    TreeViewModel model = new CustomTreeModel();

    /*
     * Create the browser using the model. We specify the default value of the
     * hidden root node as "Item 1".
     */
    CellBrowser tree = new CellBrowser(model, "Item 1");

    // Add the tree to the root layout panel.
    RootLayoutPanel.get().add(tree);
  }
}
