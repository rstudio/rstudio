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

import com.google.gwt.user.client.Element;

import java.util.Iterator;

/**
 * <p>
 * A {@link PopupPanel} that wraps its content in a 3x3 grid, which allows users
 * to add rounded corners.
 * </p>
 * 
 * <h3>Setting the Size:</h3>
 * <p>
 * If you set the width or height of the {@link DecoratedPopupPanel}, you need
 * to set the height and width of the middleCenter cell to 100% so that the
 * middleCenter cell takes up all of the available space. If you do not set the
 * width and height of the {@link DecoratedPopupPanel}, it will wrap its
 * contents tightly.
 * </p>
 * 
 * <pre>
 * .gwt-DecoratedPopupPanel .popupMiddleCenter {
 *   height: 100%;
 *   width: 100%;
 * }
 * </pre>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-DecoratedPopupPanel { the outside of the popup }</li>
 * <li>.gwt-DecoratedPopupPanel .popupContent { the wrapper around the content }</li>
 * <li>.gwt-DecoratedPopupPanel .popupTopLeft { the top left cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupTopLeftInner { the inner element of the
 * cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupTopCenter { the top center cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupTopCenterInner { the inner element of the
 * cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupTopRight { the top right cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupTopRightInner { the inner element of the
 * cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupMiddleLeft { the middle left cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupMiddleLeftInner { the inner element of
 * the cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupMiddleCenter { the middle center cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupMiddleCenterInner { the inner element of
 * the cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupMiddleRight { the middle right cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupMiddleRightInner { the inner element of
 * the cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupBottomLeft { the bottom left cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupBottomLeftInner { the inner element of
 * the cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupBottomCenter { the bottom center cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupBottomCenterInner { the inner element of
 * the cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupBottomRight { the bottom right cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupBottomRightInner { the inner element of
 * the cell }</li>
 * </ul>
 */
public class DecoratedPopupPanel extends PopupPanel {
  private static final String DEFAULT_STYLENAME = "gwt-DecoratedPopupPanel";

  /**
   * The panel used to nine box the contents.
   */
  private DecoratorPanel decPanel;

  /**
   * Creates an empty decorated popup panel. A child widget must be added to it
   * before it is shown.
   */
  public DecoratedPopupPanel() {
    this(false);
  }

  /**
   * Creates an empty decorated popup panel, specifying its "auto-hide"
   * property.
   * 
   * @param autoHide <code>true</code> if the popup should be automatically
   *          hidden when the user clicks outside of it
   */
  public DecoratedPopupPanel(boolean autoHide) {
    this(autoHide, false);
  }

  /**
   * Creates an empty decorated popup panel, specifying its "auto-hide" and
   * "modal" properties.
   * 
   * @param autoHide <code>true</code> if the popup should be automatically
   *          hidden when the user clicks outside of it
   * @param modal <code>true</code> if keyboard or mouse events that do not
   *          target the PopupPanel or its children should be ignored
   */
  public DecoratedPopupPanel(boolean autoHide, boolean modal) {
    this(autoHide, modal, "popup");
  }

  /**
   * Creates an empty decorated popup panel using the specified style names.
   * 
   * @param autoHide <code>true</code> if the popup should be automatically
   *          hidden when the user clicks outside of it
   * @param modal <code>true</code> if keyboard or mouse events that do not
   *          target the PopupPanel or its children should be ignored
   * @param prefix the prefix applied to child style names
   */
  DecoratedPopupPanel(boolean autoHide, boolean modal, String prefix) {
    super(autoHide, modal);
    String[] rowStyles = new String[] {
        prefix + "Top", prefix + "Middle", prefix + "Bottom"};
    decPanel = new DecoratorPanel(rowStyles, 1);
    decPanel.setStyleName("");
    setStylePrimaryName(DEFAULT_STYLENAME);
    super.setWidget(decPanel);
    setStyleName(getContainerElement(), "popupContent", false);
    setStyleName(decPanel.getContainerElement(), prefix + "Content", true);
  }

  @Override
  public void clear() {
    decPanel.clear();
  }

  @Override
  public Widget getWidget() {
    return decPanel.getWidget();
  }

  @Override
  public Iterator<Widget> iterator() {
    return decPanel.iterator();
  }

  @Override
  public boolean remove(Widget w) {
    return decPanel.remove(w);
  }

  @Override
  public void setWidget(Widget w) {
    decPanel.setWidget(w);
    maybeUpdateSize();
  }

  @Override
  protected void doAttachChildren() {
    // See comment in doDetachChildren for an explanation of this call
    decPanel.onAttach();
  }

  @Override
  protected void doDetachChildren() {
    // We need to detach the decPanel because it is not part of the iterator of
    // Widgets that this class returns (see the iterator() method override).
    // Detaching the decPanel detaches both itself and its children. We do not
    // call super.onDetachChildren() because that would detach the decPanel's
    // children (redundantly) without detaching the decPanel itself.
    // This is similar to a {@link ComplexPanel}, but we do not want to expose
    // the decPanel widget, as its just an internal implementation.
    decPanel.onDetach();
  }

  /**
   * Get a specific Element from the panel.
   * 
   * @param row the row index
   * @param cell the cell index
   * @return the Element at the given row and cell
   */
  protected Element getCellElement(int row, int cell) {
    return decPanel.getCellElement(row, cell);
  }
}
