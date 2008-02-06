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
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;

/**
 * A panel that arranges two widgets in a single horizontal row and allows the
 * user to interactively change the proportion of the width dedicated to each of
 * the two widgets. Widgets contained within a <code>HorizontalSplitPanel</code>
 * will be automatically decorated with scrollbars when necessary.
 * 
 * <p>
 * <img class='gallery' src='HorizontalSplitPanel.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-HorizontalSplitPanel { the panel itself }</li>
 * <li>.gwt-HorizontalSplitPanel hsplitter { the splitter }</li>
 * </ul>
 */
public final class HorizontalSplitPanel extends SplitPanel {

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

      final Element rightElem = panel.getElement(RIGHT);

      expandToFitParentHorizontally(panel.getElement(LEFT));
      expandToFitParentHorizontally(rightElem);
      expandToFitParentHorizontally(panel.getSplitElement());

      expandToFitParentUsingCssOffsets(panel.container);

      // Snap the right wrapper to the right side.
      setRight(rightElem, "0px");
    }

    public void onAttach() {
    }

    public void onDetach() {
    }

    public void onSplitResize(int px) {
      setSplitPosition(px);
    }

    public void setSplitPosition(int px) {
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

    public void updateRightWidth(Element rightElem, int newRightWidth) {
      // Update is handled by CSS.
    }
  }

  /**
   * The IE6 implementation for horizontal split panels.
   */
  private static class ImplIE6 extends Impl {

    private boolean isResizeInProgress = false;

    private int splitPosition = 0;

    @Override
    public void init(HorizontalSplitPanel panel) {
      this.panel = panel;

      final Element elem = panel.getElement();
      // Prevents inherited text-align settings from interfering with the
      // panel's layout.
      DOM.setStyleAttribute(elem, "textAlign", "left");
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
            setSplitPosition(splitPosition);
            isResizeInProgress = false;
          }
        }.schedule(resizeUpdatePeriod);
      }
      splitPosition = px;
    }

    @Override
    public void updateRightWidth(Element rightElem, int newRightWidth) {
      setWidth(rightElem, newRightWidth + "px");
    }

    private native void addResizeListener(Element container) /*-{
      var self = this;
      container.onresize = function() {
        self.@com.google.gwt.user.client.ui.HorizontalSplitPanel$ImplIE6::onResize()();
      };
    }-*/;

    private void onResize() {
      final Element leftElem = panel.getElement(LEFT);
      final Element rightElem = panel.getElement(RIGHT);

      final String height = getOffsetHeight(panel.container) + "px";
      setHeight(rightElem, height);
      setHeight(panel.getSplitElement(), height);
      setHeight(leftElem, height);
      setSplitPosition(getOffsetWidth(leftElem));
    }
  }

  /**
   * The Safari implementation which owes its existence entirely to a single
   * WebKit bug: http://bugs.webkit.org/show_bug.cgi?id=9137.
   */
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
    this(GWT.<HorizontalSplitPanelImages>create(HorizontalSplitPanelImages.class));
  }

  /**
   * Creates an empty horizontal split panel.
   */
  public HorizontalSplitPanel(HorizontalSplitPanelImages images) {
    super(DOM.createDiv(), DOM.createDiv(), preventBoxStyles(DOM.createDiv()),
        preventBoxStyles(DOM.createDiv()));

    container = preventBoxStyles(DOM.createDiv());

    buildDOM(images.horizontalSplitPanelThumb());

    setStyleName("gwt-HorizontalSplitPanel");

    impl.init(this);

    // By default, the panel will fill its parent vertically and horizontally.
    // The horizontal case is covered by the fact that the top level div is
    // block display.
    setHeight("100%");
  }

  /**
   * Gets the widget in the left side of the panel.
   * 
   * @return the widget, <code>null</code> if there is not one.
   */
  public final Widget getLeftWidget() {
    return getWidget(LEFT);
  }

  /**
   * Gets the widget in the right side of the panel.
   * 
   * @return the widget, <code>null</code> if there is not one.
   */
  public final Widget getRightWidget() {
    return getWidget(RIGHT);
  }

  /**
   * Sets the widget in the left side of the panel.
   * 
   * @param w the widget
   */
  public final void setLeftWidget(Widget w) {
    setWidget(LEFT, w);
  }

  /**
   * Sets the widget in the right side of the panel.
   * 
   * @param w the widget
   */
  public final void setRightWidget(Widget w) {
    setWidget(RIGHT, w);
  }

  @Override
  public final void setSplitPosition(String pos) {
    lastSplitPosition = pos;
    final Element leftElem = getElement(LEFT);
    setWidth(leftElem, pos);
    impl.setSplitPosition(getOffsetWidth(leftElem));
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
    DeferredCommand.addCommand(new Command() {
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
  final void onSplitterResize(int x, int y) {
    impl.onSplitResize(initialLeftWidth + x - initialThumbPos);
  }

  @Override
  final void onSplitterResizeStarted(int x, int y) {
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
    DOM.setInnerHTML(splitDiv,
        "<table class='hsplitter' height='100%' cellpadding='0' "
            + "cellspacing='0'><tr><td align='center' valign='middle'>"
            + thumbImage.getHTML());

    addScrolling(leftDiv);
    addScrolling(rightDiv);
  }
}
