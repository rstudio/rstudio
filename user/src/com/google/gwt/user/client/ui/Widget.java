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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

/**
 * The base class for the majority of user-interface objects. Widget adds
 * support for receiving events from the browser and being added directly to
 * {@link com.google.gwt.user.client.ui.Panel panels}.
 */
public class Widget extends UIObject implements EventListener {

  private boolean attached;
  private Object layoutData;
  private Widget parent;

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
   * This method is called when a widget is attached to the browser's document.
   * It must not be overridden, except by {@link Panel}. To receive
   * notification when a widget is attached to the document, override the
   * {@link #onLoad} method.
   */
  protected void onAttach() {
    if (attached) {
      return;
    }

    attached = true;

    // Set the main element's event listener. This should only be set
    // while the widget is attached, because it creates a circular
    // reference between JavaScript and the DOM.
    DOM.setEventListener(getElement(), this);

    // Now that the widget is attached, call onLoad().
    onLoad();
  }

  /**
   * This method is called when a widget is detached from the browser's
   * document. It must not be overridden, except by {@link Panel}.
   */
  protected void onDetach() {
    if (!attached) {
      return;
    }
    attached = false;

    // Clear out the element's event listener (breaking the circular
    // reference between it and the widget).
    //
    DOM.setEventListener(getElement(), null);
  }

  /**
   * This method is called when the widget becomes attached to the browser's
   * document.
   */
  protected void onLoad() {
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
   */
  void setParent(Widget parent) {
    this.parent = parent;

    if (parent == null) {
      onDetach();
    } else if (parent.isAttached()) {
      onAttach();
    }
  }
}
