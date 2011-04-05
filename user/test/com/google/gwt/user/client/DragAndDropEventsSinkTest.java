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

package com.google.gwt.user.client;

import com.google.gwt.event.dom.client.DragEnterEvent;
import com.google.gwt.event.dom.client.DragEnterHandler;
import com.google.gwt.event.dom.client.DragExitEvent;
import com.google.gwt.event.dom.client.DragExitHandler;
import com.google.gwt.event.dom.client.DragOverEvent;
import com.google.gwt.event.dom.client.DragOverHandler;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.event.dom.client.DropHandler;
import com.google.gwt.event.dom.client.HasAllDragAndDropHandlers;
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
 * Test Case for sinking of drag and drop events.
 */
public class DragAndDropEventsSinkTest extends GWTTestCase {

  private static class DragEnterHandlerImpl extends HandlerImpl implements
      DragEnterHandler {
    public void onDragEnter(DragEnterEvent event) {
      eventFired();
    }
  }

  private static class DragExitHandlerImpl extends HandlerImpl implements
      DragExitHandler {
    public void onDragExit(DragExitEvent event) {
      eventFired();
    }
  }

  private static class DragOverHandlerImpl extends HandlerImpl implements
      DragOverHandler {
    public void onDragOver(DragOverEvent event) {
      eventFired();
    }
  }

  private static class DropHandlerImpl extends HandlerImpl implements
      DropHandler {
    public void onDrop(DropEvent event) {
      eventFired();
    }
  }

  private static class HandlerImpl {
    private boolean fired = false;

    public void eventFired() {
      fired = true;
    }

    boolean hasEventFired() {
      return fired;
    }
  }

  public static native boolean isEventSupported(String eventName) /*-{
    var div = $doc.createElement("div");
    return ("on" + eventName) in div;
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testFocusPanelEventSink() {
    // skip tests on browsers that do not support native drag events
    if (!isEventSupported("dragstart")) {
      return;
    }
    
    verifyDragEnterEventSink(new FocusPanel());
    verifyDragExitEventSink(new FocusPanel());
    verifyDragOverEventSink(new FocusPanel());
    verifyDropEventSink(new FocusPanel());
  }

  public void testFocusWidgetEventSink() {
    // skip tests on browsers that do not support native drag events
    if (!isEventSupported("dragstart")) {
      return;
    }
    
    verifyDragEnterEventSink(new Anchor());
    verifyDragExitEventSink(new Anchor());
    verifyDragOverEventSink(new Anchor());
    verifyDropEventSink(new Anchor());

    verifyDragEnterEventSink(new Button());
    verifyDragExitEventSink(new Button());
    verifyDragOverEventSink(new Button());
    verifyDropEventSink(new Button());

    verifyDragEnterEventSink(new CheckBox());
    verifyDragExitEventSink(new CheckBox());
    verifyDragOverEventSink(new CheckBox());
    verifyDropEventSink(new CheckBox());

    verifyDragEnterEventSink(new ToggleButton());
    verifyDragExitEventSink(new ToggleButton());
    verifyDragOverEventSink(new ToggleButton());
    verifyDropEventSink(new ToggleButton());

    verifyDragEnterEventSink(new ListBox());
    verifyDragExitEventSink(new ListBox());
    verifyDragOverEventSink(new ListBox());
    verifyDropEventSink(new ListBox());

    verifyDragEnterEventSink(new RichTextArea());
    verifyDragExitEventSink(new RichTextArea());
    verifyDragOverEventSink(new RichTextArea());
    verifyDropEventSink(new RichTextArea());

    verifyDragEnterEventSink(new TextArea());
    verifyDragExitEventSink(new TextArea());
    verifyDragOverEventSink(new TextArea());
    verifyDropEventSink(new TextArea());

    verifyDragEnterEventSink(new PasswordTextBox());
    verifyDragExitEventSink(new PasswordTextBox());
    verifyDragOverEventSink(new PasswordTextBox());
    verifyDropEventSink(new PasswordTextBox());

    verifyDragEnterEventSink(new TextBox());
    verifyDragExitEventSink(new TextBox());
    verifyDragOverEventSink(new TextBox());
    verifyDropEventSink(new TextBox());

    verifyDragEnterEventSink(new SimpleRadioButton("foo"));
    verifyDragExitEventSink(new SimpleRadioButton("foo"));
    verifyDragOverEventSink(new SimpleRadioButton("foo"));
    verifyDropEventSink(new SimpleRadioButton("foo"));
  }

  public void testHTMLTableEventSink() {
    // skip tests on browsers that do not support native drag events
    if (!isEventSupported("dragstart")) {
      return;
    }
    
    verifyDragEnterEventSink(new Grid());
    verifyDragExitEventSink(new Grid());
    verifyDragOverEventSink(new Grid());
    verifyDropEventSink(new Grid());

    verifyDragEnterEventSink(new FlexTable());
    verifyDragExitEventSink(new FlexTable());
    verifyDragOverEventSink(new FlexTable());
    verifyDropEventSink(new FlexTable());
  }

  public void testImageEventSink() {
    // skip tests on browsers that do not support native drag events
    if (!isEventSupported("dragstart")) {
      return;
    }
    
    verifyDragEnterEventSink(new Image());
    verifyDragExitEventSink(new Image());
    verifyDragOverEventSink(new Image());
    verifyDropEventSink(new Image());
  }

  public void testLabelEventSink() {
    // skip tests on browsers that do not support native drag events
    if (!isEventSupported("dragstart")) {
      return;
    }
    
    verifyDragEnterEventSink(new Label());
    verifyDragExitEventSink(new Label());
    verifyDragOverEventSink(new Label());
    verifyDropEventSink(new Label());
  }

  @Override
  protected void gwtTearDown() throws Exception {
    // clean up after ourselves
    RootPanel.get().clear();
    super.gwtTearDown();
  }

  private <W extends Widget & HasAllDragAndDropHandlers> void verifyDragEnterEventSink(W w) {
    DragEnterHandlerImpl handler = new DragEnterHandlerImpl();
    w.addDragEnterHandler(handler);

    assertFalse(handler.hasEventFired());
    w.fireEvent(new DragEnterEvent() {
    });
    assertTrue(handler.hasEventFired());
  }

  private <W extends Widget & HasAllDragAndDropHandlers> void verifyDragExitEventSink(W w) {
    DragExitHandlerImpl handler = new DragExitHandlerImpl();
    w.addDragExitHandler(handler);

    assertFalse(handler.hasEventFired());
    w.fireEvent(new DragExitEvent() {
    });
    assertTrue(handler.hasEventFired());
  }

  private <W extends Widget & HasAllDragAndDropHandlers> void verifyDragOverEventSink(W w) {
    DragOverHandlerImpl handler = new DragOverHandlerImpl();
    w.addDragOverHandler(handler);

    assertFalse(handler.hasEventFired());
    w.fireEvent(new DragOverEvent() {
    });
    assertTrue(handler.hasEventFired());
  }

  private <W extends Widget & HasAllDragAndDropHandlers> void verifyDropEventSink(W w) {
    DropHandlerImpl handler = new DropHandlerImpl();
    w.addDropHandler(handler);

    assertFalse(handler.hasEventFired());
    w.fireEvent(new DropEvent() {
    });
    assertTrue(handler.hasEventFired());
  }
}