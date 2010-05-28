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
package com.google.gwt.sample.expenses.gwt.client;

import com.google.gwt.bikeshed.list.client.Header;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

/**
 * A {@link Header} subclass that maintains sorting state and displays an icon
 * to indicate the sort direction.
 */
public class SortableHeader extends Header<String> {

  /**
   * Image resources.
   */
  public static interface Resources extends ClientBundle {

    ImageResource downArrow();

    ImageResource upArrow();
  }

  private static final Resources RESOURCES = GWT.create(Resources.class);
  private static final int IMAGE_WIDTH = 16;
  private static final String DOWN_ARROW = makeImage(RESOURCES.downArrow());
  private static final String UP_ARROW = makeImage(RESOURCES.upArrow());

  private static String makeImage(ImageResource resource) {
    AbstractImagePrototype proto = AbstractImagePrototype.create(resource);
    return proto.getHTML().replace("style='",
        "style='position:absolute;right:0px;top:0px;");
  }

  private boolean reverseSort = false;
  private boolean sorted = false;
  private String text;

  SortableHeader(String text) {
    super(new ClickableTextCell());
    this.text = text;
  }

  public boolean getReverseSort() {
    return reverseSort;
  }

  @Override
  public String getValue() {
    return text;
  }

  public void render(StringBuilder sb) {
    int imageWidth = IMAGE_WIDTH;
    sb.append("<div style='position:relative;padding-right:");
    sb.append(imageWidth);
    sb.append("px;'>");
    if (sorted) {
      if (reverseSort) {
        sb.append(DOWN_ARROW);
      } else {
        sb.append(UP_ARROW);
      }
    } else {
      sb.append("<div style='position:absolute;display:none;'></div>");
    }
    sb.append("<div>");
    sb.append(text);
    sb.append("</div></div>");
  }

  public void setReverseSort(boolean reverseSort) {
    this.reverseSort = reverseSort;
  }

  public void setSorted(boolean sorted) {
    this.sorted = sorted;
  }

  public void toggleReverseSort() {
    this.reverseSort = !this.reverseSort;
  }
}
