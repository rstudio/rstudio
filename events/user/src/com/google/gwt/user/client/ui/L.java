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

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasAllFocusHandlers;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasMouseDownHandlers;
import com.google.gwt.event.dom.client.HasMouseMoveHandlers;
import com.google.gwt.event.dom.client.HasMouseOutHandlers;
import com.google.gwt.event.dom.client.HasMouseOverHandlers;
import com.google.gwt.event.dom.client.HasMouseUpHandlers;
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
import com.google.gwt.event.logical.shared.HideEvent;
import com.google.gwt.event.logical.shared.HideHandler;
import com.google.gwt.event.shared.AbstractEvent;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HasHandlerManager;
import com.google.gwt.event.shared.AbstractEvent.Type;

import java.util.EventListener;

/**
 * Root of legacy listener support hierarchy.
 * 
 * @param <ListenerType> listener type
 * 
 */
@Deprecated
abstract class L<ListenerType> implements EventHandler {

  public static class Change extends L<ChangeListener> implements ChangeHandler {
    @Deprecated
    public static <EventSourceType extends HasHandlerManager & HasChangeHandlers> void add(
        EventSourceType source, ChangeListener listener) {
      source.addChangeHandler(new Change(listener));
    }

    public static void remove(HasHandlerManager eventSource,
        ChangeListener listener) {
      baseRemove(eventSource, listener, ChangeEvent.TYPE);
    }

    protected Change(ChangeListener listener) {
      super(listener);
    }

    public void onChange(ChangeEvent event) {
      listener.onChange(source(event));
    }
  }
  public static class Click extends L<ClickListener> implements ClickHandler {
    @Deprecated
    public static <EventSourceType extends HasHandlerManager & HasClickHandlers> void add(
        EventSourceType source, ClickListener listener) {
      source.addClickHandler(new Click(listener));
    }

    public static void remove(HasHandlerManager eventSource,
        ClickListener listener) {
      baseRemove(eventSource, listener, ClickEvent.TYPE);
    }

    protected Click(ClickListener listener) {
      super(listener);
    }

    public void onClick(ClickEvent event) {
      listener.onClick(source(event));
    }
  }

  /*
   * Handler wrapper for {@link FocusListener}.
   */
  public static class Focus extends L<FocusListener> implements FocusHandler,
      BlurHandler {

    public static <EventSourceType extends HasHandlerManager & HasAllFocusHandlers> void add(
        EventSourceType source, FocusListener listener) {
      HasAllFocusHandlers.Adaptor.addHandlers(source, new Focus(listener));
    }

    public static void remove(HasHandlerManager eventSource,
        FocusListener listener) {
      baseRemove(eventSource, listener, LoadEvent.TYPE, ErrorEvent.TYPE);
    }

    public Focus(FocusListener listener) {
      super(listener);
    }

    public void onBlur(BlurEvent event) {
      listener.onLostFocus(source(event));
    }

    public void onFocus(FocusEvent event) {
      listener.onFocus(source(event));
    }
  }

  public abstract static class Hide extends L<EventListener> implements
      HideHandler {

    public static void remove(HasHandlerManager eventSource,
        EventListener listener) {
      baseRemove(eventSource, listener, HideEvent.TYPE);
    }

    protected Hide(EventListener listener) {
      super(listener);
    }
  }

  public static class Load extends L<LoadListener> implements LoadHandler,
      ErrorHandler {

    public static void remove(HasHandlerManager eventSource,
        LoadListener listener) {
      baseRemove(eventSource, listener, LoadEvent.TYPE, ErrorEvent.TYPE);
    }

    protected Load(LoadListener listener) {
      super(listener);
    }

    public void onError(ErrorEvent event) {
      listener.onError(source(event));
    }

    public void onLoad(LoadEvent event) {
      listener.onLoad(source(event));
    }
  }

  public static class Mouse extends L<MouseListener> implements
      MouseDownHandler, MouseUpHandler, MouseOutHandler, MouseOverHandler,
      MouseMoveHandler {

    public static <EventSourceType extends HasHandlerManager & HasMouseDownHandlers & HasMouseUpHandlers & HasMouseOutHandlers & HasMouseOverHandlers & HasMouseMoveHandlers> void add(
        EventSourceType source, MouseListener listener) {
      Mouse handlers = new Mouse(listener);
      source.addMouseDownHandler(handlers);
      source.addMouseUpHandler(handlers);
      source.addMouseOutHandler(handlers);
      source.addMouseOverHandler(handlers);
      source.addMouseMoveHandler(handlers);
    }

    public static void remove(HasHandlerManager eventSource,
        MouseListener listener) {
      baseRemove(eventSource, listener, MouseDownEvent.TYPE, MouseUpEvent.TYPE,
          MouseOverEvent.TYPE, MouseOutEvent.TYPE);
    }

    protected Mouse(MouseListener listener) {
      super(listener);
    }

    public void onMouseDown(MouseDownEvent event) {
      listener.onMouseDown(source(event), event.getClientX(),
          event.getScreenY());
    }

    public void onMouseMove(MouseMoveEvent event) {
      listener.onMouseMove(source(event), event.getClientX(),
          event.getClientY());
    }

    public void onMouseOut(MouseOutEvent event) {
      listener.onMouseLeave(source(event));
    }

    public void onMouseOver(MouseOverEvent event) {
      listener.onMouseEnter(source(event));
    }

    public void onMouseUp(MouseUpEvent event) {
      listener.onMouseUp(source(event), event.getClientX(), event.getClientY());
    }
  }

  public static class MouseWheel extends L<MouseWheelListener> implements
      MouseWheelHandler {
    public static void remove(HasHandlerManager eventSource,
        MouseWheelListener listener) {
      baseRemove(eventSource, listener, MouseWheelEvent.TYPE);
    }

    protected MouseWheel(MouseWheelListener listener) {
      super(listener);
    }

    public void onMouseWheel(MouseWheelEvent event) {
      listener.onMouseWheel(source(event), new MouseWheelVelocity(
          event.getNativeEvent()));
    }
  }

  public static class Scroll extends L<ScrollListener> implements ScrollHandler {

    public static void remove(HasHandlerManager eventSource,
        ScrollListener listener) {
      baseRemove(eventSource, listener, ScrollEvent.TYPE, ErrorEvent.TYPE);
    }

    protected Scroll(ScrollListener listener) {
      super(listener);
    }

    public void onScroll(ScrollEvent event) {
      Widget source = source(event);
      Element elem = source.getElement();
      listener.onScroll(source(event), elem.getScrollLeft(),
          elem.getScrollTop());
    }
  }

  static class Keyboard extends L<KeyboardListener> implements KeyDownHandler,
      KeyUpHandler, KeyPressHandler {

    public static <EventSourceType extends HasHandlerManager & HasAllKeyHandlers> void add(
        EventSourceType source, KeyboardListener listener) {
      HasAllKeyHandlers.Adaptor.addHandlers(source, new Keyboard(listener));
    }

    public static void remove(HasHandlerManager eventSource,
        KeyboardListener listener) {
      L.baseRemove(eventSource, listener, KeyDownEvent.TYPE, KeyUpEvent.TYPE,
          KeyPressEvent.TYPE);
    }

    public Keyboard(KeyboardListener listener) {
      super(listener);
    }

    public void onKeyDown(KeyDownEvent event) {
      listener.onKeyDown(source(event), (char) event.getKeyCode(),
          event.getKeyModifiers());
    }

    public void onKeyPress(KeyPressEvent event) {
      listener.onKeyPress(source(event),
          (char) event.getNativeEvent().getKeyCode(), event.getKeyModifiers());
    }

    public void onKeyUp(KeyUpEvent event) {
      source(event);
      listener.onKeyUp(source(event), (char) event.getKeyCode(),
          event.getKeyModifiers());
    }
  }

  static void baseRemove(HasHandlerManager eventSource, EventListener listener,
      Type... keys) {
    HandlerManager manager = eventSource.getHandlerManager();
    for (Type key : keys) {
      int handlerCount = manager.getHandlerCount(key);
      for (int i = 0; i < handlerCount; i++) {
        EventHandler handler = manager.getHandler(key, i);
        if (handler instanceof L && ((L) handler).listener.equals(listener)) {
          manager.removeHandler(key, handler);
        }
      }
    }
  }

  /**
   * Listener being wrapped.
   */
  protected final ListenerType listener;

  protected L(ListenerType listener) {
    this.listener = listener;
  }

  Widget source(AbstractEvent event) {
    return (Widget) event.getSource();
  }
}
