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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;

/**
 * A type of widget that can wrap another widget, hiding the wrapped widget's
 * methods. When added to a panel, a composite behaves exactly as if the widget
 * it wraps had been added.
 * 
 * <p>
 * The composite is useful for creating a single widget out of an aggregate of
 * multiple other widgets contained in a single panel.
 * </p>
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.CompositeExample}
 * </p>
 */
public abstract class Composite extends Widget {

  private Widget widget;

  @Override
  public boolean isAttached() {
    if (widget != null) {
      return widget.isAttached();
    }
    return false;
  }

  /**
   * Provides subclasses access to the topmost widget that defines this
   * composite.
   * 
   * @return the widget
   */
  protected Widget getWidget() {
    return widget;
  }

  /**
   * Sets the widget to be wrapped by the composite. The wrapped widget must be
   * set before calling any {@link Widget} methods on this object, or adding it
   * to a panel. This method may only be called once for a given composite.
   * 
   * @param widget the widget to be wrapped
   */
  protected void initWidget(Widget widget) {
    // Validate. Make sure the widget is not being set twice.
    if (this.widget != null) {
      throw new IllegalStateException("Composite.initWidget() may only be "
          + "called once.");
    }

    // Detach the new child.
    widget.removeFromParent();

    // Use the contained widget's element as the composite's element,
    // effectively merging them within the DOM.
    setElement(widget.getElement());

    // Logical attach.
    this.widget = widget;

    // Adopt.
    widget.setParent(this);
  }

  @Override
  public void onBrowserEvent(Event event) {
    // Delegate events to the widget.
    widget.onBrowserEvent(event);
  }

  @Override
  protected void onAttach() {
    // Call the widget's onAttach() first.
    widget.onAttach();

    // Clobber the widget's call to setEventListener(), causing all events to
    // be routed to this composite, which will delegate back to the widget by
    // default (note: it's not necessary to clear this in onDetach(), because
    // the widget's onDetach will do so).
    DOM.setEventListener(getElement(), this);

    // Call onLoad() directly, because we're not calling super.onAttach().
    onLoad();
  }

  @Override
  protected void onDetach() {
    try {
      onUnload();
    } finally {
      // We don't want an exception in user code to keep us from calling the
      // super implementation (or event listeners won't get cleaned up and
      // the attached flag will be wrong).
      widget.onDetach();
    }
  }

  /**
   * This method was for initializing the Widget to be wrapped by this
   * Composite, but has been deprecated in favor of {@link #initWidget(Widget)}.
   * 
   * @deprecated this method is deprecated, and will be removed (use
   *             {@link #initWidget(Widget)} instead)
   */
  @Deprecated
  protected void setWidget(Widget widget) {
    initWidget(widget);
  }
}
