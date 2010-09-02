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
import com.google.gwt.dom.client.NativeEvent;

/**
 * Tests for {@link TextInputCell}.
 */
public class TextInputCellTest extends EditableCellTestBase<String, String> {

  public void testOnBrowserEventChange() {
    NativeEvent event = Document.get().createChangeEvent();
    testOnBrowserEvent(
        getExpectedInnerHtml(), event, "oldValue", null, "hello", "hello");
  }

  public void testOnBrowserEventKeyUp() {
    NativeEvent event = Document.get().createKeyUpEvent(
        false, false, false, false, 0);
    testOnBrowserEvent(
        getExpectedInnerHtml(), event, "oldValue", null, null, "hello");
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
  protected String createCellViewData() {
    return "newValue";
  }

  @Override
  protected boolean dependsOnSelection() {
    return false;
  }

  @Override
  protected String[] getConsumedEvents() {
    return new String[]{"change", "keyup"};
  }

  @Override
  protected String getExpectedInnerHtml() {
    return "<input type=\"text\" value=\"hello\"></input>";
  }

  @Override
  protected String getExpectedInnerHtmlNull() {
    return "<input type=\"text\"></input>";
  }

  @Override
  protected String getExpectedInnerHtmlViewData() {
    return "<input type=\"text\" value=\"newValue\"></input>";
  }
}
