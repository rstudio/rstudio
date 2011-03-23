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
package com.google.gwt.widget.client;

import com.google.gwt.cell.client.ButtonCellBase;
import com.google.gwt.cell.client.IsCollapsible;
import com.google.gwt.cell.client.ButtonCellBase.Decoration;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasAllFocusHandlers;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasMouseDownHandlers;
import com.google.gwt.event.dom.client.HasMouseOutHandlers;
import com.google.gwt.event.dom.client.HasMouseOverHandlers;
import com.google.gwt.event.dom.client.HasMouseUpHandlers;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.CellWidget;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasEnabled;

/**
 * Base class for buttons that supports content decoration.
 * 
 * @param <C> the data type of the button's contents
 */
public class ButtonBase<C> extends CellWidget<C> implements HasEnabled, Focusable, IsCollapsible,
    HasAllFocusHandlers, HasMouseDownHandlers, HasMouseUpHandlers, HasMouseOutHandlers,
    HasMouseOverHandlers, HasClickHandlers, HasAllKeyHandlers {

  /**
   * Initialize the tab index of the cell and return it. This is needed so we
   * can modify the cell before calling the super constructor.
   * 
   * @param <C> the cell type
   * @param cell the cell
   */
  static <C extends ButtonCellBase<?>> C initializeCell(C cell) {
    cell.setTabIndex(0);
    return cell;
  }

  private final ButtonCellBase<C> cell;

  /**
   * Construct a new {@link ButtonBase} with the specified cell and an initial
   * value of <code>null</code>.
   * 
   * @param cell the cell to wrap
   */
  protected ButtonBase(ButtonCellBase<C> cell) {
    this(cell, null);
  }

  /**
   * Construct a new {@link ButtonBase} with the specified cell and initial
   * value.
   * 
   * @param cell the cell to wrap
   * @param initialValue the initial value of the Cell
   */
  protected ButtonBase(ButtonCellBase<C> cell, C initialValue) {
    super(cell, initialValue);
    this.cell = cell;
  }

  public HandlerRegistration addBlurHandler(BlurHandler handler) {
    return addDomHandler(handler, BlurEvent.getType());
  }

  public HandlerRegistration addClickHandler(ClickHandler handler) {
    return addDomHandler(handler, ClickEvent.getType());
  }

  public HandlerRegistration addFocusHandler(FocusHandler handler) {
    return addDomHandler(handler, FocusEvent.getType());
  }

  public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    return addDomHandler(handler, KeyDownEvent.getType());
  }

  public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
    return addDomHandler(handler, KeyPressEvent.getType());
  }

  public HandlerRegistration addKeyUpHandler(KeyUpHandler handler) {
    return addDomHandler(handler, KeyUpEvent.getType());
  }

  public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
    return addDomHandler(handler, MouseDownEvent.getType());
  }

  public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
    return addDomHandler(handler, MouseOutEvent.getType());
  }

  public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
    return addDomHandler(handler, MouseOverEvent.getType());
  }

  public HandlerRegistration addMouseUpHandler(MouseUpHandler handler) {
    return addDomHandler(handler, MouseUpEvent.getType());
  }

  /**
   * Get the decoration style of the button.
   */
  public Decoration getDecoration() {
    return cell.getDecoration();
  }

  public int getTabIndex() {
    return cell.getTabIndex();
  }

  public boolean isCollapseLeft() {
    return cell.isCollapseLeft();
  }

  public boolean isCollapseRight() {
    return cell.isCollapseRight();
  }

  public boolean isEnabled() {
    return cell.isEnabled();
  }

  public void setAccessKey(char key) {
    cell.setAccessKey(key);
    redraw();
  }

  public void setCollapseLeft(boolean isCollapsed) {
    if (cell.isCollapseLeft() == isCollapsed) {
      return;
    }
    cell.setCollapseLeft(isCollapsed);
    redraw();
  }

  public void setCollapseRight(boolean isCollapsed) {
    if (cell.isCollapseRight() == isCollapsed) {
      return;
    }
    cell.setCollapseRight(isCollapsed);
    redraw();
  }

  /**
   * Set the {@link Decoration} of the button.
   * 
   * @param decoration the button decoration
   */
  public void setDecoration(Decoration decoration) {
    if (cell.getDecoration() == decoration) {
      return;
    }
    cell.setDecoration(decoration);
    redraw();
  }

  public void setEnabled(boolean enabled) {
    if (cell.isEnabled() == enabled) {
      return;
    }
    cell.setEnabled(enabled);
    redraw();
  }

  public void setFocus(boolean focused) {
    cell.setFocus(getElement(), focused);
  }

  public void setTabIndex(int index) {
    cell.setTabIndex(index);
    redraw();
  }
}
