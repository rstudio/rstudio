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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * <p>
 * A {@link StackPanel} that wraps each item in a 2x3 grid (six box), which
 * allows users to add rounded corners.
 * </p>
 * 
 * <p>
 * This widget will <em>only</em> work in quirks mode. If your application is in
 * Standards Mode, use {@link StackLayoutPanel} instead.
 * </p>
 * 
 * <p>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-DecoratedStackPanel { the panel itself }</li>
 * <li>.gwt-DecoratedStackPanel .gwt-StackPanelItem { unselected items }</li>
 * <li>.gwt-DecoratedStackPanel .gwt-StackPanelItem-selected { selected items }</li>
 * <li>.gwt-DecoratedStackPanel .gwt-StackPanelContent { the wrapper around the
 * contents of the item }</li>
 * <li>.gwt-DecoratedStackPanel .stackItemTopLeft { top left corner of the
 * item}</li>
 * <li>.gwt-DecoratedStackPanel .stackItemTopLeftInner { the inner element of
 * the cell}</li>
 * <li>.gwt-DecoratedStackPanel .stackItemTopCenter { top center of the item}</li>
 * <li>.gwt-DecoratedStackPanel .stackItemTopCenterInner { the inner element of
 * the cell}</li>
 * <li>.gwt-DecoratedStackPanel .stackItemTopRight { top right corner of the
 * item}</li>
 * <li>.gwt-DecoratedStackPanel .stackItemTopRightInner { the inner element of
 * the cell}</li>
 * <li>.gwt-DecoratedStackPanel .stackItemMiddleLeft { left side of the item }</li>
 * <li>.gwt-DecoratedStackPanel .stackItemMiddleLeftInner { the inner element
 * of the cell}</li>
 * <li>.gwt-DecoratedStackPanel .stackItemMiddleCenter { center of the item,
 * where the item text resides }</li>
 * <li>.gwt-DecoratedStackPanel .stackItemMiddleCenterInner { the inner element
 * of the cell}</li>
 * <li>.gwt-DecoratedStackPanel .stackItemMiddleRight { right side of the item }</li>
 * <li>.gwt-DecoratedStackPanel .stackItemMiddleRightInner { the inner element
 * of the cell}</li>
 * </ul>
 * </p>
 * 
 * @see StackLayoutPanel
 */
public class DecoratedStackPanel extends StackPanel {
  public static final String DEFAULT_STYLENAME = "gwt-DecoratedStackPanel";

  private static final String[] DEFAULT_ROW_STYLENAMES = {
      "stackItemTop", "stackItemMiddle"};

  /**
   * Creates an empty decorated stack panel.
   */
  public DecoratedStackPanel() {
    super();
    setStylePrimaryName(DEFAULT_STYLENAME);
  }

  @Override
  Element createHeaderElem() {
    // Create the table
    Element table = DOM.createTable();
    Element tbody = DOM.createTBody();
    DOM.appendChild(table, tbody);
    DOM.setStyleAttribute(table, "width", "100%");
    DOM.setElementPropertyInt(table, "cellSpacing", 0);
    DOM.setElementPropertyInt(table, "cellPadding", 0);

    // Add the decorated rows
    for (int i = 0; i < DEFAULT_ROW_STYLENAMES.length; i++) {
      DOM.appendChild(tbody, DecoratorPanel.createTR(DEFAULT_ROW_STYLENAMES[i]));
    }

    // Return the table
    return table;
  }

  @Override
  Element getHeaderTextElem(Element headerElem) {
    Element tbody = DOM.getFirstChild(headerElem);
    Element tr = DOM.getChild(tbody, 1);
    Element td = DOM.getChild(tr, 1);
    return DOM.getFirstChild(td);
  }
}
