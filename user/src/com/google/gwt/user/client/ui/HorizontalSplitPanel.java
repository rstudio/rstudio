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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;

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
 * <li>.gwt-HorizontalSplitPanel left { the left container }</li>
 * <li>.gwt-HorizontalSplitPanel right { the right container }</li>
 * <li>.gwt-HorizontalSplitPanel splitter { the splitter }</li>
 * </ul>
 */
public final class HorizontalSplitPanel extends SplitPanel {

  /**
   * The resizing implementation for standard browsers (Anything other than
   * Safari and IE6/7)
   */
  private static class Impl {
    // The x position of the mouse when drag resizing begins.
    protected int initialThumbPos = 0;

    // Widths of elements which are needed to do relative drag resizing.
    protected int initialLeftWidth = 0;
    protected int initialLeftContentWidth = 0;
    protected int initialRightContentWidth = 0;

    /**
     * Initializes css properties on the panels DOM structure.
     * 
     * @param panel the panel
     */
    protected void init(HorizontalSplitPanel panel) {
    }

    /**
     * Called on each mouse move event during drag resizing.
     * 
     * @param panel the panel
     * @param pos the current horizontal mouse position relative to the panel
     */
    protected void onSplitResize(final HorizontalSplitPanel panel, int pos) {
      // Compute the distance the splitter must be moved.
      int offset = pos - initialThumbPos;

      /*
       * Compute the projected size of the content areas. This is to prevent
       * out-of-bounds scrolling.
       */
      int newLeftContentWidth = initialLeftContentWidth + offset;
      int newRightContentWidth = initialRightContentWidth - offset;

      if (newLeftContentWidth < 0) {
        offset -= newLeftContentWidth;
      }

      if (newRightContentWidth < 0) {
        offset += newRightContentWidth;
      }

      // Move the split position by the offset.
      setSplitPosition(panel, (initialLeftWidth + offset) + "px");
    }

    /**
     * Called whenever drag resizing begins.
     * 
     * @param panel the panel
     * @param pos the current horizontal mouse position relative to the panel
     */
    protected void onSplitResizeStarted(final HorizontalSplitPanel panel,
        int pos) {
      initialThumbPos = pos;
      initialLeftWidth = getOffsetWidth(panel.leftDiv);
      initialLeftContentWidth = getClientWidth(panel.getElement(LEFT));
      initialRightContentWidth = getClientWidth(panel.getElement(RIGHT));
    }

    /**
     * Sets the horizontal position of the splitter.
     * 
     * @param panel the panel
     * @param pos the position as a css length
     */
    protected void setSplitPosition(final HorizontalSplitPanel panel,
        final String pos) {
      /*
       * This default impl adjusts the width of the first level div and depends
       * on the outer table to adjust its cell widths appropriately.
       */
      setWidth(panel.leftDiv, pos);
    }
  }

  /**
   * The resizing implementation for IE6/7.
   */
  private static class ImplIE6 extends Impl {
    protected void init(final HorizontalSplitPanel panel) {
      /*
       * Without fixed table layout, IE will not respected the table width
       * constraints.
       */
      DOM.setStyleAttribute(panel.table, "tableLayout", "fixed");

      /*
       * Since the DOM structure will not have finished full layout, we must
       * defer until later to ensure that the inner divs update to the proper
       * size.
       */
      DeferredCommand.addCommand(new Command() {
        public void execute() {
          updateDivWidth(panel);
        }
      });
    }

    protected void onSplitResizeStarted(final HorizontalSplitPanel panel,
        final int x) {
      initialThumbPos = x;
      initialLeftWidth = getOffsetWidth(panel.leftTD);
      initialLeftContentWidth = getClientWidth(panel.getElement(LEFT));
      initialRightContentWidth = getClientWidth(panel.getElement(RIGHT));
    }

    protected void setSplitPosition(final HorizontalSplitPanel panel,
        final String pos) {
      final Element leftTD = panel.leftTD;
      // adjust the width of the table cell instead of the inner div.
      setWidth(leftTD, pos);
      updateDivWidth(panel);
    }

    /*
     * IE6 will not properly auto size the inner divs unless we explicitly set
     * their width to something that forces a layout. NOTE: 100% works for
     * quirks mode but will be problematic for standards mode when there are
     * margins, border or padding.
     */
    private void updateDivWidth(final HorizontalSplitPanel panel) {
      final String size = "100%";
      setWidth(panel.leftDiv, size);
      setWidth(panel.rightDiv, size);
      setWidth(panel.getElement(LEFT), size);
      setWidth(panel.getElement(RIGHT), size);
    }
  }

  /**
   * The resizing implemenation for Safari/WebKit.
   */
  private static class ImplSafari extends Impl {

    protected void init(final HorizontalSplitPanel panel) {
      /*
       * Without fixed table layout, Safari will not respect the css width on
       * the table.
       */
      DOM.setStyleAttribute(panel.table, "tableLayout", "fixed");

      final String autoProp = "auto";
      setWidth(panel.leftDiv, autoProp);
      setWidth(panel.rightDiv, autoProp);
      setWidth(panel.getElement(LEFT), autoProp);
      setWidth(panel.getElement(RIGHT), autoProp);

      /*
       * Safari bug: a width must be set on the table when it is added to the
       * DOM or else it cannot be set later.
       */
      panel.setWidth("100%");
    }

    protected void setSplitPosition(final HorizontalSplitPanel panel, String pos) {
      // Adjust the width of the table cell instead of the inner div.
      setWidth(panel.leftTD, pos);
    }
  }

  /**
   * Constants to provide more readable calls to {@link #getElement()} and
   * {@link #getWidget(int)}.
   */
  private static final int LEFT = 0;
  private static final int RIGHT = 1;

  private static final int DEFAULT_SPLITTER_WIDTH = 10;

  private static final int getClientWidth(final Element elem) {
    return DOM.getIntAttribute(elem, "clientWidth");
  }

  private static final int getOffsetWidth(final Element elem) {
    return DOM.getIntAttribute(elem, "offsetWidth");
  }

  private static final void setWidth(Element elem, String size) {
    DOM.setStyleAttribute(elem, "width", size);
  }

  /**
   * DOM elements needed to support splitter dragging. The underlying DOM
   * structure is:
   * 
   * <pre>
   *   table
   *     td (leftTD)
   *       div (leftDiv)
   *         div (getElement(LEFT))
   *     td (splitter)
   *     td (rightTD)
   *       div (rightDiv)
   *         div (getElement(RIGHT))
   * </pre>
   */
  private final Element table;
  private final Element leftTD, rightTD;
  private final Element leftDiv, rightDiv;

  private final Impl impl = (Impl) GWT.create(Impl.class);

  /**
   * Creates an empty horizontal split panel.
   */
  public HorizontalSplitPanel() {
    super(DOM.createTable(), DOM.createTD(), DOM.createDiv(), DOM.createDiv());

    table = getElement();
    leftDiv = preventElementBoxStyles(DOM.createDiv());
    rightDiv = preventElementBoxStyles(DOM.createDiv());
    leftTD = preventElementBoxStyles(DOM.createTD());
    rightTD = preventElementBoxStyles(DOM.createTD());

    buildDOM();

    setStyleName("gwt-HorizontalSplitPanel");

    impl.init(this);
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

  public final void setHeight(String height) {
    DOM.setStyleAttribute(getElement(LEFT), "height", height);
    DOM.setStyleAttribute(getElement(RIGHT), "height", height);
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

  public final void setSplitPosition(String pos) {
    impl.setSplitPosition(this, pos);
  }

  final void onSplitterResize(int x, int y) {
    impl.onSplitResize(this, x);
  }

  final void onSplitterResizeStarted(int x, int y) {
    impl.onSplitResizeStarted(this, x);
  }

  private void buildDOM() {
    final Element leftContentDiv = getElement(LEFT);
    final Element rightContentDiv = getElement(RIGHT);

    final Element tbody = DOM.createTBody();
    final Element tr = DOM.createTR();
    final Element splitTD = getSplitElement();

    DOM.appendChild(table, tbody);
    DOM.appendChild(tbody, tr);
    DOM.appendChild(tr, leftTD);
    DOM.appendChild(tr, splitTD);
    DOM.appendChild(tr, rightTD);
    DOM.appendChild(leftTD, leftDiv);
    DOM.appendChild(rightTD, rightDiv);
    DOM.appendChild(leftDiv, leftContentDiv);
    DOM.appendChild(rightDiv, rightContentDiv);

    DOM.setInnerHTML(splitTD, "&nbsp;");

    DOM.setAttribute(table, "cellSpacing", "0");
    DOM.setAttribute(table, "cellPadding", "0");

    addElementScrolling(leftContentDiv);
    addElementScrolling(rightContentDiv);

    setElementClassname(leftContentDiv, "left");
    setElementClassname(splitTD, "splitter");
    setElementClassname(rightContentDiv, "right");

    DOM.setStyleAttribute(leftTD, "verticalAlign", "top");
    DOM.setStyleAttribute(rightTD, "verticalAlign", "top");

    /*
     * Ensures that the splitter is of reasonable width when no CSS is active on
     * it, but this value is immediately overridden by CSS values.
     */
    DOM.setIntAttribute(splitTD, "width", DEFAULT_SPLITTER_WIDTH);
  }
}
