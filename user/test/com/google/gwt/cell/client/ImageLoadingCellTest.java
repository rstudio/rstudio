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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * Tests for {@link TextCell}.
 */
public class ImageLoadingCellTest extends CellTestBase<String> {

  @Override
  public void testRender() {
    Cell<String> cell = createCell();
    String value = createCellValue();
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    Context context = new Context(0, 0, null);
    cell.render(context, value, sb);

    // Render the html.
    Element elem = Document.get().createDivElement();
    elem.setInnerHTML(sb.toSafeHtml().asString());

    // Verify the image.
    assertEquals(2, elem.getChildCount());
    Element imgWrapper = elem.getChild(1).cast();
    ImageElement img = imgWrapper.getFirstChildElement().cast();
    assertEquals("img", img.getTagName().toLowerCase());
    assertTrue(img.getSrc().toLowerCase().endsWith("test.png"));
  }

  @Override
  public void testRenderNegativeIndex() {
    Cell<String> cell = createCell();
    String value = createCellValue();
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    Context context = new Context(-1, -1, null);
    cell.render(context, value, sb);
 
    // Render the html.
    Element elem = Document.get().createDivElement();
    elem.setInnerHTML(sb.toSafeHtml().asString());

    // Verify the image.
    assertEquals(2, elem.getChildCount());
    Element imgWrapper = elem.getChild(1).cast();
    ImageElement img = imgWrapper.getFirstChildElement().cast();
    assertEquals("img", img.getTagName().toLowerCase());
    assertTrue(img.getSrc().toLowerCase().endsWith("test.png"));
  }

  @Override
  protected Cell<String> createCell() {
    return new ImageLoadingCell();
  }

  @Override
  protected String createCellValue() {
    return "test.png";
  }

  @Override
  protected boolean dependsOnSelection() {
    return false;
  }

  @Override
  protected String[] getConsumedEvents() {
    return new String[]{"load", "error"};
  }

  @Override
  protected String getExpectedInnerHtml() {
    return "<div></div><div><img src='test.png'/></div>";
  }

  @Override
  protected String getExpectedInnerHtmlNull() {
    return "";
  }
}
