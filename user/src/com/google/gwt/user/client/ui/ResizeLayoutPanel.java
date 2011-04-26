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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.logical.shared.HasResizeHandlers;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.layout.client.Layout;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.ResizeLayoutPanel.Impl.Delegate;

/**
 * A simple panel that {@link ProvidesResize} to its one child, but does not
 * {@link RequiresResize}. Use this to embed layout panels in any location
 * within your application.
 */
public class ResizeLayoutPanel extends SimplePanel implements ProvidesResize,
    HasResizeHandlers {

  /**
   * Implementation of resize event.
   */
  abstract static class Impl {
    /**
     * Delegate event handler.
     */
    abstract static interface Delegate {
      /**
       * Called when the element is resized.
       */
      void onResize();
    }

    boolean isAttached;
    Element parent;
    private Delegate delegate;

    /**
     * Initialize the implementation.
     * 
     * @param elem the element to listen for resize
     * @param delegate the {@link Delegate} to inform when resize occurs
     */
    public void init(Element elem, Delegate delegate) {
      this.parent = elem;
      this.delegate = delegate;
    }

    /**
     * Called on attach.
     */
    public void onAttach() {
      isAttached = true;
    }

    /**
     * Called on detach.
     */
    public void onDetach() {
      isAttached = false;
    }

    /**
     * Handle a resize event.
     */
    protected void handleResize() {
      if (isAttached && delegate != null) {
        delegate.onResize();
      }
    }
  }

  /**
   * Implementation of resize event.
   */
  static class ImplStandard extends Impl implements EventListener {
    /**
     * Chrome does not fire an onresize event if the dimensions are too small to
     * render a scrollbar.
     */
    private static final String MIN_SIZE = "20px";

    private Element collapsible;
    private Element collapsibleInner;
    private Element expandable;
    private Element expandableInner;
    private int lastOffsetHeight = -1;
    private int lastOffsetWidth = -1;
    private boolean resettingScrollables;

    @Override
    public void init(Element elem, Delegate delegate) {
      super.init(elem, delegate);

      /*
       * Set the minimum dimensions to ensure that scrollbars are rendered and
       * fire onscroll events.
       */
      elem.getStyle().setProperty("minWidth", MIN_SIZE);
      elem.getStyle().setProperty("minHeight", MIN_SIZE);

      /*
       * Detect expansion. In order to detect an increase in the size of the
       * widget, we create an absolutely positioned, scrollable div with
       * height=width=100%. We then add an inner div that has fixed height and
       * width equal to 100% (converted to pixels) and set scrollLeft/scrollTop
       * to their maximum. When the outer div expands, scrollLeft/scrollTop
       * automatically becomes a smaller number and trigger an onscroll event.
       */
      expandable = Document.get().createDivElement().cast();
      expandable.getStyle().setVisibility(Visibility.HIDDEN);
      expandable.getStyle().setPosition(Position.ABSOLUTE);
      expandable.getStyle().setHeight(100.0, Unit.PCT);
      expandable.getStyle().setWidth(100.0, Unit.PCT);
      expandable.getStyle().setOverflow(Overflow.SCROLL);
      elem.appendChild(expandable);
      expandableInner = Document.get().createDivElement().cast();
      expandable.appendChild(expandableInner);
      DOM.sinkEvents(expandable, Event.ONSCROLL);

      /*
       * Detect collapse. In order to detect a decrease in the size of the
       * widget, we create an absolutely positioned, scrollable div with
       * height=width=100%. We then add an inner div that has height=width=200%
       * and max out the scrollTop/scrollLeft. When the height or width
       * decreases, the inner div loses 2px for every 1px that the scrollable
       * div loses, so the scrollTop/scrollLeft decrease and we get an onscroll
       * event.
       */
      collapsible = Document.get().createDivElement().cast();
      collapsible.getStyle().setVisibility(Visibility.HIDDEN);
      collapsible.getStyle().setPosition(Position.ABSOLUTE);
      collapsible.getStyle().setHeight(100.0, Unit.PCT);
      collapsible.getStyle().setWidth(100.0, Unit.PCT);
      collapsible.getStyle().setOverflow(Overflow.SCROLL);
      elem.appendChild(collapsible);
      collapsibleInner = Document.get().createDivElement().cast();
      collapsibleInner.getStyle().setWidth(200, Unit.PCT);
      collapsibleInner.getStyle().setHeight(200, Unit.PCT);
      collapsible.appendChild(collapsibleInner);
      DOM.sinkEvents(collapsible, Event.ONSCROLL);
    }

    @Override
    public void onAttach() {
      super.onAttach();
      DOM.setEventListener(expandable, this);
      DOM.setEventListener(collapsible, this);

      /*
       * Update the scrollables in a deferred command so the browser calculates
       * the offsetHeight/Width correctly.
       */
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        public void execute() {
          resetScrollables();
        }
      });
    }

    public void onBrowserEvent(Event event) {
      if (!resettingScrollables && Event.ONSCROLL == event.getTypeInt()) {
        EventTarget eventTarget = event.getEventTarget();
        if (!Element.is(eventTarget)) {
          return;
        }
        Element target = eventTarget.cast();
        if (target == collapsible || target == expandable) {
          handleResize();
        }
      }
    }

    @Override
    public void onDetach() {
      super.onDetach();
      DOM.setEventListener(expandable, null);
      DOM.setEventListener(collapsible, null);
      lastOffsetHeight = -1;
      lastOffsetWidth = -1;
    }

    @Override
    protected void handleResize() {
      if (resetScrollables()) {
        super.handleResize();
      }
    }

    /**
     * Reset the positions of the scrollable elements.
     * 
     * @return true if the size changed, false if not
     */
    private boolean resetScrollables() {
      /*
       * Older versions of safari trigger a synchronous scroll event when we
       * update scrollTop/scrollLeft, so we set a boolean to ignore that event.
       */
      if (resettingScrollables) {
        return false;
      }
      resettingScrollables = true;

      /*
       * Reset expandable element. Scrollbars are not rendered if the div is too
       * small, so we need to set the dimensions of the inner div to a value
       * greater than the offsetWidth/Height.
       */
      int offsetHeight = parent.getOffsetHeight();
      int offsetWidth = parent.getOffsetWidth();
      int height = offsetHeight + 100;
      int width = offsetWidth + 100;
      expandableInner.getStyle().setHeight(height, Unit.PX);
      expandableInner.getStyle().setWidth(width, Unit.PX);
      expandable.setScrollTop(height);
      expandable.setScrollLeft(width);

      // Reset collapsible element.
      collapsible.setScrollTop(collapsible.getScrollHeight() + 100);
      collapsible.setScrollLeft(collapsible.getScrollWidth() + 100);

      if (lastOffsetHeight != offsetHeight || lastOffsetWidth != offsetWidth) {
        lastOffsetHeight = offsetHeight;
        lastOffsetWidth = offsetWidth;
        resettingScrollables = false;
        return true;
      }
      resettingScrollables = false;
      return false;
    }
  }

  /**
   * Implementation of resize event used by IE.
   */
  static class ImplTrident extends Impl {

    @Override
    public void init(Element elem, Delegate delegate) {
      super.init(elem, delegate);
      initResizeEventListener(elem);
    }

    @Override
    public void onAttach() {
      super.onAttach();
      setResizeEventListener(parent, this);
    }

    @Override
    public void onDetach() {
      super.onDetach();
      setResizeEventListener(parent, null);
    }

    /**
     * Initalize the onresize listener. This method doesn't create a memory leak
     * because we don't set a back reference to the Impl class until we attach
     * to the DOM.
     */
    private native void initResizeEventListener(Element elem) /*-{
      var theElem = elem;
      var handleResize = $entry(function() {
        if (theElem.__resizeImpl) {
          theElem.__resizeImpl.@com.google.gwt.user.client.ui.ResizeLayoutPanel.Impl::handleResize()();
        }
      });
      elem.attachEvent('onresize', handleResize);
    }-*/;

    /**
     * Set the event listener that handles resize events.
     */
    private native void setResizeEventListener(Element elem, Impl listener) /*-{
      elem.__resizeImpl = listener;
    }-*/;
  }

  /**
   * Implementation of resize event used by IE6.
   */
  static class ImplIE6 extends ImplTrident {
    @Override
    public void onAttach() {
      super.onAttach();

      /*
       * IE6 doesn't render this panel unless you kick it after its been
       * attached.
       */
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        public void execute() {
          if (isAttached) {
            parent.getStyle().setProperty("zoom", "1");
          }
        }
      });
    }
  }

  private final Impl impl = GWT.create(Impl.class);
  private Layer layer;
  private final Layout layout;
  private final ScheduledCommand resizeCmd = new ScheduledCommand() {
    public void execute() {
      resizeCmdScheduled = false;
      handleResize();
    }
  };
  private boolean resizeCmdScheduled = false;

  public ResizeLayoutPanel() {
    layout = new Layout(getElement());
    impl.init(getElement(), new Delegate() {
      public void onResize() {
        scheduleResize();
      }
    });
  }

  public HandlerRegistration addResizeHandler(ResizeHandler handler) {
    return addHandler(handler, ResizeEvent.getType());
  }

  @Override
  public boolean remove(Widget w) {
    // Validate.
    if (widget != w) {
      return false;
    }

    // Orphan.
    try {
      orphan(w);
    } finally {
      // Physical detach.
      layout.removeChild(layer);
      layer = null;

      // Logical detach.
      widget = null;
    }
    return true;
  }

  @Override
  public void setWidget(Widget w) {
    // Validate
    if (w == widget) {
      return;
    }

    // Detach new child.
    if (w != null) {
      w.removeFromParent();
    }

    // Remove old child.
    if (widget != null) {
      remove(widget);
    }

    // Logical attach.
    widget = w;

    if (w != null) {
      // Physical attach.
      layer = layout.attachChild(widget.getElement(), widget);
      layer.setTopHeight(0.0, Unit.PX, 100.0, Unit.PCT);
      layer.setLeftWidth(0.0, Unit.PX, 100.0, Unit.PCT);

      adopt(w);

      // Update the layout.
      layout.layout();
      scheduleResize();
    }
  }

  @Override
  protected void onAttach() {
    super.onAttach();
    impl.onAttach();
    layout.onAttach();
    scheduleResize();
  }

  @Override
  protected void onDetach() {
    super.onDetach();
    impl.onDetach();
    layout.onDetach();
  }

  private void handleResize() {
    if (!isAttached()) {
      return;
    }

    // Provide resize to child.
    if (widget instanceof RequiresResize) {
      ((RequiresResize) widget).onResize();
    }

    // Fire resize event.
    ResizeEvent.fire(this, getOffsetWidth(), getOffsetHeight());
  }

  /**
   * Schedule a resize handler. We schedule the event so the DOM has time to
   * update the offset sizes, and to avoid duplicate resize events from both a
   * height and width resize.
   */
  private void scheduleResize() {
    if (isAttached() && !resizeCmdScheduled) {
      resizeCmdScheduled = true;
      Scheduler.get().scheduleDeferred(resizeCmd);
    }
  }
}
