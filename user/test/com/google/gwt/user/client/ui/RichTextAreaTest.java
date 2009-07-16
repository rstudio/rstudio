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
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RichTextArea.BasicFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the {@link RichTextArea} widget.
 */
public class RichTextAreaTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Test that removing and re-adding an RTA doesn't destroy its contents (Only
   * IE actually preserves dynamically-created iframe contents across DOM
   * removal/re-adding).
   */
  public void testAddEditRemoveAdd() {
    final RichTextArea area = new RichTextArea();
    RootPanel.get().add(area);
    area.setHTML("foo");

    // This has to be done on a timer because the rta can take some time to
    // finish initializing (on some browsers).
    this.delayTestFinish(1000);
    new Timer() {
      @Override
      public void run() {
        RootPanel.get().remove(area);
        RootPanel.get().add(area);

        // It's ok (and important) to check the HTML immediately after re-adding
        // the rta.
        assertEquals("foo", area.getHTML());
        finishTest();
      }
    }.schedule(500);
  }

  public void testBlurAfterAttach() {
    final RichTextArea rta = new RichTextArea();
    final List<String> actual = new ArrayList<String>();
    rta.addFocusHandler(new FocusHandler() {
      public void onFocus(FocusEvent event) {
        actual.add("test");
      }
    });
    RootPanel.get().add(rta);
    rta.setFocus(true);
    rta.setFocus(false);

    // This has to be done on a timer because the rta can take some time to
    // finish initializing (on some browsers).
    this.delayTestFinish(1000);
    new Timer() {
      @Override
      public void run() {
        assertEquals(0, actual.size());
        RootPanel.get().remove(rta);
        finishTest();
      }
    }.schedule(500);
  }

  public void testFocusAfterAttach() {
    final RichTextArea rta = new RichTextArea();
    final List<String> actual = new ArrayList<String>();
    rta.addFocusHandler(new FocusHandler() {
      public void onFocus(FocusEvent event) {
        actual.add("test");
      }
    });
    RootPanel.get().add(rta);
    rta.setFocus(true);

    // This has to be done on a timer because the rta can take some time to
    // finish initializing (on some browsers).
    this.delayTestFinish(1000);
    new Timer() {
      @Override
      public void run() {
        // IE focuses automatically, resulting in an extra event, so all we can
        // test is that we got at least one focus.
        assertTrue(actual.size() > 0);
        RootPanel.get().remove(rta);
        finishTest();
      }
    }.schedule(500);
  }

  /**
   * Test that adding and removing an RTA before initialization completes
   * doesn't throw an exception.
   */
  public void testAddRemoveBeforeInit() {
    final RichTextArea richTextArea = new RichTextArea();
    RootPanel.get().add(richTextArea);
    RootPanel.get().remove(richTextArea);
  }

  public void testFormatAfterAttach() {
    final RichTextArea area = new RichTextArea();
    BasicFormatter formatter = area.getBasicFormatter();
    RootPanel.get().add(area);
    if (formatter != null) {
      try {
        formatter.toggleBold();
        if (!GWT.isScript()) {
          fail("Expected AssertionError");
        }
      } catch (AssertionError e) {
        // Expected because the iframe is not initialized
        return;
      }
      if (!GWT.isScript()) {
        fail("Expected AssertionError");
      }
    }
  }

  public void testFormatAfterInitialize() {
    final RichTextArea area = new RichTextArea();
    RootPanel.get().add(area);

    // This has to be done on a timer because the rta can take some time to
    // finish initializing (on some browsers).
    this.delayTestFinish(1000);
    new Timer() {
      @Override
      public void run() {
        BasicFormatter formatter = area.getBasicFormatter();
        if (formatter != null) {
          formatter.toggleBold();
        }
        RootPanel.get().remove(area);
        finishTest();
      }
    }.schedule(500);
  }

  public void testFormatBeforeAttach() {
    final RichTextArea area = new RichTextArea();
    BasicFormatter formatter = area.getBasicFormatter();
    if (formatter != null) {
      try {
        formatter.toggleBold();
        if (!GWT.isScript()) {
          fail("Expected AssertionError");
        }
      } catch (AssertionError e) {
        // expected
        return;
      }
      if (!GWT.isScript()) {
        fail("Expected AssertionError");
      }
    }
  }

  public void testFormatWhenHidden() {
    final RichTextArea area = new RichTextArea();
    RootPanel.get().add(area);

    // This has to be done on a timer because the rta can take some time to
    // finish initializing (on some browsers).
    this.delayTestFinish(1000);
    new Timer() {
      @Override
      public void run() {
        area.setVisible(false);
        BasicFormatter formatter = area.getBasicFormatter();
        if (formatter != null) {
          // This won't work on some browsers, but it should return quietly.
          formatter.toggleBold();
        }
        RootPanel.get().remove(area);
        finishTest();
      }
    }.schedule(500);
  }

  /**
   * Test that events are dispatched correctly to handlers.
   */
  public void testEventDispatch() {
    final RichTextArea rta = new RichTextArea();
    RootPanel.get().add(rta);

    final List<String> actual = new ArrayList<String>();
    rta.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        assertNotNull(Event.getCurrentEvent());
        actual.add("test");
      }
    });

    // Fire a click event after the iframe is available
    delayTestFinish(1000);
    new Timer() {
      @Override
      public void run() {
        assertEquals(0, actual.size());
        NativeEvent event = getDocument(rta).createClickEvent(0, 0, 0, 0, 0,
            false, false, false, false);
        getBodyElement(rta).dispatchEvent(event);
        assertEquals(1, actual.size());
        RootPanel.get().remove(rta);
        finishTest();
      }
    }.schedule(500);
  }

  /**
   * Test that a delayed set of HTML is reflected. Some platforms have timing
   * subtleties that need to be tested.
   */
  public void testSetHTMLAfterInit() {
    final RichTextArea richTextArea = new RichTextArea();
    RootPanel.get().add(richTextArea);
    new Timer() {
      @Override
      public void run() {
        richTextArea.setHTML("<b>foo</b>");
        assertEquals("<b>foo</b>", richTextArea.getHTML().toLowerCase());
        finishTest();
      }
    }.schedule(200);
    delayTestFinish(1000);
  }

  /**
   * Test that an immediate set of HTML is reflected immediately and after a
   * delay. Some platforms have timing subtleties that need to be tested.
   */
  public void testSetHTMLBeforeInit() {
    final RichTextArea richTextArea = new RichTextArea();
    RootPanel.get().add(richTextArea);
    richTextArea.setHTML("<b>foo</b>");
    assertEquals("<b>foo</b>", richTextArea.getHTML().toLowerCase());
    new Timer() {
      @Override
      public void run() {
        assertEquals("<b>foo</b>", richTextArea.getHTML().toLowerCase());
        finishTest();
      }
    }.schedule(200);
    delayTestFinish(1000);
  }

  /**
   * Test that delayed set of text is reflected. Some platforms have timing
   * subtleties that need to be tested.
   */
  public void testSetTextAfterInit() {
    final RichTextArea richTextArea = new RichTextArea();
    RootPanel.get().add(richTextArea);
    new Timer() {
      @Override
      public void run() {
        richTextArea.setText("foo");
        assertEquals("foo", richTextArea.getText());
        finishTest();
      }
    }.schedule(200);
    delayTestFinish(1000);
  }

  /**
   * Test that an immediate set of text is reflected immediately and after a
   * delay. Some platforms have timing subtleties that need to be tested.
   */
  public void testSetTextBeforeInit() {
    final RichTextArea richTextArea = new RichTextArea();
    RootPanel.get().add(richTextArea);
    richTextArea.setText("foo");
    assertEquals("foo", richTextArea.getText());
    new Timer() {
      @Override
      public void run() {
        assertEquals("foo", richTextArea.getText());
        finishTest();
      }
    }.schedule(200);
    delayTestFinish(1000);
  }

  /**
   * Get the body element from a RichTextArea.
   * 
   * @param rta the {@link RichTextArea}
   * @return the body element
   */
  private Element getBodyElement(RichTextArea rta) {
    return getDocument(rta).getBody().cast();
  }

  /**
   * Get the iframe's Document. This is useful for creating events, which must
   * be created in the iframe's document to work correctly.
   * 
   * @param rta the {@link RichTextArea}
   * @return the document element
   */
  private Document getDocument(RichTextArea rta) {
    return getDocumentImpl(rta.getElement());
  }

  private native Document getDocumentImpl(Element iframe) /*-{
      return iframe.contentWindow.document;
    }-*/;
}
