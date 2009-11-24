/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.sample.mail.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

/**
 * A simple widget representing prev/next page navigation.
 */
class NavBar extends Composite {
  @UiTemplate("NavBar.ui.xml")
  interface Binder extends UiBinder<Widget, NavBar> { }
  private static final Binder binder = GWT.create(Binder.class);

  @UiField Element countLabel;
  @UiField Anchor newerButton;
  @UiField Anchor olderButton;

  private final MailList outer;

  public NavBar(MailList outer) {
    initWidget(binder.createAndBindUi(this));
    this.outer = outer;
  }

  public void update(int startIndex, int count, int max) {
    setVisibility(newerButton, startIndex != 0);
    setVisibility(olderButton,
        startIndex + MailList.VISIBLE_EMAIL_COUNT < count);
    countLabel.setInnerText("" + (startIndex + 1) + " - " + max + " of "
        + count);
  }

  @UiHandler("newerButton")
  void onNewerClicked(ClickEvent event) {
    outer.newer();
  }

  @UiHandler("olderButton")
  void onOlderClicked(ClickEvent event) {
    outer.older();
  }

  private void setVisibility(Widget widget, boolean visible) {
    widget.getElement().getStyle().setVisibility(
        visible ? Visibility.VISIBLE : Visibility.HIDDEN);
  }
}
