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

import com.google.gwt.event.dom.client.DragEndEvent;
import com.google.gwt.event.dom.client.DragEndHandler;
import com.google.gwt.event.dom.client.DragEnterEvent;
import com.google.gwt.event.dom.client.DragEnterHandler;
import com.google.gwt.event.dom.client.DragEvent;
import com.google.gwt.event.dom.client.DragHandler;
import com.google.gwt.event.dom.client.DragLeaveEvent;
import com.google.gwt.event.dom.client.DragLeaveHandler;
import com.google.gwt.event.dom.client.DragOverEvent;
import com.google.gwt.event.dom.client.DragOverHandler;
import com.google.gwt.event.dom.client.DragStartEvent;
import com.google.gwt.event.dom.client.DragStartHandler;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.event.dom.client.DropHandler;
import com.google.gwt.event.dom.client.HasAllDragAndDropHandlers;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Test Case for sinking of drag and drop events.
 */
public class DragAndDropEventsSinkTest extends GWTTestCase {

  private static class DragEndHandlerImpl extends HandlerImpl implements DragEndHandler {
    @Override
    public void onDragEnd(DragEndEvent event) {
      eventFired();
    }
  }

  private static class DragEnterHandlerImpl extends HandlerImpl implements DragEnterHandler {
    @Override
    public void onDragEnter(DragEnterEvent event) {
      eventFired();
    }
  }

  private static class DragHandlerImpl extends HandlerImpl implements DragHandler {
    @Override
    public void onDrag(DragEvent event) {
      eventFired();
    }
  }

  private static class DragLeaveHandlerImpl extends HandlerImpl implements DragLeaveHandler {
    @Override
    public void onDragLeave(DragLeaveEvent event) {
      eventFired();
    }
  }

  private static class DragOverHandlerImpl extends HandlerImpl implements DragOverHandler {
    @Override
    public void onDragOver(DragOverEvent event) {
      eventFired();
    }
  }

  private static class DragStartHandlerImpl extends HandlerImpl implements DragStartHandler {
    @Override
    public void onDragStart(DragStartEvent event) {
      eventFired();
    }
  }

  private static class DropHandlerImpl extends HandlerImpl implements DropHandler {
    @Override
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

  /**
   * Interface to create a widget.
   *
   * @param <W> the widget type
   */
  private interface WidgetCreator<W extends Widget & HasAllDragAndDropHandlers> {
    /**
     * Create a widget to test.
     *
     * @return the new widget
     */
    W createWidget();
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

    verifyDragAndDropEventSink(new WidgetCreator<FocusPanel>() {
      @Override
      public FocusPanel createWidget() {
        return new FocusPanel();
      }
    });
  }

  public void testFocusWidgetEventSink() {
    // skip tests on browsers that do not support native drag events
    if (!isEventSupported("dragstart")) {
      return;
    }

    verifyDragAndDropEventSink(new WidgetCreator<Anchor>() {
      @Override
      public Anchor createWidget() {
        return new Anchor();
      }
    });
  }

  public void testHTMLTableEventSink() {
    // skip tests on browsers that do not support native drag events
    if (!isEventSupported("dragstart")) {
      return;
    }

    verifyDragAndDropEventSink(new WidgetCreator<Grid>() {
      @Override
      public Grid createWidget() {
        return new Grid();
      }
    }, new WidgetCreator<FlexTable>() {
      @Override
      public FlexTable createWidget() {
        return new FlexTable();
      }
    });
  }

  public void testImageEventSink() {
    // skip tests on browsers that do not support native drag events
    if (!isEventSupported("dragstart")) {
      return;
    }

    verifyDragAndDropEventSink(new WidgetCreator<Image>() {
      @Override
      public Image createWidget() {
        return new Image();
      }
    });
  }

  public void testLabelEventSink() {
    // skip tests on browsers that do not support native drag events
    if (!isEventSupported("dragstart")) {
      return;
    }

    verifyDragAndDropEventSink(new WidgetCreator<Label>() {
      @Override
      public Label createWidget() {
        return new Label();
      }
    });
  }

  @Override
  protected void gwtTearDown() throws Exception {
    // clean up after ourselves
    RootPanel.get().clear();
    super.gwtTearDown();
  }

  private void verifyDragAndDropEventSink(WidgetCreator<?>... creators) {
    for (WidgetCreator<?> creator : creators) {
      verifyDragEventSink(creator.createWidget());
      verifyDragEndEventSink(creator.createWidget());
      verifyDragEnterEventSink(creator.createWidget());
      verifyDragLeaveEventSink(creator.createWidget());
      verifyDragOverEventSink(creator.createWidget());
      verifyDragStartEventSink(creator.createWidget());
      verifyDropEventSink(creator.createWidget());
    }
  }

  private <W extends Widget & HasAllDragAndDropHandlers> void verifyDragEndEventSink(W w) {
    DragEndHandlerImpl handler = new DragEndHandlerImpl();
    w.addDragEndHandler(handler);

    assertFalse(handler.hasEventFired());
    w.fireEvent(new DragEndEvent() {
    });
    assertTrue(handler.hasEventFired());
  }

  private <W extends Widget & HasAllDragAndDropHandlers> void verifyDragEnterEventSink(W w) {
    DragEnterHandlerImpl handler = new DragEnterHandlerImpl();
    w.addDragEnterHandler(handler);

    assertFalse(handler.hasEventFired());
    w.fireEvent(new DragEnterEvent() {
    });
    assertTrue(handler.hasEventFired());
  }

  private <W extends Widget & HasAllDragAndDropHandlers> void verifyDragEventSink(W w) {
    DragHandlerImpl handler = new DragHandlerImpl();
    w.addDragHandler(handler);

    assertFalse(handler.hasEventFired());
    w.fireEvent(new DragEvent() {
    });
    assertTrue(handler.hasEventFired());
  }

  private <W extends Widget & HasAllDragAndDropHandlers> void verifyDragLeaveEventSink(W w) {
    DragLeaveHandlerImpl handler = new DragLeaveHandlerImpl();
    w.addDragLeaveHandler(handler);

    assertFalse(handler.hasEventFired());
    w.fireEvent(new DragLeaveEvent() {
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

  private <W extends Widget & HasAllDragAndDropHandlers> void verifyDragStartEventSink(W w) {
    DragStartHandlerImpl handler = new DragStartHandlerImpl();
    w.addDragStartHandler(handler);

    assertFalse(handler.hasEventFired());
    w.fireEvent(new DragStartEvent() {
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