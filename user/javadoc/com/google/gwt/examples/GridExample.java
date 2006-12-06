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
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.RootPanel;

public class GridExample implements EntryPoint {

  public void onModuleLoad() {
    // Grids must be sized explicitly, though they can be resized later.
    Grid g = new Grid(5, 5);

    // Put some values in the grid cells.
    for (int row = 0; row < 5; ++row) {
      for (int col = 0; col < 5; ++col)
        g.setText(row, col, "" + row + ", " + col);
    }

    // Just for good measure, let's put a button in the center.
    g.setWidget(2, 2, new Button("Does nothing, but could"));

    // You can use the CellFormatter to affect the layout of the grid's cells.
    g.getCellFormatter().setWidth(0, 2, "256px");

    RootPanel.get().add(g);
  }
}
