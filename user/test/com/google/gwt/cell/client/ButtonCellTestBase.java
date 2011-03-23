/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * Bases tests for subclasses if {@link ButtonCellBase}.
 * 
 * @param <T> the cell type
 */
public abstract class ButtonCellTestBase<T> extends CellTestBase<T> {

  public void testOnBrowserEvent() {
    NativeEvent event = Document.get().createClickEvent(0, 0, 0, 0, 0, false, false, false, false);
    T value = createCellValue();
    testOnBrowserEvent(getExpectedInnerHtml(), event, value, value);
  }

  /**
   * Test that events outside of the button element are ignored.
   */
  public void testOnBrowserEventOutsideButton() {
    NativeEvent event = Document.get().createClickEvent(0, 0, 0, 0, 0, false, false, false, false);
    testOnBrowserEvent(createCell(), getExpectedInnerHtml(), event, createCellValue(), null, false);
  }

  public void testRenderAccessKey() {
    // No access key by default.
    ButtonCellBase<T> cell = createCell();
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    cell.render(null, null, sb);
    assertFalse(sb.toSafeHtml().asString().contains("accessKey"));

    // Set access key to safe value.
    cell.setAccessKey('a');
    assertEquals('a', cell.getAccessKey());
    sb = new SafeHtmlBuilder();
    cell.render(null, null, sb);
    assertTrue(sb.toSafeHtml().asString().contains("accessKey=\"a\""));

    // Set access key to unsafe value.
    cell.setAccessKey('<');
    assertEquals('<', cell.getAccessKey());
    sb = new SafeHtmlBuilder();
    cell.render(null, null, sb);
    assertFalse(sb.toSafeHtml().asString().contains("accessKey=\"<\""));
    assertTrue(sb.toSafeHtml().asString().contains("accessKey=\"&lt;\""));
  }

  public void testRenderDisabled() {
    // Default is enabled.
    ButtonCellBase<T> cell = createCell();
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    cell.render(null, null, sb);
    assertFalse(sb.toSafeHtml().asString().contains("disabled"));

    // Set tab index to 0.
    cell.setEnabled(false);
    assertFalse(cell.isEnabled());
    sb = new SafeHtmlBuilder();
    cell.render(null, null, sb);
    assertTrue(sb.toSafeHtml().asString().contains("disabled=disabled"));
  }

  public void testRenderIcon() {
    ButtonCellBase<T> cell = createCell();
    cell.setIcon(Resources.prettyPiccy());
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    cell.render(null, null, sb);
    assertTrue(sb.toSafeHtml().asString().contains("img"));
  }

  public void testRenderTabIndex() {
    // Default tab index is -1.
    ButtonCellBase<T> cell = createCell();
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    cell.render(null, null, sb);
    assertTrue(sb.toSafeHtml().asString().contains("tabindex=\"-1\""));

    // Set tab index to 0.
    cell.setTabIndex(0);
    assertEquals(0, cell.getTabIndex());
    sb = new SafeHtmlBuilder();
    cell.render(null, null, sb);
    assertTrue(sb.toSafeHtml().asString().contains("tabindex=\"0\""));
  }

  @Override
  protected abstract ButtonCellBase<T> createCell();

  @Override
  protected boolean dependsOnSelection() {
    return false;
  }

  @Override
  protected String[] getConsumedEvents() {
    return new String[]{"click", "keydown", "mousedown"};
  }
}
