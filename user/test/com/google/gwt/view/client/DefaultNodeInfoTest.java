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
import com.google.gwt.view.client.AbstractDataProviderTest.MockDataProvider;
import com.google.gwt.view.client.TreeViewModel.DefaultNodeInfo;

import junit.framework.TestCase;

/**
 * Tests for {@link DefaultNodeInfo}.
 */
public class DefaultNodeInfoTest extends TestCase {

  public void testAccessors() {
    ListDataProvider<String> provider = new ListDataProvider<String>();
    TextCell cell = new TextCell();
    SingleSelectionModel<String> selectionModel = new SingleSelectionModel<String>(
        null);
    ValueUpdater<String> valueUpdater = new ValueUpdater<String>() {
      public void update(String value) {
      }
    };
    DefaultNodeInfo<String> nodeInfo = new DefaultNodeInfo<String>(provider,
        cell, selectionModel, valueUpdater);

    assertEquals(provider, nodeInfo.getProvidesKey());
    assertEquals(cell, nodeInfo.getCell());
    assertEquals(selectionModel, nodeInfo.getSelectionModel());
    assertEquals(valueUpdater, nodeInfo.getValueUpdater());
  }

  public void testSetDataDisplay() {
    SelectionModel<String> model = new SingleSelectionModel<String>();
    DefaultSelectionEventManager<String> manager =
      DefaultSelectionEventManager.createDefaultManager();
    MockDataProvider<String> provider = new MockDataProvider<String>(null);
    DefaultNodeInfo<String> nodeInfo = new DefaultNodeInfo<String>(provider,
        new TextCell(), model, manager, null);
    MockHasData<String> display = new MockHasData<String>();
    display.setVisibleRange(0, 10);
    display.clearLastRowDataAndRange();
    assertEquals(0, display.getHandlerCount(CellPreviewEvent.getType()));

    // setDataDisplay.
    nodeInfo.setDataDisplay(display);
    assertEquals(1, display.getHandlerCount(CellPreviewEvent.getType()));
    provider.assertLastRangeChanged(display);
    provider.clearLastRangeChanged();

    display.setVisibleRange(0, 5);
    provider.assertLastRangeChanged(display);
    provider.clearLastRangeChanged();

    // unsetDataDisplay.
    nodeInfo.unsetDataDisplay();
    assertEquals(0, display.getHandlerCount(CellPreviewEvent.getType()));
    display.setVisibleRange(0, 5);
    provider.assertLastRangeChanged(null);
  }
}
