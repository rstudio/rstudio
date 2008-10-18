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

import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.shared.AbstractEvent;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlerManager;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

/**
 * The base class for the majority of user-interface objects. Widget adds
 * support for receiving events from the browser and being added directly to
 * {@link com.google.gwt.user.client.ui.Panel panels}.
 */
public class Widget extends UIObject implements EventListener,
    HasHandlerManager {

  private boolean attached;
  private Object layoutData;
  private Widget parent;

  private HandlerManager handlerManager;

  /**
   * Returns this widget's {@link HandlerManager} used for event management.
   * 
   * @return the handler manager
   */
  public final HandlerManager getHandlerManager() {
    if (handlerManager == null) {
      handlerManager = createHandlerManager();
    }
    return handlerManager;
  }

  /**
   * Gets this widget's parent panel.
   * 
   * @return the widget's parent panel
   */
  public Widget getParent() {
    return parent;
  }

  /**
   * Determines whether this widget is currently attached to the browser's
   * document (i.e., there is an unbroken chain of widgets between this widget
   * and the underlying browser document).
   * 
   * @return <code>true</code> if the widget is attached
   */
  public boolean isAttached() {
    return attached;
  }

  public void onBrowserEvent(Event nativeEvent) {
    if (handlerManager != null) {
      DomEvent.fireNativeEvent(nativeEvent, handlerManager);
    }
  }

  /**
   * Removes this widget from its parent widget. If it has no parent, this
   * method does nothing.
   * 
   * @throws IllegalStateException if this widget's parent does not support
   *           removal (e.g. {@link Composite})
   */
  public void removeFromParent() {
    if (parent instanceof HasWidgets) {
      ((HasWidgets) parent).remove(this);
    } else if (parent != null) {
      throw new IllegalStateException(
          "This widget's parent does not implement HasWidgets");
    }
  }

  /**
   * Adds a native event handler to the widget and sinks the corresponding
   * native event. If you do not want to sink the native event, use the generic
   * addHandler method instead.
   * 
   * @param <HandlerType> the type of handler to add
   * @param type the event key
   * @param handler the handler
   * @return {@link HandlerRegistration} used to remove the handler
   */
  protected <HandlerType extends EventHandler> HandlerRegistration addDomHandler(
      DomEvent.Type<?, HandlerType> type, final HandlerType handler) {
    sinkEvents(type.getNativeEventTypeInt());
    return addHandler(type, handler);
  }

  /**
   * Adds this handler to the widget.
   * 
   * @param <HandlerType> the type of handler to add
   * @param type the event type
   * @param handler the handler
   * @return {@link HandlerRegistration} used to remove the handler
   */
  protected <HandlerType extends EventHandler> HandlerRegistration addHandler(
      AbstractEvent.Type<?, HandlerType> type, final HandlerType handler) {
    return getHandlerManager().addHandler(type, handler);
  }

  /**
   * Creates the {@link HandlerManager} used by this widget for event
   * management.
   * 
   * @return the handler manager
   * 
   */
  protected HandlerManager createHandlerManager() {
    return new HandlerManager(this);
  }

  /**
   * If a widget implements HasWidgets, it must override this method and call
   * onAttach() for each of its child widgets.
   * 
   * @see Panel#onAttach()
   */
  protected void doAttachChildren() {
  }

  /**
   * If a widget implements HasWidgets, it must override this method and call
   * onDetach() for each of its child widgets.
   * 
   * @see Panel#onDetach()
   */
  protected void doDetachChildren() {
  }

  /**
   * Fires an event.
   * 
   * @param event the event
   */
  protected void fireEvent(AbstractEvent event) {
    if (handlerManager != null) {
      handlerManager.fireEvent(event);
    }
  }

  /**
   * Is the event handled by one or more handlers?
   * 
   * @param key event type key
   * @return does this event type have a current handler
   */
  protected final boolean isEventHandled(AbstractEvent.Type key) {
    return handlerManager == null ? false : handlerManager.isEventHandled(key);
  }

  /**
   * This method is called when a widget is attached to the browser's document.
   * To receive notification after a Widget has been added to the document,
   * override the {@link #onLoad} method.
   * 
   * <p>
   * Subclasses that override this method must call
   * <code>super.onAttach()</code> to ensure that the Widget has been attached
   * to its underlying Element.
   * </p>
   * 
   * @throws IllegalStateException if this widget is already attached
   */
  protected void onAttach() {
    if (isAttached()) {
      throw new IllegalStateException(
          "Should only call onAttach when the widget is detached from the browser's document");
    }

    attached = true;
    DOM.setEventListener(getElement(), this);
    doAttachChildren();

    // onLoad() gets called only *after* all of the children are attached and
    // the attached flag is set. This allows widgets to be notified when they
    // are fully attached, and panels when all of their children are attached.
    onLoad();
  }

  /**
   * This method is called when a widget is detached from the browser's
   * document. To receive notification before a Widget is removed from the
   * document, override the {@link #onUnload} method.
   * 
   * <p>
   * Subclasses that override this method must call
   * <code>super.onDetach()</code> to ensure that the Widget has been detached
   * from the underlying Element. Failure to do so will result in application
   * memory leaks due to circular references between DOM Elements and JavaScript
   * objects.
   * </p>
   * 
   * @throws IllegalStateException if this widget is already detached
   */
  protected void onDetach() {
    if (!isAttached()) {
      throw new IllegalStateException(
          "Should only call onDetach when the widget is attached to the browser's document");
    }

    try {
      // onUnload() gets called *before* everything else (the opposite of
      // onLoad()).
      onUnload();
    } finally {
      // Put this in a finally, just in case onUnload throws an exception.
      doDetachChildren();
      DOM.setEventListener(getElement(), null);
      attached = false;
    }
  }

  /**
   * This method is called immediately after a widget becomes attached to the
   * browser's document.
   */
  protected void onLoad() {
  }

  /**
   * This method is called immediately before a widget will be detached from the
   * browser's document.
   */
  protected void onUnload() {
  }

  /**
   * Removes the given handler from the specified event key. Normally,
   * applications should call {@link HandlerRegistration#removeHandler()}
   * instead. This method is provided primary to support the deprecated
   * listeners api.
   * 
   * @param <HandlerType> handler type
   * 
   * @param key the event key
   * @param handler the handler
   */
  protected <HandlerType extends EventHandler> void removeHandler(
      AbstractEvent.Type<?, HandlerType> key, final HandlerType handler) {
    if (handlerManager == null) {
      handlerManager = new HandlerManager(this);
    }
    handlerManager.removeHandler(key, handler);
  }

  /**
   * Gets the panel-defined layout data associated with this widget.
   * 
   * @return the widget's layout data
   * @see #setLayoutData
   */
  Object getLayoutData() {
    return layoutData;
  }

  @Override
  void replaceElement(com.google.gwt.dom.client.Element elem) {
    if (isAttached()) {
      // Remove old event listener to avoid leaking. onDetach will not do this
      // for us, because it is only called when the widget itself is detached
      // from the document.
      DOM.setEventListener(getElement(), null);
    }

    super.replaceElement(elem);

    if (isAttached()) {
      // Hook the event listener back up on the new element. onAttach will not
      // do this for us, because it is only called when the widget itself is
      // attached to the document.
      DOM.setEventListener(getElement(), this);
    }
  }

  /**
   * Sets the panel-defined layout data associated with this widget. Only the
   * panel that currently contains a widget should ever set this value. It
   * serves as a place to store layout bookkeeping data associated with a
   * widget.
   * 
   * @param layoutData the widget's layout data
   */
  void setLayoutData(Object layoutData) {
    this.layoutData = layoutData;
  }

  /**
   * Sets this widget's parent. This method should only be called by
   * {@link Panel} and {@link Composite}.
   * 
   * @param parent the widget's new parent
   * @throws IllegalStateException if <code>parent</code> is non-null and the
   *           widget already has a parent
   */
  void setParent(Widget parent) {
    Widget oldParent = this.parent;
    if (parent == null) {
      if (oldParent != null && oldParent.isAttached()) {
        onDetach();
        assert !isAttached() : "Failure of " + this.getClass().getName()
            + " to call super.onDetach()";
      }
      this.parent = null;
    } else {
      if (oldParent != null) {
        throw new IllegalStateException(
            "Cannot set a new parent without first clearing the old parent");
      }
      this.parent = parent;
      if (parent.isAttached()) {
        onAttach();
        assert isAttached() : "Failure of " + this.getClass().getName()
            + " to call super.onAttach()";
      }
    }
  }
}
