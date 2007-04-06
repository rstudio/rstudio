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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Color picker. Each "item" represents a CSS color.
 */
public class ColorPicker extends AbstractItemPicker {

  class ColorItem extends Item {
    private Object color;

    ColorItem(int index, Object color) {
      super(index);
      this.color = color;
    }

    Object getColor() {
      return color;
    }
  }

  /**
   * Default colors supplied to the color picker popup. The default colors are a
   * selection of 60 web-safe CSS color styles.
   */
  public static final List DEFAULT_COLORS;

  private static final String STYLENAME_DEFAULT = "gwt-ColorPicker ";

  static {
    String[] baseColors = {
        "#ffffcc", "#ffff66", "#ffcc66", "#F2984C", "#E1771E", "#B47B10",
        "#A9501B", "#6F3C1B", "#804000", "#CC0000", "#940F04", "#660000",
        "#C3D9FF", "#99C9FF", "#66B5FF", "#3D81EE", "#0066CC", "#6C82B5",
        "#32527A", "#2D6E89", "#006699", "#215670", "#003366", "#000033",
        "#CAF99B", "#80FF00", "#00FF80", "#78B749", "#2BA94F", "#38B63C",
        "#0D8F63", "#2D8930", "#1B703A", "#11593C", "#063E3F", "#002E3F",
        "#FFBBE8", "#E895CC", "#FF6FCF", "#C94093", "#9D1961", "#800040",
        "#800080", "#72179D", "#6728B2", "#6131BD", "#341473", "#400058",
        "#ffffff", "#e6e6e6", "#cccccc", "#b3b3b3", "#999999", "#808080",
        "#7f7f7f", "#666666", "#4c4c4c", "#333333", "#191919", "#000000"};
    DEFAULT_COLORS = Arrays.asList(baseColors);
  }

  private int columnsPerRow = -1;

  /**
   * Constructor for {@link ColorPicker}.
   */
  public ColorPicker() {
    this(12);
  }

  /**
   * Constructor for {@link ColorPicker}.
   * 
   * @param numColumns number of columns to be displayed
   */
  public ColorPicker(int numColumns) {
    this(DEFAULT_COLORS, numColumns);
  }

  /**
   * Constructor for {@link ColorPicker}. The passed in {@link Collection}
   * should contain objects such that the {@link String#toString()} method
   * returns a representation of a CSS color.
   * 
   * @param colors color collection
   * @param numColumns number of columns to be displayed
   */
  public ColorPicker(Collection colors, int numColumns) {
    this.setColumnsPerRow(numColumns);
    setStyleName(STYLENAME_DEFAULT);
    setItems(colors);
  }

  public boolean delegateKeyDown(char keyCode) {
    if (isAttached()) {
      switch (keyCode) {
        case KeyboardListener.KEY_DOWN:
          shiftSelection(getColumnsPerRow());
          break;
        case KeyboardListener.KEY_UP:
          shiftSelection(-getColumnsPerRow());
          break;
        case KeyboardListener.KEY_LEFT:
          shiftSelection(-1);
          break;
        case KeyboardListener.KEY_RIGHT:
          shiftSelection(1);
          break;
        case KeyboardListener.KEY_ENTER:
          commitSelection();
          break;
        default:
          // Avoid shared post processing.
          return false;
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * Gets the number of columns to display per row of colors.
   * 
   * @return numColumns number of columns
   */
  public int getColumnsPerRow() {
    return columnsPerRow;
  }

  public Object getValue(int i) {
    ColorItem item = (ColorItem) getItem(i);
    if (item == null) {
      return null;
    } else {
      return item.getColor();
    }
  }

  /**
   * Sets the number of columns to display per row of colors.
   * 
   * @param numColumns number of columns
   */
  public void setColumnsPerRow(int numColumns) {
    if (numColumns <= 0) {
      throw new IllegalStateException("Cannot use " + numColumns
          + " as the number of columns per row");
    }
    this.columnsPerRow = numColumns;
  }

  /**
   * Sets the items in the {@link ColorPicker}. Each color's
   * {@link String#toString()} method should return a representation of a CSS
   * color.
   * 
   * @param colors collection of colors.
   */
  public final void setItems(Collection colors) {
    setItems(colors.iterator());
  }

  protected void format(Element e, Object item) {
    DOM.setStyleAttribute(e, "background", item.toString());
  }

  /**
   * Sets the items in the {@link ColorPicker}.
   */
  private final void setItems(Iterator colors) {
    clearItems();
    int row = 0;
    int i = 0;
    while (true) {

      for (int column = 0; column < getColumnsPerRow(); column++) {
        if (!colors.hasNext()) {
          if (i == 0) {
            throw new IllegalStateException(
                "Cannot populate a color picker with 0 colors!");
          }
          // All items have been placed.
          return;
        }

        ColorItem item = new ColorItem(i, colors.next());
        format(item.getElement(), item.getColor());
        getLayout().setWidget(row, column, item);
        ++i;
      }
      ++row;
    }
  }

}
