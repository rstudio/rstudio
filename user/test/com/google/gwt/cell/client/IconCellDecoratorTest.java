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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.HasVerticalAlignment;

/**
 * Tests for {@link IconCellDecorator}.
 */
public class IconCellDecoratorTest extends CellTestBase<String> {

  /**
   * The images used for this test.
   */
  static interface Images extends ClientBundle {
    ImageResource prettyPiccy();
  }

  private Images images;

  /**
   * Verify that events are sent to the inner cell.
   */
  public void testOnBrowserEvent() {
    NativeEvent event = Document.get().createChangeEvent();
    testOnBrowserEvent(
        getExpectedInnerHtml(), event, "helloworld", "newValueFromInnerCell");
  }

  public void testRenderNoImage() {
    Images images = getImages();
    MockCell<String> innerCell = new MockCell<String>(
        true, "newValueFromInnerCell", "click");
    IconCellDecorator<String> cell = new IconCellDecorator<String>(
        images.prettyPiccy(), innerCell) {
      @Override
      protected boolean isIconUsed(String value) {
        return false;
      }
    };

    // Render the cell.
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    cell.render("helloworld", null, sb);

    // Compare the expected render string.
    String expected = "<div style=\"position:relative;padding-left:64px;\">";
    expected += cell.getImageHtml(
        images.prettyPiccy(), HasVerticalAlignment.ALIGN_MIDDLE, true).asString();
    expected += "<div>helloworld</div>";
    expected += "</div>";
    assertEquals(expected, sb.toSafeHtml().asString());
  }

  public void testSelectableDelegate() {
    MockCell<String> innerCell = new MockCell<String>(
        true, "newValueFromInnerCell", "click");
    IconCellDecorator<String> iconCell = new IconCellDecorator<String>(
        getImages().prettyPiccy(), innerCell);
    assertTrue(iconCell.dependsOnSelection());
    assertTrue(iconCell.handlesSelection());
  }

  public void testSetValue() {
    Cell<String> cell = createCell();
    Element parent = Document.get().createDivElement();
    parent.setInnerHTML(getExpectedInnerHtml());
    assertEquals("helloworld",
        Element.as(parent.getFirstChildElement().getChild(1)).getInnerHTML());
    cell.setValue(parent, "test", null);
    assertEquals("test",
        Element.as(parent.getFirstChildElement().getChild(1)).getInnerHTML());
  }

  @Override
  protected IconCellDecorator<String> createCell() {
    MockCell<String> innerCell = new MockCell<String>(
        false, "newValueFromInnerCell", "click");
    IconCellDecorator<String> iconCell = new IconCellDecorator<String>(
        getImages().prettyPiccy(), innerCell);
    return iconCell;
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
  protected String[] getConsumedEvents() {
    return new String[]{"click"};
  }

  @Override
  protected String getExpectedInnerHtml() {
    IconCellDecorator<String> cell = createCell();
    String html = "<div style=\"position:relative;padding-left:64px;\">";
    html += cell.getIconHtml("helloworld").asString();
    html += "<div>helloworld</div>";
    html += "</div>";
    return html;
  }

  @Override
  protected String getExpectedInnerHtmlNull() {
    IconCellDecorator<String> cell = createCell();
    String html = "<div style=\"position:relative;padding-left:64px;\">";
    html += cell.getIconHtml("helloworld").asString();
    html += "<div></div>";
    html += "</div>";
    return html;
  }

  private Images getImages() {
    if (images == null) {
      images = GWT.create(Images.class);
    }
    return images;
  }
}
