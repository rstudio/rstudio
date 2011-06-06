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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;

/**
 * EXPERIMENTAL and subject to change. Do not use this in production code.
 * <p>
 * A version of {@link Composite} that supports wrapping {@link Renderable}
 * widgets. This functionality will eventually be merged into {@link Composite}
 * itself, but is still under active development.
 * The only reason why this isn't a subclass of {@link Composite} is to avoid
 * messing up it's API, since {@link Composite} is very often subclassed.
 */
public abstract class RenderableComposite extends Widget implements IsRenderable {

  interface HTMLTemplates extends SafeHtmlTemplates {
    @Template("<span id=\"{0}\"></span>")
     SafeHtml renderWithId(String id);
  }
  private static final HTMLTemplates TEMPLATE =
      GWT.create(HTMLTemplates.class);

  private Widget widget;

  private IsRenderable renderable;

  private Element elementToWrap;

  private boolean initFinished = false;

  @Override
  public com.google.gwt.user.client.Element getElement() {
    if (!initFinished) {
      // if we're trying to get the element but we haven't finished the
      // initialization, do it now.
      initWidgetInternal();
    }
    return super.getElement();
  }

  @Override
  public boolean isAttached() {
    if (widget != null) {
      return widget.isAttached();
    }
    return false;
  }

  @Override
  public void onBrowserEvent(Event event) {
    // Fire any handler added to the composite itself.
    super.onBrowserEvent(event);

    // Delegate events to the widget.
    widget.onBrowserEvent(event);
  }

  @Override
  public void performDetachedInitialization() {
    if (renderable != null) {
      assert (initFinished == false);
      renderable.performDetachedInitialization();
      initWidgetInternal();
    } else {
      elementToWrap.getParentNode().replaceChild(widget.getElement(), elementToWrap);
    }
  }

  @Override
  public SafeHtml render(String id) {
    if (renderable != null) {
      return renderable.render(id);
    } else {
      SafeHtmlBuilder builder = new SafeHtmlBuilder();
      render(id, builder);
      return builder.toSafeHtml();
    }
  }

  @Override
  public void render(String id, SafeHtmlBuilder builder) {
    if (renderable != null) {
      renderable.render(id, builder);
    } else {
      builder.append(TEMPLATE.renderWithId(id));
    }
  }

  @Override
  public void setStyleName(String styleName) {
    if (this.widget == null) {
      throw new IllegalStateException("setStyleName called before initWidget.");
    }
    widget.setStyleName(styleName);
  }

  @Override
  public void wrapElement(Element element) {
    if (renderable != null) {
      assert (initFinished == false);
      renderable.wrapElement(element);
    } else {
      this.elementToWrap = element;
    }
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

    // Logical attach.
    this.widget = widget;

    if (widget instanceof IsRenderable) {
      // In case the Widget being wrapped is an IsRenderable, we delay finishing
      // the initialization until the performDetachedInitialization() is called.
      this.renderable = (IsRenderable) widget;
      return;
    }

    initWidgetInternal();
  }

  @Override
  protected void onAttach() {
    if (!isOrWasAttached()) {
      widget.sinkEvents(eventsToSink);
      eventsToSink = -1;
    }

    widget.onAttach();

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
      widget.onDetach();
    }
  }

  /**
   * This method was for initializing the Widget to be wrapped by this
   * Composite, but has been deprecated in favor of {@link #initWidget(Widget)}.
   *
   * @deprecated Use {@link #initWidget(Widget)} instead
   */
  @Deprecated
  protected void setWidget(Widget widget) {
    initWidget(widget);
  }

  private void initWidgetInternal() {
    // Use the contained widget's element as the composite's element,
    // effectively merging them within the DOM.
    setElement(PotentialElement.resolve(widget.getElement()));

    // Adopt.
    widget.setParent(this);

    // Mark initialization as finished, as this only needs to be run once.
    this.initFinished = true;
  }
}
