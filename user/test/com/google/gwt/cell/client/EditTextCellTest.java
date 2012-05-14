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

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.EditTextCell.ViewData;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * Tests for {@link EditTextCell}.
 */
public class EditTextCellTest extends EditableCellTestBase<String, ViewData> {

  public void testEdit() {
    EditTextCell cell = createCell();
    Element parent = Document.get().createDivElement();
    parent.setInnerHTML("<input type='text' value='editing'></input>");
    ViewData viewData = new ViewData("originalValue");
    viewData.setText("newValue");
    cell.setViewData(DEFAULT_KEY, viewData);
    Context context = new Context(0, 0, DEFAULT_KEY);
    cell.edit(context, parent, "originalValue");

    // Verify the input element.
    Element child = parent.getFirstChildElement();
    assertTrue(InputElement.is(child));
    InputElement input = child.cast();
    assertEquals("newValue", input.getValue());
  }

  /**
   * Cancel and switch to read only mode.
   */
  public void testOnBrowserEventCancel() {
    NativeEvent event = Document.get().createKeyUpEvent(
        false, false, false, false, KeyCodes.KEY_ESCAPE);
    ViewData viewData = new ViewData("originalValue");
    viewData.setText("newValue");
    Element parent = testOnBrowserEvent(
        "<input type='text' value='newValue'></input>", event, "originalValue",
        viewData, null, null);

    // Verify the input element is gone.
    assertEquals("originalValue", parent.getInnerHTML());
  }

  /**
   * Cancel and switch to read only mode after committing once.
   */
  public void testOnBrowserEventCancelSecondEdit() {
    NativeEvent event = Document.get().createKeyUpEvent(
        false, false, false, false, KeyCodes.KEY_ESCAPE);
    ViewData viewData = new ViewData("originalValue");
    viewData.setText("newValue");
    viewData.setEditing(false); // commit.
    viewData.setEditing(true);
    assertEquals("newValue", viewData.getOriginal());
    assertEquals("newValue", viewData.getText());
    viewData.setText("newValue2");
    Element parent = testOnBrowserEvent(
        "<input type='text' value='newValue2'></input>", event, "originalValue",
        viewData, null, viewData);
    assertEquals("newValue", viewData.getOriginal());
    assertEquals("newValue", viewData.getText());
    assertFalse(viewData.isEditing());

    // Verify the input element is gone.
    assertEquals("newValue", parent.getInnerHTML());
  }

  /**
   * Commit and switch to read only mode.
   */
  public void testOnBrowserEventCommit() {
    NativeEvent event = Document.get().createKeyUpEvent(
        false, false, false, false, KeyCodes.KEY_ENTER);
    ViewData viewData = new ViewData("originalValue");
    viewData.setText("newValue");
    assertTrue(viewData.isEditing());
    Element parent = testOnBrowserEvent(
        "<input type='text' value='newValue'></input>", event, "originalValue",
        viewData, "newValue", viewData);
    assertFalse(viewData.isEditing());

    // Verify the input element is gone.
    assertEquals("newValue", parent.getInnerHTML());
  }

  /**
   * Test switching into edit mode from onBrowserEvent.
   */
  public void testOnBrowserEventEdit() {
    NativeEvent event = Document.get().createClickEvent(
        0, 0, 0, 0, 0, false, false, false, false);
    ViewData expectedViewData = new ViewData("editing");
    Element parent = testOnBrowserEvent(
        "helloWorld", event, "editing", null, null, expectedViewData);

    // Verify the input element.
    Element child = parent.getFirstChildElement();
    assertTrue(InputElement.is(child));
    InputElement input = child.cast();
    assertEquals("editing", input.getValue());
  }

  /**
   * Test rendering the cell with a valid value and view data, but without
   * editing.
   */
  public void testRenderViewDataDoneEditing() {
    EditTextCell cell = createCell();
    ViewData viewData = new ViewData("originalValue");
    viewData.setText("newValue");
    viewData.setEditing(false);
    cell.setViewData(DEFAULT_KEY, viewData);
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    Context context = new Context(0, 0, DEFAULT_KEY);
    cell.render(context, "originalValue", sb);
    assertEquals("newValue", sb.toSafeHtml().asString());
  }

  /**
   * Test rendering the cell with a malicious value.
   */
  public void testRenderUnsafeHtml() {
    EditTextCell cell = createCell();
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    Context context = new Context(0, 0, null);
    cell.render(context, "<script>malicious</script>", sb);
    assertEquals("&lt;script&gt;malicious&lt;/script&gt;", sb.toSafeHtml().asString());
  }

  /**
   * Test rendering the cell with a malicious value in edit mode.
   */
  public void testRenderUnsafeHtmlWhenEditing() {
    EditTextCell cell = createCell();
    ViewData viewData = new ViewData("originalValue");
    viewData.setText("<script>malicious</script>");
    viewData.setEditing(true);
    cell.setViewData(DEFAULT_KEY, viewData);
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    Context context = new Context(0, 0, DEFAULT_KEY);
    cell.render(context, "<script>malicious</script>", sb);
    assertEquals("<input type=\"text\" value=\"&lt;script&gt;malicious&lt;/script&gt;\" "
        + "tabindex=\"-1\"></input>", sb.toSafeHtml().asString());
  }

  public void testViewData() {
    // Start in edit mode.
    ViewData viewData = new ViewData("originalValue");
    assertEquals("originalValue", viewData.getOriginal());
    assertEquals("originalValue", viewData.getText());
    assertTrue(viewData.isEditing());
    assertFalse(viewData.isEditingAgain());

    // Change the text.
    viewData.setText("newValue");
    assertEquals("originalValue", viewData.getOriginal());
    assertEquals("newValue", viewData.getText());
    assertTrue(viewData.isEditing());
    assertFalse(viewData.isEditingAgain());

    // Stop editing.
    viewData.setEditing(false);
    assertEquals("originalValue", viewData.getOriginal());
    assertEquals("newValue", viewData.getText());
    assertFalse(viewData.isEditing());
    assertFalse(viewData.isEditingAgain());

    // Edit again.
    viewData.setEditing(true);
    assertEquals("newValue", viewData.getOriginal());
    assertEquals("newValue", viewData.getText());
    assertTrue(viewData.isEditing());
    assertTrue(viewData.isEditingAgain());
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
  protected ViewData createCellViewData() {
    return new ViewData("newValue");
  }

  @Override
  protected boolean dependsOnSelection() {
    return false;
  }

  @Override
  protected String[] getConsumedEvents() {
    return new String[]{"click", "keyup", "keydown", "blur"};
  }

  @Override
  protected String getExpectedInnerHtml() {
    return "helloworld";
  }

  @Override
  protected String getExpectedInnerHtmlNull() {
    return "\u00A0";
  }

  @Override
  protected String getExpectedInnerHtmlViewData() {
    return "<input type=\"text\" value=\"newValue\" tabindex=\"-1\"></input>";
  }
}
