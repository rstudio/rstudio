/*
 * Copyright 2010 Google Inc.
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

package com.google.gwt.user.client;

import com.google.gwt.event.dom.client.HasAllTouchHandlers;
import com.google.gwt.event.dom.client.TouchCancelEvent;
import com.google.gwt.event.dom.client.TouchCancelHandler;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimpleRadioButton;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;

/**
 * Test Case for sinking of touch events.
 */
public class TouchEventSinkTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testFocusPanelTouchEventSinkByAddingHandler() {
    verifyTouchStartEventSinkOnAddHandler(new FocusPanel(), false);
    verifyTouchMoveEventSinkOnAddHandler(new FocusPanel(), false);
    verifyTouchEndEventSinkOnAddHandler(new FocusPanel(), false);
    verifyTouchCancelEventSinkOnAddHandler(new FocusPanel(), false);
  }

  public void testFocusWidgetTouchEventSinkByAddingHandler() {
    verifyTouchStartEventSinkOnAddHandler(new Anchor(), false);
    verifyTouchMoveEventSinkOnAddHandler(new Anchor(), false);
    verifyTouchEndEventSinkOnAddHandler(new Anchor(), false);
    verifyTouchCancelEventSinkOnAddHandler(new Anchor(), false);

    verifyTouchStartEventSinkOnAddHandler(new Button(), false);
    verifyTouchMoveEventSinkOnAddHandler(new Button(), false);
    verifyTouchEndEventSinkOnAddHandler(new Button(), false);
    verifyTouchCancelEventSinkOnAddHandler(new Button(), false);

    CheckBox checkBox1 = new CheckBox();
    // Get the inputElem on which events are sunk
    Element e1 = (Element) checkBox1.getElement().getFirstChildElement();
    verifyTouchStartEventSinkOnAddHandler(checkBox1, e1, false);

    CheckBox checkBox2 = new CheckBox();
    // Get the inputElem on which events are sunk
    Element e2 = (Element) checkBox2.getElement().getFirstChildElement();
    verifyTouchMoveEventSinkOnAddHandler(checkBox2, e2, false);

    CheckBox checkBox3 = new CheckBox();
    // Get the inputElem on which events are sunk
    Element e3 = (Element) checkBox3.getElement().getFirstChildElement();
    verifyTouchEndEventSinkOnAddHandler(checkBox3, e3, false);

    CheckBox checkBox4 = new CheckBox();
    // Get the inputElem on which events are sunk
    Element e4 = (Element) checkBox4.getElement().getFirstChildElement();
    verifyTouchCancelEventSinkOnAddHandler(checkBox4, e4, false);

    verifyTouchStartEventSinkOnAddHandler(new ToggleButton(), false);
    verifyTouchMoveEventSinkOnAddHandler(new ToggleButton(), false);
    verifyTouchEndEventSinkOnAddHandler(new ToggleButton(), false);
    verifyTouchCancelEventSinkOnAddHandler(new ToggleButton(), false);

    verifyTouchStartEventSinkOnAddHandler(new ListBox(), false);
    verifyTouchMoveEventSinkOnAddHandler(new ListBox(), false);
    verifyTouchEndEventSinkOnAddHandler(new ListBox(), false);
    verifyTouchCancelEventSinkOnAddHandler(new ListBox(), false);

    verifyTouchStartEventSinkOnAddHandler(new RichTextArea(), false);
    verifyTouchMoveEventSinkOnAddHandler(new RichTextArea(), false);
    verifyTouchEndEventSinkOnAddHandler(new RichTextArea(), false);
    verifyTouchCancelEventSinkOnAddHandler(new RichTextArea(), false);

    verifyTouchStartEventSinkOnAddHandler(new TextArea(), false);
    verifyTouchMoveEventSinkOnAddHandler(new TextArea(), false);
    verifyTouchEndEventSinkOnAddHandler(new TextArea(), false);
    verifyTouchCancelEventSinkOnAddHandler(new TextArea(), false);

    verifyTouchStartEventSinkOnAddHandler(new PasswordTextBox(), false);
    verifyTouchMoveEventSinkOnAddHandler(new PasswordTextBox(), false);
    verifyTouchEndEventSinkOnAddHandler(new PasswordTextBox(), false);
    verifyTouchCancelEventSinkOnAddHandler(new PasswordTextBox(), false);

    verifyTouchStartEventSinkOnAddHandler(new TextBox(), false);
    verifyTouchMoveEventSinkOnAddHandler(new TextBox(), false);
    verifyTouchEndEventSinkOnAddHandler(new TextBox(), false);
    verifyTouchCancelEventSinkOnAddHandler(new TextBox(), false);

    verifyTouchStartEventSinkOnAddHandler(new SimpleRadioButton("foo"), false);
    verifyTouchMoveEventSinkOnAddHandler(new SimpleRadioButton("foo"), false);
    verifyTouchEndEventSinkOnAddHandler(new SimpleRadioButton("foo"), false);
    verifyTouchCancelEventSinkOnAddHandler(new SimpleRadioButton("foo"), false);
  }

  public void testImageTouchEventSinkByAddingHandler() {
    /*
     * The Image widget currently sinks events too early, before handlers are
     * attached. We verify that (broken) behavior in this test. TODO(fredsa)
     * Once Image has been fixed to lazily sink events, update this test and
     * remove verifyEventSinkOnAddHandler's second parameter.
     */
    verifyTouchStartEventSinkOnAddHandler(new Image(), true);
    verifyTouchMoveEventSinkOnAddHandler(new Image(), true);
    verifyTouchEndEventSinkOnAddHandler(new Image(), true);
    verifyTouchCancelEventSinkOnAddHandler(new Image(), true);
  }

  public void testLabelTouchEventSinkByAddingHandler() {
    verifyTouchStartEventSinkOnAddHandler(new Label(), false);
    verifyTouchMoveEventSinkOnAddHandler(new Label(), false);
    verifyTouchEndEventSinkOnAddHandler(new Label(), false);
    verifyTouchCancelEventSinkOnAddHandler(new Label(), false);
  }

  public void testTouchEventBitFieldsNotTriviallyZero() {
    assertNotSame(0, Event.ONTOUCHSTART);
    assertNotSame(0, Event.ONTOUCHMOVE);
    assertNotSame(0, Event.ONTOUCHEND);
    assertNotSame(0, Event.ONTOUCHCANCEL);
  }

  @Override
  protected void gwtTearDown() throws Exception {
    // clean up after ourselves
    RootPanel.get().clear();
    super.gwtTearDown();
  }

  private <W extends Widget & HasAllTouchHandlers>
      void assertNotSunkAfterAttach(W w, String eventName, boolean isSunk) {
    assertFalse(
        "Event should not be sunk on " + w.getClass().getName() + " until a "
            + eventName + " handler has been added", isSunk);
  }

  private <W extends Widget & HasAllTouchHandlers>
      void assertSunkAfterAddHandler(W w, String eventName, boolean isSunk) {
    assertTrue("Event should have been sunk on " + w.getClass().getName()
        + " once the widget has been attached and a " + eventName
        + " handler has been added", isSunk);
  }

  private <W extends Widget & HasAllTouchHandlers> void assertSunkAfterAttach(
      W w, String eventName, boolean isSunk) {
    assertTrue("Event should have been sunk on " + w.getClass().getName()
        + " once the widget has been attached", isSunk);
  }

  private boolean isTouchCancelEventSunk(Element e) {
    return (DOM.getEventsSunk(e) & Event.ONTOUCHCANCEL) != 0;
  }

  private boolean isTouchEndEventSunk(Element e) {
    return (DOM.getEventsSunk(e) & Event.ONTOUCHEND) != 0;
  }

  private boolean isTouchMoveEventSunk(Element e) {
    return (DOM.getEventsSunk(e) & Event.ONTOUCHMOVE) != 0;
  }

  private boolean isTouchStartEventSunk(Element e) {
    return (DOM.getEventsSunk(e) & Event.ONTOUCHSTART) != 0;
  }

  private <W extends Widget & HasAllTouchHandlers>
      void verifyTouchCancelEventSinkOnAddHandler(W w, boolean allowEarlySink) {
    verifyTouchCancelEventSinkOnAddHandler(w, w.getElement(), allowEarlySink);
  }

  private <W extends Widget & HasAllTouchHandlers>
      void verifyTouchCancelEventSinkOnAddHandler(
          W w, Element e, boolean widgetSinksEventsOnAttach) {
    RootPanel.get().add(w);

    if (widgetSinksEventsOnAttach) {
      assertSunkAfterAttach(
          w, TouchCancelEvent.getType().getName(), isTouchCancelEventSunk(e));
    } else {
      assertNotSunkAfterAttach(
          w, TouchCancelEvent.getType().getName(), isTouchCancelEventSunk(e));
    }

    w.addTouchCancelHandler(new TouchCancelHandler() {
      public void onTouchCancel(TouchCancelEvent event) {
      }
    });

    assertSunkAfterAddHandler(
        w, TouchCancelEvent.getType().getName(), isTouchCancelEventSunk(e));
  }

  private <W extends Widget & HasAllTouchHandlers>
      void verifyTouchEndEventSinkOnAddHandler(W w, boolean allowEarlySink) {
    verifyTouchEndEventSinkOnAddHandler(w, w.getElement(), allowEarlySink);
  }

  private <W extends Widget & HasAllTouchHandlers>
      void verifyTouchEndEventSinkOnAddHandler(
          W w, Element e, boolean widgetSinksEventsOnAttach) {
    RootPanel.get().add(w);

    if (widgetSinksEventsOnAttach) {
      assertSunkAfterAttach(
          w, TouchEndEvent.getType().getName(), isTouchEndEventSunk(e));
    } else {
      assertNotSunkAfterAttach(
          w, TouchEndEvent.getType().getName(), isTouchEndEventSunk(e));
    }

    w.addTouchEndHandler(new TouchEndHandler() {
      public void onTouchEnd(TouchEndEvent event) {
      }
    });

    assertSunkAfterAddHandler(
        w, TouchEndEvent.getType().getName(), isTouchEndEventSunk(e));
  }

  private <W extends Widget & HasAllTouchHandlers>
      void verifyTouchMoveEventSinkOnAddHandler(W w, boolean allowEarlySink) {
    verifyTouchMoveEventSinkOnAddHandler(w, w.getElement(), allowEarlySink);
  }

  private <W extends Widget & HasAllTouchHandlers>
      void verifyTouchMoveEventSinkOnAddHandler(
          W w, Element e, boolean widgetSinksEventsOnAttach) {
    RootPanel.get().add(w);

    if (widgetSinksEventsOnAttach) {
      assertSunkAfterAttach(
          w, TouchMoveEvent.getType().getName(), isTouchMoveEventSunk(e));
    } else {
      assertNotSunkAfterAttach(
          w, TouchMoveEvent.getType().getName(), isTouchMoveEventSunk(e));
    }

    w.addTouchMoveHandler(new TouchMoveHandler() {

      public void onTouchMove(TouchMoveEvent event) {
      }
    });

    assertSunkAfterAddHandler(
        w, TouchMoveEvent.getType().getName(), isTouchMoveEventSunk(e));
  }

  private <W extends Widget & HasAllTouchHandlers>
      void verifyTouchStartEventSinkOnAddHandler(W w, boolean allowEarlySink) {
    verifyTouchStartEventSinkOnAddHandler(w, w.getElement(), allowEarlySink);
  }

  private <W extends Widget & HasAllTouchHandlers>
      void verifyTouchStartEventSinkOnAddHandler(
          W w, Element e, boolean widgetSinksEventsOnAttach) {
    RootPanel.get().add(w);

    if (widgetSinksEventsOnAttach) {
      assertSunkAfterAttach(
          w, TouchStartEvent.getType().getName(), isTouchStartEventSunk(e));
    } else {
      assertNotSunkAfterAttach(
          w, TouchStartEvent.getType().getName(), isTouchStartEventSunk(e));
    }

    w.addTouchStartHandler(new TouchStartHandler() {
      public void onTouchStart(TouchStartEvent event) {
      }
    });

    assertSunkAfterAddHandler(
        w, TouchStartEvent.getType().getName(), isTouchStartEventSunk(e));
  }
}
