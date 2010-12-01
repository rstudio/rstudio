/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.examples.cell;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.Arrays;
import java.util.List;

/**
 * Example of creating a custom {@link Cell}.
 */
public class CellExample implements EntryPoint {

  /**
   * A custom {@link Cell} used to render a string that contains the name of a
   * color.
   */
  private static class ColorCell extends AbstractCell<String> {

    @Override
    public void render(Context context, String value, SafeHtmlBuilder sb) {
      /*
       * Always do a null check on the value. Cell widgets can pass null to
       * cells if the underlying data contains a null, or if the data arrives
       * out of order.
       */
      if (value == null) {
        return;
      }

      // If the value comes from the user, we escape it to avoid XSS attacks.
      SafeHtml safeValue = SafeHtmlUtils.fromString(value);

      // Append some HTML that sets the text color.
      sb.appendHtmlConstant("<div style=\"color:" + safeValue.asString()
          + "\">");
      sb.append(safeValue);
      sb.appendHtmlConstant("</div>");
    }
  }

  /**
   * The list of data to display.
   */
  private static final List<String> COLORS = Arrays.asList("red", "green",
      "blue", "violet", "black", "gray");

  public void onModuleLoad() {
    // Create a cell to render each value.
    ColorCell cell = new ColorCell();

    // Use the cell in a CellList.
    CellList<String> cellList = new CellList<String>(cell);

    // Push the data into the widget.
    cellList.setRowData(0, COLORS);

    // Add it to the root panel.
    RootPanel.get().add(cellList);
  }
}