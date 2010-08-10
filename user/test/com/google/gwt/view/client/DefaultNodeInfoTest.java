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
package com.google.gwt.view.client;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.view.client.AbstractListViewAdapterTest.MockListViewAdapter;
import com.google.gwt.view.client.TreeViewModel.DefaultNodeInfo;

import junit.framework.TestCase;

/**
 * Tests for {@link DefaultNodeInfo}.
 */
public class DefaultNodeInfoTest extends TestCase {

  public void testAccessors() {
    ListViewAdapter<String> adapter = new ListViewAdapter<String>();
    TextCell cell = new TextCell();
    SingleSelectionModel<String> selectionModel = new SingleSelectionModel<
        String>();
    ValueUpdater<String> valueUpdater = new ValueUpdater<String>() {
      public void update(String value) {
      }
    };
    DefaultNodeInfo<String> nodeInfo = new DefaultNodeInfo<String>(
        adapter, cell, selectionModel, valueUpdater);

    assertEquals(adapter, nodeInfo.getProvidesKey());
    assertEquals(cell, nodeInfo.getCell());
    assertEquals(selectionModel, nodeInfo.getSelectionModel());
    assertEquals(valueUpdater, nodeInfo.getValueUpdater());
  }

  public void testSetView() {
    MockListViewAdapter<String> adapter = new MockListViewAdapter<String>();
    DefaultNodeInfo<String> nodeInfo = new DefaultNodeInfo<String>(
        adapter, new TextCell());
    MockHasData<String> view = new MockHasData<String>();
    view.setVisibleRange(0, 10);
    view.clearLastRowValuesAndRange();

    // setView.
    nodeInfo.setView(view);
    adapter.assertLastRangeChanged(view);
    adapter.clearLastRangeChanged();

    view.setVisibleRange(0, 5);
    adapter.assertLastRangeChanged(view);
    adapter.clearLastRangeChanged();

    // unsetView.
    nodeInfo.unsetView();
    view.setVisibleRange(0, 5);
    adapter.assertLastRangeChanged(null);
  }
}
