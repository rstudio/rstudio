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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
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

  /**
   * Tests {@link Anchor#wrap(Element)}.
   */
  public void testAnchor() {
    ensureDiv().setInnerHTML("<a id='foo' href='" + TEST_URL + "'>myAnchor</a>");
    Anchor anchor = Anchor.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(anchor);
    assertEquals(TEST_URL, anchor.getHref());
    assertEquals("myAnchor", anchor.getText());
  }

  /**
   * Tests {@link Button#wrap(Element)}.
   */
  public void testButton() {
    ensureDiv().setInnerHTML("<button id='foo'>myButton</button>");
    Button button = Button.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(button);
    assertEquals("myButton", button.getText());
  }

  /**
   * Tests that {@link RootPanel#detachNow(Widget)} can only be called once per
   * widget.
   */
  public void testDetachNowTwiceFails() {
    // Testing hosted-mode-only assertion.
    if (!GWT.isScript()) {
      try {
        // Trying to pass the same widget to RootPanel.detachNow() twice
        // should fail an assertion.
        ensureDiv().setInnerHTML(
            "<a id='foo' href='" + TEST_URL + "'>myAnchor</a>");
        Anchor a = Anchor.wrap(Document.get().getElementById("foo"));
        RootPanel.detachNow(a); // pass
        RootPanel.detachNow(a); // fail
        fail("Expected assertion failure calling detachNow() twice");
      } catch (AssertionError e) {
      }
    }
  }

  /**
   * Tests that {@link RootPanel#detachOnWindowClose(Widget)} can only be called
   * once per widget.
   */
  public void testDetachOnWindowCloseTwiceFails() {
    // Testing hosted-mode-only assertion.
    if (!GWT.isScript()) {
      try {
        // Trying to pass the same widget to RootPanel.detachOnUnload() twice
        // should fail an assertion (the first call is implicit through
        // Anchor.wrap()).
        ensureDiv().setInnerHTML(
            "<a id='foo' href='" + TEST_URL + "'>myAnchor</a>");
        Anchor a = Anchor.wrap(Document.get().getElementById("foo")); // pass
        RootPanel.detachOnWindowClose(a); // fail
        fail("Expected assertion failure calling detachOnLoad() twice");
      } catch (AssertionError e) {
      }
    }
  }

  /**
   * Tests {@link FileUpload#wrap(Element)}.
   */
  public void testFileUpload() {
    ensureDiv().setInnerHTML("<input type='file' id='foo'>myInput</input>");
    FileUpload upload = FileUpload.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(upload);
  }

  /**
   * Tests {@link FormPanel#wrap(Element)}.
   */
  public void testFormPanel() {
    ensureDiv().setInnerHTML("<form id='foo'></form>");
    FormPanel formPanel = FormPanel.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(formPanel);
  }

  /**
   * Tests {@link Frame#wrap(Element)}.
   */
  public void testFrame() {
    ensureDiv().setInnerHTML("<iframe id='foo'>myFrame</iframe>");
    Frame frame = Frame.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(frame);
  }

  /**
   * Tests {@link Hidden#wrap(Element)}.
   */
  public void testHidden() {
    ensureDiv().setInnerHTML("<input type='hidden' id='foo'></input>");
    Hidden hidden = Hidden.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(hidden);
  }

  /**
   * Tests {@link HTML#wrap(Element)}.
   */
  public void testHTML() {
    ensureDiv().setInnerHTML("<div id='foo'>myHTML</div>");
    HTML html = HTML.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(html);
    assertEquals("myHTML", html.getHTML());
  }

  /**
   * Tests {@link HTMLPanel#wrap(Element)}.
   */
  public void testHTMLPanel() {
    ensureDiv().setInnerHTML("<div id='foo'>my<div id='bar'>HTML</div></div>");
    Element bar = Document.get().getElementById("bar");
    HTMLPanel html = HTMLPanel.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(html);
    assertTrue(html.getElement().isOrHasChild(bar));
  }  

  /**
   * Tests {@link Image#wrap(Element)}.
   */
  public void testImage() {
    ensureDiv().setInnerHTML("<img id='foo' src='" + IMG_URL + "'>");
    Image image = Image.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(image);
    assertEquals(IMG_URL, image.getUrl());
  }

  /**
   * Tests {@link InlineHTML#wrap(Element)}.
   */
  public void testInlineHTML() {
    ensureDiv().setInnerHTML("<span id='foo'>myInlineHTML</span>");
    InlineHTML html = InlineHTML.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(html);
    assertEquals("myInlineHTML", html.getHTML());
  }

  /**
   * Tests {@link InlineLabel#wrap(Element)}.
   */
  public void testInlineLabel() {
    ensureDiv().setInnerHTML("<span id='foo'>myInlineLabel</span>");
    InlineLabel label = InlineLabel.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(label);
    assertEquals("myInlineLabel", label.getText());
  }

  /**
   * Tests {@link Label#wrap(Element)}.
   */
  public void testLabel() {
    ensureDiv().setInnerHTML("<div id='foo'>myLabel</div>");
    Label label = Label.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(label);
    assertEquals("myLabel", label.getText());
  }

  /**
   * Tests {@link ListBox#wrap(Element)}.
   */
  public void testListBox() {
    ensureDiv().setInnerHTML("<select id='foo'></select>");
    ListBox listBox = ListBox.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(listBox);
  }

  /**
   * Tests that all widgets passed to
   * {@link RootPanel#detachOnWindowClose(Widget)} are cleaned up properly when
   * the window is unloaded, regardless of whether their associated elements are
   * still in the DOM or not.
   */
  public void testOnUnloadDetachesAllWidgets() {
    // Testing hosted-mode-only assertion.
    if (!GWT.isScript()) {
      ensureDiv().setInnerHTML(
          "<a id='foo' href='" + TEST_URL + "'>myAnchor</a>"
              + "<a id='bar' href='" + TEST_URL + "'>myOtherAnchor</a>");

      // Wrap one widget that will be left in the DOM normally.
      Element fooElem = Document.get().getElementById("foo");
      Anchor fooAnchor = Anchor.wrap(fooElem);

      // Wrap another widget and remove its element from the DOM.
      Element barElem = Document.get().getElementById("bar");
      Anchor barAnchor = Anchor.wrap(barElem);
      barElem.getParentElement().removeChild(barElem);

      // Fake an unload by telling the RootPanel to go ahead and detach all
      // of its widgets.
      RootPanel.detachWidgets();

      // Now make sure that both widgets were detached properly.
      assertFalse("fooAnchor should have been detached", fooAnchor.isAttached());
      assertFalse("barAnchor should have been detached", barAnchor.isAttached());
    }
  }

  /**
   * Tests {@link PasswordTextBox#wrap(Element)}.
   */
  public void testPasswordTextBox() {
    ensureDiv().setInnerHTML("<input type='password' id='foo'></input>");
    PasswordTextBox textBox = PasswordTextBox.wrap(Document.get().getElementById(
        "foo"));

    assertExistsAndAttached(textBox);
  }

  /**
   * Tests {@link SimpleCheckBox#wrap(Element)}.
   */
  public void testSimpleCheckBox() {
    ensureDiv().setInnerHTML("<input type='checkbox' id='foo'></input>");
    SimpleCheckBox checkBox = SimpleCheckBox.wrap(Document.get().getElementById(
        "foo"));

    assertExistsAndAttached(checkBox);
  }

  /**
   * Tests {@link SimpleRadioButton#wrap(Element)}.
   */
  public void testSimpleRadioButton() {
    ensureDiv().setInnerHTML("<input type='radio' id='foo'></input>");
    SimpleRadioButton radio = SimpleRadioButton.wrap(Document.get().getElementById(
        "foo"));

    assertExistsAndAttached(radio);
  }

  /**
   * Tests {@link TextArea#wrap(Element)}.
   */
  public void testTextArea() {
    ensureDiv().setInnerHTML("<textarea rows='1' cols='1' id='foo'></textarea>");
    TextArea textArea = TextArea.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(textArea);
  }

  /**
   * Tests {@link TextBox#wrap(Element)}.
   */
  public void testTextBox() {
    ensureDiv().setInnerHTML("<input type='text' id='foo'></input>");
    TextBox textBox = TextBox.wrap(Document.get().getElementById("foo"));

    assertExistsAndAttached(textBox);
  }

  /**
   * Tests that wrapping an element that is already a child of an existing
   * widget's element fails.
   */
  public void testWrappingChildElementFails() {
    // Testing hosted-mode-only assertion.
    if (!GWT.isScript()) {
      try {
        // Create a panel that contains HTML with a unique id, which we're
        // going to try and wrap below.
        FlowPanel p = new FlowPanel();
        RootPanel.get().add(p);
        p.add(new HTML("<a id='twcef_id'>foo</a>"));

        // Get the element and try to wrap it.
        Element unwrappableElement = Document.get().getElementById("twcef_id");
        Anchor.wrap(unwrappableElement);
        fail("Attempting to wrap the above element should have failed.");
      } catch (AssertionError e) {
        // Expected error.
      }
    }
  }

  /**
   * Tests that wrap() may only be called on elements that are already attached
   * to the DOM.
   */
  public void testWrapUnattachedFails() {
    // Testing hosted-mode-only assertion.
    if (!GWT.isScript()) {
      try {
        // Trying to wrap an unattached element should fail an assertion.
        // We only test this for one element/widget type, because they
        // all call RootPanel.detachOnUnload(), where the actual assertion
        // occurs.
        AnchorElement aElem = Document.get().createAnchorElement();
        Anchor.wrap(aElem);
        fail("Expected assertion failure wrapping unattached element");
      } catch (AssertionError e) {
      }
    }
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
