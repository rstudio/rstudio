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
import com.google.gwt.user.client.Element;

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
 * <h3>Example</h3> {@example com.google.gwt.examples.CompositeExample}
 * </p>
 */
public abstract class Composite extends Widget {

  private Widget widget;

  /**
   * This override checks to ensure {@link #initWidget(Widget)} has been called.
   */
  public Element getElement() {
    if (widget == null) {
      throw new IllegalStateException("initWidget() was never called in "
        + GWT.getTypeName(this));
    }
    return super.getElement();
  }

  /**
   * Sets the widget to be wrapped by the composite. The wrapped widget must be
   * set before calling any {@link Widget} methods on this object, or adding it
   * to a panel. This method may only be called once for a given composite.
   * 
   * @param widget the widget to be wrapped
   */
  protected void initWidget(Widget widget) {
    // Make sure the widget is not being set twice.
    if (this.widget != null) {
      throw new IllegalStateException("Composite.initWidget() may only be "
        + "called once.");
    }

    widget.removeFromParent();

    // Use the contained widget's element as the composite's element,
    // effectively merging them within the DOM.
    setElement(widget.getElement());

    // The Composite now owns this widget.
    this.widget = widget;
    widget.setParent(this);
  }

  protected void onAttach() {
    super.onAttach();

    // Call onAttach() on behalf of the contained widget.
    widget.onAttach();
  }

  protected void onDetach() {
    super.onDetach();

    // Call onDetach() on behalf of the contained widget.
    widget.onDetach();
  }

  /**
   * Sets the widget to be wrapped by the composite.
   * 
   * @param widget the widget to be wrapped
   * @deprecated this method is deprecated, and will be removed when GWT leaves
   *             beta (use {@link #initWidget(Widget)} instead)
   */
  protected void setWidget(Widget widget) {
    initWidget(widget);
  }
}
