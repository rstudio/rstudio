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

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.view.client.DefaultSelectionEventManager.BlacklistEventTranslator;
import com.google.gwt.view.client.DefaultSelectionEventManager.SelectAction;
import com.google.gwt.view.client.DefaultSelectionEventManager.WhitelistEventTranslator;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link DefaultSelectionEventManager}.
 */
public class DefaultSelectionEventManagerTest extends GWTTestCase {

  /**
   * The mock display used in most tests.
   */
  MockHasData<String> display;

  /**
   * The {@link DefaultSelectionEventManager} used in most tests.
   */
  DefaultSelectionEventManager<String> manager;

  @Override
  public String getModuleName() {
    return "com.google.gwt.view.View";
  }

  public void testBlacklistEventTranslator() {
    BlacklistEventTranslator<String> translator = new BlacklistEventTranslator<String>(
        1, 3);
    assertTrue(translator.isColumnBlacklisted(1));
    assertFalse(translator.isColumnBlacklisted(2));
    assertTrue(translator.isColumnBlacklisted(3));

    translator.setColumnBlacklisted(2, true);
    assertTrue(translator.isColumnBlacklisted(1));
    assertTrue(translator.isColumnBlacklisted(2));
    assertTrue(translator.isColumnBlacklisted(3));

    translator.setColumnBlacklisted(2, false);
    assertTrue(translator.isColumnBlacklisted(1));
    assertFalse(translator.isColumnBlacklisted(2));
    assertTrue(translator.isColumnBlacklisted(3));

    translator.clearBlacklist();
    assertFalse(translator.isColumnBlacklisted(1));
    assertFalse(translator.isColumnBlacklisted(2));
    assertFalse(translator.isColumnBlacklisted(3));
  }

  public void testClearSelection() {
    MultiSelectionModel<String> model = new MultiSelectionModel<String>();
    model.setSelected("foo", true);
    model.setSelected("bar", true);
    model.setSelected("baz", true);
    assertEquals(3, model.getSelectedSet().size());

    manager.clearSelection(model);
    assertSelected(model);
  }

  public void testDoMultiSelection() {
    MultiSelectionModel<String> model = new MultiSelectionModel<String>();
    display.setSelectionModel(model);

    // Select one value.
    manager.doMultiSelection(model, display, 0, "test 0", null, false, false);
    assertSelected(model, "test 0");

    // Select another value without clearing.
    manager.doMultiSelection(model, display, 2, "test 2", null, false, false);
    assertSelected(model, "test 0", "test 2");

    // Select a selected value and clear.
    manager.doMultiSelection(model, display, 2, "test 2", null, false, true);
    assertSelected(model, "test 2");

    // Select a different value and clear.
    manager.doMultiSelection(model, display, 3, "test 3", null, false, true);
    assertSelected(model, "test 3");
  }

  /**
   * Test that the user can change the selected range based at an anchor point.
   */
  public void testDoMultiSelectionChangeRange() {
    MultiSelectionModel<String> model = new MultiSelectionModel<String>();
    display.setSelectionModel(model);

    // Select the anchor.
    manager.doMultiSelection(model, display, 5, "test 5", null, false, false);
    assertSelected(model, "test 5");

    // Select a range in the positive direction.
    manager.doMultiSelection(model, display, 9, "test 9", null, true, false);
    assertSelected(model, "test 5", "test 6", "test 7", "test 8", "test 9");

    // Reduce the length of the range.
    manager.doMultiSelection(model, display, 7, "test 7", null, true, false);
    assertSelected(model, "test 5", "test 6", "test 7");

    // Increase the length of the range.
    manager.doMultiSelection(model, display, 9, "test 9", null, true, false);
    assertSelected(model, "test 5", "test 6", "test 7", "test 8", "test 9");

    // Change the range to the negative direction.
    manager.doMultiSelection(model, display, 1, "test 1", null, true, false);
    assertSelected(model, "test 1", "test 2", "test 3", "test 4", "test 5");

    // Reduce the length of the range.
    manager.doMultiSelection(model, display, 3, "test 3", null, true, false);
    assertSelected(model, "test 3", "test 4", "test 5");

    // Increase the length of the range.
    manager.doMultiSelection(model, display, 1, "test 1", null, true, false);
    assertSelected(model, "test 1", "test 2", "test 3", "test 4", "test 5");
  }

  public void testDoMultiSelectionDeselect() {
    MultiSelectionModel<String> model = new MultiSelectionModel<String>();
    display.setSelectionModel(model);

    // Select one value.
    manager.doMultiSelection(model, display, 0, "test 0", SelectAction.SELECT,
        false, false);
    assertSelected(model, "test 0");

    // Deselect another value.
    manager.doMultiSelection(model, display, 3, "test 3",
        SelectAction.DESELECT, false, false);
    assertSelected(model, "test 0");

    // Deselect the value.
    manager.doMultiSelection(model, display, 0, "test 0",
        SelectAction.DESELECT, false, false);
    assertSelected(model);
  }

  public void testDoMultiSelectionIgnore() {
    MultiSelectionModel<String> model = new MultiSelectionModel<String>();
    display.setSelectionModel(model);

    // Select one value.
    manager.doMultiSelection(model, display, 0, "test 0", SelectAction.IGNORE,
        false, false);
    assertSelected(model);
  }

  public void testDoMultiSelectionRange() {
    MultiSelectionModel<String> model = new MultiSelectionModel<String>();
    display.setSelectionModel(model);

    // Select range, but really only one value because nothing is selected.
    manager.doMultiSelection(model, display, 3, "test 3", null, true, false);
    assertSelected(model, "test 3");

    // Select a range.
    manager.doMultiSelection(model, display, 5, "test 5", null, true, false);
    assertSelected(model, "test 3", "test 4", "test 5");

    // Select a different value and do not clear.
    manager.doMultiSelection(model, display, 7, "test 7", null, false, false);
    assertSelected(model, "test 3", "test 4", "test 5", "test 7");

    // Select a second range and do not clear.
    manager.doMultiSelection(model, display, 9, "test 9", null, true, false);
    assertSelected(model, "test 3", "test 4", "test 5", "test 7", "test 8",
        "test 9");

    // Select a different value and do not clear.
    manager.doMultiSelection(model, display, 0, "test 0", null, false, false);
    assertSelected(model, "test 0", "test 3", "test 4", "test 5", "test 7",
        "test 8", "test 9");

    // Select an overlapping range and clear.
    manager.doMultiSelection(model, display, 4, "test 4", null, true, true);
    assertSelected(model, "test 0", "test 1", "test 2", "test 3", "test 4");

    // Select a value and clear.
    manager.doMultiSelection(model, display, 3, "test 3", null, false, true);
    assertSelected(model, "test 3");
  }

  /**
   * Test that selecting a range works when the visible range doesn't start at index 0.
   */
  public void testDoMultiSelectionRangeWithPaging() {
    MultiSelectionModel<String> model = new MultiSelectionModel<String>();
    display.setVisibleRange(10, 10);
    display.setRowData(10, createData(10, 10));
    display.setSelectionModel(model);

    // Select range, but really only one value because nothing is selected.
    manager.doMultiSelection(model, display, 13, "test 13", null, true, false);
    assertSelected(model, "test 13");

    // Select a range.
    manager.doMultiSelection(model, display, 15, "test 15", null, true, false);
    assertSelected(model, "test 13", "test 14", "test 15");
  }

  public void testDoMultiSelectionSelect() {
    MultiSelectionModel<String> model = new MultiSelectionModel<String>();
    display.setSelectionModel(model);

    // Select one value.
    manager.doMultiSelection(model, display, 0, "test 0", SelectAction.SELECT,
        false, false);
    assertSelected(model, "test 0");

    // Add another value.
    manager.doMultiSelection(model, display, 3, "test 3", SelectAction.SELECT,
        false, false);
    assertSelected(model, "test 0", "test 3");
  }

  public void testDoMultiSelectionToggle() {
    MultiSelectionModel<String> model = new MultiSelectionModel<String>();
    display.setSelectionModel(model);

    // Toggle a deselected value.
    manager.doMultiSelection(model, display, 0, "test 0", SelectAction.TOGGLE,
        false, false);
    assertSelected(model, "test 0");

    // Toggle a selected value.
    manager.doMultiSelection(model, display, 0, "test 0", SelectAction.TOGGLE,
        false, false);
    assertSelected(model);
  }

  public void testHandleSelectionEvent() {
    SingleSelectionModel<String> model = new SingleSelectionModel<String>();
    display.setSelectionModel(model);
    model.setSelected("test 0", true);

    // Select a different value.
    NativeEvent nativeEvent = Document.get().createClickEvent(0, 0, 0, 0, 0,
        false, false, false, false);
    CellPreviewEvent<String> event = new CellPreviewEvent<String>(nativeEvent,
        display, new Context(1, 0, null), "test 1", false, false);
    manager.handleSelectionEvent(event, null, model);
    assertEquals("test 1", model.getSelectedObject());

    // Select the same value.
    manager.handleSelectionEvent(event, null, model);
    assertEquals("test 1", model.getSelectedObject());

    // Ctrl+Select the same value.
    nativeEvent = Document.get().createClickEvent(0, 0, 0, 0, 0, true, false,
        false, true);
    event = new CellPreviewEvent<String>(nativeEvent, display, new Context(1,
        0, null), "test 1", false, false);
    manager.handleSelectionEvent(event, null, model);
    assertNull(model.getSelectedObject());

    // Spacebar a different value.
    nativeEvent = Document.get().createKeyUpEvent(false, false, false, false,
        32);
    event = new CellPreviewEvent<String>(nativeEvent, display, new Context(2,
        0, null), "test 2", false, false);
    manager.handleSelectionEvent(event, null, model);
    assertEquals("test 2", model.getSelectedObject());

    // Spacebar the same value.
    manager.handleSelectionEvent(event, null, model);
    assertNull(model.getSelectedObject());
  }

  public void testHandleSelectionEventDeselect() {
    SingleSelectionModel<String> model = new SingleSelectionModel<String>();
    display.setSelectionModel(model);
    model.setSelected("test 0", true);

    // Deselect a different value.
    NativeEvent nativeEvent = Document.get().createClickEvent(0, 0, 0, 0, 0,
        false, false, false, false);
    CellPreviewEvent<String> event = new CellPreviewEvent<String>(nativeEvent,
        display, new Context(1, 0, null), "test 1", false, false);
    manager.handleSelectionEvent(event, SelectAction.DESELECT, model);
    assertEquals("test 0", model.getSelectedObject());

    // Deselect the same value.
    event = new CellPreviewEvent<String>(nativeEvent, display, new Context(0,
        0, null), "test 0", false, false);
    manager.handleSelectionEvent(event, SelectAction.DESELECT, model);
    assertNull(model.getSelectedObject());
  }

  public void testHandleSelectionEventIgnore() {
    SingleSelectionModel<String> model = new SingleSelectionModel<String>();
    display.setSelectionModel(model);
    model.setSelected("test 0", true);

    NativeEvent nativeEvent = Document.get().createClickEvent(0, 0, 0, 0, 0,
        false, false, false, false);
    CellPreviewEvent<String> event = new CellPreviewEvent<String>(nativeEvent,
        display, new Context(3, 0, null), "test 3", false, false);
    manager.handleSelectionEvent(event, SelectAction.IGNORE, model);
    assertEquals("test 0", model.getSelectedObject());
  }

  public void testHandleSelectionEventSelect() {
    SingleSelectionModel<String> model = new SingleSelectionModel<String>();
    display.setSelectionModel(model);
    model.setSelected("test 0", true);

    // Select the same value.
    NativeEvent nativeEvent = Document.get().createClickEvent(0, 0, 0, 0, 0,
        false, false, false, false);
    CellPreviewEvent<String> event = new CellPreviewEvent<String>(nativeEvent,
        display, new Context(0, 0, null), "test 0", false, false);
    manager.handleSelectionEvent(event, SelectAction.SELECT, model);
    assertEquals("test 0", model.getSelectedObject());

    // Select a different value.
    event = new CellPreviewEvent<String>(nativeEvent, display, new Context(1,
        0, null), "test 1", false, false);
    manager.handleSelectionEvent(event, SelectAction.SELECT, model);
    assertEquals("test 1", model.getSelectedObject());
  }

  public void testHandleSelectionEventToggle() {
    SingleSelectionModel<String> model = new SingleSelectionModel<String>();
    display.setSelectionModel(model);
    model.setSelected("test 0", true);

    // Toggle a different value.
    NativeEvent nativeEvent = Document.get().createClickEvent(0, 0, 0, 0, 0,
        false, false, false, false);
    CellPreviewEvent<String> event = new CellPreviewEvent<String>(nativeEvent,
        display, new Context(1, 0, null), "test 1", false, false);
    manager.handleSelectionEvent(event, SelectAction.TOGGLE, model);
    assertEquals("test 1", model.getSelectedObject());

    // Toggle the same value.
    manager.handleSelectionEvent(event, SelectAction.TOGGLE, model);
    assertNull(model.getSelectedObject());
  }

  public void testSelectOne() {
    MultiSelectionModel<String> model = new MultiSelectionModel<String>();
    model.setSelected("foo", true);
    model.setSelected("bar", true);
    model.setSelected("baz", true);
    assertSelected(model, "foo", "bar", "baz");

    manager.selectOne(model, "biz", true, true);
    assertSelected(model, "biz");
  }

  public void testSelectOneAlreadySelected() {
    MultiSelectionModel<String> model = new MultiSelectionModel<String>();
    model.setSelected("foo", true);
    model.setSelected("bar", true);
    model.setSelected("baz", true);
    assertSelected(model, "foo", "bar", "baz");

    manager.selectOne(model, "bar", true, true);
    assertSelected(model, "bar");
  }

  public void testSelectOneWithoutClearing() {
    MultiSelectionModel<String> model = new MultiSelectionModel<String>();
    model.setSelected("foo", true);
    model.setSelected("bar", true);
    model.setSelected("baz", true);
    assertSelected(model, "foo", "bar", "baz");

    manager.selectOne(model, "biz", true, false);
    assertSelected(model, "foo", "bar", "baz", "biz");
  }

  public void testSetRangeSelection() {
    MultiSelectionModel<String> model = new MultiSelectionModel<String>();
    display.setSelectionModel(model);
    model.setSelected("test 1", true);
    model.setSelected("test 2", true);
    model.setSelected("test 4", true);
    assertSelected(model, "test 1", "test 2", "test 4");

    // Select a new range 6-7.
    manager.setRangeSelection(model, display, new Range(6, 2), true, false);
    assertSelected(model, "test 1", "test 2", "test 4", "test 6", "test 7");

    // Select an overlap range 1-3.
    manager.setRangeSelection(model, display, new Range(1, 3), true, false);
    assertSelected(model, "test 1", "test 2", "test 3", "test 4", "test 6",
        "test 7");

    // Select an overlap range 2-5 and clear.
    manager.setRangeSelection(model, display, new Range(2, 4), true, true);
    assertSelected(model, "test 2", "test 3", "test 4", "test 5");
  }

  public void testSetRangeSelectionDeselect() {
    MultiSelectionModel<String> model = new MultiSelectionModel<String>();
    display.setSelectionModel(model);
    model.setSelected("test 1", true);
    model.setSelected("test 2", true);
    model.setSelected("test 4", true);
    assertSelected(model, "test 1", "test 2", "test 4");

    // Deselect an overlapping range 2-5.
    manager.setRangeSelection(model, display, new Range(2, 4), false, false);
    assertSelected(model, "test 1");
  }

  public void testWhitelistEventTranslator() {
    WhitelistEventTranslator<String> translator = new WhitelistEventTranslator<String>(
        1, 3);
    assertTrue(translator.isColumnWhitelisted(1));
    assertFalse(translator.isColumnWhitelisted(2));
    assertTrue(translator.isColumnWhitelisted(3));

    translator.setColumnWhitelisted(2, true);
    assertTrue(translator.isColumnWhitelisted(1));
    assertTrue(translator.isColumnWhitelisted(2));
    assertTrue(translator.isColumnWhitelisted(3));

    translator.setColumnWhitelisted(2, false);
    assertTrue(translator.isColumnWhitelisted(1));
    assertFalse(translator.isColumnWhitelisted(2));
    assertTrue(translator.isColumnWhitelisted(3));

    translator.clearWhitelist();
    assertFalse(translator.isColumnWhitelisted(1));
    assertFalse(translator.isColumnWhitelisted(2));
    assertFalse(translator.isColumnWhitelisted(3));
  }

  /**
   * Assert that the expected values are selected in the specified
   * {@link MultiSelectionModel}.
   * 
   * @param <T> the data type
   * @param model the {@link MultiSelectionModel} to check
   * @param expected the expected values
   */
  protected <T> void assertSelected(MultiSelectionModel<T> model, T... expected) {
    assertEquals(expected.length, model.getSelectedSet().size());
    for (T value : expected) {
      assertTrue(model.isSelected(value));
    }
  }

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

  @Override
  protected void gwtSetUp() {
    display = new MockHasData<String>();
    display.setRowData(0, createData(0, 10));
    manager = DefaultSelectionEventManager.<String> createDefaultManager();
  }

  @Override
  protected void gwtTearDown() {
    display = null;
  }
}
