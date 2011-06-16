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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * Functional test of UiBinder.
 */
public class UiRendererTest extends GWTTestCase {
  private UiRendererUi safeHtmlUi;

  @Override
  public String getModuleName() {
    return "com.google.gwt.uibinder.test.LazyWidgetBuilderSuite";
  }

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    UiRendererTestApp app = UiRendererTestApp.getInstance();
    safeHtmlUi = app.getSafeHtmlUi();
  }

  public void testSafeHtmlRendererText() {
    SafeHtml render = safeHtmlUi.render();

    LabelElement renderedHtml = Document.get().createLabelElement();
    renderedHtml.setInnerHTML(render.asString());

    Node innerDiv = renderedHtml.getFirstChild();

    // Was the first span rendered as a "HTML-safe" text string?
    Node spanWithConstantTextNode = innerDiv.getChild(0);
    assertEquals("span", spanWithConstantTextNode.getNodeName().toLowerCase());
    assertEquals(Node.TEXT_NODE, spanWithConstantTextNode.getFirstChild().getNodeType());
    assertEquals("<b>This text won't be bold!</b>",
        spanWithConstantTextNode.getFirstChild().getNodeValue());

    Node firstRawTextNode = innerDiv.getChild(1);
    assertEquals(Node.TEXT_NODE, firstRawTextNode.getNodeType());
    assertEquals(" Hello, ", firstRawTextNode.getNodeValue());

    // Fields not present in owning class produce no content
    Node firstFieldNode = innerDiv.getChild(2);
    assertEquals(Node.ELEMENT_NODE, firstFieldNode.getNodeType());
    assertEquals("span", firstFieldNode.getNodeName().toLowerCase());
    assertFalse(firstFieldNode.hasChildNodes());

    // ui:msg tags get rendered but the "<ui:msg>" tag is not
    Node secondRawTextNode = innerDiv.getChild(3);
    assertEquals(Node.TEXT_NODE, secondRawTextNode.getNodeType());
    assertEquals(". How goes it? ", secondRawTextNode.getNodeValue());
  }
}
