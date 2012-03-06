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

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.layout.client.Layout;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CommonResources;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.ImportedWithPrefix;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;

/**
 * A custom version of the {@link ScrollPanel} that allows user provided
 * scrollbars.
 * 
 * <p>
 * The postion of scrollbars in a {@link CustomScrollPanel} differs from that of
 * a native scrollable element. In a native element, scrollbars appear adjacent
 * to the content, shrinking the content client height and width when they
 * appear. {@link CustomScrollPanel} instead overlays scrollbars on top of the
 * content, so the content does not change size when scrollbars appear. If the
 * scrollbars obscures the content, you can set the <code>padding-top</code> and
 * <code>padding-bottom</code> of the content to shift the content out from
 * under the scrollbars.
 * </p>
 * 
 * <p>
 * NOTE: Unlike {@link ScrollPanel}, which implements {@link RequiresResize} but
 * doesn't really require it, {@link CustomScrollPanel} actually does require
 * resize and should only be added to a panel that implements
 * {@link ProvidesResize}, such as most layout panels and
 * {@link ResizeLayoutPanel}.
 * </p>
 */
public class CustomScrollPanel extends ScrollPanel {

  /**
   * A ClientBundle of resources used by this widget.
   */
  public interface Resources extends ClientBundle {
    /**
     * The styles used in this widget.
     */
    @Source(Style.DEFAULT_CSS)
    Style customScrollPanelStyle();
  }

  /**
   * Styles used by this widget.
   */
  @ImportedWithPrefix("gwt-CustomScrollPanel")
  public interface Style extends CssResource {
    /**
     * The path to the default CSS styles used by this resource.
     */
    String DEFAULT_CSS = "com/google/gwt/user/client/ui/CustomScrollPanel.css";

    /**
     * Applied to the widget.
     */
    String customScrollPanel();

    /**
     * Applied to the square that appears in the bottom corner where the
     * vertical and horizontal scrollbars meet, when both are visible.
     */
    String customScrollPanelCorner();
  }

  private static Resources DEFAULT_RESOURCES;

  /**
   * The timeout to ignore scroll events after updating the scroll position.
   * Some browsers queue up scroll events and fire them after a delay. So, if
   * the user quickly scrolls from position 0 to 100 to 200, the scroll event
   * will fire for position 100 after the scroller has already moved to position
   * 200. If we do not ignore the scroll events, we can end up with a loop where
   * the scrollbars update the scroll position, and vice versa.
   */
  private static int IGNORE_SCROLL_TIMEOUT = 500;

  /**
   * Get the default {@link Resources} for this widget.
   */
  private static Resources getDefaultResources() {
    if (DEFAULT_RESOURCES == null) {
      DEFAULT_RESOURCES = GWT.create(Resources.class);
    }
    return DEFAULT_RESOURCES;
  }

  private boolean alwaysShowScrollbars;
  private final ResizeLayoutPanel.Impl containerResizeImpl = GWT
      .create(ResizeLayoutPanel.Impl.class);
  private final Element cornerElem;
  private final Layer cornerLayer;
  private double ignoreContentUntil = 0;
  private double ignoreScrollbarsUntil = 0;
  private final Layout layout;
  private final Layer scrollableLayer;

  // Information about the horizontal scrollbar.
  private HorizontalScrollbar hScrollbar;
  private int hScrollbarHeight;
  private HandlerRegistration hScrollbarHandler;
  private Layer hScrollbarLayer;

  // Information about the vertical scrollbar.
  private VerticalScrollbar vScrollbar;
  private int vScrollbarWidth;
  private HandlerRegistration vScrollbarHandler;
  private Layer vScrollbarLayer;

  /**
   * Creates an empty {@link CustomScrollPanel}.
   */
  public CustomScrollPanel() {
    this(getDefaultResources());
  }

  public CustomScrollPanel(Resources resources) {
    super(DOM.createDiv(), DOM.createDiv(), DOM.createDiv());

    // Inject the styles used by this widget.
    Style style = resources.customScrollPanelStyle();
    style.ensureInjected();
    setStyleName(style.customScrollPanel());

    // Initialize the layout implementation.
    layout = new Layout(getElement());

    /*
     * Apply the inline block style to the container element so it resizes with
     * the content.
     */
    Element containerElem = getContainerElement();
    containerElem.setClassName(CommonResources.getInlineBlockStyle());

    /*
     * Attach the scrollable element with the container. The scrollable element
     * always shows its scrollbars, but they are hidden beneath the root
     * element.
     */
    Element scrollable = getScrollableElement();
    scrollable.getStyle().setOverflow(Overflow.SCROLL);
    scrollable.appendChild(containerElem);
    scrollableLayer = layout.attachChild(scrollable);

    /*
     * Hide the native scrollbars beneath the root element. The scrollable
     * element's dimensions are large enough that the scrollbars are outside of
     * the root element, and the root element is set to hide overflow, making
     * the scrollbars invisible. The scrollable elements dimensions are set
     * after the widget is attached to the document in hideNativeScrollbars().
     */
    getElement().getStyle().setOverflow(Overflow.HIDDEN);

    /*
     * Create a corner element that appears at the gap where the vertical and
     * horizontal scrollbars meet (when both are visible). This prevents the
     * content from peeking out from this gap.
     */
    cornerElem = Document.get().createDivElement();
    cornerElem.addClassName(style.customScrollPanelCorner());
    cornerLayer = layout.attachChild(cornerElem);

    // Initialize the default scrollbars using the transparent styles.
    NativeHorizontalScrollbar.Resources hResources =
        GWT.create(NativeHorizontalScrollbar.ResourcesTransparant.class);
    setHorizontalScrollbar(new NativeHorizontalScrollbar(hResources), AbstractNativeScrollbar
        .getNativeScrollbarHeight());
    NativeVerticalScrollbar.Resources vResources =
        GWT.create(NativeVerticalScrollbar.ResourcesTransparant.class);
    setVerticalScrollbar(new NativeVerticalScrollbar(vResources), AbstractNativeScrollbar
        .getNativeScrollbarWidth());

    /*
     * Add a handler to catch changes in the content size and update the
     * scrollbars accordingly.
     */
    ResizeLayoutPanel.Impl.Delegate containerResizeDelegate =
        new ResizeLayoutPanel.Impl.Delegate() {
          @Override
          public void onResize() {
            maybeUpdateScrollbars();
          }
        };
    containerResizeImpl.init(getContainerElement(), containerResizeDelegate);

    /*
     * Listen for scroll events from the root element and the scrollable element
     * so we can align the scrollbars with the content. Scroll events usually
     * come from the scrollable element, but they can also come from the root
     * element if the user clicks and drags the content, which reveals the
     * hidden scrollbars.
     */
    Event.sinkEvents(getElement(), Event.ONSCROLL);
    Event.sinkEvents(getScrollableElement(), Event.ONSCROLL);
  }

  /**
   * Creates a {@link CustomScrollPanel} with the specified child widget.
   * 
   * @param child the widget to be wrapped by the scroll panel
   */
  public CustomScrollPanel(Widget child) {
    this(getDefaultResources());
    setWidget(child);
  }

  /**
   * Get the scrollbar used for horizontal scrolling.
   * 
   * @return the horizontal scrollbar, or null if none specified
   */
  public HorizontalScrollbar getHorizontalScrollbar() {
    return hScrollbar;
  }

  /**
   * Get the scrollbar used for vertical scrolling.
   * 
   * @return the vertical scrollbar, or null if none specified
   */
  public VerticalScrollbar getVerticalScrollbar() {
    return vScrollbar;
  }

  @Override
  public void onBrowserEvent(Event event) {
    // Align the scrollbars with the content.
    if (Event.ONSCROLL == event.getTypeInt()) {
      double curTime = Duration.currentTimeMillis();
      if (curTime > ignoreContentUntil) {
        ignoreScrollbarsUntil = curTime + IGNORE_SCROLL_TIMEOUT;
        maybeUpdateScrollbarPositions();
      }
    }

    super.onBrowserEvent(event);
  }

  @Override
  public void onResize() {
    maybeUpdateScrollbars();
    super.onResize();
  }

  @Override
  public boolean remove(Widget w) {
    // Validate.
    if (w.getParent() != this) {
      return false;
    }

    if (w == getWidget()) {
      // Remove the content widget.
      boolean toRet = super.remove(w);
      maybeUpdateScrollbars();
      return toRet;
    }

    // Remove a scrollbar.
    try {
      // Orphan.
      orphan(w);
    } finally {
      // Physical detach.
      w.getElement().removeFromParent();

      // Logical detach.
      Widget hScrollbarWidget = (hScrollbar == null) ? null : hScrollbar.asWidget();
      Widget vScrollbarWidget = (vScrollbar == null) ? null : vScrollbar.asWidget();
      if (w == hScrollbarWidget) {
        hScrollbar = null;
        hScrollbarHandler.removeHandler();
        hScrollbarHandler = null;
        layout.removeChild(hScrollbarLayer);
        hScrollbarLayer = null;
      } else if (w == vScrollbarWidget) {
        vScrollbar = null;
        vScrollbarHandler.removeHandler();
        vScrollbarHandler = null;
        layout.removeChild(vScrollbarLayer);
        vScrollbarLayer = null;
      }
    }
    maybeUpdateScrollbars();
    return true;
  }

  /**
   * Remove the {@link HorizontalScrollbar}, if one exists.
   */
  public void removeHorizontalScrollbar() {
    if (hScrollbar != null) {
      remove(hScrollbar);
    }
  }

  /**
   * Remove the {@link VerticalScrollbar}, if one exists.
   */
  public void removeVerticalScrollbar() {
    if (vScrollbar != null) {
      remove(vScrollbar);
    }
  }

  @Override
  public void setAlwaysShowScrollBars(boolean alwaysShow) {
    if (this.alwaysShowScrollbars != alwaysShow) {
      this.alwaysShowScrollbars = alwaysShow;
      maybeUpdateScrollbars();
    }
  }

  /**
   * Set the scrollbar used for horizontal scrolling.
   * 
   * @param scrollbar the scrollbar, or null to clear it
   * @param height the height of the scrollbar in pixels
   */
  public void setHorizontalScrollbar(final HorizontalScrollbar scrollbar, int height) {
    // Physical attach.
    hScrollbarLayer = add(scrollbar, hScrollbar, hScrollbarLayer);

    // Logical attach.
    hScrollbar = scrollbar;
    hScrollbarHeight = height;

    // Initialize the new scrollbar.
    if (scrollbar != null) {
      hScrollbarHandler = scrollbar.addScrollHandler(new ScrollHandler() {
        @Override
        public void onScroll(ScrollEvent event) {
          double curTime = Duration.currentTimeMillis();
          if (curTime > ignoreScrollbarsUntil) {
            ignoreContentUntil = curTime + IGNORE_SCROLL_TIMEOUT;
            int hPos = scrollbar.getHorizontalScrollPosition();
            if (getHorizontalScrollPosition() != hPos) {
              setHorizontalScrollPosition(hPos);
            }
          }
        }
      });
    }
    maybeUpdateScrollbars();
  }

  /**
   * Set the scrollbar used for vertical scrolling.
   * 
   * @param scrollbar the scrollbar, or null to clear it
   * @param width the width of the scrollbar in pixels
   */
  public void setVerticalScrollbar(final VerticalScrollbar scrollbar, int width) {
    // Physical attach.
    vScrollbarLayer = add(scrollbar, vScrollbar, vScrollbarLayer);

    // Logical attach.
    vScrollbar = scrollbar;
    vScrollbarWidth = width;

    // Initialize the new scrollbar.
    if (scrollbar != null) {
      vScrollbarHandler = scrollbar.addScrollHandler(new ScrollHandler() {
        @Override
        public void onScroll(ScrollEvent event) {
          double curTime = Duration.currentTimeMillis();
          if (curTime > ignoreScrollbarsUntil) {
            ignoreContentUntil = curTime + IGNORE_SCROLL_TIMEOUT;
            int vPos = scrollbar.getVerticalScrollPosition();
            int v = getVerticalScrollPosition();
            if (getVerticalScrollPosition() != vPos) {
              setVerticalScrollPosition(vPos);
            }
          }
        }
      });
    }
    maybeUpdateScrollbars();
  }

  @Override
  public void setWidget(Widget w) {
    // Early exit if the widget is unchanged. Avoids updating the scrollbars.
    if (w == getWidget()) {
      return;
    }

    super.setWidget(w);
    maybeUpdateScrollbars();
  }

  @Override
  protected void doAttachChildren() {
    AttachDetachException.tryCommand(AttachDetachException.attachCommand, getWidget(), hScrollbar,
        vScrollbar);
  }

  @Override
  protected void doDetachChildren() {
    AttachDetachException.tryCommand(AttachDetachException.detachCommand, getWidget(), hScrollbar,
        vScrollbar);
  }

  @Override
  protected void onAttach() {
    super.onAttach();
    containerResizeImpl.onAttach();
    layout.onAttach();
  }

  @Override
  protected void onDetach() {
    super.onDetach();
    containerResizeImpl.onDetach();
    layout.onDetach();
  }

  @Override
  protected void onLoad() {
    hideNativeScrollbars();
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      @Override
      public void execute() {
        maybeUpdateScrollbars();
      }
    });
  }

  /**
   * Add a widget to the panel in the specified layer. Note that this method
   * does not do the logical attach.
   * 
   * @param w the widget to add, or null to clear the widget
   * @param toReplace the widget to replace
   * @param layer the layer in which the existing widget is placed
   * @return the layer in which the new widget is placed, or null if no widget
   */
  private Layer add(IsWidget w, IsWidget toReplace, Layer layer) {
    // Validate.
    if (w == toReplace) {
      return layer;
    }

    // Detach new child.
    if (w != null) {
      w.asWidget().removeFromParent();
    }

    // Remove old child.
    if (toReplace != null) {
      remove(toReplace);
    }

    Layer toRet = null;
    if (w != null) {
      // Physical attach.
      toRet = layout.attachChild(w.asWidget().getElement());

      adopt(w.asWidget());
    }

    return toRet;
  }

  /**
   * Hide the native scrollbars. We call this after attaching to ensure that we
   * inherit the direction (rtl or ltr).
   */
  private void hideNativeScrollbars() {
    int barWidth = AbstractNativeScrollbar.getNativeScrollbarWidth();
    int barHeight = AbstractNativeScrollbar.getNativeScrollbarHeight();
    scrollableLayer.setTopBottom(0.0, Unit.PX, -barHeight, Unit.PX);
    if (AbstractNativeScrollbar.isScrollbarLeftAlignedInRtl()
        && ScrollImpl.get().isRtl(getScrollableElement())) {
      scrollableLayer.setLeftRight(-barWidth, Unit.PX, 0.0, Unit.PX);
    } else {
      scrollableLayer.setLeftRight(0.0, Unit.PX, -barWidth, Unit.PX);
    }
    layout.layout();
  }

  /**
   * Synchronize the scroll positions of the scrollbars with the actual scroll
   * position of the content.
   */
  private void maybeUpdateScrollbarPositions() {
    if (!isAttached()) {
      return;
    }

    if (hScrollbar != null) {
      int hPos = getHorizontalScrollPosition();
      if (hScrollbar.getHorizontalScrollPosition() != hPos) {
        hScrollbar.setHorizontalScrollPosition(hPos);
      }
    }
    if (vScrollbar != null) {
      int vPos = getVerticalScrollPosition();
      if (vScrollbar.getVerticalScrollPosition() != vPos) {
        vScrollbar.setVerticalScrollPosition(vPos);
      }
    }

    /*
     * Ensure that the viewport is anchored to the corner. If the user clicks
     * and drags the content, its possible to shift the viewport and reveal the
     * hidden scrollbars.
     */
    if (getElement().getScrollLeft() != 0) {
      getElement().setScrollLeft(0);
    }
    if (getElement().getScrollTop() != 0) {
      getElement().setScrollTop(0);
    }
  }

  /**
   * Update the position of the scrollbars.
   * 
   * <p>
   * If only the vertical scrollbar is present, it takes up the entire height of
   * the right side. If only the horizontal scrollbar is present, it takes up
   * the entire width of the bottom. If both scrollbars are present, the
   * vertical scrollbar extends from the top to just above the horizontal
   * scrollbar, and the horizontal scrollbar extends from the left to just right
   * of the vertical scrollbar, leaving a small square in the bottom right
   * corner.
   * 
   * <p>
   * In RTL, the vertical scrollbar appears on the right.
   */
  private void maybeUpdateScrollbars() {
    if (!isAttached()) {
      return;
    }

    /*
     * Measure the height and width of the content directly. Note that measuring
     * the height and width of the container element (which should be the same)
     * doesn't work correctly in IE.
     */
    Widget w = getWidget();
    int contentHeight = (w == null) ? 0 : w.getOffsetHeight();
    int contentWidth = (w == null) ? 0 : w.getOffsetWidth();

    // Determine which scrollbars to show.
    int realScrollbarHeight = 0;
    int realScrollbarWidth = 0;
    if (hScrollbar != null
        && (alwaysShowScrollbars || getElement().getClientWidth() < contentWidth)) {
      // Horizontal scrollbar is defined and required.
      realScrollbarHeight = hScrollbarHeight;
    }
    if (vScrollbar != null
        && (alwaysShowScrollbars || getElement().getClientHeight() < contentHeight)) {
      // Vertical scrollbar is defined and required.
      realScrollbarWidth = vScrollbarWidth;
    }

    /*
     * Add some padding to the so bottom we can scroll to the bottom without the
     * content being hidden beneath the horizontal scrollbar.
     */
    if (w != null) {
      if (realScrollbarHeight > 0) {
        w.getElement().getStyle().setMarginBottom(realScrollbarHeight, Unit.PX);
        contentHeight += realScrollbarHeight;
      } else {
        w.getElement().getStyle().clearMarginBottom();
      }
    }

    // Adjust the scrollbar layers to display the visible scrollbars.
    boolean isRtl = ScrollImpl.get().isRtl(getScrollableElement());
    if (realScrollbarHeight > 0) {
      hScrollbarLayer.setVisible(true);
      if (isRtl) {
        hScrollbarLayer.setLeftRight(realScrollbarWidth, Unit.PX, 0.0, Unit.PX);
      } else {
        hScrollbarLayer.setLeftRight(0.0, Unit.PX, realScrollbarWidth, Unit.PX);
      }
      hScrollbarLayer.setBottomHeight(0.0, Unit.PX, realScrollbarHeight, Unit.PX);
      hScrollbar.setScrollWidth(Math.max(0, contentWidth - realScrollbarWidth));
    } else if (hScrollbarLayer != null) {
      hScrollbarLayer.setVisible(false);
    }
    if (realScrollbarWidth > 0) {
      vScrollbarLayer.setVisible(true);
      vScrollbarLayer.setTopBottom(0.0, Unit.PX, realScrollbarHeight, Unit.PX);
      if (isRtl) {
        vScrollbarLayer.setLeftWidth(0.0, Unit.PX, realScrollbarWidth, Unit.PX);
      } else {
        vScrollbarLayer.setRightWidth(0.0, Unit.PX, realScrollbarWidth, Unit.PX);
      }
      vScrollbar.setScrollHeight(Math.max(0, contentHeight - realScrollbarHeight));
    } else if (vScrollbarLayer != null) {
      vScrollbarLayer.setVisible(false);
    }

    /*
     * Show the corner in the gap between the vertical and horizontal
     * scrollbars.
     */
    cornerLayer.setBottomHeight(0.0, Unit.PX, realScrollbarHeight, Unit.PX);
    if (isRtl) {
      cornerLayer.setLeftWidth(0.0, Unit.PX, realScrollbarWidth, Unit.PX);
    } else {
      cornerLayer.setRightWidth(0.0, Unit.PX, realScrollbarWidth, Unit.PX);
    }
    cornerLayer.setVisible(hScrollbarHeight > 0 && vScrollbarWidth > 0);

    // Apply the layout.
    layout.layout();
    maybeUpdateScrollbarPositions();
  }
}
