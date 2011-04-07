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
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * Tests for {@link TextInputCell}.
 */
public class TextInputCellTest extends
    EditableCellTestBase<String, TextInputCell.ViewData> {

  public void testOnBrowserEventChange() {
    NativeEvent event = Document.get().createChangeEvent();
    TextInputCell.ViewData expected = new TextInputCell.ViewData("oldValue");
    expected.setLastValue("hello");
    expected.setCurrentValue("hello");
    testOnBrowserEvent(getExpectedInnerHtml(), event, "oldValue", null,
        "hello", expected);
  }

  public void testOnBrowserEventKeyUp() {
    NativeEvent event = Document.get().createKeyUpEvent(false, false, false,
        false, 0);
    TextInputCell.ViewData expected = new TextInputCell.ViewData("oldValue");
    expected.setCurrentValue("hello");
    testOnBrowserEvent(getExpectedInnerHtml(), event, "oldValue", null, null,
        expected);
  }

  /**
   * Test rendering the cell with a malicious value.
   */
  public void testRenderUnsafeHtml() {
    Cell<String> cell = createCell();
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    Context context = new Context(0, 0, null);
    cell.render(context, "<script>malicious</script>", sb);
    assertEquals(
        "<input type=\"text\" value=\"&lt;script&gt;malicious&lt;/script&gt;\" tabindex=\"-1\">"
            + "</input>", sb.toSafeHtml().asString());
  }

  @Override
  protected TextInputCell createCell() {
    return new TextInputCell();
  }

  @Override
  protected String createCellValue() {
    return "hello";
  }

  @Override
  protected TextInputCell.ViewData createCellViewData() {
    return new TextInputCell.ViewData("newValue");
  }

  @Override
  protected boolean dependsOnSelection() {
    return false;
  }

  @Override
  protected String[] getConsumedEvents() {
    return new String[]{"change", "keyup", "keydown", "focus", "blur"};
  }

  @Override
  protected String getExpectedInnerHtml() {
    return "<input type=\"text\" value=\"hello\" tabindex=\"-1\"></input>";
  }

  @Override
  protected String getExpectedInnerHtmlNull() {
    return "<input type=\"text\" tabindex=\"-1\"></input>";
  }

  @Override
  protected String getExpectedInnerHtmlViewData() {
    return "<input type=\"text\" value=\"newValue\" tabindex=\"-1\"></input>";
  }
}
