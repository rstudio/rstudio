/*
 * Copyright 2007 Google Inc.
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
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventPreview;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.impl.PopupImpl;

/**
 * A panel that can "pop up" over other widgets. It overlays the browser's
 * client area (and any previously-created popups).
 * 
 * <p>
 * <img class='gallery' src='PopupPanel.png'/>
 * </p>
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.PopupPanelExample}
 * </p>
 */
public class PopupPanel extends SimplePanel implements SourcesPopupEvents,
    EventPreview {

  private static final PopupImpl impl = (PopupImpl) GWT.create(PopupImpl.class);

  private boolean autoHide, modal, showing;
  private PopupListenerCollection popupListeners;

  /**
   * Creates an empty popup panel. A child widget must be added to it before it
   * is shown.
   */
  public PopupPanel() {
    super(impl.createElement());
    DOM.setStyleAttribute(getElement(), "position", "absolute");
  }

  /**
   * Creates an empty popup panel, specifying its "auto-hide" property.
   * 
   * @param autoHide <code>true</code> if the popup should be automatically
   *          hidden when the user clicks outside of it
   */
  public PopupPanel(boolean autoHide) {
    this();
    this.autoHide = autoHide;
  }

  /**
   * Creates an empty popup panel, specifying its "auto-hide" property.
   * 
   * @param autoHide <code>true</code> if the popup should be automatically
   *          hidden when the user clicks outside of it
   * @param modal <code>true</code> if keyboard or mouse events that do not
   *          target the PopupPanel or its children should be ignored
   */
  public PopupPanel(boolean autoHide, boolean modal) {
    this(autoHide);
    this.modal = modal;
  }

  public void addPopupListener(PopupListener listener) {
    if (popupListeners == null) {
      popupListeners = new PopupListenerCollection();
    }
    popupListeners.add(listener);
  }

  /**
   * Centers the popup in the browser window.
   * 
   * <p>
   * Note that the popup must be shown before this method is called.
   * </p>
   */
  public void center() {
    // Centering will not work properly until the panel is shown, because it
    // cannot be measured until it is attached to the DOM.
    if (!showing) {
      throw new IllegalStateException("PopupPanel must be shown before it may "
          + "be centered.");
    }

    int left = (Window.getClientWidth() - getOffsetWidth()) / 2;
    int top = (Window.getClientHeight() - getOffsetHeight()) / 2;
    setPopupPosition(Window.getScrollLeft() + left, Window.getScrollTop() + top);
  }

  /**
   * Gets the popup's left position relative to the browser's client area.
   * 
   * @return the popup's left position
   */
  public int getPopupLeft() {
    return DOM.getElementPropertyInt(getElement(), "offsetLeft");
  }

  /**
   * Gets the popup's top position relative to the browser's client area.
   * 
   * @return the popup's top position
   */
  public int getPopupTop() {
    return DOM.getElementPropertyInt(getElement(), "offsetTop");
  }

  /**
   * Hides the popup. This has no effect if it is not currently visible.
   */
  public void hide() {
    hide(false);
  }

  public boolean onEventPreview(Event event) {

    Element target = DOM.eventGetTarget(event);
    boolean eventTargetsPopup = DOM.isOrHasChild(getElement(), target);

    int type = DOM.eventGetType(event);
    switch (type) {
      case Event.ONKEYDOWN: {
        if (eventTargetsPopup) {
          return onKeyDownPreview((char) DOM.eventGetKeyCode(event),
              KeyboardListenerCollection.getKeyboardModifiers(event));
        } else {
          return !modal;
        }
      }
      case Event.ONKEYUP: {
        if (eventTargetsPopup) {
          return onKeyUpPreview((char) DOM.eventGetKeyCode(event),
              KeyboardListenerCollection.getKeyboardModifiers(event));
        } else {
          return !modal;
        }
      }
      case Event.ONKEYPRESS: {
        if (eventTargetsPopup) {
          return onKeyPressPreview((char) DOM.eventGetKeyCode(event),
              KeyboardListenerCollection.getKeyboardModifiers(event));
        } else {
          return !modal;
        }
      }

      case Event.ONMOUSEDOWN:
      case Event.ONMOUSEUP:
      case Event.ONMOUSEMOVE:
      case Event.ONCLICK:
      case Event.ONDBLCLICK: {
        // Don't eat events if event capture is enabled, as this can interfere
        // with dialog dragging, for example.
        if (DOM.getCaptureElement() != null) {
          return true;
        }

        // If it's an outside click and auto-hide is enabled:
        // hide the popup and _don't_ eat the event. ONMOUSEDOWN is used to
        // prevent problems with showing a popup in response to a mousedown.
        if (!eventTargetsPopup && autoHide && (type == Event.ONMOUSEDOWN)) {
          hide(true);
          return true;
        }

        break;
      }

      case Event.ONFOCUS: {
        if (modal && !eventTargetsPopup && (target != null)) {
          blur(target);
          return false;
        }
      }
    }

    return !modal || (modal && eventTargetsPopup);
  }

  /**
   * Popups get an opportunity to preview keyboard events before they are passed
   * to a widget contained by the Popup.
   * 
   * @param key the key code of the depressed key
   * @param modifiers keyboard modifiers, as specified in
   *          {@link KeyboardListener}.
   * @return <code>false</code> to suppress the event
   */
  public boolean onKeyDownPreview(char key, int modifiers) {
    return true;
  }

  /**
   * Popups get an opportunity to preview keyboard events before they are passed
   * to a widget contained by the Popup.
   * 
   * @param key the unicode character pressed
   * @param modifiers keyboard modifiers, as specified in
   *          {@link KeyboardListener}.
   * @return <code>false</code> to suppress the event
   */
  public boolean onKeyPressPreview(char key, int modifiers) {
    return true;
  }

  /**
   * Popups get an opportunity to preview keyboard events before they are passed
   * to a widget contained by the Popup.
   * 
   * @param key the key code of the released key
   * @param modifiers keyboard modifiers, as specified in
   *          {@link KeyboardListener}.
   * @return <code>false</code> to suppress the event
   */
  public boolean onKeyUpPreview(char key, int modifiers) {
    return true;
  }

  public boolean remove(Widget w) {
    if (!super.remove(w)) {
      return false;
    }
    return true;
  }

  public void removePopupListener(PopupListener listener) {
    if (popupListeners != null) {
      popupListeners.remove(listener);
    }
  }

  /**
   * Sets the popup's position relative to the browser's client area. The
   * popup's position may be set before calling {@link #show()}.
   * 
   * @param left the left position, in pixels
   * @param top the top position, in pixels
   */
  public void setPopupPosition(int left, int top) {
    // Keep the popup within the browser's client area, so that they can't get
    // 'lost' and become impossible to interact with. Note that we don't attempt
    // to keep popups pegged to the bottom and right edges, as they will then
    // cause scrollbars to appear, so the user can't lose them.
    if (left < 0) {
      left = 0;
    }
    if (top < 0) {
      top = 0;
    }

    // Set the popup's position manually, allowing setPopupPosition() to be
    // called before show() is called (so a popup can be positioned without it
    // 'jumping' on the screen).
    Element elem = getElement();
    DOM.setStyleAttribute(elem, "left", left + "px");
    DOM.setStyleAttribute(elem, "top", top + "px");
  }

  /**
   * Sets whether this object is visible.
   *
   * @param visible <code>true</code> to show the object, <code>false</code>
   *          to hide it
   */
  public void setVisible(boolean visible) {
    // We use visibility here instead of UIObject's default of display
    // Because the panel is absolutely positioned, this will not create
    // "holes" in displayed contents and it allows normal layout passes
    // to occur so the size of the PopupPanel can be reliably determined.
    DOM.setStyleAttribute(getElement(), "visibility",
        visible ? "visible" : "hidden");
    
    // If the PopupImpl creates an iframe shim, it's also necessary to hide it
    // as well.
    impl.setVisible(getElement(), visible);
  }

  /**
   * Shows the popup. It must have a child widget before this method is called.
   */
  public void show() {
    if (showing) {
      return;
    }
    showing = true;
    DOM.addEventPreview(this);

    RootPanel.get().add(this);
    impl.onShow(getElement());
  }

  /**
   * This method is called when a widget is detached from the browser's
   * document. To receive notification before the PopupPanel is removed from the
   * document, override the {@link Widget#onUnload()} method instead.
   */
  protected void onDetach() {
    DOM.removeEventPreview(this);
    super.onDetach();
  }

  /**
   * Remove focus from an Element.
   * 
   * @param elt The Element on which <code>blur()</code> will be invoked
   */
  private native void blur(Element elt) /*-{
    if (elt.blur) {
      elt.blur();
    }
  }-*/;

  private void hide(boolean autoClosed) {
    if (!showing) {
      return;
    }
    showing = false;

    RootPanel.get().remove(this);
    impl.onHide(getElement());
    if (popupListeners != null) {
      popupListeners.firePopupClosed(this, autoClosed);
    }
  }
}
