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
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiBinderUtil;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiRenderer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Tests SafeUri parsing with the lazy widget builder.
 */
public class LazyWidgetBuilderSafeUriIntegrationTest extends SafeUriIntegrationTest {
  interface Renderer extends UiRenderer {
    AnchorElement getJsAnchorFromSafeUri(Element ancestor);

    AnchorElement getJsAnchorFromString(Element ancestor);
    
    AnchorElement getJsAnchorFromStringControl(Element ancestor);

    AnchorElement getHttpAnchorFromString(Element ancestor);

    AnchorElement getInlineHttpAnchor(Element ancestor);

    AnchorElement getInlineJavascriptAnchor(Element ancestor);

    AnchorElement getHttpAnchorFromConstructedString(Element ancestor);

    void render(SafeHtmlBuilder b, FakeBundle values);
  }

  static class Renderable extends Composite {
    interface Binder extends UiBinder<Widget, Renderable> {
    }

    private static final Binder BINDER = GWT.create(Binder.class);

    @UiField
    AnchorElement jsAnchorFromSafeUri;
    @UiField
    AnchorElement jsAnchorFromString;
    @UiField
    AnchorElement jsAnchorFromStringControl;
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

    public Renderable() {
      initWidget(BINDER.createAndBindUi(this));
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.uibinder.test.LazyWidgetBuilderSuite";
  }

  public void testIsRenderable() {
    Renderable ui = new Renderable();

    try {
      RootPanel.get().add(ui);
      assertEquals(values.anUnsafeUri(), ui.jsAnchorFromSafeUri.getHref());
      assertEquals(ui.jsAnchorFromString.getHref(), ui.jsAnchorFromString.getHref());
      assertEquals("http://www.google.com/images/logo_sm.gif", ui.inlineHttpAnchor.getHref());
      assertEquals("javascript:void(0)", ui.inlineJavascriptAnchor.getHref());
      assertEquals(values.aSelector() + values.aGifPath(),
          ui.httpAnchorFromConstructedString.getHref());

      assertEquals(values.anUnsafeUri(), ui.jsAnchorFromSafeUriObj.uri.asString());
      assertEquals("http://www.google.com/images/logo_sm.gif",
          ui.inlineHttpAnchorObj.uri.asString());
      assertEquals("javascript:void(0)", ui.inlineJavascriptAnchorObj.uri.asString());
    } finally {
      ui.removeFromParent();
    }
  }

  public void testRenderer() {
    Renderer r = GWT.create(Renderer.class);
    SafeHtmlBuilder b = new SafeHtmlBuilder();
    r.render(b, values);

    Element e = UiBinderUtil.fromHtml(b.toSafeHtml().asString());
    Document.get().getBody().appendChild(e);
    try {
      assertEquals(values.anUnsafeUri(), r.getJsAnchorFromSafeUri(e).getHref());

      assertEquals(r.getJsAnchorFromStringControl(e).getHref(), r.getJsAnchorFromString(e).getHref());

      assertEquals("http://www.google.com/images/logo_sm.gif", r.getInlineHttpAnchor(e).getHref());
      assertEquals("javascript:void(0)", r.getInlineJavascriptAnchor(e).getHref());

      assertEquals(values.aSelector() + values.aGifPath(),
          r.getHttpAnchorFromConstructedString(e).getHref());
    } finally {
      e.removeFromParent();
    }
  }
}
