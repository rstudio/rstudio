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
package com.google.gwt.sample.expenses.client;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

/**
 * A {@link Header} subclass that maintains sorting state and displays an icon
 * to indicate the sort direction.
 */
public class SortableHeader extends Header<String> {

  interface Template extends SafeHtmlTemplates {
    @Template("<div style=\"position:relative;cursor:hand;cursor:pointer;"
        + "padding-right:{0}px;\">{1}<div>{2}</div></div>")
    SafeHtml sorted(int imageWidth, SafeHtml arrow, String text);

    @Template("<div style=\"position:relative;cursor:hand;cursor:pointer;"
        + "padding-right:{0}px;\"><div style=\"position:absolute;display:none;"
        + "\"></div><div>{1}</div></div>")
    SafeHtml unsorted(int imageWidth, String text);
  }

  private static Template template;

  /**
   * Image resources.
   */
  public static interface Resources extends ClientBundle {

    ImageResource downArrow();

    ImageResource upArrow();
  }

  private static final Resources RESOURCES = GWT.create(Resources.class);
  private static final int IMAGE_WIDTH = 16;
  private static final SafeHtml DOWN_ARROW = makeImage(RESOURCES.downArrow());
  private static final SafeHtml UP_ARROW = makeImage(RESOURCES.upArrow());

  private static SafeHtml makeImage(ImageResource resource) {
    AbstractImagePrototype proto = AbstractImagePrototype.create(resource);
    String html = proto.getHTML().replace("style='",
        "style='position:absolute;right:0px;top:0px;");
    return SafeHtmlUtils.fromTrustedString(html);
  }

  private boolean reverseSort = false;
  private boolean sorted = false;
  private String text;

  SortableHeader(String text) {
    super(new ClickableTextCell());
    if (template == null) {
      template = GWT.create(Template.class);
    }
    this.text = text;
  }

  public boolean getReverseSort() {
    return reverseSort;
  }

  @Override
  public String getValue() {
    return text;
  }

  @Override
  public void render(Context context, SafeHtmlBuilder sb) {
    if (sorted) {
      sb.append(template.sorted(IMAGE_WIDTH, reverseSort ? DOWN_ARROW : UP_ARROW, text));
    } else {
      sb.append(template.unsorted(IMAGE_WIDTH, text));
    }
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
