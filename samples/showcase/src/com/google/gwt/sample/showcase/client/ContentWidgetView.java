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
package com.google.gwt.sample.showcase.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A view of a {@link ContentWidget}.
 */
public class ContentWidgetView extends ResizeComposite {

  interface ContentWidgetViewUiBinder extends UiBinder<Widget, ContentWidgetView> {
  }

  private static ContentWidgetViewUiBinder uiBinder = GWT.create(ContentWidgetViewUiBinder.class);

  @UiField
  Element descElem;

  @UiField(provided = true)
  SimplePanel examplePanel;

  @UiField
  Element nameElem;

  private final boolean hasMargins;

  public ContentWidgetView(boolean hasMargins, boolean scrollable) {
    this.hasMargins = hasMargins;
    examplePanel = scrollable ? new ScrollPanel() : new SimpleLayoutPanel();
    examplePanel.setSize("100%", "100%");
    initWidget(uiBinder.createAndBindUi(this));
  }

  public void setDescription(SafeHtml html) {
    descElem.setInnerHTML(html.asString());
  }

  public void setExample(Widget widget) {
    examplePanel.setWidget(widget);
    if (hasMargins) {
      widget.getElement().getStyle().setMarginLeft(10.0, Unit.PX);
      widget.getElement().getStyle().setMarginRight(10.0, Unit.PX);
    }
  }

  public void setName(String text) {
    nameElem.setInnerText(text);
  }
}
