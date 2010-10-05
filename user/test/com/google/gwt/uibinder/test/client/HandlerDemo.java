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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.Widget;

/**
 * A simple demo showing how UiHandler works.
 */
public class HandlerDemo extends Composite {

  @UiTemplate("HandlerDemo.ui.xml")
  interface MyUiBinder extends UiBinder<Widget, HandlerDemo> {
  }
  private static final MyUiBinder binder = GWT.create(MyUiBinder.class);

  @UiField FormPanel panelForm;
  @UiField TextBox textBoxValueChange;
  @UiField(provided = true) final CustomEventWidget<String> customEventWidget;

  public HandlerDemo() {
    this.customEventWidget = new CustomEventWidget<String>("a simple test");
    initWidget(binder.createAndBindUi(this));
  }

  @UiHandler("panelForm")
  public void doSubmit(SubmitEvent event) {
    eventMessage(event);
    event.cancel();
  }

  @UiHandler("textBoxValueChange")
  protected void doChangeValue(ValueChangeEvent<String> event) {
    eventMessage(event);
  }

  @UiHandler({"buttonClick", "labelClick"})
  void doClick(ClickEvent event) {
    eventMessage(event);
  }

  @UiHandler("buttonSubmit")
  void doClickSubmit(@SuppressWarnings("unused") ClickEvent ignored) {
    panelForm.submit();
  }

  @UiHandler("buttonMouseOut")
  void doMouseOut(MouseOutEvent event) {
    eventMessage(event);
  }

  @UiHandler("buttonMouseOver")
  void doMouseOver(MouseOverEvent event) {
    eventMessage(event);
  }

  @UiHandler("tree")
  void onTreeClose(CloseEvent<TreeItem> event) {
    eventMessage(event);
  }

  @UiHandler("tree")
  void onTreeOpen(OpenEvent<TreeItem> event) {
    eventMessage(event);
  }

  @UiHandler("customEventWidget")
  void onCustomEvent(CustomEvent<String> event) {
    eventMessage("CustomEventWidget clicked. Event throwing '"
        + event.getValue() + "'");
  }

  private void eventMessage(GwtEvent<?> event) {
    eventMessage(event.toDebugString());
  }

  private void eventMessage(String message) {
    Window.alert(message);
  }
}
