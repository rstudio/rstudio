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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.TextAlign;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;

/**
 * A panel that arranges two widgets in a single horizontal row and allows the
 * user to interactively change the proportion of the width dedicated to each of
 * the two widgets. Widgets contained within a <code>HorizontalSplitPanel</code>
 * will be automatically decorated with scrollbars when necessary.
 * 
 * <p>
 * This widget will <em>only</em> work in quirks mode. If your application is in
 * Standards Mode, use {@link SplitLayoutPanel} instead.
 * </p>
 * 
 * <p>
 * <img class='gallery' src='doc-files/HorizontalSplitPanel.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul>
 * <li>.gwt-HorizontalSplitPanel { the panel itself }</li>
 * <li>.gwt-HorizontalSplitPanel hsplitter { the splitter }</li>
 * </ul>
 * 
 * @deprecated Use {@link SplitLayoutPanel} instead, but understand that it is
 *             not a drop in replacement for this class. It requires standards
 *             mode, and is most easily used under a {@link RootLayoutPanel} (as
 *             opposed to a {@link RootPanel}
 *
 * @see SplitLayoutPanel
 */
@Deprecated
public final class HorizontalSplitPanel extends SplitPanel {
  /**
   * The default resources used by this widget.
   */
  public interface Resources extends ClientBundle {
    /**
     * An image representing the drag thumb.
     */
    @Source("splitPanelThumb.png")
    ImageResource horizontalSplitPanelThumb();
  }

  /**
   * The standard implementation for horizontal split panels.
   */
  private static class Impl {
    private static void expandToFitParentHorizontally(Element elem) {
      addAbsolutePositoning(elem);
      final String zeroSize = "0px";
      setTop(elem, zeroSize);
      setBottom(elem, zeroSize);
    }

    protected HorizontalSplitPanel panel;

    public void init(HorizontalSplitPanel panel) {
      this.panel = panel;

      DOM.setStyleAttribute(panel.getElement(), "position", "relative");

      expandToFitParentHorizontally(panel.getElement(LEFT));
      expandToFitParentHorizontally(panel.getElement(RIGHT));
      expandToFitParentHorizontally(panel.getSplitElement());

      expandToFitParentUsingCssOffsets(panel.container);

      // Right now, both panes are stacked on top of each other
      // on either the left side or the right side of the containing
      // panel. This happens because both panes have position:absolute
      // and no left/top values. The panes will be on the left side
      // if the directionality is LTR, and on the right side if the
      // directionality is RTL. In the LTR case, we need to snap the
      // right pane to the right of the container, and in the RTL case,
      // we need to snap the left pane to the left of the container.
      if (LocaleInfo.getCurrentLocale().isRTL()) {
        setLeft(panel.getElement(LEFT), "0px");
      } else {
        setRight(panel.getElement(RIGHT), "0px");
      }
    }

    public void onAttach() {
    }

    public void onDetach() {
    }

    public void onSplitResize(int px) {
      setSplitPositionUsingPixels(px);
    }

    public void setSplitPosition(String pos) {
      final Element leftElem = panel.getElement(LEFT);
      setWidth(leftElem, pos);
      setSplitPositionUsingPixels(getOffsetWidth(leftElem));
    }

    /**
     * Set the splitter's position in units of pixels.
     * 
     * px represents the splitter's position as a distance of px pixels from the
     * left edge of the container. This is true even in a bidi environment.
     * Callers of this method must be aware of this constraint.
     */
    public void setSplitPositionUsingPixels(int px) {
      final Element splitElem = panel.getSplitElement();

      final int rootElemWidth = getOffsetWidth(panel.container);
      final int splitElemWidth = getOffsetWidth(splitElem);

      // This represents an invalid state where layout is incomplete. This
      // typically happens before DOM attachment, but I leave it here as a
      // precaution because negative width/height style attributes produce
      // errors on IE.
      if (rootElemWidth < splitElemWidth) {
        return;
      }

      // Compute the new right side width.
      int newRightWidth = rootElemWidth - px - splitElemWidth;

      // Constrain the dragging to the physical size of the panel.
      if (px < 0) {
        px = 0;
        newRightWidth = rootElemWidth - splitElemWidth;
      } else if (newRightWidth < 0) {
        px = rootElemWidth - splitElemWidth;
        newRightWidth = 0;
      }

      final Element rightElem = panel.getElement(RIGHT);

      // Set the width of the left side.
      setWidth(panel.getElement(LEFT), px + "px");

      // Move the splitter to the right edge of the left element.
      setLeft(splitElem, px + "px");

      // Move the right element to the right of the splitter.
      setLeft(rightElem, (px + splitElemWidth) + "px");

      updateRightWidth(rightElem, newRightWidth);
    }

    /**
     * Implemented by subclasses.
     * 
     * @param rightElem
     * @param newRightWidth
     */
    public void updateRightWidth(Element rightElem, int newRightWidth) {
      // No need to update the width of the right side; this will be
      // recomputed automatically by CSS. This is helpful, as we do not
      // have to worry about watching for resize events and adjusting the
      // right-side width.
    }
  }

  /**
   * The IE6 implementation for horizontal split panels.
   */
  @SuppressWarnings("unused")
  // will be used by IE6 permutation
  private static class ImplIE6 extends Impl {

    private boolean isResizeInProgress = false;

    private int splitPosition = 0;

    @Override
    public void init(HorizontalSplitPanel panel) {
      this.panel = panel;

      final Element elem = panel.getElement();

      // Prevents inherited text-align settings from interfering with the
      // panel's layout. The setting we choose must be bidi-sensitive,
      // as left-alignment is the default with LTR directionality, and
      // right-alignment is the default with RTL directionality.
      if (LocaleInfo.getCurrentLocale().isRTL()) {
        elem.getStyle().setTextAlign(TextAlign.RIGHT);
      } else {
        elem.getStyle().setTextAlign(TextAlign.LEFT);
      }

      DOM.setStyleAttribute(elem, "position", "relative");

      /*
       * Technically, these are snapped to the top and bottom, but IE doesn't
       * provide a reliable way to make that happen, so a resize listener is
       * wired up to control the height of these elements.
       */
      addAbsolutePositoning(panel.getElement(LEFT));
      addAbsolutePositoning(panel.getElement(RIGHT));
      addAbsolutePositoning(panel.getSplitElement());

      expandToFitParentUsingPercentages(panel.container);

      if (LocaleInfo.getCurrentLocale().isRTL()) {
        // Snap the left pane to the left edge of the container. We
        // only need to do this when layout is RTL; if we don't, the
        // left pane will overlap the right pane.
        setLeft(panel.getElement(LEFT), "0px");
      }
    }

    @Override
    public void onAttach() {
      addResizeListener(panel.container);
      onResize();
    }

    @Override
    public void onDetach() {
      DOM.setElementAttribute(panel.container, "onresize", null);
    }

    @Override
    public void onSplitResize(int px) {
      final int resizeUpdatePeriod = 20; // ms
      if (!isResizeInProgress) {
        isResizeInProgress = true;
        new Timer() {
          @Override
          public void run() {
            setSplitPositionUsingPixels(splitPosition);
            isResizeInProgress = false;
          }
        }.schedule(resizeUpdatePeriod);
      }
      splitPosition = px;
    }

    @Override
    public void setSplitPositionUsingPixels(int px) {
      if (LocaleInfo.getCurrentLocale().isRTL()) {
        final Element splitElem = panel.getSplitElement();

        final int rootElemWidth = getOffsetWidth(panel.container);
        final int splitElemWidth = getOffsetWidth(splitElem);

        // This represents an invalid state where layout is incomplete. This
        // typically happens before DOM attachment, but I leave it here as a
        // precaution because negative width/height style attributes produce
        // errors on IE.
        if (rootElemWidth < splitElemWidth) {
          return;
        }

        // Compute the new right side width.
        int newRightWidth = rootElemWidth - px - splitElemWidth;

        // Constrain the dragging to the physical size of the panel.
        if (px < 0) {
          px = 0;
          newRightWidth = rootElemWidth - splitElemWidth;
        } else if (newRightWidth < 0) {
          px = rootElemWidth - splitElemWidth;
          newRightWidth = 0;
        }

        // Set the width of the right side.
        setWidth(panel.getElement(RIGHT), newRightWidth + "px");

        // Move the splitter to the right edge of the left element.
        setLeft(splitElem, px + "px");

        // Update the width of the left side
        if (px == 0) {

          // This takes care of a qurky RTL layout bug with IE6.
          // During DOM construction and layout, onResize events
          // are fired, and this method is called with px == 0.
          // If one tries to set the width of the LEFT element to
          // before layout completes, the RIGHT element will
          // appear to be blanked out.
          Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            public void execute() {
              setWidth(panel.getElement(LEFT), "0px");
            }
          });
        } else {
          setWidth(panel.getElement(LEFT), px + "px");
        }

      } else {
        super.setSplitPositionUsingPixels(px);
      }
    }

    @Override
    public void updateRightWidth(Element rightElem, int newRightWidth) {
      setWidth(rightElem, newRightWidth + "px");
    }

    private native void addResizeListener(Element container) /*-{
      var self = this;
      container.onresize = $entry(function() {
        self.@com.google.gwt.user.client.ui.HorizontalSplitPanel$ImplIE6::onResize()();
      });
    }-*/;

    private void onResize() {
      final Element leftElem = panel.getElement(LEFT);
      final Element rightElem = panel.getElement(RIGHT);

      final String height = getOffsetHeight(panel.container) + "px";
      setHeight(rightElem, height);
      setHeight(panel.getSplitElement(), height);
      setHeight(leftElem, height);
      setSplitPositionUsingPixels(getOffsetWidth(leftElem));
    }
  }

  /**
   * The Safari implementation which owes its existence entirely to a single
   * WebKit bug: http://bugs.webkit.org/show_bug.cgi?id=9137.
   */
  @SuppressWarnings("unused")
  // will be used by Safari permutation
  private static class ImplSafari extends Impl {
    @Override
    public void init(HorizontalSplitPanel panel) {
      this.panel = panel;
      final String fullSize = "100%";
      super.init(panel);
      setHeight(panel.container, fullSize);
      setHeight(panel.getElement(LEFT), fullSize);
      setHeight(panel.getElement(RIGHT), fullSize);
      setHeight(panel.getSplitElement(), fullSize);
    }
  }

  /**
   * Constant makes for readable calls to {@link #getElement(int)} and
   * {@link #getWidget(int)}.
   */
  private static final int LEFT = 0;

  /**
   * Constant makes for readable calls to {@link #getElement(int)} and
   * {@link #getWidget(int)}.
   */
  private static final int RIGHT = 1;

  // A style-free element to serve as the root container.
  private final Element container;

  private final Impl impl = GWT.create(Impl.class);

  /**
   * If the split position is set while the split panel is not attached, save it
   * here to be applied when the panel is attached to the document.
   */
  private String lastSplitPosition = "50%";

  private int initialThumbPos;

  private int initialLeftWidth;

  public HorizontalSplitPanel() {
    this(GWT.<Resources> create(Resources.class));
  }

  /**
   * Creates an empty horizontal split panel.
   * 
   * @param images ImageBundle containing an image for the splitter's drag thumb
   * @deprecated replaced by {@link #HorizontalSplitPanel(Resources)}
   */
  @Deprecated
  public HorizontalSplitPanel(HorizontalSplitPanelImages images) {
    this(images.horizontalSplitPanelThumb());
  }

  /**
   * Creates an empty horizontal split panel.
   * 
   * @param resources ClientBundle containing an image for the splitter's drag
   *          thumb
   */
  public HorizontalSplitPanel(Resources resources) {
    this(AbstractImagePrototype.create(resources.horizontalSplitPanelThumb()));
  }

  private HorizontalSplitPanel(AbstractImagePrototype thumbImage) {
    super(DOM.createDiv(), DOM.createDiv(), preventBoxStyles(DOM.createDiv()),
        preventBoxStyles(DOM.createDiv()));

    container = preventBoxStyles(DOM.createDiv());

    buildDOM(thumbImage);

    setStyleName("gwt-HorizontalSplitPanel");

    impl.init(this);

    // By default, the panel will fill its parent vertically and horizontally.
    // The horizontal case is covered by the fact that the top level div is
    // block display.
    setHeight("100%");
  }

  /**
   * Adds a widget to a pane in the HorizontalSplitPanel. The method will first
   * attempt to add the widget to the left pane. If a widget is already in that
   * position, it will attempt to add the widget to the right pane. If a widget
   * is already in that position, an exception will be thrown, as a
   * HorizontalSplitPanel can contain at most two widgets.
   * 
   * Note that this method is bidi-sensitive. In an RTL environment, this method
   * will first attempt to add the widget to the right pane, and if a widget is
   * already in that position, it will attempt to add the widget to the left
   * pane.
   * 
   * @param w the widget to be added
   * @throws IllegalStateException
   */
  @Override
  public void add(Widget w) {
    if (getStartOfLineWidget() == null) {
      setStartOfLineWidget(w);
    } else if (getEndOfLineWidget() == null) {
      setEndOfLineWidget(w);
    } else {
      throw new IllegalStateException(
          "A Splitter can only contain two Widgets.");
    }
  }

  /**
   * Gets the widget in the pane that is at the end of the line direction for
   * the layout. That is, in an RTL layout, gets the widget in the left pane,
   * and in an LTR layout, gets the widget in the right pane.
   * 
   * @return the widget, <code>null</code> if there is not one.
   */
  public Widget getEndOfLineWidget() {
    return getWidget(getEndOfLinePos());
  }

  /**
   * Gets the widget in the left side of the panel.
   * 
   * @return the widget, <code>null</code> if there is not one.
   */
  public Widget getLeftWidget() {
    return getWidget(LEFT);
  }

  /**
   * Gets the widget in the right side of the panel.
   * 
   * @return the widget, <code>null</code> if there is not one.
   */
  public Widget getRightWidget() {
    return getWidget(RIGHT);
  }

  /**
   * Gets the widget in the pane that is at the start of the line direction for
   * the layout. That is, in an RTL environment, gets the widget in the right
   * pane, and in an LTR environment, gets the widget in the left pane.
   * 
   * @return the widget, <code>null</code> if there is not one.
   */
  public Widget getStartOfLineWidget() {
    return getWidget(getStartOfLinePos());
  }

  /**
   * Sets the widget in the pane that is at the end of the line direction for
   * the layout. That is, in an RTL layout, sets the widget in the left pane,
   * and in and RTL layout, sets the widget in the right pane.
   * 
   * @param w the widget
   */
  public void setEndOfLineWidget(Widget w) {
    setWidget(getEndOfLinePos(), w);
  }

  /**
   * Sets the widget in the left side of the panel.
   * 
   * @param w the widget
   */
  public void setLeftWidget(Widget w) {
    setWidget(LEFT, w);
  }

  /**
   * Sets the widget in the right side of the panel.
   * 
   * @param w the widget
   */
  public void setRightWidget(Widget w) {
    setWidget(RIGHT, w);
  }

  /**
   * Moves the position of the splitter.
   * 
   * This method is not bidi-sensitive. The size specified is always the size of
   * the left region, regardless of directionality.
   * 
   * @param pos the new size of the left region in CSS units (e.g. "10px",
   *          "1em")
   */
  @Override
  public void setSplitPosition(String pos) {
    lastSplitPosition = pos;
    impl.setSplitPosition(pos);
  }

  /**
   * Sets the widget in the pane that is at the start of the line direction for
   * the layout. That is, in an RTL layout, sets the widget in the right pane,
   * and in and RTL layout, sets the widget in the left pane.
   * 
   * @param w the widget
   */
  public void setStartOfLineWidget(Widget w) {
    setWidget(getStartOfLinePos(), w);
  }

  /**
   * <b>Affected Elements:</b>
   * <ul>
   * <li>-splitter = the container containing the splitter element.</li>
   * <li>-right = the container on the right side of the splitter.</li>
   * <li>-left = the container on the left side of the splitter.</li>
   * </ul>
   * 
   * @see UIObject#onEnsureDebugId(String)
   */
  @Override
  protected void onEnsureDebugId(String baseID) {
    super.onEnsureDebugId(baseID);
    ensureDebugId(getElement(LEFT), baseID, "left");
    ensureDebugId(getElement(RIGHT), baseID, "right");
  }

  @Override
  protected void onLoad() {
    impl.onAttach();

    /*
     * If the split position has been changed while detached, apply the change.
     * Set the position realizing that it might not work until after layout
     * runs. This first call is simply to try to avoid a jitter effect if
     * possible.
     */
    setSplitPosition(lastSplitPosition);
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        setSplitPosition(lastSplitPosition);
      }
    });
  }

  @Override
  protected void onUnload() {
    impl.onDetach();
  }

  @Override
  void onSplitterResize(int x, int y) {
    impl.onSplitResize(initialLeftWidth + x - initialThumbPos);
  }

  @Override
  void onSplitterResizeStarted(int x, int y) {
    initialThumbPos = x;
    initialLeftWidth = getOffsetWidth(getElement(LEFT));
  }

  private void buildDOM(AbstractImagePrototype thumbImage) {
    final Element leftDiv = getElement(LEFT);
    final Element rightDiv = getElement(RIGHT);
    final Element splitDiv = getSplitElement();

    DOM.appendChild(getElement(), container);

    DOM.appendChild(container, leftDiv);
    DOM.appendChild(container, splitDiv);
    DOM.appendChild(container, rightDiv);

    /*
     * Sadly, this is the only way I've found to get vertical centering in this
     * case. The usually CSS hacks (display: table-cell, vertical-align: middle)
     * don't work in an absolute positioned DIV.
     */
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<table class='hsplitter' height='100%' cellpadding='0' "
        + "cellspacing='0'><tr><td align='center' valign='middle'>");
    sb.append(thumbImage.getSafeHtml());
    splitDiv.setInnerSafeHtml(sb.toSafeHtml());

    addScrolling(leftDiv);
    addScrolling(rightDiv);
  }

  private int getEndOfLinePos() {
    return (LocaleInfo.getCurrentLocale().isRTL() ? LEFT : RIGHT);
  }

  private int getStartOfLinePos() {
    return (LocaleInfo.getCurrentLocale().isRTL() ? RIGHT : LEFT);
  }
}
