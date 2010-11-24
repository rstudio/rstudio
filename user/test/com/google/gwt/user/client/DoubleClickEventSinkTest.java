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

import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.HasDoubleClickHandlers;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Grid;
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
 * Test Case for sinking of double click events.
 */
public class DoubleClickEventSinkTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testDoubleClickBitFieldNotTriviallyZero() {
    assertNotSame(0, Event.ONDBLCLICK);
  }

  public void testFocusPanelDoubleClickEventSinkByAddingHandler() {
    verifyEventSinkOnAddHandler(new FocusPanel(), false);
  }

  public void testFocusWidgetDoubleClickEventSinkByAddingHandler() {
    verifyEventSinkOnAddHandler(new Anchor(), false);
    verifyEventSinkOnAddHandler(new Button(), false);

    CheckBox checkBox = new CheckBox();
    // Get the inputElem on which events are sunk
    Element e = (Element) checkBox.getElement().getFirstChildElement();
    verifyEventSinkOnAddHandler(checkBox, e, false);

    verifyEventSinkOnAddHandler(new ToggleButton(), false);
    verifyEventSinkOnAddHandler(new ListBox(), false);
    verifyEventSinkOnAddHandler(new RichTextArea(), false);
    verifyEventSinkOnAddHandler(new TextArea(), false);
    verifyEventSinkOnAddHandler(new PasswordTextBox(), false);
    verifyEventSinkOnAddHandler(new TextBox(), false);
    verifyEventSinkOnAddHandler(new SimpleRadioButton("foo"), false);
  }

  public void testHTMLTableDoubleClickEventSinkByAddingHandler() {
    verifyEventSinkOnAddHandler(new Grid(), false);
    verifyEventSinkOnAddHandler(new FlexTable(), false);
  }

  public void testImageDoubleClickEventSinkByAddingHandler() {
    /*
     * The Image widget currently sinks events too early, before handlers are
     * attached. We verify that (broken) behavior in this test.
     * TODO(fredsa) Once Image has been fixed to lazily sink events, update
     * this test and remove verifyEventSinkOnAddHandler's second parameter.
     */
    verifyEventSinkOnAddHandler(new Image(), true);
  }

  public void testLabelDoubleClickEventSinkByAddingHandler() {
    verifyEventSinkOnAddHandler(new Label(), false);
  }

  @Override
  protected void gwtTearDown() throws Exception {
    // clean up after ourselves
    RootPanel.get().clear();
    super.gwtTearDown();
  }

  private boolean isDoubleClickEventSunk(Element e) {
    return (DOM.getEventsSunk(e) & Event.ONDBLCLICK) != 0;
  }

  private <W extends Widget & HasDoubleClickHandlers>
      void verifyEventSinkOnAddHandler(W w, boolean allowEarlySink) {
    verifyEventSinkOnAddHandler(w, w.getElement(), allowEarlySink);
  }

  private <W extends Widget & HasDoubleClickHandlers>
      void verifyEventSinkOnAddHandler(W w, Element e, boolean widgetSinksEventsOnAttach) {
    RootPanel.get().add(w);

    if (widgetSinksEventsOnAttach) {
      assertTrue("Event should have been sunk on " + w.getClass().getName()
          + " once the widget has been attached", isDoubleClickEventSunk(e));
    } else {
      assertFalse(
          "Event should not be sunk on " + w.getClass().getName() + " until a "
              + DoubleClickEvent.getType().getName()
              + " handler has been added", isDoubleClickEventSunk(e));
    }

    w.addDoubleClickHandler(new DoubleClickHandler() {
      public void onDoubleClick(DoubleClickEvent event) {
      }
    });

    assertTrue(
        "Event should have been sunk on " + w.getClass().getName()
            + " once the widget has been attached and a "
            + DoubleClickEvent.getType().getName() + " handler has been added",
        isDoubleClickEventSunk(e));
  }
}
