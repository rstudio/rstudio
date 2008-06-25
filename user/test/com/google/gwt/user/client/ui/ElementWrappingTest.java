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
package com.google.gwt.user.client.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for various methods of the form {@link Button#wrap(Element)}.
 */
public class ElementWrappingTest extends GWTTestCase {

  private static final String TEST_URL = "http://www.google.com/";
  private static final String IMG_URL = "http://www.google.com/images/logo_sm.gif";

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.UserTest";
  }

  public void testAnchor() {
    ensureDiv().setInnerHTML("<a id='foo' href='" + TEST_URL + "'>myAnchor</a>");
    Anchor anchor = Anchor.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(anchor);
    assertEquals(TEST_URL, anchor.getHref());
    assertEquals("myAnchor", anchor.getText());
  }

  public void testButton() {
    ensureDiv().setInnerHTML("<button id='foo'>myButton</button>");
    Button button = Button.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(button);
    assertEquals("myButton", button.getText());
  }

  public void testFileUpload() {
    ensureDiv().setInnerHTML("<input type='file' id='foo'>myInput</input>");
    FileUpload upload = FileUpload.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(upload);
  }

  public void testFormPanel() {
    ensureDiv().setInnerHTML("<form id='foo'></form>");
    FormPanel formPanel = FormPanel.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(formPanel);
  }

  public void testFrame() {
    ensureDiv().setInnerHTML("<iframe id='foo'>myFrame</iframe>");
    Frame frame = Frame.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(frame);
  }

  public void testHidden() {
    ensureDiv().setInnerHTML("<input type='hidden' id='foo'></input>");
    Hidden hidden = Hidden.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(hidden);
  }

  public void testHTML() {
    ensureDiv().setInnerHTML("<div id='foo'>myHTML</div>");
    HTML html = HTML.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(html);
    assertEquals("myHTML", html.getHTML());
  }

  public void testImage() {
    ensureDiv().setInnerHTML("<img id='foo' src='" + IMG_URL + "'>");
    Image image = Image.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(image);
    assertEquals(IMG_URL, image.getUrl());
  }

  public void testInlineHTML() {
    ensureDiv().setInnerHTML("<span id='foo'>myInlineHTML</span>");
    InlineHTML html = InlineHTML.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(html);
    assertEquals("myInlineHTML", html.getHTML());
  }

  public void testInlineLabel() {
    ensureDiv().setInnerHTML("<span id='foo'>myInlineLabel</span>");
    InlineLabel label = InlineLabel.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(label);
    assertEquals("myInlineLabel", label.getText());
  }

  public void testLabel() {
    ensureDiv().setInnerHTML("<div id='foo'>myLabel</div>");
    Label label = Label.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(label);
    assertEquals("myLabel", label.getText());
  }

  public void testListBox() {
    ensureDiv().setInnerHTML("<select id='foo'></select>");
    ListBox listBox = ListBox.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(listBox);
  }

  public void testPasswordTextBox() {
    ensureDiv().setInnerHTML("<input type='password' id='foo'></input>");
    PasswordTextBox textBox = PasswordTextBox.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(textBox);
  }

  public void testSimpleCheckBox() {
    ensureDiv().setInnerHTML("<input type='checkbox' id='foo'></input>");
    SimpleCheckBox checkBox = SimpleCheckBox.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(checkBox);
  }

  public void testSimpleRadioButton() {
    ensureDiv().setInnerHTML("<input type='radio' id='foo'></input>");
    SimpleRadioButton radio = SimpleRadioButton.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(radio);
  }

  public void testTextBox() {
    ensureDiv().setInnerHTML("<input type='text' id='foo'></input>");
    TextBox textBox = TextBox.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(textBox);
  }

  private void assertExistsAndAttached(Widget widget) {
    assertNotNull(widget);
    assertTrue(widget.isAttached());
  }

  private Element ensureDiv() {
    Document doc = Document.get();
    Element div = doc.getElementById("wrapperDiv");
    if (div == null) {
      div = doc.createDivElement();
      div.setId("wrapperDiv");
      doc.getBody().appendChild(div);
    }
    return div;
  }
}
