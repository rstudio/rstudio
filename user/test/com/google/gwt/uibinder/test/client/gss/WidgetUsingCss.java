/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.uibinder.test.client.gss;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

/**
 * Widget not using GSS syntax in the inline style of its UiBinder template.
 */
public class WidgetUsingCss extends Composite {
  interface WidgetUsingCssUiBinder extends UiBinder<Widget, WidgetUsingCss> {
  }

  interface CssStyle extends CssResource {
    String main();
  }

  static final String WHITE = "white";

  private static WidgetUsingCssUiBinder uiBinder = GWT.create(WidgetUsingCssUiBinder.class);

  @UiField
  CssStyle style;

  public WidgetUsingCss() {
    initWidget(uiBinder.createAndBindUi(this));
  }
}
