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
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-DecoratedPopupPanel { the outside of the popup }</li>
 * <li>.gwt-DecoratedPopupPanel .content { the wrapper around the content }</li>
 * </ul>
 * <p>
 * The styles that apply to {@link DecoratorPanel} also apply to PopupPanel.
 * </p>
 */
public class DecoratedPopupPanel extends PopupPanel {
  private static final String DEFAULT_STYLENAME = "gwt-DecoratedPopupPanel";

  /**
   * The panel used to nine box the contents.
   */
  private DecoratorPanel decPanel = new DecoratorPanel();

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
    super(autoHide, modal);
    decPanel.setStyleName("");
    setStylePrimaryName(DEFAULT_STYLENAME);
    super.setWidget(decPanel);
    setStyleName(getContainerElement(), "content", false);
    setStyleName(decPanel.getContainerElement(), "content", true);
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
