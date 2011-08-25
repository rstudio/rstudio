/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiBinderUtil;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;

/**
 * Test SafeUri parsing.
 */
public class SafeUriIntegrationTest extends GWTTestCase {
  static class BinderUi {
    interface Binder extends UiBinder<Widget, BinderUi> {
    }

    static final Binder binder = GWT.create(Binder.class);

    @UiField
    AnchorElement jsAnchorFromSafeUri;
    @UiField
    AnchorElement jsAnchorFromString;
    @UiField
    AnchorElement httpAnchorFromString;
    @UiField
    AnchorElement inlineHttpAnchor;
    @UiField
    AnchorElement inlineJavascriptAnchor;
    @UiField
    AnchorElement httpAnchorFromConstructedString;

    @UiField
    HasUri jsAnchorFromSafeUriObj;
    @UiField
    HasUri inlineHttpAnchorObj;
    @UiField
    HasUri inlineJavascriptAnchorObj;

    BinderUi() {
      binder.createAndBindUi(this);
    }
  }

  protected FakeBundle values = new FakeBundle();
  
  @Override
  public String getModuleName() {
    return "com.google.gwt.uibinder.test.UiBinderSuite";
  }
  
  public void testBinder() {
    BinderUi ui = new BinderUi();

    assertEquals(values.anUnsafeUri(), ui.jsAnchorFromSafeUri.getHref());
    AnchorElement anchor = UiBinderUtil.fromHtml("<a href='#'>snot</a>").cast();
    assertEquals(anchor.getHref(), ui.jsAnchorFromString.getHref());
    assertEquals("http://www.google.com/images/logo_sm.gif", ui.inlineHttpAnchor.getHref());
    assertEquals("javascript:void(0)", ui.inlineJavascriptAnchor.getHref());
    assertEquals(values.aSelector() + values.aGifPath(), ui.httpAnchorFromConstructedString.getHref());

    assertEquals(values.anUnsafeUri(), ui.jsAnchorFromSafeUriObj.uri.asString());
    assertEquals("http://www.google.com/images/logo_sm.gif", ui.inlineHttpAnchorObj.uri.asString());
    assertEquals("javascript:void(0)", ui.inlineJavascriptAnchorObj.uri.asString());
  }
}
