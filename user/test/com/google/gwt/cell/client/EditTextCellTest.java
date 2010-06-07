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
package com.google.gwt.cell.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;

/**
 * Tests for {@link EditTextCell}.
 */
public class EditTextCellTest extends CellTestBase<String> {

  public void testRenderViewData() {
    Cell<String> cell = createCell();
    StringBuilder sb = new StringBuilder();
    cell.render("test", "editing", sb);
    assertEquals("<input type='text' value='editing'></input>", sb.toString());
  }

  public void testEdit() {
    EditTextCell cell = createCell();
    Element parent = Document.get().createDivElement();
    parent.setInnerHTML("<input type='text' value='editing'></input>");
    cell.edit(parent, "editing");

    // Verify the input element.
    Element child = parent.getFirstChildElement();
    assertTrue(InputElement.is(child));
    InputElement input = child.cast();
    assertEquals("editing", input.getValue());
  }

  /**
   * Cancel and switch to read only mode.
   */
  public void testOnBrowserEventCancel() {
    NativeEvent event = Document.get().createKeyDownEvent(false, false, false,
        false, KeyCodes.KEY_ESCAPE);
    Element parent = testOnBrowserEvent(
        "<input type='text' value='newValue'></input>", event, "originalValue",
        "originalValue", null);

    // Verify the input element is gone.
    assertEquals("originalValue", parent.getInnerHTML());
  }

  /**
   * Commit and switch to read only mode.
   */
  public void testOnBrowserEventCommit() {
    NativeEvent event = Document.get().createKeyDownEvent(false, false, false,
        false, KeyCodes.KEY_ENTER);
    Element parent = testOnBrowserEvent(
        "<input type='text' value='newValue'></input>", event, "originalValue",
        "originalValue", "newValue");

    // Verify the input element is gone.
    assertEquals("newValue", parent.getInnerHTML());
  }

  /**
   * Test switching into edit mode from onBrowserEvent.
   */
  public void testOnBrowserEventEdit() {
    NativeEvent event = Document.get().createClickEvent(0, 0, 0, 0, 0, false,
        false, false, false);
    Element parent = testOnBrowserEvent("helloworld", event, null, "editing",
        null);

    // Verify the input element.
    Element child = parent.getFirstChildElement();
    assertTrue(InputElement.is(child));
    InputElement input = child.cast();
    assertEquals("editing", input.getValue());
  }

  @Override
  protected boolean consumesEvents() {
    return true;
  }

  @Override
  protected EditTextCell createCell() {
    return new EditTextCell();
  }

  @Override
  protected String createCellValue() {
    return "helloworld";
  }

  @Override
  protected boolean dependsOnSelection() {
    return false;
  }

  @Override
  protected String getExpectedInnerHtml() {
    return "helloworld";
  }

  @Override
  protected String getExpectedInnerHtmlNull() {
    return "";
  }
}
