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

package com.google.gwt.user.client;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.GwtEvent.Type;

import java.util.EventListener;

/**
 * Legacy listener support hierarchy for <code>com.google.gwt.user.client</code>.
 * Gathers the bulk of the legacy glue code in one place, for easy deletion when
 * Listener methods are deleted. 
 * 
 * @see com.google.gwt.user.ListenerWrapper
 * @param <T> listener type
 */
@Deprecated
abstract class ListenerWrapper<T> implements EventHandler {
  public static class HistoryChange extends ListenerWrapper<HistoryListener> implements
      ValueChangeHandler<String> {
    @Deprecated
    public static void add(HistoryListener listener) {
      History.addValueChangeHandler(new HistoryChange(listener));
    }

    public static void remove(HandlerManager manager, HistoryListener listener) {
      baseRemove(manager, listener, ValueChangeEvent.getType());
    }

    private HistoryChange(HistoryListener listener) {
      super(listener);
    }

    public void onValueChange(ValueChangeEvent<String> event) {
      listener.onHistoryChanged(event.getValue());
    }
  }

  public static class WindowClose extends ListenerWrapper<WindowCloseListener> implements
      Window.ClosingHandler, CloseHandler<Window> {
    @Deprecated
    public static void add(WindowCloseListener listener) {
      WindowClose handler = new WindowClose(listener);
      Window.addWindowClosingHandler(handler);
      Window.addCloseHandler(handler);
    }

    public static void remove(HandlerManager manager,
        WindowCloseListener listener) {
      baseRemove(manager, listener, Window.ClosingEvent.getType(),
          CloseEvent.getType());
    }

    private WindowClose(WindowCloseListener listener) {
      super(listener);
    }

    public void onClose(CloseEvent<Window> event) {
      listener.onWindowClosed();
    }

    public void onWindowClosing(Window.ClosingEvent event) {
      String message = listener.onWindowClosing();
      if (event.getMessage() == null) {
        event.setMessage(message);
      }
    }
  }

  public static class WindowResize extends ListenerWrapper<WindowResizeListener> implements
      ResizeHandler {
    @Deprecated
    public static void add(WindowResizeListener listener) {
      Window.addResizeHandler(new WindowResize(listener));
    }

    public static void remove(HandlerManager manager,
        WindowResizeListener listener) {
      baseRemove(manager, listener, ResizeEvent.getType());
    }

    private WindowResize(WindowResizeListener listener) {
      super(listener);
    }

    public void onResize(ResizeEvent event) {
      listener.onWindowResized(event.getWidth(), event.getHeight());
    }
  }

  public static class WindowScroll extends ListenerWrapper<WindowScrollListener> implements
      Window.ScrollHandler {
    @Deprecated
    public static void add(WindowScrollListener listener) {
      Window.addWindowScrollHandler(new WindowScroll(listener));
    }

    public static void remove(HandlerManager manager,
        WindowScrollListener listener) {
      baseRemove(manager, listener, Window.ScrollEvent.getType());
    }

    private WindowScroll(WindowScrollListener listener) {
      super(listener);
    }

    public void onWindowScroll(Window.ScrollEvent event) {
      listener.onWindowScrolled(event.getScrollLeft(), event.getScrollTop());
    }
  }


  // This is an internal helper method with the current formulation, we have
  // lost the info needed to make it safe by this point.
  @SuppressWarnings("unchecked")
  // This is a direct copy of the baseRemove from
  // com.google.gwt.user.client.ui.ListenerWrapper. Change in parallel.
  static <H extends EventHandler> void baseRemove(HandlerManager manager,
      EventListener listener, Type... keys) {
    if (manager != null) {
      for (Type<H> key : keys) {
        int handlerCount = manager.getHandlerCount(key);
        // We are removing things as we traverse, have to go backward
        for (int i = handlerCount - 1; i >= 0; i--) {
          H handler = manager.getHandler(key, i);
          if (handler instanceof ListenerWrapper
              && ((ListenerWrapper) handler).listener.equals(listener)) {
            manager.removeHandler(key, handler);
          }
        }
      }
    }
  }
  
  /**
   * Listener being wrapped.
   */
  protected final T listener;

  protected ListenerWrapper(T listener) {
    this.listener = listener;
  }
}
