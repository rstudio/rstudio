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

import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * <p>
 * A {@link SimplePanel} that wraps its contents in stylized boxes, which can be
 * used to add rounded corners to a {@link Widget}.
 * </p>
 * <p>
 * This widget will <em>only</em> work in quirks mode in most cases.
 * Specifically, setting the height or width of the DecoratorPanel will result
 * in rendering issues.
 * </p>
 * <p>
 * Wrapping a {@link Widget} in a "9-box" allows users to specify images in each
 * of the corners and along the four borders. This method allows the content
 * within the {@link DecoratorPanel} to resize without disrupting the look of
 * the border. In addition, rounded corners can generally be combined into a
 * single image file, which reduces the number of downloaded files at startup.
 * This class also simplifies the process of using AlphaImageLoaders to support
 * 8-bit transparencies (anti-aliasing and shadows) in ie6, which does not
 * support them normally.
 * </p>
 * <h3>Setting the Size:</h3>
 * <p>
 * If you set the width or height of the {@link DecoratorPanel}, you need to
 * set the height and width of the middleCenter cell to 100% so that the
 * middleCenter cell takes up all of the available space. If you do not set the
 * width and height of the {@link DecoratorPanel}, it will wrap its contents
 * tightly.
 * </p>
 * 
 * <pre>
 * .gwt-DecoratorPanel .middleCenter {
 *   height: 100%;
 *   width: 100%;
 * }
 * </pre>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-DecoratorPanel { the panel }</li>
 * <li>.gwt-DecoratorPanel .top { the top row }</li>
 * <li>.gwt-DecoratorPanel .topLeft { the top left cell }</li>
 * <li>.gwt-DecoratorPanel .topLeftInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratorPanel .topCenter { the top center cell }</li>
 * <li>.gwt-DecoratorPanel .topCenterInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratorPanel .topRight { the top right cell }</li>
 * <li>.gwt-DecoratorPanel .topRightInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratorPanel .middle { the middle row }</li>
 * <li>.gwt-DecoratorPanel .middleLeft { the middle left cell }</li>
 * <li>.gwt-DecoratorPanel .middleLeftInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratorPanel .middleCenter { the middle center cell }</li>
 * <li>.gwt-DecoratorPanel .middleCenterInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratorPanel .middleRight { the middle right cell }</li>
 * <li>.gwt-DecoratorPanel .middleRightInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratorPanel .bottom { the bottom row }</li>
 * <li>.gwt-DecoratorPanel .bottomLeft { the bottom left cell }</li>
 * <li>.gwt-DecoratorPanel .bottomLeftInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratorPanel .bottomCenter { the bottom center cell }</li>
 * <li>.gwt-DecoratorPanel .bottomCenterInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratorPanel .bottomRight { the bottom right cell }</li>
 * <li>.gwt-DecoratorPanel .bottomRightInner { the inner element of the cell }</li>
 * </ul>
 */
public class DecoratorPanel extends SimplePanel {
  /**
   * The default style name.
   */
  private static final String DEFAULT_STYLENAME = "gwt-DecoratorPanel";

  /**
   * The default styles applied to each row.
   */
  private static final String[] DEFAULT_ROW_STYLENAMES = {
      "top", "middle", "bottom"};

  /**
   * Create a new row with a specific style name. The row will contain three
   * cells (Left, Center, and Right), each prefixed with the specified style
   * name.
   * 
   * This method allows Widgets to reuse the code on a DOM level, without
   * creating a DecoratorPanel Widget.
   * 
   * @param styleName the style name
   * @return the new row {@link Element}
   */
  static Element createTR(String styleName) {
    Element trElem = DOM.createTR();
    setStyleName(trElem, styleName);
    if (LocaleInfo.getCurrentLocale().isRTL()) {
      DOM.appendChild(trElem, createTD(styleName + "Right"));
      DOM.appendChild(trElem, createTD(styleName + "Center"));
      DOM.appendChild(trElem, createTD(styleName + "Left"));
    } else {
      DOM.appendChild(trElem, createTD(styleName + "Left"));
      DOM.appendChild(trElem, createTD(styleName + "Center"));
      DOM.appendChild(trElem, createTD(styleName + "Right"));
    }
    return trElem;
  }

  /**
   * Create a new table cell with a specific style name.
   * 
   * @param styleName the style name
   * @return the new cell {@link Element}
   */
  private static Element createTD(String styleName) {
    Element tdElem = DOM.createTD();
    Element inner = DOM.createDiv();
    DOM.appendChild(tdElem, inner);
    setStyleName(tdElem, styleName);
    setStyleName(inner, styleName + "Inner");
    return tdElem;
  }

  /**
   * The container element at the center of the panel.
   */
  private Element containerElem;

  /**
   * The table body element.
   */
  private Element tbody;

  /**
   * Create a new {@link DecoratorPanel}.
   */
  public DecoratorPanel() {
    this(DEFAULT_ROW_STYLENAMES, 1);
  }

  /**
   * Creates a new panel using the specified style names to apply to each row.
   * Each row will contain three cells (Left, Center, and Right). The Center
   * cell in the containerIndex row will contain the {@link Widget}.
   * 
   * @param rowStyles an array of style names to apply to each row
   * @param containerIndex the index of the container row
   */
  DecoratorPanel(String[] rowStyles, int containerIndex) {
    super(DOM.createTable());

    // Add a tbody
    Element table = getElement();
    tbody = DOM.createTBody();
    DOM.appendChild(table, tbody);
    DOM.setElementPropertyInt(table, "cellSpacing", 0);
    DOM.setElementPropertyInt(table, "cellPadding", 0);

    // Add each row
    for (int i = 0; i < rowStyles.length; i++) {
      Element row = createTR(rowStyles[i]);
      DOM.appendChild(tbody, row);
      if (i == containerIndex) {
        containerElem = DOM.getFirstChild(DOM.getChild(row, 1));
      }
    }

    // Set the overall style name
    setStyleName(DEFAULT_STYLENAME);
  }

  /**
   * Get a specific Element from the panel.
   * 
   * @param row the row index
   * @param cell the cell index
   * @return the Element at the given row and cell
   */
  protected Element getCellElement(int row, int cell) {
    Element tr = DOM.getChild(tbody, row);
    Element td = DOM.getChild(tr, cell);
    return DOM.getFirstChild(td);
  }

  @Override
  protected Element getContainerElement() {
    return containerElem;
  }
}
