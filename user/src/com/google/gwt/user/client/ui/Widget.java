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
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.event.logical.shared.HasAttachHandlers;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

/**
 * The base class for the majority of user-interface objects. Widget adds
 * support for receiving events from the browser and being added directly to
 * {@link com.google.gwt.user.client.ui.Panel panels}.
 */
public class Widget extends UIObject implements EventListener, HasAttachHandlers,
    IsWidget {

  /**
   * This convenience method makes a null-safe call to
   * {@link IsWidget#asWidget()}.
   *
   * @return the widget aspect, or <code>null</code> if w is null
   */
  public static Widget asWidgetOrNull(IsWidget w) {
    return w == null ? null : w.asWidget();
  }
  /**
   * A bit-map of the events that should be sunk when the widget is attached to
   * the DOM. (We delay the sinking of events to improve startup performance.)
   * When the widget is attached, this is set to -1
   * <p>
   * Package protected to allow Composite to see it.
   */
  int eventsToSink;
  private boolean attached;
  private HandlerManager handlerManager;
  private Object layoutData;
  private Widget parent;

  public HandlerRegistration addAttachHandler(Handler handler) {
    return addHandler(handler, AttachEvent.getType());
  }

  /**
   * For <a href=
   * "http://code.google.com/p/google-web-toolkit/wiki/UnderstandingMemoryLeaks"
   * >browsers which do not leak</a>, adds a native event handler to the widget.
   * Note that, unlike the
   * {@link #addDomHandler(EventHandler, com.google.gwt.event.dom.client.DomEvent.Type)}
   * implementation, there is no need to attach the widget to the DOM in order
   * to cause the event handlers to be attached.
   *
   * @param <H> the type of handler to add
   * @param type the event key
   * @param handler the handler
   * @return {@link HandlerRegistration} used to remove the handler
   */
  public final <H extends EventHandler> HandlerRegistration addBitlessDomHandler(
      final H handler, DomEvent.Type<H> type) {
    assert handler != null : "handler must not be null";
    assert type != null : "type must not be null";
    sinkBitlessEvent(type.getName());
    return ensureHandlers().addHandler(type, handler);
  }

  /**
   * Adds a native event handler to the widget and sinks the corresponding
   * native event. If you do not want to sink the native event, use the generic
   * addHandler method instead.
   *
   * @param <H> the type of handler to add
   * @param type the event key
   * @param handler the handler
   * @return {@link HandlerRegistration} used to remove the handler
   */
  public final <H extends EventHandler> HandlerRegistration addDomHandler(
      final H handler, DomEvent.Type<H> type) {
    assert handler != null : "handler must not be null";
    assert type != null : "type must not be null";
    int typeInt = Event.getTypeInt(type.getName());
    if (typeInt == -1) {
      sinkBitlessEvent(type.getName());
    } else {
      sinkEvents(typeInt);
    }
    return ensureHandlers().addHandler(type, handler);
  }

  /**
   * Adds this handler to the widget.
   *
   * @param <H> the type of handler to add
   * @param type the event type
   * @param handler the handler
   * @return {@link HandlerRegistration} used to remove the handler
   */
  public final <H extends EventHandler> HandlerRegistration addHandler(
      final H handler, GwtEvent.Type<H> type) {
    return ensureHandlers().addHandler(type, handler);
  }

  public Widget asWidget() {
    return this;
  }

  public void fireEvent(GwtEvent<?> event) {
    if (handlerManager != null) {
      handlerManager.fireEvent(event);
    }
  }

  /**
   * Gets the panel-defined layout data associated with this widget.
   *
   * @return the widget's layout data
   * @see #setLayoutData
   */
  public Object getLayoutData() {
    return layoutData;
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

  public void onBrowserEvent(Event event) {
    switch (DOM.eventGetType(event)) {
      case Event.ONMOUSEOVER:
        // Only fire the mouse over event if it's coming from outside this
        // widget.
      case Event.ONMOUSEOUT:
        // Only fire the mouse out event if it's leaving this
        // widget.
        Element related = event.getRelatedEventTarget().cast();
        if (related != null && getElement().isOrHasChild(related)) {
          return;
        }
        break;
    }
    DomEvent.fireNativeEvent(event, this, this.getElement());
  }

  /**
   * Removes this widget from its parent widget, if one exists.
   *
   * <p>
   * If it has no parent, this method does nothing. If it is a "root" widget
   * (meaning it's been added to the detach list via
   * {@link RootPanel#detachOnWindowClose(Widget)}), it will be removed from the
   * detached immediately. This makes it possible for Composites and Panels to
   * adopt root widgets.
   * </p>
   *
   * @throws IllegalStateException if this widget's parent does not support
   *           removal (e.g. {@link Composite})
   */
  public void removeFromParent() {
    if (parent == null) {
      // If the widget had no parent, check to see if it was in the detach list
      // and remove it if necessary.
      if (RootPanel.isInDetachList(this)) {
        RootPanel.detachNow(this);
      }
    } else if (parent instanceof HasWidgets) {
      ((HasWidgets) parent).remove(this);
    } else if (parent != null) {
      throw new IllegalStateException(
          "This widget's parent does not implement HasWidgets");
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
  public void setLayoutData(Object layoutData) {
    this.layoutData = layoutData;
  }

  /**
   * Overridden to defer the call to super.sinkEvents until the first time this
   * widget is attached to the dom, as a performance enhancement. Subclasses
   * wishing to customize sinkEvents can preserve this deferred sink behavior by
   * putting their implementation behind a check of
   * <code>isOrWasAttached()</code>:
   *
   * <pre>
   * {@literal @}Override
   * public void sinkEvents(int eventBitsToAdd) {
   *   if (isOrWasAttached()) {
   *     /{@literal *} customized sink code goes here {@literal *}/
   *   } else {
   *     super.sinkEvents(eventBitsToAdd);
   *  }
   *} </pre>
   */
  @Override
  public void sinkEvents(int eventBitsToAdd) {
    if (isOrWasAttached()) {
      super.sinkEvents(eventBitsToAdd);
    } else {
      eventsToSink |= eventBitsToAdd;
    }
  }

  /**
   * Creates the {@link HandlerManager} used by this Widget. You can override
   * this method to create a custom {@link HandlerManager}.
   *
   * @return the {@link HandlerManager} you want to use
   */
  protected HandlerManager createHandlerManager() {
    return new HandlerManager(this);
  }

  /**
   * Fires an event on a child widget. Used to delegate the handling of an event
   * from one widget to another.
   *
   * @param event the event
   * @param target fire the event on the given target
   */
  protected void delegateEvent(Widget target, GwtEvent<?> event) {
    target.fireEvent(event);
  }

  /**
   * If a widget contains one or more child widgets that are not in the logical
   * widget hierarchy (the child is physically connected only on the DOM level),
   * it must override this method and call {@link #onAttach()} for each of its
   * child widgets.
   *
   * @see #onAttach()
   */
  protected void doAttachChildren() {
  }

  /**
   * If a widget contains one or more child widgets that are not in the logical
   * widget hierarchy (the child is physically connected only on the DOM level),
   * it must override this method and call {@link #onDetach()} for each of its
   * child widgets.
   *
   * @see #onDetach()
   */
  protected void doDetachChildren() {
  }

  /**
   * Gets the number of handlers listening to the event type.
   *
   * @param type the event type
   * @return the number of registered handlers
   */
  protected int getHandlerCount(GwtEvent.Type<?> type) {
    return handlerManager == null ? 0 : handlerManager.getHandlerCount(type);
  }

  /**
   * Has this widget ever been attached?
   *
   * @return true if this widget ever been attached to the DOM, false otherwise
   */
  protected final boolean isOrWasAttached() {
    return eventsToSink == -1;
  }

  /**
   * <p>
   * This method is called when a widget is attached to the browser's document.
   * To receive notification after a Widget has been added to the document,
   * override the {@link #onLoad} method or use {@link #addAttachHandler}.
   * </p>
   * <p>
   * It is strongly recommended that you override {@link #onLoad()} or
   * {@link #doAttachChildren()} instead of this method to avoid inconsistencies
   * between logical and physical attachment states.
   * </p>
   * <p>
   * Subclasses that override this method must call
   * <code>super.onAttach()</code> to ensure that the Widget has been attached
   * to its underlying Element.
   * </p>
   *
   * @throws IllegalStateException if this widget is already attached
   * @see #onLoad()
   * @see #doAttachChildren()
   */
  protected void onAttach() {
    if (isAttached()) {
      throw new IllegalStateException(
          "Should only call onAttach when the widget is detached from the browser's document");
    }

    attached = true;

    // Event hookup code
    DOM.setEventListener(getElement(), this);
    int bitsToAdd = eventsToSink;
    eventsToSink = -1;
    if (bitsToAdd > 0) {
      sinkEvents(bitsToAdd);
    }
    doAttachChildren();

    // onLoad() gets called only *after* all of the children are attached and
    // the attached flag is set. This allows widgets to be notified when they
    // are fully attached, and panels when all of their children are attached.
    onLoad();
    AttachEvent.fire(this, true);
  }

  /**
   * <p>
   * This method is called when a widget is detached from the browser's
   * document. To receive notification before a Widget is removed from the
   * document, override the {@link #onUnload} method or use {@link #addAttachHandler}.
   * </p>
   * <p>
   * It is strongly recommended that you override {@link #onUnload()} or
   * {@link #doDetachChildren()} instead of this method to avoid inconsistencies
   * between logical and physical attachment states.
   * </p>
   * <p>
   * Subclasses that override this method must call
   * <code>super.onDetach()</code> to ensure that the Widget has been detached
   * from the underlying Element. Failure to do so will result in application
   * memory leaks due to circular references between DOM Elements and JavaScript
   * objects.
   * </p>
   *
   * @throws IllegalStateException if this widget is already detached
   * @see #onUnload()
   * @see #doDetachChildren()
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
      AttachEvent.fire(this, false);
    } finally {
      // Put this in a finally, just in case onUnload throws an exception.
      try {
        doDetachChildren();
      } finally {
        // Put this in a finally, in case doDetachChildren throws an exception.
        DOM.setEventListener(getElement(), null);
        attached = false;
      }
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
   * Ensures the existence of the handler manager.
   *
   * @return the handler manager
   * */
  HandlerManager ensureHandlers() {
    return handlerManager == null ? handlerManager = createHandlerManager()
        : handlerManager;
  }

  HandlerManager getHandlerManager() {
    return handlerManager;
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
      try {
        if (oldParent != null && oldParent.isAttached()) {
          onDetach();
          assert !isAttached() : "Failure of " + this.getClass().getName()
              + " to call super.onDetach()";
        }
      } finally {
        // Put this in a finally in case onDetach throws an exception.
        this.parent = null;
      }
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
