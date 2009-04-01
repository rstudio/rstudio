/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.HasContextMenuHandlers;
import com.google.gwt.event.dom.client.HasDoubleClickHandlers;
import com.google.gwt.event.dom.client.HasMouseWheelHandlers;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;

import java.util.HashMap;
import java.util.Map;

/**
 * Verify that events fire in all browsers.
 */
public class VisualsForEventsFiring extends AbstractIssue {
  private static class CustomImage extends Image implements
      HasDoubleClickHandlers, HasContextMenuHandlers {
    public HandlerRegistration addContextMenuHandler(ContextMenuHandler handler) {
      return addDomHandler(handler, ContextMenuEvent.getType());
    }

    public HandlerRegistration addDoubleClickHandler(DoubleClickHandler handler) {
      return addDomHandler(handler, DoubleClickEvent.getType());
    }
  }

  private static class CustomScrollPanel extends ScrollPanel implements
      HasMouseWheelHandlers {
    public CustomScrollPanel(Widget content) {
      super(content);
    }

    public HandlerRegistration addMouseWheelHandler(MouseWheelHandler handler) {
      return addDomHandler(handler, MouseWheelEvent.getType());
    }
  }

  private static class CustomTextBox extends TextBox implements
      HasChangeHandlers {
    public HandlerRegistration addChangeHandler(ChangeHandler handler) {
      return addDomHandler(handler, ChangeEvent.getType());
    }
  }

  private static final int WINDOW_EVENT_SCROLL = -1;
  private static final int WINDOW_EVENT_RESIZE = -2;
  private static final int WINDOW_EVENT_CLOSING = -3;
  private static final int VELOCITY_EVENT = -4;

  /**
   * The main grid used for layout.
   */
  private FlexTable layout = null;

  private Map<Integer, Integer> eventMap = new HashMap<Integer, Integer>();

  @Override
  public Widget createIssue() {
    // Create a grid to hold all of the tests
    eventMap.clear();
    layout = new FlexTable();
    layout.setCellPadding(3);
    layout.setBorderWidth(2);
    layout.setHTML(0, 0, "<b>Action to Perform</b>");
    layout.setHTML(0, 1, "<b>Event</b>");
    layout.setHTML(0, 2, "<b>Status</b>");

    prepMouseEvents();
    prepKeyboardEvents();
    prepScrollAndMouseWheelEvents();
    prepLoadEvents();
    prepWindowEvents();

    // The following are not testable or not supported in all browsers
    // onlosecapture

    return layout;
  }

  @Override
  public String getInstructions() {
    return "Use the Widgets below to verify that all events fire in all "
        + "browsers.  To test an event, perform the action require to trigger "
        + "the event using the provided widget.";
  }

  @Override
  public String getSummary() {
    return "Events fire in current browsers";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

  /**
   * Add a test that is dependent on a previous test.
   * 
   * @param eventType the type of event defined in {@link Event}
   * @param eventName the name of the event
   * @return the index of the test
   */
  private int addDependentTest(int eventType, String eventName) {
    // Find the last test
    int numRows = layout.getRowCount();
    eventMap.put(new Integer(eventType), new Integer(numRows));
    for (int i = numRows - 1; i > 0; i--) {
      if (layout.getCellCount(i) == 3) {
        FlexCellFormatter formatter = layout.getFlexCellFormatter();
        int rowSpan = formatter.getRowSpan(i, 0);
        formatter.setRowSpan(i, 0, rowSpan + 1);
        break;
      }
    }
    layout.setText(numRows, 0, eventName);
    layout.setText(numRows, 1, "?");
    return numRows;
  }

  /**
   * Add a new test that tests one or more events.
   * 
   * @param eventType the type of event defined in {@link Event}
   * @param eventName the name of the event
   * @param trigger the widget that triggers the events
   * @return the index of the test
   */
  private int addTest(int eventType, String eventName, Widget trigger) {
    int numRows = layout.getRowCount();
    eventMap.put(new Integer(eventType), new Integer(numRows));
    layout.setWidget(numRows, 0, trigger);
    layout.setText(numRows, 1, eventName);
    layout.setText(numRows, 2, "?");

    FlexCellFormatter formatter = layout.getFlexCellFormatter();
    formatter.setVerticalAlignment(numRows, 0,
        HasVerticalAlignment.ALIGN_MIDDLE);
    formatter.setHorizontalAlignment(numRows, 0,
        HasHorizontalAlignment.ALIGN_CENTER);
    return numRows;
  }

  /**
   * Mark the event as passed.
   * 
   * @param event the event that was triggered
   */
  private void passTest(NativeEvent event) {
    passTest(Event.as(event).getTypeInt());
  }

  /**
   * Mark the event as passed.
   * 
   * @param eventType the event type that was triggered
   */
  private void passTest(int eventType) {
    int rowIndex = eventMap.get(new Integer(eventType));
    if (layout.getCellCount(rowIndex) == 3) {
      layout.setHTML(rowIndex, 2, "pass");
    } else {
      layout.setHTML(rowIndex, 1, "pass");
    }
  }

  private void prepKeyboardEvents() {
    // Setup a text box to trigger the events
    CustomTextBox textBox = new CustomTextBox();

    // Setup the tests
    textBox.setText("Type a letter");
    addTest(Event.ONKEYDOWN, "keydown", textBox);
    addDependentTest(Event.ONKEYPRESS, "keypress");
    addDependentTest(Event.ONKEYUP, "keyup");
    addDependentTest(Event.ONFOCUS, "focus");
    addDependentTest(Event.ONBLUR, "blur");
    addDependentTest(Event.ONCHANGE, "change");

    // Add event handlers
    textBox.addKeyDownHandler(new KeyDownHandler() {
      public void onKeyDown(KeyDownEvent event) {

        event.isAltKeyDown();
        event.isControlKeyDown();
        event.isShiftKeyDown();
        event.isMetaKeyDown();
        assert event.getNativeKeyCode() > 0;
        passTest(event.getNativeEvent());
      }
    });
    textBox.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        event.isAltKeyDown();
        event.isControlKeyDown();
        event.isShiftKeyDown();
        event.isMetaKeyDown();
        assert event.getNativeKeyCode() > 0;
        passTest(event.getNativeEvent());
      }
    });
    textBox.addKeyPressHandler(new KeyPressHandler() {
      public void onKeyPress(KeyPressEvent event) {
        event.isAltKeyDown();
        event.isControlKeyDown();
        event.isShiftKeyDown();
        event.isMetaKeyDown();
        assert event.getCharCode() > 0;
        passTest(event.getNativeEvent());
      }
    });
    textBox.addFocusHandler(new FocusHandler() {
      public void onFocus(FocusEvent event) {
        passTest(event.getNativeEvent());
      }
    });
    textBox.addBlurHandler(new BlurHandler() {
      public void onBlur(BlurEvent event) {
        passTest(event.getNativeEvent());
      }
    });
    textBox.addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        passTest(event.getNativeEvent());
      }
    });
  }

  private void prepLoadEvents() {
    // Create an image to trigger events
    final CustomImage loadable = new CustomImage();

    // Setup the tests
    addTest(Event.ONERROR, "error", loadable);
    addDependentTest(Event.ONLOAD, "load");
    addDependentTest(Event.ONCONTEXTMENU, "contextMenu");
    addDependentTest(Event.ONDBLCLICK, "dblclick");

    // Add the handlers
    loadable.addErrorHandler(new ErrorHandler() {
      public void onError(ErrorEvent event) {
        loadable.setUrl("issues/images/gwtLogo.png");
        passTest(event.getNativeEvent());
      }
    });
    loadable.addLoadHandler(new LoadHandler() {
      public void onLoad(LoadEvent event) {
        passTest(event.getNativeEvent());
      }
    });
    loadable.addDoubleClickHandler(new DoubleClickHandler() {
      public void onDoubleClick(DoubleClickEvent event) {
        passTest(event.getNativeEvent());
      }
    });
    loadable.addContextMenuHandler(new ContextMenuHandler() {
      public void onContextMenu(ContextMenuEvent event) {
        passTest(event.getNativeEvent());
      }
    });

    // Trigger the events
    loadable.setUrl("imageDoesNotExist.abc");
  }

  private void prepMouseEvents() {
    // Create a button to trigger events
    final Button button = new Button("Click me, move over me") {
      @Override
      public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);

        // Verify that values associated with events are defined. For some
        // values, we just want to make sure we can get them without any
        // errors.
        assert event.getClientX() > 0;
        assert event.getClientY() > 0;
        assert event.getScreenX() > 0;
        assert event.getScreenY() > 0;
        event.getAltKey();
        event.getCtrlKey();
        event.getShiftKey();
        event.getMetaKey();
      }
    };

    // Setup the tests
    addTest(Event.ONCLICK, "click", button);
    addDependentTest(Event.ONMOUSEDOWN, "mousedown");
    addDependentTest(Event.ONMOUSEUP, "mouseup");
    addDependentTest(Event.ONMOUSEOVER, "mouseover");
    addDependentTest(Event.ONMOUSEOUT, "mouseout");
    addDependentTest(Event.ONMOUSEMOVE, "mousemove");

    // Add event handlers
    button.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        passTest(event.getNativeEvent());
      }
    });
    button.addMouseDownHandler(new MouseDownHandler() {
      public void onMouseDown(MouseDownEvent event) {
        event.getNativeButton();
        passTest(event.getNativeEvent());
      }
    });
    button.addMouseUpHandler(new MouseUpHandler() {
      public void onMouseUp(MouseUpEvent event) {
        event.getNativeButton();
        passTest(event.getNativeEvent());
      }
    });
    button.addMouseMoveHandler(new MouseMoveHandler() {
      public void onMouseMove(MouseMoveEvent event) {
        passTest(event.getNativeEvent());
      }
    });
    button.addMouseOutHandler(new MouseOutHandler() {
      public void onMouseOut(MouseOutEvent event) {
        NativeEvent nativeEvent = event.getNativeEvent();
        assert nativeEvent.getRelatedEventTarget() != null;
        assert nativeEvent.getEventTarget() != null;
        if (button.getElement().equals(nativeEvent.getEventTarget())
            && button.getElement().getParentElement().equals(
                nativeEvent.getRelatedEventTarget())) {
          passTest(nativeEvent);
        }
      }
    });
    button.addMouseOverHandler(new MouseOverHandler() {
      public void onMouseOver(MouseOverEvent event) {
        NativeEvent nativeEvent = event.getNativeEvent();
        assert nativeEvent.getRelatedEventTarget() != null;
        assert nativeEvent.getEventTarget() != null;
        if (button.getElement().equals(nativeEvent.getEventTarget())
            && button.getElement().getParentElement().equals(
                nativeEvent.getRelatedEventTarget())) {
          passTest(nativeEvent);
        }
      }
    });
  }

  private void prepScrollAndMouseWheelEvents() {
    // Create a widget to trigger events
    String scrollableMessage = "Scroll to the bottom<br>(using mouse wheel<br>"
        + "if supported)";
    HTML scrollableContents = new HTML(scrollableMessage);
    scrollableContents.setPixelSize(400, 400);
    scrollableContents.getElement().getStyle().setProperty("textAlign", "left");
    CustomScrollPanel scrollable = new CustomScrollPanel(scrollableContents);

    // Setup the tests
    scrollable.setAlwaysShowScrollBars(true);
    scrollable.setPixelSize(200, 100);
    addTest(Event.ONSCROLL, "scroll", scrollable);
    addDependentTest(Event.ONMOUSEWHEEL, "mousewheel");

    // Display the mouse wheel velocity
    final int velocityIndex = addDependentTest(VELOCITY_EVENT, "velocityY");

    // Add event handlers
    scrollable.addScrollHandler(new ScrollHandler() {
      public void onScroll(ScrollEvent event) {
        passTest(event.getNativeEvent());
      }
    });
    scrollable.addMouseWheelHandler(new MouseWheelHandler() {
      public void onMouseWheel(MouseWheelEvent event) {
        event.getClientX();
        event.getClientY();
        event.getScreenX();
        event.getScreenY();
        int velocityY = event.getDeltaY();
        layout.setText(velocityIndex, 1, velocityY + "");
        passTest(event.getNativeEvent());
      }
    });
  }

  private void prepWindowEvents() {
    Label windowLabel = new Label("Window level events");

    // Setup the tests
    addTest(WINDOW_EVENT_SCROLL, "window.onscroll", windowLabel);
    addDependentTest(WINDOW_EVENT_RESIZE, "window.onresize");
    addDependentTest(WINDOW_EVENT_CLOSING, "window.onbeforeunload");

    // Add event handlers
    Window.addWindowScrollHandler(new Window.ScrollHandler() {
      public void onWindowScroll(Window.ScrollEvent event) {
        passTest(WINDOW_EVENT_SCROLL);
      }
    });
    Window.addResizeHandler(new ResizeHandler() {
      public void onResize(ResizeEvent event) {
        passTest(WINDOW_EVENT_RESIZE);
      }
    });
    Window.addWindowClosingHandler(new Window.ClosingHandler() {
      public void onWindowClosing(Window.ClosingEvent event) {
        event.setMessage("Stay and verify that window.onbeforeunload() has passed");
        passTest(WINDOW_EVENT_CLOSING);
      }
    });
  }
}
