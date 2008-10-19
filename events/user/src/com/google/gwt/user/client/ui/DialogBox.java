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
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;

/**
 * A form of popup that has a caption area at the top and can be dragged by the
 * user. Unlike a PopupPanel, calls to {@link #setWidth(String)} and
 * {@link #setHeight(String)} will set the width and height of the dialog box
 * itself, even if a widget has not been added as yet.
 * <p>
 * <img class='gallery' src='DialogBox.png'/>
 * </p>
 * <h3>CSS Style Rules</h3> <ul class='css'> <li>.gwt-DialogBox { the outside of
 * the dialog }</li> <li>.gwt-DialogBox .Caption { the caption }</li> <li>
 * .gwt-DialogBox .dialogContent { the wrapepr around the content }</li> <li>
 * .gwt-DialogBox .dialogTopLeft { the top left cell }</li> <li>.gwt-DialogBox
 * .dialogTopLeftInner { the inner element of the cell }</li> <li>.gwt-DialogBox
 * .dialogTopCenter { the top center cell, where the caption is located }</li>
 * <li>.gwt-DialogBox .dialogTopCenterInner { the inner element of the cell }</li>
 * <li>.gwt-DialogBox .dialogTopRight { the top right cell }</li> <li>
 * .gwt-DialogBox .dialogTopRightInner { the inner element of the cell }</li>
 * <li>.gwt-DialogBox .dialogMiddleLeft { the middle left cell }</li> <li>
 * .gwt-DialogBox .dialogMiddleLeftInner { the inner element of the cell }</li>
 * <li>.gwt-DialogBox .dialogMiddleCenter { the middle center cell, where the
 * content is located }</li> <li>.gwt-DialogBox .dialogMiddleCenterInner { the
 * inner element of the cell }</li> <li>.gwt-DialogBox .dialogMiddleRight { the
 * middle right cell }</li> <li>.gwt-DialogBox .dialogMiddleRightInner { the
 * inner element of the cell }</li> <li>.gwt-DialogBox .dialogBottomLeft { the
 * bottom left cell }</li> <li>.gwt-DialogBox .dialogBottomLeftInner { the inner
 * element of the cell }</li> <li>.gwt-DialogBox .dialogBottomCenter { the
 * bottom center cell }</li> <li>.gwt-DialogBox .dialogBottomCenterInner { the
 * inner element of the cell }</li> <li>.gwt-DialogBox .dialogBottomRight { the
 * bottom right cell }</li> <li>.gwt-DialogBox .dialogBottomRightInner { the
 * inner element of the cell }</li> </ul>
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.DialogBoxExample}
 * </p>
 */
public class DialogBox extends DecoratedPopupPanel implements HasHTML, HasText {

  private class MouseHandlers implements MouseDownHandler, MouseMoveHandler,
      MouseUpHandler {

    public void onMouseDown(MouseDownEvent event) {
      dragging = true;
      DOM.setCapture(getElement());
      dragStartX = event.getRelativeX(getElement());
      dragStartY = event.getRelativeY(getElement());
      windowWidth = Window.getClientWidth();
    }

    public void onMouseMove(MouseMoveEvent event) {
      if (dragging) {
        int absX = event.getRelativeX(getElement()) + getAbsoluteLeft();
        int absY = event.getRelativeY(getElement()) + getAbsoluteTop();

        // if the mouse is off the screen to the left, right, or top, don't
        // move the dialog box. This would let users lose dialog boxes, which
        // would be bad for modal popups.
        if (absX < clientLeft || absX >= windowWidth || absY < clientTop) {
          return;
        }

        setPopupPosition(absX - dragStartX, absY - dragStartY);
      }
    }

    public void onMouseUp(MouseUpEvent event) {
      dragging = false;
      DOM.releaseCapture(getElement());
    }
  }

  /**
   * The default style name.
   */
  private static final String DEFAULT_STYLENAME = "gwt-DialogBox";
  private HTML caption = new HTML();
  private boolean dragging;
  private int dragStartX, dragStartY;
  private int windowWidth;
  private int clientLeft;
  private int clientTop;

  /**
   * Creates an empty dialog box. It should not be shown until its child widget
   * has been added using {@link #add(Widget)}.
   */
  public DialogBox() {
    this(false);
  }

  /**
   * Creates an empty dialog box specifying its "auto-hide" property. It should
   * not be shown until its child widget has been added using
   * {@link #add(Widget)}.
   * 
   * @param autoHide <code>true</code> if the dialog should be automatically
   *          hidden when the user clicks outside of it
   */
  public DialogBox(boolean autoHide) {
    this(autoHide, true);
  }

  /**
   * Creates an empty dialog box specifying its "auto-hide" property. It should
   * not be shown until its child widget has been added using
   * {@link #add(Widget)}.
   * 
   * @param autoHide <code>true</code> if the dialog should be automatically
   *          hidden when the user clicks outside of it
   * @param modal <code>true</code> if keyboard and mouse events for widgets not
   *          contained by the dialog should be ignored
   */
  public DialogBox(boolean autoHide, boolean modal) {
    super(autoHide, modal, "dialog");

    // Add the caption to the top row of the decorator panel. We need to
    // logically adopt the caption so we can catch mouse events.
    Element td = getCellElement(0, 1);
    td.appendChild(caption.getElement());
    caption.setStyleName("Caption");

    // Set the style name
    setStyleName(DEFAULT_STYLENAME);

    // Sink the events
    sinkEvents(Event.MOUSEEVENTS);

    MouseHandlers mouse = new MouseHandlers();
    addDomHandler(MouseDownEvent.TYPE, mouse);
    addDomHandler(MouseUpEvent.TYPE, mouse);
    addDomHandler(MouseMoveEvent.TYPE, mouse);
    windowWidth = Window.getClientWidth();
    clientLeft = Document.get().getBodyOffsetLeft();
    clientTop = Document.get().getBodyOffsetTop();
  }

  public String getHTML() {
    return caption.getHTML();
  }

  public String getText() {
    return caption.getText();
  }

  @Override
  public boolean onEventPreview(Event event) {
    // When dragging ignore all other events.
    if (dragging) {
      // While dragging, all normal operations should be suspended.
      DOM.eventPreventDefault(event);
      return true;
    } else {
      return super.onEventPreview(event);
    }
  }

  /**
   * Sets the html string inside the caption.
   * 
   * Use {@link #setWidget(Widget)} to set the contents inside the
   * {@link DialogBox}.
   * 
   * @param html the object's new HTML
   */
  public void setHTML(String html) {
    caption.setHTML(html);
  }

  /**
   * Sets the text inside the caption.
   * 
   * Use {@link #setWidget(Widget)} to set the contents inside the
   * {@link DialogBox}.
   * 
   * @param text the object's new text
   */
  public void setText(String text) {
    caption.setText(text);
  }

  /**
   * <b>Affected Elements:</b>
   * <ul>
   * <li>-caption = text at the top of the {@link DialogBox}.</li>
   * <li>-content = the container around the content.</li>
   * </ul>
   * 
   * @see UIObject#onEnsureDebugId(String)
   */
  @Override
  protected void onEnsureDebugId(String baseID) {
    super.onEnsureDebugId(baseID);
    caption.ensureDebugId(baseID + "-caption");
    ensureDebugId(getCellElement(1, 1), baseID, "content");
  }
}
