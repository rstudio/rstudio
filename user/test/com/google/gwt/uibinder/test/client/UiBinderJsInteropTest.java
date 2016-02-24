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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Widget;

/**
 * Test for UiBinder JsInterop capabilities.
 */
public class UiBinderJsInteropTest extends GWTTestCase {

  static class MyWidgetBasedUi extends WidgetBasedUi {
    @UiTemplate("WidgetBasedUi.ui.xml")
    interface Binder extends UiBinder<Widget, MyWidgetBasedUi> { }

    @UiField TestJsElementType myJsElementType;
    @UiField TestJsElementTypeClass myJsElementTypeClass;

    @Override
    protected void init() {
      Binder binder = GWT.create(Binder.class);
      initWidget(binder.createAndBindUi(this));
    }
  }

  public void testJsElementTypes() {
    MyWidgetBasedUi widget = new MyWidgetBasedUi();
    assertEquals("JS-ELEMENT-TYPE", widget.myJsElementType.getTagName());
    assertEquals("JS-ELEMENT-TYPE-CLASS", widget.myJsElementTypeClass.getTagName());
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.uibinder.test.UiBinderSuite";
  }
}
