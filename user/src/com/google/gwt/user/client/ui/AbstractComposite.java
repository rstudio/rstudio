/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;

/**
 * A type of widget that can wrap another widget, hiding the wrapped widget's
 * methods. When added to a panel, a composite behaves exactly as if the widget
 * it wraps had been added.
 * <p>
 * The composite is useful for creating a single widget out of an aggregate of
 * multiple other widgets contained in a single panel.
 * 
 * @see Composite
 * 
 * @param <W> the type of IsWidget this composite wraps
 */
public class AbstractComposite<W extends IsWidget> extends Widget {

  private W isWidget;

  @Override
  public boolean isAttached() {
    if (isWidget != null) {
      return isWidget.asWidget().isAttached();
    }
    return false;
  }

  @Override
  public void onBrowserEvent(Event event) {
    // Fire any handler added to the composite itself.
    super.onBrowserEvent(event);

    // Delegate events to the widget.
    getWidget().asWidget().onBrowserEvent(event);
  }

  /**
   * Provides subclasses access to the widget that defines this composite.
   * 
   * @return the widget
   */
  protected W getWidget() {
    return isWidget;
  }

  /**
   * Sets the widget to be wrapped by the composite. The wrapped widget must be
   * set before calling any {@link Widget} methods on this object, or adding it
   * to a panel. This method may only be called once for a given composite.
   * <p>
   * The widget returned by the wrapped object's {@link IsWidget#asWidget()} method
   * must be stable. If it changes, the results are unpredictable.
   * 
   * @param isWidget the widget to be wrapped
   */
  protected void initWidget(W isWidget) {
    // Validate. Make sure the widget is not being set twice.
    if (this.isWidget != null) {
      throw new IllegalStateException("initWidget() may only be called once.");
    }

    // TODO rjrjr: avoids breaking some mocked unit tests, should fix the tests instead
    Widget newChild = isWidget instanceof Widget ? (Widget) isWidget : isWidget.asWidget();

    // Detach the new child.
    newChild.removeFromParent();

    // Use the contained widget's element as the composite's element,
    // effectively merging them within the DOM.
    setElement(newChild.getElement());

    // Logical attach.
    this.isWidget = isWidget;

    // Adopt.
    newChild.setParent(this);
  }

  /**
   * 
   */
  @Override
  protected void onAttach() {
    Widget asWidget = getWidget().asWidget();

    if (!isOrWasAttached()) {
      asWidget.sinkEvents(eventsToSink);
      eventsToSink = -1;
    }

    asWidget.onAttach();

    // Clobber the widget's call to setEventListener(), causing all events to
    // be routed to this composite, which will delegate back to the widget by
    // default (note: it's not necessary to clear this in onDetach(), because
    // the widget's onDetach will do so).
    DOM.setEventListener(getElement(), this);

    // Call onLoad() directly, because we're not calling super.onAttach().
    onLoad();
    AttachEvent.fire(this, true);
  }

  @Override
  protected void onDetach() {
    try {
      onUnload();
      AttachEvent.fire(this, false);
    } finally {
      // We don't want an exception in user code to keep us from calling the
      // super implementation (or event listeners won't get cleaned up and
      // the attached flag will be wrong).
      getWidget().asWidget().onDetach();
    }
  }
}