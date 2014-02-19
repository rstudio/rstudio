/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dom.client;

import com.google.gwt.core.shared.impl.StringCase;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests the {@link Document} class.
 */
public class DocumentTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dom.DOMTest";
  }

  // create*Element
  public void testElementCreators() {
    Document doc = Document.get();

    assertEquals("a", StringCase.toLower(doc.createAnchorElement().getTagName()));
    assertEquals("area", StringCase.toLower(doc.createAreaElement().getTagName()));
    assertEquals("base", StringCase.toLower(doc.createBaseElement().getTagName()));
    assertEquals("blockquote",
        StringCase.toLower(doc.createBlockQuoteElement().getTagName()));
    assertEquals("br", StringCase.toLower(doc.createBRElement().getTagName()));
    assertEquals("caption",
        StringCase.toLower(doc.createCaptionElement().getTagName()));
    assertEquals("col", StringCase.toLower(doc.createColElement().getTagName()));
    assertEquals("colgroup",
        StringCase.toLower(doc.createColGroupElement().getTagName()));
    assertEquals("del", StringCase.toLower(doc.createDelElement().getTagName()));
    assertEquals("div", StringCase.toLower(doc.createDivElement().getTagName()));
    assertEquals("dl", StringCase.toLower(doc.createDLElement().getTagName()));
    assertEquals("fieldset",
        StringCase.toLower(doc.createFieldSetElement().getTagName()));
    assertEquals("form", StringCase.toLower(doc.createFormElement().getTagName()));
    assertEquals("frame", StringCase.toLower(doc.createFrameElement().getTagName()));
    assertEquals("frameset",
        StringCase.toLower(doc.createFrameSetElement().getTagName()));
    assertEquals("head", StringCase.toLower(doc.createHeadElement().getTagName()));
    assertEquals("h1", StringCase.toLower(doc.createHElement(1).getTagName()));
    assertEquals("hr", StringCase.toLower(doc.createHRElement().getTagName()));
    assertEquals("iframe", StringCase.toLower(doc.createIFrameElement().getTagName()));
    assertEquals("img", StringCase.toLower(doc.createImageElement().getTagName()));
    assertEquals("ins", StringCase.toLower(doc.createInsElement().getTagName()));
    assertEquals("label", StringCase.toLower(doc.createLabelElement().getTagName()));
    assertEquals("legend", StringCase.toLower(doc.createLegendElement().getTagName()));
    assertEquals("li", StringCase.toLower(doc.createLIElement().getTagName()));
    assertEquals("link", StringCase.toLower(doc.createLinkElement().getTagName()));
    assertEquals("map", StringCase.toLower(doc.createMapElement().getTagName()));
    assertEquals("meta", StringCase.toLower(doc.createMetaElement().getTagName()));
    assertEquals("object", StringCase.toLower(doc.createObjectElement().getTagName()));
    assertEquals("ol", StringCase.toLower(doc.createOLElement().getTagName()));
    assertEquals("optgroup",
        StringCase.toLower(doc.createOptGroupElement().getTagName()));
    assertEquals("option", StringCase.toLower(doc.createOptionElement().getTagName()));
    assertEquals("param", StringCase.toLower(doc.createParamElement().getTagName()));
    assertEquals("p", StringCase.toLower(doc.createPElement().getTagName()));
    assertEquals("pre", StringCase.toLower(doc.createPreElement().getTagName()));
    assertEquals("q", StringCase.toLower(doc.createQElement().getTagName()));
    assertEquals("script", StringCase.toLower(doc.createScriptElement().getTagName()));
    assertEquals("select", StringCase.toLower(doc.createSelectElement().getTagName()));
    assertEquals("select",
        StringCase.toLower(doc.createSelectElement(false).getTagName()));
    assertEquals("span", StringCase.toLower(doc.createSpanElement().getTagName()));
    assertEquals("style", StringCase.toLower(doc.createStyleElement().getTagName()));
    assertEquals("table", StringCase.toLower(doc.createTableElement().getTagName()));
    assertEquals("tbody", StringCase.toLower(doc.createTBodyElement().getTagName()));
    assertEquals("td", StringCase.toLower(doc.createTDElement().getTagName()));
    assertEquals("textarea",
        StringCase.toLower(doc.createTextAreaElement().getTagName()));
    assertEquals("tfoot", StringCase.toLower(doc.createTFootElement().getTagName()));
    assertEquals("thead", StringCase.toLower(doc.createTHeadElement().getTagName()));
    assertEquals("th", StringCase.toLower(doc.createTHElement().getTagName()));
    assertEquals("title", StringCase.toLower(doc.createTitleElement().getTagName()));
    assertEquals("tr", StringCase.toLower(doc.createTRElement().getTagName()));
    assertEquals("ul", StringCase.toLower(doc.createULElement().getTagName()));

    assertEquals("button",
        StringCase.toLower(doc.createPushButtonElement().getTagName()));
    assertEquals("button",
        StringCase.toLower(doc.createResetButtonElement().getTagName()));
    assertEquals("button",
        StringCase.toLower(doc.createSubmitButtonElement().getTagName()));

    assertEquals("button",
        StringCase.toLower(doc.createPushButtonElement().getType()));
    assertEquals("reset",
        StringCase.toLower(doc.createResetButtonElement().getType()));
    assertEquals("submit",
        StringCase.toLower(doc.createSubmitButtonElement().getType()));

    assertEquals("input",
        StringCase.toLower(doc.createCheckInputElement().getTagName()));
    assertEquals("input",
        StringCase.toLower(doc.createFileInputElement().getTagName()));
    assertEquals("input",
        StringCase.toLower(doc.createHiddenInputElement().getTagName()));
    assertEquals("input",
        StringCase.toLower(doc.createImageInputElement().getTagName()));
    assertEquals("input",
        StringCase.toLower(doc.createPasswordInputElement().getTagName()));
    assertEquals("input",
        StringCase.toLower(doc.createRadioInputElement("foo").getTagName()));
    assertEquals("input",
        StringCase.toLower(doc.createTextInputElement().getTagName()));

    assertEquals("button",
        StringCase.toLower(doc.createButtonInputElement().getType()));
    assertEquals("checkbox",
        StringCase.toLower(doc.createCheckInputElement().getType()));
    assertEquals("file", StringCase.toLower(doc.createFileInputElement().getType()));
    assertEquals("hidden",
        StringCase.toLower(doc.createHiddenInputElement().getType()));
    assertEquals("image", StringCase.toLower(doc.createImageInputElement().getType()));
    assertEquals("password",
        StringCase.toLower(doc.createPasswordInputElement().getType()));
    assertEquals("radio",
        StringCase.toLower(doc.createRadioInputElement("foo").getType()));
    assertEquals("reset", StringCase.toLower(doc.createResetInputElement().getType()));
    assertEquals("submit",
        StringCase.toLower(doc.createSubmitInputElement().getType()));
    assertEquals("text", StringCase.toLower(doc.createTextInputElement().getType()));
  }

  /**
   * getElementById, getElementsByTagName.
   */
  public void testGetElements() {
    Document doc = Document.get();

    DivElement div = doc.createDivElement();
    doc.getBody().appendChild(div);

    div.setInnerHTML("<span><button id='foo'>foo</button><span><button>bar</button></span></span>");

    NodeList<Element> nodes = doc.getElementsByTagName("button");
    assertEquals(2, nodes.getLength());
    assertEquals("foo", nodes.getItem(0).getInnerText());
    assertEquals("bar", nodes.getItem(1).getInnerText());

    Element foo = doc.getElementById("foo");
    assertEquals("foo", foo.getId());
  }

  /**
   * domain, referrer, title, url.
   */
  public void testProperties() {
    Document doc = Document.get();

    assertTrue(doc.getURL().startsWith("http"));
    // TODO: referrer
    // TODO: domain

    doc.setTitle("myTitle");
    assertEquals("myTitle", doc.getTitle());
  }
}
