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
package com.google.gwt.user.cellview.client;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link CellList}.
 */
public class CellListTest extends AbstractHasDataTestBase {

  public void testGetRowElement() {
    CellList<String> list = createAbstractHasData(new TextCell());
    list.setRowData(0, createData(0, 10));

    // Ensure that calling getRowElement() flushes all pending changes.
    assertNotNull(list.getRowElement(9));
  }

  public void testCellEvent() {
    IndexCell<String> cell = new IndexCell<String>("click");
    CellList<String> list = createAbstractHasData(cell);
    RootPanel.get().add(list);
    list.setRowData(createData(0, 10));
    list.getPresenter().flush();

    // Trigger an event at index 5.
    NativeEvent event = Document.get().createClickEvent(0, 0, 0, 0, 0, false,
        false, false, false);
    list.getRowElement(5).dispatchEvent(event);
    cell.assertLastBrowserEventIndex(5);
    cell.assertLastEditingIndex(5);

    RootPanel.get().remove(list);
  }

  /**
   * Test that the correct values are sent to the Cell to be rendered.
   */
  public void testRenderWithKeyProvider() {
    // Create a cell that verifies the render args.
    final List<String> rendered = new ArrayList<String>();
    final Cell<String> cell = new TextCell() {
      @Override
      public void render(Context context, SafeHtml data, SafeHtmlBuilder sb) {
        int call = rendered.size();
        rendered.add(data.asString());
        assertTrue("render() called more than ten times", rendered.size() < 11);

        Object key = context.getKey();
        assertEquals("test " + call, data.asString());
        assertTrue(key instanceof Integer);
        assertEquals(call, key);
      }
    };

    // Create a model with only one level, and three values at that level.
    ProvidesKey<String> keyProvider = new ProvidesKey<String>() {
      @Override
      public Object getKey(String item) {
        return Integer.parseInt(item.substring(5));
      }
    };
    CellList<String> cellList = new CellList<String>(cell, keyProvider);
    cellList.setRowData(createData(0, 10));
    cellList.getPresenter().flush();
    assertEquals(10, rendered.size());
  }

  /**
   * Test that clicking on the first item selects the item.
   */
  public void testSelectFirstItem() {
    IndexCell<String> cell = new IndexCell<String>();
    AbstractHasData<String> display = createAbstractHasData(cell);
    populateData(display);

    // Bind to selection.
    SingleSelectionModel<String> selectionModel = new SingleSelectionModel<String>();
    display.setSelectionModel(selectionModel);
    display.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.BOUND_TO_SELECTION);
    display.getPresenter().flush();
    assertNull(selectionModel.getSelectedObject());
    assertEquals(0, display.getKeyboardSelectedRow());

    // Fire a click event on the first item.
    RootPanel.get().add(display);
    NativeEvent clickEvent =
        Document.get().createClickEvent(0, 0, 0, 0, 0, false, false, false, false);
    display.getChildElement(0).dispatchEvent(clickEvent);
    display.getPresenter().flush();

    // Verify that the first item is now selected.
    assertEquals("test 0", selectionModel.getSelectedObject());
    assertEquals(0, display.getKeyboardSelectedRow());

    // Cleanup.
    RootPanel.get().remove(display);
  }

  @SuppressWarnings("deprecation")
  public void testSetEmptyListWidget() {
    CellList<String> cellList = createAbstractHasData(new TextCell());

    // Set a widget.
    Label l = new Label("Empty");
    cellList.setEmptyListWidget(l);
    assertEquals(l, cellList.getEmptyListWidget());

    // Set a message.
    SafeHtml message = SafeHtmlUtils.fromString("empty");
    cellList.setEmptyListMessage(message);
    assertEquals(message, cellList.getEmptyListMessage());
    assertNotSame(l, cellList.getEmptyListWidget());

    // Null widget.
    cellList.setEmptyListWidget(null);
    assertNull(cellList.getEmptyListWidget());
  }

  public void testSetLoadingIndicator() {
    CellList<String> cellList = createAbstractHasData(new TextCell());

    // Set a widget.
    Label l = new Label("Loading");
    cellList.setLoadingIndicator(l);
    assertEquals(l, cellList.getLoadingIndicator());

    // Null widget.
    cellList.setLoadingIndicator(null);
    assertNull(cellList.getLoadingIndicator());
  }

  @Override
  protected CellList<String> createAbstractHasData(Cell<String> cell) {
    return new CellList<String>(cell);
  }
}
