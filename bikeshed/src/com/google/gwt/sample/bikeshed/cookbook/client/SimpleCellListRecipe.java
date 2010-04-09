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
package com.google.gwt.sample.bikeshed.cookbook.client;

import com.google.gwt.bikeshed.cells.client.TextCell;
import com.google.gwt.bikeshed.list.client.SimpleCellList;
import com.google.gwt.bikeshed.list.shared.ListViewAdapter;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

/**
 * SimpleCellList Recipe.
 */
public class SimpleCellListRecipe extends Recipe {

  public SimpleCellListRecipe() {
    super("Simple Cell List");
  }

  @Override
  protected Widget createWidget() {
    ListViewAdapter<String> adapter = new ListViewAdapter<String>();
    final List<String> list = adapter.getList();
    for (int i = 0; i < 40; i++) {
      list.add("" + ((i + 10) * 1000));
    }

    SimpleCellList<String> simpleCellList =
      new SimpleCellList<String>(TextCell.getInstance(), 10, 5);
    adapter.addView(simpleCellList);

    new Timer() {
      int index = 0;

      @Override
      public void run() {
          list.set(index, "" + (Integer.parseInt(list.get(index)) + 1));
          list.set(index + 15, "" + (Integer.parseInt(list.get(index + 15)) + 1));
          index = (index + 1) % 10;
          schedule(100);
      }
    }.schedule(100);

    return simpleCellList;
  }
}
