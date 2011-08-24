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
package com.google.gwt.examples.cell;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesUtils;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.Arrays;
import java.util.List;

/**
 * Example of creating a custom {@link Cell} that responds to events.
 */
public class CellWithEventsExample implements EntryPoint {

  /**
   * A custom {@link Cell} used to render a string that contains the name of a
   * color.
   */
  static class ColorCell extends AbstractCell<String> {

    /**
     * The HTML templates used to render the cell.
     */
    interface Templates extends SafeHtmlTemplates {
      /**
       * The template for this Cell, which includes styles and a value.
       * 
       * @param styles the styles to include in the style attribute of the div
       * @param value the safe value. Since the value type is {@link SafeHtml},
       *          it will not be escaped before including it in the template.
       *          Alternatively, you could make the value type String, in which
       *          case the value would be escaped.
       * @return a {@link SafeHtml} instance
       */
      @SafeHtmlTemplates.Template("<div style=\"{0}\">{1}</div>")
      SafeHtml cell(SafeStyles styles, SafeHtml value);
    }

    /**
     * Create a singleton instance of the templates used to render the cell.
     */
    private static Templates templates = GWT.create(Templates.class);

    public ColorCell() {
      /*
       * Sink the click and keydown events. We handle click events in this
       * class. AbstractCell will handle the keydown event and call
       * onEnterKeyDown() if the user presses the enter key while the cell is
       * selected.
       */
      super("click", "keydown");
    }

    /**
     * Called when an event occurs in a rendered instance of this Cell. The
     * parent element refers to the element that contains the rendered cell, NOT
     * to the outermost element that the Cell rendered.
     */
    @Override
    public void onBrowserEvent(Context context, Element parent, String value, NativeEvent event,
        ValueUpdater<String> valueUpdater) {
      // Let AbstractCell handle the keydown event.
      super.onBrowserEvent(context, parent, value, event, valueUpdater);

      // Handle the click event.
      if ("click".equals(event.getType())) {
        // Ignore clicks that occur outside of the outermost element.
        EventTarget eventTarget = event.getEventTarget();
        if (parent.getFirstChildElement().isOrHasChild(Element.as(eventTarget))) {
          doAction(value, valueUpdater);
        }
      }
    }

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

      // Use the template to create the Cell's html.
      SafeStyles styles = SafeStylesUtils.forTrustedColor(safeValue.asString());
      SafeHtml rendered = templates.cell(styles, safeValue);
      sb.append(rendered);
    }

    /**
     * onEnterKeyDown is called when the user presses the ENTER key will the
     * Cell is selected. You are not required to override this method, but its a
     * common convention that allows your cell to respond to key events.
     */
    @Override
    protected void onEnterKeyDown(Context context, Element parent, String value, NativeEvent event,
        ValueUpdater<String> valueUpdater) {
      doAction(value, valueUpdater);
    }

    private void doAction(String value, ValueUpdater<String> valueUpdater) {
      // Alert the user that they selected a value.
      Window.alert("You selected the color " + value);

      // Trigger a value updater. In this case, the value doesn't actually
      // change, but we use a ValueUpdater to let the app know that a value
      // was clicked.
      valueUpdater.update(value);
    }
  }

  /**
   * The list of data to display.
   */
  private static final List<String> COLORS = Arrays.asList("red", "green", "blue", "violet",
      "black", "gray");

  @Override
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