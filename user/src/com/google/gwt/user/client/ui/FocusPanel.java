/*
 * Copyright 2006 Google Inc.
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
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.impl.FocusImpl;

/**
 * A simple panel that makes its contents focusable, and adds the ability to
 * catch mouse and keyboard events.
 */
public class FocusPanel extends SimplePanel implements HasFocus,
    SourcesClickEvents, SourcesMouseEvents {

  static final FocusImpl impl = (FocusImpl) GWT.create(FocusImpl.class);

  private ClickListenerCollection clickListeners;
  private FocusListenerCollection focusListeners;
  private KeyboardListenerCollection keyboardListeners;
  private MouseListenerCollection mouseListeners;

  public FocusPanel() {
    super(impl.createFocusable());
    sinkEvents(Event.FOCUSEVENTS | Event.KEYEVENTS | Event.ONCLICK
      | Event.MOUSEEVENTS);
  }

  public FocusPanel(Widget child) {
    this();
    setWidget(child);
  }

  public void addClickListener(ClickListener listener) {
    if (clickListeners == null) {
      clickListeners = new ClickListenerCollection();
    }
    clickListeners.add(listener);
  }

  public void addFocusListener(FocusListener listener) {
    if (focusListeners == null) {
      focusListeners = new FocusListenerCollection();
    }
    focusListeners.add(listener);
  }

  public void addKeyboardListener(KeyboardListener listener) {
    if (keyboardListeners == null) {
      keyboardListeners = new KeyboardListenerCollection();
    }
    keyboardListeners.add(listener);
  }

  public void addMouseListener(MouseListener listener) {
    if (mouseListeners == null) {
      mouseListeners = new MouseListenerCollection();
    }
    mouseListeners.add(listener);
  }

  public int getTabIndex() {
    return impl.getTabIndex(getElement());
  }

  public void onBrowserEvent(Event event) {
    switch (DOM.eventGetType(event)) {
      case Event.ONCLICK:
        if (clickListeners != null) {
          clickListeners.fireClick(this);
        }
        break;

      case Event.ONMOUSEDOWN:
      case Event.ONMOUSEUP:
      case Event.ONMOUSEMOVE:
      case Event.ONMOUSEOVER:
      case Event.ONMOUSEOUT:
        if (mouseListeners != null) {
          mouseListeners.fireMouseEvent(this, event);
        }
        break;

      case Event.ONBLUR:
      case Event.ONFOCUS:
        if (focusListeners != null) {
          focusListeners.fireFocusEvent(this, event);
        }
        break;

      case Event.ONKEYDOWN:
      case Event.ONKEYUP:
      case Event.ONKEYPRESS:
        if (keyboardListeners != null) {
          keyboardListeners.fireKeyboardEvent(this, event);
        }
        break;
    }
  }

  public void removeClickListener(ClickListener listener) {
    if (clickListeners != null) {
      clickListeners.remove(listener);
    }
  }

  public void removeFocusListener(FocusListener listener) {
    if (focusListeners != null) {
      focusListeners.remove(listener);
    }
  }

  public void removeKeyboardListener(KeyboardListener listener) {
    if (keyboardListeners != null) {
      keyboardListeners.remove(listener);
    }
  }

  public void removeMouseListener(MouseListener listener) {
    if (mouseListeners != null) {
      mouseListeners.remove(listener);
    }
  }

  public void setAccessKey(char key) {
    impl.setAccessKey(getElement(), key);
  }

  public void setFocus(boolean focused) {
    if (focused) {
      impl.focus(getElement());
    } else {
      impl.blur(getElement());
    }
  }

  public void setTabIndex(int index) {
    impl.setTabIndex(getElement(), index);
  }
}
