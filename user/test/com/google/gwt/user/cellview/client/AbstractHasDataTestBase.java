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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.Window;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.Range;

import java.util.ArrayList;
import java.util.List;

/**
 * Base tests for {@link AbstractHasData}.
 */
public abstract class AbstractHasDataTestBase extends GWTTestCase {

  /**
   * A mock cell that tests the index specified in each method.
   * 
   * @param <C> the cell type
   */
  protected static class IndexCell<C> extends AbstractCell<C> {
    private int lastBrowserEventIndex = -1;
    private int lastEditingIndex = -1;
    private int lastRenderIndex = -1;
    private int lastResetFocusIndex = -1;

    public IndexCell(String... consumedEvents) {
      super(consumedEvents);
    }

    public void assertLastBrowserEventIndex(int expected) {
      assertEquals(expected, lastBrowserEventIndex);
    }

    public void assertLastEditingIndex(int expected) {
      assertEquals(expected, lastEditingIndex);
    }

    public void assertLastRenderIndex(int expected) {
      assertEquals(expected, lastRenderIndex);
    }

    public void assertLastResetFocusIndex(int expected) {
      assertEquals(expected, lastResetFocusIndex);
    }

    @Override
    public boolean isEditing(Context context, Element parent, C value) {
      this.lastEditingIndex = context.getIndex();
      return false;
    }

    @Override
    public void onBrowserEvent(Context context, Element parent, C value,
        NativeEvent event, ValueUpdater<C> valueUpdater) {
      this.lastBrowserEventIndex = context.getIndex();
    }

    @Override
    public void render(Context context, C value, SafeHtmlBuilder sb) {
      this.lastRenderIndex = context.getIndex();
      sb.appendEscaped("index " + this.lastRenderIndex);
    }

    @Override
    public boolean resetFocus(Context context, Element parent, C value) {
      this.lastResetFocusIndex = context.getIndex();
      return false;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.cellview.CellView";
  }

  public void testGetVisibleItem() {
    AbstractHasData<String> display = createAbstractHasData(new TextCell());
    ListDataProvider<String> provider = new ListDataProvider<String>(
        createData(0, 13));
    provider.addDataDisplay(display);
    display.setVisibleRange(10, 10);

    // No items when no data is present.
    assertEquals("test 10", display.getVisibleItem(0));
    assertEquals("test 11", display.getVisibleItem(1));
    assertEquals("test 12", display.getVisibleItem(2));

    // Out of range.
    try {
      assertEquals("test 10", display.getVisibleItem(-1));
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }

    // Within page range, but out of data range.
    try {
      assertEquals("test 10", display.getVisibleItem(4));
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }
  }

  public void testGetVisibleItems() {
    AbstractHasData<String> display = createAbstractHasData(new TextCell());
    ListDataProvider<String> provider = new ListDataProvider<String>();
    provider.addDataDisplay(display);
    display.setVisibleRange(10, 3);

    // No items when no data is present.
    assertEquals(0, display.getVisibleItems().size());

    // Set some data.
    provider.setList(createData(0, 13));
    List<String> items = display.getVisibleItems();
    assertEquals(3, items.size());
    assertEquals("test 10", items.get(0));
    assertEquals("test 11", items.get(1));
    assertEquals("test 12", items.get(2));
  }

  /**
   * Test that we don't get any errors when keyboard selection is disabled.
   */
  public void testKeyboardSelectionPolicyDisabled() {
    AbstractHasData<String> display = createAbstractHasData(new TextCell());
    display.setRowData(createData(0, 10));
    display.getPresenter().flush();
    display.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);

    assertNull(display.getKeyboardSelectedElement());
    display.resetFocusOnCell();
    display.setAccessKey('a');
    display.setTabIndex(1);
  }

  public void testResetFocus() {
    IndexCell<String> cell = new IndexCell<String>();
    AbstractHasData<String> display = createAbstractHasData(cell);
    display.setRowData(createData(0, 10));
    display.getPresenter().flush();

    cell.assertLastResetFocusIndex(-1);
    display.getPresenter().setKeyboardSelectedRow(5, false, false);
    display.resetFocusOnCell();
    cell.assertLastResetFocusIndex(5);
  }

  public void testSetRowData() {
    IndexCell<String> cell = new IndexCell<String>();
    AbstractHasData<String> display = createAbstractHasData(cell);

    // Set exact data.
    List<String> values = createData(0, 62);
    display.setRowData(values);
    assertEquals(62, display.getRowCount());
    assertTrue(display.isRowCountExact());
    assertEquals(values, display.getVisibleItems());
    assertEquals(new Range(0, 62), display.getVisibleRange());

    // Add some data.
    List<String> moreValues = createData(62, 10);
    display.setVisibleRange(0, 100);
    display.setRowData(62, moreValues);
    assertEquals(72, display.getRowCount());
    assertTrue(display.isRowCountExact());
    assertEquals("test 62", display.getVisibleItem(62));
    assertEquals("test 71", display.getVisibleItem(71));
    assertEquals(72, display.getVisibleItems().size());
    assertEquals(new Range(0, 100), display.getVisibleRange());

    // Push the exact data again.
    display.setRowData(values);
    assertEquals(62, display.getRowCount());
    assertTrue(display.isRowCountExact());
    assertEquals(values, display.getVisibleItems());
    assertEquals(new Range(0, 62), display.getVisibleRange());
    display.getPresenter().flush();

    // Render one row and verify the index.
    display.setRowData(5, createData(100, 1));
    display.getPresenter().flush();
    assertEquals("test 100", display.getVisibleItem(5));
    cell.assertLastRenderIndex(5);
  }

  public void testSetTabIndex() {
    // Skip this test on Safari 3 because it does not support focusable divs.
    String userAgent = Window.Navigator.getUserAgent();
    if (userAgent.contains("Safari")) {
      RegExp versionRegExp = RegExp.compile("Version/[0-3]", "ig");
      MatchResult result = versionRegExp.exec(userAgent);
      if (result != null && result.getGroupCount() > 0) {
        return;
      }
    }

    AbstractHasData<String> display = createAbstractHasData(new TextCell());
    ListDataProvider<String> provider = new ListDataProvider<String>(
        createData(0, 10));
    provider.addDataDisplay(display);
    display.getPresenter().flush();

    // Default tab index is 0.
    assertEquals(0, display.getTabIndex());
    assertEquals(0, display.getKeyboardSelectedElement().getTabIndex());

    // Set tab index to 2.
    display.setTabIndex(2);
    assertEquals(2, display.getTabIndex());
    assertEquals(2, display.getKeyboardSelectedElement().getTabIndex());

    // Push new data.
    provider.refresh();
    display.getPresenter().flush();
    assertEquals(2, display.getTabIndex());
    assertEquals(2, display.getKeyboardSelectedElement().getTabIndex());
  }

  /**
   * Create an {@link AbstractHasData} to test.
   * 
   * @param cell the cell to use
   * @return the widget to test
   */
  protected abstract AbstractHasData<String> createAbstractHasData(
      Cell<String> cell);

  /**
   * Create a list of data for testing.
   * 
   * @param start the start index
   * @param length the length
   * @return a list of data
   */
  protected List<String> createData(int start, int length) {
    List<String> toRet = new ArrayList<String>();
    for (int i = 0; i < length; i++) {
      toRet.add("test " + (i + start));
    }
    return toRet;
  }

  /**
   * Populate the entire range of a view.
   */
  protected void populateData(AbstractHasData<String> view) {
    Range range = view.getVisibleRange();
    int start = range.getStart();
    int length = range.getLength();
    view.setRowData(start, createData(start, length));
  }
}
