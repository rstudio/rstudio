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
 * <li>.gwt-HorizontalSplitPanel hsplitter { the splitter }</li>
 * </ul>
 */
public final class HorizontalSplitPanel extends SplitPanel {

  /**
   * The standard implementation for horizontal split panels.
   */
  private static class Impl {
    private static void snapToAllEdges(Element elem) {
      enableAbsolutePositon(elem);
      DOM.setStyleAttribute(elem, "left", "0");
      DOM.setStyleAttribute(elem, "right", "0");
      DOM.setStyleAttribute(elem, "top", "0");
      DOM.setStyleAttribute(elem, "bottom", "0");
    }

    private static void snapToTopAndBottomEdges(Element elem) {
      enableAbsolutePositon(elem);
      DOM.setStyleAttribute(elem, "top", "0");
      DOM.setStyleAttribute(elem, "bottom", "0");
    }

    public void init(HorizontalSplitPanel panel) {
      DOM.setStyleAttribute(panel.getElement(), "position", "relative");
      snapToAllEdges(panel.rootWrapper);
      snapToAllEdges(panel.getElement(LEFT));
      snapToAllEdges(panel.getElement(RIGHT));

      snapToTopAndBottomEdges(panel.leftWrapper);
      snapToTopAndBottomEdges(panel.rightWrapper);
      snapToTopAndBottomEdges(panel.getSplitElement());

      // This ensures that any overflow is hidden on the right side of the
      // panel. This can happen when the right side panel is smaller than
      // the borders on its child.
      addElementClipping(panel.rightWrapper);
    }

    public void onAttach(HorizontalSplitPanel panel) {
    }

    public void onDetach(HorizontalSplitPanel panel) {
    }
  }

  /**
   * The IE6 implementation for horizontal split panels.
   */
  private static class ImplIE6 extends Impl {
    private static final String FULLSIZE = "100%";

    private static native void addResizeListener(HorizontalSplitPanel panel) /*-{
      var root = panel.@com.google.gwt.user.client.ui.HorizontalSplitPanel::rootWrapper;
      root.onresize = function() {
        @com.google.gwt.user.client.ui.HorizontalSplitPanel$ImplIE6::onResize(Lcom/google/gwt/user/client/ui/HorizontalSplitPanel;)(panel);
      };
    }-*/;

    private static void onResize(HorizontalSplitPanel panel) {
      final String height = getOffsetHeight(panel.rootWrapper) + "px";
      setHeight(panel.rightWrapper, height);
      setHeight(panel.getSplitElement(), height);
      setHeight(panel.leftWrapper, height);
    }

    private static void snapToAllEdges(Element elem) {
      enableAbsolutePositon(elem);
      setWidth(elem, FULLSIZE);
      setHeight(elem, FULLSIZE);
    }

    private static void snapToTopAndBottomEdges(Element elem) {
      enableAbsolutePositon(elem);
      setHeight(elem, FULLSIZE);
    }

    public void init(HorizontalSplitPanel panel) {
      final Element elem = panel.getElement();
      // Prevents inherited text-align settings from interfering with the
      // panel's layout.
      DOM.setStyleAttribute(elem, "textAlign", "left");
      DOM.setStyleAttribute(elem, "position", "relative");

      enableAbsolutePositon(panel.rightWrapper);
      enableAbsolutePositon(panel.leftWrapper);

      snapToAllEdges(panel.rootWrapper);
      snapToAllEdges(panel.getElement(LEFT));
      snapToAllEdges(panel.getElement(RIGHT));
      snapToTopAndBottomEdges(panel.getSplitElement());
    }

    public void onAttach(HorizontalSplitPanel panel) {
      addResizeListener(panel);
      onResize(panel);
    }

    public void onDetach(HorizontalSplitPanel panel) {
      DOM.setElementAttribute(panel.rootWrapper, "onresize", null);
    }
  }

  /**
   * Constants to provide more readable calls to {@link #getElement()} and
   * {@link #getWidget(int)}.
   */
  private static final int LEFT = 0;

  private static final int RIGHT = 1;

  private static final Impl impl = (Impl) GWT.create(Impl.class);

  private static void enableAbsolutePositon(Element elem) {
    DOM.setStyleAttribute(elem, "position", "absolute");
  }

  private static final int getClientWidth(final Element elem) {
    return DOM.getElementPropertyInt(elem, "clientWidth");
  }

  private static final int getOffsetHeight(Element elem) {
    return DOM.getElementPropertyInt(elem, "offsetHeight");
  }

  private static final int getOffsetWidth(final Element elem) {
    return DOM.getElementPropertyInt(elem, "offsetWidth");
  }

  private static final void setHeight(Element elem, String size) {
    DOM.setStyleAttribute(elem, "height", size);
  }

  private static final void setLeft(Element elem, String size) {
    DOM.setStyleAttribute(elem, "left", size);
  }

  private static final void setWidth(Element elem, String size) {
    DOM.setStyleAttribute(elem, "width", size);
  }

  /**
   * DOM elements needed to support splitter dragging. The underlying DOM
   * structure is:
   * 
   * <pre>
   *   div
   *     div (rootWrapper)
   *       div (leftWrapper)
   *         div (getElement(LEFT))
   *       div (getSplitElement())
   *       div (rightWrapper)
   *         div (getElement(RIGHT))
   * </pre>
   */
  private final Element rootWrapper, leftWrapper, rightWrapper;

  /**
   * If the split position is set while the split panel is not attached, save it
   * here to be applied when the panel is attached to the document.
   */
  private String lastSplitPosition = "50%";

  private int initialThumbPos;

  private int initialLeftWidth;

  /**
   * Creates an empty horizontal split panel.
   */
  public HorizontalSplitPanel() {
    super(DOM.createDiv(), DOM.createDiv(), DOM.createDiv(), DOM.createDiv());

    rootWrapper = preventElementBoxStyles(DOM.createDiv());
    leftWrapper = preventElementBoxStyles(DOM.createDiv());
    rightWrapper = preventElementBoxStyles(DOM.createDiv());

    buildDOM();

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

  public final void setSplitPosition(String pos) {
    lastSplitPosition = pos;
    final Element leftElem = leftWrapper;
    setWidth(leftElem, pos);
    setSplitPosition(getOffsetWidth(leftElem));
  }

  public void setWidth(String width) {
    super.setWidth(width);
    setSplitPosition(getOffsetWidth(leftWrapper));
  }

  protected void onLoad() {
    // If the split position has been changed while detached, apply the change
    impl.onAttach(this);

    /*
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

  protected void onUnload() {
    impl.onDetach(this);
  }

  final void onSplitterResize(int x, int y) {
    // Move the split position by the appropriate offset.
    setSplitPosition(initialLeftWidth + x - initialThumbPos);
  }

  final void onSplitterResizeStarted(int x, int y) {
    initialThumbPos = x;
    initialLeftWidth = getClientWidth(leftWrapper);
  }

  private void buildDOM() {
    final Element leftDiv = getElement(LEFT);
    final Element rightDiv = getElement(RIGHT);
    final Element splitDiv = getSplitElement();

    DOM.appendChild(getElement(), rootWrapper);

    DOM.appendChild(rootWrapper, leftWrapper);
    DOM.appendChild(rootWrapper, splitDiv);
    DOM.appendChild(rootWrapper, rightWrapper);

    DOM.appendChild(leftWrapper, leftDiv);
    DOM.appendChild(rightWrapper, rightDiv);

    DOM.setInnerHTML(splitDiv, "&nbsp;");

    addElementScrolling(leftDiv);
    addElementScrolling(rightDiv);

    setElementClassname(leftDiv, "left");
    setElementClassname(splitDiv, "hsplitter");
    setElementClassname(rightDiv, "right");
  }

  private final void setSplitPosition(int px) {
    final Element splitElem = getSplitElement();

    final int rootElemWidth = getOffsetWidth(rootWrapper);
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

    final Element rightElem = rightWrapper;

    // Set the width of the left side.
    setWidth(leftWrapper, px + "px");

    // Move the splitter to the right edge of the left element.
    setLeft(splitElem, px + "px");

    // Move the right element to the right of the splitter.
    setLeft(rightElem, (px + splitElemWidth) + "px");

    // Update the right element's width.
    setWidth(rightElem, newRightWidth + "px");
  }
}
