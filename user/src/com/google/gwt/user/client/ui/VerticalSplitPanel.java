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
 * A panel that arranges two widgets in a single vertical column and allows the
 * user to interactively change the proportion of the height dedicated to each
 * of the two widgets. Widgets contained within a
 * <code>VerticalSplitterPanel</code> will be automatically decorated with
 * scrollbars when necessary.
 * 
 * <p>
 * <img class='gallery' src='VerticalSplitPanel.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-VerticalSplitPanel { the panel itself }</li>
 * <li>.gwt-VerticalSplitPanel vsplitter { the splitter }</li>
 * </ul>
 */
public final class VerticalSplitPanel extends SplitPanel {

  /**
   * Provides a base implementation for splitter layout that relies on CSS
   * positioned layout.
   */
  private static class Impl {
    private static void expandToFitParentHorizontally(Element elem) {
      addAbsolutePositoning(elem);
      DOM.setStyleAttribute(elem, "left", "0");
      DOM.setStyleAttribute(elem, "right", "0");
    }

    protected VerticalSplitPanel panel;

    public void init(VerticalSplitPanel panel) {
      this.panel = panel;

      DOM.setStyleAttribute(panel.getElement(), "position", "relative");

      final Element topElem = panel.getElement(TOP);
      final Element bottomElem = panel.getElement(BOTTOM);

      expandToFitParentHorizontally(topElem);
      expandToFitParentHorizontally(bottomElem);
      expandToFitParentHorizontally(panel.getSplitElement());

      expandToFitParentUsingCssOffsets(panel.container);

      // Snap the bottom wrapper to the bottom side.
      DOM.setStyleAttribute(bottomElem, "bottom", "0");
    }

    public void onAttach() {
    }

    public void onDetach() {
    }

    public void onSplitterResize(int px) {
      setSplitPosition(px);
    }

    public void setSplitPosition(int px) {
      final Element splitElem = panel.getSplitElement();

      final int rootElemHeight = getOffsetHeight(panel.container);
      final int splitElemHeight = getOffsetHeight(splitElem);

      if (rootElemHeight < splitElemHeight) {
        return;
      }

      int newBottomHeight = rootElemHeight - px - splitElemHeight;
      if (px < 0) {
        px = 0;
        newBottomHeight = rootElemHeight - splitElemHeight;
      } else if (newBottomHeight < 0) {
        px = rootElemHeight - splitElemHeight;
        newBottomHeight = 0;
      }

      updateElements(panel.getElement(TOP), splitElem,
          panel.getElement(BOTTOM), px, px + splitElemHeight, newBottomHeight);
    }

    /**
     * @param topElem
     * @param splitElem
     * @param bottomElem
     * @param topHeight
     * @param bottomTop
     * @param bottomHeight
     */
    protected void updateElements(Element topElem, Element splitElem,
        Element bottomElem, int topHeight, int bottomTop, int bottomHeight) {
      setHeight(topElem, topHeight + "px");

      setTop(splitElem, topHeight + "px");

      setTop(bottomElem, bottomTop + "px");

      // bottom's height is handled by CSS.
    }
  }

  /**
   * Provides an implementation for IE6/7 that relies on 100% length in CSS.
   */
  @SuppressWarnings("unused") // will be used by IE6 permutation
  private static class ImplIE6 extends Impl {

    private static void expandToFitParentHorizontally(Element elem) {
      addAbsolutePositoning(elem);
      setLeft(elem, "0");
      setWidth(elem, "100%");
    }

    private boolean isResizeInProgress = false;

    private int splitPosition;

    private boolean isTopHidden = false, isBottomHidden = false;

    @Override
    public void init(VerticalSplitPanel panel) {
      this.panel = panel;

      final Element elem = panel.getElement();

      // Prevents inherited text-align settings from interfering with the
      // panel's layout.
      DOM.setStyleAttribute(elem, "textAlign", "left");
      DOM.setStyleAttribute(elem, "position", "relative");

      final Element topElem = panel.getElement(TOP);
      final Element bottomElem = panel.getElement(BOTTOM);

      expandToFitParentHorizontally(topElem);
      expandToFitParentHorizontally(bottomElem);
      expandToFitParentHorizontally(panel.getSplitElement());

      expandToFitParentUsingPercentages(panel.container);
    }

    @Override
    public void onAttach() {
      addResizeListener(panel.container);
      onResize();
    }

    @Override
    public void onDetach() {
      DOM.setElementProperty(panel.container, "onresize", null);
    }

    @Override
    public void onSplitterResize(int px) {
      /*
       * IE6/7 has event priority issues that will prevent the repaints from
       * happening quickly enough causing the interaction to seem unresponsive.
       * The following is simply a poor man's mouse event coalescing.
       */
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
    protected void updateElements(Element topElem, Element splitElem,
        Element bottomElem, int topHeight, int bottomTop, int bottomHeight) {
      /*
       * IE6/7 has a quirk where a zero height element with non-zero height
       * children will expand larger than 100%. To prevent this, the width is
       * explicitly set to zero when height is zero.
       */
      if (topHeight == 0) {
        setWidth(topElem, "0px");
        isTopHidden = true;
      } else if (isTopHidden) {
        setWidth(topElem, "100%");
        isTopHidden = false;
      }

      if (bottomHeight == 0) {
        setWidth(bottomElem, "0px");
        isBottomHidden = true;
      } else if (isBottomHidden) {
        setWidth(bottomElem, "100%");
        isBottomHidden = false;
      }

      super.updateElements(topElem, splitElem, bottomElem, topHeight,
          bottomTop, bottomHeight);

      // IE6/7 cannot update properly with CSS alone.
      setHeight(bottomElem, bottomHeight + "px");
    }

    private native void addResizeListener(Element container) /*-{
         var self = this;
         container.onresize = $entry(function() {
           self.@com.google.gwt.user.client.ui.VerticalSplitPanel$ImplIE6::onResize()();
         });
      }-*/;

    private void onResize() {
      setSplitPosition(getOffsetHeight(panel.getElement(TOP)));
    }
  }

  /**
   * Constant makes for readable calls to {@link #getElement(int)} and
   * {@link #getWidget(int)}.
   */
  private static final int TOP = 0;

  /**
   * Constant makes for readable calls to {@link #getElement(int)} and
   * {@link #getWidget(int)}.
   */
  private static final int BOTTOM = 1;

  // Captures the height of the top container when drag resizing starts.
  private int initialTopHeight = 0;

  // Captures the offset of a user's mouse pointer during drag resizing.
  private int initialThumbPos = 0;

  // A style-free element to serve as the root container.
  private final Element container;

  private final Impl impl = GWT.create(Impl.class);

  private String lastSplitPosition;

  public VerticalSplitPanel() {
    this(GWT.<VerticalSplitPanelImages>create(VerticalSplitPanelImages.class));
  }

  /**
   * Creates an empty vertical split panel.
   */
  public VerticalSplitPanel(VerticalSplitPanelImages images) {
    super(DOM.createDiv(), DOM.createDiv(), preventBoxStyles(DOM.createDiv()),
        preventBoxStyles(DOM.createDiv()));

    container = preventBoxStyles(DOM.createDiv());

    buildDOM(images.verticalSplitPanelThumb());

    setStyleName("gwt-VerticalSplitPanel");

    impl.init(this);

    setSplitPosition("50%");
  }

  /**
   * Gets the widget in the bottom of the panel.
   * 
   * @return the widget, <code>null</code> if there is not one
   */
  public Widget getBottomWidget() {
    return getWidget(BOTTOM);
  }

  /**
   * Gets the widget in the top of the panel.
   * 
   * @return the widget, <code>null</code> if there is not one
   */
  public Widget getTopWidget() {
    return getWidget(TOP);
  }

  /**
   * Sets the widget in the bottom of the panel.
   * 
   * @param w the widget
   */
  public void setBottomWidget(Widget w) {
    setWidget(BOTTOM, w);
  }

  @Override
  public void setHeight(String height) {
    super.setHeight(height);
  }

  @Override
  public void setSplitPosition(String pos) {
    lastSplitPosition = pos;
    final Element topElem = getElement(TOP);
    setHeight(topElem, pos);
    impl.setSplitPosition(getOffsetHeight(topElem));
  }

  /**
   * Sets the widget in the top of the panel.
   * 
   * @param w the widget
   */
  public void setTopWidget(Widget w) {
    setWidget(TOP, w);
  }

  /**
   * <b>Affected Elements:</b>
   * <ul>
   * <li>-splitter = the container containing the splitter element.</li>
   * <li>-top = the container above the splitter.</li>
   * <li>-bottom = the container below the splitter.</li>
   * </ul>
   * 
   * @see UIObject#onEnsureDebugId(String)
   */
  @Override
  protected void onEnsureDebugId(String baseID) {
    super.onEnsureDebugId(baseID);
    ensureDebugId(getElement(TOP), baseID, "top");
    ensureDebugId(getElement(BOTTOM), baseID, "bottom");
  }

  @Override
  protected void onLoad() {
    impl.onAttach();

    /*
     * Set the position realizing it might not work until after layout runs.
     * This first call is simply to try to avoid a jitter effect if possible.
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
  void onSplitterResize(int x, int y) {
    impl.onSplitterResize(initialTopHeight + y - initialThumbPos);
  }

  @Override
  void onSplitterResizeStarted(int x, int y) {
    initialThumbPos = y;
    initialTopHeight = getOffsetHeight(getElement(TOP));
  }

  private void buildDOM(AbstractImagePrototype thumb) {
    final Element topDiv = getElement(TOP);
    final Element bottomDiv = getElement(BOTTOM);
    final Element splitDiv = getSplitElement();

    DOM.appendChild(getElement(), container);

    DOM.appendChild(container, topDiv);
    DOM.appendChild(container, splitDiv);
    DOM.appendChild(container, bottomDiv);

    /*
     * The style name is placed on the table rather than splitElem to allow the
     * splitter to be styled without interfering with layout.
     */
    DOM.setInnerHTML(splitDiv, "<div class='vsplitter' "
        + "style='text-align:center;'>" + thumb.getHTML() + "</div>");

    addScrolling(topDiv);
    addScrolling(bottomDiv);
  }
}
